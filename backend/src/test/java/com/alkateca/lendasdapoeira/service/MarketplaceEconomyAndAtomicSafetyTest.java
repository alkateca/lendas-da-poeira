package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.MarketListing;
import com.alkateca.lendasdapoeira.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class MarketplaceEconomyAndAtomicSafetyTest {

    private PlayerStorageService playerStorageService;
    private ShopService shopService;
    private MarketplaceService marketplaceService;

    private Player seller;
    private Player buyer1;
    private Player buyer2;

    @BeforeEach
    void setUp() {
        playerStorageService = new PlayerStorageService();
        OpenBoosterService openBoosterService = new OpenBoosterService();
        PresetDeckService presetDeckService = new PresetDeckService();
        shopService = new ShopService(openBoosterService, presetDeckService);
        marketplaceService = new MarketplaceService(playerStorageService, shopService);

        seller = new Player(UUID.randomUUID(), "Vendedor", null);
        seller.setGold(200);
        seller.addCardToCollection("Bola de fogo", 2);
        seller.addHeroToCollection("Moyra, Aprendiz da Santa", 1);

        // Salva um baralho usando 1 cópia de "Bola de fogo" e 1 "Moyra, Aprendiz da Santa"
        Card deckCard = new Card();
        deckCard.setCardName("Bola de fogo");
        HeroCard deckHero = new HeroCard();
        deckHero.setCardName("Moyra, Aprendiz da Santa");
        Deck activeDeck = new Deck("Deck Principal", new ArrayList<>(List.of(deckCard)), new ArrayList<>(List.of(deckHero)));
        seller.setCurrentDeck(activeDeck);

        buyer1 = new Player(UUID.randomUUID(), "Comprador 1", null);
        buyer1.setGold(500);

        buyer2 = new Player(UUID.randomUUID(), "Comprador 2", null);
        buyer2.setGold(500);

        playerStorageService.saveOrUpdatePlayer(seller);
        playerStorageService.saveOrUpdatePlayer(buyer1);
        playerStorageService.saveOrUpdatePlayer(buyer2);
    }

    @Test
    @DisplayName("Deve remover carta em custódia ao anunciar e bloquear venda de cópias em uso nos baralhos")
    void shouldLockCardInEscrowAndPreventSellingCardsUsedInDecks() {
        // Arrange
        String idempotencyKey1 = "LIST_KEY_1_" + UUID.randomUUID();
        String idempotencyKey2 = "LIST_KEY_2_" + UUID.randomUUID();

        // Act
        // Vende a 1ª cópia excedente de "Bola de fogo" (possui 2, usa 1 no deck -> permitido)
        MarketListing listing = marketplaceService.createListing(
                seller.getUuid(), "Bola de fogo", false, 120, idempotencyKey1
        );

        // Assert
        assertNotNull(listing.getId());
        assertEquals(MarketListing.ListingStatus.ACTIVE, listing.getStatus());
        assertEquals(1, seller.getOwnedCardCount("Bola de fogo"), "A cópia anunciada deve sair imediatamente da coleção (escrow).");

        // Tentativa de vender a 2ª cópia que está em uso no currentDeck deve falhar
        assertThrows(IllegalStateException.class, () -> {
            marketplaceService.createListing(seller.getUuid(), "Bola de fogo", false, 120, idempotencyKey2);
        }, "Não deve permitir vender cópia necessária para manter o baralho salvo válido.");

        // Tentativa de vender o único Herói alocado no baralho salvo também deve falhar
        assertThrows(IllegalStateException.class, () -> {
            marketplaceService.createListing(seller.getUuid(), "Moyra, Aprendiz da Santa", true, 300, "LIST_HERO_KEY");
        }, "Não deve permitir vender Herói alocado em baralho salvo.");
    }

    @Test
    @DisplayName("Deve transferir Gold e carta atomicamente na compra e devolver carta no cancelamento")
    void shouldBuyAndCancelListingsAtomically() {
        // Arrange
        seller.addCardToCollection("Rapieira de Cristal", 1);
        MarketListing listingToBuy = marketplaceService.createListing(
                seller.getUuid(), "Bola de fogo", false, 150, "KEY_CREATE_BUY_" + UUID.randomUUID()
        );
        MarketListing listingToCancel = marketplaceService.createListing(
                seller.getUuid(), "Rapieira de Cristal", false, 90, "KEY_CREATE_CANCEL_" + UUID.randomUUID()
        );
        assertEquals(0, seller.getOwnedCardCount("Rapieira de Cristal"));

        // Act
        MarketListing soldListing = marketplaceService.buyListing(
                buyer1.getUuid(), listingToBuy.getId(), "KEY_BUY_1_" + UUID.randomUUID()
        );
        MarketListing cancelledListing = marketplaceService.cancelListing(
                seller.getUuid(), listingToCancel.getId(), "KEY_CANCEL_1_" + UUID.randomUUID()
        );

        // Assert
        assertEquals(MarketListing.ListingStatus.SOLD, soldListing.getStatus());
        assertEquals(buyer1.getUuid(), soldListing.getBuyerId());
        assertEquals(350, buyer1.getGold(), "Comprador deve ter 150 Gold debitados (500 -> 350).");
        assertEquals(350, seller.getGold(), "Vendedor deve receber 150 Gold (200 -> 350).");
        assertEquals(1, buyer1.getOwnedCardCount("Bola de fogo"), "Comprador deve receber 1 unidade da carta na coleção.");

        assertEquals(MarketListing.ListingStatus.CANCELLED, cancelledListing.getStatus());
        assertEquals(1, seller.getOwnedCardCount("Rapieira de Cristal"), "Cancelar o anúncio deve devolver a carta à coleção do vendedor.");
    }

    @Test
    @DisplayName("Deve proteger contra requisições repetidas (Idempotency-Key) e compras concorrentes no mesmo anúncio")
    void shouldPreventDuplicateIdempotencyAndConcurrentDoublePurchase() throws Exception {
        // Arrange
        MarketListing listing = marketplaceService.createListing(
                seller.getUuid(), "Bola de fogo", false, 200, "UNIQUE_LIST_" + UUID.randomUUID()
        );

        // Act & Assert 1: Mesma Idempotency-Key enviada duas vezes na criação
        seller.addCardToCollection("Lamina de Cristal", 2);
        String repeatedKey = "REPEATED_KEY_" + UUID.randomUUID();
        marketplaceService.createListing(seller.getUuid(), "Lamina de Cristal", false, 50, repeatedKey);
        assertThrows(IllegalStateException.class, () -> {
            marketplaceService.createListing(seller.getUuid(), "Lamina de Cristal", false, 50, repeatedKey);
        }, "Requisição repetida com a mesma Idempotency-Key deve ser bloqueada.");

        // Act 2: Dois compradores tentam comprar o mesmo anúncio simultaneamente em threads paralelas
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Callable<Void> taskBuyer1 = () -> {
            try {
                marketplaceService.buyListing(buyer1.getUuid(), listing.getId(), "BUY_T1_" + UUID.randomUUID());
                successCount.incrementAndGet();
            } catch (IllegalStateException e) {
                failureCount.incrementAndGet();
            }
            return null;
        };

        Callable<Void> taskBuyer2 = () -> {
            try {
                marketplaceService.buyListing(buyer2.getUuid(), listing.getId(), "BUY_T2_" + UUID.randomUUID());
                successCount.incrementAndGet();
            } catch (IllegalStateException e) {
                failureCount.incrementAndGet();
            }
            return null;
        };

        List<Future<Void>> futures = executor.invokeAll(List.of(taskBuyer1, taskBuyer2));
        for (Future<Void> f : futures) {
            f.get();
        }
        executor.shutdown();

        // Assert 2: Apenas 1 compra pode ter sucesso e o vendedor só pode receber 200 Gold uma única vez
        assertEquals(1, successCount.get(), "Apenas um comprador deve conseguir adquirir o anúncio.");
        assertEquals(1, failureCount.get(), "A segunda requisição concorrente deve falhar com segurança.");
        assertEquals(400, seller.getGold(), "O vendedor deve receber exatamente 200 Gold (200 + 200 = 400).");
        int totalCardsDistributed = buyer1.getOwnedCardCount("Bola de fogo") + buyer2.getOwnedCardCount("Bola de fogo");
        assertEquals(1, totalCardsDistributed, "Apenas 1 cópia da carta pode existir somando os dois compradores.");
    }
}
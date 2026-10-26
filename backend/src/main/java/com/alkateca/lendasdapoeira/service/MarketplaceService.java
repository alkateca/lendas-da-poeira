package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.DTO.ShopDtos.PurchaseResponseDTO;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.MarketListing;
import com.alkateca.lendasdapoeira.entity.Player;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class MarketplaceService {

    private final PlayerStorageService playerStorageService;
    private final ShopService shopService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final File listingsFile = new File("data/market_listings.json");

    private final Map<UUID, MarketListing> listings = new LinkedHashMap<>();
    private final Set<String> processedIdempotencyKeys = Collections.synchronizedSet(new LinkedHashSet<>());
    private final ReentrantLock economyLock = new ReentrantLock(true);

    public MarketplaceService(PlayerStorageService playerStorageService, ShopService shopService) {
        this.playerStorageService = playerStorageService;
        this.shopService = shopService;
    }

    @PostConstruct
    public void init() {
        try {
            File dir = new File("data");
            if (!dir.exists()) dir.mkdir();
            if (listingsFile.exists()) {
                List<MarketListing> loaded = mapper.readValue(listingsFile, new TypeReference<List<MarketListing>>() {});
                for (MarketListing item : loaded) {
                    listings.put(item.getId(), item);
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao carregar market_listings.json: " + e.getMessage());
        }
    }

    private void saveListingsToDisk() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(listingsFile, new ArrayList<>(listings.values()));
        } catch (IOException e) {
            System.err.println("Erro ao salvar market_listings.json: " + e.getMessage());
        }
    }

    private void verifyAndRegisterIdempotency(String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (!processedIdempotencyKeys.add(idempotencyKey.trim())) {
                throw new IllegalStateException("Operação já processada (requisição duplicada bloqueada).");
            }
        }
    }

    public List<MarketListing> listActiveListings() {
        economyLock.lock();
        try {
            return listings.values().stream()
                    .filter(l -> l.getStatus() == MarketListing.ListingStatus.ACTIVE)
                    .sorted(Comparator.comparingLong(MarketListing::getCreatedAtEpochMs).reversed())
                    .collect(Collectors.toList());
        } finally {
            economyLock.unlock();
        }
    }

    /**
     * Anuncia 1 unidade de carta ou herói da coleção do jogador pelo preço desejado.
     * Retira imediatamente a carta da coleção do vendedor (custódia/escrow).
     */
    public MarketListing createListing(UUID sellerId, String cardName, boolean isHero, int price, String idempotencyKey) {
        if (price <= 0) {
            throw new IllegalArgumentException("O preço de venda deve ser maior que zero.");
        }
        if (cardName == null || cardName.isBlank()) {
            throw new IllegalArgumentException("Nome da carta inválido.");
        }

        economyLock.lock();
        try {
            verifyAndRegisterIdempotency(idempotencyKey);

            Player seller = playerStorageService.getPlayer(sellerId);
            if (seller == null) {
                throw new IllegalArgumentException("Jogador vendedor não encontrado.");
            }

            // Impede que o jogador venda uma cópia que está alocada em seus baralhos salvos
            int maxUsedInDecks = getMaxCopiesUsedInPlayerDecks(seller, cardName, isHero);
            int currentlyOwned = isHero ? seller.getOwnedHeroCount(cardName) : seller.getOwnedCardCount(cardName);

            if (currentlyOwned <= 0) {
                throw new IllegalStateException("Você não possui '" + cardName + "' na sua coleção.");
            }
            if (currentlyOwned - 1 < maxUsedInDecks) {
                throw new IllegalStateException(
                        "Não é possível vender '" + cardName + "': todas as suas cópias estão em uso nos seus baralhos salvos."
                );
            }

            if (isHero) {
                seller.removeHeroFromCollection(cardName, 1);
            } else {
                seller.removeCardFromCollection(cardName, 1);
            }

            MarketListing listing = new MarketListing(
                    UUID.randomUUID(),
                    seller.getUuid(),
                    seller.getName(),
                    cardName,
                    isHero,
                    price,
                    MarketListing.ListingStatus.ACTIVE,
                    System.currentTimeMillis(),
                    null
            );

            listings.put(listing.getId(), listing);
            playerStorageService.saveOrUpdatePlayer(seller);
            saveListingsToDisk();

            return listing;
        } finally {
            economyLock.unlock();
        }
    }

    /**
     * Compra uma carta anunciada por outro jogador de forma atômica.
     */
    public MarketListing buyListing(UUID buyerId, UUID listingId, String idempotencyKey) {
        economyLock.lock();
        try {
            verifyAndRegisterIdempotency(idempotencyKey);

            MarketListing listing = listings.get(listingId);
            if (listing == null || listing.getStatus() != MarketListing.ListingStatus.ACTIVE) {
                throw new IllegalStateException("Este anúncio não está mais disponível no mercado.");
            }

            if (listing.getSellerId().equals(buyerId)) {
                throw new IllegalArgumentException("Você não pode comprar o seu próprio anúncio. Use a opção de cancelar.");
            }

            Player buyer = playerStorageService.getPlayer(buyerId);
            Player seller = playerStorageService.getPlayer(listing.getSellerId());

            if (buyer == null || seller == null) {
                throw new IllegalArgumentException("Comprador ou vendedor não encontrado.");
            }

            if (buyer.getGold() < listing.getPrice()) {
                throw new IllegalStateException(
                        "Saldo de Gold insuficiente! Necessário: " + listing.getPrice() + ", Disponível: " + buyer.getGold()
                );
            }

            // Operação financeira e transferência de item atômicas
            buyer.deductGold(listing.getPrice());
            seller.addGold(listing.getPrice());

            if (listing.isHeroCard()) {
                buyer.addHeroToCollection(listing.getCardName(), 1);
            } else {
                buyer.addCardToCollection(listing.getCardName(), 1);
            }

            listing.setStatus(MarketListing.ListingStatus.SOLD);
            listing.setBuyerId(buyer.getUuid());

            playerStorageService.saveOrUpdatePlayer(buyer);
            playerStorageService.saveOrUpdatePlayer(seller);
            saveListingsToDisk();

            return listing;
        } finally {
            economyLock.unlock();
        }
    }

    /**
     * Cancela um anúncio ativo e devolve a carta/herói para a coleção do vendedor.
     */
    public MarketListing cancelListing(UUID sellerId, UUID listingId, String idempotencyKey) {
        economyLock.lock();
        try {
            verifyAndRegisterIdempotency(idempotencyKey);

            MarketListing listing = listings.get(listingId);
            if (listing == null || listing.getStatus() != MarketListing.ListingStatus.ACTIVE) {
                throw new IllegalStateException("Anúncio não encontrado ou já encerrado.");
            }
            if (!listing.getSellerId().equals(sellerId)) {
                throw new IllegalArgumentException("Apenas o dono do anúncio pode cancelá-lo.");
            }

            Player seller = playerStorageService.getPlayer(sellerId);
            if (seller == null) {
                throw new IllegalArgumentException("Vendedor não encontrado.");
            }

            listing.setStatus(MarketListing.ListingStatus.CANCELLED);

            if (listing.isHeroCard()) {
                seller.addHeroToCollection(listing.getCardName(), 1);
            } else {
                seller.addCardToCollection(listing.getCardName(), 1);
            }

            playerStorageService.saveOrUpdatePlayer(seller);
            saveListingsToDisk();

            return listing;
        } finally {
            economyLock.unlock();
        }
    }

    /**
     * Executa compras de Boosters/Decks do ShopService dentro do mesmo lock atômico e persiste em disco.
     */
    public PurchaseResponseDTO purchaseCatalogProductAtomically(UUID playerId, String productId, String idempotencyKey) {
        economyLock.lock();
        try {
            verifyAndRegisterIdempotency(idempotencyKey);
            Player player = playerStorageService.getPlayer(playerId);
            PurchaseResponseDTO response = shopService.purchaseProduct(player, productId);
            playerStorageService.saveOrUpdatePlayer(player);
            return response;
        } finally {
            economyLock.unlock();
        }
    }

    private int getMaxCopiesUsedInPlayerDecks(Player player, String targetName, boolean isHero) {
        List<Deck> allDecks = new ArrayList<>();
        if (player.getCurrentDeck() != null) allDecks.add(player.getCurrentDeck());
        if (player.getDecks() != null) allDecks.addAll(player.getDecks());

        int maxUsed = 0;
        for (Deck deck : allDecks) {
            if (deck == null) continue;
            int countInDeck = 0;
            if (isHero && deck.getHeroList() != null) {
                for (HeroCard h : deck.getHeroList()) {
                    if (h != null && targetName.equalsIgnoreCase(h.getCardName())) countInDeck++;
                }
            } else if (!isHero) {
                if (deck.getCardList() != null) {
                    for (Card c : deck.getCardList()) {
                        if (c != null && targetName.equalsIgnoreCase(c.getCardName())) countInDeck++;
                    }
                }
                if (deck.getLiberationList() != null) {
                    for (Card l : deck.getLiberationList()) {
                        if (l != null && targetName.equalsIgnoreCase(l.getCardName())) countInDeck++;
                    }
                }
            }
            if (countInDeck > maxUsed) {
                maxUsed = countInDeck;
            }
        }
        return maxUsed;
    }
}

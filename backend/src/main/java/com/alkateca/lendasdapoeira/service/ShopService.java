package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.DTO.ShopDtos.PurchaseResponseDTO;
import com.alkateca.lendasdapoeira.DTO.ShopDtos.RevealedCardDTO;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.entity.Product;
import com.alkateca.lendasdapoeira.enums.CardSet;
import com.alkateca.lendasdapoeira.enums.ProductType;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ShopService {

    private final OpenBoosterService openBoosterService;
    private final PresetDeckService presetDeckService;
    private final Map<String, Product> productCatalog = new LinkedHashMap<>();

    public ShopService(OpenBoosterService openBoosterService, PresetDeckService presetDeckService) {
        this.openBoosterService = openBoosterService;
        this.presetDeckService = presetDeckService;
        initDefaultProducts();
    }

    private void initDefaultProducts() {
        registerProduct(new Product(
                "booster-base-5",
                "Pacote das Areias (5 Cartas)",
                "Contém 5 cartas sorteadas da Coleção Base (60% Comum, 25% Incomum, 12% Rara, 3% Lendária). Chance de conter Herói!",
                ProductType.BOOSTER,
                100,
                CardSet.BASE,
                5,
                0.25,
                null
        ));

        registerProduct(new Product(
                "booster-elite-5",
                "Cofre de Cristal (5 Cartas + Herói)",
                "Pacote especial de 5 cartas da Coleção Base com 100% de garantia de vir 1 Herói no último slot!",
                ProductType.BOOSTER,
                220,
                CardSet.BASE,
                5,
                1.0,
                null
        ));

        registerProduct(new Product(
                "deck-preset-1",
                "Deck Estruturado: Ordem do Cristal",
                "Desbloqueia 3 Heróis e 30 Cartas estruturadas diretamente na sua coleção e lista de baralhos.",
                ProductType.DECK,
                350,
                CardSet.BASE,
                0,
                0.0,
                "PRESET_0"
        ));

        registerProduct(new Product(
                "deck-preset-2",
                "Deck Estruturado: Fúria das Cinzas",
                "Baralho tático completo pronto para batalha. Todas as cartas e heróis são adicionados à sua coleção.",
                ProductType.DECK,
                350,
                CardSet.BASE,
                0,
                0.0,
                "PRESET_1"
        ));

        registerProduct(new Product(
                "item-sleeve-phantom",
                "Protetor de Carta: Phantom CRT",
                "Item colecionável de personalização de terminal.",
                ProductType.ITEM,
                150,
                CardSet.BASE,
                0,
                0.0,
                null
        ));
    }

    public void registerProduct(Product product) {
        if (product != null && product.getId() != null) {
            productCatalog.put(product.getId(), product);
        }
    }

    public List<Product> listProducts() {
        return new ArrayList<>(productCatalog.values());
    }

    public PurchaseResponseDTO purchaseProduct(Player player, String productId) {
        if (player == null) {
            throw new IllegalArgumentException("Jogador não encontrado.");
        }

        Product product = productCatalog.get(productId);
        if (product == null) {
            throw new IllegalArgumentException("Produto inexistente no catálogo: " + productId);
        }

        // Critério de Aceite 1: Jogador não pode comprar se não tiver saldo suficiente
        if (player.getPoeira() < product.getPrice()) {
            throw new IllegalStateException(
                    "Saldo insuficiente! Necessário: " + product.getPrice() + " Poeira (Disponível: " + player.getPoeira() + ")."
            );
        }

        player.deductPoeira(product.getPrice());

        List<RevealedCardDTO> openedCards = new ArrayList<>();
        Deck unlockedDeck = null;
        String message;

        switch (product.getType()) {
            case BOOSTER -> {
                openedCards = openBoosterService.openBoosterForPlayer(player, product);
                message = "Booster aberto com sucesso! " + openedCards.size() + " cartas adicionadas à sua coleção.";
            }
            case DECK -> {
                unlockedDeck = grantStructuredDeckToPlayer(player, product);
                message = "Deck Estruturado '" + unlockedDeck.getDeckName() + "' e todas as suas cartas foram adicionados à sua coleção!";
            }
            case ITEM -> {
                player.addItemToCollection(product.getId(), 1);
                message = "Item '" + product.getName() + "' adquirido com sucesso!";
            }
            default -> throw new IllegalStateException("Tipo de produto não suportado: " + product.getType());
        }

        return new PurchaseResponseDTO(
                product.getId(),
                product.getType().name(),
                message,
                player,
                openedCards,
                unlockedDeck
        );
    }

    private Deck grantStructuredDeckToPlayer(Player player, Product deckProduct) {
        Deck templateDeck = presetDeckService.getPresetDeckById(deckProduct.getPresetDeckId(), player.getUuid());
        if (templateDeck == null) {
            throw new IllegalStateException("Não foi possível gerar o deck estruturado.");
        }

        templateDeck.setUuid(UUID.randomUUID());
        templateDeck.setDeckName(deckProduct.getName().replace("Deck Estruturado: ", ""));

        if (templateDeck.getHeroList() != null) {
            for (HeroCard h : templateDeck.getHeroList()) {
                if (h != null) player.addHeroToCollection(h.getCardName(), 1);
            }
        }

        if (templateDeck.getCardList() != null) {
            for (Card c : templateDeck.getCardList()) {
                if (c != null) player.addCardToCollection(c.getCardName(), 1);
            }
        }

        if (templateDeck.getLiberationList() != null) {
            for (Card lib : templateDeck.getLiberationList()) {
                if (lib != null) player.addCardToCollection(lib.getCardName(), 1);
            }
        }

        if (player.getDecks() == null) {
            player.setDecks(new ArrayList<>());
        }
        player.getDecks().add(templateDeck);

        return templateDeck;
    }
}
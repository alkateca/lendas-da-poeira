package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.DTO.ShopDtos.RevealedCardDTO;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.entity.Product;
import com.alkateca.lendasdapoeira.enums.CardRarity;
import com.alkateca.lendasdapoeira.enums.CardSet;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OpenBoosterService {

    // Tabela de Drop Rate (Soma = 100%)
    public static final Map<CardRarity, Double> DEFAULT_RARITY_WEIGHTS = Map.of(
            CardRarity.COMUM, 60.0,
            CardRarity.INCOMUM, 25.0,
            CardRarity.RARA, 12.0,
            CardRarity.LENDARIA, 3.0
    );

    private final SecureRandom random = new SecureRandom();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private List<Card> catalogCards = new ArrayList<>();
    private List<HeroCard> catalogHeroes = new ArrayList<>();

    @PostConstruct
    public void loadCatalog() {
        try (InputStream cardsStream = new ClassPathResource("cards.json").getInputStream()) {
            this.catalogCards = objectMapper.readValue(cardsStream, new TypeReference<List<Card>>() {});
        } catch (Exception e) {
            System.err.println("Erro ao carregar cards.json no OpenBoosterService: " + e.getMessage());
        }

        try (InputStream heroesStream = new ClassPathResource("heros.json").getInputStream()) {
            this.catalogHeroes = objectMapper.readValue(heroesStream, new TypeReference<List<HeroCard>>() {});
        } catch (Exception e) {
            System.err.println("Erro ao carregar heros.json no OpenBoosterService: " + e.getMessage());
        }
    }

    // Permite injetar catálogos em testes unitários sem depender de I/O
    public void setCatalogsForTesting(List<Card> cards, List<HeroCard> heroes) {
        this.catalogCards = cards != null ? cards : new ArrayList<>();
        this.catalogHeroes = heroes != null ? heroes : new ArrayList<>();
    }

    public List<RevealedCardDTO> openBoosterForPlayer(Player player, Product boosterProduct) {
        int count = boosterProduct.getCardsPerBooster() > 0 ? boosterProduct.getCardsPerBooster() : 5;
        CardSet targetSet = boosterProduct.getCardSet();

        List<Card> eligibleCards = catalogCards.stream()
                .filter(c -> targetSet == null || c.getCardSet() == null || c.getCardSet() == targetSet)
                .collect(Collectors.toList());

        List<HeroCard> eligibleHeroes = catalogHeroes.stream()
                .filter(h -> targetSet == null || h.getCardSet() == null || h.getCardSet() == targetSet)
                .collect(Collectors.toList());

        if (eligibleCards.isEmpty()) {
            throw new IllegalStateException("Catálogo de cartas vazio para abrir o booster.");
        }

        List<RevealedCardDTO> results = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            CardRarity rolledRarity = rollRarity(DEFAULT_RARITY_WEIGHTS);
            boolean isLastSlot = (i == count - 1);
            boolean rollHero = isLastSlot
                    && !eligibleHeroes.isEmpty()
                    && random.nextDouble() < boosterProduct.getHeroSlotProbability();

            if (rollHero) {
                HeroCard pulledHero = pickRandomByRarity(eligibleHeroes, rolledRarity);
                player.addHeroToCollection(pulledHero.getCardName(), 1);
                results.add(new RevealedCardDTO(
                        pulledHero.getCardName(),
                        "HERO",
                        pulledHero.getColor() != null ? pulledHero.getColor().name() : "NEUTRAL",
                        pulledHero.getRarity() != null ? pulledHero.getRarity().name() : rolledRarity.name(),
                        pulledHero.getDescricao(),
                        true,
                        pulledHero.getEspirito(),
                        pulledHero.getAtaque(),
                        pulledHero.getDefesa(),
                        pulledHero.getVidaMaxima(),
                        player.getOwnedHeroCount(pulledHero.getCardName())
                ));
            } else {
                Card pulledCard = pickRandomByRarity(eligibleCards, rolledRarity);
                player.addCardToCollection(pulledCard.getCardName(), 1);
                results.add(new RevealedCardDTO(
                        pulledCard.getCardName(),
                        pulledCard.getCardType() != null ? pulledCard.getCardType().name() : "SPELL",
                        pulledCard.getColor() != null ? pulledCard.getColor().name() : "NEUTRAL",
                        pulledCard.getRarity() != null ? pulledCard.getRarity().name() : rolledRarity.name(),
                        pulledCard.getDescricao(),
                        false,
                        0, 0, 0, 0,
                        player.getOwnedCardCount(pulledCard.getCardName())
                ));
            }
        }

        return results;
    }

    public CardRarity rollRarity(Map<CardRarity, Double> weights) {
        double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
        double roll = random.nextDouble() * totalWeight;
        double cumulative = 0.0;

        for (CardRarity rarity : List.of(CardRarity.COMUM, CardRarity.INCOMUM, CardRarity.RARA, CardRarity.LENDARIA)) {
            cumulative += weights.getOrDefault(rarity, 0.0);
            if (roll <= cumulative) {
                return rarity;
            }
        }
        return CardRarity.COMUM;
    }

    private <T extends Card> T pickRandomByRarity(List<T> pool, CardRarity targetRarity) {
        List<T> matching = pool.stream()
                .filter(c -> c.getRarity() == targetRarity)
                .collect(Collectors.toList());

        if (matching.isEmpty()) {
            return pool.get(random.nextInt(pool.size()));
        }
        return matching.get(random.nextInt(matching.size()));
    }
}
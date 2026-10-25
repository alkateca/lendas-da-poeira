package com.alkateca.lendasdapoeira.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Player {

    public static final int DEFAULT_INITIAL_ELO = 1000;

    private UUID uuid;
    private String name;
    private Deck currentDeck;
    private List<Deck> decks = new ArrayList<>();

    // Inventário / Coleção Pessoal do Jogador (Chave: cardName -> Valor: Quantidade possuída)
    private Map<String, Integer> ownedHeroes = new HashMap<>();
    private Map<String, Integer> ownedCards = new HashMap<>();

    // Sistema de Ranqueamento Duplo
    private int permanentElo = DEFAULT_INITIAL_ELO;
    private int weeklyElo = DEFAULT_INITIAL_ELO;
    private String lastWeeklyResetWeek;

    // Construtor compatível com o restante do sistema
    public Player(UUID uuid, String name, Deck currentDeck) {
        this.uuid = uuid;
        this.name = name;
        this.currentDeck = currentDeck;
        this.permanentElo = DEFAULT_INITIAL_ELO;
        this.weeklyElo = DEFAULT_INITIAL_ELO;
    }

    public int getOwnedHeroCount(String heroName) {
        if (heroName == null || ownedHeroes == null) return 0;
        return ownedHeroes.getOrDefault(heroName, 0);
    }

    public int getOwnedCardCount(String cardName) {
        if (cardName == null || ownedCards == null) return 0;
        return ownedCards.getOrDefault(cardName, 0);
    }

    public void addHeroToCollection(String heroName, int copies) {
        if (heroName == null || copies <= 0) return;
        if (this.ownedHeroes == null) this.ownedHeroes = new HashMap<>();
        this.ownedHeroes.put(heroName, getOwnedHeroCount(heroName) + copies);
    }

    public void addCardToCollection(String cardName, int copies) {
        if (cardName == null || copies <= 0) return;
        if (this.ownedCards == null) this.ownedCards = new HashMap<>();
        this.ownedCards.put(cardName, Math.min(3, getOwnedCardCount(cardName) + copies));
    }
}

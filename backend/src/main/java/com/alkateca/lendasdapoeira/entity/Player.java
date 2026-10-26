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

    // Sistema monetário
    public static final int DEFAULT_INITIAL_POEIRA = 5000;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Balance {
        private int gold = DEFAULT_INITIAL_POEIRA;
    }

    private Balance balance = new Balance(DEFAULT_INITIAL_POEIRA);

    private int poeira = DEFAULT_INITIAL_POEIRA;
    private java.util.Map<String, Integer> ownedItems = new java.util.HashMap<>();

    // Construtor compatível com o restante do sistema
    public Player(UUID uuid, String name, Deck currentDeck) {
        this.uuid = uuid;
        this.name = name;
        this.currentDeck = currentDeck;
        this.permanentElo = DEFAULT_INITIAL_ELO;
        this.weeklyElo = DEFAULT_INITIAL_ELO;
    }

    public Balance getBalance() {
        if (this.balance == null) {
            this.balance = new Balance(this.poeira);
        }
        return this.balance;
    }

    public void setBalance(Balance balance) {
        this.balance = balance != null ? balance : new Balance(0);
        this.poeira = this.balance.getGold();
    }

    public int getGold() {
        return getBalance().getGold();
    }

    public void setGold(int gold) {
        if (gold < 0) {
            throw new IllegalArgumentException("O saldo de Gold não pode ser negativo.");
        }
        getBalance().setGold(gold);
        this.poeira = gold;
    }

    public int getPoeira() {
        return getGold();
    }

    public void setPoeira(int poeira) {
        setGold(Math.max(0, poeira));
    }

    public synchronized void addGold(int amount) {
        if (amount <= 0) return;
        setGold(getGold() + amount);
    }

    public synchronized void deductGold(int amount) {
        if (amount <= 0) return;
        if (getGold() < amount) {
            throw new IllegalStateException("Saldo de Gold insuficiente. Necessário: " + amount + ", Disponível: " + getGold());
        }
        setGold(getGold() - amount);
    }

    public void addPoeira(int amount) {
        addGold(amount);
    }

    public void deductPoeira(int amount) {
        deductGold(amount);
    }

    public int getOwnedHeroCount(String heroName) {
        return (heroName == null || ownedHeroes == null) ? 0 : ownedHeroes.getOrDefault(heroName, 0);
    }

    public int getOwnedCardCount(String cardName) {
        return (cardName == null || ownedCards == null) ? 0 : ownedCards.getOrDefault(cardName, 0);
    }

    public void addHeroToCollection(String heroName, int copies) {
        if (heroName == null || copies <= 0) return;
        if (this.ownedHeroes == null) this.ownedHeroes = new java.util.HashMap<>();
        this.ownedHeroes.put(heroName, getOwnedHeroCount(heroName) + copies);
    }

    public void addCardToCollection(String cardName, int copies) {
        if (cardName == null || copies <= 0) return;
        if (this.ownedCards == null) this.ownedCards = new java.util.HashMap<>();
        this.ownedCards.put(cardName, getOwnedCardCount(cardName) + copies);
    }

    public synchronized void removeCardFromCollection(String cardName, int copies) {
        if (cardName == null || copies <= 0) {
            throw new IllegalArgumentException("Carta ou quantidade inválida para remoção.");
        }
        int current = getOwnedCardCount(cardName);
        if (current < copies) {
            throw new IllegalStateException("Quantidade insuficiente da carta '" + cardName + "' na coleção.");
        }
        int remaining = current - copies;
        if (remaining == 0) {
            this.ownedCards.remove(cardName);
        } else {
            this.ownedCards.put(cardName, remaining);
        }
    }

    public synchronized void removeHeroFromCollection(String heroName, int copies) {
        if (heroName == null || copies <= 0) {
            throw new IllegalArgumentException("Herói ou quantidade inválida para remoção.");
        }
        int current = getOwnedHeroCount(heroName);
        if (current < copies) {
            throw new IllegalStateException("Quantidade insuficiente do Herói '" + heroName + "' na coleção.");
        }
        int remaining = current - copies;
        if (remaining == 0) {
            this.ownedHeroes.remove(heroName);
        } else {
            this.ownedHeroes.put(heroName, remaining);
        }
    }

    public void addItemToCollection(String itemId, int quantity) {
        if (itemId == null || quantity <= 0) return;
        if (this.ownedItems == null) this.ownedItems = new java.util.HashMap<>();
        this.ownedItems.put(itemId, this.ownedItems.getOrDefault(itemId, 0) + quantity);
    }
}

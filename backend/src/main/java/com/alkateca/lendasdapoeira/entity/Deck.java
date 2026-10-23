package com.alkateca.lendasdapoeira.entity;

import lombok.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Deck {

    private UUID uuid;
    private String deckName;

    private List<HeroCard> heroList = new ArrayList<>();
    private List<Card> cardList = new ArrayList<>();
    private List<Card> liberationList = new ArrayList<>();

    public Deck(String deckId, List<Card> deckList, List<HeroCard> heroList) {
        this.uuid = UUID.randomUUID();
        this.deckName = deckId;
        this.cardList = deckList != null ? deckList : new ArrayList<>();
        this.heroList = heroList != null ? heroList : new ArrayList<>();
        this.liberationList = new ArrayList<>();
    }

    public Deck(String deckId, List<Card> deckList, List<HeroCard> heroList, List<Card> liberationList) {
        this.uuid = UUID.randomUUID();
        this.deckName = deckId;
        this.cardList = deckList != null ? deckList : new ArrayList<>();
        this.heroList = heroList != null ? heroList : new ArrayList<>();
        this.liberationList = liberationList != null ? liberationList : new ArrayList<>();
    }

    public void shuffleDeck() {
        Collections.shuffle(cardList);
    }
}

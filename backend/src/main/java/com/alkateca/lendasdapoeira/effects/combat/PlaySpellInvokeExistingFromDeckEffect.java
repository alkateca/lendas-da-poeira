package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.UUID;

public class PlaySpellInvokeExistingFromDeckEffect implements Effect {

    private final String cardName;

    public PlaySpellInvokeExistingFromDeckEffect(String cardName) {
        this.cardName = cardName;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();

        Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (myPlayer == null || myPlayer.getCurrentDeck() == null || myPlayer.getCurrentDeck().getCardList() == null) {
            return;
        }

        boolean hasCopyInDeck = myPlayer.getCurrentDeck().getCardList().stream()
                .anyMatch(c -> c.getZoneId() == ZoneId.DECK && cardName.equalsIgnoreCase(c.getCardName()));

        if (!hasCopyInDeck) {
            System.out.println("-> Nenhuma cópia restante de " + cardName + " no baralho (DECK) para invocar.");
            return;
        }

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        System.out.println("-> Invocando cópia existente do baralho: " + cardName + "!");
        gameEngine.invokeCard(myPlayer, sourceCard, myHero, cardName);
    }
}

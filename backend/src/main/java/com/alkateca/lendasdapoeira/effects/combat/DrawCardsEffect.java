package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

public class DrawCardsEffect implements Effect {

    private final int cardsToDraw;

    public DrawCardsEffect(int cardsToDraw) {
        this.cardsToDraw = cardsToDraw;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (myPlayer == null || myPlayer.getCurrentDeck() == null || myPlayer.getCurrentDeck().getCardList() == null) {
            return;
        }

        int drawn = 0;
        while (drawn < cardsToDraw) {
            Card cardToDraw = myPlayer.getCurrentDeck().getCardList().stream()
                    .filter(c -> c.getZoneId() == ZoneId.DECK)
                    .findFirst()
                    .orElse(null);

            if (cardToDraw != null) {
                cardToDraw.setZoneId(ZoneId.HAND);
                drawn++;
            } else {
                break;
            }
        }

        System.out.println("-> " + myPlayer.getName() + " comprou " + drawn + " carta(s)!");
    }
}

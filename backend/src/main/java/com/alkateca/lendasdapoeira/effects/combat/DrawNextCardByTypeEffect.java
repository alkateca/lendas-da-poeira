package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

public class DrawNextCardByTypeEffect implements Effect {

    private final ItemType typeToDraw;

    public DrawNextCardByTypeEffect(ItemType typeToDraw) {
        this.typeToDraw = typeToDraw;
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

        Card cardToDraw = myPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.DECK && c.getItemType() == typeToDraw)
                .findFirst()
                .orElse(null);

        if (cardToDraw != null) {
            cardToDraw.setZoneId(ZoneId.HAND);
            gameEngine.recordExtraCardDraw(myPlayer.getUuid(), 1);
            System.out.println("-> " + myPlayer.getName() + " adicionou " + cardToDraw.getCardName() +
                    " (" + typeToDraw.name() + ") do baralho para a mão!");
        } else {
            System.out.println("-> Nenhuma carta do tipo " + typeToDraw.name() + " encontrada no baralho.");
        }
    }
}

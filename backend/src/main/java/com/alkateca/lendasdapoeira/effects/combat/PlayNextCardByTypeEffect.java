package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

public class PlayNextCardByTypeEffect implements Effect {

    private final ItemType typeToPlay;

    public PlayNextCardByTypeEffect(ItemType typeToPlay) {
        this.typeToPlay = typeToPlay;
    }

    public ItemType getTypeToPlay() {
        return typeToPlay;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId() != null
                && sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        Player ownerPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (ownerPlayer == null || ownerPlayer.getCurrentDeck() == null
                || ownerPlayer.getCurrentDeck().getCardList() == null) {
            return;
        }

        Card nextCard = ownerPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c != null
                        && c.getZoneId() == ZoneId.DECK
                        && c.getItemType() == typeToPlay)
                .findFirst()
                .orElse(null);

        if (nextCard != null) {
            System.out.println("-> " + nextCard.getCardName() + " (" + typeToPlay
                    + ") foi encontrada no baralho e colocada em resolução!");
            gameEngine.playCardIntoResolution(nextCard);
        } else {
            System.out.println("-> Nenhuma carta do tipo " + typeToPlay + " foi encontrada no baralho.");
        }
    }
}

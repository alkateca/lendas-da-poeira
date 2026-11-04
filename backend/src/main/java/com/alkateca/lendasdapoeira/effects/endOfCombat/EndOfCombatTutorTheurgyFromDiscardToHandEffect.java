package com.alkateca.lendasdapoeira.effects.endOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

public class EndOfCombatTutorTheurgyFromDiscardToHandEffect implements Effect {

    public EndOfCombatTutorTheurgyFromDiscardToHandEffect() {
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_END;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId() != null
                && sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        com.alkateca.lendasdapoeira.entity.Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (myPlayer == null || myPlayer.getCurrentDeck() == null
                || myPlayer.getCurrentDeck().getCardList() == null) {
            return;
        }

        Card theurgyCard = myPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c != null
                        && c.getZoneId() == ZoneId.DISCARD
                        && c.getCardType() == CardType.THEURGY)
                .findFirst()
                .orElse(null);

        if (theurgyCard != null) {
            theurgyCard.setZoneId(ZoneId.HAND);
            System.out.println("-> A Teurgia " + theurgyCard.getCardName() +
                    " retornou do descarte para a mão de " + myPlayer.getName() + "!");
        } else {
            System.out.println("-> Nenhuma carta do tipo THEURGY encontrada no descarte de " + myPlayer.getName() + ".");
        }
    }
}

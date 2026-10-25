package com.alkateca.lendasdapoeira.effects.endOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.List;
import java.util.stream.Collectors;

public class EndOfCombatReturnAttachedCardsToDeckEffect implements Effect {
    private final List<ItemType> itemTypesToReturn;

    public EndOfCombatReturnAttachedCardsToDeckEffect(List<ItemType> itemTypesToReturn) {
        this.itemTypesToReturn = itemTypesToReturn;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_END;
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

        List<Card> ownerPlayedCards = isPlayer1
                ? gameEngine.getGameState().getPlayer1PlayedCards()
                : gameEngine.getGameState().getPlayer2PlayedCards();

        List<Card> attachedToReturn = myPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getItemType() != null && itemTypesToReturn.contains(c.getItemType()))
                .filter(c -> (c.getZoneId() == ZoneId.ATTACHED && sourceCard.getUuid().equals(c.getAttachedToCardId()))
                          || (ownerPlayedCards != null && ownerPlayedCards.contains(c)))
                .collect(Collectors.toList());

        for (Card card : attachedToReturn) {
            if (ownerPlayedCards != null) {
                ownerPlayedCards.remove(card);
            }
            // Garante que o vínculo esteja apontando para Asahum antes do detach reverter os buffs (onDetach)
            if (card.getAttachedToCardId() == null) {
                card.setAttachedToCardId(sourceCard.getUuid());
                card.setZoneId(ZoneId.ATTACHED);
            }
            gameEngine.detachCard(card, ZoneId.DECK);
            System.out.println("-> " + card.getCardName() + " retornou para o baralho de " + myPlayer.getName() + "!");
        }

        if (!attachedToReturn.isEmpty()) {
            myPlayer.getCurrentDeck().shuffleDeck();
        }
    }
}

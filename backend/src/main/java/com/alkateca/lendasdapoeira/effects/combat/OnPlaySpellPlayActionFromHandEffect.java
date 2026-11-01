package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.List;
import java.util.UUID;

public class OnPlaySpellPlayActionFromHandEffect implements Effect {

    public OnPlaySpellPlayActionFromHandEffect() {
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

        if (myPlayer == null || myPlayer.getCurrentDeck() == null
                || myPlayer.getCurrentDeck().getCardList() == null) {
            return;
        }

        Card recentlyPlayedSpell = findRecentlyResolvedSpell(gameEngine, myPlayer.getUuid(), sourceCard, isPlayer1);
        if (recentlyPlayedSpell == null || recentlyPlayedSpell.getCardType() != CardType.SPELL) {
            return;
        }

        List<Card> actionsInHand = myPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c != null
                        && c.getZoneId() == ZoneId.HAND
                        && c.getCardType() == CardType.ACTION)
                .toList();

        if (actionsInHand.isEmpty()) {
            System.out.println("-> Nenhuma carta de Ação disponível na mão de " + myPlayer.getName() + ".");
            return;
        }

        List<UUID> validTargetUuids = actionsInHand.stream()
                .map(Card::getUuid)
                .toList();

        gameEngine.requestPlayerChoice(
                myPlayer.getUuid(),
                sourceCard,
                "HAND_ACTION",
                "Escolha 1 carta de Ação da sua mão para jogar:",
                validTargetUuids,
                true,
                chosenCardUuid -> {
                    if (chosenCardUuid == null) return;
                    Card chosenAction = myPlayer.getCurrentDeck().getCardList().stream()
                            .filter(c -> chosenCardUuid.equals(c.getUuid()) && c.getZoneId() == ZoneId.HAND)
                            .findFirst()
                            .orElse(null);

                    if (chosenAction != null) {
                        System.out.println("-> " + chosenAction.getCardName()
                                + " foi jogada da mão para a fila de resolução!");
                        gameEngine.playCardIntoResolution(chosenAction);
                    }
                }
        );
    }

    private Card findRecentlyResolvedSpell(GameEngine gameEngine, UUID ownerId, Card sourceCard, boolean isPlayer1) {
        List<Card> resolvedCards = gameEngine.getResolvedCardsInCurrentCombat();
        if (resolvedCards != null && !resolvedCards.isEmpty()) {
            for (int i = resolvedCards.size() - 1; i >= 0; i--) {
                Card c = resolvedCards.get(i);
                if (c != null && ownerId.equals(c.getOwnerId()) && !c.getUuid().equals(sourceCard.getUuid())) {
                    return c;
                }
            }
        }
        if (gameEngine.getGameState() != null) {
            List<Card> playedCards = isPlayer1
                    ? gameEngine.getGameState().getPlayer1PlayedCards()
                    : gameEngine.getGameState().getPlayer2PlayedCards();
            if (playedCards != null) {
                for (int i = playedCards.size() - 1; i >= 0; i--) {
                    Card c = playedCards.get(i);
                    if (c != null && !c.getUuid().equals(sourceCard.getUuid()) && c.getCardType() == CardType.SPELL) {
                        return c;
                    }
                }
            }
        }
        return null;
    }
}

package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlaySpellChooseAndDiscardEnemyItemEffect implements Effect {

    public PlaySpellChooseAndDiscardEnemyItemEffect() {
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        Player myPlayer = isPlayer1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();

        UUID enemyHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                : gameEngine.getGameState().getActiveHeroPlayer1Id();

        if (myPlayer == null || enemyHeroId == null) {
            return;
        }

        List<Card> allCards = new ArrayList<>();
        if (gameEngine.getPlayer1() != null && gameEngine.getPlayer1().getCurrentDeck() != null
                && gameEngine.getPlayer1().getCurrentDeck().getCardList() != null) {
            allCards.addAll(gameEngine.getPlayer1().getCurrentDeck().getCardList());
        }
        if (gameEngine.getPlayer2() != null && gameEngine.getPlayer2().getCurrentDeck() != null
                && gameEngine.getPlayer2().getCurrentDeck().getCardList() != null) {
            allCards.addAll(gameEngine.getPlayer2().getCurrentDeck().getCardList());
        }

        List<UUID> validItemUuids = allCards.stream()
                .filter(c -> c.getZoneId() == ZoneId.ATTACHED
                        && enemyHeroId.equals(c.getAttachedToCardId())
                        && c.getCardType() == CardType.ITEM)
                .map(Card::getUuid)
                .collect(Collectors.toList());

        gameEngine.requestPlayerChoice(
                myPlayer.getUuid(),
                sourceCard,
                "ATTACHED",
                "Escolha 1 Item equipado no Herói Inimigo para descartar",
                validItemUuids,
                false,
                chosenItemUuid -> {
                    if (chosenItemUuid == null) return;
                    Card chosenItem = allCards.stream()
                            .filter(c -> c.getUuid().equals(chosenItemUuid)
                                    && c.getZoneId() == ZoneId.ATTACHED
                                    && enemyHeroId.equals(c.getAttachedToCardId()))
                            .findFirst()
                            .orElse(null);

                    if (chosenItem != null) {
                        gameEngine.detachCard(chosenItem, ZoneId.DISCARD);
                        System.out.println("-> Item " + chosenItem.getCardName() +
                                " foi descartado do Herói Inimigo por " + sourceCard.getCardName() + "!");
                    }
                }
        );
    }
}

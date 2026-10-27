package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayActionUnequipEnemyItemConditionalWoundedEffect implements Effect {

    private final int baseUnequipCount;
    private final int woundedUnequipCount;

    public PlayActionUnequipEnemyItemConditionalWoundedEffect(int baseUnequipCount, int woundedUnequipCount) {
        this.baseUnequipCount = baseUnequipCount;
        this.woundedUnequipCount = woundedUnequipCount;
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
        Player enemyPlayer = isPlayer1 ? gameEngine.getPlayer2() : gameEngine.getPlayer1();

        UUID enemyHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                : gameEngine.getGameState().getActiveHeroPlayer1Id();

        if (myPlayer == null || enemyPlayer == null || enemyHeroId == null) {
            return;
        }

        HeroCard enemyHero = (HeroCard) enemyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(enemyHeroId))
                .findFirst()
                .orElse(null);

        if (enemyHero == null) {
            return;
        }

        boolean isWounded = enemyHero.getVidaAtual() < enemyHero.getVidaMaxima();
        int maxToUnequip = isWounded ? woundedUnequipCount : baseUnequipCount;

        requestNextUnequip(gameEngine, sourceCard, myPlayer, enemyHero, maxToUnequip, maxToUnequip);
    }

    private void requestNextUnequip(
            GameEngine gameEngine,
            Card sourceCard,
            Player myPlayer,
            HeroCard enemyHero,
            int remaining,
            int totalAllowed) {
        if (remaining <= 0) {
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

        List<UUID> attachedItemUuids = allCards.stream()
                .filter(c -> c.getZoneId() == ZoneId.ATTACHED
                        && enemyHero.getUuid().equals(c.getAttachedToCardId())
                        && c.getCardType() == CardType.ITEM)
                .map(Card::getUuid)
                .collect(Collectors.toList());

        if (attachedItemUuids.isEmpty()) {
            if (remaining == totalAllowed) {
                System.out.println("-> O Herói Inimigo não possui Itens equipados para desequipar.");
            }
            return;
        }

        boolean canSkip = remaining < totalAllowed;
        gameEngine.requestPlayerChoice(
                myPlayer.getUuid(),
                sourceCard,
                "ATTACHED",
                "Escolha 1 Item equipado no Herói Inimigo para desequipar (Restantes: " + remaining + ")",
                attachedItemUuids,
                canSkip,
                chosenCardUuid -> {
                    if (chosenCardUuid == null) return;

                    Card chosenCard = allCards.stream()
                            .filter(c -> c.getUuid().equals(chosenCardUuid)
                                    && c.getZoneId() == ZoneId.ATTACHED
                                    && enemyHero.getUuid().equals(c.getAttachedToCardId()))
                            .findFirst()
                            .orElse(null);

                    if (chosenCard != null) {
                        gameEngine.detachCard(chosenCard, ZoneId.DISCARD);
                        System.out.println("-> " + chosenCard.getCardName() + " foi desequipada de " +
                                enemyHero.getCardName() + " por " + sourceCard.getCardName() + "!");

                        if (remaining - 1 > 0) {
                            requestNextUnequip(gameEngine, sourceCard, myPlayer, enemyHero, remaining - 1, totalAllowed);
                        }
                    }
                }
        );
    }
}

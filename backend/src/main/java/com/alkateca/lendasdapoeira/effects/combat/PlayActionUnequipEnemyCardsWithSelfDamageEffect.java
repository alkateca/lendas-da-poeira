package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayActionUnequipEnemyCardsWithSelfDamageEffect implements Effect {

    private final int maxCardsToUnequip;
    private final int selfDamagePerCard;

    public PlayActionUnequipEnemyCardsWithSelfDamageEffect(int maxCardsToUnequip, int selfDamagePerCard) {
        this.maxCardsToUnequip = maxCardsToUnequip;
        this.selfDamagePerCard = selfDamagePerCard;
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

        UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();
        UUID enemyHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                : gameEngine.getGameState().getActiveHeroPlayer1Id();

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        HeroCard enemyHero = (HeroCard) enemyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(enemyHeroId))
                .findFirst()
                .orElse(null);

        if (myHero == null || enemyHero == null) {
            return;
        }

        requestNextUnequip(gameEngine, sourceCard, myPlayer, myHero, enemyHero, maxCardsToUnequip);
    }

    private void requestNextUnequip(
            GameEngine gameEngine,
            Card sourceCard,
            Player myPlayer,
            HeroCard myHero,
            HeroCard enemyHero,
            int remaining) {
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

        List<UUID> attachedEnemyUuids = allCards.stream()
                .filter(c -> c.getZoneId() == ZoneId.ATTACHED && enemyHero.getUuid().equals(c.getAttachedToCardId()))
                .map(Card::getUuid)
                .collect(Collectors.toList());

        if (attachedEnemyUuids.isEmpty()) {
            System.out.println("-> O Herói Inimigo não possui cartas equipadas para desequipar.");
            return;
        }

        gameEngine.requestPlayerChoice(
                myPlayer.getUuid(),
                sourceCard,
                "ATTACHED",
                "Escolha até " + remaining + " carta(s) equipada(s) no Herói Inimigo para desequipar",
                attachedEnemyUuids,
                true,
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
                        myHero.setVidaAtual(myHero.getVidaAtual() - selfDamagePerCard);
                        System.out.println("-> " + chosenCard.getCardName() + " foi desequipada de " + enemyHero.getCardName() +
                                "! " + myHero.getCardName() + " sofreu " + selfDamagePerCard +
                                " de Dano Direto (Vida atual: " + myHero.getVidaAtual() + ").");

                        if (remaining - 1 > 0) {
                            requestNextUnequip(gameEngine, sourceCard, myPlayer, myHero, enemyHero, remaining - 1);
                        }
                    }
                }
        );
    }
}

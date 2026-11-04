package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.List;
import java.util.UUID;

public class OnPlaySpellHealHeroEffect implements Effect {

    private final int healAmount;

    public OnPlaySpellHealHeroEffect(int healAmount) {
        this.healAmount = healAmount;
    }

    public int getHealAmount() {
        return healAmount;
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

        if (ownerPlayer == null) {
            return;
        }

        Card recentlyResolvedSpell = findRecentlyResolvedSpell(gameEngine, ownerPlayer.getUuid(), sourceCard, isPlayer1);
        if (recentlyResolvedSpell == null || recentlyResolvedSpell.getCardType() != CardType.SPELL) {
            return;
        }

        UUID targetHeroId = sourceCard.getAttachedToCardId();
        if (targetHeroId == null && gameEngine.getGameState() != null) {
            targetHeroId = isPlayer1
                    ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                    : gameEngine.getGameState().getActiveHeroPlayer2Id();
        }

        HeroCard targetHero = gameEngine.findHeroById(targetHeroId);
        if (targetHero != null && Boolean.TRUE.equals(targetHero.getEstaVivo())) {
            int oldHp = targetHero.getVidaAtual();
            int newHp = Math.min(oldHp + healAmount, targetHero.getVidaMaxima());
            int recovered = Math.max(0, newHp - oldHp);
            targetHero.setVidaAtual(newHp);

            System.out.println("-> " + targetHero.getCardName() + " recuperou " + recovered
                    + " pontos de vida pela conjuração de " + recentlyResolvedSpell.getCardName()
                    + "! Vida restante: " + targetHero.getVidaAtual());
        }
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

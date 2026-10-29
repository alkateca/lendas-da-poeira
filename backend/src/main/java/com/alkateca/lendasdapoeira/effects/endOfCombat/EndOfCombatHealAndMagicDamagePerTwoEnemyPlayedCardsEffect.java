package com.alkateca.lendasdapoeira.effects.endOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.UUID;

public class EndOfCombatHealAndMagicDamagePerTwoEnemyPlayedCardsEffect implements Effect {

    private final int healPerPair;
    private final int magicDamagePerPair;

    public EndOfCombatHealAndMagicDamagePerTwoEnemyPlayedCardsEffect(int healPerPair, int magicDamagePerPair) {
        this.healPerPair = healPerPair;
        this.magicDamagePerPair = magicDamagePerPair;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_END;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();
        UUID enemyHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                : gameEngine.getGameState().getActiveHeroPlayer1Id();

        Player myPlayer = isPlayer1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();
        Player enemyPlayer = isPlayer1 ? gameEngine.getPlayer2() : gameEngine.getPlayer1();

        if (myPlayer == null || enemyPlayer == null) {
            return;
        }

        int enemyPlayedCount = gameEngine.getCardsPlayedByPlayerInCurrentTurn(enemyPlayer.getUuid());
        int pairs = enemyPlayedCount / 2;

        if (pairs <= 0) {
            System.out.println("-> Oponente jogou apenas " + enemyPlayedCount + " carta(s) neste turno (menos de 2).");
            return;
        }

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        HeroCard enemyHero = (HeroCard) enemyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(enemyHeroId))
                .findFirst()
                .orElse(null);

        if (myHero != null && Boolean.TRUE.equals(myHero.getEstaVivo())) {
            int totalHeal = healPerPair * pairs;
            int novaVida = Math.min(myHero.getVidaMaxima(), myHero.getVidaAtual() + totalHeal);
            myHero.setVidaAtual(novaVida);
            System.out.println("-> " + myHero.getCardName() + " recuperou " + totalHeal +
                    " de Vida (" + enemyPlayedCount + " cartas jogadas pelo oponente)! Vida restante: " + myHero.getVidaAtual());
        }

        if (enemyHero != null) {
            int rawMagicDamage = magicDamagePerPair * pairs;
            int totalDamage = gameEngine.calculateMagicDamage(myHero, enemyHero, rawMagicDamage);
            enemyHero.setVidaAtual(enemyHero.getVidaAtual() - totalDamage);
            gameEngine.evaluateInstantDeathByAura();
            System.out.println("-> " + enemyHero.getCardName() + " sofreu " + totalDamage +
                    " de dano mágico! Vida restante: " + enemyHero.getVidaAtual());
        }
    }
}

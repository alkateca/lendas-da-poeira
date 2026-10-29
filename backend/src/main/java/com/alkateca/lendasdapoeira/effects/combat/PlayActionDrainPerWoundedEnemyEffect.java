package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.UUID;

public class PlayActionDrainPerWoundedEnemyEffect implements Effect {

    private final int healPerWounded;
    private final int directDamagePerWounded;

    public PlayActionDrainPerWoundedEnemyEffect(int healPerWounded, int directDamagePerWounded) {
        this.healPerWounded = healPerWounded;
        this.directDamagePerWounded = directDamagePerWounded;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
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

        if (myPlayer == null || enemyPlayer == null || enemyPlayer.getCurrentDeck() == null
                || enemyPlayer.getCurrentDeck().getHeroList() == null) {
            return;
        }

        int woundedCount = (int) enemyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(hero -> hero != null
                        && Boolean.TRUE.equals(hero.getEstaVivo())
                        && hero.getVidaAtual() < hero.getVidaMaxima())
                .count();

        if (woundedCount <= 0) {
            System.out.println("-> Nenhum Herói Inimigo Ferido em campo.");
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
            int totalHeal = healPerWounded * woundedCount;
            int novaVida = Math.min(myHero.getVidaMaxima(), myHero.getVidaAtual() + totalHeal);
            myHero.setVidaAtual(novaVida);
            System.out.println("-> " + myHero.getCardName() + " recuperou " + totalHeal +
                    " de Vida (" + woundedCount + " inimigo(s) Ferido(s))! Vida restante: " + myHero.getVidaAtual());
        }

        if (enemyHero != null) {
            int totalDamage = gameEngine.calculateDirectDamage(myHero, enemyHero, directDamagePerWounded * woundedCount);
            enemyHero.setVidaAtual(enemyHero.getVidaAtual() - totalDamage);
            gameEngine.evaluateInstantDeathByAura();
            System.out.println("-> " + enemyHero.getCardName() + " sofreu " + totalDamage +
                    " de Dano Direto! Vida restante: " + enemyHero.getVidaAtual());
        }
    }
}

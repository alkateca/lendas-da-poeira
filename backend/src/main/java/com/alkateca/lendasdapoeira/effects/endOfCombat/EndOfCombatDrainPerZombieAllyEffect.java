package com.alkateca.lendasdapoeira.effects.endOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.Raca;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.UUID;

public class EndOfCombatDrainPerZombieAllyEffect implements Effect {

    private final int healPerZombie;
    private final int directDamagePerZombie;

    public EndOfCombatDrainPerZombieAllyEffect(int healPerZombie, int directDamagePerZombie) {
        this.healPerZombie = healPerZombie;
        this.directDamagePerZombie = directDamagePerZombie;
    }

    public int getHealPerZombie() {
        return healPerZombie;
    }

    public int getDirectDamagePerZombie() {
        return directDamagePerZombie;
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

        UUID enemyHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                : gameEngine.getGameState().getActiveHeroPlayer1Id();

        Player enemyPlayer = isPlayer1
                ? gameEngine.getPlayer2()
                : gameEngine.getPlayer1();

        HeroCard enemyHero = (HeroCard) enemyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(enemyHeroId))
                .findFirst()
                .orElse(null);

        UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();

        Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        int zombieCount = (int) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h != null
                        && Boolean.TRUE.equals(h.getEstaVivo())
                        && h.getVidaAtual() > 0
                        && isZombieHero(h))
                .count();

        if (zombieCount <= 0) {
            System.out.println("-> Nenhum herói aliado Zumbi vivo. Nenhum efeito aplicado.");
            return;
        }

        if (myHero != null && Boolean.TRUE.equals(myHero.getEstaVivo())) {
            int totalHeal = zombieCount * healPerZombie;
            int oldHp = myHero.getVidaAtual();
            int newHp = Math.min(oldHp + totalHeal, myHero.getVidaMaxima());
            int recovered = Math.max(0, newHp - oldHp);
            myHero.setVidaAtual(newHp);

            System.out.println("-> " + myHero.getCardName() + " recuperou " + recovered
                    + " de vida (" + zombieCount + " aliado(s) Zumbi)! Vida restante: " + myHero.getVidaAtual());
        }

        if (enemyHero != null) {
            int baseDirectDamage = zombieCount * directDamagePerZombie;
            int finalDamage = gameEngine.calculateDirectDamage(myHero, enemyHero, baseDirectDamage);
            enemyHero.setVidaAtual(enemyHero.getVidaAtual() - finalDamage);

            System.out.println("-> " + enemyHero.getCardName() + " sofreu " + finalDamage
                    + " de dano direto (" + zombieCount + " aliado(s) Zumbi)! Vida restante: " + enemyHero.getVidaAtual());
        }
    }

    private boolean isZombieHero(HeroCard hero) {
        return hero.getOrdem() != null && hero.getOrdem().stream()
                .anyMatch(o -> o != null && o.name().equalsIgnoreCase("ZUMBI"));
    }
}

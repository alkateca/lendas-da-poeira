package com.alkateca.lendasdapoeira.effects.startOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.UUID;

public class StartOfCombatBuffAttackIfEnemyWoundedEffect implements Effect {

    private final int attackBonus;
    private int appliedBonus = 0;

    public StartOfCombatBuffAttackIfEnemyWoundedEffect(int attackBonus) {
        this.attackBonus = attackBonus;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_START;
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

        if (myPlayer == null || enemyPlayer == null || myHeroId == null || enemyHeroId == null) {
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

        if (myHero != null && enemyHero != null && enemyHero.getVidaAtual() < enemyHero.getVidaMaxima()) {
            this.appliedBonus = attackBonus;
            myHero.setAtaque(myHero.getAtaque() + attackBonus);
            System.out.println("-> " + myHero.getCardName() + " recebeu +" + attackBonus +
                    " de Ataque até o Final do Combate pois " + enemyHero.getCardName() + " está Ferido!");

            gameEngine.registerCombatEndCleanup(() -> {
                if (this.appliedBonus > 0) {
                    myHero.setAtaque(myHero.getAtaque() - this.appliedBonus);
                    this.appliedBonus = 0;
                }
            });
        }
    }

    @Override
    public void onDetach(GameEngine gameEngine, Card sourceCard, HeroCard targetHero) {
        if (targetHero != null && this.appliedBonus > 0) {
            targetHero.setAtaque(targetHero.getAtaque() - this.appliedBonus);
            this.appliedBonus = 0;
        }
    }
}

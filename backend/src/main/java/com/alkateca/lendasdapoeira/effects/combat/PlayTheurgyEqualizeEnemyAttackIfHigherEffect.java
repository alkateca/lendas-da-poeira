package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class PlayTheurgyEqualizeEnemyAttackIfHigherEffect implements Effect {

    public PlayTheurgyEqualizeEnemyAttackIfHigherEffect() {
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        java.util.UUID enemyHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                : gameEngine.getGameState().getActiveHeroPlayer1Id();

        com.alkateca.lendasdapoeira.entity.Player enemyPlayer = isPlayer1
                ? gameEngine.getPlayer2()
                : gameEngine.getPlayer1();

        HeroCard enemyHero = (HeroCard) enemyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(enemyHeroId))
                .findFirst()
                .orElse(null);

        java.util.UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();

        com.alkateca.lendasdapoeira.entity.Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        if (myHero != null && enemyHero != null && enemyHero.getAtaque() > myHero.getAtaque()) {
            int originalEnemyAttack = enemyHero.getAtaque();
            int targetAttack = myHero.getAtaque();
            int attackReduction = originalEnemyAttack - targetAttack;

            enemyHero.setAtaque(targetAttack);

            gameEngine.registerCombatEndCleanup(() -> {
                enemyHero.setAtaque(enemyHero.getAtaque() + attackReduction);
            });

            System.out.println("-> O Ataque de " + enemyHero.getCardName() + " foi igualado para " +
                    targetAttack + " (-" + attackReduction + " ATQ até o final do combate)! Vida restante: " +
                    enemyHero.getVidaAtual());
        } else {
            System.out.println("-> O Ataque do Herói Inimigo não é superior ao do seu Herói. Efeito ignorado.");
        }
    }
}

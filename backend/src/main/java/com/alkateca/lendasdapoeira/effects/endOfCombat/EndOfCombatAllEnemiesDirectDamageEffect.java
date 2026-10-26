package com.alkateca.lendasdapoeira.effects.endOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class EndOfCombatAllEnemiesDirectDamageEffect implements Effect {

    private final int directDamage;
    private final boolean allEnemies;

    public EndOfCombatAllEnemiesDirectDamageEffect(int directDamage) {
        this(directDamage, true);
    }

    public EndOfCombatAllEnemiesDirectDamageEffect(int directDamage, boolean allEnemies) {
        this.directDamage = directDamage;
        this.allEnemies = allEnemies;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_END;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");
        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        Player enemyPlayer = isPlayer1 ? gameEngine.getPlayer2() : gameEngine.getPlayer1();
        java.util.UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();
        HeroCard myHero = gameEngine.findHeroById(myHeroId);

        if (enemyPlayer == null || enemyPlayer.getCurrentDeck() == null || enemyPlayer.getCurrentDeck().getHeroList() == null) {
            return;
        }

        if (!allEnemies) {
            java.util.UUID activeEnemyHeroId = isPlayer1
                    ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                    : gameEngine.getGameState().getActiveHeroPlayer1Id();
            HeroCard activeEnemy = gameEngine.findHeroById(activeEnemyHeroId);
            if (activeEnemy != null) {
                int damage = gameEngine.calculateDirectDamage(myHero, activeEnemy, directDamage);
                activeEnemy.setVidaAtual(activeEnemy.getVidaAtual() - damage);
                System.out.println("-> " + activeEnemy.getCardName() + " sofreu " + damage +
                        " de Dano Direto! Vida restante: " + activeEnemy.getVidaAtual());
            }
            return;
        }

        for (Card c : enemyPlayer.getCurrentDeck().getHeroList()) {
            if (c instanceof HeroCard enemyHero && Boolean.TRUE.equals(enemyHero.getEstaVivo())) {
                int damage = gameEngine.calculateDirectDamage(myHero, enemyHero, directDamage);
                enemyHero.setVidaAtual(enemyHero.getVidaAtual() - damage);
                System.out.println("-> " + enemyHero.getCardName() + " sofreu " + damage +
                        " de Dano Direto! Vida restante: " + enemyHero.getVidaAtual());
            }
        }
    }
}

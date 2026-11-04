package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class PlaySpellSetEnemySpiritExactEffect implements Effect {

    private final int targetSpirit;

    public PlaySpellSetEnemySpiritExactEffect(int targetSpirit) {
        this.targetSpirit = targetSpirit;
    }

    public int getTargetSpirit() {
        return targetSpirit;
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

        if (enemyHero != null) {
            int originalSpirit = enemyHero.getEspirito();
            int delta = targetSpirit - originalSpirit;
            enemyHero.setEspirito(targetSpirit);

            gameEngine.registerCombatEndCleanup(() -> {
                enemyHero.setEspirito(enemyHero.getEspirito() - delta);
            });

            System.out.println("-> O Espírito de " + enemyHero.getCardName() + " foi definido para "
                    + targetSpirit + " até o final do combate (Espírito anterior: " + originalSpirit
                    + ")! Vida restante: " + enemyHero.getVidaAtual());
        }
    }
}

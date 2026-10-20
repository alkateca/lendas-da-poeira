package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class PlaySpellSetEnemyDefenseToZeroEffect implements Effect {

    private HeroCard targetHero;
    private int removedDefense = 0;
    private boolean activeDebuff = false;

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        if (gameEngine.getTurnPhase() == TurnPhase.COMBAT_END) {
            revertDebuff();
            return;
        }

        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");
        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        java.util.UUID enemyHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                : gameEngine.getGameState().getActiveHeroPlayer1Id();
        com.alkateca.lendasdapoeira.entity.Player enemyPlayer = isPlayer1
                ? gameEngine.getPlayer2()
                : gameEngine.getPlayer1();

        HeroCard enemyHero = (HeroCard) enemyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(enemyHeroId)).findFirst().orElse(null);

        if (enemyHero != null) {
            this.removedDefense = enemyHero.getDefesa();
            this.targetHero = enemyHero;
            this.activeDebuff = true;
            enemyHero.setDefesa(0);
            gameEngine.registerCombatEndCleanup(this::revertDebuff);
            System.out.println("-> Defesa de " + enemyHero.getCardName() + " foi zerada até o Final do Combate!");
        }
    }

    private void revertDebuff() {
        if (activeDebuff && targetHero != null) {
            targetHero.setDefesa(targetHero.getDefesa() + removedDefense);
            System.out.println("-> Defesa de " + targetHero.getCardName() + " restaurada no Final do Combate: " + targetHero.getDefesa());
            activeDebuff = false;
            removedDefense = 0;
            targetHero = null;
        }
    }
}

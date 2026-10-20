package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class PlaySpellReduceEnemyDefenseByTheirSpiritEffect implements Effect {

    private HeroCard targetHero;
    private int appliedReduction = 0;
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
            int reduction = enemyHero.getEspirito();
            int newDef = enemyHero.getDefesa() - reduction;
            enemyHero.setDefesa(newDef);
            this.targetHero = enemyHero;
            this.appliedReduction = reduction;
            this.activeDebuff = true;
            gameEngine.registerCombatEndCleanup(this::revertDebuff);
            System.out.println("-> A Defesa de " + enemyHero.getCardName() + " foi reduzida em " + reduction + " até o Final do Combate!");
        }
    }

    private void revertDebuff() {
        if (activeDebuff && targetHero != null) {
            targetHero.setDefesa(targetHero.getDefesa() + appliedReduction);
            System.out.println("-> Redução de Defesa de " + targetHero.getCardName() + " expirou no Final do Combate.");
            activeDebuff = false;
            appliedReduction = 0;
            targetHero = null;
        }
    }
}

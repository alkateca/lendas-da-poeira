package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class PlaySpellSetEnemySpiritEffect implements Effect {

    private HeroCard targetHero;
    private int spiritDelta = 0;
    private boolean activeModification = false;

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        if (gameEngine.getTurnPhase() == TurnPhase.COMBAT_END) {
            revertModification();
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
            this.spiritDelta = 2 - enemyHero.getEspirito();
            this.targetHero = enemyHero;
            this.activeModification = true;
            enemyHero.setEspirito(2);
            gameEngine.registerCombatEndCleanup(this::revertModification);
            System.out.println("-> Espírito de " + enemyHero.getCardName() + " foi cravado em 2 até o Final do Combate!");
        }
    }

    private void revertModification() {
        if (activeModification && targetHero != null) {
            targetHero.setEspirito(targetHero.getEspirito() - spiritDelta);
            System.out.println("-> Espírito de " + targetHero.getCardName() + " restaurado no Final do Combate: " + targetHero.getEspirito());
            activeModification = false;
            spiritDelta = 0;
            targetHero = null;
        }
    }
}

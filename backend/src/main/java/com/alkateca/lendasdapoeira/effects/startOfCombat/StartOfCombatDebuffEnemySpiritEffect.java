package com.alkateca.lendasdapoeira.effects.startOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class StartOfCombatDebuffEnemySpiritEffect implements Effect {

    private final int spiritDebuff;
    private HeroCard debuffedHero;
    private boolean activeDebuff = false;

    public StartOfCombatDebuffEnemySpiritEffect(int spiritDebuff) {
        this.spiritDebuff = spiritDebuff;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_START;
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
            enemyHero.setEspirito(enemyHero.getEspirito() - spiritDebuff);
            this.debuffedHero = enemyHero;
            this.activeDebuff = true;
            gameEngine.registerCombatEndCleanup(this::revertDebuff);
            System.out.println("-> O Espírito de " + enemyHero.getCardName() + " foi reduzido em " + spiritDebuff + " até o Final do Combate!");
        }
    }

    private void revertDebuff() {
        if (activeDebuff && debuffedHero != null) {
            debuffedHero.setEspirito(debuffedHero.getEspirito() + spiritDebuff);
            System.out.println("-> Debuff de Espírito (-" + spiritDebuff + ") de " + debuffedHero.getCardName() + " expirou no Final do Combate.");
            activeDebuff = false;
            debuffedHero = null;
        }
    }
}

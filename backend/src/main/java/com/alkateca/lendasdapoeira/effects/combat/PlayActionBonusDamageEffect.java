package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class PlayActionBonusDamageEffect implements Effect {

    private final int bonusDano;
    private HeroCard buffedHero;
    private boolean activeBuff = false;

    public PlayActionBonusDamageEffect(int bonusDano) {
        this.bonusDano = bonusDano;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        if (gameEngine.getTurnPhase() == TurnPhase.COMBAT_END) {
            revertBuff();
            return;
        }

        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");
        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        java.util.UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();
        com.alkateca.lendasdapoeira.entity.Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId)).findFirst().orElse(null);

        if (myHero != null) {
            myHero.setDanoBonus(myHero.getDanoBonus() + bonusDano);
            this.buffedHero = myHero;
            this.activeBuff = true;
            gameEngine.registerCombatEndCleanup(this::revertBuff);
            System.out.println("-> " + myHero.getCardName() + " recebeu +" + bonusDano + " de Dano Bônus até o Final do Combate!");
        }
    }

    private void revertBuff() {
        if (activeBuff && buffedHero != null) {
            buffedHero.setDanoBonus(buffedHero.getDanoBonus() - bonusDano);
            System.out.println("-> Dano Bônus temporário (-" + bonusDano + ") de " + buffedHero.getCardName() + " expirou no Final do Combate.");
            activeBuff = false;
            buffedHero = null;
        }
    }
}

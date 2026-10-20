package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class PlaySpellDoubleSpiritToAttackEffect implements Effect {

    private HeroCard buffedHero;
    private int appliedBuff = 0;
    private boolean activeBuff = false;

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
            int buffAmount = myHero.getEspirito() * 2;
            myHero.setAtaque(myHero.getAtaque() + buffAmount);
            this.buffedHero = myHero;
            this.appliedBuff = buffAmount;
            this.activeBuff = true;
            gameEngine.registerCombatEndCleanup(this::revertBuff);
            System.out.println("-> " + myHero.getCardName() + " recebeu +" + buffAmount + " de Ataque (Dobro do Espírito) até o Final do Combate!");
        }
    }

    private void revertBuff() {
        if (activeBuff && buffedHero != null) {
            buffedHero.setAtaque(buffedHero.getAtaque() - appliedBuff);
            System.out.println("-> Buff de Ataque (-" + appliedBuff + ") de " + buffedHero.getCardName() + " expirou no Final do Combate.");
            activeBuff = false;
            appliedBuff = 0;
            buffedHero = null;
        }
    }
}

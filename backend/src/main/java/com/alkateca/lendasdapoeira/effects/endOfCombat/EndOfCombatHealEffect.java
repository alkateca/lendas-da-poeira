package com.alkateca.lendasdapoeira.effects.endOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class EndOfCombatHealEffect implements Effect {
    private final int healAmount;

    public EndOfCombatHealEffect(int healAmount) {
        this.healAmount = healAmount;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_END;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
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
            int finalHeal = healAmount + gameEngine.getGoblinEffectBonus(sourceCard);
            int novaVida = myHero.getVidaAtual() + finalHeal;
            if (novaVida > myHero.getVidaMaxima()) novaVida = myHero.getVidaMaxima();
            myHero.setVidaAtual(novaVida);
            System.out.println("-> " + myHero.getCardName() + " recuperou " + finalHeal + " de Vida! Vida restante: " + myHero.getVidaAtual());
        }
    }
}

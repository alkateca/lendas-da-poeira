package com.alkateca.lendasdapoeira.effects.endOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class EndOfCombatHealAlliesEffect implements Effect {

    private final int healAmount;

    public EndOfCombatHealAlliesEffect(int healAmount) {
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

        com.alkateca.lendasdapoeira.entity.Player allyPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        int healedCount = 0;
        for (Card card : allyPlayer.getCurrentDeck().getHeroList()) {
            if (card instanceof HeroCard) {
                HeroCard hero = (HeroCard) card;
                if (Boolean.TRUE.equals(hero.getEstaVivo())) {
                    int currentLife = hero.getVidaAtual();
                    int maxLife = hero.getVidaMaxima();
                    if (currentLife < maxLife) {
                        int newLife = Math.min(currentLife + healAmount, maxLife);
                        hero.setVidaAtual(newLife);
                        healedCount++;
                        System.out.println("-> " + hero.getCardName() + " recuperou " + (newLife - currentLife) + " de Vida! Vida restante: " + hero.getVidaAtual());
                    }
                }
            }
        }
        
        if (healedCount == 0) {
            System.out.println("-> Nenhum aliado precisava de cura.");
        }
    }
}

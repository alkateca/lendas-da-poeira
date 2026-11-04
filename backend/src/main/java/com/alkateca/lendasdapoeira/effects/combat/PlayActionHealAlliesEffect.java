package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class PlayActionHealAlliesEffect implements Effect {

    private final int healAmount;

    public PlayActionHealAlliesEffect(int healAmount) {
        this.healAmount = healAmount;
    }

    public int getHealAmount() {
        return healAmount;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (myPlayer == null || myPlayer.getCurrentDeck() == null
                || myPlayer.getCurrentDeck().getHeroList() == null) {
            return;
        }

        int totalHeal = healAmount + gameEngine.getGoblinEffectBonus(sourceCard);

        for (HeroCard allyHero : myPlayer.getCurrentDeck().getHeroList()) {
            if (allyHero != null && Boolean.TRUE.equals(allyHero.getEstaVivo())) {
                int oldHp = allyHero.getVidaAtual();
                int newHp = Math.min(allyHero.getVidaMaxima(), oldHp + totalHeal);
                int recovered = Math.max(0, newHp - oldHp);
                allyHero.setVidaAtual(newHp);

                System.out.println("-> " + allyHero.getCardName() + " recuperou " + recovered +
                        " pontos de vida! Vida restante: " + allyHero.getVidaAtual());
            }
        }
    }
}

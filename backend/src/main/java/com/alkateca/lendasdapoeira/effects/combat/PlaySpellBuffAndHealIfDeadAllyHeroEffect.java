package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class PlaySpellBuffAndHealIfDeadAllyHeroEffect implements Effect {

    private final int bonusAtaque;
    private final int healAmount;

    public PlaySpellBuffAndHealIfDeadAllyHeroEffect(int bonusAtaque, int healAmount) {
        this.bonusAtaque = bonusAtaque;
        this.healAmount = healAmount;
    }

    public int getBonusAtaque() {
        return bonusAtaque;
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

        java.util.UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();

        com.alkateca.lendasdapoeira.entity.Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (myPlayer == null || myPlayer.getCurrentDeck() == null
                || myPlayer.getCurrentDeck().getHeroList() == null) {
            return;
        }

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        boolean hasDeadAlly = myPlayer.getCurrentDeck().getHeroList().stream()
                .anyMatch(h -> h != null && (!Boolean.TRUE.equals(h.getEstaVivo()) || h.getVidaAtual() <= 0));

        if (hasDeadAlly && myHero != null && Boolean.TRUE.equals(myHero.getEstaVivo())) {
            myHero.setAtaque(myHero.getAtaque() + bonusAtaque);

            gameEngine.registerCombatEndCleanup(() -> {
                myHero.setAtaque(myHero.getAtaque() - bonusAtaque);
            });

            int oldHp = myHero.getVidaAtual();
            int newHp = Math.min(myHero.getVidaMaxima(), oldHp + healAmount);
            int recovered = Math.max(0, newHp - oldHp);
            myHero.setVidaAtual(newHp);

            System.out.println("-> " + myHero.getCardName() + " recebeu +" + bonusAtaque
                    + " de Ataque até o final do combate e recuperou " + recovered
                    + " pontos de vida! Vida restante: " + myHero.getVidaAtual());
        } else {
            System.out.println("-> Nenhum herói aliado morto. Efeito condicional não ativado.");
        }
    }
}

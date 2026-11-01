package com.alkateca.lendasdapoeira.effects.startOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class StartOfCombatAlliesBuffAttackEffect implements Effect {

    private final int bonusAtaque;

    public StartOfCombatAlliesBuffAttackEffect(int bonusAtaque) {
        this.bonusAtaque = bonusAtaque;
    }

    public int getBonusAtaque() {
        return bonusAtaque;
    }

    @Override
    public boolean canTriggerFromBench() {
        return true;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_START;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        com.alkateca.lendasdapoeira.entity.Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (myPlayer == null || myPlayer.getCurrentDeck() == null
                || myPlayer.getCurrentDeck().getHeroList() == null) {
            return;
        }

        for (HeroCard allyHero : myPlayer.getCurrentDeck().getHeroList()) {
            if (allyHero != null && Boolean.TRUE.equals(allyHero.getEstaVivo()) && allyHero.getVidaAtual() > 0) {
                allyHero.setAtaque(allyHero.getAtaque() + bonusAtaque);

                gameEngine.registerCombatEndCleanup(() -> {
                    allyHero.setAtaque(allyHero.getAtaque() - bonusAtaque);
                });

                System.out.println("-> " + allyHero.getCardName() + " recebeu +" + bonusAtaque +
                        " de Ataque até o final do combate! Vida restante: " + allyHero.getVidaAtual());
            }
        }
    }
}

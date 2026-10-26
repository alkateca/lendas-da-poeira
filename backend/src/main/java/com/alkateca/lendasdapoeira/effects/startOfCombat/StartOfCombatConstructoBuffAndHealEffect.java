package com.alkateca.lendasdapoeira.effects.startOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.Raca;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class StartOfCombatConstructoBuffAndHealEffect implements Effect {

    private final int attackBuff;
    private final int healAmount;

    public StartOfCombatConstructoBuffAndHealEffect(int attackBuff, int healAmount) {
        this.attackBuff = attackBuff;
        this.healAmount = healAmount;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_START;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");
        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        Player allyPlayer = isPlayer1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();

        if (allyPlayer == null || allyPlayer.getCurrentDeck() == null || allyPlayer.getCurrentDeck().getHeroList() == null) {
            return;
        }

        for (Card c : allyPlayer.getCurrentDeck().getHeroList()) {
            if (c instanceof HeroCard hero
                    && Boolean.TRUE.equals(hero.getEstaVivo())
                    && hero.getRacas() != null
                    && hero.getRacas().contains(Raca.CONSTRUCTO)) {
                hero.setAtaque(hero.getAtaque() + attackBuff);
                int newLife = Math.min(hero.getVidaAtual() + healAmount, hero.getVidaMaxima());
                hero.setVidaAtual(newLife);
                System.out.println("-> Constructo " + hero.getCardName() + " recebeu +" + attackBuff +
                        " de Ataque e recuperou até " + healAmount + " de Vida! Vida restante: " + hero.getVidaAtual());
            }
        }
    }
}

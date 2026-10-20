package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class AuraAlliesBonusDamageEffect implements Effect {

    private final int bonusDamage;
    private boolean applied = false;

    public AuraAlliesBonusDamageEffect(int bonusDamage) {
        this.bonusDamage = bonusDamage;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return !applied && (turnPhase == TurnPhase.GAME_START
                || turnPhase == TurnPhase.COMBAT_START
                || turnPhase == TurnPhase.RESOLUTION);
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        if (applied) return;
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");
        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        com.alkateca.lendasdapoeira.entity.Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (myPlayer != null && myPlayer.getCurrentDeck() != null && myPlayer.getCurrentDeck().getHeroList() != null) {
            for (Card card : myPlayer.getCurrentDeck().getHeroList()) {
                HeroCard hero = (HeroCard) card;
                hero.setDanoBonus(hero.getDanoBonus() + bonusDamage);
            }
            applied = true;
            System.out.println("-> Todos os aliados receberam +" + bonusDamage + " de Dano Bônus via Aura!");
        }
    }
}

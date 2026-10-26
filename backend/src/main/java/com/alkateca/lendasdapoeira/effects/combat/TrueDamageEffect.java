package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class TrueDamageEffect implements Effect {

    private int damage;

    public TrueDamageEffect(int damage) {
        this.damage = damage;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase){

        return turnPhase == TurnPhase.RESOLUTION;

    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard){

        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        java.util.UUID enemyHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                : gameEngine.getGameState().getActiveHeroPlayer1Id();

        com.alkateca.lendasdapoeira.entity.Player enemyPlayer = isPlayer1
                ? gameEngine.getPlayer2()
                : gameEngine.getPlayer1();

        HeroCard enemyHero = (HeroCard) enemyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(enemyHeroId))
                .findFirst()
                .orElse(null);

        if (enemyHero != null) {
            java.util.UUID myHeroId = isPlayer1
                    ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                    : gameEngine.getGameState().getActiveHeroPlayer2Id();
            HeroCard myHero = gameEngine.findHeroById(myHeroId);
            int finalDamage = gameEngine.calculateDirectDamage(myHero, enemyHero, damage);
            enemyHero.setVidaAtual(enemyHero.getVidaAtual() - finalDamage);
            System.out.println("-> " + enemyHero.getCardName() + " sofreu " + finalDamage +
                    " de dano direto! Vida restante: " + enemyHero.getVidaAtual());
        }

    }

}

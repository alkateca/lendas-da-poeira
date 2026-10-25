package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class PlaySpellHealBySpiritEffect implements Effect {

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

        com.alkateca.lendasdapoeira.entity.Player myPlayer = isPlayer1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();
        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId)).findFirst().orElse(null);

        if (myHero != null) {
            int healAmount = Math.max(0, myHero.getEspirito() + 2);
            int novaVida = Math.min(myHero.getVidaMaxima(), myHero.getVidaAtual() + healAmount);
            myHero.setVidaAtual(novaVida);
            System.out.println("-> " + myHero.getCardName() + " curou " + healAmount +
                    " de Vida baseada no seu Espírito! Vida restante: " + myHero.getVidaAtual());
        }
    }
}

package com.alkateca.lendasdapoeira.effects.endOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

public class EndOfCombatReviveAndExileEffect implements Effect {

    private final int reviveHealth;

    public EndOfCombatReviveAndExileEffect(int reviveHealth) {
        this.reviveHealth = reviveHealth;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_END;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        java.util.UUID allyHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();

        com.alkateca.lendasdapoeira.entity.Player allyPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        HeroCard allyHero = (HeroCard) allyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(allyHeroId))
                .findFirst()
                .orElse(null);

        if (allyHero != null && Boolean.TRUE.equals(allyHero.getEstaVivo())) {
            if (allyHero.getVidaAtual() < 0) {
                allyHero.setVidaAtual(reviveHealth);
                System.out.println("-> " + allyHero.getCardName() + " recuperou vida! Vida restante: " + allyHero.getVidaAtual());
            }
        }
        
        // Exile the item
        sourceCard.setZoneId(ZoneId.EXILE);
        sourceCard.setAttachedToCardId(null);
        System.out.println("-> " + sourceCard.getCardName() + " foi exilado!");
    }
}

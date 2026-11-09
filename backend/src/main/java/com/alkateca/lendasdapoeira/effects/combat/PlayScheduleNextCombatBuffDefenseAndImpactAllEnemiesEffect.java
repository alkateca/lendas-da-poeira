package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.UUID;

public class PlayScheduleNextCombatBuffDefenseAndImpactAllEnemiesEffect implements Effect {

    private final int defenseBuff;

    public PlayScheduleNextCombatBuffDefenseAndImpactAllEnemiesEffect(int defenseBuff) {
        this.defenseBuff = defenseBuff;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado! Agendado para o Início do Próximo Combate.");

        UUID ownerId = sourceCard.getOwnerId();
        Effect selfEffect = this;

        gameEngine.scheduleNextCombatStartEffect(ownerId, () -> {
            boolean isP1 = ownerId != null && ownerId.equals(gameEngine.getPlayer1().getUuid());
            Player myPlayer = isP1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();
            Player enemyPlayer = isP1 ? gameEngine.getPlayer2() : gameEngine.getPlayer1();
            UUID myHeroId = isP1
                    ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                    : gameEngine.getGameState().getActiveHeroPlayer2Id();

            if (myPlayer == null || enemyPlayer == null || myHeroId == null) return;

            HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                    .filter(h -> h.getUuid().equals(myHeroId))
                    .findFirst()
                    .orElse(null);

            if (myHero != null && Boolean.TRUE.equals(myHero.getEstaVivo())) {
                myHero.setDefesa(myHero.getDefesa() + defenseBuff);
                gameEngine.registerCombatEndCleanup(() -> myHero.setDefesa(myHero.getDefesa() - defenseBuff));
                System.out.println("-> [" + sourceCard.getCardName() + "] " + myHero.getCardName() +
                        " recebeu +" + defenseBuff + " de Defesa e desfere Impacto em todos os Heróis Inimigos!");

                for (Card c : enemyPlayer.getCurrentDeck().getHeroList()) {
                    if (c instanceof HeroCard enemyHero && Boolean.TRUE.equals(enemyHero.getEstaVivo())) {
                        gameEngine.resolveImpactOnTarget(myHero, enemyHero, sourceCard);
                    }
                }
                gameEngine.captureSnapshot(sourceCard, selfEffect, "EFFECT_RESOLVED");
            }
        });
    }
}

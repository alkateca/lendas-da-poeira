package com.alkateca.lendasdapoeira.effects.startOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.UUID;

public class StartOfCombatZeroEnemyDefenseIfWoundedEffect implements Effect {

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_START;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        UUID enemyHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                : gameEngine.getGameState().getActiveHeroPlayer1Id();

        Player enemyPlayer = isPlayer1
                ? gameEngine.getPlayer2()
                : gameEngine.getPlayer1();

        if (enemyPlayer == null || enemyHeroId == null) return;

        HeroCard enemyHero = (HeroCard) enemyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(enemyHeroId))
                .findFirst()
                .orElse(null);

        if (enemyHero != null && enemyHero.getVidaAtual() < enemyHero.getVidaMaxima()) {
            int removedDefense = enemyHero.getDefesa();
            enemyHero.setDefesa(0);
            gameEngine.registerCombatEndCleanup(() -> {
                enemyHero.setDefesa(enemyHero.getDefesa() + removedDefense);
                System.out.println("-> Defesa de " + enemyHero.getCardName() +
                        " restaurada no Final do Combate: " + enemyHero.getDefesa());
            });
            System.out.println("-> " + enemyHero.getCardName() +
                    " está Ferido! Sua Defesa se tornou 0 até o Final do Combate.");
        }
    }
}

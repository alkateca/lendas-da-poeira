package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class AuraEnemyDirectDamageOnOpponentPlayCardEffect implements Effect {

    private final int directDamage;

    public AuraEnemyDirectDamageOnOpponentPlayCardEffect(int directDamage) {
        this.directDamage = directDamage;
    }

    public int getDirectDamage() {
        return directDamage;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
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

        java.util.UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();

        com.alkateca.lendasdapoeira.entity.Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        if (enemyHero != null) {
            int opponentCardsPlayed = gameEngine.getCardsPlayedByPlayerInCurrentTurn(enemyPlayer.getUuid());
            int triggers = Math.max(1, opponentCardsPlayed);
            int totalDirectDamage = 0;

            for (int i = 0; i < triggers; i++) {
                int dmg = gameEngine.calculateDirectDamage(myHero, enemyHero, directDamage);
                enemyHero.setVidaAtual(enemyHero.getVidaAtual() - dmg);
                totalDirectDamage += dmg;
            }

            System.out.println("-> " + enemyHero.getCardName() + " sofreu " + totalDirectDamage +
                    " de dano direto! Vida restante: " + enemyHero.getVidaAtual());
        }
    }
}

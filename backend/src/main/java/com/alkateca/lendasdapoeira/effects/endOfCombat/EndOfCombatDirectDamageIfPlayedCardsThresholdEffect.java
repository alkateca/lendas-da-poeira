package com.alkateca.lendasdapoeira.effects.endOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class EndOfCombatDirectDamageIfPlayedCardsThresholdEffect implements Effect {

    private final int minCardsPlayed;
    private final int directDamage;

    public EndOfCombatDirectDamageIfPlayedCardsThresholdEffect(int minCardsPlayed, int directDamage) {
        this.minCardsPlayed = minCardsPlayed;
        this.directDamage = directDamage;
    }

    public int getMinCardsPlayed() {
        return minCardsPlayed;
    }

    public int getDirectDamage() {
        return directDamage;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_END;
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

        int cardsPlayedCount = gameEngine.getCardsPlayedByPlayerInCurrentTurn(myPlayer.getUuid());

        if (cardsPlayedCount >= minCardsPlayed && enemyHero != null) {
            int bonusGoblin = gameEngine.getGoblinEffectBonus(sourceCard);
            int finalDamage = gameEngine.calculateDirectDamage(myHero, enemyHero, directDamage + bonusGoblin);
            enemyHero.setVidaAtual(enemyHero.getVidaAtual() - finalDamage);

            System.out.println("-> " + enemyHero.getCardName() + " sofreu " + finalDamage +
                    " de dano direto (" + cardsPlayedCount + " cartas jogadas neste turno)! Vida restante: " +
                    enemyHero.getVidaAtual());
        } else {
            System.out.println("-> Quantidade de cartas jogadas (" + cardsPlayedCount + "/" + minCardsPlayed +
                    ") insuficiente. Nenhum dano direto aplicado.");
        }
    }
}

package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.UUID;

public class PlayTheurgyMagicDamagePerOpponentPlayedOrInvokedCardEffect implements Effect {

    private final int magicDamagePerCard;

    public PlayTheurgyMagicDamagePerOpponentPlayedOrInvokedCardEffect(int magicDamagePerCard) {
        this.magicDamagePerCard = magicDamagePerCard;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();
        UUID enemyHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                : gameEngine.getGameState().getActiveHeroPlayer1Id();

        Player myPlayer = isPlayer1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();
        Player enemyPlayer = isPlayer1 ? gameEngine.getPlayer2() : gameEngine.getPlayer1();

        if (myPlayer == null || enemyPlayer == null) {
            return;
        }

        int opponentCardsCount = gameEngine.getCardsPlayedOrInvokedByPlayerInCurrentCombat(enemyPlayer.getUuid());
        if (opponentCardsCount <= 0) {
            System.out.println("-> Nenhuma carta jogada ou invocada pelo oponente neste combate.");
            return;
        }

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        HeroCard enemyHero = (HeroCard) enemyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(enemyHeroId))
                .findFirst()
                .orElse(null);

        if (enemyHero != null) {
            int rawMagicDamage = magicDamagePerCard * opponentCardsCount;
            int finalDamage = gameEngine.calculateMagicDamage(myHero, enemyHero, rawMagicDamage);
            enemyHero.setVidaAtual(enemyHero.getVidaAtual() - finalDamage);
            gameEngine.evaluateInstantDeathByAura();
            System.out.println("-> " + enemyHero.getCardName() + " sofreu " + finalDamage +
                    " de dano mágico (" + opponentCardsCount + " carta(s) jogada(s)/invocada(s) pelo oponente)! Vida restante: " +
                    enemyHero.getVidaAtual());
        }
    }
}

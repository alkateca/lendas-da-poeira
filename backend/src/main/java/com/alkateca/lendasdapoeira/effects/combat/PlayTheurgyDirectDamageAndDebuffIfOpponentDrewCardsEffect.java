package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.UUID;

public class PlayTheurgyDirectDamageAndDebuffIfOpponentDrewCardsEffect implements Effect {

    private final int directDamage;
    private final int attackDebuff;

    public PlayTheurgyDirectDamageAndDebuffIfOpponentDrewCardsEffect(int directDamage, int attackDebuff) {
        this.directDamage = directDamage;
        this.attackDebuff = attackDebuff;
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

        if (!gameEngine.hasDrawnCardsOutsideChoiceInCurrentTurn(enemyPlayer.getUuid())) {
            System.out.println("-> O oponente não comprou cartas fora da Fase de Escolha neste turno.");
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
            int dmg = gameEngine.calculateDirectDamage(myHero, enemyHero, directDamage);
            enemyHero.setVidaAtual(enemyHero.getVidaAtual() - dmg);
            gameEngine.evaluateInstantDeathByAura();
            System.out.println("-> " + enemyHero.getCardName() + " sofreu " + dmg +
                    " de Dano Direto! Vida restante: " + enemyHero.getVidaAtual());

            int prevAtk = enemyHero.getAtaque();
            enemyHero.setAtaque(prevAtk - attackDebuff);
            int actualReduction = prevAtk - enemyHero.getAtaque();
            if (actualReduction > 0) {
                gameEngine.registerCombatEndCleanup(() -> {
                    enemyHero.setAtaque(enemyHero.getAtaque() + actualReduction);
                    System.out.println("-> Ataque de " + enemyHero.getCardName() +
                            " restaurado no Final do Combate: " + enemyHero.getAtaque());
                });
            }
            System.out.println("-> " + enemyHero.getCardName() + " recebeu -" + attackDebuff +
                    " de Ataque até o Final do Combate! Ataque atual: " + enemyHero.getAtaque());
        }
    }
}

package com.alkateca.lendasdapoeira.effects.startOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.UUID;

public class StartOfCombatBuffIfEnemyHigherMaxHpEffect implements Effect {

    private final int bonusAtaque;
    private final int bonusDefesa;

    public StartOfCombatBuffIfEnemyHigherMaxHpEffect(int bonusAtaque, int bonusDefesa) {
        this.bonusAtaque = bonusAtaque;
        this.bonusDefesa = bonusDefesa;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_START;
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

        if (myPlayer == null || enemyPlayer == null || myHeroId == null || enemyHeroId == null) {
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

        if (myHero != null && enemyHero != null && enemyHero.getVidaMaxima() > myHero.getVidaMaxima()) {
            myHero.setAtaque(myHero.getAtaque() + bonusAtaque);
            myHero.setDefesa(myHero.getDefesa() + bonusDefesa);

            gameEngine.registerCombatEndCleanup(() -> {
                myHero.setAtaque(myHero.getAtaque() - bonusAtaque);
                myHero.setDefesa(myHero.getDefesa() - bonusDefesa);
                System.out.println("-> Buff temporário de " + sourceCard.getCardName() +
                        " expirou em " + myHero.getCardName() + ".");
            });

            System.out.println("-> " + enemyHero.getCardName() + " possui mais Vida Máxima (" +
                    enemyHero.getVidaMaxima() + " > " + myHero.getVidaMaxima() + ")! " +
                    myHero.getCardName() + " recebeu +" + bonusAtaque + " ATQ e +" + bonusDefesa +
                    " DEF até o Final do Combate!");
        }
    }
}

package com.alkateca.lendasdapoeira.effects.startOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.Classe;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.UUID;

public class StartOfCombatBonusAttackVsClassEffect implements Effect {

    private final Classe targetEnemyClass;
    private final int bonusAttack;

    public StartOfCombatBonusAttackVsClassEffect(Classe targetEnemyClass, int bonusAttack) {
        this.targetEnemyClass = targetEnemyClass;
        this.bonusAttack = bonusAttack;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_START;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();
        UUID enemyHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                : gameEngine.getGameState().getActiveHeroPlayer1Id();

        Player myPlayer = isPlayer1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();
        Player enemyPlayer = isPlayer1 ? gameEngine.getPlayer2() : gameEngine.getPlayer1();

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        HeroCard enemyHero = (HeroCard) enemyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(enemyHeroId))
                .findFirst()
                .orElse(null);

        if (myHero != null && enemyHero != null && enemyHero.getClasse() != null && enemyHero.getClasse().contains(targetEnemyClass)) {
            System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");
            myHero.setAtaque(myHero.getAtaque() + bonusAttack);
            gameEngine.registerCombatEndCleanup(() -> {
                myHero.setAtaque(myHero.getAtaque() - bonusAttack);
            });
            System.out.println("-> " + myHero.getCardName() + " recebeu +" + bonusAttack +
                    " de Ataque contra " + enemyHero.getCardName() + " (" + targetEnemyClass.name() + ") neste combate!");
        }
    }
}

package com.alkateca.lendasdapoeira.effects.endOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.UUID;

public class EndOfCombatExtraAttackIfHighAttackEffect implements Effect {

    private final int minAttackThreshold;

    public EndOfCombatExtraAttackIfHighAttackEffect(int minAttackThreshold) {
        this.minAttackThreshold = minAttackThreshold;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_END;
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

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        HeroCard enemyHero = (HeroCard) enemyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(enemyHeroId))
                .findFirst()
                .orElse(null);

        if (myHero != null && enemyHero != null) {
            int peakAttack = Math.max(myHero.getAtaque(), gameEngine.getMaxHeroAttackInCurrentCombat(myHero.getUuid()));
            if (peakAttack >= minAttackThreshold) {
                int extraDamage = gameEngine.calculateBasicAttackDamage(myHero, enemyHero);
                enemyHero.setVidaAtual(enemyHero.getVidaAtual() - extraDamage);
                System.out.println("-> Ataque Extra de " + myHero.getCardName() + " (Ataque atingiu " + peakAttack +
                        " >= " + minAttackThreshold + "): " + enemyHero.getCardName() + " sofreu " + extraDamage +
                        " de dano! Vida restante: " + enemyHero.getVidaAtual());
            } else {
                System.out.println("-> Ataque de " + myHero.getCardName() + " (" + peakAttack +
                        ") não atingiu " + minAttackThreshold + " para realizar ataque extra.");
            }
        }
    }
}

package com.alkateca.lendasdapoeira.effects.endOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.UUID;

public class EndOfCombatDirectDamageByCombatHealingEffect implements Effect {

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

        HeroCard myHero = gameEngine.findHeroById(myHeroId);
        Player enemyPlayer = isPlayer1 ? gameEngine.getPlayer2() : gameEngine.getPlayer1();

        HeroCard enemyHero = (HeroCard) enemyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(enemyHeroId))
                .findFirst()
                .orElse(null);

        if (enemyHero != null) {
            int totalEffectiveHeal = gameEngine.getEffectiveHealingInCurrentCombat(sourceCard.getOwnerId());
            if (totalEffectiveHeal > 0) {
                int damage = gameEngine.calculateDirectDamage(myHero, enemyHero, totalEffectiveHeal);
                enemyHero.setVidaAtual(enemyHero.getVidaAtual() - damage);
                System.out.println("-> " + enemyHero.getCardName() + " sofreu " + damage +
                        " de Dano Direto (soma da cura efetiva do combate)! Vida restante: " + enemyHero.getVidaAtual());
            } else {
                System.out.println("-> Nenhuma cura efetiva realizada neste combate (0 de Dano Direto).");
            }
        }
    }
}

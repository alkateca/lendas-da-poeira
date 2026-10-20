package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.UUID;

public class PlayDebuffEnemyDefenseEffect implements Effect {

    private final int defenseDebuff;
    private HeroCard debuffedHero;
    private boolean activeDebuff = false;

    public PlayDebuffEnemyDefenseEffect(int defenseDebuff) {
        this.defenseDebuff = defenseDebuff;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        if (gameEngine.getTurnPhase() == TurnPhase.COMBAT_END) {
            revertDebuff();
            return;
        }

        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");
        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        UUID enemyHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                : gameEngine.getGameState().getActiveHeroPlayer1Id();
        Player enemyPlayer = isPlayer1
                ? gameEngine.getPlayer2()
                : gameEngine.getPlayer1();

        HeroCard enemyHero = (HeroCard) enemyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> enemyHeroId != null
                        ? h.getUuid().equals(enemyHeroId)
                        : Boolean.TRUE.equals(((HeroCard) h).getEstaVivo()))
                .findFirst()
                .orElse(null);

        if (enemyHero != null) {
            enemyHero.setDefesa(enemyHero.getDefesa() - defenseDebuff);
            this.debuffedHero = enemyHero;
            this.activeDebuff = true;
            gameEngine.registerCombatEndCleanup(this::revertDebuff);
            System.out.println("-> A Defesa de " + enemyHero.getCardName() + " foi reduzida em " + defenseDebuff +
                    " até o Final do Combate! Defesa atual: " + enemyHero.getDefesa());
        }
    }

    private void revertDebuff() {
        if (activeDebuff && debuffedHero != null) {
            debuffedHero.setDefesa(debuffedHero.getDefesa() + defenseDebuff);
            System.out.println("-> Debuff de Defesa de " + debuffedHero.getCardName() + " expirou no Final do Combate! Defesa restaurada: " + debuffedHero.getDefesa());
            activeDebuff = false;
            debuffedHero = null;
        }
    }
}

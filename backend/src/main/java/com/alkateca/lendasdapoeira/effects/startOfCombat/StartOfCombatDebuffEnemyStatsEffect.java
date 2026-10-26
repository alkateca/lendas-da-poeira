package com.alkateca.lendasdapoeira.effects.startOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class StartOfCombatDebuffEnemyStatsEffect implements Effect {

    private final int spiritDebuff;
    private final int attackDebuff;
    private final int defenseDebuff;
    private final int maxLifeDebuff;

    public StartOfCombatDebuffEnemyStatsEffect(int spiritDebuff, int attackDebuff, int defenseDebuff, int maxLifeDebuff) {
        this.spiritDebuff = spiritDebuff;
        this.attackDebuff = attackDebuff;
        this.defenseDebuff = defenseDebuff;
        this.maxLifeDebuff = maxLifeDebuff;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_START;
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

        if (enemyHero != null) {
            enemyHero.setEspirito(enemyHero.getEspirito() - spiritDebuff);
            enemyHero.setAtaque(enemyHero.getAtaque() - attackDebuff);
            enemyHero.setDefesa(enemyHero.getDefesa() - defenseDebuff);
            int newMaxLife = Math.max(1, enemyHero.getVidaMaxima() - maxLifeDebuff);
            enemyHero.setVidaMaxima(newMaxLife);
            if (enemyHero.getVidaAtual() > newMaxLife) {
                enemyHero.setVidaAtual(newMaxLife);
            }

            System.out.println("-> " + enemyHero.getCardName() + " sofreu -" + spiritDebuff + " ESP, -" +
                    attackDebuff + " ATQ, -" + defenseDebuff + " DEF e -" + maxLifeDebuff +
                    " Vida Máxima até o Final da Partida! Vida restante: " + enemyHero.getVidaAtual() + "/" + enemyHero.getVidaMaxima());
        }
    }
}

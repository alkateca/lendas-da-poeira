package com.alkateca.lendasdapoeira.effects.gameStart;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class StartOfGameDebuffEnemyDefenseEffect implements Effect {

    private final int defenseDebuff;
    private boolean applied = false;

    public StartOfGameDebuffEnemyDefenseEffect(int defenseDebuff) {
        this.defenseDebuff = defenseDebuff;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return !applied && (turnPhase == TurnPhase.GAME_START || turnPhase == TurnPhase.COMBAT_START);
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        if (applied) return;
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");
        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        java.util.UUID enemyHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                : gameEngine.getGameState().getActiveHeroPlayer1Id();
        com.alkateca.lendasdapoeira.entity.Player enemyPlayer = isPlayer1
                ? gameEngine.getPlayer2()
                : gameEngine.getPlayer1();

        HeroCard enemyHero = (HeroCard) enemyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> enemyHeroId != null ? h.getUuid().equals(enemyHeroId) : Boolean.TRUE.equals(((HeroCard) h).getEstaVivo()))
                .findFirst()
                .orElse(null);

        if (enemyHero != null) {
            enemyHero.setDefesa(enemyHero.getDefesa() - defenseDebuff);
            applied = true;
            System.out.println("-> A Defesa de " + enemyHero.getCardName() + " foi reduzida em " + defenseDebuff +
                    "! Defesa atual: " + enemyHero.getDefesa());
        }
    }
}

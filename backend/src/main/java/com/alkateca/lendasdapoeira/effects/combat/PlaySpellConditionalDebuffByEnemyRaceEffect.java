package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.Raca;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class PlaySpellConditionalDebuffByEnemyRaceEffect implements Effect {

    private final Raca targetRace;
    private final int defenseDebuffIfRace;
    private final int attackDebuffIfNotRace;

    public PlaySpellConditionalDebuffByEnemyRaceEffect(Raca targetRace, int defenseDebuffIfRace, int attackDebuffIfNotRace) {
        this.targetRace = targetRace;
        this.defenseDebuffIfRace = defenseDebuffIfRace;
        this.attackDebuffIfNotRace = attackDebuffIfNotRace;
    }

    public Raca getTargetRace() {
        return targetRace;
    }

    public int getDefenseDebuffIfRace() {
        return defenseDebuffIfRace;
    }

    public int getAttackDebuffIfNotRace() {
        return attackDebuffIfNotRace;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
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

        if (enemyHero == null) {
            return;
        }

        boolean isTargetRace = enemyHero.getRacas() != null && enemyHero.getRacas().contains(targetRace);

        if (isTargetRace) {
            enemyHero.setDefesa(enemyHero.getDefesa() - defenseDebuffIfRace);

            gameEngine.registerCombatEndCleanup(() -> {
                enemyHero.setDefesa(enemyHero.getDefesa() + defenseDebuffIfRace);
            });

            System.out.println("-> " + enemyHero.getCardName() + " é da raça " + targetRace
                    + " e recebeu -" + defenseDebuffIfRace + " de Defesa até o final do combate! Vida restante: "
                    + enemyHero.getVidaAtual());
        } else {
            enemyHero.setAtaque(enemyHero.getAtaque() - attackDebuffIfNotRace);

            gameEngine.registerCombatEndCleanup(() -> {
                enemyHero.setAtaque(enemyHero.getAtaque() + attackDebuffIfNotRace);
            });

            System.out.println("-> " + enemyHero.getCardName() + " não é da raça " + targetRace
                    + " e recebeu -" + attackDebuffIfNotRace + " de Ataque até o final do combate! Vida restante: "
                    + enemyHero.getVidaAtual());
        }
    }
}

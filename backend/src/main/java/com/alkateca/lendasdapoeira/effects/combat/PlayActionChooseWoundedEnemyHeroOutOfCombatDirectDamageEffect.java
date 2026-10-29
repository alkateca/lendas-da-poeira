package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayActionChooseWoundedEnemyHeroOutOfCombatDirectDamageEffect implements Effect {

    private final int directDamage;

    public PlayActionChooseWoundedEnemyHeroOutOfCombatDirectDamageEffect(int directDamage) {
        this.directDamage = directDamage;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        Player myPlayer = isPlayer1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();
        Player enemyPlayer = isPlayer1 ? gameEngine.getPlayer2() : gameEngine.getPlayer1();

        if (myPlayer == null || enemyPlayer == null || enemyPlayer.getCurrentDeck() == null
                || enemyPlayer.getCurrentDeck().getHeroList() == null) {
            return;
        }

        UUID enemyActiveHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                : gameEngine.getGameState().getActiveHeroPlayer1Id();

        List<UUID> validEnemyHeroUuids = enemyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(hero -> hero != null
                        && Boolean.TRUE.equals(hero.getEstaVivo())
                        && hero.getVidaAtual() < hero.getVidaMaxima()
                        && (hero.getZoneId() == ZoneId.BENCH || (enemyActiveHeroId != null && !hero.getUuid().equals(enemyActiveHeroId))))
                .map(Card::getUuid)
                .collect(Collectors.toList());

        if (validEnemyHeroUuids.isEmpty()) {
            System.out.println("-> Nenhum Herói Inimigo Ferido fora de combate disponível.");
            return;
        }

        gameEngine.requestPlayerChoice(
                myPlayer.getUuid(),
                sourceCard,
                "ENEMY_HERO",
                "Escolha um Herói Inimigo Ferido fora de combate para sofrer " + directDamage + " de Dano Direto",
                validEnemyHeroUuids,
                false,
                chosenHeroUuid -> {
                    if (chosenHeroUuid == null) return;
                    HeroCard chosenEnemyHero = enemyPlayer.getCurrentDeck().getHeroList().stream()
                            .filter(c -> c != null && c.getUuid().equals(chosenHeroUuid))
                            .findFirst()
                            .orElse(null);

                    if (chosenEnemyHero != null && Boolean.TRUE.equals(chosenEnemyHero.getEstaVivo())) {
                        UUID myHeroId = isPlayer1
                                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                                : gameEngine.getGameState().getActiveHeroPlayer2Id();
                        HeroCard myHero = gameEngine.findHeroById(myHeroId);
                        int finalDamage = gameEngine.calculateDirectDamage(myHero, chosenEnemyHero, directDamage);
                        chosenEnemyHero.setVidaAtual(chosenEnemyHero.getVidaAtual() - finalDamage);
                        gameEngine.evaluateInstantDeathByAura();
                        System.out.println("-> " + chosenEnemyHero.getCardName() + " sofreu " + finalDamage +
                                " de Dano Direto de " + sourceCard.getCardName() + "! (Vida restante: " +
                                chosenEnemyHero.getVidaAtual() + ")");
                    }
                }
        );
    }
}

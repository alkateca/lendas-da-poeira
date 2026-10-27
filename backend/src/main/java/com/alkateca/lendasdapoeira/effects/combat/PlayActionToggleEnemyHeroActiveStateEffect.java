package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayActionToggleEnemyHeroActiveStateEffect implements Effect {

    public PlayActionToggleEnemyHeroActiveStateEffect() {
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

        List<UUID> validEnemyHeroUuids = enemyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(c -> c instanceof HeroCard && Boolean.TRUE.equals(((HeroCard) c).getEstaVivo()))
                .map(Card::getUuid)
                .collect(Collectors.toList());

        gameEngine.requestPlayerChoice(
                myPlayer.getUuid(),
                sourceCard,
                "ENEMY_HERO",
                "Escolha um Herói Inimigo para alterar seu Estado Ativo",
                validEnemyHeroUuids,
                false,
                chosenHeroUuid -> {
                    if (chosenHeroUuid == null) return;
                    HeroCard chosenHero = gameEngine.findHeroById(chosenHeroUuid);
                    if (chosenHero != null && Boolean.TRUE.equals(chosenHero.getEstaVivo())) {
                        boolean newState = !Boolean.TRUE.equals(chosenHero.getEstaAtivo());
                        chosenHero.setEstaAtivo(newState);
                        System.out.println("-> Estado Ativo de " + chosenHero.getCardName() +
                                " foi alterado para " + newState + " por " + sourceCard.getCardName() + "!");
                    }
                }
        );
    }
}

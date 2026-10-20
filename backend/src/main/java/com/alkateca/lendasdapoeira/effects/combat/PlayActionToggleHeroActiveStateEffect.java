package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayActionToggleHeroActiveStateEffect implements Effect {

    public PlayActionToggleHeroActiveStateEffect() {
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

        if (myPlayer == null) {
            return;
        }

        List<Card> allHeroes = new ArrayList<>();
        if (gameEngine.getPlayer1() != null && gameEngine.getPlayer1().getCurrentDeck() != null
                && gameEngine.getPlayer1().getCurrentDeck().getHeroList() != null) {
            allHeroes.addAll(gameEngine.getPlayer1().getCurrentDeck().getHeroList());
        }
        if (gameEngine.getPlayer2() != null && gameEngine.getPlayer2().getCurrentDeck() != null
                && gameEngine.getPlayer2().getCurrentDeck().getHeroList() != null) {
            allHeroes.addAll(gameEngine.getPlayer2().getCurrentDeck().getHeroList());
        }

        List<UUID> validHeroUuids = allHeroes.stream()
                .filter(c -> c instanceof HeroCard && Boolean.TRUE.equals(((HeroCard) c).getEstaVivo()))
                .map(Card::getUuid)
                .collect(Collectors.toList());

        gameEngine.requestPlayerChoice(
                myPlayer.getUuid(),
                sourceCard,
                "ANY_HERO",
                "Escolha um Herói para alterar seu Estado Ativo",
                validHeroUuids,
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

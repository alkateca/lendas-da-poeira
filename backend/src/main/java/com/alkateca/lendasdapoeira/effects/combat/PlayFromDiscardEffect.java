package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayFromDiscardEffect implements Effect {
    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        List<UUID> validDiscardUuids = myPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.DISCARD && !c.getUuid().equals(sourceCard.getUuid()))
                .map(Card::getUuid)
                .collect(Collectors.toList());

        gameEngine.requestPlayerChoice(
                myPlayer.getUuid(),
                sourceCard,
                "DISCARD",
                "Escolha uma carta do seu descarte para jogar:",
                validDiscardUuids,
                false,
                chosenUuid -> {
                    Card chosenCard = myPlayer.getCurrentDeck().getCardList().stream()
                            .filter(c -> c.getUuid().equals(chosenUuid))
                            .findFirst()
                            .orElse(null);

                    if (chosenCard != null) {
                        System.out.println("-> Jogador " + myPlayer.getName() + " jogou a carta " + chosenCard.getCardName() + " do descarte!");
                        gameEngine.playCardIntoResolution(chosenCard);
                    }
                }
        );
    }
}

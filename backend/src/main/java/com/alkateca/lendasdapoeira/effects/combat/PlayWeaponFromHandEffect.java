package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayWeaponFromHandEffect implements Effect {

    private final int empunhadura;

    public PlayWeaponFromHandEffect(int empunhadura) {
        this.empunhadura = empunhadura;
    }

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

        if (myPlayer == null || myPlayer.getCurrentDeck() == null || myPlayer.getCurrentDeck().getCardList() == null) {
            return;
        }

        List<UUID> validTargetUuids = myPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.HAND
                        && c.getItemType() == ItemType.ARMA
                        && c.getEmpunhadura() == empunhadura
                        && !c.getUuid().equals(sourceCard.getUuid()))
                .map(Card::getUuid)
                .collect(Collectors.toList());

        gameEngine.requestPlayerChoice(
                myPlayer.getUuid(),
                sourceCard,
                "HAND",
                "Escolha 1 Arma de Empunhadura " + empunhadura + " da sua mão para jogar",
                validTargetUuids,
                false,
                chosenCardUuid -> {
                    if (chosenCardUuid == null) return;
                    Card chosenCard = myPlayer.getCurrentDeck().getCardList().stream()
                            .filter(c -> c.getUuid().equals(chosenCardUuid) && c.getZoneId() == ZoneId.HAND)
                            .findFirst()
                            .orElse(null);

                    if (chosenCard != null) {
                        System.out.println("-> " + myPlayer.getName() + " jogou a Arma " + chosenCard.getCardName() +
                                " (Empunhadura " + empunhadura + ") da mão!");
                        gameEngine.playCardIntoResolution(chosenCard);
                    }
                }
        );
    }
}

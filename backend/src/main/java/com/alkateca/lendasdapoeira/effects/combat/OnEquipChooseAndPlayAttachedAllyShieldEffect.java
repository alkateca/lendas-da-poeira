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

public class OnEquipChooseAndPlayAttachedAllyShieldEffect implements Effect {

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        Player myPlayer = isPlayer1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();
        if (myPlayer == null || myPlayer.getCurrentDeck() == null || myPlayer.getCurrentDeck().getCardList() == null) {
            return;
        }

        List<Card> myCards = myPlayer.getCurrentDeck().getCardList();
        List<UUID> validShieldUuids = myCards.stream()
                .filter(c -> c.getZoneId() == ZoneId.ATTACHED && c.getItemType() == ItemType.ESCUDO)
                .map(Card::getUuid)
                .collect(Collectors.toList());

        gameEngine.requestPlayerChoice(
                myPlayer.getUuid(),
                sourceCard,
                "ATTACHED",
                "Escolha 1 Escudo anexado a um Herói Aliado para jogar",
                validShieldUuids,
                false,
                chosenShieldUuid -> {
                    if (chosenShieldUuid == null) return;
                    Card chosenShield = myCards.stream()
                            .filter(c -> c.getUuid().equals(chosenShieldUuid)
                                    && c.getZoneId() == ZoneId.ATTACHED
                                    && c.getItemType() == ItemType.ESCUDO)
                            .findFirst()
                            .orElse(null);

                    if (chosenShield != null) {
                        gameEngine.detachCard(chosenShield, ZoneId.RESOLVE);
                        System.out.println("-> Escudo " + chosenShield.getCardName() +
                                " foi re-jogado por " + sourceCard.getCardName() + "!");
                        gameEngine.playCardIntoResolution(chosenShield);
                    }
                }
        );
    }
}

package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.Raca;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.List;
import java.util.UUID;

public class PlaySpellPlayItemFromHandIfRaceEffect implements Effect {

    private final Raca requiredRace;

    public PlaySpellPlayItemFromHandIfRaceEffect(Raca requiredRace) {
        this.requiredRace = requiredRace;
    }

    public Raca getRequiredRace() {
        return requiredRace;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();

        Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (myPlayer == null || myPlayer.getCurrentDeck() == null
                || myPlayer.getCurrentDeck().getHeroList() == null
                || myPlayer.getCurrentDeck().getCardList() == null) {
            return;
        }

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        if (myHero == null || myHero.getRacas() == null || !myHero.getRacas().contains(requiredRace)) {
            System.out.println("-> O Herói Aliado ativo não pertence à raça " + requiredRace + ". Efeito ignorado.");
            return;
        }

        List<Card> itemsInHand = myPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c != null
                        && c.getZoneId() == ZoneId.HAND
                        && c.getCardType() == CardType.ITEM)
                .toList();

        if (itemsInHand.isEmpty()) {
            System.out.println("-> Nenhum Item disponível na mão de " + myPlayer.getName() + ".");
            return;
        }

        List<UUID> validTargetUuids = itemsInHand.stream()
                .map(Card::getUuid)
                .toList();

        gameEngine.requestPlayerChoice(
                myPlayer.getUuid(),
                sourceCard,
                "HAND_ITEM",
                "Escolha 1 Item da sua mão para jogar:",
                validTargetUuids,
                true,
                chosenCardUuid -> {
                    if (chosenCardUuid == null) return;
                    Card chosenItem = myPlayer.getCurrentDeck().getCardList().stream()
                            .filter(c -> chosenCardUuid.equals(c.getUuid()) && c.getZoneId() == ZoneId.HAND)
                            .findFirst()
                            .orElse(null);

                    if (chosenItem != null) {
                        System.out.println("-> " + chosenItem.getCardName()
                                + " foi jogado da mão para a fila de resolução!");
                        gameEngine.playCardIntoResolution(chosenItem);
                    }
                }
        );
    }
}

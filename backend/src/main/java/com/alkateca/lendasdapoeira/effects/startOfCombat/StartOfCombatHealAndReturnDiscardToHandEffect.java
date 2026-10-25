package com.alkateca.lendasdapoeira.effects.startOfCombat;

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

public class StartOfCombatHealAndReturnDiscardToHandEffect implements Effect {

    private final int healAmount;
    private final int maxCardsFromDiscard;

    public StartOfCombatHealAndReturnDiscardToHandEffect(int healAmount, int maxCardsFromDiscard) {
        this.healAmount = healAmount;
        this.maxCardsFromDiscard = maxCardsFromDiscard;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_START;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();

        if (myHeroId == null || !myHeroId.equals(sourceCard.getUuid())) {
            return;
        }

        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (myPlayer == null || myPlayer.getCurrentDeck() == null) {
            return;
        }

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        if (myHero != null && Boolean.TRUE.equals(myHero.getEstaVivo())) {
            int currentLife = myHero.getVidaAtual();
            int maxLife = myHero.getVidaMaxima();
            int newLife = Math.min(currentLife + healAmount, maxLife);
            myHero.setVidaAtual(newLife);
            System.out.println("-> " + myHero.getCardName() + " recuperou " + (newLife - currentLife) +
                    " de Vida! (Vida atual: " + myHero.getVidaAtual() + ")");

            if (newLife > currentLife) {
                gameEngine.captureSnapshot(sourceCard, this, "EFFECT_RESOLVED");
            }
        }

        requestNextDiscardCard(gameEngine, sourceCard, myPlayer, maxCardsFromDiscard);
    }

    private void requestNextDiscardCard(
            GameEngine gameEngine,
            Card sourceCard,
            Player myPlayer,
            int remaining) {
        if (remaining <= 0 || myPlayer.getCurrentDeck().getCardList() == null) {
            return;
        }

        List<UUID> validDiscardUuids = myPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.DISCARD)
                .map(Card::getUuid)
                .collect(Collectors.toList());

        if (validDiscardUuids.isEmpty()) {
            System.out.println("-> Nenhuma carta no descarte para retornar à mão.");
            return;
        }

        gameEngine.requestPlayerChoice(
                myPlayer.getUuid(),
                sourceCard,
                "DISCARD",
                "Escolha até " + remaining + " carta(s) do seu descarte para adicionar à sua mão",
                validDiscardUuids,
                true,
                chosenCardUuid -> {
                    if (chosenCardUuid == null) return;

                    Card chosenCard = myPlayer.getCurrentDeck().getCardList().stream()
                            .filter(c -> c.getUuid().equals(chosenCardUuid) && c.getZoneId() == ZoneId.DISCARD)
                            .findFirst()
                            .orElse(null);

                    if (chosenCard != null) {
                        chosenCard.setZoneId(ZoneId.HAND);
                        System.out.println("-> " + chosenCard.getCardName() + " retornou do descarte para a mão de "
                                + myPlayer.getName() + "!");

                        if (remaining - 1 > 0) {
                            requestNextDiscardCard(gameEngine, sourceCard, myPlayer, remaining - 1);
                        }
                    }
                }
        );
    }
}

package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.List;
import java.util.UUID;

public class OnPlayCardTypeInvokeCardEffect implements Effect {

    private final ItemType triggerType;
    private final String cardToInvoke;

    public OnPlayCardTypeInvokeCardEffect(ItemType triggerType, String cardToInvoke) {
        this.triggerType = triggerType;
        this.cardToInvoke = cardToInvoke;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public boolean shouldCaptureSnapshot() {
        return false;
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

        List<Card> playedCards = isPlayer1
                ? gameEngine.getGameState().getPlayer1PlayedCards()
                : gameEngine.getGameState().getPlayer2PlayedCards();

        long triggerCount = playedCards.stream()
                .filter(c -> c.getItemType() == triggerType)
                .count();

        if (triggerCount > 0) {
            System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

            Player myPlayer = isPlayer1
                    ? gameEngine.getPlayer1()
                    : gameEngine.getPlayer2();

            HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                    .filter(h -> h.getUuid().equals(myHeroId))
                    .findFirst()
                    .orElse(null);

            gameEngine.captureSnapshot(sourceCard, this, "EFFECT_RESOLVED");

            for (int i = 0; i < triggerCount; i++) {
                System.out.println("-> Ao jogar " + triggerType.name() + ", " + sourceCard.getCardName() +
                        " invocou: " + cardToInvoke + "!");
                gameEngine.invokeCard(myPlayer, myHero, cardToInvoke);
            }
        }
    }
}

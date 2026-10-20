package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class OnPlayCardTypeInvokeCardEffect implements Effect {

    private final ItemType triggerType;
    private final CardType triggerCardType;
    private final String cardToInvoke;
    private final Set<UUID> processedCardIds = new HashSet<>();

    public OnPlayCardTypeInvokeCardEffect(ItemType triggerType, String cardToInvoke) {
        this.triggerType = triggerType;
        this.triggerCardType = null;
        this.cardToInvoke = cardToInvoke;
    }

    public OnPlayCardTypeInvokeCardEffect(CardType triggerCardType, String cardToInvoke) {
        this.triggerType = null;
        this.triggerCardType = triggerCardType;
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

    private boolean matchesTrigger(Card card) {
        if (card == null) return false;
        if (triggerType != null) {
            return card.getItemType() == triggerType;
        }
        return triggerCardType != null && card.getCardType() == triggerCardType;
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

        List<Card> resolvedCards = gameEngine.getResolvedPlayedCardsForPlayer(sourceCard.getOwnerId());
        List<Card> newMatchingCards = resolvedCards.stream()
                .filter(c -> matchesTrigger(c) && !processedCardIds.contains(c.getUuid()))
                .toList();

        if (!newMatchingCards.isEmpty()) {
            System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

            for (Card c : newMatchingCards) {
                processedCardIds.add(c.getUuid());
            }
            gameEngine.registerCombatEndCleanup(processedCardIds::clear);

            Player myPlayer = isPlayer1
                    ? gameEngine.getPlayer1()
                    : gameEngine.getPlayer2();

            HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                    .filter(h -> h.getUuid().equals(myHeroId))
                    .findFirst()
                    .orElse(null);

            gameEngine.captureSnapshot(sourceCard, this, "EFFECT_RESOLVED");

            String triggerLabel = triggerType != null ? triggerType.name() : triggerCardType.name();
            for (int i = 0; i < newMatchingCards.size(); i++) {
                System.out.println("-> Ao jogar " + triggerLabel + ", " + sourceCard.getCardName() +
                        " invocou: " + cardToInvoke + "!");
                gameEngine.invokeCard(myPlayer, myHero, cardToInvoke);
            }
        }
    }
}

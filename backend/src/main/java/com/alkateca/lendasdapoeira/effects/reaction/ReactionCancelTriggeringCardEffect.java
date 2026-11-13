package com.alkateca.lendasdapoeira.effects.reaction;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;

public class ReactionCancelTriggeringCardEffect implements ReactionEffect {

    private final ReactionTriggerType triggerType;

    public ReactionCancelTriggeringCardEffect() {
        this(ReactionTriggerType.ON_ENEMY_PLAY_ANY_CARD);
    }

    public ReactionCancelTriggeringCardEffect(ReactionTriggerType triggerType) {
        this.triggerType = triggerType != null ? triggerType : ReactionTriggerType.ON_ENEMY_PLAY_ANY_CARD;
    }

    @Override
    public boolean canReact(GameEngine gameEngine, Card reactionCard, Card triggeringCard, Effect triggeringEffect) {
        if (reactionCard == null || triggeringCard == null) {
            return false;
        }
        if (reactionCard.getOwnerId() != null && reactionCard.getOwnerId().equals(triggeringCard.getOwnerId())) {
            return false;
        }
        return triggerType.matches(triggeringCard, triggeringEffect);
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");
        Card triggerCard = gameEngine.getCurrentReactionTriggerCard();
        if (triggerCard != null) {
            gameEngine.cancelOrDiscardCardInPlay(triggerCard);
            System.out.println("-> Reação [" + sourceCard.getCardName() + "] anulou e descartou '"
                    + triggerCard.getCardName() + "'!");
        }
    }
}

package com.alkateca.lendasdapoeira.effects.reaction;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import lombok.Getter;

@Getter
public class TriggeredReactionEffect implements ReactionEffect {

    private final ReactionTriggerType triggerType;
    private final Effect innerEffect;
    private final boolean proactiveAllowed;

    public TriggeredReactionEffect(ReactionTriggerType triggerType, Effect innerEffect) {
        this(triggerType, innerEffect, false);
    }

    public TriggeredReactionEffect(ReactionTriggerType triggerType, Effect innerEffect, boolean proactiveAllowed) {
        this.triggerType = triggerType;
        this.innerEffect = innerEffect;
        this.proactiveAllowed = proactiveAllowed;
    }

    @Override
    public boolean canReact(GameEngine gameEngine, Card reactionCard, Card triggeringCard, Effect triggeringEffect) {
        if (reactionCard == null || triggeringCard == null) {
            return false;
        }
        if (reactionCard.getOwnerId() != null && reactionCard.getOwnerId().equals(triggeringCard.getOwnerId())) {
            return false;
        }
        return triggerType != null && triggerType.matches(triggeringCard, triggeringEffect);
    }

    @Override
    public boolean canPlayProactively() {
        return proactiveAllowed;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        if (innerEffect != null) {
            innerEffect.resolve(gameEngine, sourceCard);
        }
    }

    @Override
    public void onDetach(GameEngine gameEngine, Card sourceCard, HeroCard targetHero) {
        if (innerEffect != null) {
            innerEffect.onDetach(gameEngine, sourceCard, targetHero);
        }
    }

    @Override
    public boolean shouldCaptureSnapshot() {
        return innerEffect == null || innerEffect.shouldCaptureSnapshot();
    }
}

package com.alkateca.lendasdapoeira.effects.reaction;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.enums.CardType;

public enum ReactionTriggerType {
    ON_ENEMY_PLAY_ANY_CARD,
    ON_ENEMY_PLAY_SPELL,
    ON_ENEMY_PLAY_ACTION,
    ON_ENEMY_PLAY_ITEM,
    ON_ENEMY_PLAY_THEURGY,
    ON_ENEMY_PLAY_PET,
    ON_ENEMY_DAMAGE_EFFECT;

    public boolean matches(Card triggeringCard, Effect triggeringEffect) {
        if (triggeringCard == null) {
            return false;
        }
        return switch (this) {
            case ON_ENEMY_PLAY_ANY_CARD -> triggeringCard.getCardType() != CardType.HERO;
            case ON_ENEMY_PLAY_SPELL -> triggeringCard.getCardType() == CardType.SPELL;
            case ON_ENEMY_PLAY_ACTION -> triggeringCard.getCardType() == CardType.ACTION;
            case ON_ENEMY_PLAY_ITEM -> triggeringCard.getCardType() == CardType.ITEM;
            case ON_ENEMY_PLAY_THEURGY -> triggeringCard.getCardType() == CardType.THEURGY;
            case ON_ENEMY_PLAY_PET -> triggeringCard.getCardType() == CardType.PET;
            case ON_ENEMY_DAMAGE_EFFECT -> {
                if (triggeringEffect != null && isDamageEffect(triggeringEffect)) {
                    yield true;
                }
                if (triggeringCard.getEffects() != null) {
                    yield triggeringCard.getEffects().stream().anyMatch(ReactionTriggerType::isDamageEffect);
                }
                yield false;
            }
        };
    }

    private static boolean isDamageEffect(Effect eff) {
        if (eff == null) return false;
        String name = eff.getClass().getSimpleName();
        return name.contains("Damage") || name.contains("Impact");
    }
}

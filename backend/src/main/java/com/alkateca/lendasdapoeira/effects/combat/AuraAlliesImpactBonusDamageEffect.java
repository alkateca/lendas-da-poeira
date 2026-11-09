package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import lombok.Getter;

@Getter
public class AuraAlliesImpactBonusDamageEffect implements Effect {

    private final int bonusImpactDamage;

    public AuraAlliesImpactBonusDamageEffect(int bonusImpactDamage) {
        this.bonusImpactDamage = bonusImpactDamage;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return false;
    }

    @Override
    public boolean shouldCaptureSnapshot() {
        return false;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        // Aura passiva consultada dinamicamente pelo GameEngine ao resolver efeitos de Impacto.
    }
}

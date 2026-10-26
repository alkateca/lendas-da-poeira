package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import lombok.Getter;

@Getter
public class AuraGoblinEffectAmplifierEffect implements Effect {

    private final int bonusEffectValue;

    public AuraGoblinEffectAmplifierEffect(int bonusEffectValue) {
        this.bonusEffectValue = bonusEffectValue;
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
        // Aura passiva consultada dinamicamente pelo GameEngine enquanto o portador estiver vivo.
    }
}

package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import lombok.Getter;

@Getter
public class AuraBonusDamageTakenByEnemyEffect implements Effect {

    private final int extraDamage;

    public AuraBonusDamageTakenByEnemyEffect(int extraDamage) {
        this.extraDamage = extraDamage;
    }

    @Override
    public boolean canTriggerFromBench() {
        return true;
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
        // Aura passiva consultada dinamicamente pelo GameEngine quando um Herói Inimigo sofre dano.
    }
}

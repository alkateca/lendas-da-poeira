package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class AuraEnemyDeadBelowZeroEffect implements Effect {

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
        gameEngine.evaluateInstantDeathByAura();
    }
}

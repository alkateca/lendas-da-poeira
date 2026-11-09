package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import lombok.Getter;

@Getter
public class OnImpactHealHeroEffect implements Effect {

    private final int healAmount;

    public OnImpactHealHeroEffect(int healAmount) {
        this.healAmount = healAmount;
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
        // Disparado dinamicamente pelo GameEngine sempre que um efeito de Impacto é ativado pelo Herói portador.
    }
}

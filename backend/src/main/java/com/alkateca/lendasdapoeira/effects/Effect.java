package com.alkateca.lendasdapoeira.effects;

import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public interface Effect {

    boolean predicate(Card sourceCard, TurnPhase turnPhase);

    void resolve(GameEngine gameEngine, Card sourceCard);

    /**
     * Chamado automaticamente pelo GameEngine quando uma carta deixa a zona ATTACHED.
     */
    default void onDetach(GameEngine gameEngine, Card sourceCard, HeroCard targetHero) {
        // Implementação padrão vazia para efeitos que não precisam reverter status ao desanexar
    }

    default boolean shouldCaptureSnapshot() {
        return true;
    }
}
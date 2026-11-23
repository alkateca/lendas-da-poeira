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

    default boolean canTriggerFromBench() {
        return false;
    }

    /**
     * Indica se este efeito (em um Herói ou carta já em {@code ZoneId.ATTACHED}) deve ser acionado
     * automaticamente uma vez na transição para {@code TurnPhase.RESOLUTION}, independente de haver
     * cartas jogadas na pilha. Efeitos do tipo "ao jogar" / "ao equipar" permanecem como {@code false}.
     */
    default boolean canTriggerOnResolutionPhaseEntry() {
        return false;
    }

    /**
     * Indica se este efeito em uma carta ainda na zona {@code ZoneId.RESOLVE} deve disparar
     * quando o motor entrar em fases posteriores de combate ({@code COMBAT_START}, {@code COMBAT}, {@code COMBAT_END}).
     */
    default boolean canTriggerFromResolveZone(TurnPhase turnPhase) {
        return false;
    }
}

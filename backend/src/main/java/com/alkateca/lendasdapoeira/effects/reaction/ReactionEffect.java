package com.alkateca.lendasdapoeira.effects.reaction;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public interface ReactionEffect extends Effect {

    /**
     * Avalia se esta carta de reação pode ser ativada em resposta à carta ou efeito do oponente.
     */
    boolean canReact(GameEngine gameEngine, Card reactionCard, Card triggeringCard, Effect triggeringEffect);

    /**
     * Define se a carta pode ser jogada proativamente numa janela normal de resolução
     * além de atuar como reação. Por padrão, cartas de reação só ativam em resposta ao gatilho.
     */
    default boolean canPlayProactively() {
        return false;
    }

    @Override
    default boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }
}

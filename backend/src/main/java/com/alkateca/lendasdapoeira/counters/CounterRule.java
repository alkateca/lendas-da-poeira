package com.alkateca.lendasdapoeira.counters;

import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.CounterType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

/**
 * Contrato desacoplado de regra sistêmica associada a um {@link CounterType}.
 * Novos tipos de token com comportamento passivo, reativo em cálculo de dano ou por fase do turno
 * podem ser plugados implementando esta interface e registrando no {@link CounterRuleRegistry}.
 */
public interface CounterRule {

    CounterType getCounterType();

    /**
     * Bônus ou modificador aplicado durante o cálculo de dano mágico antes da mitigação por Espírito.
     */
    default int modifyMagicDamage(GameEngine engine, HeroCard attacker, HeroCard defender, int baseDamage, Card sourceCard) {
        return 0;
    }

    /**
     * Bônus ou modificador aplicado durante o cálculo de dano físico antes da mitigação por Defesa.
     */
    default int modifyPhysicalDamage(GameEngine engine, HeroCard attacker, HeroCard defender, int baseDamage, Card sourceCard) {
        return 0;
    }

    /**
     * Bônus ou modificador aplicado durante o cálculo de dano direto.
     */
    default int modifyDirectDamage(GameEngine engine, HeroCard attacker, HeroCard defender, int baseDamage, Card sourceCard) {
        return 0;
    }

    /**
     * Gancho opcional executado quando o motor entra em uma {@link TurnPhase}.
     */
    default void onTurnPhase(GameEngine engine, TurnPhase phase) {
        // Sem comportamento periódico por padrão
    }
}

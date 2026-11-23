package com.alkateca.lendasdapoeira.counters;

import com.alkateca.lendasdapoeira.enums.CounterType;

/**
 * Regra do marcador {@link CounterType#SEDA}:
 * recurso acumulável não detonado automaticamente no dano; é consumido explicitamente por
 * cartas e habilidades que ampliam seus efeitos (bônus de ataque, defesa, espírito, cura ou dano).
 */
public class SedaCounterRule implements CounterRule {

    @Override
    public CounterType getCounterType() {
        return CounterType.SEDA;
    }
}

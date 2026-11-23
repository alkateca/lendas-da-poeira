package com.alkateca.lendasdapoeira.counters;

/**
 * Estratégias padronizadas para consumo de contadores (tokens/marcadores) em cartas e Heróis.
 */
public enum CounterConsumptionMode {
    /**
     * Consome todos os marcadores disponíveis daquele tipo na carta/herói alvo.
     */
    CONSUME_ALL,

    /**
     * Consome até {@code requestedAmount} marcadores disponíveis (entre 0 e o limite).
     */
    CONSUME_UP_TO,

    /**
     * Consome exatamente {@code requestedAmount} marcadores apenas se o alvo possuir saldo suficiente;
     * caso contrário, não consome nenhum (retorna 0).
     */
    CONSUME_EXACT
}

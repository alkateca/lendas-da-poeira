package com.alkateca.lendasdapoeira.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Raca {
    HUMANO("Humano"),
    GOBLIN("Goblin"),
    CONSTRUCTO("Constructo"),
    QUIMERA("Quimera"),
    ZUMBI("Zumbi"),
    CRISTAL("Cristal"),
    CAVALEIRO("Cavaleiro"),
    GUARDIOES_DO_TRONO("Guardiões do Trono");

    private final String nomeFormatado;

    Raca(String nomeFormatado) {
        this.nomeFormatado = nomeFormatado;
    }

    @JsonValue
    public String getNomeFormatado() {
        return nomeFormatado;
    }

    @com.fasterxml.jackson.annotation.JsonCreator
    public static Raca fromValue(String value) {
        if (value == null) return null;
        for (Raca r : Raca.values()) {
            if (r.nomeFormatado.equalsIgnoreCase(value) || r.name().equalsIgnoreCase(value)) {
                return r;
            }
        }
        if ("Guardião do Trono".equalsIgnoreCase(value)) {
            return GUARDIOES_DO_TRONO;
        }
        throw new IllegalArgumentException("Raça desconhecida: " + value);
    }
}
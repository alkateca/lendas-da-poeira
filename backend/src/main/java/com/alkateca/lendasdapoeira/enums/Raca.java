package com.alkateca.lendasdapoeira.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Raca {
    GOBLIN("Goblin"),
    CONSTRUCTO("Constructo");

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
        throw new IllegalArgumentException("Raça desconhecida: " + value);
    }
}
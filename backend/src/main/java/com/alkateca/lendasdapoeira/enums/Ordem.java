package com.alkateca.lendasdapoeira.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Ordem {
    CAVALEIRO("Cavaleiro"),
    CRISTAL("Cristal"),
    ZUMBI("Zumbi"),
    GUARDIAO_DO_TRONO("Guardião do Trono");

    private final String nomeFormatado;

    Ordem(String nomeFormatado) {
        this.nomeFormatado = nomeFormatado;
    }

    @JsonValue
    public String getNomeFormatado() {
        return nomeFormatado;
    }

    @JsonCreator
    public static Ordem fromValue(String value) {
        if (value == null) return null;
        for (Ordem o : Ordem.values()) {
            if (o.nomeFormatado.equalsIgnoreCase(value) || o.name().equalsIgnoreCase(value)) {
                return o;
            }
        }
        if ("Guardiões do Trono".equalsIgnoreCase(value)) {
            return GUARDIAO_DO_TRONO;
        }
        throw new IllegalArgumentException("Ordem desconhecida: " + value);
    }
}
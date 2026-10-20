package com.alkateca.lendasdapoeira.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Ordem {
    CAVALEIRO("Cavaleiro"),
    CRISTAL("Cristal"),
    ZUMBI("Zumbi"),
    GUARDAO_DO_TRONO("Guardião do Trono");

    private final String nomeFormatado;

    Ordem(String nomeFormatado) {
        this.nomeFormatado = nomeFormatado;
    }

    @JsonValue
    public String getNomeFormatado() {
        return nomeFormatado;
    }
}
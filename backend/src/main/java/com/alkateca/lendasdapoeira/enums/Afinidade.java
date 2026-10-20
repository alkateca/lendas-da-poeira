package com.alkateca.lendasdapoeira.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Afinidade {
    CRISTAL("Cristal"),
    AR("Ar"),
    AGUA("Agua"),
    TERRA("Terra"),
    FOGO("Fogo");

    private final String nomeFormatado;

    Afinidade(String nomeFormatado) {
        this.nomeFormatado = nomeFormatado;
    }

    @JsonValue
    public String getNomeFormatado() {
        return nomeFormatado;
    }
}
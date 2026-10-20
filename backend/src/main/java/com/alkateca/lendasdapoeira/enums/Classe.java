package com.alkateca.lendasdapoeira.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum Classe {
    CRIADOR("Criador"),
    EMISSOR("Emissor"),
    TRANSFORMADOR("Transformador");

    private final String nomeFormatado;

    Classe(String nomeFormatado) {
        this.nomeFormatado = nomeFormatado;
    }

    @JsonValue
    public String getNomeFormatado() {
        return nomeFormatado;
    }
}
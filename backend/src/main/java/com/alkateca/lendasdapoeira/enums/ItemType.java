package com.alkateca.lendasdapoeira.enums;

public enum ItemType {
    ARMA,
    ARMADURA,
    ESCUDO,
    JOIA,
    FERRAMENTA,
    NENHUM;

    public static String nomeOuPadrao(ItemType tipo, boolean visivel) {
        return (visivel && tipo != null) ? tipo.name() : NENHUM.name();
    }
}

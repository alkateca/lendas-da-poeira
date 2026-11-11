package com.alkateca.lendasdapoeira.enums;

public enum ItemType {
    ARMA,
    ARMADURA,
    ESCUDO,
    JOIA,
    FERRAMENTA,
    PET,
    NENHUM;

    public boolean isSingleSlotReplaceable() {
        return this == ARMADURA || this == ESCUDO || this == PET;
    }

    public static String nomeOuPadrao(ItemType tipo, boolean visivel) {
        return (visivel && tipo != null) ? tipo.name() : NENHUM.name();
    }
}

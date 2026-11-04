package com.alkateca.lendasdapoeira.DTO;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class CardDTO {
    private String id;
    private String name;
    private String type;
    private String zone;
    private String ownerId;
    private String rarity;
    private String cardSet;

    // Atributos numéricos de Herói
    private Integer hp;
    private Integer maxHp;
    private Integer attack;
    private Integer defense;
    private Integer spirit;

    // NOVOS CAMPOS: Bônus calculados pelo Back-End para Cartas Equipáveis / Buffs Anexáveis
    private boolean equipable;
    private int bonusEspirito;
    private int bonusAtaque;
    private int bonusDefesa;
    private int bonusVidaMaxima;
    private int empunhadura;

    // NOVO CAMPO: Chaves da trilha vertical que devem acender na UI
    // Valores possíveis: "ON_PLAY", "COMBAT_START", "AURA_COMBAT", "COMBAT_END"
    private List<String> activePhaseKeys = new ArrayList<>();

    private int attachedCount;
    private String description;
    private Boolean active;
    private List<String> keywords = new ArrayList<>();
}
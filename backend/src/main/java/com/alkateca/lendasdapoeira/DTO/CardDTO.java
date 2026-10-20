package com.alkateca.lendasdapoeira.DTO;

import lombok.Data;

@Data
public class CardDTO {
    private String id;
    private String name;
    private String type;
    private String zone;
    private String ownerId;

    // Atributos numéricos (Ficarão nulos se a carta for Item/Magia)
    private Integer hp;
    private Integer maxHp;
    private Integer attack;
    private Integer defense;
    private Integer spirit;

    // Para renderizar o badge de "X Itens" na interface
    private int attachedCount;
    
    private String description;
    
    private Boolean active;
}
package com.alkateca.lendasdapoeira.entity;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.engine.ResolutionQueue;
import com.alkateca.lendasdapoeira.enums.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.alkateca.lendasdapoeira.enums.Colors;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Card {

    private UUID uuid;
    private String cardName;
    private CardType cardType;
    private Colors color;
    private UUID ownerId;
    private ZoneId zoneId;
    private ItemType itemType = ItemType.NENHUM;
    private int empunhadura = 0;
    private List<Raca> racas;
    private List<Afinidade> afinidades;
    private List<Ordem> ordem;
    private List<Classe> classe;
    private String descricao;
    private boolean unica;


    private UUID attachedToCardId;

    private Integer turnsRemaining;

    private List<Effect> effects = new ArrayList<>();

    public void onPhaseChange(TurnPhase currentPhase, ResolutionQueue queue) {
        if (effects == null) return;

        for (Effect effect : effects) {
            if (effect.predicate(this, currentPhase)) {
                queue.enqueue(effect, this);
            }
        }
    }

}

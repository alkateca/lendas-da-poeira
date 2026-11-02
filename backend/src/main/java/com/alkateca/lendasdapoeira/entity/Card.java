package com.alkateca.lendasdapoeira.entity;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.ResolutionQueue;
import com.alkateca.lendasdapoeira.enums.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Card {

    private UUID uuid;
    private String cardName;
    private CardType cardType;
    private Colors color;
    private CardRarity rarity;
    private CardSet cardSet;
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
    private List<String> keywords = new ArrayList<>();

    private UUID attachedToCardId;
    private Integer turnsRemaining;

    // Metadados do Sistema de Liberação (Missão / Evolução)
    private boolean liberationCard = false;
    private String missionType;          // Ex: "DEAL_DAMAGE", "SURVIVE_TURNS", "PLAY_SPELL", "PLAY_AFFINITY_FOGO", "DEFEAT_HERO"
    private int missionTargetValue = 0;
    private int missionCurrentProgress = 0;
    private String targetBaseHeroName;   // Ex: "Moyra, Aprendiz da Santa"
    private String evolvesIntoHeroName;  // Ex: "Moyra, Santa das Laminas"

    @JsonIgnore
    private List<Effect> effects = new ArrayList<>();

    public void onPhaseChange(TurnPhase currentPhase, ResolutionQueue queue) {
        if (effects == null) return;

        for (Effect effect : effects) {
            if (effect.predicate(this, currentPhase)) {
                queue.enqueue(effect, this);
            }
        }
    }

    // Adicionar dentro de src/main/java/com/alkateca/lendasdapoeira/entity/Card.java:

    @JsonIgnore
    public boolean isEquipable() {
        if (this.cardType == CardType.ITEM || (this.itemType != null && this.itemType != ItemType.NENHUM)) {
            return true;
        }
        return this.effects != null && this.effects.stream()
                .anyMatch(e -> e instanceof com.alkateca.lendasdapoeira.effects.combat.EquipBuffEffect);
    }

    @JsonIgnore
    public int getBonusEspirito() {
        if (this.effects == null) return 0;
        return this.effects.stream()
                .filter(e -> e instanceof com.alkateca.lendasdapoeira.effects.combat.EquipBuffEffect)
                .mapToInt(e -> ((com.alkateca.lendasdapoeira.effects.combat.EquipBuffEffect) e).getBonusEspirito())
                .sum();
    }

    @JsonIgnore
    public int getBonusAtaque() {
        if (this.effects == null) return 0;
        return this.effects.stream()
                .filter(e -> e instanceof com.alkateca.lendasdapoeira.effects.combat.EquipBuffEffect)
                .mapToInt(e -> ((com.alkateca.lendasdapoeira.effects.combat.EquipBuffEffect) e).getBonusAtaque())
                .sum();
    }

    @JsonIgnore
    public int getBonusDefesa() {
        if (this.effects == null) return 0;
        return this.effects.stream()
                .filter(e -> e instanceof com.alkateca.lendasdapoeira.effects.combat.EquipBuffEffect)
                .mapToInt(e -> ((com.alkateca.lendasdapoeira.effects.combat.EquipBuffEffect) e).getBonusDefesa())
                .sum();
    }

    @JsonIgnore
    public java.util.List<String> getActivePhaseKeys() {
        java.util.List<String> phases = new java.util.ArrayList<>();
        if (this.effects == null) return phases;

        for (com.alkateca.lendasdapoeira.effects.Effect effect : this.effects) {
            if (effect instanceof com.alkateca.lendasdapoeira.effects.combat.EquipBuffEffect) {
                continue;
            }
            if (effect.predicate(this, TurnPhase.COMBAT_START) && !phases.contains("COMBAT_START")) {
                phases.add("COMBAT_START");
            } else if (effect.predicate(this, TurnPhase.COMBAT_END) && !phases.contains("COMBAT_END")) {
                phases.add("COMBAT_END");
            } else if (effect.predicate(this, TurnPhase.COMBAT) && !phases.contains("AURA_COMBAT")) {
                phases.add("AURA_COMBAT");
            } else if (effect.predicate(this, TurnPhase.RESOLUTION) && !phases.contains("ON_PLAY")) {
                phases.add("ON_PLAY");
            }
        }
        return phases;
    }
}

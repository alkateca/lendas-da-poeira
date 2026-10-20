package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.DTO.MatchDtos;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MatchSnapshotDTO {
    private long snapshotSequence;
    private TurnPhase turnPhase;
    private UUID activeTurnPlayerId;
    private boolean waitingForMainButton;
    private TriggerContextDTO triggerContext;

    private PlayerViewDTO localPlayer;
    private PlayerViewDTO opponentPlayer;

    private MatchDtos.PendingChoiceDTO pendingChoice;



    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    public static class PendingChoiceDTO extends MatchDtos.PendingChoiceDTO {
        private UUID choosingPlayerId;      // Quem deve escolher (bloqueia o outro jogador com "Aguardando oponente...")
        private UUID sourceCardUuid;        // Carta que gerou a pergunta
        private String sourceCardName;
        private String targetType;          // "HAND", "DISCARD", "DECK", "ALLY_HERO", "ENEMY_HERO", "ATTACHED"
        private String promptMessage;       // Ex: "Escolha 1 carta da sua mão para jogar"
        private boolean canSkip;            // Equivalente ao botão "Pular Efeito" do Lua
        private List<UUID> validTargetUuids = new ArrayList<>(); // UUIDs exatos que podem ser clicados
    }

    @Data
    public static class TriggerContextDTO {
        private String eventType; // "PHASE_CHANGE", "EFFECT_RESOLVED", "PHYSICAL_COMBAT_EXCHANGE"
        private UUID sourceCardUuid;
        private String sourceCardName;
        private String effectClassName;
        private String logMessage;
    }

    @Data
    public static class PlayerViewDTO {
        private UUID playerId;
        private String name;
        private int deckCount;
        private int handCount;
        private UUID activeHeroId;
        private List<HeroSnapshotDTO> heroes = new ArrayList<>();
        private List<CardSnapshotDTO> hand = new ArrayList<>();
        private List<CardSnapshotDTO> playedCardsInResolve = new ArrayList<>();
        private List<CardSnapshotDTO> discardPile = new ArrayList<>();
        private List<CardSnapshotDTO> exilePile = new ArrayList<>();
    }

    @Data
    public static class HeroSnapshotDTO {
        private UUID uuid;
        private String cardName;
        private String color;
        private String descricao;
        private String zoneId;
        private int espirito;
        private int ataque;
        private int defesa;
        private int vidaMaxima;
        private int vidaAtual;
        private int maxWeaponSlots;
        private int usedWeaponSlots;
        private int reducaoDano;
        private int danoBonus;
        private int vulnerabilidade;
        private List<String> racas = new ArrayList<>();
        private List<String> afinidades = new ArrayList<>();
        private List<String> ordem = new ArrayList<>();
        private List<String> classe = new ArrayList<>();
        private Boolean estaVivo;
        private Boolean estaAtivo;
        private List<CardSnapshotDTO> attachedCards = new ArrayList<>();
    }

    @Data
    public static class CardSnapshotDTO {
        private UUID uuid;
        private String cardName;
        private String cardType;
        private String color;
        private String itemType;
        private int empunhadura;
        private String descricao;
        private String zoneId;
        private boolean faceUp;
    }
}
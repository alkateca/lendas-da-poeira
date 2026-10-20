package com.alkateca.lendasdapoeira.DTO;

import com.alkateca.lendasdapoeira.engine.MatchSnapshotDTO;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MatchDtos {

    private PendingChoiceDTO pendingChoice;

    @Data
    public static class PendingChoiceDTO {
        private UUID choosingPlayerId;      // Quem deve escolher (bloqueia o outro jogador com "Aguardando oponente...")
        private UUID sourceCardUuid;        // Carta que gerou a pergunta
        private String sourceCardName;
        private String targetType;          // "HAND", "DISCARD", "DECK", "ALLY_HERO", "ENEMY_HERO", "ATTACHED"
        private String promptMessage;       // Ex: "Escolha 1 carta da sua mão para jogar"
        private boolean canSkip;            // Equivalente ao botão "Pular Efeito" do Lua
        private List<UUID> validTargetUuids = new ArrayList<>(); // UUIDs exatos que podem ser clicados
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JoinQueueRequestDTO {
        private UUID playerId;
        private String playerName;
        private Deck currentDeck; // Permite receber o baralho ativo do Front-End
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchmakingStatusResponseDTO {
        private String status; // "WAITING" ou "MATCH_FOUND"
        private UUID matchId;
        private UUID localPlayerId;
        private List<MatchSnapshotDTO> initialSnapshots;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerActionRequestDTO {
        private UUID matchId;
        private UUID playerId;
        private TurnPhase phase;
        private UUID selectedOwnHeroId;
        private UUID selectedEnemyHeroId;
        private List<UUID> playedCardUuids;
        private List<UUID> discardCardUuids;
        private UUID selectedTargetUuid;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchTimelineResponseDTO {
        private boolean waitingForOpponent;
        private List<MatchSnapshotDTO> snapshots;
    }
}
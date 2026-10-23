package com.alkateca.lendasdapoeira.DTO;

import com.alkateca.lendasdapoeira.engine.MatchSnapshotDTO;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.enums.MatchMode;
import com.alkateca.lendasdapoeira.enums.MatchStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MatchDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JoinQueueRequestDTO {
        private UUID playerId;
        private String playerName;
        private Deck currentDeck;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MatchmakingStatusResponseDTO {
        private String status; // "WAITING" ou "MATCH_FOUND"
        private UUID matchId;
        private UUID playerId;
        private MatchMode matchMode;
        private MatchStatus matchStatus;
        private List<MatchSnapshotDTO> snapshots = new ArrayList<>();

        // Construtor retrocompatível
        public MatchmakingStatusResponseDTO(String status, UUID matchId, UUID playerId, List<MatchSnapshotDTO> snapshots) {
            this.status = status;
            this.matchId = matchId;
            this.playerId = playerId;
            this.snapshots = snapshots;
        }

        // Aliases para retrocompatibilidade JSON com front-end atual
        public UUID getLocalPlayerId() {
            return this.playerId;
        }

        public List<MatchSnapshotDTO> getInitialSnapshots() {
            return this.snapshots;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlayerActionRequestDTO {
        private UUID matchId; // Opcional no body (validado contra o PathVariable)
        private UUID playerId;
        private UUID selectedOwnHeroId;
        private UUID selectedEnemyHeroId;
        private List<UUID> playedCardUuids = new ArrayList<>();
        private List<UUID> discardCardUuids = new ArrayList<>();
        private UUID selectedTargetUuid;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MatchTimelineResponseDTO {
        private UUID matchId;
        private MatchMode matchMode;
        private MatchStatus matchStatus;
        private boolean waitingForOpponent;
        private List<MatchSnapshotDTO> snapshots = new ArrayList<>();

        // Construtor retrocompatível
        public MatchTimelineResponseDTO(boolean waitingForOpponent, List<MatchSnapshotDTO> snapshots) {
            this.waitingForOpponent = waitingForOpponent;
            this.snapshots = snapshots;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PendingChoiceDTO {
        private UUID choosingPlayerId;
        private UUID sourceCardUuid;
        private String sourceCardName;
        private String targetType;
        private String promptMessage;
        private boolean canSkip;
        private List<UUID> validTargetUuids = new ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LobbyPlayerDTO {
        private UUID playerId;
        private String playerName;
        private int permanentElo;
        private boolean inQueue;
        /**
         * Status contextual em relação ao jogador que está visualizando a lista:
         * "NONE" -> Pode ser desafiado (exibe botão "Desafiar")
         * "CHALLENGE_SENT" -> Você já desafiou este jogador ("Desafio Pendente")
         * "CHALLENGE_RECEIVED" -> Este jogador desafiou você (exibe botão "Aceitar Combate")
         */
        private String challengeStatus;
        private UUID challengeId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DirectChallengeDTO {
        private UUID challengeId;
        private UUID challengerId;
        private String challengerName;
        private int challengerElo;
        private UUID targetId;
        private String targetName;
        private long createdAtEpochMilli;
        private long expiresAtEpochMilli;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChallengeRequestDTO {
        private UUID challengerId;
        private String challengerName;
        private Deck challengerDeck;
        private UUID targetPlayerId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AcceptChallengeRequestDTO {
        private UUID challengeId;
        private UUID targetPlayerId;
        private String targetPlayerName;
        private Deck targetDeck;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LobbyStateResponseDTO {
        private String status; // "IN_LOBBY", "IN_QUEUE" ou "MATCH_FOUND"
        private List<LobbyPlayerDTO> players = new ArrayList<>();
        private List<DirectChallengeDTO> incomingChallenges = new ArrayList<>();
        private List<DirectChallengeDTO> outgoingChallenges = new ArrayList<>();
        private MatchmakingStatusResponseDTO activeMatch; // Preenchido se status == "MATCH_FOUND"
    }
}

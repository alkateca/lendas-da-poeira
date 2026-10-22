package com.alkateca.lendasdapoeira.entity;

import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.enums.MatchMode;
import com.alkateca.lendasdapoeira.enums.MatchStatus;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Getter
@Setter
public class Match {

    private final UUID matchId;
    private final MatchMode mode;
    private volatile MatchStatus status;
    private final GameEngine gameEngine;
    private final Map<UUID, ParticipantSession> participants = new ConcurrentHashMap<>();
    private final Instant createdAt;
    private volatile Instant lastActivityAt;

    // Lock dedicado a esta partida para garantir thread-safety sem bloquear outras partidas
    private final Object matchLock = new Object();

    @Data
    @AllArgsConstructor
    public static class ParticipantSession {
        private UUID playerId;
        private String playerName;
        private boolean isBot;
        private String sessionId; // ID de sessão WebSocket/HTTP quando aplicável
    }

    public Match(UUID matchId, MatchMode mode, Player player1, Player player2, boolean isPlayer2Bot) {
        this.matchId = matchId != null ? matchId : UUID.randomUUID();
        this.mode = mode;
        this.status = MatchStatus.WAITING;
        this.createdAt = Instant.now();
        this.lastActivityAt = Instant.now();

        if (player1 != null) {
            registerParticipant(player1, false, null);
        }
        if (player2 != null) {
            registerParticipant(player2, isPlayer2Bot, isPlayer2Bot ? "BOT_SESSION" : null);
        }

        this.gameEngine = new GameEngine(player1, player2);
    }

    public void registerParticipant(Player player, boolean isBot, String sessionId) {
        participants.put(
                player.getUuid(),
                new ParticipantSession(player.getUuid(), player.getName(), isBot, sessionId)
        );
    }

    public void startMatch() {
        synchronized (matchLock) {
            this.status = MatchStatus.RUNNING;
            this.lastActivityAt = Instant.now();
            this.gameEngine.startGame();
            syncStatusWithEngine();
        }
    }

    /**
     * Executa qualquer comando na instância do GameEngine de forma thread-safe e isolada por partida.
     */
    public <T> T executeLocked(Supplier<T> action) {
        synchronized (matchLock) {
            this.lastActivityAt = Instant.now();
            T result = action.get();
            syncStatusWithEngine();
            return result;
        }
    }

    public void syncStatusWithEngine() {
        if (this.gameEngine != null && this.gameEngine.getTurnPhase() == TurnPhase.GAME_OVER) {
            this.status = MatchStatus.FINISHED;
        }
    }

    public boolean hasParticipant(UUID playerId) {
        return playerId != null && participants.containsKey(playerId);
    }

    public UUID getBotPlayerId() {
        return participants.values().stream()
                .filter(ParticipantSession::isBot)
                .map(ParticipantSession::getPlayerId)
                .findFirst()
                .orElse(null);
    }
}

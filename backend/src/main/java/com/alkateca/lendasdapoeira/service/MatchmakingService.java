package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.DTO.MatchDtos.*;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.engine.MatchSnapshotDTO;
import com.alkateca.lendasdapoeira.engine.SnapshotFactory;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Match;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class MatchmakingService {

    private static final Duration FINISHED_MATCH_RETENTION = Duration.ofMinutes(10);
    private static final Duration INACTIVE_MATCH_TIMEOUT = Duration.ofMinutes(60);
    private static final Duration LOBBY_PRESENCE_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration CHALLENGE_EXPIRATION = Duration.ofSeconds(60);

    public static class LobbyPresenceEntry {
        private Player player;
        private Instant lastSeenAt;

        public LobbyPresenceEntry(Player player, Instant lastSeenAt) {
            this.player = player;
            this.lastSeenAt = lastSeenAt;
        }

        public Player getPlayer() { return player; }
        public void setPlayer(Player player) { this.player = player; }
        public Instant getLastSeenAt() { return lastSeenAt; }
        public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    }

    public static class DirectChallengeEntry {
        private final UUID challengeId;
        private final Player challenger;
        private final UUID targetPlayerId;
        private final Instant createdAt;
        private final Instant expiresAt;

        public DirectChallengeEntry(UUID challengeId, Player challenger, UUID targetPlayerId, Instant createdAt, Instant expiresAt) {
            this.challengeId = challengeId;
            this.challenger = challenger;
            this.targetPlayerId = targetPlayerId;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
        }

        public UUID getChallengeId() { return challengeId; }
        public Player getChallenger() { return challenger; }
        public UUID getTargetPlayerId() { return targetPlayerId; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getExpiresAt() { return expiresAt; }
    }

    private final List<Player> waitingPool = Collections.synchronizedList(new ArrayList<>());
    private final Map<UUID, Match> activeMatches = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToMatchMap = new ConcurrentHashMap<>();
    private final Map<UUID, LobbyPresenceEntry> lobbyPresence = new ConcurrentHashMap<>();
    private final Map<UUID, DirectChallengeEntry> directChallenges = new ConcurrentHashMap<>();
    private final EloRatingService eloRatingService;
    private final PlayerStorageService playerStorageService;

    @org.springframework.beans.factory.annotation.Autowired
    public MatchmakingService(EloRatingService eloRatingService, PlayerStorageService playerStorageService) {
        this.eloRatingService = eloRatingService;
        this.playerStorageService = playerStorageService;
    }

    public MatchmakingService(EloRatingService eloRatingService) {
        this(eloRatingService, null);
    }

    // Construtor padrão para testes unitários sem Spring Context
    public MatchmakingService() {
        this(null, null);
    }

    // =========================================================================
    // SALÃO DE PARTIDAS (LOBBY PVP) E DESAFIOS DIRETOS
    // =========================================================================

    /**
     * Registra ou atualiza o heartbeat do jogador no Salão PvP.
     */
    public synchronized LobbyStateResponseDTO joinOrHeartbeatLobby(Player player) {
        UUID playerId = player.getUuid();
        cleanupPreviousFinishedMatchForPlayer(playerId);
        cleanupExpiredLobbyAndChallenges();

        lobbyPresence.compute(playerId, (id, existing) -> {
            if (existing == null) {
                System.out.println("🏛️ Jogador entrou no Salão PvP: " + player.getName());
                return new LobbyPresenceEntry(player, Instant.now());
            } else {
                existing.setPlayer(player);
                existing.setLastSeenAt(Instant.now());
                return existing;
            }
        });

        return getLobbyState(playerId);
    }

    /**
     * Remove o jogador do Salão PvP, da fila de espera e limpa seus desafios.
     */
    public synchronized void leaveLobby(UUID playerId) {
        if (playerId == null) return;
        lobbyPresence.remove(playerId);
        waitingPool.removeIf(p -> p.getUuid().equals(playerId));
        removeAllChallengesForPlayer(playerId);
        System.out.println("🚪 Jogador saiu do Salão PvP: " + playerId);
    }

    /**
     * Retorna a visão completa do Salão para um jogador específico, incluindo
     * se uma partida já foi encontrada (via fila ou desafio aceito).
     */
    public synchronized LobbyStateResponseDTO getLobbyState(UUID viewerId) {
        cleanupExpiredLobbyAndChallenges();

        LobbyPresenceEntry viewerEntry = lobbyPresence.get(viewerId);
        if (viewerEntry != null) {
            viewerEntry.setLastSeenAt(Instant.now());
        }

        // Verifica se o jogador já foi alocado em uma partida ativa
        UUID activeMatchId = playerToMatchMap.get(viewerId);
        if (activeMatchId != null) {
            Match match = activeMatches.get(activeMatchId);
            if (match != null && match.getStatus() != MatchStatus.FINISHED) {
                MatchmakingStatusResponseDTO matchDto = buildMatchFoundResponse(match, viewerId);
                return new LobbyStateResponseDTO(
                        "MATCH_FOUND",
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        matchDto
                );
            }
        }

        Set<UUID> queuedPlayerIds = waitingPool.stream()
                .map(Player::getUuid)
                .collect(Collectors.toSet());

        List<DirectChallengeDTO> incoming = new ArrayList<>();
        List<DirectChallengeDTO> outgoing = new ArrayList<>();
        Map<UUID, DirectChallengeEntry> challengeByOtherPlayer = new HashMap<>();

        for (DirectChallengeEntry ce : directChallenges.values()) {
            UUID challengerId = ce.getChallenger().getUuid();
            UUID targetId = ce.getTargetPlayerId();
            LobbyPresenceEntry targetEntry = lobbyPresence.get(targetId);
            String targetName = targetEntry != null ? targetEntry.getPlayer().getName() : "Oponente";

            DirectChallengeDTO dto = new DirectChallengeDTO(
                    ce.getChallengeId(),
                    challengerId,
                    ce.getChallenger().getName(),
                    ce.getChallenger().getPermanentElo(),
                    targetId,
                    targetName,
                    ce.getCreatedAt().toEpochMilli(),
                    ce.getExpiresAt().toEpochMilli()
            );

            if (targetId.equals(viewerId)) {
                incoming.add(dto);
                challengeByOtherPlayer.put(challengerId, ce);
            } else if (challengerId.equals(viewerId)) {
                outgoing.add(dto);
                challengeByOtherPlayer.put(targetId, ce);
            }
        }

        List<LobbyPlayerDTO> playerList = new ArrayList<>();
        for (LobbyPresenceEntry entry : lobbyPresence.values()) {
            Player p = entry.getPlayer();
            UUID pid = p.getUuid();

            // Não exibe jogadores que já estão dentro de uma partida ativa
            if (hasActiveUnfinishedMatch(pid)) {
                continue;
            }

            String challengeStatus = "NONE";
            UUID challengeId = null;

            DirectChallengeEntry rel = challengeByOtherPlayer.get(pid);
            if (rel != null) {
                challengeId = rel.getChallengeId();
                if (rel.getChallenger().getUuid().equals(viewerId)) {
                    challengeStatus = "CHALLENGE_SENT";
                } else if (rel.getTargetPlayerId().equals(viewerId)) {
                    challengeStatus = "CHALLENGE_RECEIVED";
                }
            }

            playerList.add(new LobbyPlayerDTO(
                    pid,
                    p.getName(),
                    p.getPermanentElo(),
                    queuedPlayerIds.contains(pid),
                    challengeStatus,
                    challengeId
            ));
        }

        // Ordena: quem desafiou você primeiro, depois por ELO permanente decrescente
        playerList.sort((a, b) -> {
            boolean aRec = "CHALLENGE_RECEIVED".equals(a.getChallengeStatus());
            boolean bRec = "CHALLENGE_RECEIVED".equals(b.getChallengeStatus());
            if (aRec != bRec) return aRec ? -1 : 1;
            return Integer.compare(b.getPermanentElo(), a.getPermanentElo());
        });

        String lobbyStatus = queuedPlayerIds.contains(viewerId) ? "IN_QUEUE" : "IN_LOBBY";
        return new LobbyStateResponseDTO(lobbyStatus, playerList, incoming, outgoing, null);
    }

    /**
     * Envia um desafio direto de um jogador no salão para outro jogador alvo.
     */
    public synchronized DirectChallengeDTO sendDirectChallenge(Player challenger, UUID targetPlayerId) {
        cleanupExpiredLobbyAndChallenges();

        if (challenger == null || challenger.getUuid() == null || targetPlayerId == null) {
            throw new IllegalArgumentException("Desafiante e jogador alvo são obrigatórios.");
        }
        UUID challengerId = challenger.getUuid();
        if (challengerId.equals(targetPlayerId)) {
            throw new IllegalArgumentException("Você não pode desafiar a si mesmo.");
        }

        if (hasActiveUnfinishedMatch(challengerId) || hasActiveUnfinishedMatch(targetPlayerId)) {
            throw new IllegalStateException("Um dos jogadores já está em uma partida em andamento.");
        }

        LobbyPresenceEntry targetPresence = lobbyPresence.get(targetPlayerId);
        if (targetPresence == null) {
            throw new IllegalArgumentException("O jogador alvo não está presente no salão.");
        }

        // Garante que o desafiante também conste no salão atualizado
        lobbyPresence.put(challengerId, new LobbyPresenceEntry(challenger, Instant.now()));

        // Se já houver um desafio idêntico pendente, retorna o existente
        for (DirectChallengeEntry existing : directChallenges.values()) {
            if (existing.getChallenger().getUuid().equals(challengerId)
                    && existing.getTargetPlayerId().equals(targetPlayerId)) {
                return new DirectChallengeDTO(
                        existing.getChallengeId(),
                        challengerId,
                        challenger.getName(),
                        challenger.getPermanentElo(),
                        targetPlayerId,
                        targetPresence.getPlayer().getName(),
                        existing.getCreatedAt().toEpochMilli(),
                        existing.getExpiresAt().toEpochMilli()
                );
            }
        }

        Instant now = Instant.now();
        Instant expires = now.plus(CHALLENGE_EXPIRATION);
        UUID challengeId = UUID.randomUUID();

        DirectChallengeEntry entry = new DirectChallengeEntry(challengeId, challenger, targetPlayerId, now, expires);
        directChallenges.put(challengeId, entry);

        System.out.println("🧤 Desafio Direto enviado! " + challenger.getName()
                + " desafiou " + targetPresence.getPlayer().getName() + " (ID: " + challengeId + ")");

        return new DirectChallengeDTO(
                challengeId,
                challengerId,
                challenger.getName(),
                challenger.getPermanentElo(),
                targetPlayerId,
                targetPresence.getPlayer().getName(),
                now.toEpochMilli(),
                expires.toEpochMilli()
        );
    }

    /**
     * Aceita um desafio direto, remove ambos da fila/salão e instancia a partida PvP no GameEngine.
     */
    public synchronized MatchmakingStatusResponseDTO acceptDirectChallenge(UUID challengeId, Player acceptingTargetPlayer) {
        cleanupExpiredLobbyAndChallenges();

        if (challengeId == null || acceptingTargetPlayer == null || acceptingTargetPlayer.getUuid() == null) {
            throw new IllegalArgumentException("Dados para aceitar desafio são inválidos.");
        }

        DirectChallengeEntry challenge = directChallenges.get(challengeId);
        if (challenge == null) {
            throw new IllegalArgumentException("Desafio não encontrado ou expirado: " + challengeId);
        }

        UUID targetId = acceptingTargetPlayer.getUuid();
        if (!challenge.getTargetPlayerId().equals(targetId)) {
            throw new IllegalStateException("Este desafio não pertence ao jogador informado.");
        }

        Player challenger = challenge.getChallenger();
        UUID challengerId = challenger.getUuid();

        if (hasActiveUnfinishedMatch(challengerId) || hasActiveUnfinishedMatch(targetId)) {
            directChallenges.remove(challengeId);
            throw new IllegalStateException("Um dos jogadores já iniciou outra partida.");
        }

        // Remove ambos da fila de matchmaking automático e limpa desafios pendentes de ambos
        waitingPool.removeIf(p -> p.getUuid().equals(challengerId) || p.getUuid().equals(targetId));
        removeAllChallengesForPlayer(challengerId);
        removeAllChallengesForPlayer(targetId);
        lobbyPresence.remove(challengerId);
        lobbyPresence.remove(targetId);

        // Instancia e inicia a partida PvP no GameEngine
        UUID matchId = UUID.randomUUID();
        Match match = new Match(matchId, MatchMode.PVP, challenger, acceptingTargetPlayer, false);
        match.getGameEngine().setPvpMatch(true); // Ativa recompensa de 150 Gold para PvP
        match.startMatch();

        activeMatches.put(matchId, match);
        playerToMatchMap.put(challengerId, matchId);
        playerToMatchMap.put(targetId, matchId);

        System.out.println("⚔️ Desafio Aceito! Partida PvP " + matchId + ": "
                + challenger.getName() + " vs " + acceptingTargetPlayer.getName());

        return buildMatchFoundResponse(match, targetId);
    }

    /**
     * Cancela ou recusa um desafio direto específico.
     */
    public synchronized void cancelDirectChallenge(UUID challengeId, UUID requesterId) {
        if (challengeId == null) return;
        DirectChallengeEntry entry = directChallenges.get(challengeId);
        if (entry != null) {
            if (requesterId == null
                    || entry.getChallenger().getUuid().equals(requesterId)
                    || entry.getTargetPlayerId().equals(requesterId)) {
                directChallenges.remove(challengeId);
                System.out.println("❌ Desafio Direto cancelado/recusado: " + challengeId);
            }
        }
    }

    /**
     * Limpa desafios expirados e jogadores inativos do salão.
     */
    public synchronized int cleanupExpiredLobbyAndChallenges() {
        Instant now = Instant.now();
        int removedCount = 0;

        // 1. Expiração de desafios diretos
        Iterator<Map.Entry<UUID, DirectChallengeEntry>> challengeIt = directChallenges.entrySet().iterator();
        while (challengeIt.hasNext()) {
            Map.Entry<UUID, DirectChallengeEntry> entry = challengeIt.next();
            if (now.isAfter(entry.getValue().getExpiresAt())) {
                challengeIt.remove();
                removedCount++;
            }
        }

        // 2. Expiração de presença no salão por inatividade
        Iterator<Map.Entry<UUID, LobbyPresenceEntry>> lobbyIt = lobbyPresence.entrySet().iterator();
        while (lobbyIt.hasNext()) {
            Map.Entry<UUID, LobbyPresenceEntry> entry = lobbyIt.next();
            if (Duration.between(entry.getValue().getLastSeenAt(), now).compareTo(LOBBY_PRESENCE_TIMEOUT) > 0) {
                UUID inactivePlayerId = entry.getKey();
                lobbyIt.remove();
                waitingPool.removeIf(p -> p.getUuid().equals(inactivePlayerId));
                removeAllChallengesForPlayer(inactivePlayerId);
                removedCount++;
            }
        }

        return removedCount;
    }

    private void removeAllChallengesForPlayer(UUID playerId) {
        directChallenges.entrySet().removeIf(e ->
                e.getValue().getChallenger().getUuid().equals(playerId)
                        || e.getValue().getTargetPlayerId().equals(playerId)
        );
    }

    private boolean hasActiveUnfinishedMatch(UUID playerId) {
        UUID matchId = playerToMatchMap.get(playerId);
        if (matchId == null) return false;
        Match m = activeMatches.get(matchId);
        return m != null && m.getStatus() != MatchStatus.FINISHED;
    }

    // =========================================================================
    // MÉTODOS EXISTENTES (PVE, FILA AUTOMÁTICA E ROTEAMENTO DE PARTIDAS)
    // =========================================================================

    private void syncMatchPlayerProgressToStorage(Player matchPlayer, MatchMode mode, boolean isPlayer1) {
        if (matchPlayer == null || matchPlayer.getUuid() == null || playerStorageService == null) {
            return;
        }
        if (!isPlayer1 && mode == MatchMode.PVE) {
            return;
        }
        if (PresetDeckService.PLAYER_1_UUID.equals(matchPlayer.getUuid())
                || PresetDeckService.PLAYER_2_UUID.equals(matchPlayer.getUuid())) {
            return;
        }

        Player stored = playerStorageService.getPlayer(matchPlayer.getUuid());
        if (stored != null) {
            stored.setGold(matchPlayer.getGold());
            stored.setPermanentElo(matchPlayer.getPermanentElo());
            stored.setWeeklyElo(matchPlayer.getWeeklyElo());
            if (matchPlayer.getLastWeeklyResetWeek() != null) {
                stored.setLastWeeklyResetWeek(matchPlayer.getLastWeeklyResetWeek());
            }
            playerStorageService.saveOrUpdatePlayer(stored);
        } else {
            playerStorageService.saveOrUpdatePlayer(matchPlayer);
        }
    }

    private void applyEloIfMatchFinished(Match match) {
        if (match == null || match.isEloProcessed()) return;
        GameEngine engine = match.getGameEngine();
        if (engine.getTurnPhase() == TurnPhase.GAME_OVER && engine.getWinnerPlayerId() != null) {
            match.setEloProcessed(true);
            if (eloRatingService != null && !engine.getSnapshotHistory().isEmpty()) {
                MatchSnapshotDTO lastSnap = engine.getSnapshotHistory().get(engine.getSnapshotHistory().size() - 1);
                eloRatingService.processMatchEloUpdate(
                        engine.getPlayer1(),
                        engine.getPlayer2(),
                        engine.getWinnerPlayerId(),
                        match.getMode(),
                        lastSnap.getMatchResult()
                );
            }
        }
    }

    public MatchmakingStatusResponseDTO startPveMatch(Player humanPlayer, Player botPlayer) {
        cleanupPreviousFinishedMatchForPlayer(humanPlayer.getUuid());

        UUID matchId = UUID.randomUUID();
        Match match = new Match(matchId, MatchMode.PVE, humanPlayer, botPlayer, true);

        activeMatches.put(matchId, match);
        playerToMatchMap.put(humanPlayer.getUuid(), matchId);

        match.executeLocked(() -> {
            match.startMatch();
            executeBotActionsIfNeeded(match);
            return null;
        });

        System.out.println("🤖 Partida PvE Iniciada! Match " + matchId + ": "
                + humanPlayer.getName() + " vs " + botPlayer.getName());

        return buildMatchFoundResponse(match, humanPlayer.getUuid());
    }

    public synchronized MatchmakingStatusResponseDTO joinQueue(Player joiningPlayer) {
        UUID playerId = joiningPlayer.getUuid();
        cleanupPreviousFinishedMatchForPlayer(playerId);

        if (playerToMatchMap.containsKey(playerId)) {
            UUID existingMatchId = playerToMatchMap.get(playerId);
            Match existingMatch = activeMatches.get(existingMatchId);
            if (existingMatch != null && existingMatch.getStatus() != MatchStatus.FINISHED) {
                return buildMatchFoundResponse(existingMatch, playerId);
            }
        }

        waitingPool.removeIf(p -> p.getUuid().equals(playerId));

        if (!waitingPool.isEmpty()) {
            int randomIndex = ThreadLocalRandom.current().nextInt(waitingPool.size());
            Player opponent = waitingPool.remove(randomIndex);

            removeAllChallengesForPlayer(opponent.getUuid());
            removeAllChallengesForPlayer(playerId);
            lobbyPresence.remove(opponent.getUuid());
            lobbyPresence.remove(playerId);

            UUID matchId = UUID.randomUUID();
            Match match = new Match(matchId, MatchMode.PVP, opponent, joiningPlayer, false);
            match.getGameEngine().setPvpMatch(true); // Ativa recompensa de 150 Gold para PvP
            match.startMatch();

            activeMatches.put(matchId, match);
            playerToMatchMap.put(opponent.getUuid(), matchId);
            playerToMatchMap.put(joiningPlayer.getUuid(), matchId);

            System.out.println("⚔️ Matchmaking Concluído! Partida " + matchId + ": "
                    + opponent.getName() + " vs " + joiningPlayer.getName());

            return buildMatchFoundResponse(match, playerId);

        } else {
            waitingPool.add(joiningPlayer);
            System.out.println("⏳ Jogador " + joiningPlayer.getName() + " entrou no pool de espera.");
            return new MatchmakingStatusResponseDTO("WAITING", null, playerId, Collections.emptyList());
        }
    }

    public MatchmakingStatusResponseDTO checkStatus(UUID playerId) {
        UUID matchId = playerToMatchMap.get(playerId);
        if (matchId != null) {
            Match match = activeMatches.get(matchId);
            if (match != null && match.getStatus() != MatchStatus.FINISHED) {
                return buildMatchFoundResponse(match, playerId);
            }
        }
        return new MatchmakingStatusResponseDTO("WAITING", null, playerId, Collections.emptyList());
    }

    public synchronized void leaveQueue(UUID playerId) {
        waitingPool.removeIf(p -> p.getUuid().equals(playerId));
        System.out.println("🚪 Jogador cancelou a busca e saiu do pool.");
    }

    public Map<String, Object> forfeitOrLeaveMatch(UUID matchId, UUID playerId) {
        Match match = activeMatches.get(matchId);
        if (match == null) {
            if (playerId != null) {
                playerToMatchMap.remove(playerId);
            }
            return Map.of("status", "ALREADY_CLOSED", "matchId", matchId);
        }

        if (match.getMode() == MatchMode.PVE) {
            match.executeLocked(() -> {
                match.getGameEngine().gameOver();
                match.setStatus(MatchStatus.FINISHED);
                return null;
            });
            removeMatch(matchId);
            System.out.println("🏳️ Partida PvE " + matchId + " encerrada e destruída pelo jogador " + playerId);
            return Map.of(
                    "status", "PVE_DESTROYED",
                    "matchId", matchId,
                    "mode", "PVE"
            );
        }

        return match.executeLocked(() -> {
            applyEloIfMatchFinished(match);
            GameEngine engine = match.getGameEngine();
            if (match.getStatus() != MatchStatus.FINISHED && engine.getTurnPhase() != TurnPhase.GAME_OVER) {
                engine.forfeitMatch(playerId);
                match.syncStatusWithEngine();
            }
            applyEloIfMatchFinished(match);
            if (engine.getWinnerPlayerId() != null) {
                syncMatchPlayerProgressToStorage(engine.getPlayer1(), match.getMode(), true);
                syncMatchPlayerProgressToStorage(engine.getPlayer2(), match.getMode(), false);
            }
            releasePlayersFromMatch(match);

            System.out.println("🏳️ Partida PvP " + matchId + ": Jogador " + playerId
                    + " desistiu. Vitória por W.O. concedida ao oponente (" + engine.getWinnerPlayerId() + ").");

            return Map.of(
                    "status", "PVP_FORFEITED",
                    "matchId", matchId,
                    "mode", "PVP",
                    "winnerPlayerId", engine.getWinnerPlayerId() != null ? engine.getWinnerPlayerId() : ""
            );
        });
    }

    public MatchTimelineResponseDTO processPlayerAction(UUID matchId, PlayerActionRequestDTO request) {
        Match match = getMatchOrThrow(matchId);

        return match.executeLocked(() -> {
            if (match.getStatus() == MatchStatus.FINISHED) {
                throw new IllegalStateException("A partida já foi encerrada: " + matchId);
            }

            UUID playerId = request.getPlayerId();
            if (!match.hasParticipant(playerId)) {
                throw new IllegalArgumentException("Jogador não pertence a esta partida: " + playerId);
            }

            GameEngine engine = match.getGameEngine();
            boolean isP1 = playerId.equals(engine.getPlayer1().getUuid());

            long lastSeqBeforeAction = engine.getSnapshotHistory().isEmpty()
                    ? 0
                    : engine.getSnapshotHistory().get(engine.getSnapshotHistory().size() - 1).getSnapshotSequence();

            TurnPhase currentPhase = engine.getTurnPhase();

            if (engine.isWaitingForPlayerChoice()) {
                engine.submitPlayerChoice(playerId, request.getSelectedTargetUuid());
            } else if (currentPhase == TurnPhase.CHOICE) {
                engine.submitHeroChoice(playerId, request.getSelectedOwnHeroId(), request.getSelectedEnemyHeroId());
            } else if (currentPhase == TurnPhase.PREPARATION) {
                engine.submitPreparationCards(playerId, request.getPlayedCardUuids());
            } else if (currentPhase == TurnPhase.DISCARD) {
                engine.submitDiscard(playerId, request.getDiscardCardUuids());
            } else {
                throw new IllegalStateException("Fase atual não aceita ações manuais: " + currentPhase);
            }

            executeBotActionsIfNeeded(match);
            match.syncStatusWithEngine();
            applyEloIfMatchFinished(match);

            if (engine.getWinnerPlayerId() != null) {
                syncMatchPlayerProgressToStorage(engine.getPlayer1(), match.getMode(), true);
                syncMatchPlayerProgressToStorage(engine.getPlayer2(), match.getMode(), false);
            }

            if (match.getStatus() == MatchStatus.FINISHED) {
                releasePlayersFromMatch(match);
            }

            boolean waitingForOpponent = (isP1 && engine.isPlayer1Ready() && !engine.isPlayer2Ready())
                    || (!isP1 && engine.isPlayer2Ready() && !engine.isPlayer1Ready());

            List<MatchSnapshotDTO> newSnapshots = getAdaptedSnapshotsSince(engine, playerId, lastSeqBeforeAction);
            match.updatePhaseTimerIfNeeded(newSnapshots.size());
            return new MatchTimelineResponseDTO(waitingForOpponent, newSnapshots);
        });
    }

    public MatchTimelineResponseDTO processPhaseTimeout(UUID matchId, UUID reportingPlayerId, long sinceSequence) {
        Match match = getMatchOrThrow(matchId);

        return match.executeLocked(() -> {
            resolvePhaseTimeoutLocked(match, reportingPlayerId);
            GameEngine engine = match.getGameEngine();
            UUID viewerId = reportingPlayerId != null ? reportingPlayerId : engine.getPlayer1().getUuid();
            List<MatchSnapshotDTO> newSnapshots = getAdaptedSnapshotsSince(engine, viewerId, sinceSequence);
            return new MatchTimelineResponseDTO(false, newSnapshots);
        });
    }

    private void resolvePhaseTimeoutLocked(Match match, UUID reportingPlayerId) {
        if (match == null || match.getMode() != MatchMode.PVP || match.getStatus() == MatchStatus.FINISHED) {
            return;
        }
        GameEngine engine = match.getGameEngine();
        if (engine.getTurnPhase() == TurnPhase.GAME_OVER) {
            match.syncStatusWithEngine();
            return;
        }

        UUID timedOutPlayerId = determineTimedOutPlayerId(engine, reportingPlayerId);
        if (timedOutPlayerId == null) {
            return;
        }

        engine.timeoutMatch(timedOutPlayerId);
        match.syncStatusWithEngine();
        match.updatePhaseTimerIfNeeded(0);
        applyEloIfMatchFinished(match);

        if (engine.getWinnerPlayerId() != null) {
            syncMatchPlayerProgressToStorage(engine.getPlayer1(), match.getMode(), true);
            syncMatchPlayerProgressToStorage(engine.getPlayer2(), match.getMode(), false);
        }
        releasePlayersFromMatch(match);

        System.out.println("⏱️ Partida PvP " + match.getMatchId() + ": Jogador " + timedOutPlayerId
                + " excedeu o tempo de 30s da fase. Vitória concedida a " + engine.getWinnerPlayerId() + ".");
    }

    private UUID determineTimedOutPlayerId(GameEngine engine, UUID reportingPlayerId) {
        if (engine.isWaitingForPlayerChoice() && engine.getGameState().getPendingChoice() != null) {
            return engine.getGameState().getPendingChoice().getChoosingPlayerId();
        }
        TurnPhase phase = engine.getTurnPhase();
        if (phase == TurnPhase.CHOICE) {
            return engine.getGameState().getActiveTurnPlayerId();
        }
        if (phase == TurnPhase.PREPARATION || phase == TurnPhase.DISCARD) {
            boolean p1Ready = engine.isPlayer1Ready();
            boolean p2Ready = engine.isPlayer2Ready();
            if (p1Ready && !p2Ready) {
                return engine.getPlayer2().getUuid();
            }
            if (p2Ready && !p1Ready) {
                return engine.getPlayer1().getUuid();
            }
            if (reportingPlayerId != null) {
                return reportingPlayerId;
            }
            return engine.getGameState().getActiveTurnPlayerId();
        }
        return null;
    }

    public MatchTimelineResponseDTO getMatchStateSince(UUID matchId, UUID viewerId, long sinceSequence) {
        Match match = getMatchOrThrow(matchId);

        return match.executeLocked(() -> {
            if (match.isPhaseDeadlineExpired(Instant.now())) {
                resolvePhaseTimeoutLocked(match, null);
            }
            GameEngine engine = match.getGameEngine();
            boolean isP1 = viewerId.equals(engine.getPlayer1().getUuid());
            boolean waitingForOpponent = (isP1 && engine.isPlayer1Ready() && !engine.isPlayer2Ready())
                    || (!isP1 && engine.isPlayer2Ready() && !engine.isPlayer1Ready());

            List<MatchSnapshotDTO> newSnapshots = getAdaptedSnapshotsSince(engine, viewerId, sinceSequence);
            return new MatchTimelineResponseDTO(waitingForOpponent, newSnapshots);
        });
    }

    public Match getMatchOrThrow(UUID matchId) {
        if (matchId == null) {
            throw new IllegalArgumentException("O matchId é obrigatório.");
        }
        Match match = activeMatches.get(matchId);
        if (match == null) {
            throw new IllegalArgumentException("Partida não encontrada: " + matchId);
        }
        return match;
    }

    public Optional<Match> findMatch(UUID matchId) {
        return Optional.ofNullable(activeMatches.get(matchId));
    }

    public int getActiveMatchCount() {
        return activeMatches.size();
    }

    @Scheduled(fixedRate = 60000)
    public int cleanupFinishedMatches() {
        cleanupExpiredLobbyAndChallenges();
        Instant now = Instant.now();
        List<UUID> toRemove = new ArrayList<>();

        for (Map.Entry<UUID, Match> entry : activeMatches.entrySet()) {
            Match match = entry.getValue();
            boolean finishedExpired = match.getStatus() == MatchStatus.FINISHED
                    && Duration.between(match.getLastActivityAt(), now).compareTo(FINISHED_MATCH_RETENTION) >= 0;
            boolean abandoned = Duration.between(match.getLastActivityAt(), now).compareTo(INACTIVE_MATCH_TIMEOUT) >= 0;

            if (finishedExpired || abandoned) {
                toRemove.add(entry.getKey());
            }
        }

        for (UUID matchId : toRemove) {
            removeMatch(matchId);
        }
        return toRemove.size();
    }

    public void removeMatch(UUID matchId) {
        Match removed = activeMatches.remove(matchId);
        if (removed != null) {
            releasePlayersFromMatch(removed);
        }
    }

    private void releasePlayersFromMatch(Match match) {
        for (UUID participantId : match.getParticipants().keySet()) {
            playerToMatchMap.remove(participantId, match.getMatchId());
        }
    }

    private void cleanupPreviousFinishedMatchForPlayer(UUID playerId) {
        UUID previousMatchId = playerToMatchMap.get(playerId);
        if (previousMatchId != null) {
            Match previousMatch = activeMatches.get(previousMatchId);
            if (previousMatch == null || previousMatch.getStatus() == MatchStatus.FINISHED) {
                playerToMatchMap.remove(playerId, previousMatchId);
            }
        }
    }

    private void executeBotActionsIfNeeded(Match match) {
        if (match.getMode() != MatchMode.PVE) return;

        UUID botId = match.getBotPlayerId();
        if (botId == null) return;

        GameEngine engine = match.getGameEngine();
        boolean isBotP1 = botId.equals(engine.getPlayer1().getUuid());
        Player botPlayer = isBotP1 ? engine.getPlayer1() : engine.getPlayer2();
        Player humanPlayer = isBotP1 ? engine.getPlayer2() : engine.getPlayer1();
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        int safetyLoop = 0;
        while (safetyLoop++ < 10) {
            if (engine.getTurnPhase() == TurnPhase.GAME_OVER) break;

            if (engine.isWaitingForPlayerChoice()) {
                MatchSnapshotDTO.PendingChoiceDTO pending = engine.getGameState().getPendingChoice();
                if (pending != null && botId.equals(pending.getChoosingPlayerId())) {
                    List<UUID> validTargets = pending.getValidTargetUuids();
                    UUID chosenTarget = (validTargets != null && !validTargets.isEmpty())
                            ? validTargets.get(rng.nextInt(validTargets.size()))
                            : null;
                    engine.submitPlayerChoice(botId, chosenTarget);
                    continue;
                }
                break;
            }

            if (engine.getTurnPhase() == TurnPhase.CHOICE
                    && botId.equals(engine.getGameState().getActiveTurnPlayerId())) {

                List<HeroCard> botAvailableHeroes = botPlayer.getCurrentDeck().getHeroList().stream()
                        .filter(h -> Boolean.TRUE.equals(h.getEstaVivo()) && Boolean.TRUE.equals(h.getEstaAtivo()))
                        .collect(Collectors.toList());

                List<HeroCard> humanAliveHeroes = humanPlayer.getCurrentDeck().getHeroList().stream()
                        .filter(h -> Boolean.TRUE.equals(h.getEstaVivo()))
                        .collect(Collectors.toList());

                if (!botAvailableHeroes.isEmpty() && !humanAliveHeroes.isEmpty()) {
                    HeroCard chosenBotHero = botAvailableHeroes.get(rng.nextInt(botAvailableHeroes.size()));
                    HeroCard chosenHumanHero = humanAliveHeroes.get(rng.nextInt(humanAliveHeroes.size()));
                    engine.submitHeroChoice(botId, chosenBotHero.getUuid(), chosenHumanHero.getUuid());
                    continue;
                }
            }

            boolean botReady = isBotP1 ? engine.isPlayer1Ready() : engine.isPlayer2Ready();
            boolean humanReady = isBotP1 ? engine.isPlayer2Ready() : engine.isPlayer1Ready();

            if (engine.getTurnPhase() == TurnPhase.PREPARATION && humanReady && !botReady) {
                UUID botActiveHeroId = isBotP1
                        ? engine.getGameState().getActiveHeroPlayer1Id()
                        : engine.getGameState().getActiveHeroPlayer2Id();
                HeroCard botActiveHero = engine.findHeroById(botActiveHeroId);

                int freeWeaponSlots = 0;
                if (botActiveHero != null) {
                    freeWeaponSlots = Math.max(0, botActiveHero.getMaxWeaponSlots() - engine.getUsedWeaponSlotsForHero(botActiveHeroId));
                }

                List<Card> botHand = botPlayer.getCurrentDeck().getCardList().stream()
                        .filter(c -> c.getZoneId() == ZoneId.HAND)
                        .collect(Collectors.toList());

                List<UUID> cardsToPlay = new ArrayList<>();
                for (Card c : botHand) {
                    if (cardsToPlay.size() >= 2) break;
                    if (c.getItemType() == ItemType.ARMA) {
                        int emp = Math.max(1, c.getEmpunhadura());
                        if (emp <= freeWeaponSlots) {
                            cardsToPlay.add(c.getUuid());
                            freeWeaponSlots -= emp;
                        }
                    } else {
                        cardsToPlay.add(c.getUuid());
                    }
                }

                engine.submitPreparationCards(botId, cardsToPlay);
                continue;
            }

            if (engine.getTurnPhase() == TurnPhase.DISCARD && humanReady && !botReady) {
                List<Card> botHand = botPlayer.getCurrentDeck().getCardList().stream()
                        .filter(c -> c.getZoneId() == ZoneId.HAND)
                        .collect(Collectors.toList());

                List<UUID> toDiscard = new ArrayList<>();
                if (!botHand.isEmpty() && rng.nextBoolean()) {
                    toDiscard.add(botHand.get(rng.nextInt(botHand.size())).getUuid());
                }

                engine.submitDiscard(botId, toDiscard);
                continue;
            }

            break;
        }
    }

    private MatchmakingStatusResponseDTO buildMatchFoundResponse(Match match, UUID viewerId) {
        GameEngine engine = match.getGameEngine();
        List<MatchSnapshotDTO> initialSnapshots = getAdaptedSnapshotsSince(engine, viewerId, 0);
        return new MatchmakingStatusResponseDTO("MATCH_FOUND", match.getMatchId(), viewerId, initialSnapshots);
    }

    private List<MatchSnapshotDTO> getAdaptedSnapshotsSince(GameEngine engine, UUID viewerId, long sinceSequence) {
        return engine.getSnapshotHistory().stream()
                .filter(s -> s.getSnapshotSequence() > sinceSequence)
                .map(raw -> SnapshotFactory.adaptSnapshotForViewer(raw, viewerId))
                .collect(Collectors.toList());
    }
}

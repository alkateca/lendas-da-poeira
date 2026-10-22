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

    private final List<Player> waitingPool = Collections.synchronizedList(new ArrayList<>());
    private final Map<UUID, Match> activeMatches = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToMatchMap = new ConcurrentHashMap<>();

    /**
     * Inicia imediatamente uma partida PvE isolada contra a Máquina (Bot).
     */
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

    /**
     * Coloca o jogador no pool de espera PvP ou, se houver oponentes, cria uma entidade Match isolada.
     */
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

            UUID matchId = UUID.randomUUID();
            Match match = new Match(matchId, MatchMode.PVP, opponent, joiningPlayer, false);
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

    /**
     * Roteamento thread-safe por matchId (não bloqueia outras partidas em andamento).
     */
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

            // Se a partida terminou, desvincula os jogadores para que possam iniciar novas partidas imediatamente
            if (match.getStatus() == MatchStatus.FINISHED) {
                releasePlayersFromMatch(match);
            }

            boolean waitingForOpponent = (isP1 && engine.isPlayer1Ready() && !engine.isPlayer2Ready())
                    || (!isP1 && engine.isPlayer2Ready() && !engine.isPlayer1Ready());

            List<MatchSnapshotDTO> newSnapshots = getAdaptedSnapshotsSince(engine, playerId, lastSeqBeforeAction);
            return new MatchTimelineResponseDTO(waitingForOpponent, newSnapshots);
        });
    }

    public MatchTimelineResponseDTO getMatchStateSince(UUID matchId, UUID viewerId, long sinceSequence) {
        Match match = getMatchOrThrow(matchId);

        return match.executeLocked(() -> {
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

    /**
     * Garbage Collection de partidas finalizadas ou órfãs.
     * Pode ser chamado manualmente ou via agendamento do Spring.
     */
    @Scheduled(fixedRate = 60000)
    public int cleanupFinishedMatches() {
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

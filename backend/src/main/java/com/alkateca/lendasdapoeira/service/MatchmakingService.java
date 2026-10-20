package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.DTO.MatchDtos.*;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.engine.MatchSnapshotDTO;
import com.alkateca.lendasdapoeira.engine.SnapshotFactory;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MatchmakingService {

    private final List<Player> waitingPool = Collections.synchronizedList(new ArrayList<>());
    private final Map<UUID, GameEngine> activeMatches = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> playerToMatchMap = new ConcurrentHashMap<>();

    // Mapa para identificar partidas PvE: matchId -> botPlayerId
    private final Map<UUID, UUID> matchToBotMap = new ConcurrentHashMap<>();

    private final Random randomGenerator = new Random();

    /**
     * Inicia imediatamente uma partida PvE contra a Máquina (Bot).
     */
    public synchronized MatchmakingStatusResponseDTO startPveMatch(Player humanPlayer, Player botPlayer) {
        UUID matchId = UUID.randomUUID();

        GameEngine engine = new GameEngine(humanPlayer, botPlayer);
        engine.startGame(); // Sorteia cara ou coroa nos logs, compra 5 cartas e vai para TurnPhase.CHOICE

        activeMatches.put(matchId, engine);
        playerToMatchMap.put(humanPlayer.getUuid(), matchId);
        matchToBotMap.put(matchId, botPlayer.getUuid());

        System.out.println("🤖 Partida PvE Iniciada! Match " + matchId + ": "
                + humanPlayer.getName() + " vs " + botPlayer.getName());

        // Se a Máquina ganhou no sorteio inicial, ela já escolhe os Heróis da fase CHOICE!
        executeBotActionsIfNeeded(matchId, engine);

        return buildMatchFoundResponse(matchId, humanPlayer.getUuid());
    }

    /**
     * Coloca o jogador no pool de espera PvP ou, se houver oponentes, sorteia um aleatoriamente e cria a partida.
     */
    public synchronized MatchmakingStatusResponseDTO joinQueue(Player joiningPlayer) {
        UUID playerId = joiningPlayer.getUuid();

        if (playerToMatchMap.containsKey(playerId)) {
            UUID matchId = playerToMatchMap.get(playerId);
            return buildMatchFoundResponse(matchId, playerId);
        }

        waitingPool.removeIf(p -> p.getUuid().equals(playerId));

        if (!waitingPool.isEmpty()) {
            int randomIndex = randomGenerator.nextInt(waitingPool.size());
            Player opponent = waitingPool.remove(randomIndex);

            UUID matchId = UUID.randomUUID();

            GameEngine engine = new GameEngine(opponent, joiningPlayer);
            engine.startGame();

            activeMatches.put(matchId, engine);
            playerToMatchMap.put(opponent.getUuid(), matchId);
            playerToMatchMap.put(joiningPlayer.getUuid(), matchId);

            System.out.println("⚔️ Matchmaking Aleatório Concluído! Partida " + matchId + ": "
                    + opponent.getName() + " vs " + joiningPlayer.getName());

            return buildMatchFoundResponse(matchId, playerId);

        } else {
            waitingPool.add(joiningPlayer);
            System.out.println("⏳ Jogador " + joiningPlayer.getName() + " entrou no pool de espera.");
            return new MatchmakingStatusResponseDTO("WAITING", null, playerId, Collections.emptyList());
        }
    }

    public MatchmakingStatusResponseDTO checkStatus(UUID playerId) {
        UUID matchId = playerToMatchMap.get(playerId);
        if (matchId != null && activeMatches.containsKey(matchId)) {
            return buildMatchFoundResponse(matchId, playerId);
        }
        return new MatchmakingStatusResponseDTO("WAITING", null, playerId, Collections.emptyList());
    }

    public synchronized void leaveQueue(UUID playerId) {
        waitingPool.removeIf(p -> p.getUuid().equals(playerId));
        System.out.println("🚪 Jogador cancelou a busca e saiu do pool.");
    }

    public synchronized MatchTimelineResponseDTO processPlayerAction(UUID matchId, PlayerActionRequestDTO request) {
        GameEngine engine = activeMatches.get(matchId);
        if (engine == null) {
            throw new IllegalArgumentException("Partida não encontrada: " + matchId);
        }

        UUID playerId = request.getPlayerId();
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

        // Caso seja uma partida PvE, aciona a resposta imediata da Máquina!
        executeBotActionsIfNeeded(matchId, engine);

        boolean waitingForOpponent = (isP1 && engine.isPlayer1Ready() && !engine.isPlayer2Ready())
                || (!isP1 && engine.isPlayer2Ready() && !engine.isPlayer1Ready());

        List<MatchSnapshotDTO> newSnapshots = getAdaptedSnapshotsSince(engine, playerId, lastSeqBeforeAction);
        return new MatchTimelineResponseDTO(waitingForOpponent, newSnapshots);
    }

    /**
     * Executa em loop seguro todas as decisões pendentes do Bot em partidas PvE.
     */
    private void executeBotActionsIfNeeded(UUID matchId, GameEngine engine) {
        UUID botId = matchToBotMap.get(matchId);
        if (botId == null) return; // Partida PvP normal

        boolean isBotP1 = botId.equals(engine.getPlayer1().getUuid());
        Player botPlayer = isBotP1 ? engine.getPlayer1() : engine.getPlayer2();
        Player humanPlayer = isBotP1 ? engine.getPlayer2() : engine.getPlayer1();

        int safetyLoop = 0;
        while (safetyLoop++ < 10) {
            if (engine.getTurnPhase() == TurnPhase.GAME_OVER) break;

            // 1. Se a engine pausou aguardando uma escolha de alvo (PendingChoice) do Bot
            if (engine.isWaitingForPlayerChoice()) {
                MatchSnapshotDTO.PendingChoiceDTO pending = engine.getGameState().getPendingChoice();
                if (pending != null && botId.equals(pending.getChoosingPlayerId())) {
                    List<UUID> validTargets = pending.getValidTargetUuids();
                    UUID chosenTarget = (validTargets != null && !validTargets.isEmpty())
                            ? validTargets.get(randomGenerator.nextInt(validTargets.size()))
                            : null;
                    engine.submitPlayerChoice(botId, chosenTarget);
                    continue;
                }
                break; // Está aguardando escolha do jogador humano
            }

            // 2. Fase CHOICE: se a iniciativa atual é do Bot, sorteia 1 Herói ativo do Bot e 1 Herói vivo do Humano
            if (engine.getTurnPhase() == TurnPhase.CHOICE
                    && botId.equals(engine.getGameState().getActiveTurnPlayerId())) {

                List<HeroCard> botAvailableHeroes = botPlayer.getCurrentDeck().getHeroList().stream()
                        .filter(h -> Boolean.TRUE.equals(h.getEstaVivo()) && Boolean.TRUE.equals(h.getEstaAtivo()))
                        .collect(Collectors.toList());

                List<HeroCard> humanAliveHeroes = humanPlayer.getCurrentDeck().getHeroList().stream()
                        .filter(h -> Boolean.TRUE.equals(h.getEstaVivo()))
                        .collect(Collectors.toList());

                if (!botAvailableHeroes.isEmpty() && !humanAliveHeroes.isEmpty()) {
                    HeroCard chosenBotHero = botAvailableHeroes.get(randomGenerator.nextInt(botAvailableHeroes.size()));
                    HeroCard chosenHumanHero = humanAliveHeroes.get(randomGenerator.nextInt(humanAliveHeroes.size()));
                    engine.submitHeroChoice(botId, chosenBotHero.getUuid(), chosenHumanHero.getUuid());
                    continue;
                }
            }

            // 3. Fase PREPARATION: se o humano já confirmou (ou se o Bot ainda não confirmou), o Bot escolhe até 2 cartas
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

            // 4. Fase DISCARD: quando o humano confirmar o descarte, o Bot confirma (descartando até 1 carta aleatória)
            if (engine.getTurnPhase() == TurnPhase.DISCARD && humanReady && !botReady) {
                List<Card> botHand = botPlayer.getCurrentDeck().getCardList().stream()
                        .filter(c -> c.getZoneId() == ZoneId.HAND)
                        .collect(Collectors.toList());

                List<UUID> toDiscard = new ArrayList<>();
                if (!botHand.isEmpty() && randomGenerator.nextBoolean()) {
                    toDiscard.add(botHand.get(randomGenerator.nextInt(botHand.size())).getUuid());
                }

                engine.submitDiscard(botId, toDiscard);
                continue;
            }

            break;
        }
    }

    public MatchTimelineResponseDTO getMatchStateSince(UUID matchId, UUID viewerId, long sinceSequence) {
        GameEngine engine = activeMatches.get(matchId);
        if (engine == null) {
            throw new IllegalArgumentException("Partida não encontrada: " + matchId);
        }

        boolean isP1 = viewerId.equals(engine.getPlayer1().getUuid());
        boolean waitingForOpponent = (isP1 && engine.isPlayer1Ready() && !engine.isPlayer2Ready())
                || (!isP1 && engine.isPlayer2Ready() && !engine.isPlayer1Ready());

        List<MatchSnapshotDTO> newSnapshots = getAdaptedSnapshotsSince(engine, viewerId, sinceSequence);
        return new MatchTimelineResponseDTO(waitingForOpponent, newSnapshots);
    }

    private MatchmakingStatusResponseDTO buildMatchFoundResponse(UUID matchId, UUID viewerId) {
        GameEngine engine = activeMatches.get(matchId);
        List<MatchSnapshotDTO> initialSnapshots = getAdaptedSnapshotsSince(engine, viewerId, 0);
        return new MatchmakingStatusResponseDTO("MATCH_FOUND", matchId, viewerId, initialSnapshots);
    }

    private List<MatchSnapshotDTO> getAdaptedSnapshotsSince(GameEngine engine, UUID viewerId, long sinceSequence) {
        return engine.getSnapshotHistory().stream()
                .filter(s -> s.getSnapshotSequence() > sinceSequence)
                .map(raw -> SnapshotFactory.adaptSnapshotForViewer(raw, viewerId))
                .collect(Collectors.toList());
    }
}
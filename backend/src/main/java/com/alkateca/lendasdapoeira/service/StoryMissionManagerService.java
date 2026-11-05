package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.DTO.story.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StoryMissionManagerService {

    private final StoryMissionLoaderService loaderService;
    private final StoryTriggerEvaluator triggerEvaluator;

    // Progresso em memória por playerId (pode ser persistido no repositório do Player posteriormente)
    private final Map<String, Set<String>> completedMissionsByPlayer = new ConcurrentHashMap<>();

    // Rastreia qual missão pertence a cada matchId ativa e quais diálogos já dispararam
    private final Map<String, StoryMissionDefinition> activeMissionByMatchId = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> triggeredDialoguesByMatchId = new ConcurrentHashMap<>();

    public StoryMissionManagerService(
            StoryMissionLoaderService loaderService,
            StoryTriggerEvaluator triggerEvaluator
    ) {
        this.loaderService = loaderService;
        this.triggerEvaluator = triggerEvaluator;
    }

    public List<StoryMissionSummaryDTO> getMissionsCatalogForPlayer(String playerId) {
        Set<String> completed = (playerId != null)
                ? completedMissionsByPlayer.getOrDefault(playerId, Collections.emptySet())
                : Collections.emptySet();

        List<StoryMissionDefinition> allMissions = loaderService.listAllMissions();

        return allMissions.stream().map(mission -> {
            boolean isCompleted = completed.contains(mission.id());
            // Capítulo 1 de cada cor está sempre desbloqueado; capítulos seguintes exigem o anterior concluído
            boolean isUnlocked = mission.capitulo() <= 1 || isPreviousChapterCompleted(allMissions, completed, mission);
            return StoryMissionSummaryDTO.fromDefinition(mission, isUnlocked, isCompleted);
        }).toList();
    }

    private boolean isPreviousChapterCompleted(
            List<StoryMissionDefinition> allMissions,
            Set<String> completedIds,
            StoryMissionDefinition current
    ) {
        return allMissions.stream()
                .filter(m -> m.cor().equalsIgnoreCase(current.cor()) && m.capitulo() == current.capitulo() - 1)
                .findFirst()
                .map(prev -> completedIds.contains(prev.id()))
                .orElse(true);
    }

    public StartStoryMissionResponseDTO startStoryMatch(
            StoryMissionDefinition mission,
            String playerId,
            String playerName
    ) {
        String matchId = UUID.randomUUID().toString();
        activeMissionByMatchId.put(matchId, mission);
        triggeredDialoguesByMatchId.put(matchId, ConcurrentHashMap.newKeySet());

        /*
         * INTEGRAÇÃO COM O SEU GAME ENGINE:
         * Aqui você pode invocar o seu serviço existente de criação de partidas PvE, por exemplo:
         *
         * Deck playerPresetDeck = deckPresetService.buildPresetDeck(mission.playerDeckPresetId());
         * Deck whiteAiDeck = deckPresetService.buildCustomDeck(mission.aiDeck().heros(), mission.aiDeck().cards());
         * TurnTimelineResponseDTO timeline = gameEngine.startCustomPveMatch(matchId, playerId, playerName, playerPresetDeck, "Ecos da Memória (WHITE)", whiteAiDeck);
         * List<Object> initialSnapshots = new ArrayList<>(timeline.getSnapshots());
         */

        Map<String, Object> initialSnapshot = Map.of(
                "matchId", matchId,
                "turnPhase", "CHOICE",
                "activeTurnPlayerId", playerId,
                "pvpMatch", false,
                "localPlayer", Map.of(
                        "playerId", playerId,
                        "name", playerName,
                        "deckCount", 25,
                        "hand", List.of(),
                        "heroes", List.of(),
                        "discardPile", List.of(),
                        "playedCardsInResolve", List.of()
                ),
                "opponentPlayer", Map.of(
                        "playerId", "AI_ARCHIVE_WHITE",
                        "name", "Ecos da Memória (WHITE)",
                        "deckCount", 25,
                        "handCount", 5,
                        "heroes", List.of(),
                        "discardPile", List.of(),
                        "playedCardsInResolve", List.of()
                ),
                "pendingDialogues", List.of()
        );

        return new StartStoryMissionResponseDTO(
                matchId,
                playerId,
                List.of(initialSnapshot),
                mission
        );
    }

    public boolean markMissionAsCompleted(String playerId, String missionId) {
        if (playerId == null || playerId.isBlank()) return false;
        return completedMissionsByPlayer
                .computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet())
                .add(missionId);
    }

    /**
     * Método utilitário para o GameEngine consultar diálogos engatilhados ao jogar uma carta na partida.
     */
    public List<ScriptedDialogueDTO> checkCardPlayedTriggers(String matchId, String cardName) {
        StoryMissionDefinition mission = activeMissionByMatchId.get(matchId);
        Set<String> triggered = triggeredDialoguesByMatchId.get(matchId);
        if (mission == null || triggered == null) return List.of();
        return triggerEvaluator.evaluateCardPlayed(mission, triggered, cardName);
    }

    /**
     * Método utilitário para o GameEngine consultar diálogos engatilhados na morte de um Herói.
     */
    public List<ScriptedDialogueDTO> checkHeroDeathTriggers(String matchId, String deadHeroName) {
        StoryMissionDefinition mission = activeMissionByMatchId.get(matchId);
        Set<String> triggered = triggeredDialoguesByMatchId.get(matchId);
        if (mission == null || triggered == null) return List.of();
        return triggerEvaluator.evaluateHeroDeath(mission, triggered, deadHeroName);
    }
}

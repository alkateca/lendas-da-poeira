package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.DTO.MatchDtos.MatchmakingStatusResponseDTO;
import com.alkateca.lendasdapoeira.DTO.story.StartStoryMissionResponseDTO;
import com.alkateca.lendasdapoeira.DTO.story.StoryMissionDefinition;
import com.alkateca.lendasdapoeira.DTO.story.StoryMissionSummaryDTO;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.Player;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StoryMissionManagerService {

    private final StoryMissionLoaderService missionLoaderService;
    private final MatchmakingService matchmakingService;
    private final PlayerStorageService playerStorageService;
    private final PresetDeckService presetDeckService;

    public StoryMissionManagerService(
            StoryMissionLoaderService missionLoaderService,
            MatchmakingService matchmakingService,
            PlayerStorageService playerStorageService,
            PresetDeckService presetDeckService
    ) {
        this.missionLoaderService = missionLoaderService;
        this.matchmakingService = matchmakingService;
        this.playerStorageService = playerStorageService;
        this.presetDeckService = presetDeckService;
    }

    public List<StoryMissionSummaryDTO> getMissionsCatalogForPlayer(String playerId) {
        List<StoryMissionDefinition> allMissions = missionLoaderService.listAllMissions();
        Set<String> completedMissions = Collections.emptySet();

        if (playerId != null && !playerId.isBlank()) {
            try {
                UUID uuid = UUID.fromString(playerId);
                Player player = playerStorageService.findById(uuid);
                if (player != null && player.getCompletedMissions() != null) {
                    completedMissions = player.getCompletedMissions();
                }
            } catch (Exception ignored) {}
        }

        Map<String, List<StoryMissionDefinition>> missionsByColor = allMissions.stream()
                .collect(Collectors.groupingBy(m -> m.cor() != null ? m.cor().toUpperCase() : "NEUTRAL"));

        List<StoryMissionSummaryDTO> result = new ArrayList<>();

        for (List<StoryMissionDefinition> wingMissions : missionsByColor.values()) {
            wingMissions.sort(Comparator.comparingInt(StoryMissionDefinition::capitulo));

            String previousMissionId = null;
            for (StoryMissionDefinition mission : wingMissions) {
                boolean isCompleted = completedMissions.contains(mission.id());
                boolean isUnlocked = (mission.capitulo() <= 1)
                        || (previousMissionId == null)
                        || completedMissions.contains(previousMissionId);

                result.add(StoryMissionSummaryDTO.fromDefinition(mission, isUnlocked, isCompleted));

                previousMissionId = mission.id();
            }
        }

        result.sort(Comparator.comparing(StoryMissionSummaryDTO::cor)
                .thenComparingInt(StoryMissionSummaryDTO::capitulo));

        return result;
    }

    public StartStoryMissionResponseDTO startStoryMatch(
            StoryMissionDefinition mission,
            String playerId,
            String playerName
    ) {
        UUID pId;
        try {
            pId = UUID.fromString(playerId);
        } catch (Exception e) {
            pId = UUID.nameUUIDFromBytes(playerId.getBytes());
        }

        String finalName = (playerName != null && !playerName.isBlank())
                ? playerName
                : "Viajante da Poeira";

        Player humanPlayer = new Player();
        humanPlayer.setUuid(pId);
        humanPlayer.setName(finalName);

        Player savedPlayer = playerStorageService.findById(pId);
        Deck playerDeck = null;
        if (savedPlayer != null && savedPlayer.getCurrentDeck() != null
                && savedPlayer.getCurrentDeck().getHeroList() != null
                && !savedPlayer.getCurrentDeck().getHeroList().isEmpty()) {
            playerDeck = savedPlayer.getCurrentDeck();
        }
        if (playerDeck == null) {
            playerDeck = presetDeckService.createDefaultDeck();
        }
        humanPlayer.setCurrentDeck(playerDeck);

        Player machinePlayer = presetDeckService.createRandomBotPlayer();
        machinePlayer.setName("Ecos da Memória (WHITE)");

        MatchmakingStatusResponseDTO matchResult = matchmakingService.startPveMatch(humanPlayer, machinePlayer);

        return new StartStoryMissionResponseDTO(
                matchResult.getMatchId(),
                matchResult.getLocalPlayerId(),
                matchResult.getInitialSnapshots(),
                mission
        );
    }

    public boolean markMissionAsCompleted(String playerId, String missionId) {
        if (playerId == null || playerId.isBlank() || missionId == null || missionId.isBlank()) {
            return false;
        }

        try {
            UUID uuid = UUID.fromString(playerId);
            Player player = playerStorageService.findById(uuid);
            if (player != null) {
                boolean newlyAdded = player.getCompletedMissions().add(missionId);
                if (newlyAdded) {
                    playerStorageService.savePlayer(player);
                }
                return newlyAdded;
            }
        } catch (Exception ignored) {}

        return true;
    }
}

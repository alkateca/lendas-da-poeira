package com.alkateca.lendasdapoeira.controller;

import com.alkateca.lendasdapoeira.DTO.MatchDtos.MatchmakingStatusResponseDTO;
import com.alkateca.lendasdapoeira.DTO.story.StoryMissionDefinition;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.service.MatchmakingService;
import com.alkateca.lendasdapoeira.service.PlayerStorageService;
import com.alkateca.lendasdapoeira.service.PresetDeckService;
import com.alkateca.lendasdapoeira.service.StoryMissionLoaderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/story")
@CrossOrigin(origins = "*")
public class StoryMissionController {

    private final StoryMissionLoaderService storyMissionLoaderService;
    private final MatchmakingService matchmakingService;
    private final PlayerStorageService playerStorageService;
    private final PresetDeckService presetDeckService;

    public StoryMissionController(
            StoryMissionLoaderService storyMissionLoaderService,
            MatchmakingService matchmakingService,
            PlayerStorageService playerStorageService,
            PresetDeckService presetDeckService) {
        this.storyMissionLoaderService = storyMissionLoaderService;
        this.matchmakingService = matchmakingService;
        this.playerStorageService = playerStorageService;
        this.presetDeckService = presetDeckService;
    }

    @GetMapping("/missions")
    public ResponseEntity<List<StoryMissionDefinition>> listMissions(@RequestParam(required = false) String playerId) {
        return ResponseEntity.ok(storyMissionLoaderService.listAllMissions());
    }

    @PostMapping("/missions/{missionId}/start")
    public ResponseEntity<?> startMission(
            @PathVariable String missionId,
            @RequestBody(required = false) Map<String, Object> payload) {

        StoryMissionDefinition mission = storyMissionLoaderService.getMissionById(missionId)
                .orElseThrow(() -> new IllegalArgumentException("Missão não encontrada: " + missionId));

        UUID pId = UUID.randomUUID();
        String pName = "Viajante dos Arquivos";

        if (payload != null) {
            if (payload.get("playerId") != null) {
                try {
                    pId = UUID.fromString(payload.get("playerId").toString());
                } catch (Exception ignored) {
                    pId = UUID.nameUUIDFromBytes(payload.get("playerId").toString().getBytes());
                }
            }
            if (payload.get("playerName") != null && !payload.get("playerName").toString().isBlank()) {
                pName = payload.get("playerName").toString();
            }
        }

        Player humanPlayer = new Player();
        humanPlayer.setUuid(pId);
        humanPlayer.setName(pName);

        Deck deck = presetDeckService.createDefaultDeck();
        humanPlayer.setCurrentDeck(deck);

        Player machinePlayer = presetDeckService.createRandomBotPlayer();
        machinePlayer.setName("Ecos da Memória (WHITE)");

        MatchmakingStatusResponseDTO matchResult = matchmakingService.startPveMatch(humanPlayer, machinePlayer);

        Map<String, Object> response = Map.of(
                "matchId", matchResult.getMatchId(),
                "localPlayerId", matchResult.getLocalPlayerId(),
                "initialSnapshots", matchResult.getInitialSnapshots(),
                "mission", mission
        );

        return ResponseEntity.ok(response);
    }
}

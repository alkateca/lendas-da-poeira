package com.alkateca.lendasdapoeira.controller;

import com.alkateca.lendasdapoeira.DTO.story.StartStoryMissionRequestDTO;
import com.alkateca.lendasdapoeira.DTO.story.StartStoryMissionResponseDTO;
import com.alkateca.lendasdapoeira.DTO.story.StoryMissionDefinition;
import com.alkateca.lendasdapoeira.DTO.story.StoryMissionSummaryDTO;
import com.alkateca.lendasdapoeira.service.StoryMissionLoaderService;
import com.alkateca.lendasdapoeira.service.StoryMissionManagerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/story/missions")
@CrossOrigin(origins = "*")
public class StoryMissionController {

    private final StoryMissionLoaderService missionLoaderService;
    private final StoryMissionManagerService missionManagerService;

    public StoryMissionController(
            StoryMissionLoaderService missionLoaderService,
            StoryMissionManagerService missionManagerService
    ) {
        this.missionLoaderService = missionLoaderService;
        this.missionManagerService = missionManagerService;
    }

    /**
     * Lista os capítulos dos Grandes Arquivos da Poeira agrupados por cor e capítulo,
     * indicando o estado de desbloqueio e conclusão para o jogador.
     */
    @GetMapping
    public ResponseEntity<List<StoryMissionSummaryDTO>> listMissions(
            @RequestParam(value = "playerId", required = false) String playerId
    ) {
        List<StoryMissionSummaryDTO> catalog = missionManagerService.getMissionsCatalogForPlayer(playerId);
        return ResponseEntity.ok(catalog);
    }

    /**
     * Obtém os detalhes completos de uma missão específica lida do ficheiro YAML.
     */
    @GetMapping("/{missionId}")
    public ResponseEntity<StoryMissionDefinition> getMissionDetails(
            @PathVariable String missionId
    ) {
        StoryMissionDefinition mission = missionLoaderService.getMissionById(missionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Missão não encontrada nos Grandes Arquivos: " + missionId
                ));
        return ResponseEntity.ok(mission);
    }

    /**
     * Constrói o Player humano (com o baralho temático da missão) e o Player da máquina
     * (Ecos da Memória - WHITE) usando o CardFactory, e inicia a partida no MatchmakingService.
     */
    @PostMapping("/{missionId}/start")
    public ResponseEntity<StartStoryMissionResponseDTO> startStoryMission(
            @PathVariable String missionId,
            @RequestParam(value = "playerId", required = false) String queryPlayerId,
            @RequestBody(required = false) StartStoryMissionRequestDTO request
    ) {
        StoryMissionDefinition mission = missionLoaderService.getMissionById(missionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Missão não encontrada para iniciar partida: " + missionId
                ));

        String rawPlayerId = (request != null && request.playerId() != null && !request.playerId().isBlank())
                ? request.playerId()
                : queryPlayerId;

        UUID resolvedPlayerId = parseOrGenerateUuid(rawPlayerId);

        String resolvedPlayerName = (request != null && request.playerName() != null && !request.playerName().isBlank())
                ? request.playerName()
                : "Viajante da Poeira";

        StartStoryMissionResponseDTO response = missionManagerService.startStoryMatch(
                mission,
                resolvedPlayerId,
                resolvedPlayerName
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Regista a conclusão da missão após a vitória do jogador na StoryMatchScene.
     */
    @PostMapping("/{missionId}/complete")
    public ResponseEntity<Map<String, Object>> completeStoryMission(
            @PathVariable String missionId,
            @RequestParam("playerId") String playerId
    ) {
        if (missionLoaderService.getMissionById(missionId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Missão inválida: " + missionId);
        }

        boolean firstTime = missionManagerService.markMissionAsCompleted(playerId, missionId);

        return ResponseEntity.ok(Map.of(
                "missionId", missionId,
                "playerId", playerId,
                "completed", true,
                "firstTimeCompletion", firstTime
        ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Argumento inválido"));
    }

    private UUID parseOrGenerateUuid(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(rawId.trim());
        } catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(rawId.trim().getBytes());
        }
    }
}

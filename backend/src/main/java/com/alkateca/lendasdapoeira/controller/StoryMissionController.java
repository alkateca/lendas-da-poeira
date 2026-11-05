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
     * Retorna a lista de todas as missões agrupadas/ordenadas por cor e capítulo,
     * enriquecida com o progresso do jogador (unlocked / completed).
     */
    @GetMapping
    public ResponseEntity<List<StoryMissionSummaryDTO>> listMissions(
            @RequestParam(value = "playerId", required = false) String playerId
    ) {
        List<StoryMissionSummaryDTO> catalog = missionManagerService.getMissionsCatalogForPlayer(playerId);
        return ResponseEntity.ok(catalog);
    }

    /**
     * Retorna a definição completa de uma missão específica (incluindo prólogo, epílogo e eventos).
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
     * Instancia uma partida PvE no GameEngine associada à missionId selecionada,
     * carregando o baralho preset da cor ensinada e o baralho WHITE da máquina.
     */
    @PostMapping("/{missionId}/start")
    public ResponseEntity<StartStoryMissionResponseDTO> startStoryMission(
            @PathVariable String missionId,
            @RequestBody(required = false) StartStoryMissionRequestDTO request
    ) {
        StoryMissionDefinition mission = missionLoaderService.getMissionById(missionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Missão não encontrada para iniciar partida: " + missionId
                ));

        String playerId = (request != null && request.playerId() != null && !request.playerId().isBlank())
                ? request.playerId()
                : "local-story-player";
        String playerName = (request != null && request.playerName() != null && !request.playerName().isBlank())
                ? request.playerName()
                : "Viajante da Poeira";

        StartStoryMissionResponseDTO response = missionManagerService.startStoryMatch(
                mission,
                playerId,
                playerName
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Regista a conclusão da missão após a vitória do jogador e desbloqueia o próximo capítulo.
     */
    @PostMapping("/{missionId}/complete")
    public ResponseEntity<Map<String, Object>> completeStoryMission(
            @PathVariable String missionId,
            @RequestParam("playerId") String playerId
    ) {
        if (missionLoaderService.getMissionById(missionId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Missão inválida: " + missionId);
        }

        boolean newlyCompleted = missionManagerService.markMissionAsCompleted(playerId, missionId);

        return ResponseEntity.ok(Map.of(
                "missionId", missionId,
                "playerId", playerId,
                "completed", true,
                "firstTimeCompletion", newlyCompleted
        ));
    }
}

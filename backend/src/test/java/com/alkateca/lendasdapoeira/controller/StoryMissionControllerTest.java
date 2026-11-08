package com.alkateca.lendasdapoeira.controller;

import com.alkateca.lendasdapoeira.DTO.story.StartStoryMissionResponseDTO;
import com.alkateca.lendasdapoeira.DTO.story.StoryMissionDefinition;
import com.alkateca.lendasdapoeira.service.StoryMissionLoaderService;
import com.alkateca.lendasdapoeira.service.StoryMissionManagerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class StoryMissionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StoryMissionLoaderService loaderService;

    @Mock
    private StoryMissionManagerService managerService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StoryMissionController(loaderService, managerService)).build();
    }

    @Test
    @DisplayName("GET /api/story/missions/{missionId} deve retornar 404 quando a missão não existir")
    void shouldReturn404WhenMissionDoesNotExist() throws Exception {
        when(loaderService.getMissionById("missao_inexistente")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/story/missions/missao_inexistente"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/story/missions/{missionId}/start deve aceitar playerId textual e converter em UUID determinístico")
    void shouldAcceptNonUuidStringPlayerIdAndStartMission() throws Exception {
        StoryMissionDefinition mission = new StoryMissionDefinition(
                "mission_blue_01", "O Juramento do Espelho", "Cavaleiros", "BLUE", 1,
                "Prólogo", "Epílogo", "DECK_BLUE", null, List.of()
        );

        when(loaderService.getMissionById("mission_blue_01")).thenReturn(Optional.of(mission));
        when(managerService.startStoryMatch(eq(mission), any(UUID.class), eq("Viajante")))
                .thenReturn(new StartStoryMissionResponseDTO("match-999", "player-uuid", List.of(), mission));

        String body = """
                {
                  "playerId": "id-customizado-sem-formato-uuid",
                  "playerName": "Viajante"
                }
                """;

        mockMvc.perform(post("/api/story/missions/mission_blue_01/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").value("match-999"))
                .andExpect(jsonPath("$.mission.cor").value("BLUE"));
    }

    @Test
    @DisplayName("POST /api/story/missions/{missionId}/complete deve registrar conclusão da missão")
    void shouldCompleteStoryMissionSuccessfully() throws Exception {
        StoryMissionDefinition mission = new StoryMissionDefinition(
                "mission_red_01", "Clarão", "Sub", "RED", 1,
                "P", "E", "D", null, List.of()
        );

        when(loaderService.getMissionById("mission_red_01")).thenReturn(Optional.of(mission));
        when(managerService.markMissionAsCompleted("player-1", "mission_red_01")).thenReturn(true);

        mockMvc.perform(post("/api/story/missions/mission_red_01/complete")
                        .param("playerId", "player-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true))
                .andExpect(jsonPath("$.firstTimeCompletion").value(true));
    }
}

package com.alkateca.lendasdapoeira.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class StoryMissionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/story/missions deve retornar o catálogo de missões agrupadas por cor")
    void shouldReturnStoryMissionsCatalog() throws Exception {
        mockMvc.perform(get("/api/story/missions")
                        .param("playerId", "test-player-uuid")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("POST /api/story/missions/{missionId}/start deve instanciar partida PvE associada à missão")
    void shouldStartStoryMissionPveMatch() throws Exception {
        String requestPayload = """
                {
                  "playerId": "player-test-001",
                  "playerName": "Viajante dos Arquivos",
                  "missionId": "mission_red_01"
                }
                """;

        mockMvc.perform(post("/api/story/missions/mission_red_01/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchId").isNotEmpty())
                .andExpect(jsonPath("$.initialSnapshots").isArray())
                .andExpect(jsonPath("$.mission.id").value("mission_red_01"))
                .andExpect(jsonPath("$.mission.cor").value("RED"));
    }
}

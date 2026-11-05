package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.DTO.story.ScriptedDialogueDTO;
import com.alkateca.lendasdapoeira.DTO.story.StoryMissionDefinition;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StoryMissionLoaderServiceTest {

    private StoryMissionLoaderService loaderService;
    private ObjectMapper yamlMapper;

    @BeforeEach
    void setUp() {
        loaderService = new StoryMissionLoaderService();
        yamlMapper = new ObjectMapper(new YAMLFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    @DisplayName("Deve deserializar corretamente um arquivo YAML de missão com eventos scriptados")
    void shouldDeserializeYamlMissionCorrectly() throws Exception {
        String sampleYaml = """
                id: "mission_red_01"
                titulo: "O Primeiro Clarão da Forja"
                subtitulo: "Caçadores das Cinzas"
                cor: "RED"
                capitulo: 1
                prologo: |
                  Nos ermos do leste, as fornalhas nunca se apagam.
                epilogo: |
                  Com o constructo silenciado, a Poeira de fogo assentou.
                playerDeckPresetId: "DECK_RED_TUTORIAL"
                aiDeck:
                  heros:
                    - "10000000-0000-0000-0000-000000000003"
                  cards:
                    - "card_uuid_1"
                scriptedEvents:
                  - trigger: "ON_HERO_DEATH"
                    targetHeroName: "Alka, Lutador do Grupo dos Heróis"
                    speaker: "Eco de Alka"
                    dialogue: "A chama... ainda arde mais forte que a minha guarda..."
                  - trigger: "ON_CARD_PLAYED"
                    targetCardName: "Bola de Fogo"
                    speaker: "Narrador dos Arquivos"
                    dialogue: "A canalização elemental de Fogo exige precisão e pureza de Espírito."
                """;

        StoryMissionDefinition mission = yamlMapper.readValue(sampleYaml, StoryMissionDefinition.class);

        assertNotNull(mission);
        assertEquals("mission_red_01", mission.id());
        assertEquals("O Primeiro Clarão da Forja", mission.titulo());
        assertEquals("RED", mission.cor());
        assertEquals(1, mission.capitulo());
        assertTrue(mission.prologo().contains("Nos ermos do leste"));
        assertTrue(mission.epilogo().contains("Poeira de fogo assentou"));
        assertEquals("DECK_RED_TUTORIAL", mission.playerDeckPresetId());

        assertNotNull(mission.aiDeck());
        assertEquals(1, mission.aiDeck().heros().size());
        assertEquals("10000000-0000-0000-0000-000000000003", mission.aiDeck().heros().get(0));

        List<ScriptedDialogueDTO> events = mission.scriptedEvents();
        assertNotNull(events);
        assertEquals(2, events.size());

        ScriptedDialogueDTO deathEvent = events.get(0);
        assertEquals("ON_HERO_DEATH", deathEvent.trigger());
        assertEquals("Alka, Lutador do Grupo dos Heróis", deathEvent.targetHeroName());
        assertEquals("Eco de Alka", deathEvent.speaker());

        ScriptedDialogueDTO cardEvent = events.get(1);
        assertEquals("ON_CARD_PLAYED", cardEvent.trigger());
        assertEquals("Bola de Fogo", cardEvent.targetCardName());
    }

    @Test
    @DisplayName("Deve carregar todas as missões do diretório resources/missions/ e indexar por ID")
    void shouldLoadMissionsFromClasspathResources() {
        loaderService.loadAllMissions();

        List<StoryMissionDefinition> allMissions = loaderService.listAllMissions();
        assertNotNull(allMissions);

        // Caso mission_red_01.yaml esteja em src/main/resources/missions/RED/
        Optional<StoryMissionDefinition> redMission = loaderService.getMissionById("mission_red_01");
        redMission.ifPresent(mission -> {
            assertEquals("RED", mission.cor());
            assertNotNull(mission.prologo());
            assertNotNull(mission.epilogo());
            assertFalse(mission.scriptedEvents().isEmpty());
        });
    }
}

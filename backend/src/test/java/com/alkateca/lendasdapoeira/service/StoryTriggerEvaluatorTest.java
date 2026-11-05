package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.DTO.story.ScriptedDialogueDTO;
import com.alkateca.lendasdapoeira.DTO.story.StoryMissionDefinition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StoryTriggerEvaluatorTest {

    private StoryTriggerEvaluator evaluator;
    private StoryMissionDefinition redMission;
    private Set<String> triggeredKeys;

    @BeforeEach
    void setUp() {
        evaluator = new StoryTriggerEvaluator();
        triggeredKeys = new HashSet<>();

        List<ScriptedDialogueDTO> scriptedEvents = List.of(
                new ScriptedDialogueDTO(
                        "ON_CARD_PLAYED",
                        null,
                        "Bola de Fogo",
                        null,
                        "Narrador dos Arquivos",
                        "A canalização elemental de Fogo exige precisão e pureza de Espírito."
                ),
                new ScriptedDialogueDTO(
                        "ON_HERO_DEATH",
                        "Alka, Lutador do Grupo dos Heróis",
                        null,
                        null,
                        "Eco de Alka",
                        "A chama... ainda arde mais forte que a minha guarda..."
                )
        );

        redMission = new StoryMissionDefinition(
                "mission_red_01",
                "O Primeiro Clarão da Forja",
                "Caçadores das Cinzas",
                "RED",
                1,
                "Prólogo...",
                "Epílogo...",
                "DECK_RED_TUTORIAL",
                new StoryMissionDefinition.AiDeckConfig(List.of(), List.of()),
                scriptedEvents
        );
    }

    @Test
    @DisplayName("Deve disparar diálogo ON_CARD_PLAYED na primeira vez que Bola de Fogo é jogada e ignorar nas próximas")
    void shouldTriggerOnCardPlayedOnlyOnce() {
        // 1ª vez jogando Bola de Fogo -> Deve disparar
        List<ScriptedDialogueDTO> firstPlay = evaluator.evaluateCardPlayed(redMission, triggeredKeys, "Bola de Fogo");
        assertEquals(1, firstPlay.size());
        assertEquals("Narrador dos Arquivos", firstPlay.get(0).speaker());

        // 2ª vez jogando Bola de Fogo na mesma partida -> Não deve repetir o diálogo
        List<ScriptedDialogueDTO> secondPlay = evaluator.evaluateCardPlayed(redMission, triggeredKeys, "Bola de Fogo");
        assertTrue(secondPlay.isEmpty(), "O evento scriptado não deve disparar mais de uma vez na mesma partida");
    }

    @Test
    @DisplayName("Não deve disparar diálogo ON_CARD_PLAYED para cartas não listadas na missão")
    void shouldNotTriggerForUnlistedCard() {
        List<ScriptedDialogueDTO> result = evaluator.evaluateCardPlayed(redMission, triggeredKeys, "Sica");
        assertTrue(result.isEmpty());
        assertTrue(triggeredKeys.isEmpty());
    }

    @Test
    @DisplayName("Deve disparar diálogo ON_HERO_DEATH quando o herói chefe da IA (Alka) é derrotado")
    void shouldTriggerOnSpecificHeroDeath() {
        // Morte de outro herói qualquer -> vazio
        List<ScriptedDialogueDTO> otherDeath = evaluator.evaluateHeroDeath(
                redMission, triggeredKeys, "Alucinação Cintilante"
        );
        assertTrue(otherDeath.isEmpty());

        // Morte de Alka (case-insensitive) -> dispara diálogo dramático
        List<ScriptedDialogueDTO> alkaDeath = evaluator.evaluateHeroDeath(
                redMission, triggeredKeys, "alka, lutador do grupo dos heróis"
        );
        assertEquals(1, alkaDeath.size());
        assertEquals("Eco de Alka", alkaDeath.get(0).speaker());
        assertTrue(alkaDeath.get(0).dialogue().contains("minha guarda"));

        // Checagem subsequente no próximo snapshot com Alka morto -> não duplica
        List<ScriptedDialogueDTO> duplicateCheck = evaluator.evaluateHeroDeath(
                redMission, triggeredKeys, "Alka, Lutador do Grupo dos Heróis"
        );
        assertTrue(duplicateCheck.isEmpty());
    }
}

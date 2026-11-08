package com.alkateca.lendasdapoeira.DTO.story;

import java.util.List;

public record StoryMissionDefinition(
    String id,
    String titulo,
    String subtitulo,
    String cor,
    int capitulo,
    String prologo,
    String epilogo,
    String playerDeckPresetId,
    AiDeckConfig aiDeck,
    List<ScriptedDialogueDTO> scriptedEvents
) {
    public record AiDeckConfig(
        List<String> heros,
        List<String> cards
    ) {}
}

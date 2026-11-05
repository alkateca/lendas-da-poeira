package com.alkateca.lendasdapoeira.DTO.story;

import java.util.List;

public record StoryMissionSummaryDTO(
    String id,
    String titulo,
    String subtitulo,
    String cor,
    int capitulo,
    String prologo,
    String epilogo,
    boolean unlocked,
    boolean completed,
    List<ScriptedDialogueDTO> scriptedEvents
) {
    public static StoryMissionSummaryDTO fromDefinition(
            StoryMissionDefinition def,
            boolean unlocked,
            boolean completed
    ) {
        return new StoryMissionSummaryDTO(
                def.id(),
                def.titulo(),
                def.subtitulo(),
                def.cor(),
                def.capitulo(),
                def.prologo(),
                def.epilogo(),
                unlocked,
                completed,
                def.scriptedEvents() != null ? def.scriptedEvents() : List.of()
        );
    }
}

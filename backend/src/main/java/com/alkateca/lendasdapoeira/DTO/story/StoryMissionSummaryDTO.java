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
) {}

package com.alkateca.lendasdapoeira.DTO.story;

import com.alkateca.lendasdapoeira.engine.MatchSnapshotDTO;

import java.util.List;
import java.util.UUID;

public record StartStoryMissionResponseDTO(
        UUID matchId,
        UUID localPlayerId,
        List<MatchSnapshotDTO> initialSnapshots,
        StoryMissionDefinition mission
) {}

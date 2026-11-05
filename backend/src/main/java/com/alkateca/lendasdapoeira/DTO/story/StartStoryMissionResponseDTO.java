package com.alkateca.lendasdapoeira.DTO.story;

import java.util.List;
import java.util.UUID;

public record StartStoryMissionResponseDTO(
    String matchId,
    String localPlayerId,
    List<Object> initialSnapshots,
    StoryMissionDefinition mission
) {
    @SuppressWarnings("unchecked")
    public StartStoryMissionResponseDTO(UUID matchId, UUID localPlayerId, List<?> initialSnapshots, StoryMissionDefinition mission) {
        this(
            matchId != null ? matchId.toString() : null,
            localPlayerId != null ? localPlayerId.toString() : null,
            initialSnapshots != null ? (List<Object>) initialSnapshots : List.of(),
            mission
        );
    }
}

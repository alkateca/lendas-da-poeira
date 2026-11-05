package com.alkateca.lendasdapoeira.DTO.story;

public record StartStoryMissionRequestDTO(
        String playerId,
        String playerName,
        String missionId
) {
    public StartStoryMissionRequestDTO(String playerId, String playerName) {
        this(playerId, playerName, null);
    }
}

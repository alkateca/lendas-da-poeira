package com.alkateca.lendasdapoeira.DTO;

import com.alkateca.lendasdapoeira.enums.TurnPhase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerActionRequestDTO {

    private UUID matchId;
    private UUID playerId;
    private TurnPhase currentPhase;

    // Usado na TurnPhase.CHOICE (pelo jogador dono do turno ativo)
    private UUID selectedOwnHeroId;
    private UUID selectedEnemyHeroId;

    // Usado na TurnPhase.PREPARATION (cartas baixadas da HAND para RESOLVE)
    private List<UUID> playedCardUuids = new ArrayList<>();

    // Usado na TurnPhase.DISCARD (cartas selecionadas na HAND para DISCARD)
    private List<UUID> discardCardUuids = new ArrayList<>();
}
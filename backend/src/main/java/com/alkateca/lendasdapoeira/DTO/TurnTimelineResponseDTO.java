package com.alkateca.lendasdapoeira.DTO;

import com.alkateca.lendasdapoeira.engine.MatchSnapshotDTO;
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
public class TurnTimelineResponseDTO {

    private UUID viewerPlayerId;
    private TurnPhase finalTurnPhase;
    private boolean waitingForOpponent;
    private String statusMessage;
    private List<MatchSnapshotDTO> snapshots = new ArrayList<>();
}
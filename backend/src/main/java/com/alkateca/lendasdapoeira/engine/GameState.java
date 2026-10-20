package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GameState {

    private Player player1;
    private Player player2;
    private TurnPhase turnPhase;
    private UUID activeTurnPlayerId;

    private UUID activeHeroPlayer1Id;
    private UUID activeHeroPlayer2Id;

    private List<Card> player1PlayedCards = new ArrayList<>();
    private List<Card> player2PlayedCards = new ArrayList<>();

    private List<Card> cardsOnBoard = new ArrayList<>();

    private MatchSnapshotDTO.PendingChoiceDTO pendingChoice;

}

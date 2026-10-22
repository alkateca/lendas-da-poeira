package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.DTO.MatchDtos.MatchTimelineResponseDTO;
import com.alkateca.lendasdapoeira.DTO.MatchDtos.MatchmakingStatusResponseDTO;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Match;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.MatchStatus;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;
import com.alkateca.lendasdapoeira.service.MatchmakingService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MatchForfeitTest {

    private Player createTestPlayer(String name) {
        Player p = new Player();
        p.setUuid(UUID.randomUUID());
        p.setName(name);

        HeroCard h = new HeroCard();
        h.setUuid(UUID.randomUUID());
        h.setCardName("Alka, Lutador do Grupo dos Heróis");
        h.setOwnerId(p.getUuid());
        h.setVidaMaxima(15);
        h.setVidaAtual(15);
        h.setEstaVivo(true);
        h.setEstaAtivo(true);
        h.setZoneId(ZoneId.BENCH);

        p.setCurrentDeck(new Deck("Deck", new ArrayList<>(), new ArrayList<>(List.of(h))));
        return p;
    }

    @Test
    void pveLeaveShouldDestroyMatchImmediately() {
        MatchmakingService service = new MatchmakingService();
        Player human = createTestPlayer("Humano");
        Player bot = createTestPlayer("Bot");

        MatchmakingStatusResponseDTO startRes = service.startPveMatch(human, bot);
        UUID matchId = startRes.getMatchId();
        assertTrue(service.findMatch(matchId).isPresent());

        Map<String, Object> leaveRes = service.forfeitOrLeaveMatch(matchId, human.getUuid());
        assertEquals("PVE_DESTROYED", leaveRes.get("status"));
        assertTrue(service.findMatch(matchId).isEmpty(), "Partida PvE deve ser removida imediatamente da memória");
    }

    @Test
    void pvpForfeitShouldGrantVictoryToOpponentAndNotifyViaSnapshot() {
        MatchmakingService service = new MatchmakingService();
        Player p1 = createTestPlayer("Jogador 1");
        Player p2 = createTestPlayer("Jogador 2");

        service.joinQueue(p1);
        MatchmakingStatusResponseDTO pvpStart = service.joinQueue(p2);
        UUID matchId = pvpStart.getMatchId();

        // Jogador 1 desiste da partida PvP
        Map<String, Object> forfeitRes = service.forfeitOrLeaveMatch(matchId, p1.getUuid());
        assertEquals("PVP_FORFEITED", forfeitRes.get("status"));
        assertEquals(p2.getUuid(), forfeitRes.get("winnerPlayerId"));

        Match match = service.getMatchOrThrow(matchId);
        assertEquals(MatchStatus.FINISHED, match.getStatus());
        assertEquals(TurnPhase.GAME_OVER, match.getGameEngine().getTurnPhase());

        // Jogador 2 consulta o estado da partida e recebe a notificação de vitória por W.O.
        MatchTimelineResponseDTO p2Timeline = service.getMatchStateSince(matchId, p2.getUuid(), 0);
        MatchSnapshotDTO lastSnapshot = p2Timeline.getSnapshots().get(p2Timeline.getSnapshots().size() - 1);

        assertEquals(TurnPhase.GAME_OVER, lastSnapshot.getTurnPhase());
        assertEquals(p2.getUuid(), lastSnapshot.getWinnerPlayerId());
        assertEquals("FORFEIT", lastSnapshot.getWinReason());
        assertEquals("MATCH_FORFEITED", lastSnapshot.getTriggerContext().getEventType());
    }
}

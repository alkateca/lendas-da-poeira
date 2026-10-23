package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.DTO.MatchDtos.*;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Match;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.Colors;
import com.alkateca.lendasdapoeira.enums.MatchMode;
import com.alkateca.lendasdapoeira.enums.MatchStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PvpLobbyAndChallengeTest {

    private MatchmakingService matchmakingService;

    @BeforeEach
    void setUp() {
        matchmakingService = new MatchmakingService();
    }

    private Player createValidPlayer(String name, int permanentElo) {
        UUID playerId = UUID.randomUUID();

        List<HeroCard> heroes = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            HeroCard h = new HeroCard();
            h.setUuid(UUID.randomUUID());
            h.setOwnerId(playerId);
            h.setCardName("Herói " + i + " de " + name);
            h.setCardType(CardType.HERO);
            h.setColor(Colors.WHITE);
            h.setEspirito(2);
            h.setAtaque(4);
            h.setDefesa(2);
            h.setVidaMaxima(15);
            h.setVidaAtual(15);
            h.setMaxWeaponSlots(2);
            h.setEstaVivo(true);
            h.setEstaAtivo(true);
            heroes.add(h);
        }

        List<Card> cards = new ArrayList<>();
        for (int i = 1; i <= 30; i++) {
            Card c = new Card();
            c.setUuid(UUID.randomUUID());
            c.setOwnerId(playerId);
            c.setCardName("Carta " + i);
            c.setCardType(CardType.SPELL);
            c.setColor(Colors.WHITE);
            cards.add(c);
        }

        Deck deck = new Deck("Deck de " + name, cards, heroes);
        Player player = new Player(playerId, name, deck);
        player.setPermanentElo(permanentElo);
        return player;
    }

    @Test
    @DisplayName("Deve registrar presença de múltiplos jogadores no salão e exibir ELO Permanente")
    void shouldTrackLobbyPresenceAndPermanentElo() {
        Player p1 = createValidPlayer("Ayla", 1250);
        Player p2 = createValidPlayer("Boran", 980);

        matchmakingService.joinOrHeartbeatLobby(p1);
        LobbyStateResponseDTO stateForP2 = matchmakingService.joinOrHeartbeatLobby(p2);

        assertEquals("IN_LOBBY", stateForP2.getStatus());
        assertEquals(2, stateForP2.getPlayers().size());

        LobbyPlayerDTO topPlayer = stateForP2.getPlayers().get(0);
        assertEquals("Ayla", topPlayer.getPlayerName());
        assertEquals(1250, topPlayer.getPermanentElo());

        // Ao sair do salão, a lista deve refletir a desconexão imediatamente
        matchmakingService.leaveLobby(p1.getUuid());
        LobbyStateResponseDTO updatedState = matchmakingService.getLobbyState(p2.getUuid());
        assertEquals(1, updatedState.getPlayers().size());
        assertEquals("Boran", updatedState.getPlayers().get(0).getPlayerName());
    }

    @Test
    @DisplayName("Deve enviar desafio direto e refletir os status CHALLENGE_SENT e CHALLENGE_RECEIVED")
    void shouldSendDirectChallengeAndUpdateLobbyStatuses() {
        Player challenger = createValidPlayer("Desafiante", 1100);
        Player target = createValidPlayer("Alvo", 1150);

        matchmakingService.joinOrHeartbeatLobby(challenger);
        matchmakingService.joinOrHeartbeatLobby(target);

        DirectChallengeDTO challenge = matchmakingService.sendDirectChallenge(challenger, target.getUuid());
        assertNotNull(challenge.getChallengeId());
        assertEquals(challenger.getUuid(), challenge.getChallengerId());
        assertEquals(target.getUuid(), challenge.getTargetId());

        // Visão do desafiante
        LobbyStateResponseDTO challengerView = matchmakingService.getLobbyState(challenger.getUuid());
        assertEquals(1, challengerView.getOutgoingChallenges().size());
        LobbyPlayerDTO targetRowInChallengerView = challengerView.getPlayers().stream()
                .filter(p -> p.getPlayerId().equals(target.getUuid()))
                .findFirst()
                .orElseThrow();
        assertEquals("CHALLENGE_SENT", targetRowInChallengerView.getChallengeStatus());

        // Visão do desafiado (Alvo)
        LobbyStateResponseDTO targetView = matchmakingService.getLobbyState(target.getUuid());
        assertEquals(1, targetView.getIncomingChallenges().size());
        LobbyPlayerDTO challengerRowInTargetView = targetView.getPlayers().stream()
                .filter(p -> p.getPlayerId().equals(challenger.getUuid()))
                .findFirst()
                .orElseThrow();
        assertEquals("CHALLENGE_RECEIVED", challengerRowInTargetView.getChallengeStatus());
        assertEquals(challenge.getChallengeId(), challengerRowInTargetView.getChallengeId());
    }

    @Test
    @DisplayName("Deve instanciar partida PvP no GameEngine ao aceitar desafio direto e encaminhar ambos")
    void shouldInstantiatePvpMatchWhenChallengeAccepted() {
        Player challenger = createValidPlayer("Kael", 1300);
        Player target = createValidPlayer("Lyra", 1280);

        matchmakingService.joinOrHeartbeatLobby(challenger);
        matchmakingService.joinOrHeartbeatLobby(target);

        DirectChallengeDTO challenge = matchmakingService.sendDirectChallenge(challenger, target.getUuid());

        // Jogador desafiado aceita o combate
        MatchmakingStatusResponseDTO acceptResponse = matchmakingService.acceptDirectChallenge(
                challenge.getChallengeId(),
                target
        );

        assertEquals("MATCH_FOUND", acceptResponse.getStatus());
        assertNotNull(acceptResponse.getMatchId());
        assertFalse(acceptResponse.getSnapshots().isEmpty());

        // Quando o desafiante consulta o estado do salão no próximo polling, recebe MATCH_FOUND com a mesma partida
        LobbyStateResponseDTO challengerLobbyState = matchmakingService.getLobbyState(challenger.getUuid());
        assertEquals("MATCH_FOUND", challengerLobbyState.getStatus());
        assertNotNull(challengerLobbyState.getActiveMatch());
        assertEquals(acceptResponse.getMatchId(), challengerLobbyState.getActiveMatch().getMatchId());

        // Verifica que a entidade Match foi devidamente criada e iniciada no GameEngine
        Match createdMatch = matchmakingService.getMatchOrThrow(acceptResponse.getMatchId());
        assertEquals(MatchMode.PVP, createdMatch.getMode());
        assertEquals(MatchStatus.RUNNING, createdMatch.getStatus());
        assertTrue(createdMatch.hasParticipant(challenger.getUuid()));
        assertTrue(createdMatch.hasParticipant(target.getUuid()));
    }

    @Test
    @DisplayName("Deve permitir cancelar desafio direto e limpar o status pendente")
    void shouldCancelDirectChallenge() {
        Player p1 = createValidPlayer("Vex", 1000);
        Player p2 = createValidPlayer("Zara", 1020);

        matchmakingService.joinOrHeartbeatLobby(p1);
        matchmakingService.joinOrHeartbeatLobby(p2);

        DirectChallengeDTO challenge = matchmakingService.sendDirectChallenge(p1, p2.getUuid());
        matchmakingService.cancelDirectChallenge(challenge.getChallengeId(), p1.getUuid());

        LobbyStateResponseDTO p2View = matchmakingService.getLobbyState(p2.getUuid());
        assertTrue(p2View.getIncomingChallenges().isEmpty());
    }

    @Test
    @DisplayName("Deve parear dois jogadores do salão quando ambos clicam em Achar Partida (Matchmaking)")
    void shouldPairPlayersUsingMatchmakingQueueFromLobby() {
        Player p1 = createValidPlayer("Dante", 1050);
        Player p2 = createValidPlayer("Nero", 1070);

        matchmakingService.joinOrHeartbeatLobby(p1);
        matchmakingService.joinOrHeartbeatLobby(p2);

        MatchmakingStatusResponseDTO q1 = matchmakingService.joinQueue(p1);
        assertEquals("WAITING", q1.getStatus());

        LobbyStateResponseDTO p1LobbyWhileWaiting = matchmakingService.getLobbyState(p1.getUuid());
        assertEquals("IN_QUEUE", p1LobbyWhileWaiting.getStatus());

        MatchmakingStatusResponseDTO q2 = matchmakingService.joinQueue(p2);
        assertEquals("MATCH_FOUND", q2.getStatus());
        assertNotNull(q2.getMatchId());

        // O jogador 1 também recebe MATCH_FOUND ao consultar o estado do salão
        LobbyStateResponseDTO p1LobbyAfterMatch = matchmakingService.getLobbyState(p1.getUuid());
        assertEquals("MATCH_FOUND", p1LobbyAfterMatch.getStatus());
        assertEquals(q2.getMatchId(), p1LobbyAfterMatch.getActiveMatch().getMatchId());
    }
}

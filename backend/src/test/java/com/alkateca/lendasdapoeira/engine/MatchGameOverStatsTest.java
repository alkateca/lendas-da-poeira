package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MatchGameOverStatsTest {

    private Player createPlayer(String name, int heroHp) {
        Player p = new Player();
        p.setUuid(UUID.randomUUID());
        p.setName(name);

        HeroCard h = new HeroCard();
        h.setUuid(UUID.randomUUID());
        h.setCardName("Hero of " + name);
        h.setOwnerId(p.getUuid());
        h.setVidaMaxima(heroHp);
        h.setVidaAtual(heroHp);
        h.setEstaVivo(true);
        h.setEstaAtivo(true);
        h.setZoneId(ZoneId.BENCH);

        List<Card> cards = new ArrayList<>();
        p.setCurrentDeck(new Deck("Deck", cards, new ArrayList<>(List.of(h))));
        return p;
    }

    @Test
    void testMetricsTrackingAndGameOverResultSummary() {
        Player p1 = createPlayer("Jogador 1", 20);
        Player p2 = createPlayer("Jogador 2", 20);

        GameEngine engine = new GameEngine(p1, p2);
        HeroCard p1Hero = p1.getCurrentDeck().getHeroList().get(0);
        HeroCard p2Hero = p2.getCurrentDeck().getHeroList().get(0);

        // Simula dano sofrido por P2 causado por P1
        p2Hero.setVidaAtual(12); // P2 tomou 8 de dano
        engine.captureSnapshot(p1Hero, null, "PHYSICAL_COMBAT_EXCHANGE");

        // Simula cura em P2
        p2Hero.setVidaAtual(15); // P2 curou 3 de vida
        engine.captureSnapshot(null, null, "EFFECT_RESOLVED");

        // Simula cartas jogadas
        engine.incrementCardsPlayed(p1.getUuid(), 3);
        engine.incrementCardsPlayed(p2.getUuid(), 2);

        // Simula derrota de P2 (eliminação)
        p2Hero.setVidaAtual(0);
        p2Hero.setEstaVivo(false);
        engine.captureSnapshot(p1Hero, null, "PHYSICAL_COMBAT_EXCHANGE");

        // Encerra partida
        engine.setTurnPhase(TurnPhase.GAME_OVER);
        engine.setWinnerPlayerId(p1.getUuid());
        engine.setWinReason("ELIMINATION");

        MatchSnapshotDTO.MatchResultSummaryDTO summary = engine.buildMatchResultSummary();
        assertNotNull(summary);
        assertEquals(p1.getUuid(), summary.getWinnerPlayerId());
        assertEquals("Jogador 1", summary.getWinnerName());
        assertEquals("ELIMINATION", summary.getWinReason());

        // Estatísticas P1
        MatchSnapshotDTO.PlayerMatchStatsDTO p1Stats = summary.getPlayer1Stats();
        assertNotNull(p1Stats);
        assertTrue(p1Stats.isWinner());
        assertEquals(23, p1Stats.getTotalDamageDealt()); // 8 inicial + 15 final = 23 de dano causado
        assertEquals(0, p1Stats.getTotalDamageTaken());
        assertEquals(3, p1Stats.getCardsPlayed());
        assertEquals(1, p1Stats.getHeroesDefeated());
        assertEquals(1, p1Stats.getHeroesAlive());
        assertTrue(p1Stats.getMatchScore() > 0);

        // Estatísticas P2
        MatchSnapshotDTO.PlayerMatchStatsDTO p2Stats = summary.getPlayer2Stats();
        assertNotNull(p2Stats);
        assertFalse(p2Stats.isWinner());
        assertEquals(0, p2Stats.getTotalDamageDealt());
        assertEquals(23, p2Stats.getTotalDamageTaken());
        assertEquals(3, p2Stats.getTotalHealingDone());
        assertEquals(2, p2Stats.getCardsPlayed());
        assertEquals(0, p2Stats.getHeroesDefeated());
        assertEquals(0, p2Stats.getHeroesAlive());

        // Snapshot com GAME_OVER deve anexar MatchResult
        MatchSnapshotDTO snapshot = SnapshotFactory.createSnapshot(engine, null, null, "GAME_OVER");
        assertNotNull(snapshot.getMatchResult());
        assertEquals("Jogador 1", snapshot.getMatchResult().getWinnerName());

        // Adapter para viewer preserva o MatchResult
        MatchSnapshotDTO adapted = SnapshotFactory.adaptSnapshotForViewer(snapshot, p1.getUuid());
        assertNotNull(adapted.getMatchResult());
        assertEquals(summary.getWinnerPlayerId(), adapted.getMatchResult().getWinnerPlayerId());
    }

    @Test
    void testForfeitMatchGeneratesResultSummary() {
        Player p1 = createPlayer("Jogador 1", 20);
        Player p2 = createPlayer("Jogador 2", 20);

        GameEngine engine = new GameEngine(p1, p2);
        engine.forfeitMatch(p2.getUuid());

        assertEquals(TurnPhase.GAME_OVER, engine.getTurnPhase());
        assertEquals(p1.getUuid(), engine.getWinnerPlayerId());
        assertEquals("FORFEIT", engine.getWinReason());

        MatchSnapshotDTO.MatchResultSummaryDTO summary = engine.buildMatchResultSummary();
        assertNotNull(summary);
        assertEquals(p1.getUuid(), summary.getWinnerPlayerId());
        assertEquals("FORFEIT", summary.getWinReason());
        assertTrue(summary.getPlayer1Stats().isWinner());
        assertFalse(summary.getPlayer2Stats().isWinner());
    }
}

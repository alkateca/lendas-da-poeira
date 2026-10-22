package com.alkateca.lendasdapoeira.engine;

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

class MatchStatsAccumulationTest {

    private Player buildPlayer(String name, String heroName, int hp, int atq, int def, int esp) {
        Player p = new Player();
        p.setUuid(UUID.randomUUID());
        p.setName(name);

        HeroCard h = new HeroCard();
        h.setUuid(UUID.randomUUID());
        h.setCardName(heroName);
        h.setOwnerId(p.getUuid());
        h.setVidaMaxima(hp);
        h.setVidaAtual(hp);
        h.setAtaque(atq);
        h.setDefesa(def);
        h.setEspirito(esp);
        h.setEstaVivo(true);
        h.setEstaAtivo(true);
        h.setZoneId(ZoneId.BENCH);
        CardFactory.buildCardLogic(h);

        p.setCurrentDeck(new Deck("Deck", new ArrayList<>(), new ArrayList<>(List.of(h))));
        return p;
    }

    @Test
    void shouldAccuratelyTrackDamageDealtTakenHealingAndVictoryScore() {
        // P1 tem Esquadrão Goblin (cura 2 no final do combate) com 10 HP, 6 ATQ, 1 DEF
        Player p1 = buildPlayer("P1", "Esquadrão Goblin", 10, 6, 1, 2);
        // P2 tem Alka com 4 HP, 3 ATQ, 1 DEF (sofrerá 5 de dano físico e morrerá no 1º combate)
        Player p2 = buildPlayer("P2", "Alka, Lutador do Grupo dos Heróis", 4, 3, 1, 2);

        GameEngine engine = new GameEngine(p1, p2);
        engine.startGame();

        UUID activeId = engine.getGameState().getActiveTurnPlayerId();
        UUID p1HeroId = p1.getCurrentDeck().getHeroList().get(0).getUuid();
        UUID p2HeroId = p2.getCurrentDeck().getHeroList().get(0).getUuid();

        if (activeId.equals(p1.getUuid())) {
            engine.submitHeroChoice(p1.getUuid(), p1HeroId, p2HeroId);
        } else {
            engine.submitHeroChoice(p2.getUuid(), p2HeroId, p1HeroId);
        }

        engine.submitPreparationCards(p1.getUuid(), List.of());
        engine.submitPreparationCards(p2.getUuid(), List.of());

        assertEquals(TurnPhase.GAME_OVER, engine.getTurnPhase());

        MatchSnapshotDTO lastSnapshot = engine.getSnapshotHistory().get(engine.getSnapshotHistory().size() - 1);
        MatchSnapshotDTO.MatchResultSummaryDTO summary = lastSnapshot.getMatchResult();

        assertNotNull(summary);
        assertEquals(p1.getUuid(), summary.getWinnerPlayerId());
        assertEquals("ELIMINATION", summary.getWinReason());
        assertEquals(1, summary.getTurnsElapsed());

        // P1 causou 5 de dano físico (6 ATQ - 1 DEF), recebeu 2 de dano (3 ATQ - 1 DEF) e curou 2 no fim do combate
        assertEquals(5, summary.getPlayer1Stats().getTotalDamageDealt());
        assertEquals(2, summary.getPlayer1Stats().getTotalDamageTaken());
        assertEquals(2, summary.getPlayer1Stats().getTotalHealingDone());
        assertEquals(1, summary.getPlayer1Stats().getHeroesDefeated());
        assertTrue(summary.getPlayer1Stats().isWinner());

        // P2 causou 2 de dano físico, recebeu 5 de dano e não curou nada
        assertEquals(2, summary.getPlayer2Stats().getTotalDamageDealt());
        assertEquals(5, summary.getPlayer2Stats().getTotalDamageTaken());
        assertEquals(0, summary.getPlayer2Stats().getTotalHealingDone());
        assertFalse(summary.getPlayer2Stats().isWinner());

        // P1: 5 de dano + 2 de cura = 7 pontos
        assertEquals(7, summary.getPlayer1Stats().getMatchScore());

        // P2: 2 de dano + 0 de cura = 2 pontos
        assertEquals(2, summary.getPlayer2Stats().getMatchScore());
    }
}

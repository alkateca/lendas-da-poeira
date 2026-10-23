package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.engine.MatchSnapshotDTO;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.MatchMode;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EloRatingServiceTest {

    @Test
    void shouldInitializeAndResetWeeklyEloWhenIsoWeekChanges() {
        EloRatingService eloService = new EloRatingService(null);

        Player player = new Player();
        player.setUuid(UUID.randomUUID());
        player.setName("Alkateca");
        player.setPermanentElo(1450);
        player.setWeeklyElo(1320);
        player.setLastWeeklyResetWeek("2025-W01"); // Semana antiga

        boolean resetOccurred = eloService.ensurePlayerEloInitializedAndCurrentWeek(player);

        assertTrue(resetOccurred);
        assertEquals(1450, player.getPermanentElo(), "ELO Permanente NUNCA deve ser resetado na virada da semana");
        assertEquals(1000, player.getWeeklyElo(), "ELO Semanal deve voltar para 1000 na virada da semana");
        assertEquals(eloService.getCurrentIsoWeekKey(), player.getLastWeeklyResetWeek());
    }

    @Test
    void shouldCalculateBothPermanentAndWeeklyEloAfterPvpMatch() {
        PlayerStorageService mockStorage = new PlayerStorageService();
        EloRatingService eloService = new EloRatingService(mockStorage);

        Player p1 = new Player(UUID.randomUUID(), "Jogador 1", null);
        Player p2 = new Player(UUID.randomUUID(), "Jogador 2", null);

        MatchSnapshotDTO.PlayerMatchStatsDTO p1Stats = new MatchSnapshotDTO.PlayerMatchStatsDTO();
        MatchSnapshotDTO.PlayerMatchStatsDTO p2Stats = new MatchSnapshotDTO.PlayerMatchStatsDTO();
        MatchSnapshotDTO.MatchResultSummaryDTO summary = new MatchSnapshotDTO.MatchResultSummaryDTO(
                p1.getUuid(), p1.getName(), "ELIMINATION", 3, p1Stats, p2Stats
        );

        eloService.processMatchEloUpdate(p1, p2, p1.getUuid(), MatchMode.PVP, summary);

        // Jogadores começam com 1000: vitória em PvP soma +16 no Permanente (K=32) e +20 no Semanal (K=40)
        assertEquals(1016, p1.getPermanentElo());
        assertEquals(1020, p1.getWeeklyElo());
        assertEquals(16, summary.getPlayer1Stats().getPermanentEloDelta());
        assertEquals(20, summary.getPlayer1Stats().getWeeklyEloDelta());

        assertEquals(984, p2.getPermanentElo());
        assertEquals(980, p2.getWeeklyElo());
        assertEquals(-16, summary.getPlayer2Stats().getPermanentEloDelta());
        assertEquals(-20, summary.getPlayer2Stats().getWeeklyEloDelta());
    }
}

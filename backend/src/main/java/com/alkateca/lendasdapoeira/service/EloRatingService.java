package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.engine.MatchSnapshotDTO;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.MatchMode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.UUID;

@Service
public class EloRatingService {

    public static final int INITIAL_ELO = 1000;
    public static final int MIN_ELO_FLOOR = 100;

    private static final int K_FACTOR_PERMANENT_PVP = 32;
    private static final int K_FACTOR_WEEKLY_PVP = 40;
    private static final int K_FACTOR_PVE = 12;

    private final PlayerStorageService playerStorageService;

    public EloRatingService(PlayerStorageService playerStorageService) {
        this.playerStorageService = playerStorageService;
    }

    /**
     * Retorna o identificador da semana atual (ex: "2026-W43").
     */
    public String getCurrentIsoWeekKey() {
        LocalDate now = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
        int year = now.get(IsoFields.WEEK_BASED_YEAR);
        int week = now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return String.format("%d-W%02d", year, week);
    }

    /**
     * Verifica se o jogador precisa sofrer reset do ELO Semanal (virada de semana)
     * ou inicialização de valores legados (quando lido de JSON antigo com 0).
     */
    public boolean ensurePlayerEloInitializedAndCurrentWeek(Player player) {
        if (player == null) return false;
        boolean modified = false;

        if (player.getPermanentElo() <= 0) {
            player.setPermanentElo(INITIAL_ELO);
            modified = true;
        }
        if (player.getWeeklyElo() <= 0) {
            player.setWeeklyElo(INITIAL_ELO);
            modified = true;
        }

        String currentWeek = getCurrentIsoWeekKey();
        if (!currentWeek.equals(player.getLastWeeklyResetWeek())) {
            player.setWeeklyElo(INITIAL_ELO);
            player.setLastWeeklyResetWeek(currentWeek);
            modified = true;
        }

        return modified;
    }

    /**
     * Scheduler que verifica e aplica o reset semanal automaticamente (roda a cada 1 hora e toda segunda às 00:00).
     */
    @Scheduled(fixedRate = 3600000)
    public void scheduledWeeklyEloResetCheck() {
        if (playerStorageService == null) return;
        playerStorageService.resetAllPlayersWeeklyEloIfNeeded(this::ensurePlayerEloInitializedAndCurrentWeek);
    }

    /**
     * Calcula e persiste o novo ELO Permanente e ELO Semanal ao término de uma partida,
     * preenchendo os metadados no MatchResultSummaryDTO para exibição na tela de vitória/derrota.
     */
    public void processMatchEloUpdate(
            Player matchPlayer1,
            Player matchPlayer2,
            UUID winnerId,
            MatchMode mode,
            MatchSnapshotDTO.MatchResultSummaryDTO resultSummary) {

        if (matchPlayer1 == null || matchPlayer2 == null || winnerId == null) return;

        Player storedP1 = playerStorageService.getPlayer(matchPlayer1.getUuid());
        Player storedP2 = (mode == MatchMode.PVP) ? playerStorageService.getPlayer(matchPlayer2.getUuid()) : null;

        Player p1 = storedP1 != null ? storedP1 : matchPlayer1;
        Player p2 = storedP2 != null ? storedP2 : matchPlayer2;

        ensurePlayerEloInitializedAndCurrentWeek(p1);
        ensurePlayerEloInitializedAndCurrentWeek(p2);

        boolean p1Won = winnerId.equals(p1.getUuid());
        double scoreP1 = p1Won ? 1.0 : 0.0;
        double scoreP2 = p1Won ? 0.0 : 1.0;

        int kPerm = (mode == MatchMode.PVP) ? K_FACTOR_PERMANENT_PVP : K_FACTOR_PVE;
        int kWeek = (mode == MatchMode.PVP) ? K_FACTOR_WEEKLY_PVP : K_FACTOR_PVE;

        // 1. Cálculo para ELO Permanente
        int p1OldPerm = p1.getPermanentElo();
        int p2OldPerm = p2.getPermanentElo();
        int p1PermDelta = calculateEloDelta(p1OldPerm, p2OldPerm, scoreP1, kPerm);
        int p2PermDelta = calculateEloDelta(p2OldPerm, p1OldPerm, scoreP2, kPerm);

        int p1NewPerm = Math.max(MIN_ELO_FLOOR, p1OldPerm + p1PermDelta);
        int p2NewPerm = Math.max(MIN_ELO_FLOOR, p2OldPerm + p2PermDelta);

        // 2. Cálculo para ELO Semanal
        int p1OldWeek = p1.getWeeklyElo();
        int p2OldWeek = p2.getWeeklyElo();
        int p1WeekDelta = calculateEloDelta(p1OldWeek, p2OldWeek, scoreP1, kWeek);
        int p2WeekDelta = calculateEloDelta(p2OldWeek, p1OldWeek, scoreP2, kWeek);

        int p1NewWeek = Math.max(MIN_ELO_FLOOR, p1OldWeek + p1WeekDelta);
        int p2NewWeek = Math.max(MIN_ELO_FLOOR, p2OldWeek + p2WeekDelta);

        // Atualiza as instâncias tanto da partida quanto do storage
        p1.setPermanentElo(p1NewPerm);
        p1.setWeeklyElo(p1NewWeek);
        matchPlayer1.setPermanentElo(p1NewPerm);
        matchPlayer1.setWeeklyElo(p1NewWeek);

        p2.setPermanentElo(p2NewPerm);
        p2.setWeeklyElo(p2NewWeek);
        matchPlayer2.setPermanentElo(p2NewPerm);
        matchPlayer2.setWeeklyElo(p2NewWeek);

        if (storedP1 != null) {
            playerStorageService.saveOrUpdatePlayer(p1);
        }
        if (storedP2 != null && mode == MatchMode.PVP) {
            playerStorageService.saveOrUpdatePlayer(p2);
        }

        // Injeta os novos valores e variações no resumo da partida para o Front-End exibir
        if (resultSummary != null) {
            if (resultSummary.getPlayer1Stats() != null) {
                resultSummary.getPlayer1Stats().setPermanentElo(p1NewPerm);
                resultSummary.getPlayer1Stats().setPermanentEloDelta(p1NewPerm - p1OldPerm);
                resultSummary.getPlayer1Stats().setWeeklyElo(p1NewWeek);
                resultSummary.getPlayer1Stats().setWeeklyEloDelta(p1NewWeek - p1OldWeek);
            }
            if (resultSummary.getPlayer2Stats() != null) {
                resultSummary.getPlayer2Stats().setPermanentElo(p2NewPerm);
                resultSummary.getPlayer2Stats().setPermanentEloDelta(p2NewPerm - p2OldPerm);
                resultSummary.getPlayer2Stats().setWeeklyElo(p2NewWeek);
                resultSummary.getPlayer2Stats().setWeeklyEloDelta(p2NewWeek - p2OldWeek);
            }
        }
    }

    public int calculateEloDelta(int playerRating, int opponentRating, double actualScore, int kFactor) {
        double expectedScore = 1.0 / (1.0 + Math.pow(10.0, (opponentRating - playerRating) / 400.0));
        return (int) Math.round(kFactor * (actualScore - expectedScore));
    }
}

package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MatchRewardEconomyIntegrationTest {

    private Player createPlayerWithSingleHero(String name, int initialGold, int hp, int atq, int def) {
        Player player = new Player();
        player.setUuid(UUID.randomUUID());
        player.setName(name);
        player.setGold(initialGold);

        HeroCard hero = new HeroCard();
        hero.setUuid(UUID.randomUUID());
        hero.setCardName("Herói de " + name);
        hero.setOwnerId(player.getUuid());
        hero.setVidaMaxima(hp);
        hero.setVidaAtual(hp);
        hero.setAtaque(atq);
        hero.setDefesa(def);
        hero.setEspirito(1);
        hero.setEstaVivo(true);
        hero.setEstaAtivo(true);
        hero.setZoneId(ZoneId.BENCH);

        player.setCurrentDeck(new Deck("Deck", new ArrayList<>(), new ArrayList<>(List.of(hero))));
        return player;
    }

    @Test
    @DisplayName("Partida PvE deve creditar exatamente 50 Gold ao vencedor apenas no GAME_OVER")
    void shouldGrant50GoldOnPveVictoryOnlyAtGameOver() {
        // Arrange
        Player p1 = createPlayerWithSingleHero("Jogador PvE", 100, 20, 10, 2);
        Player bot = createPlayerWithSingleHero("Bot", 100, 5, 2, 1);

        GameEngine engine = new GameEngine(p1, bot);
        engine.setPvpMatch(false);
        engine.startGame();

        // Assert intermediário: snapshots de início de partida NÃO devem alterar o saldo de Gold
        assertEquals(100, p1.getGold(), "O saldo não deve ser alterado durante snapshots intermediários.");

        // Act
        UUID activeTurnPlayer = engine.getGameState().getActiveTurnPlayerId();
        UUID p1HeroId = p1.getCurrentDeck().getHeroList().get(0).getUuid();
        UUID botHeroId = bot.getCurrentDeck().getHeroList().get(0).getUuid();

        if (activeTurnPlayer.equals(p1.getUuid())) {
            engine.submitHeroChoice(p1.getUuid(), p1HeroId, botHeroId);
        } else {
            engine.submitHeroChoice(bot.getUuid(), botHeroId, p1HeroId);
        }

        engine.submitPreparationCards(p1.getUuid(), List.of());
        engine.submitPreparationCards(bot.getUuid(), List.of());

        // Assert
        assertEquals(TurnPhase.GAME_OVER, engine.getTurnPhase());
        assertEquals(p1.getUuid(), engine.getWinnerPlayerId());
        assertEquals(150, p1.getGold(), "Vencedor de partida PvE deve receber +50 Gold (100 -> 150).");
        assertEquals(100, bot.getGold(), "Derrotado em partida PvE não deve receber Gold de vitória.");
    }

    @Test
    @DisplayName("Partida PvP deve creditar exatamente 150 Gold ao vencedor e bloquear duplicação em chamadas repetidas")
    void shouldGrant150GoldOnPvpVictoryWithoutDuplication() {
        // Arrange
        Player p1 = createPlayerWithSingleHero("Jogador 1", 300, 20, 12, 2);
        Player p2 = createPlayerWithSingleHero("Jogador 2", 300, 6, 3, 1);

        GameEngine engine = new GameEngine(p1, p2);
        engine.setPvpMatch(true);
        engine.startGame();

        // Act
        UUID activeTurnPlayer = engine.getGameState().getActiveTurnPlayerId();
        UUID p1HeroId = p1.getCurrentDeck().getHeroList().get(0).getUuid();
        UUID p2HeroId = p2.getCurrentDeck().getHeroList().get(0).getUuid();

        if (activeTurnPlayer.equals(p1.getUuid())) {
            engine.submitHeroChoice(p1.getUuid(), p1HeroId, p2HeroId);
        } else {
            engine.submitHeroChoice(p2.getUuid(), p2HeroId, p1HeroId);
        }

        engine.submitPreparationCards(p1.getUuid(), List.of());
        engine.submitPreparationCards(p2.getUuid(), List.of());

        // Chamada extra a buildMatchResultSummary não deve duplicar o prêmio
        engine.buildMatchResultSummary();

        // Assert
        assertEquals(TurnPhase.GAME_OVER, engine.getTurnPhase());
        assertEquals(450, p1.getGold(), "Vencedor de partida PvP deve receber exatamente +150 Gold (300 -> 450).");
        assertEquals(300, p2.getGold(), "Derrotado não deve receber Gold.");
    }

    @Test
    @DisplayName("Desistência em PvP (FORFEIT) deve conceder 150 Gold ao oponente vencedor e 0 ao desistente")
    void shouldGrant150GoldToOpponentWhenPlayerForfeitsInPvp() {
        // Arrange
        Player p1 = createPlayerWithSingleHero("Jogador 1", 200, 20, 5, 2);
        Player p2 = createPlayerWithSingleHero("Jogador 2", 200, 20, 5, 2);

        GameEngine engine = new GameEngine(p1, p2);
        engine.setPvpMatch(true);
        engine.startGame();

        // Act
        engine.forfeitMatch(p2.getUuid());

        // Assert
        assertEquals(TurnPhase.GAME_OVER, engine.getTurnPhase());
        assertEquals(p1.getUuid(), engine.getWinnerPlayerId());
        assertEquals(350, p1.getGold(), "Vencedor por W.O. no PvP deve receber +150 Gold (200 -> 350).");
        assertEquals(200, p2.getGold(), "Jogador que desistiu deve receber 0 Gold.");
    }
}
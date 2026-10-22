package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.DTO.MatchDtos.MatchmakingStatusResponseDTO;
import com.alkateca.lendasdapoeira.DTO.MatchDtos.PlayerActionRequestDTO;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Match;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.MatchStatus;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;
import com.alkateca.lendasdapoeira.service.MatchmakingService;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ParallelMatchEngineTest {

    private Player createMockPlayer(String name) {
        Player player = new Player();
        player.setUuid(UUID.randomUUID());
        player.setName(name);

        List<HeroCard> heroes = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            HeroCard hero = new HeroCard();
            hero.setUuid(UUID.randomUUID());
            hero.setCardName("Herói " + i + " de " + name);
            hero.setOwnerId(player.getUuid());
            hero.setVidaMaxima(20);
            hero.setVidaAtual(20);
            hero.setAtaque(3);
            hero.setDefesa(1);
            hero.setEspirito(2);
            hero.setEstaVivo(true);
            hero.setEstaAtivo(true);
            hero.setZoneId(ZoneId.BENCH);
            CardFactory.buildCardLogic(hero);
            heroes.add(hero);
        }

        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            Card card = new Card();
            card.setUuid(UUID.randomUUID());
            card.setCardName("Bola de fogo");
            card.setCardType(CardType.SPELL);
            card.setOwnerId(player.getUuid());
            card.setZoneId(ZoneId.DECK);
            CardFactory.buildCardLogic(card);
            cards.add(card);
        }

        Deck deck = new Deck();
        deck.setHeroList(heroes);
        deck.setCardList(cards);
        player.setCurrentDeck(deck);

        return player;
    }

    @Test
    void multipleConcurrentMatchesShouldRunStrictlyIsolated() throws InterruptedException, ExecutionException {
        MatchmakingService matchmakingService = new MatchmakingService();
        int totalMatches = 10;
        ExecutorService executor = Executors.newFixedThreadPool(totalMatches);

        List<Callable<UUID>> tasks = new ArrayList<>();

        for (int i = 0; i < totalMatches; i++) {
            final int index = i;
            tasks.add(() -> {
                Player p1 = createMockPlayer("P1_M" + index);
                Player p2 = createMockPlayer("P2_M" + index);

                // Cria partida PvE isolada
                MatchmakingStatusResponseDTO response = matchmakingService.startPveMatch(p1, p2);
                assertNotNull(response.getMatchId());

                Match match = matchmakingService.getMatchOrThrow(response.getMatchId());
                assertEquals(MatchStatus.RUNNING, match.getStatus());

                GameEngine engine = match.getGameEngine();

                // Verifica que a sequência de snapshots da partida começa do 1 (isolada de outras partidas)
                assertEquals(1L, engine.getSnapshotHistory().get(0).getSnapshotSequence());

                // Se for a vez do humano na fase CHOICE, realiza a escolha de herói
                if (engine.getTurnPhase() == TurnPhase.CHOICE
                        && p1.getUuid().equals(engine.getGameState().getActiveTurnPlayerId())) {
                    PlayerActionRequestDTO action = new PlayerActionRequestDTO();
                    action.setPlayerId(p1.getUuid());
                    action.setSelectedOwnHeroId(p1.getCurrentDeck().getHeroList().get(0).getUuid());
                    action.setSelectedEnemyHeroId(p2.getCurrentDeck().getHeroList().get(0).getUuid());
                    matchmakingService.processPlayerAction(match.getMatchId(), action);
                }

                assertEquals(TurnPhase.PREPARATION, engine.getTurnPhase());
                return match.getMatchId();
            });
        }

        List<Future<UUID>> results = executor.invokeAll(tasks);
        executor.shutdown();

        Set<UUID> matchIds = new CopyOnWriteArraySet<>();
        for (Future<UUID> future : results) {
            matchIds.add(future.get());
        }

        // Garante que todas as 10 partidas têm IDs únicos e estados independentes
        assertEquals(totalMatches, matchIds.size());
        assertEquals(totalMatches, matchmakingService.getActiveMatchCount());
    }
}

package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.DTO.MatchDtos.PlayerActionRequestDTO;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Match;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.ZoneId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerCollectionIntegrationTest {

    @Test
    void testStarterCollectionAndDeckOwnershipValidation() {
        // Arrange (Preparação)
        DeckValidatorService validatorService = new DeckValidatorService();
        Player player = new Player(UUID.randomUUID(), "TestPlayer", null);

        HeroCard starterHero = new HeroCard();
        starterHero.setCardName("Moyra, Aprendiz da Santa");

        Card bolaDeFogo = new Card();
        bolaDeFogo.setCardName("Bola de fogo");

        Deck starterDeck = new Deck();
        starterDeck.setHeroList(List.of(starterHero));
        starterDeck.setCardList(List.of(bolaDeFogo)); // Concede apenas 1 cópia inicial

        // Act 1 (Ação: Conceder Kit Inicial)
        validatorService.ensureStarterCollection(player, starterDeck, List.of());

        // Assert 1 (Validação do Kit Inicial)
        assertEquals(1, player.getOwnedHeroCount("Moyra, Aprendiz da Santa"), "O jogador deve possuir 1 cópia do herói inicial.");
        assertEquals(1, player.getOwnedCardCount("Bola de fogo"), "O jogador deve possuir exatamente 1 cópia de Bola de fogo.");

        // Act & Assert 2 (Tentativa de usar 2 cópias possuindo apenas 1 na coleção)
        Deck invalidCopiesDeck = new Deck();
        invalidCopiesDeck.setHeroList(List.of(starterHero));
        invalidCopiesDeck.setCardList(List.of(bolaDeFogo, bolaDeFogo));

        assertThrows(IllegalArgumentException.class, () -> {
            validatorService.validateDeckOwnership(player, invalidCopiesDeck);
        }, "Deve rejeitar baralho que utiliza 2 cópias quando o jogador possui apenas 1.");

        // Act & Assert 3 (Tentativa de usar Herói não pertencente à coleção)
        HeroCard unownedHero = new HeroCard();
        unownedHero.setCardName("Kael, Domador de Feras");

        Deck unownedHeroDeck = new Deck();
        unownedHeroDeck.setHeroList(List.of(unownedHero));
        unownedHeroDeck.setCardList(List.of(bolaDeFogo));

        assertThrows(IllegalArgumentException.class, () -> {
            validatorService.validateDeckOwnership(player, unownedHeroDeck);
        }, "Deve rejeitar baralho contendo herói não adquirido na coleção.");
    }

    @Test
    void testPostMatchPersistencePreservesPlayerProfileDecksAndCollectionAndIgnoresBot() {
        // Arrange
        PlayerStorageService storageService = new PlayerStorageService();
        EloRatingService eloService = new EloRatingService(storageService);
        MatchmakingService matchmakingService = new MatchmakingService(eloService, storageService);

        UUID playerId = UUID.randomUUID();
        Player storedPlayer = new Player(playerId, "alka", null);
        storedPlayer.setGold(5000);
        storedPlayer.addHeroToCollection("Chefe Carrasco", 3);
        storedPlayer.addCardToCollection("Cheiro de Sangue", 3);

        HeroCard savedHeroTemplate = new HeroCard();
        savedHeroTemplate.setCardName("Chefe Carrasco");
        savedHeroTemplate.setVidaMaxima(15);
        savedHeroTemplate.setVidaAtual(15);
        Deck savedDeck = new Deck("Deck Carrasco", new ArrayList<>(), new ArrayList<>(List.of(savedHeroTemplate)));
        storedPlayer.setDecks(new ArrayList<>(List.of(savedDeck)));
        storedPlayer.setCurrentDeck(savedDeck);
        storageService.saveOrUpdatePlayer(storedPlayer);

        // Cria instância temporária de partida (como o MatchController faz) sem coleção/decks
        HeroCard matchHero = new HeroCard();
        matchHero.setUuid(UUID.randomUUID());
        matchHero.setOwnerId(playerId);
        matchHero.setCardName("Chefe Carrasco");
        matchHero.setAtaque(20);
        matchHero.setDefesa(2);
        matchHero.setVidaMaxima(15);
        matchHero.setVidaAtual(15);
        matchHero.setEstaVivo(true);
        matchHero.setEstaAtivo(true);
        matchHero.setZoneId(ZoneId.BENCH);

        Player transientMatchPlayer = new Player(playerId, "alka", new Deck("Deck de Batalha", new ArrayList<>(), new ArrayList<>(List.of(matchHero))));
        transientMatchPlayer.setGold(5000);

        HeroCard botHero = new HeroCard();
        botHero.setUuid(UUID.randomUUID());
        botHero.setOwnerId(PresetDeckService.PLAYER_2_UUID);
        botHero.setCardName("Bot Hero");
        botHero.setAtaque(1);
        botHero.setDefesa(0);
        botHero.setVidaMaxima(5);
        botHero.setVidaAtual(5);
        botHero.setEstaVivo(true);
        botHero.setEstaAtivo(true);
        botHero.setZoneId(ZoneId.BENCH);

        Player botPlayer = new Player(PresetDeckService.PLAYER_2_UUID, "\uD83E\uDD16 Bot - Teste", new Deck("Deck Bot", new ArrayList<>(), new ArrayList<>(List.of(botHero))));

        // Act: Inicia partida PvE e vence no primeiro combate
        var startResp = matchmakingService.startPveMatch(transientMatchPlayer, botPlayer);
        Match match = matchmakingService.getMatchOrThrow(startResp.getMatchId());

        if (match.getGameEngine().getGameState().getActiveTurnPlayerId().equals(playerId)) {
            PlayerActionRequestDTO choiceReq = new PlayerActionRequestDTO();
            choiceReq.setMatchId(match.getMatchId());
            choiceReq.setPlayerId(playerId);
            choiceReq.setSelectedOwnHeroId(matchHero.getUuid());
            choiceReq.setSelectedEnemyHeroId(botHero.getUuid());
            matchmakingService.processPlayerAction(match.getMatchId(), choiceReq);
        }

        PlayerActionRequestDTO prepReq = new PlayerActionRequestDTO();
        prepReq.setMatchId(match.getMatchId());
        prepReq.setPlayerId(playerId);
        prepReq.setPlayedCardUuids(List.of());
        matchmakingService.processPlayerAction(match.getMatchId(), prepReq);

        // Assert: O perfil salvo deve ter recebido +50 Gold e ELO sem perder os decks nem a coleção!
        Player afterMatch = storageService.getPlayer(playerId);
        assertNotNull(afterMatch);
        assertEquals(5050, afterMatch.getGold(), "O Gold ganho na partida deve ser sincronizado no perfil salvo.");
        assertEquals(3, afterMatch.getOwnedHeroCount("Chefe Carrasco"), "A coleção de heróis NÃO pode ser apagada após a partida.");
        assertEquals(3, afterMatch.getOwnedCardCount("Cheiro de Sangue"), "A coleção de cartas NÃO pode ser apagada após a partida.");
        assertEquals(1, afterMatch.getDecks().size(), "Os baralhos salvos NÃO podem ser apagados após a partida.");
        assertEquals("Deck Carrasco", afterMatch.getCurrentDeck().getDeckName(), "O currentDeck salvo NÃO pode ser sobrescrito pelo deck mutado de batalha.");
        assertNull(storageService.getPlayer(PresetDeckService.PLAYER_2_UUID), "O Bot NÃO deve ser salvo no banco de jogadores.");
    }
}

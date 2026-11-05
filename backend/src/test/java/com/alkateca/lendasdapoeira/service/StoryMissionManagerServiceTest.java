package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.DTO.MatchDtos.MatchmakingStatusResponseDTO;
import com.alkateca.lendasdapoeira.DTO.story.*;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.Colors;
import com.alkateca.lendasdapoeira.enums.ItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoryMissionManagerServiceTest {

    @Mock
    private StoryMissionLoaderService loaderService;

    @Mock
    private MatchmakingService matchmakingService;

    @Mock
    private PresetDeckService presetDeckService;

    @Mock
    private PlayerStorageService playerStorageService;

    private StoryTriggerEvaluator triggerEvaluator;
    private StoryMissionManagerService managerService;

    private StoryMissionDefinition redChap1;
    private StoryMissionDefinition redChap2;

    @BeforeEach
    void setUp() {
        triggerEvaluator = new StoryTriggerEvaluator();
        managerService = new StoryMissionManagerService(
                loaderService,
                triggerEvaluator,
                matchmakingService,
                presetDeckService,
                playerStorageService
        );

        redChap1 = new StoryMissionDefinition(
                "mission_red_01",
                "O Primeiro Clarão da Forja",
                "Caçadores das Cinzas",
                "RED",
                1,
                "Prólogo Cap 1",
                "Epílogo Cap 1",
                "DECK_RED_01",
                new StoryMissionDefinition.AiDeckConfig(
                        List.of("Alka, Lutador do Grupo dos Heróis"),
                        List.of("Golpe de Memória")
                ),
                List.of(new ScriptedDialogueDTO(
                        "ON_CARD_PLAYED", null, "Bola de Fogo", null,
                        "Narrador dos Arquivos", "Fogo conjurado!"
                ))
        );

        redChap2 = new StoryMissionDefinition(
                "mission_red_02",
                "Cauterização nas Cinzas",
                "Círculo das Chamas",
                "RED",
                2,
                "Prólogo Cap 2",
                "Epílogo Cap 2",
                "DECK_RED_02",
                new StoryMissionDefinition.AiDeckConfig(List.of(), List.of()),
                List.of()
        );
    }

    @Test
    @DisplayName("Deve bloquear o Capítulo 2 até que o jogador conclua o Capítulo 1 da mesma cor")
    void shouldUnlockChapter2OnlyAfterChapter1IsCompleted() {
        when(loaderService.listAllMissions()).thenReturn(List.of(redChap1, redChap2));
        String playerId = "player-progress-test";

        // Antes de vencer o Capítulo 1
        List<StoryMissionSummaryDTO> initialCatalog = managerService.getMissionsCatalogForPlayer(playerId);
        assertEquals(2, initialCatalog.size());
        assertTrue(initialCatalog.get(0).unlocked(), "Capítulo 1 deve iniciar desbloqueado");
        assertFalse(initialCatalog.get(0).completed());
        assertFalse(initialCatalog.get(1).unlocked(), "Capítulo 2 deve iniciar bloqueado");

        // Jogador vence o Capítulo 1
        boolean firstTime = managerService.markMissionAsCompleted(playerId, "mission_red_01");
        assertTrue(firstTime);
        assertFalse(managerService.markMissionAsCompleted(playerId, "mission_red_01"), "Segunda marcação deve retornar false");

        // Consulta o catálogo novamente
        List<StoryMissionSummaryDTO> updatedCatalog = managerService.getMissionsCatalogForPlayer(playerId);
        assertTrue(updatedCatalog.get(0).completed());
        assertTrue(updatedCatalog.get(1).unlocked(), "Capítulo 2 deve desbloquear após concluir o Capítulo 1");
    }

    @Test
    @DisplayName("Deve montar o baralho do jogador na cor da missão e o baralho WHITE da IA ao iniciar partida")
    void shouldBuildPlayerAndWhiteAiDecksAndStartPveMatch() {
        HeroCard xiaRed = createMockHero("Xia, Caçador de Bestas", Colors.RED);
        HeroCard asahumRed = createMockHero("Asahum, Caçadora de Bestas", Colors.RED);
        HeroCard arsenalRed = createMockHero("Arsenal Vivo", Colors.RED);

        HeroCard alkaWhite = createMockHero("Alka, Lutador do Grupo dos Heróis", Colors.WHITE);
        HeroCard leoneWhite = createMockHero("Leone, Clériga do Grupo dos Heróis", Colors.WHITE);
        HeroCard miragemWhite = createMockHero("Alucinação Cintilante", Colors.WHITE);

        Card fireballRed = createMockCard("Bola de Fogo", Colors.RED, CardType.SPELL);
        Card whiteStrike = createMockCard("Golpe de Memória", Colors.WHITE, CardType.ACTION);

        when(presetDeckService.getCatalogHeroes()).thenReturn(
                List.of(xiaRed, asahumRed, arsenalRed, alkaWhite, leoneWhite, miragemWhite)
        );
        when(presetDeckService.getCatalogCards()).thenReturn(List.of(fireballRed, whiteStrike));

        UUID generatedMatchId = UUID.randomUUID();
        MatchmakingStatusResponseDTO mockStatus = new MatchmakingStatusResponseDTO();
        mockStatus.setMatchId(generatedMatchId);
        mockStatus.setInitialSnapshots(List.of());

        when(matchmakingService.startPveMatch(any(Player.class), any(Player.class))).thenReturn(mockStatus);

        UUID playerUuid = UUID.randomUUID();
        StartStoryMissionResponseDTO response = managerService.startStoryMatch(redChap1, playerUuid, "José");

        assertEquals(generatedMatchId.toString(), response.matchId());
        assertEquals(playerUuid.toString(), response.localPlayerId());

        ArgumentCaptor<Player> humanCaptor = ArgumentCaptor.forClass(Player.class);
        ArgumentCaptor<Player> botCaptor = ArgumentCaptor.forClass(Player.class);
        verify(matchmakingService, times(1)).startPveMatch(humanCaptor.capture(), botCaptor.capture());

        Player human = humanCaptor.getValue();
        Player bot = botCaptor.getValue();

        // Verifica se o jogador recebeu 3 heróis RED e 30 cartas
        assertEquals(3, human.getDeck().getHeroList().size());
        assertTrue(human.getDeck().getHeroList().stream().allMatch(h -> h.getColor() == Colors.RED));
        assertEquals(30, human.getDeck().getCardList().size());

        // Verifica se a máquina ("Ecos da Memória") recebeu os 3 heróis WHITE e 30 cartas
        assertEquals("Ecos da Memória (WHITE)", bot.getName());
        assertEquals(3, bot.getDeck().getHeroList().size());
        assertTrue(bot.getDeck().getHeroList().stream().allMatch(h -> h.getColor() == Colors.WHITE));
        assertEquals(30, bot.getDeck().getCardList().size());

        // Verifica o rastreamento de gatilhos vinculados ao matchId gerado
        List<ScriptedDialogueDTO> triggered = managerService.checkCardPlayedTriggers(generatedMatchId, "Bola de Fogo");
        assertEquals(1, triggered.size());
        assertTrue(managerService.checkCardPlayedTriggers(generatedMatchId, "Bola de Fogo").isEmpty());
    }

    private HeroCard createMockHero(String name, Colors color) {
        HeroCard h = new HeroCard();
        h.setUuid(UUID.randomUUID());
        h.setCardName(name);
        h.setColor(color);
        h.setCardType(CardType.HERO);
        h.setVidaMaxima(16);
        h.setVidaAtual(16);
        h.setAtaque(4);
        h.setDefesa(2);
        h.setEspirito(2);
        return h;
    }

    private Card createMockCard(String name, Colors color, CardType type) {
        Card c = new Card();
        c.setUuid(UUID.randomUUID());
        c.setCardName(name);
        c.setColor(color);
        c.setCardType(type);
        c.setItemType(ItemType.NENHUM);
        return c;
    }
}

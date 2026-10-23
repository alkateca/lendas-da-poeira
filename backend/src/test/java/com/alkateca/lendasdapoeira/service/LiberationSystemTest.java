package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.engine.CardFactory;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.engine.MatchSnapshotDTO;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LiberationSystemTest {

    private GameEngine gameEngine;
    private Player player1;
    private Player player2;
    private HeroCard moyraBaseHero;
    private HeroCard enemyHero;
    private Card liberationCard;

    @BeforeEach
    void setUp() {
        UUID p1Id = UUID.randomUUID();
        UUID p2Id = UUID.randomUUID();

        moyraBaseHero = new HeroCard();
        moyraBaseHero.setUuid(UUID.randomUUID());
        moyraBaseHero.setOwnerId(p1Id);
        moyraBaseHero.setCardName("Moyra, Aprendiz da Santa");
        moyraBaseHero.setCardType(CardType.HERO);
        moyraBaseHero.setColor(Colors.WHITE);
        moyraBaseHero.setEspirito(3);
        moyraBaseHero.setAtaque(3);
        moyraBaseHero.setDefesa(2);
        moyraBaseHero.setVidaMaxima(16);
        moyraBaseHero.setVidaAtual(16);
        moyraBaseHero.setMaxWeaponSlots(2);
        moyraBaseHero.setEstaVivo(true);
        moyraBaseHero.setEstaAtivo(true);
        CardFactory.buildCardLogic(moyraBaseHero);

        enemyHero = new HeroCard();
        enemyHero.setUuid(UUID.randomUUID());
        enemyHero.setOwnerId(p2Id);
        enemyHero.setCardName("Alka, Lutador do Grupo dos Heróis");
        enemyHero.setCardType(CardType.HERO);
        enemyHero.setColor(Colors.RED);
        enemyHero.setEspirito(2);
        enemyHero.setAtaque(4);
        enemyHero.setDefesa(2);
        enemyHero.setVidaMaxima(20);
        enemyHero.setVidaAtual(20);
        enemyHero.setMaxWeaponSlots(2);
        enemyHero.setEstaVivo(true);
        enemyHero.setEstaAtivo(true);

        liberationCard = new Card();
        liberationCard.setUuid(UUID.randomUUID());
        liberationCard.setOwnerId(p1Id);
        liberationCard.setCardName("Sagração da Santa das Lâminas");
        liberationCard.setCardType(CardType.THEURGY);
        liberationCard.setColor(Colors.WHITE);
        CardFactory.buildCardLogic(liberationCard);

        List<Card> p1DeckCards = createThirtyCards(p1Id);
        List<Card> p2DeckCards = createThirtyCards(p2Id);

        Deck p1Deck = new Deck("Deck P1", p1DeckCards, new ArrayList<>(List.of(moyraBaseHero)), new ArrayList<>(List.of(liberationCard)));
        Deck p2Deck = new Deck("Deck P2", p2DeckCards, new ArrayList<>(List.of(enemyHero)), new ArrayList<>());

        player1 = new Player(p1Id, "Viajante 1", p1Deck);
        player2 = new Player(p2Id, "Viajante 2", p2Deck);

        gameEngine = new GameEngine(player1, player2);
        gameEngine.startGame();
    }

    private List<Card> createThirtyCards(UUID ownerId) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            Card c = new Card();
            c.setUuid(UUID.randomUUID());
            c.setOwnerId(ownerId);
            c.setCardName("Lamina de Cristal");
            c.setCardType(CardType.ITEM);
            c.setItemType(ItemType.ARMA);
            c.setEmpunhadura(1);
            c.setColor(Colors.WHITE);
            CardFactory.buildCardLogic(c);
            cards.add(c);
        }
        return cards;
    }

    @Test
    @DisplayName("1. Carta de Liberação deve iniciar fora das 30 cartas do baralho na zona LIBERATION")
    void shouldStartInLiberationZoneOutsideMainDeck() {
        assertEquals(ZoneId.LIBERATION, liberationCard.getZoneId());
        assertEquals(0, liberationCard.getMissionCurrentProgress());
        assertEquals(2, liberationCard.getMissionTargetValue());
        assertFalse(player1.getCurrentDeck().getCardList().contains(liberationCard));

        MatchSnapshotDTO lastSnapshot = gameEngine.getSnapshotHistory().get(gameEngine.getSnapshotHistory().size() - 1);
        assertEquals(1, lastSnapshot.getLocalPlayer().getLiberationZone().size());
        assertEquals("Sagração da Santa das Lâminas", lastSnapshot.getLocalPlayer().getLiberationZone().get(0).getCardName());
    }

    @Test
    @DisplayName("2. Deve atualizar progresso da missão, emitir snapshots MISSION_PROGRESS e CARD_LIBERATED e mover para a HAND")
    void shouldTrackProgressAndLiberateCardToHand() {
        // Progresso 1/2
        gameEngine.trackMissionProgress(player1.getUuid(), "PLAY_SPELL", 1);
        assertEquals(1, liberationCard.getMissionCurrentProgress());
        assertEquals(ZoneId.LIBERATION, liberationCard.getZoneId());

        // Progresso 2/2 -> Libera para a Mão (HAND)
        gameEngine.trackMissionProgress(player1.getUuid(), "PLAY_SPELL", 1);
        assertEquals(2, liberationCard.getMissionCurrentProgress());
        assertEquals(ZoneId.HAND, liberationCard.getZoneId());
        assertTrue(player1.getCurrentDeck().getCardList().contains(liberationCard));

        boolean hasLiberatedEvent = gameEngine.getSnapshotHistory().stream()
                .anyMatch(s -> s.getTriggerContext() != null && "CARD_LIBERATED".equals(s.getTriggerContext().getEventType()));
        assertTrue(hasLiberatedEvent);
    }

    @Test
    @DisplayName("3. Ao jogar a Carta de Liberação em combate, deve evoluir o Herói preservando vida proporcional e anexos")
    void shouldEvolveHeroWhenLiberationCardIsResolvedInCombat() {
        // Simula que Moyra sofreu dano antes da evolução (8/16 = 50% de vida) e tem 1 arma anexada
        moyraBaseHero.setVidaAtual(8);

        Card attachedSword = player1.getCurrentDeck().getCardList().get(0);
        attachedSword.setZoneId(ZoneId.ATTACHED);
        attachedSword.setAttachedToCardId(moyraBaseHero.getUuid());

        // Completa a missão para colocar a carta na mão de Player 1
        gameEngine.trackMissionProgress(player1.getUuid(), "PLAY_SPELL", 2);
        assertEquals(ZoneId.HAND, liberationCard.getZoneId());

        // Força iniciativa para P1 e escolhe os heróis para o combate
        gameEngine.getGameState().setActiveTurnPlayerId(player1.getUuid());
        gameEngine.submitHeroChoice(player1.getUuid(), moyraBaseHero.getUuid(), enemyHero.getUuid());

        // P1 joga a Carta de Liberação recém-liberada; P2 passa sem jogar cartas
        gameEngine.submitPreparationCards(player1.getUuid(), List.of(liberationCard.getUuid()));
        gameEngine.submitPreparationCards(player2.getUuid(), List.of());

        // Verifica que Moyra evoluiu para "Moyra, Santa das Laminas"
        assertEquals("Moyra, Santa das Laminas", moyraBaseHero.getCardName());
        assertEquals(22, moyraBaseHero.getVidaMaxima());
        assertEquals(3, moyraBaseHero.getMaxWeaponSlots());

        // Verifica que a arma anexada continuou vinculada ao mesmo UUID do Herói evoluído
        assertEquals(ZoneId.ATTACHED, attachedSword.getZoneId());
        assertEquals(moyraBaseHero.getUuid(), attachedSword.getAttachedToCardId());

        // Verifica que o evento HERO_EVOLVED foi registrado no histórico de snapshots
        boolean hasHeroEvolvedSnapshot = gameEngine.getSnapshotHistory().stream()
                .anyMatch(s -> s.getTriggerContext() != null && "HERO_EVOLVED".equals(s.getTriggerContext().getEventType()));
        assertTrue(hasHeroEvolvedSnapshot);
    }
}

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
    private Card liberationSpellCard;
    private Card liberationReviveCard;

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
        enemyHero.setCardName("Inimigo de Teste");
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

        // Carta de Liberação 1: Sagração da Santa das Lâminas (Missão de Feitiços)
        liberationSpellCard = new Card();
        liberationSpellCard.setUuid(UUID.randomUUID());
        liberationSpellCard.setOwnerId(p1Id);
        liberationSpellCard.setCardName("Sagração da Santa das Lâminas");
        liberationSpellCard.setCardType(CardType.THEURGY);
        liberationSpellCard.setColor(Colors.WHITE);
        CardFactory.buildCardLogic(liberationSpellCard);

        // Carta de Liberação 2: Ascensão da Horda Lendária (Missão de Aliados Mortos)
        liberationReviveCard = new Card();
        liberationReviveCard.setUuid(UUID.randomUUID());
        liberationReviveCard.setOwnerId(p1Id);
        liberationReviveCard.setCardName("Ascensão da Horda Lendária");
        liberationReviveCard.setCardType(CardType.ACTION);
        liberationReviveCard.setColor(Colors.GREEN);
        CardFactory.buildCardLogic(liberationReviveCard);

        List<Card> p1DeckCards = createThirtyCards(p1Id);
        List<Card> p2DeckCards = createThirtyCards(p2Id);

        Deck p1Deck = new Deck("Deck P1", p1DeckCards, new ArrayList<>(List.of(moyraBaseHero)), new ArrayList<>(List.of(liberationSpellCard, liberationReviveCard)));
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
    @DisplayName("1. Cartas de Liberação devem iniciar na zona LIBERATION, fora do baralho principal de 30 cartas")
    void shouldStartInLiberationZoneOutsideMainDeck() {
        assertEquals(ZoneId.LIBERATION, liberationSpellCard.getZoneId());
        assertEquals(ZoneId.LIBERATION, liberationReviveCard.getZoneId());
        assertEquals(0, liberationSpellCard.getMissionCurrentProgress());
        assertFalse(player1.getCurrentDeck().getCardList().contains(liberationSpellCard));

        MatchSnapshotDTO lastSnapshot = gameEngine.getSnapshotHistory().get(gameEngine.getSnapshotHistory().size() - 1);
        assertEquals(2, lastSnapshot.getLocalPlayer().getLiberationZone().size());
    }

    @Test
    @DisplayName("2. Deve progredir missão de feitiços, emitir snapshot CARD_LIBERATED e mover para a HAND")
    void shouldTrackSpellMissionAndLiberateToHand() {
        gameEngine.trackMissionProgress(player1.getUuid(), "PLAY_SPELL", 1);
        assertEquals(1, liberationSpellCard.getMissionCurrentProgress());
        assertEquals(ZoneId.LIBERATION, liberationSpellCard.getZoneId());

        gameEngine.trackMissionProgress(player1.getUuid(), "PLAY_SPELL", 1);
        assertEquals(2, liberationSpellCard.getMissionCurrentProgress());
        assertEquals(ZoneId.HAND, liberationSpellCard.getZoneId());
        assertTrue(player1.getCurrentDeck().getCardList().contains(liberationSpellCard));

        boolean hasLiberatedEvent = gameEngine.getSnapshotHistory().stream()
                .anyMatch(s -> s.getTriggerContext() != null && "CARD_LIBERATED".equals(s.getTriggerContext().getEventType()));
        assertTrue(hasLiberatedEvent);
    }

    @Test
    @DisplayName("3. Deve desanexar automaticamente os itens equipados quando um Herói morre")
    void shouldDetachItemsWhenHeroDies() {
        Card weapon = player1.getCurrentDeck().getCardList().get(0);
        weapon.setZoneId(ZoneId.ATTACHED);
        weapon.setAttachedToCardId(moyraBaseHero.getUuid());

        // Simula a morte do herói
        gameEngine.detachAllCardsFromHero(moyraBaseHero.getUuid(), ZoneId.DISCARD);

        assertEquals(ZoneId.DISCARD, weapon.getZoneId());
        assertNull(weapon.getAttachedToCardId());
    }

    @Test
    @DisplayName("4. Ao jogar a Carta de Liberação na preparação, deve evoluir o Herói ativo na arena")
    void shouldEvolveHeroWhenLiberationCardIsResolved() {
        moyraBaseHero.setVidaAtual(8); // 50% de vida

        gameEngine.trackMissionProgress(player1.getUuid(), "PLAY_SPELL", 2);
        assertEquals(ZoneId.HAND, liberationSpellCard.getZoneId());

        gameEngine.getGameState().setActiveTurnPlayerId(player1.getUuid());
        gameEngine.submitHeroChoice(player1.getUuid(), moyraBaseHero.getUuid(), enemyHero.getUuid());

        gameEngine.submitPreparationCards(player1.getUuid(), List.of(liberationSpellCard.getUuid()));
        gameEngine.submitPreparationCards(player2.getUuid(), List.of());

        assertEquals("Moyra, Santa das Laminas", moyraBaseHero.getCardName());
        assertEquals(22, moyraBaseHero.getVidaMaxima());
        assertEquals(3, moyraBaseHero.getMaxWeaponSlots());

        boolean hasHeroEvolvedSnapshot = gameEngine.getSnapshotHistory().stream()
                .anyMatch(s -> s.getTriggerContext() != null && "HERO_EVOLVED".equals(s.getTriggerContext().getEventType()));
        assertTrue(hasHeroEvolvedSnapshot);
    }
}

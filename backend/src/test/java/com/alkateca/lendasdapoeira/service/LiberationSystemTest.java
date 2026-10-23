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
    private HeroCard goblinSquadHero;
    private HeroCard goblinKingHero;
    private HeroCard enemyHero;

    private Card moyraLiberationCard;
    private Card goblinReviveLiberationCard;

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

        goblinSquadHero = new HeroCard();
        goblinSquadHero.setUuid(UUID.randomUUID());
        goblinSquadHero.setOwnerId(p1Id);
        goblinSquadHero.setCardName("Esquadrão Goblin");
        goblinSquadHero.setCardType(CardType.HERO);
        goblinSquadHero.setColor(Colors.GREEN);
        goblinSquadHero.setEspirito(2);
        goblinSquadHero.setAtaque(3);
        goblinSquadHero.setDefesa(2);
        goblinSquadHero.setVidaMaxima(14);
        goblinSquadHero.setVidaAtual(14);
        goblinSquadHero.setMaxWeaponSlots(2);
        goblinSquadHero.setEstaVivo(true);
        goblinSquadHero.setEstaAtivo(true);
        CardFactory.buildCardLogic(goblinSquadHero);

        goblinKingHero = new HeroCard();
        goblinKingHero.setUuid(UUID.randomUUID());
        goblinKingHero.setOwnerId(p1Id);
        goblinKingHero.setCardName("Rei Goblin");
        goblinKingHero.setCardType(CardType.HERO);
        goblinKingHero.setColor(Colors.GREEN);
        goblinKingHero.setEspirito(2);
        goblinKingHero.setAtaque(4);
        goblinKingHero.setDefesa(2);
        goblinKingHero.setVidaMaxima(15);
        goblinKingHero.setVidaAtual(15);
        goblinKingHero.setMaxWeaponSlots(2);
        goblinKingHero.setEstaVivo(true);
        goblinKingHero.setEstaAtivo(true);
        CardFactory.buildCardLogic(goblinKingHero);

        enemyHero = new HeroCard();
        enemyHero.setUuid(UUID.randomUUID());
        enemyHero.setOwnerId(p2Id);
        enemyHero.setCardName("Inimigo de Teste");
        enemyHero.setCardType(CardType.HERO);
        enemyHero.setColor(Colors.RED);
        enemyHero.setEspirito(2);
        enemyHero.setAtaque(4);
        enemyHero.setDefesa(2);
        enemyHero.setVidaMaxima(30);
        enemyHero.setVidaAtual(30);
        enemyHero.setMaxWeaponSlots(2);
        enemyHero.setEstaVivo(true);
        enemyHero.setEstaAtivo(true);

        // Carta de Liberação 1: Evolução da Moyra + Jogar 1 carta da mão
        moyraLiberationCard = new Card();
        moyraLiberationCard.setUuid(UUID.randomUUID());
        moyraLiberationCard.setOwnerId(p1Id);
        moyraLiberationCard.setCardName("Sagração da Santa das Lâminas");
        moyraLiberationCard.setCardType(CardType.THEURGY);
        moyraLiberationCard.setColor(Colors.WHITE);
        CardFactory.buildCardLogic(moyraLiberationCard);

        // Carta de Liberação 2: Reviver e Evoluir após 2 aliados mortos
        goblinReviveLiberationCard = new Card();
        goblinReviveLiberationCard.setUuid(UUID.randomUUID());
        goblinReviveLiberationCard.setOwnerId(p1Id);
        goblinReviveLiberationCard.setCardName("Ascensão da Horda Lendária");
        goblinReviveLiberationCard.setCardType(CardType.ACTION);
        goblinReviveLiberationCard.setColor(Colors.GREEN);
        CardFactory.buildCardLogic(goblinReviveLiberationCard);

        List<Card> p1DeckCards = createThirtyCards(p1Id);
        List<Card> p2DeckCards = createThirtyCards(p2Id);

        Deck p1Deck = new Deck(
                "Deck P1",
                p1DeckCards,
                new ArrayList<>(List.of(moyraBaseHero, goblinSquadHero, goblinKingHero)),
                new ArrayList<>(List.of(moyraLiberationCard, goblinReviveLiberationCard))
        );
        Deck p2Deck = new Deck(
                "Deck P2",
                p2DeckCards,
                new ArrayList<>(List.of(enemyHero)),
                new ArrayList<>()
        );

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
            c.setZoneId(ZoneId.DECK);
            CardFactory.buildCardLogic(c);
            cards.add(c);
        }
        return cards;
    }

    @Test
    @DisplayName("1. Cartas de Liberação iniciam na zona LIBERATION fora das 30 cartas do baralho e fora da mão inicial")
    void shouldInitializeInLiberationZoneOutsideMainDeck() {
        assertEquals(ZoneId.LIBERATION, moyraLiberationCard.getZoneId());
        assertEquals(ZoneId.LIBERATION, goblinReviveLiberationCard.getZoneId());
        assertFalse(player1.getCurrentDeck().getCardList().contains(moyraLiberationCard));
        assertFalse(player1.getCurrentDeck().getCardList().contains(goblinReviveLiberationCard));

        long initialHandCount = player1.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.HAND)
                .count();
        assertEquals(5, initialHandCount);
    }

    @Test
    @DisplayName("2. Caso Moyra: Ao cumprir missão, substitui pelo campeão evoluído e permite jogar 1 carta da mão à escolha")
    void shouldEvolveMoyraAndPlayChosenCardFromHand() {
        moyraBaseHero.setVidaAtual(8); // 50% de vida (8/16)

        // Equipa uma arma em Moyra antes da evolução
        Card preEquippedWeapon = player1.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.DECK)
                .findFirst()
                .orElseThrow();
        preEquippedWeapon.setZoneId(ZoneId.ATTACHED);
        preEquippedWeapon.setAttachedToCardId(moyraBaseHero.getUuid());

        // Completa a missão de jogar 2 feitiços -> move Sagração da Santa das Lâminas para a mão
        gameEngine.trackMissionProgress(player1.getUuid(), "PLAY_SPELL", 2);
        assertEquals(ZoneId.HAND, moyraLiberationCard.getZoneId());

        // Seleciona uma carta que já está na mão de P1 para ser jogada pelo efeito secundário da Liberação
        Card extraCardInHand = player1.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.HAND && !c.getUuid().equals(moyraLiberationCard.getUuid()))
                .findFirst()
                .orElseThrow();

        gameEngine.getGameState().setActiveTurnPlayerId(player1.getUuid());
        gameEngine.submitHeroChoice(player1.getUuid(), moyraBaseHero.getUuid(), enemyHero.getUuid());

        // P1 joga a Carta de Liberação; P2 passa
        gameEngine.submitPreparationCards(player1.getUuid(), List.of(moyraLiberationCard.getUuid()));
        gameEngine.submitPreparationCards(player2.getUuid(), List.of());

        // 1º Efeito resolveu: Moyra evoluiu para "Moyra, Santa das Laminas" preservando a arma equipada!
        assertEquals("Moyra, Santa das Laminas", moyraBaseHero.getCardName());
        assertEquals(22, moyraBaseHero.getVidaMaxima());
        assertEquals(3, moyraBaseHero.getMaxWeaponSlots());
        assertEquals(ZoneId.ATTACHED, preEquippedWeapon.getZoneId());
        assertEquals(moyraBaseHero.getUuid(), preEquippedWeapon.getAttachedToCardId());

        // 2º Efeito (PlayCardFromHandEffect) abriu escolha pendente para jogar 1 carta da mão:
        if (gameEngine.isWaitingForPlayerChoice()) {
            gameEngine.submitPlayerChoice(player1.getUuid(), extraCardInHand.getUuid());
        }

        // Após o término do combate, a arma extra escolhida da mão também foi resolvida e equipada em Moyra!
        assertEquals(ZoneId.ATTACHED, extraCardInHand.getZoneId());
        assertEquals(moyraBaseHero.getUuid(), extraCardInHand.getAttachedToCardId());
        // E a carta de liberação com "Exilar." foi para o EXILE
        assertEquals(ZoneId.EXILE, moyraLiberationCard.getZoneId());
    }

    @Test
    @DisplayName("3. Caso Ressurreição Goblin: Ao morrerem 2 aliados, descarta seus anexos, libera a Ação e revive o morto com status extras e herança de efeitos")
    void shouldLiberateOnTwoAlliesDeadAndReviveWithExtraStatsAndInheritedEffects() {
        // Equipa uma arma no Esquadrão Goblin antes dele morrer
        Card goblinWeapon = player1.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.DECK)
                .findFirst()
                .orElseThrow();
        goblinWeapon.setZoneId(ZoneId.ATTACHED);
        goblinWeapon.setAttachedToCardId(goblinSquadHero.getUuid());
        goblinSquadHero.setAtaque(goblinSquadHero.getAtaque() + 1); // +1 ATQ da Lamina de Cristal

        int initialEffectCount = goblinSquadHero.getEffects().size();

        // Simula que Esquadrão Goblin e Rei Goblin foram abatidos no combate (vidaAtual <= 0)
        goblinSquadHero.setVidaAtual(0);
        goblinKingHero.setVidaAtual(0);

        gameEngine.combatEnd(); // Avalia mortes, descarta anexos dos mortos e dispara ALLY_HEROES_DEAD (2/2)

        // 1. Verifica que os 2 heróis morreram e que a arma anexada ao morto foi descartada automaticamente!
        assertFalse(goblinSquadHero.getEstaVivo());
        assertFalse(goblinKingHero.getEstaVivo());
        assertEquals(ZoneId.DISCARD, goblinWeapon.getZoneId());
        assertNull(goblinWeapon.getAttachedToCardId());

        // 2. Verifica que a Carta de Liberação "Ascensão da Horda Lendária" entrou automaticamente na mão!
        assertEquals(2, goblinReviveLiberationCard.getMissionCurrentProgress());
        assertEquals(ZoneId.HAND, goblinReviveLiberationCard.getZoneId());

        // Avança a fase de descarte para iniciar o novo turno
        gameEngine.submitDiscard(player1.getUuid(), List.of());
        gameEngine.submitDiscard(player2.getUuid(), List.of());

        // No novo turno, Moyra entra em combate e joga "Ascensão da Horda Lendária"
        gameEngine.getGameState().setActiveTurnPlayerId(player1.getUuid());
        gameEngine.submitHeroChoice(player1.getUuid(), moyraBaseHero.getUuid(), enemyHero.getUuid());

        gameEngine.submitPreparationCards(player1.getUuid(), List.of(goblinReviveLiberationCard.getUuid()));
        gameEngine.submitPreparationCards(player2.getUuid(), List.of());

        // Como havia 2 aliados mortos (Esquadrão Goblin e Rei Goblin), o motor solicita escolher qual morto reviver:
        assertTrue(gameEngine.isWaitingForPlayerChoice());
        gameEngine.submitPlayerChoice(player1.getUuid(), goblinSquadHero.getUuid());

        // 3. Verifica que o Esquadrão Goblin foi revivido, mudou de nome, recebeu os status extras (+2 ESP, +3 ATQ, +2 DEF, +8 HP),
        // herdou seus efeitos originais e NÃO trouxe a arma descartada de volta!
        assertTrue(goblinSquadHero.getEstaVivo());
        assertEquals("Heróis Lendários dos Goblin", goblinSquadHero.getCardName());
        assertEquals(4, goblinSquadHero.getEspirito());   // 2 base + 2 bônus
        assertEquals(6, goblinSquadHero.getAtaque());     // 3 base (arma foi desanexada na morte) + 3 bônus
        assertEquals(4, goblinSquadHero.getDefesa());     // 2 base + 2 bônus
        assertEquals(22, goblinSquadHero.getVidaMaxima()); // 14 base + 8 bônus
        assertEquals(22, goblinSquadHero.getVidaAtual());
        assertTrue(goblinSquadHero.getEffects().size() >= initialEffectCount);
        assertEquals(ZoneId.DISCARD, goblinWeapon.getZoneId());
    }
}

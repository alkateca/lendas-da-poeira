package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.Colors;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ConstructoArquivistaIntegrationTest {

    private GameEngine engine;
    private GameState gameState;
    private Player p1;
    private Player p2;

    @BeforeEach
    void setUp() {
        engine = new GameEngine();
        gameState = new GameState();
        engine.setGameState(gameState);
        engine.setCardsOnBoard(new ArrayList<>());

        p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        p1.setName("Player 1");
        Deck deck1 = new Deck();
        deck1.setHeroList(new ArrayList<>());
        deck1.setCardList(new ArrayList<>());
        p1.setCurrentDeck(deck1);
        engine.setPlayer1(p1);
        gameState.setPlayer1(p1);

        p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        p2.setName("Player 2");
        Deck deck2 = new Deck();
        deck2.setHeroList(new ArrayList<>());
        deck2.setCardList(new ArrayList<>());
        p2.setCurrentDeck(deck2);
        engine.setPlayer2(p2);
        gameState.setPlayer2(p2);

        gameState.setActiveTurnPlayerId(p1.getUuid());
    }

    private HeroCard createHero(Player owner, String name, int esp, int atk, int def, int hp, ZoneId zone) {
        HeroCard hero = new HeroCard();
        hero.setUuid(UUID.randomUUID());
        hero.setCardName(name);
        hero.setCardType(CardType.HERO);
        hero.setColor(Colors.YELLOW);
        hero.setOwnerId(owner.getUuid());
        hero.setEspirito(esp);
        hero.setAtaque(atk);
        hero.setDefesa(def);
        hero.setVidaMaxima(hp);
        hero.setVidaAtual(hp);
        hero.setMaxWeaponSlots(2);
        hero.setEstaVivo(true);
        hero.setEstaAtivo(true);
        hero.setZoneId(zone);
        owner.getCurrentDeck().getHeroList().add(hero);
        return hero;
    }

    @Test
    @DisplayName("Constructo Arquivista: Início do Combate invoca Risada Sarcástica")
    void testConstructoArquivistaStartOfCombatInvokeRisadaSarcastica() {
        // Arrange
        HeroCard arquivista = createHero(p1, "Constructo Arquivista", 3, 4, 3, 15, ZoneId.BATTLE);
        gameState.setActiveHeroPlayer1Id(arquivista.getUuid());
        CardFactory.buildCardLogic(arquivista);

        HeroCard enemyHero = createHero(p2, "Inimigo", 1, 3, 1, 20, ZoneId.BATTLE);
        gameState.setActiveHeroPlayer2Id(enemyHero.getUuid());

        // Adiciona Risada Sarcástica no baralho do P1 para ser invocada
        Card risada = new Card();
        risada.setUuid(UUID.randomUUID());
        risada.setCardName("Risada Sarcástica");
        risada.setCardType(CardType.THEURGY);
        risada.setColor(Colors.NEUTRAL);
        risada.setZoneId(ZoneId.DECK);
        risada.setOwnerId(p1.getUuid());
        p1.getCurrentDeck().getCardList().add(risada);

        // Act
        engine.setTurnPhase(TurnPhase.COMBAT_START);
        arquivista.getEffects().forEach(effect -> {
            if (effect.predicate(arquivista, TurnPhase.COMBAT_START)) {
                effect.resolve(engine, arquivista);
            }
        });

        // Assert
        assertEquals(ZoneId.RESOLVE, risada.getZoneId(), "Risada Sarcástica deve estar na zona RESOLVE ao ser invocada.");
    }

    @Test
    @DisplayName("Constructo Arquivista: Aura concede +1 de Ataque para todos heróis aliados quando Tristeza é descartada")
    void testConstructoArquivistaAuraBuffAttackOnDiscardTristeza() {
        // Arrange
        HeroCard arquivista = createHero(p1, "Constructo Arquivista", 3, 4, 3, 15, ZoneId.BATTLE);
        gameState.setActiveHeroPlayer1Id(arquivista.getUuid());
        CardFactory.buildCardLogic(arquivista);

        HeroCard aliadoBanco = createHero(p1, "Aliado Banco", 2, 2, 2, 12, ZoneId.BENCH);

        HeroCard enemyHero = createHero(p2, "Inimigo", 1, 3, 1, 20, ZoneId.BATTLE);
        gameState.setActiveHeroPlayer2Id(enemyHero.getUuid());

        // Cria carta Tristeza na mão do oponente P2
        Card tristeza = new Card();
        tristeza.setUuid(UUID.randomUUID());
        tristeza.setCardName("Tristeza");
        tristeza.setCardType(CardType.ACTION);
        tristeza.setColor(Colors.NEUTRAL);
        tristeza.setItemType(ItemType.NENHUM);
        tristeza.setZoneId(ZoneId.HAND);
        tristeza.setOwnerId(p2.getUuid());
        p2.getCurrentDeck().getCardList().add(tristeza);

        engine.setTurnPhase(TurnPhase.DISCARD);

        // Act: P2 descarta a Tristeza
        engine.submitDiscard(p2.getUuid(), List.of(tristeza.getUuid()));

        // Assert
        assertEquals(ZoneId.DISCARD, tristeza.getZoneId(), "Tristeza deve ter ido para a zona DISCARD.");
        assertEquals(5, arquivista.getAtaque(), "Constructo Arquivista deve receber +1 de Ataque (4 -> 5).");
        assertEquals(3, aliadoBanco.getAtaque(), "Aliado no banco deve receber +1 de Ataque (2 -> 3).");
    }

    @Test
    @DisplayName("Constructo Arquivista: Aura não bufa heróis mortos e não duplica sem novos descartes")
    void testConstructoArquivistaAuraDoesNotBuffDeadOrDuplicate() {
        // Arrange
        HeroCard arquivista = createHero(p1, "Constructo Arquivista", 3, 4, 3, 15, ZoneId.BATTLE);
        gameState.setActiveHeroPlayer1Id(arquivista.getUuid());
        CardFactory.buildCardLogic(arquivista);

        HeroCard aliadoMorto = createHero(p1, "Aliado Caído", 2, 2, 2, 0, ZoneId.BENCH);
        aliadoMorto.setEstaVivo(false);

        HeroCard enemyHero = createHero(p2, "Inimigo", 1, 3, 1, 20, ZoneId.BATTLE);
        gameState.setActiveHeroPlayer2Id(enemyHero.getUuid());

        Card tristeza = new Card();
        tristeza.setUuid(UUID.randomUUID());
        tristeza.setCardName("Tristeza");
        tristeza.setCardType(CardType.ACTION);
        tristeza.setColor(Colors.NEUTRAL);
        tristeza.setItemType(ItemType.NENHUM);
        tristeza.setZoneId(ZoneId.HAND);
        tristeza.setOwnerId(p2.getUuid());
        p2.getCurrentDeck().getCardList().add(tristeza);

        engine.setTurnPhase(TurnPhase.DISCARD);

        // Act
        engine.submitDiscard(p2.getUuid(), List.of(tristeza.getUuid()));

        // Act 2: Segunda chamada sem novas Tristezas não deve buffar novamente
        engine.submitDiscard(p1.getUuid(), new ArrayList<>());

        // Assert
        assertEquals(5, arquivista.getAtaque(), "Ataque deve ter aumentado exatamente +1.");
        assertEquals(2, aliadoMorto.getAtaque(), "Herói morto não deve receber buff.");
    }
}

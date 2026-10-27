package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.Colors;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeguindoOrdensIntegrationTest {

    @Test
    void testSeguindoOrdensDamageOutOfCombatHeroAndToggleActiveState() {
        // Arrange
        GameEngine engine = new GameEngine();
        GameState gameState = new GameState();
        engine.setGameState(gameState);
        engine.setCardsOnBoard(new ArrayList<>());

        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        p1.setName("Jogador 1");
        Deck deck1 = new Deck();
        deck1.setHeroList(new ArrayList<>());
        deck1.setCardList(new ArrayList<>());
        p1.setCurrentDeck(deck1);
        engine.setPlayer1(p1);
        gameState.setPlayer1(p1);

        Player p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        p2.setName("Jogador 2");
        Deck deck2 = new Deck();
        deck2.setHeroList(new ArrayList<>());
        deck2.setCardList(new ArrayList<>());
        p2.setCurrentDeck(deck2);
        engine.setPlayer2(p2);
        gameState.setPlayer2(p2);

        // Herói P1 em batalha
        HeroCard p1Hero = new HeroCard();
        p1Hero.setUuid(UUID.randomUUID());
        p1Hero.setCardName("Herói Atacante");
        p1Hero.setOwnerId(p1.getUuid());
        p1Hero.setVidaMaxima(15);
        p1Hero.setVidaAtual(15);
        p1Hero.setEstaVivo(true);
        p1Hero.setEstaAtivo(true);
        p1Hero.setZoneId(ZoneId.BATTLE);
        deck1.getHeroList().add(p1Hero);
        gameState.setActiveHeroPlayer1Id(p1Hero.getUuid());

        // Equipe P2: 3 Heróis (1 em batalha, 2 na bancada) conforme arquitetura oficial
        HeroCard p2ActiveHero = new HeroCard();
        p2ActiveHero.setUuid(UUID.randomUUID());
        p2ActiveHero.setCardName("Herói Defensor Em Batalha");
        p2ActiveHero.setOwnerId(p2.getUuid());
        p2ActiveHero.setVidaMaxima(20);
        p2ActiveHero.setVidaAtual(20);
        p2ActiveHero.setEstaVivo(true);
        p2ActiveHero.setEstaAtivo(true);
        p2ActiveHero.setZoneId(ZoneId.BATTLE);
        deck2.getHeroList().add(p2ActiveHero);
        gameState.setActiveHeroPlayer2Id(p2ActiveHero.getUuid());

        HeroCard p2BenchHero1 = new HeroCard();
        p2BenchHero1.setUuid(UUID.randomUUID());
        p2BenchHero1.setCardName("Herói Fora de Combate 1");
        p2BenchHero1.setOwnerId(p2.getUuid());
        p2BenchHero1.setVidaMaxima(15);
        p2BenchHero1.setVidaAtual(15);
        p2BenchHero1.setEstaVivo(true);
        p2BenchHero1.setEstaAtivo(true);
        p2BenchHero1.setZoneId(ZoneId.BENCH);
        deck2.getHeroList().add(p2BenchHero1);

        HeroCard p2BenchHero2 = new HeroCard();
        p2BenchHero2.setUuid(UUID.randomUUID());
        p2BenchHero2.setCardName("Herói Fora de Combate 2");
        p2BenchHero2.setOwnerId(p2.getUuid());
        p2BenchHero2.setVidaMaxima(15);
        p2BenchHero2.setVidaAtual(15);
        p2BenchHero2.setEstaVivo(true);
        p2BenchHero2.setEstaAtivo(true);
        p2BenchHero2.setZoneId(ZoneId.BENCH);
        deck2.getHeroList().add(p2BenchHero2);

        // Instancia a carta Seguindo Ordens
        Card card = new Card();
        card.setUuid(UUID.randomUUID());
        card.setCardName("Seguindo Ordens");
        card.setCardType(CardType.ACTION);
        card.setColor(Colors.BLACK);
        card.setOwnerId(p1.getUuid());
        card.setZoneId(ZoneId.RESOLVE);
        CardFactory.buildCardLogic(card);

        engine.setTurnPhase(TurnPhase.RESOLUTION);

        // Act: Enfileira ambos os efeitos da carta na fila de resolução do motor
        card.getEffects().forEach(eff -> engine.getResolutionQueue().enqueue(eff, card));
        engine.getResolutionQueue().resolveAll(engine);

        // Assert 1: Efeito 1 solicita escolha de Herói Inimigo fora de combate
        assertNotNull(gameState.getPendingChoice(), "Deve solicitar a escolha de um Herói Inimigo fora de combate.");
        assertTrue(gameState.getPendingChoice().getValidTargetUuids().contains(p2BenchHero1.getUuid()),
                "O Herói fora de combate 1 deve ser um alvo válido.");
        assertTrue(gameState.getPendingChoice().getValidTargetUuids().contains(p2BenchHero2.getUuid()),
                "O Herói fora de combate 2 deve ser um alvo válido.");
        assertFalse(gameState.getPendingChoice().getValidTargetUuids().contains(p2ActiveHero.getUuid()),
                "O Herói em combate NÃO deve ser um alvo válido para este efeito.");

        // Submete a primeira escolha: Causa 1 de Dano Direto no Herói fora de combate 1
        engine.submitPlayerChoice(p1.getUuid(), p2BenchHero1.getUuid());
        assertEquals(14, p2BenchHero1.getVidaAtual(), "O Herói fora de combate deve sofrer 1 de Dano Direto (15 -> 14).");

        // Assert 2: Efeito 2 foi ativado em cadeia na fila e solicita escolha de Herói Inimigo para alterar estado ativo
        assertNotNull(gameState.getPendingChoice(), "Deve solicitar a escolha de um Herói Inimigo para alterar o estado ativo.");
        assertTrue(gameState.getPendingChoice().getValidTargetUuids().contains(p2BenchHero1.getUuid()),
                "Herói fora de combate 1 é um Herói Inimigo válido.");
        assertTrue(gameState.getPendingChoice().getValidTargetUuids().contains(p2BenchHero2.getUuid()),
                "Herói fora de combate 2 é um Herói Inimigo válido.");
        assertTrue(gameState.getPendingChoice().getValidTargetUuids().contains(p2ActiveHero.getUuid()),
                "Herói em combate é um Herói Inimigo válido.");

        // Submete a segunda escolha: Alterna o estado ativo de p2BenchHero1 (true -> false)
        engine.submitPlayerChoice(p1.getUuid(), p2BenchHero1.getUuid());
        assertFalse(p2BenchHero1.getEstaAtivo(), "O Estado Ativo do Herói Inimigo fora de combate deve ser alternado de true para false.");
        assertTrue(p2BenchHero2.getEstaAtivo(), "O segundo Herói da bancada deve permanecer ativo.");
    }
}

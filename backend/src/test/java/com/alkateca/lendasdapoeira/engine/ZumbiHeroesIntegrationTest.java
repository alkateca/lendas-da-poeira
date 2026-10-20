package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.Raca;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ZumbiHeroesIntegrationTest {

    @Test
    void testRainhaGoblinEffects() {
        // Arrange (Preparação)
        GameEngine engine = new GameEngine();
        GameState gameState = new GameState();
        engine.setGameState(gameState);

        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        Deck deck1 = new Deck();
        deck1.setHeroList(new ArrayList<>());
        p1.setCurrentDeck(deck1);
        engine.setPlayer1(p1);

        Player p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        engine.setPlayer2(p2);

        HeroCard p1Hero = new HeroCard();
        p1Hero.setUuid(UUID.randomUUID());
        p1Hero.setVidaMaxima(15);
        p1Hero.setVidaAtual(10);
        p1Hero.setEstaVivo(true);
        p1Hero.setCardName("Heroi Teste");
        deck1.getHeroList().add(p1Hero);

        gameState.setActiveHeroPlayer1Id(p1Hero.getUuid());

        // Fabrica a carta e aplica a lógica
        Card rainhaGoblin = new Card();
        rainhaGoblin.setCardName("Rainha Goblin");
        rainhaGoblin.setOwnerId(p1.getUuid());
        CardFactory.buildCardLogic(rainhaGoblin);

        // Act (Ação / Avanço Rápido)
        engine.setTurnPhase(TurnPhase.COMBAT_END);
        rainhaGoblin.getEffects().forEach(effect -> {
            if (effect.predicate(rainhaGoblin, TurnPhase.COMBAT_END)) {
                effect.resolve(engine, rainhaGoblin);
            }
        });

        // Assert (Validação)
        assertEquals(12, p1Hero.getVidaAtual(), "A vida do herói aliado deveria subir em 2.");
    }

    @Test
    void testQuimeraCarniceiraEffects() {
        // Arrange (Preparação)
        GameEngine engine = new GameEngine();
        GameState gameState = new GameState();
        engine.setGameState(gameState);

        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        Deck deck1 = new Deck();
        deck1.setHeroList(new ArrayList<>());
        p1.setCurrentDeck(deck1);
        engine.setPlayer1(p1);

        Player p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        engine.setPlayer2(p2);

        HeroCard p1Hero = new HeroCard();
        p1Hero.setUuid(UUID.randomUUID());
        p1Hero.setEspirito(0);
        p1Hero.setAtaque(5);
        p1Hero.setVidaMaxima(14);
        p1Hero.setVidaAtual(10);
        p1Hero.setEstaVivo(true);
        p1Hero.setRacas(Arrays.asList(Raca.ZUMBI));
        p1Hero.setCardName("Heroi Zumbi Ativo");
        deck1.getHeroList().add(p1Hero);
        gameState.setActiveHeroPlayer1Id(p1Hero.getUuid());

        HeroCard p1Ally = new HeroCard();
        p1Ally.setUuid(UUID.randomUUID());
        p1Ally.setEstaVivo(true);
        p1Ally.setRacas(Arrays.asList(Raca.ZUMBI));
        p1Ally.setCardName("Aliado Zumbi Banco");
        deck1.getHeroList().add(p1Ally);

        // Fabrica a carta e aplica a lógica
        Card quimera = new Card();
        quimera.setCardName("Quimera Carniceira");
        quimera.setOwnerId(p1.getUuid());
        CardFactory.buildCardLogic(quimera);

        // Act (Ação / Avanço Rápido)
        engine.setTurnPhase(TurnPhase.COMBAT_START);
        quimera.getEffects().forEach(effect -> {
            if (effect.predicate(quimera, TurnPhase.COMBAT_START)) {
                effect.resolve(engine, quimera);
            }
        });

        // Assert (Validação)
        assertEquals(2, p1Hero.getEspirito(), "O Espírito do herói deve ter aumentado em +2.");
        assertEquals(7, p1Hero.getAtaque(), "O Ataque do herói deve ter aumentado em +2.");
        assertEquals(14, p1Hero.getVidaAtual(), "A Vida do herói deveria subir de 10 para 14.");
    }

    @Test
    void testNecromanteDasAreiasEffects() {
        // Arrange (Preparação)
        GameEngine engine = new GameEngine();
        GameState gameState = new GameState();
        engine.setGameState(gameState);

        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        engine.setPlayer1(p1);

        // Fabrica a carta e aplica a lógica
        Card necromante = new Card();
        necromante.setCardName("Necromante das Areias");
        necromante.setOwnerId(p1.getUuid());
        CardFactory.buildCardLogic(necromante);

        // Act (Ação / Avanço Rápido) & Assert (Validação)
        engine.setTurnPhase(TurnPhase.COMBAT_START);
        assertDoesNotThrow(() -> {
            necromante.getEffects().forEach(effect -> {
                if (effect.predicate(necromante, TurnPhase.COMBAT_START)) {
                    effect.resolve(engine, necromante);
                }
            });
        }, "A invocação simulada não deve lançar exceções.");
    }
}

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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManoplasDoNecromanteIntegrationTest {

    @Test
    void testManoplasDoNecromanteBuffScaledByMissingHpAndCappedAtThree() {
        // Arrange
        GameEngine engine = new GameEngine();
        GameState gameState = new GameState();
        engine.setGameState(gameState);
        engine.setCardsOnBoard(new ArrayList<>());

        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        Deck deck1 = new Deck();
        deck1.setHeroList(new ArrayList<>());
        deck1.setCardList(new ArrayList<>());
        p1.setCurrentDeck(deck1);
        engine.setPlayer1(p1);
        gameState.setPlayer1(p1);

        Player p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        Deck deck2 = new Deck();
        deck2.setHeroList(new ArrayList<>());
        deck2.setCardList(new ArrayList<>());
        p2.setCurrentDeck(deck2);
        engine.setPlayer2(p2);
        gameState.setPlayer2(p2);

        HeroCard myHero = new HeroCard();
        myHero.setUuid(UUID.randomUUID());
        myHero.setCardName("Herói Portador");
        myHero.setOwnerId(p1.getUuid());
        myHero.setEspirito(1);
        myHero.setAtaque(3);
        myHero.setDefesa(2);
        myHero.setVidaMaxima(15);
        myHero.setVidaAtual(15);
        myHero.setEstaVivo(true);
        myHero.setZoneId(ZoneId.BATTLE);
        deck1.getHeroList().add(myHero);
        gameState.setActiveHeroPlayer1Id(myHero.getUuid());

        // Inimigo com 2 de vida perdida (20 - 18 = 2 de diferença)
        HeroCard enemyHero = new HeroCard();
        enemyHero.setUuid(UUID.randomUUID());
        enemyHero.setCardName("Herói Inimigo Ferido");
        enemyHero.setOwnerId(p2.getUuid());
        enemyHero.setVidaMaxima(20);
        enemyHero.setVidaAtual(18);
        enemyHero.setEstaVivo(true);
        enemyHero.setZoneId(ZoneId.BATTLE);
        deck2.getHeroList().add(enemyHero);
        gameState.setActiveHeroPlayer2Id(enemyHero.getUuid());

        Card manoplas = new Card();
        manoplas.setUuid(UUID.randomUUID());
        manoplas.setCardName("Manoplas do Necromante");
        manoplas.setCardType(CardType.ITEM);
        manoplas.setItemType(ItemType.ARMADURA);
        manoplas.setEmpunhadura(0);
        manoplas.setColor(Colors.BLACK);
        manoplas.setOwnerId(p1.getUuid());
        manoplas.setZoneId(ZoneId.ATTACHED);
        manoplas.setAttachedToCardId(myHero.getUuid());
        CardFactory.buildCardLogic(manoplas);
        deck1.getCardList().add(manoplas);

        // Act 1: COMBAT_START com 2 de vida perdida -> recebe +2 ESP, +2 ATQ, +2 DEF
        engine.setTurnPhase(TurnPhase.COMBAT_START);
        manoplas.getEffects().forEach(eff -> {
            if (eff.predicate(manoplas, TurnPhase.COMBAT_START)) {
                eff.resolve(engine, manoplas);
            }
        });

        // Assert 1: Bônus de +2 aplicado
        assertEquals(3, myHero.getEspirito(), "Espírito deve aumentar em +2 (1 -> 3).");
        assertEquals(5, myHero.getAtaque(), "Ataque deve aumentar em +2 (3 -> 5).");
        assertEquals(4, myHero.getDefesa(), "Defesa deve aumentar em +2 (2 -> 4).");

        // Act 2: Fim do combate -> Cleanup expira o buff
        engine.runCombatEndCleanup();

        // Assert 2: Status retornam aos valores base
        assertEquals(1, myHero.getEspirito(), "Espírito deve retornar para 1 após o combate.");
        assertEquals(3, myHero.getAtaque(), "Ataque deve retornar para 3 após o combate.");
        assertEquals(2, myHero.getDefesa(), "Defesa deve retornar para 2 após o combate.");
    }

    @Test
    void testManoplasDoNecromanteCappedAtThreeWhenDamageExceedsThree() {
        // Arrange
        GameEngine engine = new GameEngine();
        GameState gameState = new GameState();
        engine.setGameState(gameState);
        engine.setCardsOnBoard(new ArrayList<>());

        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        Deck deck1 = new Deck();
        deck1.setHeroList(new ArrayList<>());
        deck1.setCardList(new ArrayList<>());
        p1.setCurrentDeck(deck1);
        engine.setPlayer1(p1);
        gameState.setPlayer1(p1);

        Player p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        Deck deck2 = new Deck();
        deck2.setHeroList(new ArrayList<>());
        deck2.setCardList(new ArrayList<>());
        p2.setCurrentDeck(deck2);
        engine.setPlayer2(p2);
        gameState.setPlayer2(p2);

        HeroCard myHero = new HeroCard();
        myHero.setUuid(UUID.randomUUID());
        myHero.setCardName("Herói Portador");
        myHero.setOwnerId(p1.getUuid());
        myHero.setEspirito(2);
        myHero.setAtaque(2);
        myHero.setDefesa(2);
        myHero.setVidaMaxima(15);
        myHero.setVidaAtual(15);
        myHero.setEstaVivo(true);
        myHero.setZoneId(ZoneId.BATTLE);
        deck1.getHeroList().add(myHero);
        gameState.setActiveHeroPlayer1Id(myHero.getUuid());

        // Inimigo com 8 de vida perdida (20 - 12 = 8 de diferença, maior que o teto de 3)
        HeroCard enemyHero = new HeroCard();
        enemyHero.setUuid(UUID.randomUUID());
        enemyHero.setCardName("Herói Inimigo Muito Ferido");
        enemyHero.setOwnerId(p2.getUuid());
        enemyHero.setVidaMaxima(20);
        enemyHero.setVidaAtual(12);
        enemyHero.setEstaVivo(true);
        enemyHero.setZoneId(ZoneId.BATTLE);
        deck2.getHeroList().add(enemyHero);
        gameState.setActiveHeroPlayer2Id(enemyHero.getUuid());

        Card manoplas = new Card();
        manoplas.setUuid(UUID.randomUUID());
        manoplas.setCardName("Manoplas do Necromante");
        manoplas.setCardType(CardType.ITEM);
        manoplas.setItemType(ItemType.ARMADURA);
        manoplas.setEmpunhadura(0);
        manoplas.setColor(Colors.BLACK);
        manoplas.setOwnerId(p1.getUuid());
        manoplas.setZoneId(ZoneId.ATTACHED);
        manoplas.setAttachedToCardId(myHero.getUuid());
        CardFactory.buildCardLogic(manoplas);
        deck1.getCardList().add(manoplas);

        // Act: COMBAT_START
        engine.setTurnPhase(TurnPhase.COMBAT_START);
        manoplas.getEffects().forEach(eff -> {
            if (eff.predicate(manoplas, TurnPhase.COMBAT_START)) {
                eff.resolve(engine, manoplas);
            }
        });

        // Assert: Bônus deve ser limitado em no máximo +3
        assertEquals(5, myHero.getEspirito(), "Espírito deve ser limitado ao teto de +3 (2 -> 5).");
        assertEquals(5, myHero.getAtaque(), "Ataque deve ser limitado ao teto de +3 (2 -> 5).");
        assertEquals(5, myHero.getDefesa(), "Defesa deve ser limitada ao teto de +3 (2 -> 5).");
    }
}

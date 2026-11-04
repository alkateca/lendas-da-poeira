package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NewCardsBatch3IntegrationTest {

    private GameEngine engine;
    private GameState gameState;
    private Player p1;
    private Player p2;
    private HeroCard heroP1;
    private HeroCard heroP2;

    @BeforeEach
    void setUp() {
        engine = new GameEngine();
        gameState = new GameState();
        engine.setGameState(gameState);
        engine.setCardsOnBoard(new ArrayList<>());
        engine.setSnapshotHistory(new ArrayList<>());

        p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        p1.setName("Jogador 1");
        Deck deck1 = new Deck();
        deck1.setHeroList(new ArrayList<>());
        deck1.setCardList(new ArrayList<>());
        p1.setCurrentDeck(deck1);
        engine.setPlayer1(p1);
        gameState.setPlayer1(p1);

        p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        p2.setName("Jogador 2");
        Deck deck2 = new Deck();
        deck2.setHeroList(new ArrayList<>());
        deck2.setCardList(new ArrayList<>());
        p2.setCurrentDeck(deck2);
        engine.setPlayer2(p2);
        gameState.setPlayer2(p2);

        heroP1 = createHero(p1, "Herói Algoz P1", 2, 5, 2, 20, 15);
        heroP1.setEstaAtivo(true);
        deck1.getHeroList().add(heroP1);
        gameState.setActiveHeroPlayer1Id(heroP1.getUuid());

        heroP2 = createHero(p2, "Herói Alvo P2", 2, 4, 2, 20, 20); // Vida cheia (20/20)
        heroP2.setEstaAtivo(true);
        deck2.getHeroList().add(heroP2);
        gameState.setActiveHeroPlayer2Id(heroP2.getUuid());
    }

    private HeroCard createHero(Player owner, String name, int esp, int atk, int def, int maxHp, int currentHp) {
        HeroCard hero = new HeroCard();
        hero.setUuid(UUID.randomUUID());
        hero.setCardName(name);
        hero.setCardType(CardType.HERO);
        hero.setColor(Colors.NEUTRAL);
        hero.setOwnerId(owner.getUuid());
        hero.setEspirito(esp);
        hero.setAtaque(atk);
        hero.setDefesa(def);
        hero.setVidaMaxima(maxHp);
        hero.setVidaAtual(currentHp);
        hero.setEstaVivo(true);
        hero.setMaxWeaponSlots(2);
        return hero;
    }

    private Card createCard(String name, CardType cardType, ItemType itemType, int empunhadura, Player owner) {
        Card card = new Card();
        card.setUuid(UUID.randomUUID());
        card.setCardName(name);
        card.setCardType(cardType);
        card.setItemType(itemType);
        card.setEmpunhadura(empunhadura);
        card.setOwnerId(owner.getUuid());
        card.setZoneId(ZoneId.HAND);
        CardFactory.buildCardLogic(card);
        owner.getCurrentDeck().getCardList().add(card);
        return card;
    }

    @Test
    void testMantoDeFerroDoAlgoz_DefenseBuffIfEnemyWounded() {
        Card manto = createCard("Manto de Ferro do Algoz", CardType.ITEM, ItemType.ARMADURA, 0, p1);

        // Equipar concede +1 DEF (2 -> 3)
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        manto.setZoneId(ZoneId.RESOLVE);
        manto.getEffects().forEach(eff -> {
            if (eff.predicate(manto, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, manto);
            }
        });
        assertEquals(3, heroP1.getDefesa(), "Manto deve conceder +1 DEF passiva.");

        // Início do combate com inimigo saudável (20/20): não deve conceder bônus
        engine.setTurnPhase(TurnPhase.COMBAT_START);
        manto.getEffects().forEach(eff -> {
            if (eff.predicate(manto, TurnPhase.COMBAT_START)) {
                eff.resolve(engine, manto);
            }
        });
        assertEquals(3, heroP1.getDefesa(), "Não deve receber bônus se inimigo não estiver ferido.");

        // Agora fere o inimigo (20 -> 15)
        heroP2.setVidaAtual(15);
        manto.getEffects().forEach(eff -> {
            if (eff.predicate(manto, TurnPhase.COMBAT_START)) {
                eff.resolve(engine, manto);
            }
        });
        assertEquals(5, heroP1.getDefesa(), "Deve receber +2 de Defesa pois o inimigo está ferido (3 -> 5).");

        // Fim de combate: cleanup remove o bônus
        engine.setTurnPhase(TurnPhase.COMBAT_END);
        engine.runCombatEndCleanup();
        assertEquals(3, heroP1.getDefesa(), "Cleanup deve reverter os 2 de defesa.");
    }

    @Test
    void testCorrentesDoCadafalso_AttackBuffAndDirectDamageIfWounded() {
        Card correntes = createCard("Correntes do Cadafalso", CardType.ITEM, ItemType.FERRAMENTA, 0, p1);
        correntes.setZoneId(ZoneId.ATTACHED);
        correntes.setAttachedToCardId(heroP1.getUuid());

        // Inimigo ferido (18/20)
        heroP2.setVidaAtual(18);

        // 1. Início do combate: concede +2 de Ataque se inimigo ferido (5 -> 7)
        engine.setTurnPhase(TurnPhase.COMBAT_START);
        correntes.getEffects().forEach(eff -> {
            if (eff.predicate(correntes, TurnPhase.COMBAT_START)) {
                eff.resolve(engine, correntes);
            }
        });
        assertEquals(7, heroP1.getAtaque(), "Correntes deve conceder +2 de Ataque contra inimigo ferido.");

        // 2. Fim do combate: causa 2 de dano direto (18 -> 16)
        engine.setTurnPhase(TurnPhase.COMBAT_END);
        correntes.getEffects().forEach(eff -> {
            if (eff.predicate(correntes, TurnPhase.COMBAT_END)) {
                eff.resolve(engine, correntes);
            }
        });
        assertEquals(16, heroP2.getVidaAtual(), "Inimigo ferido deve sofrer 2 de dano direto ao final do combate.");

        // 3. Cleanup reverte o ataque
        engine.runCombatEndCleanup();
        assertEquals(5, heroP1.getAtaque(), "Bônus temporário de ataque deve ser revertido no cleanup.");
    }

    @Test
    void testTomoDasSentencasAntigas_AddTristezaToEnemyDeck() {
        Card tomo = createCard("Tomo das Sentenças Antigas", CardType.ITEM, ItemType.JOIA, 0, p1);

        // 1. Equipar concede +1 ESP e +1 DEF
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        tomo.setZoneId(ZoneId.RESOLVE);
        tomo.getEffects().forEach(eff -> {
            if (eff.predicate(tomo, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, tomo);
            }
        });
        assertEquals(3, heroP1.getEspirito(), "Tomo deve conceder +1 ESP.");
        assertEquals(3, heroP1.getDefesa(), "Tomo deve conceder +1 DEF.");

        // 2. Início do combate: adiciona carta 'Tristeza' ao deck inimigo
        int deckSizeAntes = p2.getCurrentDeck().getCardList().size();
        engine.setTurnPhase(TurnPhase.COMBAT_START);
        tomo.getEffects().forEach(eff -> {
            if (eff.predicate(tomo, TurnPhase.COMBAT_START)) {
                eff.resolve(engine, tomo);
            }
        });

        assertEquals(deckSizeAntes + 1, p2.getCurrentDeck().getCardList().size(), "Deck inimigo deve ter +1 carta.");
        Card addedCard = p2.getCurrentDeck().getCardList().get(p2.getCurrentDeck().getCardList().size() - 1);
        assertEquals("Tristeza", addedCard.getCardName(), "A carta adicionada deve ser 'Tristeza'.");
        assertEquals(ZoneId.DECK, addedCard.getZoneId());
        assertEquals(p2.getUuid(), addedCard.getOwnerId());
    }
}

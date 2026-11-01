package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NewCardsBatch2IntegrationTest {

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

        heroP1 = createHero(p1, "Herói Necromante P1", 2, 5, 2, 20, 15);
        heroP1.setEstaAtivo(true);
        deck1.getHeroList().add(heroP1);
        gameState.setActiveHeroPlayer1Id(heroP1.getUuid());

        heroP2 = createHero(p2, "Herói Oponente P2", 2, 4, 2, 20, 15);
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
    void testBaluarteDeEspelhosDoDragao_InvokeSpellAndReturnToDeck() {
        Card baluarte = createCard("Baluarte de Espelhos do Dragão", CardType.ITEM, ItemType.ESCUDO, 1, p1);

        // Feitiço com afinidade Cristal no descarte
        Card feiticoCristal = createCard("Estilhaço de Cristal", CardType.SPELL, ItemType.NENHUM, 0, p1);
        feiticoCristal.setZoneId(ZoneId.DISCARD);
        feiticoCristal.setAfinidades(new ArrayList<>(List.of(Afinidade.CRISTAL)));

        // 1. Equipar em RESOLUTION: concede +1 DEF (2 -> 3)
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        baluarte.setZoneId(ZoneId.RESOLVE);
        baluarte.getEffects().forEach(eff -> {
            if (eff.predicate(baluarte, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, baluarte);
            }
        });
        assertEquals(3, heroP1.getDefesa(), "Baluarte deve conceder +1 de Defesa.");

        // O efeito PlayInvokeSpellFromDiscardByAffinityEffect solicita a escolha do jogador
        assertTrue(engine.isWaitingForPlayerChoice(), "Deve haver escolha pendente para selecionar feitiço do descarte.");
        assertNotNull(gameState.getPendingChoice(), "Deve haver escolha pendente no gameState.");
        assertTrue(gameState.getPendingChoice().getValidTargetUuids().contains(feiticoCristal.getUuid()), "Feitiço de cristal deve ser alvo válido.");

        // Simula resposta do jogador escolhendo o feitiço
        engine.submitPlayerChoice(p1.getUuid(), feiticoCristal.getUuid());

        // 2. Fim de combate: retorna o feitiço do descarte para o deck
        feiticoCristal.setZoneId(ZoneId.DISCARD);
        engine.setTurnPhase(TurnPhase.COMBAT_END);
        baluarte.getEffects().forEach(eff -> {
            if (eff.predicate(baluarte, TurnPhase.COMBAT_END)) {
                eff.resolve(engine, baluarte);
            }
        });
        assertEquals(ZoneId.DECK, feiticoCristal.getZoneId(), "Feitiço com afinidade Cristal deve retornar ao baralho no fim do combate.");
    }

    @Test
    void testPlacasDeCristalDraconiano_BuffPerDeadAllyHero() {
        Card placas = createCard("Placas de Cristal Draconiano", CardType.ITEM, ItemType.ARMADURA, 0, p1);

        // Adiciona 2 heróis aliados mortos
        HeroCard morto1 = createHero(p1, "Aliado Morto 1", 1, 2, 1, 10, 0);
        morto1.setEstaVivo(false);
        p1.getCurrentDeck().getHeroList().add(morto1);

        HeroCard morto2 = createHero(p1, "Aliado Morto 2", 1, 2, 1, 10, 0);
        morto2.setEstaVivo(false);
        p1.getCurrentDeck().getHeroList().add(morto2);

        // 1. Equipar concede +2 DEF (2 -> 4)
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        placas.setZoneId(ZoneId.RESOLVE);
        placas.getEffects().forEach(eff -> {
            if (eff.predicate(placas, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, placas);
            }
        });
        assertEquals(4, heroP1.getDefesa(), "Placas deve conceder +2 de Defesa passiva.");

        // 2. Início do combate: +1 ESP e +2 ATQ por aliado morto (2 mortos = +2 ESP, +4 ATQ)
        engine.setTurnPhase(TurnPhase.COMBAT_START);
        placas.getEffects().forEach(eff -> {
            if (eff.predicate(placas, TurnPhase.COMBAT_START)) {
                eff.resolve(engine, placas);
            }
        });
        assertEquals(4, heroP1.getEspirito(), "Deve receber +2 de Espírito por 2 heróis mortos (2 -> 4).");
        assertEquals(9, heroP1.getAtaque(), "Deve receber +4 de Ataque por 2 heróis mortos (5 -> 9).");

        // 3. Fim do combate: cleanup remove o bônus temporário
        engine.setTurnPhase(TurnPhase.COMBAT_END);
        engine.runCombatEndCleanup();
        assertEquals(2, heroP1.getEspirito(), "Bônus temporário de Espírito deve ser revertido.");
        assertEquals(5, heroP1.getAtaque(), "Bônus temporário de Ataque deve ser revertido.");
    }

    @Test
    void testFilacteriaDasAreiasNegras_DrainPerZombieAlly() {
        Card filacteria = createCard("Filactéria das Areias Negras", CardType.ITEM, ItemType.JOIA, 0, p1);

        // Adiciona 2 heróis Zumbis aliados vivos
        HeroCard zumbi1 = createHero(p1, "Zumbi Aliado 1", 1, 2, 1, 10, 8);
        zumbi1.setRacas(new ArrayList<>(List.of(Raca.ZUMBI)));
        p1.getCurrentDeck().getHeroList().add(zumbi1);

        HeroCard zumbi2 = createHero(p1, "Zumbi Aliado 2", 1, 2, 1, 10, 7);
        zumbi2.setRacas(new ArrayList<>(List.of(Raca.ZUMBI)));
        p1.getCurrentDeck().getHeroList().add(zumbi2);

        // 1. Equipar concede +2 ESP e +2 DEF
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        filacteria.setZoneId(ZoneId.RESOLVE);
        filacteria.getEffects().forEach(eff -> {
            if (eff.predicate(filacteria, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, filacteria);
            }
        });
        assertEquals(4, heroP1.getEspirito(), "Filactéria deve conceder +2 ESP.");
        assertEquals(4, heroP1.getDefesa(), "Filactéria deve conceder +2 DEF.");

        // 2. Fim do combate: drena 1 de vida e causa 1 de dano direto por Zumbi (2 Zumbis = cura 2, dano direto 2)
        int hpAliadoAntes = heroP1.getVidaAtual();
        int hpInimigoAntes = heroP2.getVidaAtual();

        engine.setTurnPhase(TurnPhase.COMBAT_END);
        filacteria.getEffects().forEach(eff -> {
            if (eff.predicate(filacteria, TurnPhase.COMBAT_END)) {
                eff.resolve(engine, filacteria);
            }
        });

        assertEquals(hpAliadoAntes + 2, heroP1.getVidaAtual(), "Herói aliado deve recuperar 2 de Vida por 2 Zumbis vivos.");
        assertEquals(hpInimigoAntes - 2, heroP2.getVidaAtual(), "Herói inimigo deve sofrer 2 de dano direto por 2 Zumbis vivos.");
    }
}

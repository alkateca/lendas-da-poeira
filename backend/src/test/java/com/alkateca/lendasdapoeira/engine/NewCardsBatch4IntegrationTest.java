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

class NewCardsBatch4IntegrationTest {

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

        heroP1 = createHero(p1, "Paladino da Tundra P1", 2, 5, 2, 25, 15);
        heroP1.setEstaAtivo(true);
        deck1.getHeroList().add(heroP1);
        gameState.setActiveHeroPlayer1Id(heroP1.getUuid());

        heroP2 = createHero(p2, "Inimigo Gelado P2", 2, 4, 2, 20, 20);
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
    void testMantoCerimonialDoTrono_HealAndTutorTheurgy() {
        Card manto = createCard("Manto Cerimonial do Trono", CardType.ITEM, ItemType.ARMADURA, 0, p1);

        // Teurgia no descarte para ser resgatada
        Card teurgia = createCard("Teurgia Sagrada", CardType.THEURGY, ItemType.NENHUM, 0, p1);
        teurgia.setZoneId(ZoneId.DISCARD);

        // 1. Equipar concede +1 DEF (2 -> 3)
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        manto.setZoneId(ZoneId.RESOLVE);
        manto.getEffects().forEach(eff -> {
            if (eff.predicate(manto, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, manto);
            }
        });
        assertEquals(3, heroP1.getDefesa(), "Manto deve conceder +1 DEF passiva.");

        // 2. Início do combate: cura 3 de vida (15 -> 18)
        engine.setTurnPhase(TurnPhase.COMBAT_START);
        manto.getEffects().forEach(eff -> {
            if (eff.predicate(manto, TurnPhase.COMBAT_START)) {
                eff.resolve(engine, manto);
            }
        });
        assertEquals(18, heroP1.getVidaAtual(), "Manto deve curar 3 de Vida no início do combate.");

        // 3. Fim do combate: resgata a Teurgia do descarte para a mão
        engine.setTurnPhase(TurnPhase.COMBAT_END);
        manto.getEffects().forEach(eff -> {
            if (eff.predicate(manto, TurnPhase.COMBAT_END)) {
                eff.resolve(engine, manto);
            }
        });
        assertEquals(ZoneId.HAND, teurgia.getZoneId(), "A Teurgia deve ter retornado do descarte para a mão.");
    }

    @Test
    void testEscudoDaDevocaoCega_HealPerEquippedWeapon() {
        Card escudo = createCard("Escudo da Devoção Cega", CardType.ITEM, ItemType.ESCUDO, 1, p1);

        // Anexa 2 armas ao herói
        Card arma1 = createCard("Espada 1", CardType.ITEM, ItemType.ARMA, 1, p1);
        arma1.setZoneId(ZoneId.ATTACHED);
        arma1.setAttachedToCardId(heroP1.getUuid());

        Card arma2 = createCard("Espada 2", CardType.ITEM, ItemType.ARMA, 1, p1);
        arma2.setZoneId(ZoneId.ATTACHED);
        arma2.setAttachedToCardId(heroP1.getUuid());

        // 1. Equipar escudo concede +1 DEF (2 -> 3)
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        escudo.setZoneId(ZoneId.RESOLVE);
        escudo.getEffects().forEach(eff -> {
            if (eff.predicate(escudo, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, escudo);
            }
        });
        assertEquals(3, heroP1.getDefesa(), "Escudo deve conceder +1 DEF passiva.");

        // 2. Início do combate: cura 2 de vida por arma equipada (2 armas = cura 4; 15 -> 19)
        engine.setTurnPhase(TurnPhase.COMBAT_START);
        escudo.getEffects().forEach(eff -> {
            if (eff.predicate(escudo, TurnPhase.COMBAT_START)) {
                eff.resolve(engine, escudo);
            }
        });
        assertEquals(19, heroP1.getVidaAtual(), "Escudo deve curar 4 de Vida (2 armas equipadas * 2 cura).");
    }

    @Test
    void testBaluarteDaTundraAncestral_DamageReductionAndVulnerability() {
        Card baluarte = createCard("Baluarte da Tundra Ancestral", CardType.ITEM, ItemType.ESCUDO, 1, p1);

        // 1. Equipar concede +3 DEF e +1 de Redução de Dano
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        baluarte.setZoneId(ZoneId.RESOLVE);
        baluarte.getEffects().forEach(eff -> {
            if (eff.predicate(baluarte, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, baluarte);
            }
        });
        assertEquals(5, heroP1.getDefesa(), "Baluarte deve conceder +3 DEF (2 -> 5).");
        assertEquals(1, heroP1.getReducaoDano(), "Baluarte deve conceder +1 de Redução de Dano.");

        // 2. Início do combate: aplica 1 de vulnerabilidade no inimigo
        engine.setTurnPhase(TurnPhase.COMBAT_START);
        baluarte.getEffects().forEach(eff -> {
            if (eff.predicate(baluarte, TurnPhase.COMBAT_START)) {
                eff.resolve(engine, baluarte);
            }
        });
        assertEquals(1, heroP2.getVulnerabilidade(), "Inimigo deve receber +1 de Vulnerabilidade.");

        // 3. Desanexar remove a redução de dano
        baluarte.getEffects().forEach(eff -> eff.onDetach(engine, baluarte, heroP1));
        assertEquals(0, heroP1.getReducaoDano(), "Redução de dano deve ser revertida ao desanexar.");
    }

    @Test
    void testCouracaDaTundraProfunda_BuffPerEnemyVulnerability() {
        Card couraca = createCard("Couraça da Tundra Profunda", CardType.ITEM, ItemType.ARMADURA, 0, p1);

        // Define 4 de vulnerabilidade no inimigo
        heroP2.setVulnerabilidade(4);

        // 1. Equipar concede +2 DEF passiva (2 -> 4)
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        couraca.setZoneId(ZoneId.RESOLVE);
        couraca.getEffects().forEach(eff -> {
            if (eff.predicate(couraca, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, couraca);
            }
        });
        assertEquals(4, heroP1.getDefesa(), "Couraça deve conceder +2 DEF passiva.");

        // 2. Início do combate: a cada 2 de vulnerabilidade, ganha +1 DEF (4 vuln = +2 DEF; 4 -> 6)
        engine.setTurnPhase(TurnPhase.COMBAT_START);
        couraca.getEffects().forEach(eff -> {
            if (eff.predicate(couraca, TurnPhase.COMBAT_START)) {
                eff.resolve(engine, couraca);
            }
        });
        assertEquals(6, heroP1.getDefesa(), "Couraça deve conceder +2 DEF adicional por 4 pontos de vulnerabilidade.");

        // 3. Fim de combate: cura 2 e cleanup reverte os 2 de defesa do início do combate
        int hpAntes = heroP1.getVidaAtual();
        engine.setTurnPhase(TurnPhase.COMBAT_END);
        couraca.getEffects().forEach(eff -> {
            if (eff.predicate(couraca, TurnPhase.COMBAT_END)) {
                eff.resolve(engine, couraca);
            }
        });
        assertEquals(hpAntes + 2, heroP1.getVidaAtual(), "Couraça cura 2 no final do combate.");

        engine.runCombatEndCleanup();
        assertEquals(4, heroP1.getDefesa(), "Bônus por vulnerabilidade deve ser revertido no cleanup (6 -> 4).");
    }

    @Test
    void testGarrasCongeladas_BuffAttackIfEnemyHasVulnerability() {
        Card garras = createCard("Garras Congeladas", CardType.ITEM, ItemType.ARMA, 1, p1);

        // Inimigo sem vulnerabilidade
        heroP2.setVulnerabilidade(0);

        // 1. Equipar concede +2 ATQ e +1 DEF passivos (5 -> 7 ATQ, 2 -> 3 DEF)
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        garras.setZoneId(ZoneId.RESOLVE);
        garras.getEffects().forEach(eff -> {
            if (eff.predicate(garras, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, garras);
            }
        });
        assertEquals(7, heroP1.getAtaque(), "Garras deve conceder +2 ATQ passivo.");
        assertEquals(3, heroP1.getDefesa(), "Garras deve conceder +1 DEF passiva.");

        // Início do combate sem vulnerabilidade no inimigo: nenhum ataque extra
        engine.setTurnPhase(TurnPhase.COMBAT_START);
        garras.getEffects().forEach(eff -> {
            if (eff.predicate(garras, TurnPhase.COMBAT_START)) {
                eff.resolve(engine, garras);
            }
        });
        assertEquals(7, heroP1.getAtaque(), "Sem vulnerabilidade no inimigo, nenhum bônus adicional.");

        // Agora inimigo recebe 1 de vulnerabilidade
        heroP2.setVulnerabilidade(1);
        garras.getEffects().forEach(eff -> {
            if (eff.predicate(garras, TurnPhase.COMBAT_START)) {
                eff.resolve(engine, garras);
            }
        });
        assertEquals(9, heroP1.getAtaque(), "Inimigo vulnerável ativa +2 ATQ adicional (7 -> 9).");

        // Fim de combate: cleanup reverte o bônus adicional de combate
        engine.setTurnPhase(TurnPhase.COMBAT_END);
        engine.runCombatEndCleanup();
        assertEquals(7, heroP1.getAtaque(), "Ataque deve voltar a 7 após cleanup.");
    }
}

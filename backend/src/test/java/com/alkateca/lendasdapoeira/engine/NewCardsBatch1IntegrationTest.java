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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NewCardsBatch1IntegrationTest {

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

        heroP1 = createHero(p1, "Herói Ativo P1", 2, 5, 3, 20, 15);
        heroP1.setEstaAtivo(true);
        deck1.getHeroList().add(heroP1);
        gameState.setActiveHeroPlayer1Id(heroP1.getUuid());

        heroP2 = createHero(p2, "Herói Ativo P2", 2, 4, 2, 20, 15);
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
    void testBolsaDeArmadilhasDoMestreCacador_EquipAndTransfer() {
        Card bolsa = createCard("Bolsa de Armadilhas do Mestre Caçador", CardType.ITEM, ItemType.FERRAMENTA, 0, p1);

        // Outra ferramenta no baralho para ser jogada em resolução
        Card armadilhaDeck = createCard("Armadilha de Espinhos", CardType.ITEM, ItemType.FERRAMENTA, 0, p1);
        armadilhaDeck.setZoneId(ZoneId.DECK);

        // Segundo herói aliado ativo no banco
        HeroCard allyBench = createHero(p1, "Herói Reserva Ativo", 1, 3, 2, 15, 12);
        allyBench.setEstaAtivo(true);
        p1.getCurrentDeck().getHeroList().add(allyBench);

        // 1. Equipar em RESOLUTION: +2 ATQ, +2 DEF (5 -> 7 ATQ, 3 -> 5 DEF)
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        bolsa.setZoneId(ZoneId.RESOLVE);
        bolsa.getEffects().forEach(eff -> {
            if (eff.predicate(bolsa, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, bolsa);
            }
        });

        assertEquals(7, heroP1.getAtaque(), "Bolsa de Armadilhas deve conceder +2 de Ataque.");
        assertEquals(5, heroP1.getDefesa(), "Bolsa de Armadilhas deve conceder +2 de Defesa.");
        assertEquals(ZoneId.RESOLVE, armadilhaDeck.getZoneId(), "A próxima ferramenta do deck deve ser colocada em resolução.");

        // 2. Final do combate: transfere para allyBench
        engine.setTurnPhase(TurnPhase.COMBAT_END);
        bolsa.getEffects().forEach(eff -> {
            if (eff.predicate(bolsa, TurnPhase.COMBAT_END)) {
                eff.resolve(engine, bolsa);
            }
        });

        assertEquals(allyBench.getUuid(), bolsa.getAttachedToCardId(), "Deve ser transferida para o herói aliado ativo.");
        assertEquals(ZoneId.ATTACHED, bolsa.getZoneId());
    }

    @Test
    void testRedeDeContencaoDeFeras_DebuffEnemyAndBuffOnPlayFerramenta() {
        Card rede = createCard("Rede de Contenção de Feras", CardType.ITEM, ItemType.FERRAMENTA, 0, p1);
        rede.setAttachedToCardId(heroP1.getUuid());
        rede.setZoneId(ZoneId.ATTACHED);

        // 1. Início do combate: o inimigo perde 2 de ataque (4 -> 2)
        engine.setTurnPhase(TurnPhase.COMBAT_START);
        rede.getEffects().forEach(eff -> {
            if (eff.predicate(rede, TurnPhase.COMBAT_START)) {
                eff.resolve(engine, rede);
            }
        });
        assertEquals(2, heroP2.getAtaque(), "Rede deve reduzir ataque inimigo em 2 no início do combate.");

        // 2. Ao jogar outra ferramenta na Resolução: recebe +1 ATQ e +1 DEF temporários
        Card ferramentaJogada = createCard("Ferramenta Teste", CardType.ITEM, ItemType.FERRAMENTA, 0, p1);
        ferramentaJogada.setZoneId(ZoneId.RESOLVE);
        engine.getResolvedCardsInCurrentCombat().add(ferramentaJogada);

        engine.setTurnPhase(TurnPhase.RESOLUTION);
        rede.getEffects().forEach(eff -> {
            if (eff.predicate(rede, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, rede);
            }
        });
        assertEquals(6, heroP1.getAtaque(), "Herói deve receber +1 ATQ ao resolver Ferramenta.");
        assertEquals(4, heroP1.getDefesa(), "Herói deve receber +1 DEF ao resolver Ferramenta.");

        // 3. Cleanup no fim de combate reverte os bônus
        engine.setTurnPhase(TurnPhase.COMBAT_END);
        engine.runCombatEndCleanup();
        assertEquals(5, heroP1.getAtaque(), "Bônus temporário de ataque deve ser revertido no cleanup.");
        assertEquals(3, heroP1.getDefesa(), "Bônus temporário de defesa deve ser revertido no cleanup.");
    }

    @Test
    void testEscamasDoDragaoEterno_EquipBuffAndEndOfCombatHeal() {
        Card escamas = createCard("Escamas do Dragão Eterno", CardType.ITEM, ItemType.ARMADURA, 0, p1);

        // 1. Equipar em RESOLUTION: +2 ESP, +3 DEF (2 -> 4 ESP, 3 -> 6 DEF)
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        escamas.setZoneId(ZoneId.RESOLVE);
        escamas.getEffects().forEach(eff -> {
            if (eff.predicate(escamas, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, escamas);
            }
        });
        assertEquals(4, heroP1.getEspirito(), "Escamas deve conceder +2 de Espírito.");
        assertEquals(6, heroP1.getDefesa(), "Escamas deve conceder +3 de Defesa.");

        // 2. Fim do combate: cura 3 se inimigo sofreu dano mágico
        engine.recordHeroDealtPhysicalOrMagicDamage(heroP1.getUuid());

        int hpAntes = heroP1.getVidaAtual();
        engine.setTurnPhase(TurnPhase.COMBAT_END);
        escamas.getEffects().forEach(eff -> {
            if (eff.predicate(escamas, TurnPhase.COMBAT_END)) {
                eff.resolve(engine, escamas);
            }
        });
        assertEquals(hpAntes + 3, heroP1.getVidaAtual(), "Escamas deve curar 3 de Vida ao fim do combate se inimigo sofreu dano mágico.");
    }

    @Test
    void testBastaoDoMongeIncandescente_BonusDamage() {
        Card bastao = createCard("Bastão do Monge Incandescente", CardType.ITEM, ItemType.ARMA, 1, p1);

        // 1. Equipar concede +2 de Dano Bônus
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        bastao.setZoneId(ZoneId.RESOLVE);
        bastao.getEffects().forEach(eff -> {
            if (eff.predicate(bastao, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, bastao);
            }
        });
        assertEquals(2, heroP1.getDanoBonus(), "Bastão deve conceder +2 de Dano Bônus.");

        // 2. Desanexar remove o dano bônus
        bastao.getEffects().forEach(eff -> eff.onDetach(engine, bastao, heroP1));
        assertEquals(0, heroP1.getDanoBonus(), "Dano bônus deve ser removido ao desanexar.");
    }

    @Test
    void testIncensarioPurificador_EquipAndHealOnSpell() {
        Card incensario = createCard("Incensário Purificador", CardType.ITEM, ItemType.JOIA, 0, p1);

        // 1. Equipar concede +1 ESP, +1 DEF (2 -> 3 ESP, 3 -> 4 DEF)
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        incensario.setZoneId(ZoneId.RESOLVE);
        incensario.getEffects().forEach(eff -> {
            if (eff.predicate(incensario, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, incensario);
            }
        });
        assertEquals(3, heroP1.getEspirito(), "Incensário deve conceder +1 ESP.");
        assertEquals(4, heroP1.getDefesa(), "Incensário deve conceder +1 DEF.");

        // 2. Jogar um feitiço: cura 2 de vida
        Card feitiço = createCard("Feitiço Teste", CardType.SPELL, ItemType.NENHUM, 0, p1);
        feitiço.setZoneId(ZoneId.RESOLVE);
        engine.getResolvedCardsInCurrentCombat().add(feitiço);

        int hpAntes = heroP1.getVidaAtual();
        incensario.getEffects().forEach(eff -> {
            if (eff.predicate(incensario, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, incensario);
            }
        });
        assertEquals(hpAntes + 2, heroP1.getVidaAtual(), "Incensário deve curar 2 de Vida ao jogar Feitiço.");
    }
}

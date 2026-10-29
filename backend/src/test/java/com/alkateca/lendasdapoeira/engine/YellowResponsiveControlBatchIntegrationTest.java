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

class YellowResponsiveControlBatchIntegrationTest {

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
        p1.setName("Inquisidor P1");
        Deck deck1 = new Deck();
        deck1.setHeroList(new ArrayList<>());
        deck1.setCardList(new ArrayList<>());
        p1.setCurrentDeck(deck1);
        engine.setPlayer1(p1);
        gameState.setPlayer1(p1);

        p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        p2.setName("Herege P2");
        Deck deck2 = new Deck();
        deck2.setHeroList(new ArrayList<>());
        deck2.setCardList(new ArrayList<>());
        p2.setCurrentDeck(deck2);
        engine.setPlayer2(p2);
        gameState.setPlayer2(p2);
    }

    private HeroCard createHero(Player owner, String name, int esp, int atk, int def, int maxHp, int currentHp, ZoneId zone) {
        HeroCard hero = new HeroCard();
        hero.setUuid(UUID.randomUUID());
        hero.setCardName(name);
        hero.setCardType(CardType.HERO);
        hero.setColor(Colors.YELLOW);
        hero.setOwnerId(owner.getUuid());
        hero.setEspirito(esp);
        hero.setAtaque(atk);
        hero.setDefesa(def);
        hero.setVidaMaxima(maxHp);
        hero.setVidaAtual(currentHp);
        hero.setMaxWeaponSlots(2);
        hero.setEstaVivo(true);
        hero.setEstaAtivo(true);
        hero.setZoneId(zone);
        CardFactory.buildCardLogic(hero);
        owner.getCurrentDeck().getHeroList().add(hero);
        return hero;
    }

    private Card createCard(Player owner, String name, CardType type, ItemType itemType, int empunhadura, ZoneId zone) {
        Card card = new Card();
        card.setUuid(UUID.randomUUID());
        card.setCardName(name);
        card.setCardType(type);
        card.setItemType(itemType);
        card.setEmpunhadura(empunhadura);
        card.setColor(Colors.YELLOW);
        card.setOwnerId(owner.getUuid());
        card.setZoneId(zone);
        CardFactory.buildCardLogic(card);
        owner.getCurrentDeck().getCardList().add(card);
        return card;
    }

    @Test
    void testGrandeInquisidorVaelAuraAndStartOfCombatVulnerability() {
        // Arrange
        HeroCard vael = createHero(p1, "Grande Inquisidor Vael, Voz do Trono", 3, 4, 3, 16, 16, ZoneId.BATTLE);
        HeroCard enemy = createHero(p2, "Inimigo Invocador", 2, 4, 2, 18, 18, ZoneId.BATTLE);
        gameState.setActiveHeroPlayer1Id(vael.getUuid());
        gameState.setActiveHeroPlayer2Id(enemy.getUuid());

        Card annex1 = createCard(p2, "Broche de Cristal", CardType.ITEM, ItemType.JOIA, 0, ZoneId.ATTACHED);
        annex1.setAttachedToCardId(enemy.getUuid());
        Card annex2 = createCard(p2, "Lamina de Cristal", CardType.ITEM, ItemType.ARMA, 1, ZoneId.ATTACHED);
        annex2.setAttachedToCardId(enemy.getUuid());

        // Act 1: Início do Combate -> Inimigo tem 2 cartas anexadas -> recebe +1 Vulnerabilidade até o Final do Combate
        engine.setTurnPhase(TurnPhase.COMBAT_START);
        vael.onPhaseChange(TurnPhase.COMBAT_START, engine.getResolutionQueue());
        engine.getResolutionQueue().resolveAll(engine);

        // Assert 1
        assertEquals(1, enemy.getVulnerabilidade(), "Inimigo com 2 cartas anexadas deve receber +1 de Vulnerabilidade até o Final do Combate.");

        // Act 2: Oponente invoca uma carta -> Aura de Vael causa 1 de Dano Direto (+1 da Vulnerabilidade = 2)
        engine.invokeCard(p2, enemy, "Bola de fogo");

        // Assert 2
        assertEquals(16, enemy.getVidaAtual(), "Inimigo deve sofrer 1 de Dano Direto da Aura + 1 de Vulnerabilidade (18 -> 16).");

        // Act 3: Limpeza de Fim de Combate
        engine.runCombatEndCleanup();
        assertEquals(0, enemy.getVulnerabilidade(), "Vulnerabilidade temporária de Vael deve expirar no Final do Combate.");
    }

    @Test
    void testJusticarElyonEndOfCombatPerTwoOpponentPlayedCards() {
        // Arrange
        HeroCard elyon = createHero(p1, "Justiçar Elyon, Balança Imutável", 3, 5, 3, 17, 10, ZoneId.BATTLE);
        HeroCard enemy = createHero(p2, "Inimigo Gastador", 1, 4, 2, 20, 20, ZoneId.BATTLE);
        gameState.setActiveHeroPlayer1Id(elyon.getUuid());
        gameState.setActiveHeroPlayer2Id(enemy.getUuid());

        // Oponente jogou 4 cartas neste turno (2 pares)
        for (int i = 0; i < 4; i++) {
            Card c = createCard(p2, "Carta Jogada " + i, CardType.ACTION, ItemType.NENHUM, 0, ZoneId.RESOLVE);
            gameState.getPlayer2PlayedCards().add(c);
        }

        // Act: Final do Combate
        engine.setTurnPhase(TurnPhase.COMBAT_END);
        elyon.onPhaseChange(TurnPhase.COMBAT_END, engine.getResolutionQueue());
        engine.getResolutionQueue().resolveAll(engine);

        // Assert: 2 pares -> Cura 6 de Vida (10 -> 16) e causa 6 de Dano Mágico bruto (6 - 1 ESP = 5 -> 20 para 15)
        assertEquals(16, elyon.getVidaAtual(), "Elyon deve recuperar 3 de Vida para cada 2 cartas jogadas pelo oponente (total +6).");
        assertEquals(15, enemy.getVidaAtual(), "Inimigo deve sofrer 6 de Dano Mágico bruto mitigado por 1 de Espírito (20 -> 15).");
    }

    @Test
    void testEditoContraAGananciaWithOpponentExtraDraw() {
        // Arrange
        HeroCard ally = createHero(p1, "Guardião Aliado", 3, 4, 3, 16, 16, ZoneId.BATTLE);
        HeroCard enemy = createHero(p2, "Inimigo Ganancioso", 2, 6, 2, 20, 20, ZoneId.BATTLE);
        gameState.setActiveHeroPlayer1Id(ally.getUuid());
        gameState.setActiveHeroPlayer2Id(enemy.getUuid());

        createCard(p2, "Carta no Deck 1", CardType.ACTION, ItemType.NENHUM, 0, ZoneId.DECK);
        Card drawAction = createCard(p2, "Ração de emergencia", CardType.ACTION, ItemType.NENHUM, 0, ZoneId.RESOLVE);
        Card edito = createCard(p1, "Édito contra a Ganância", CardType.THEURGY, ItemType.NENHUM, 0, ZoneId.RESOLVE);

        // Act: Fase de Resolução (fora da Fase de Escolha) -> Oponente compra carta e depois sofre Édito contra a Ganância
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        drawAction.getEffects().get(0).resolve(engine, drawAction); // DrawCardsEffect(2)

        edito.getEffects().forEach(eff -> {
            if (eff.predicate(edito, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, edito);
            }
        });

        // Assert
        // 1º Efeito: X + 2 Dano Mágico (3 ESP + 2 = 5 bruto - 2 ESP inimigo = 3 de dano -> 20 para 17)
        // 2º Efeito: Oponente comprou fora da Fase de Escolha -> 3 Dano Direto (17 para 14) e -2 de Ataque (6 para 4)
        assertEquals(14, enemy.getVidaAtual(), "Inimigo deve sofrer 3 de Dano Mágico + 3 de Dano Direto (20 -> 14).");
        assertEquals(4, enemy.getAtaque(), "Inimigo deve receber -2 de Ataque até o Final do Combate (6 -> 4).");

        engine.runCombatEndCleanup();
        assertEquals(6, enemy.getAtaque(), "Ataque do inimigo deve ser restaurado no Final do Combate.");
    }

    @Test
    void testRetribuicaoDogmaticaDefenseBuffAndScalingMagicDamage() {
        // Arrange
        HeroCard ally = createHero(p1, "Guardião Defensor", 2, 4, 3, 16, 16, ZoneId.BATTLE);
        HeroCard enemy = createHero(p2, "Inimigo Ativo", 2, 5, 2, 20, 20, ZoneId.BATTLE);
        gameState.setActiveHeroPlayer1Id(ally.getUuid());
        gameState.setActiveHeroPlayer2Id(enemy.getUuid());

        // Oponente jogou 2 cartas e invocou 1 carta neste combate (total = 3 cartas)
        Card played1 = createCard(p2, "Jogada 1", CardType.ACTION, ItemType.NENHUM, 0, ZoneId.RESOLVE);
        Card played2 = createCard(p2, "Jogada 2", CardType.SPELL, ItemType.NENHUM, 0, ZoneId.RESOLVE);
        gameState.getPlayer2PlayedCards().add(played1);
        gameState.getPlayer2PlayedCards().add(played2);

        engine.setTurnPhase(TurnPhase.COMBAT_START);
        engine.invokeCard(p2, enemy, "Bola de fogo");

        Card retribuicao = createCard(p1, "Retribuição Dogmática", CardType.THEURGY, ItemType.NENHUM, 0, ZoneId.RESOLVE);

        // Act
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        retribuicao.getEffects().forEach(eff -> {
            if (eff.predicate(retribuicao, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, retribuicao);
            }
        });

        // Assert: +2 Defesa (3 -> 5) e 3 cartas * 2 = 6 Dano Mágico bruto - 2 ESP inimigo = 4 Dano Mágico (20 -> 16)
        assertEquals(5, ally.getDefesa(), "Seu Herói deve receber +2 de Defesa até o Final do Combate (3 -> 5).");
        assertEquals(16, enemy.getVidaAtual(), "Inimigo deve sofrer 6 de Dano Mágico bruto mitigado por 2 de Espírito (20 -> 16).");

        engine.runCombatEndCleanup();
        assertEquals(3, ally.getDefesa(), "Defesa deve retornar ao valor base após o Final do Combate.");
    }

    @Test
    void testInterditoSagradoZeroBonusDamageAndWeaponAttackDebuff() {
        // Arrange
        HeroCard ally = createHero(p1, "Guardião Aliado", 3, 4, 3, 16, 16, ZoneId.BATTLE);
        HeroCard enemy = createHero(p2, "Inimigo Armado", 1, 7, 2, 18, 18, ZoneId.BATTLE);
        enemy.setDanoBonus(4);
        gameState.setActiveHeroPlayer1Id(ally.getUuid());
        gameState.setActiveHeroPlayer2Id(enemy.getUuid());

        Card enemyWeapon = createCard(p2, "Lamina de Cristal", CardType.ITEM, ItemType.ARMA, 1, ZoneId.ATTACHED);
        enemyWeapon.setAttachedToCardId(enemy.getUuid());

        Card interdito = createCard(p1, "Interdito Sagrado", CardType.THEURGY, ItemType.NENHUM, 0, ZoneId.RESOLVE);

        // Act
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        interdito.getEffects().forEach(eff -> {
            if (eff.predicate(interdito, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, interdito);
            }
        });

        // Assert
        assertEquals(0, enemy.getDanoBonus(), "O Dano Bônus do Herói Inimigo deve se tornar 0 até o Final do Combate.");
        assertEquals(5, enemy.getAtaque(), "Inimigo com Arma equipada deve receber -2 de Ataque até o Final do Combate (7 -> 5).");

        engine.runCombatEndCleanup();
        assertEquals(4, enemy.getDanoBonus(), "Dano Bônus deve ser restaurado no Final do Combate.");
        assertEquals(7, enemy.getAtaque(), "Ataque deve ser restaurado no Final do Combate.");
    }

    @Test
    void testSeloDaConfissaoEndOfCombatHealAndPermanentVulnerability() {
        // Arrange
        HeroCard ally = createHero(p1, "Guardião Confessor", 3, 4, 2, 17, 14, ZoneId.BATTLE);
        HeroCard enemy = createHero(p2, "Agressor", 1, 5, 2, 18, 18, ZoneId.BATTLE);
        gameState.setActiveHeroPlayer1Id(ally.getUuid());
        gameState.setActiveHeroPlayer2Id(enemy.getUuid());

        Card selo = createCard(p1, "Selo da Confissão", CardType.ITEM, ItemType.JOIA, 0, ZoneId.RESOLVE);

        // Act 1: Equipa o Selo na Resolução
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        selo.getEffects().forEach(eff -> {
            if (eff.predicate(selo, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, selo);
            }
        });
        assertEquals(ZoneId.ATTACHED, selo.getZoneId(), "Selo da Confissão deve ficar anexado ao Herói.");

        // Act 2: Combate Físico -> Inimigo (ATQ 5) causa 3 de Dano Físico em Aliado (DEF 2), vida de 14 vai para 11
        engine.combat();
        assertEquals(11, ally.getVidaAtual());
        assertTrue(engine.hasHeroDealtPhysicalOrMagicDamageInCurrentCombat(enemy.getUuid()));

        // Act 3: Final do Combate -> Selo cura 3 de Vida (11 -> 14) e aplica +1 Vulnerabilidade permanente no inimigo
        selo.getEffects().forEach(eff -> {
            if (eff.predicate(selo, TurnPhase.COMBAT_END)) {
                eff.resolve(engine, selo);
            }
        });

        // Assert
        assertEquals(14, ally.getVidaAtual(), "Seu Herói deve recuperar 3 de Vida no Final do Combate (11 -> 14).");
        assertEquals(1, enemy.getVulnerabilidade(), "Inimigo que causou dano deve receber +1 de Vulnerabilidade permanente.");

        engine.runCombatEndCleanup();
        assertEquals(1, enemy.getVulnerabilidade(), "A Vulnerabilidade do Selo da Confissão dura até o Final da Partida.");
    }

    @Test
    void testCetroDoJulgamentoPerpetuoEquipAndConditionalStartOfCombatBuff() {
        // Arrange
        HeroCard ally = createHero(p1, "Inquisidor", 3, 4, 3, 16, 16, ZoneId.BATTLE);
        HeroCard enemy = createHero(p2, "Colosso Inimigo", 1, 5, 2, 20, 20, ZoneId.BATTLE); // MaxHP 20 > 16
        gameState.setActiveHeroPlayer1Id(ally.getUuid());
        gameState.setActiveHeroPlayer2Id(enemy.getUuid());

        Card cetro = createCard(p1, "Cetro do Julgamento Perpétuo", CardType.ITEM, ItemType.ARMA, 2, ZoneId.RESOLVE);

        // Act 1: Equipar o Cetro (+2 ATQ, +1 ESP)
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        cetro.getEffects().forEach(eff -> {
            if (eff.predicate(cetro, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, cetro);
            }
        });

        assertEquals(6, ally.getAtaque(), "Cetro deve conceder +2 de Ataque ao equipar (4 -> 6).");
        assertEquals(4, ally.getEspirito(), "Cetro deve conceder +1 de Espírito ao equipar (3 -> 4).");

        // Act 2: Início do Combate -> Inimigo tem mais Vida Máxima (20 > 16) -> Seu Herói recebe +2 ATQ e +2 DEF até o Final do Combate
        engine.setTurnPhase(TurnPhase.COMBAT_START);
        cetro.getEffects().forEach(eff -> {
            if (eff.predicate(cetro, TurnPhase.COMBAT_START)) {
                eff.resolve(engine, cetro);
            }
        });

        // Assert
        assertEquals(8, ally.getAtaque(), "Seu Herói deve receber +2 de Ataque adicional no Início do Combate (6 -> 8).");
        assertEquals(5, ally.getDefesa(), "Seu Herói deve receber +2 de Defesa no Início do Combate (3 -> 5).");

        // Act 3: Limpeza de Fim de Combate -> Bônus temporário expira, mantendo bônus permanente da arma
        engine.runCombatEndCleanup();
        assertEquals(6, ally.getAtaque(), "Ataque deve voltar para 6 (4 base + 2 da arma) após o Final do Combate.");
        assertEquals(3, ally.getDefesa(), "Defesa deve voltar para 3 após o Final do Combate.");
        assertEquals(4, ally.getEspirito(), "Espírito de +1 da arma permanece ativo.");
    }
}

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

class BlackExecutionerBatchIntegrationTest {

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
    }

    private HeroCard createHero(Player owner, String name, int esp, int atk, int def, int maxHp, int currentHp, ZoneId zone) {
        HeroCard hero = new HeroCard();
        hero.setUuid(UUID.randomUUID());
        hero.setCardName(name);
        hero.setCardType(CardType.HERO);
        hero.setColor(Colors.BLACK);
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
        card.setColor(Colors.BLACK);
        card.setOwnerId(owner.getUuid());
        card.setZoneId(zone);
        CardFactory.buildCardLogic(card);
        owner.getCurrentDeck().getCardList().add(card);
        return card;
    }

    @Test
    void testChefeCarrascoAndEspadaDoCarrascoWoundedExecution() {
        // Arrange
        HeroCard chefe = createHero(p1, "Chefe Carrasco", 0, 6, 2, 15, 9, ZoneId.BATTLE);
        HeroCard enemy = createHero(p2, "Defensor Ferido", 1, 3, 4, 15, 8, ZoneId.BATTLE); // Ferido (8 < 15)
        gameState.setActiveHeroPlayer1Id(chefe.getUuid());
        gameState.setActiveHeroPlayer2Id(enemy.getUuid());

        Card espada = createCard(p1, "Espada do Carrasco", CardType.ITEM, ItemType.ARMA, 2, ZoneId.DECK);

        // Act 1: Início do Combate (Invoca Espada do Carrasco e zera Defesa do inimigo Ferido)
        engine.setTurnPhase(TurnPhase.COMBAT_START);
        chefe.onPhaseChange(TurnPhase.COMBAT_START, engine.getResolutionQueue());
        engine.getResolutionQueue().resolveAll(engine);

        // Assert 1
        assertEquals(9, chefe.getAtaque(), "Chefe Carrasco (6) deve receber +3 de Ataque da Espada do Carrasco (total 9).");
        assertEquals(ZoneId.ATTACHED, espada.getZoneId(), "Espada do Carrasco deve estar equipada.");
        assertEquals(0, enemy.getDefesa(), "A Defesa do Herói Inimigo Ferido deve se tornar 0 até o Final do Combate.");

        // Act 2: Combate Físico (Chefe causa 9 de dano em inimigo com 8 de vida e 0 de defesa -> vida vai a -1)
        engine.combat();
        assertEquals(-1, enemy.getVidaAtual(), "Inimigo deve ficar com -1 de Vida após sofrer 9 de dano.");

        // Act 3: Final do Combate (Espada do Carrasco cura 5 de vida por inimigo estar <= 0; Defesa do inimigo é restaurada)
        // Chefe tinha 9 de vida, sofreu 3 - 2 = 1 no combate físico (foi para 8), e recupera +5 da Espada (vai para 13)
        espada.getEffects().forEach(eff -> {
            if (eff.predicate(espada, TurnPhase.COMBAT_END)) {
                engine.getResolutionQueue().enqueue(eff, espada);
            }
        });
        engine.getResolutionQueue().resolveAll(engine);
        engine.runCombatEndCleanup();

        // Assert 3
        assertEquals(13, chefe.getVidaAtual(), "Chefe Carrasco deve recuperar 5 pontos de Vida no Final do Combate (8 -> 13).");
        assertEquals(4, enemy.getDefesa(), "A Defesa original (4) do inimigo deve ser restaurada no Final do Combate.");
    }

    @Test
    void testIzerefAuraInstantDeathBelowZeroBlocksEndOfCombatHealing() {
        // Arrange
        HeroCard izeref = createHero(p1, "Izeref, Executor da Ordem Necrótica", 1, 7, 3, 14, 14, ZoneId.BATTLE);
        HeroCard enemy = createHero(p2, "Esquadrão Goblin", 2, 4, 2, 12, 5, ZoneId.BATTLE);
        gameState.setActiveHeroPlayer1Id(izeref.getUuid());
        gameState.setActiveHeroPlayer2Id(enemy.getUuid());

        // Equipa Estrela do amanhã no inimigo (que normalmente salvaria um herói com vida < 0 no COMBAT_END)
        Card estrela = createCard(p2, "Estrela do amanhã", CardType.ITEM, ItemType.JOIA, 0, ZoneId.ATTACHED);
        estrela.setAttachedToCardId(enemy.getUuid());

        // Act: Combate físico -> Izeref (7 ATQ) vs Inimigo (2 DEF, 5 HP) -> causa 5 de dano (vida vai a 0: ainda vivo!)
        engine.combat();
        assertEquals(0, enemy.getVidaAtual(), "Com exatamente 0 de vida, ainda não está abaixo de 0.");
        assertTrue(enemy.getEstaVivo(), "Com 0 de vida (não abaixo de 0), a Aura de Izeref ainda não matou instantaneamente.");

        // Agora causa 3 de Dano Direto com Cheiro de Sangue deixando a vida abaixo de 0 (-3)
        Card cheiro = createCard(p1, "Cheiro de Sangue", CardType.ACTION, ItemType.NENHUM, 0, ZoneId.RESOLVE);
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        cheiro.getEffects().forEach(eff -> engine.getResolutionQueue().enqueue(eff, cheiro));
        engine.getResolutionQueue().resolveAll(engine);

        // Assert: Vida ficou abaixo de 0 (-3), portanto Izeref o considera morto imediatamente!
        assertEquals(-3, enemy.getVidaAtual());
        assertFalse(enemy.getEstaVivo(), "Com vida abaixo de 0, a Aura de Izeref considera o Herói Inimigo morto imediatamente.");
        assertEquals(ZoneId.DISCARD, estrela.getZoneId(), "Itens anexados ao herói morto são descartados imediatamente.");

        // Mesmo que o efeito de fim de combate tente resolver, o herói já está morto e não recupera vida
        engine.setTurnPhase(TurnPhase.COMBAT_END);
        enemy.getEffects().forEach(eff -> {
            if (eff.predicate(enemy, TurnPhase.COMBAT_END)) {
                eff.resolve(engine, enemy);
            }
        });
        assertEquals(-3, enemy.getVidaAtual(), "Herói morto pela Aura de Izeref não pode curar no Final do Combate.");
    }

    @Test
    void testConstructoCarniceiroAuraAmplifiesDamageTakenByEnemy() {
        // Arrange
        HeroCard atacante = createHero(p1, "Chefe Carrasco", 0, 6, 2, 15, 15, ZoneId.BATTLE);
        createHero(p1, "Constructo Carniceiro", 2, 7, 2, 15, 15, ZoneId.BENCH); // Na bancada com Aura ativa
        HeroCard enemy = createHero(p2, "Alvo Inimigo", 1, 4, 2, 20, 20, ZoneId.BATTLE);
        gameState.setActiveHeroPlayer1Id(atacante.getUuid());
        gameState.setActiveHeroPlayer2Id(enemy.getUuid());

        // Act & Assert: Dano Físico base 3 contra Defesa 2 = 1 + 1 (Aura do Constructo Carniceiro) = 2
        int physDmg = engine.calculatePhysicalDamage(atacante, enemy, 3);
        assertEquals(2, physDmg, "Quando o Herói Inimigo sofre dano (> 0), deve sofrer +1 de dano pela Aura do Constructo Carniceiro.");

        // Se o dano for totalmente mitigado (2 contra Defesa 2 = 0), não sofre dano, logo não ativa o +1
        int zeroDmg = engine.calculatePhysicalDamage(atacante, enemy, 2);
        assertEquals(0, zeroDmg, "Se o dano calculado for 0 (não sofreu dano), a Aura não deve adicionar +1.");
    }

    @Test
    void testCheiroDeSanguePhysicalPlusConditionalWoundedDirectDamage() {
        // Arrange
        HeroCard myHero = createHero(p1, "Atacante", 1, 5, 2, 15, 15, ZoneId.BATTLE);
        HeroCard enemy = createHero(p2, "Defensor", 1, 4, 1, 20, 20, ZoneId.BATTLE);
        gameState.setActiveHeroPlayer1Id(myHero.getUuid());
        gameState.setActiveHeroPlayer2Id(enemy.getUuid());

        Card cheiro = createCard(p1, "Cheiro de Sangue", CardType.ACTION, ItemType.NENHUM, 0, ZoneId.RESOLVE);

        // Act: Joga Cheiro de Sangue
        // 1º efeito: 3 de Dano Físico contra 1 de Defesa -> causa 2 de dano (Vida: 20 -> 18, fica Ferido!)
        // 2º efeito: Como agora está Ferido (18 < 20), sofre 2 de Dano Direto (Vida: 18 -> 16)
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        cheiro.getEffects().forEach(eff -> engine.getResolutionQueue().enqueue(eff, cheiro));
        engine.getResolutionQueue().resolveAll(engine);

        // Assert
        assertEquals(16, enemy.getVidaAtual(), "Deve sofrer 2 de Dano Físico (3 - 1 DEF) e +2 de Dano Direto por estar Ferido (20 -> 16).");
    }

    @Test
    void testArrastarParaOCadafalsoTargetsOnlyWoundedBenchHeroAndAppliesVulnerability() {
        // Arrange
        HeroCard myHero = createHero(p1, "Atacante", 1, 5, 2, 15, 15, ZoneId.BATTLE);
        HeroCard enemyActive = createHero(p2, "Inimigo em Combate", 1, 4, 2, 20, 20, ZoneId.BATTLE);
        HeroCard enemyBenchHealthy = createHero(p2, "Inimigo Bancada Saudável", 1, 4, 2, 15, 15, ZoneId.BENCH);
        HeroCard enemyBenchWounded = createHero(p2, "Inimigo Bancada Ferido", 1, 4, 2, 15, 10, ZoneId.BENCH);
        gameState.setActiveHeroPlayer1Id(myHero.getUuid());
        gameState.setActiveHeroPlayer2Id(enemyActive.getUuid());

        Card arrastar = createCard(p1, "Arrastar para o Cadafalso", CardType.ACTION, ItemType.NENHUM, 0, ZoneId.RESOLVE);

        // Act
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        arrastar.getEffects().forEach(eff -> engine.getResolutionQueue().enqueue(eff, arrastar));
        engine.getResolutionQueue().resolveAll(engine);

        // Assert 1: Apenas o herói Ferido fora de combate pode ser escolhido
        assertNotNull(gameState.getPendingChoice(), "Deve abrir escolha de Herói Inimigo Ferido fora de combate.");
        assertTrue(gameState.getPendingChoice().getValidTargetUuids().contains(enemyBenchWounded.getUuid()),
                "Herói Ferido na bancada deve ser alvo válido.");
        assertFalse(gameState.getPendingChoice().getValidTargetUuids().contains(enemyBenchHealthy.getUuid()),
                "Herói com vida cheia na bancada NÃO deve ser alvo válido.");
        assertFalse(gameState.getPendingChoice().getValidTargetUuids().contains(enemyActive.getUuid()),
                "Herói em combate NÃO deve ser alvo válido da escolha fora de combate.");

        // Confirma escolha no herói ferido da bancada
        engine.submitPlayerChoice(p1.getUuid(), enemyBenchWounded.getUuid());

        // Assert 2
        assertEquals(8, enemyBenchWounded.getVidaAtual(), "Herói Ferido fora de combate deve sofrer 2 de Dano Direto (10 -> 8).");
        assertEquals(1, enemyActive.getVulnerabilidade(), "Herói Inimigo em combate deve receber +1 de Vulnerabilidade.");
    }

    @Test
    void testColheitaDosCondenadosScalesHealAndDirectDamagePerWoundedEnemy() {
        // Arrange
        HeroCard myHero = createHero(p1, "Atacante Ferido", 1, 5, 2, 15, 8, ZoneId.BATTLE);
        HeroCard enemyActive = createHero(p2, "Inimigo Ativo Ferido", 1, 4, 2, 20, 15, ZoneId.BATTLE);
        createHero(p2, "Inimigo Bancada Ferido", 1, 4, 2, 15, 12, ZoneId.BENCH);
        createHero(p2, "Inimigo Bancada Saudável", 1, 4, 2, 15, 15, ZoneId.BENCH);
        gameState.setActiveHeroPlayer1Id(myHero.getUuid());
        gameState.setActiveHeroPlayer2Id(enemyActive.getUuid());

        Card colheita = createCard(p1, "Colheita dos Condenados", CardType.ACTION, ItemType.NENHUM, 0, ZoneId.RESOLVE);

        // Act: Existem 2 Heróis Inimigos Feridos -> cura 2*2 = 4 e causa 1*2 = 2 de Dano Direto
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        colheita.getEffects().forEach(eff -> engine.getResolutionQueue().enqueue(eff, colheita));
        engine.getResolutionQueue().resolveAll(engine);

        // Assert
        assertEquals(12, myHero.getVidaAtual(), "Seu Herói deve recuperar 4 de Vida (8 -> 12) pelos 2 inimigos Feridos.");
        assertEquals(13, enemyActive.getVidaAtual(), "O Herói Inimigo ativo deve sofrer 2 de Dano Direto (15 -> 13).");
    }

    @Test
    void testSentencaDaGuilhotinaAndCuteloDoSuplicioWoundedMechanics() {
        // Arrange
        HeroCard myHero = createHero(p1, "Atacante", 1, 5, 2, 15, 15, ZoneId.BATTLE);
        HeroCard enemyActive = createHero(p2, "Inimigo Ferido", 1, 4, 2, 20, 14, ZoneId.BATTLE);
        gameState.setActiveHeroPlayer1Id(myHero.getUuid());
        gameState.setActiveHeroPlayer2Id(enemyActive.getUuid());

        Card cutelo = createCard(p1, "Cutelo do Suplício", CardType.ITEM, ItemType.ARMA, 1, ZoneId.RESOLVE);
        Card sentenca = createCard(p1, "Sentença da Guilhotina", CardType.ACTION, ItemType.NENHUM, 0, ZoneId.RESOLVE);

        // Act 1: Equipa Cutelo do Suplício e joga Sentença da Guilhotina na RESOLUTION
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        cutelo.getEffects().forEach(eff -> {
            if (eff.predicate(cutelo, TurnPhase.RESOLUTION)) engine.getResolutionQueue().enqueue(eff, cutelo);
        });
        sentenca.getEffects().forEach(eff -> {
            if (eff.predicate(sentenca, TurnPhase.RESOLUTION)) engine.getResolutionQueue().enqueue(eff, sentenca);
        });
        engine.getResolutionQueue().resolveAll(engine);

        // Assert 1
        assertEquals(7, myHero.getAtaque(), "Cutelo do Suplício deve conceder +2 de Ataque permanente ao equipar (5 -> 7).");
        assertEquals(2, enemyActive.getVulnerabilidade(), "Sentença da Guilhotina deve aplicar +2 de Vulnerabilidade no inimigo Ferido.");

        // Act 2: No início do próximo combate, Cutelo do Suplício concede +2 de Ataque adicional por inimigo estar Ferido
        engine.setTurnPhase(TurnPhase.COMBAT_START);
        cutelo.getEffects().forEach(eff -> {
            if (eff.predicate(cutelo, TurnPhase.COMBAT_START)) engine.getResolutionQueue().enqueue(eff, cutelo);
        });
        engine.getResolutionQueue().resolveAll(engine);

        // Assert 2
        assertEquals(9, myHero.getAtaque(), "Cutelo do Suplício deve conceder +2 de Ataque até o Final do Combate contra inimigo Ferido (7 -> 9).");

        // Act 3: Final do Combate -> Limpa buffs/debuffs temporários
        engine.runCombatEndCleanup();

        // Assert 3
        assertEquals(7, myHero.getAtaque(), "O bônus temporário (+2) do Cutelo deve expirar, mantendo apenas o bônus de equipamento (7).");
        assertEquals(0, enemyActive.getVulnerabilidade(), "A Vulnerabilidade temporária (+2) da Sentença da Guilhotina deve expirar no Final do Combate.");
    }
}

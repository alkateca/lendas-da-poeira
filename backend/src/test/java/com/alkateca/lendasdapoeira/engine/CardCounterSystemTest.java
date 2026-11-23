package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.effects.combat.MagicDamageEffect;
import com.alkateca.lendasdapoeira.effects.combat.ModifyHeroCounterEffect;
import com.alkateca.lendasdapoeira.effects.combat.PlaySpellMagicDamagePlusSpiritEffect;
import com.alkateca.lendasdapoeira.effects.endOfCombat.EndOfCombatMagicDamageEffect;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CardCounterSystemTest {

    private Player player1;
    private Player player2;
    private HeroCard heroP1Battle;
    private HeroCard heroP1Bench;
    private HeroCard heroP2Battle;
    private GameEngine engine;

    @BeforeEach
    void setUp() {
        player1 = new Player();
        player1.setUuid(UUID.randomUUID());
        player1.setName("Jogador 1");

        player2 = new Player();
        player2.setUuid(UUID.randomUUID());
        player2.setName("Jogador 2");

        heroP1Battle = createHero(player1.getUuid(), "Cavaleiro Onear", 3, 5, 2, 15);
        heroP1Bench = createHero(player1.getUuid(), "Isenora, Santa das Laminas", 2, 4, 2, 13);
        heroP2Battle = createHero(player2.getUuid(), "Inquisidor", 2, 4, 2, 15);

        player1.setCurrentDeck(new Deck("Deck P1", new ArrayList<>(), new ArrayList<>(List.of(heroP1Battle, heroP1Bench))));
        player2.setCurrentDeck(new Deck("Deck P2", new ArrayList<>(), new ArrayList<>(List.of(heroP2Battle))));

        engine = new GameEngine(player1, player2);
        engine.setTurnPhase(TurnPhase.CHOICE);
        engine.getGameState().setTurnPhase(TurnPhase.CHOICE);
        engine.setActivePlayerId(player1.getUuid());
        engine.getGameState().setActiveTurnPlayerId(player1.getUuid());
        engine.submitHeroChoice(player1.getUuid(), heroP1Battle.getUuid(), heroP2Battle.getUuid());
    }

    private HeroCard createHero(UUID ownerId, String name, int esp, int atk, int def, int hp) {
        HeroCard hero = new HeroCard();
        hero.setUuid(UUID.randomUUID());
        hero.setOwnerId(ownerId);
        hero.setCardName(name);
        hero.setCardType(CardType.HERO);
        hero.setColor(Colors.BLUE);
        hero.setEspirito(esp);
        hero.setAtaque(atk);
        hero.setDefesa(def);
        hero.setVidaMaxima(hp);
        hero.setVidaAtual(hp);
        hero.setMaxWeaponSlots(2);
        hero.setEstaVivo(true);
        hero.setEstaAtivo(true);
        return hero;
    }

    @Test
    @DisplayName("1. Adição, acúmulo, remoção parcial e remoção total ao atingir 0")
    void shouldAddAccumulateAndRemoveCountersCleanly() {
        // Arrange & Act
        assertEquals(2, engine.addCounter(heroP1Battle, CounterType.POEIRA, 2));
        assertEquals(5, engine.addCounter(heroP1Battle, CounterType.POEIRA, 3));
        assertEquals(5, engine.addCounter(heroP1Battle, CounterType.POEIRA, 0));
        assertEquals(5, engine.addCounter(heroP1Battle, CounterType.POEIRA, -2));

        // Assert acúmulo
        assertEquals(5, engine.getCounterCount(heroP1Battle, CounterType.POEIRA));
        assertTrue(heroP1Battle.hasCounter(CounterType.POEIRA));
        assertEquals(5, heroP1Battle.getTotalCounters());

        // Act remoção parcial e remoção até 0 (clamp)
        assertEquals(2, engine.removeCounter(heroP1Battle, CounterType.POEIRA, 3));
        assertEquals(0, engine.removeCounter(heroP1Battle, CounterType.POEIRA, 10));

        // Assert entrada removida ao chegar a 0
        assertFalse(heroP1Battle.hasCounter(CounterType.POEIRA));
        assertFalse(heroP1Battle.getCounters().containsKey(CounterType.POEIRA));
        assertEquals(0, heroP1Battle.getTotalCounters());
    }

    @Test
    @DisplayName("2. Múltiplos tipos de contadores e serialização no MatchSnapshotDTO (BENCH, BATTLE e RESOLVE)")
    void shouldSerializeMultipleCounterTypesAcrossBenchBattleAndResolve() {
        // Arrange: Poeira + Pólvora no Herói em BATTLE; Veneno no Herói em BENCH
        engine.addCounter(heroP1Battle, CounterType.POEIRA, 3);
        engine.addCounter(heroP1Battle, CounterType.POLVORA, 2);
        engine.addCounter(heroP1Bench, CounterType.VENENO, 4);

        Card resolveCard = new Card();
        resolveCard.setUuid(UUID.randomUUID());
        resolveCard.setOwnerId(player1.getUuid());
        resolveCard.setCardName("Bateria de Poeira");
        resolveCard.setCardType(CardType.SPELL);
        resolveCard.setColor(Colors.RED);
        resolveCard.setZoneId(ZoneId.RESOLVE);
        engine.addCounter(resolveCard, CounterType.POEIRA, 1);
        engine.getGameState().getPlayer1PlayedCards().add(resolveCard);

        // Act: Snapshot durante PREPARATION (carta em RESOLVE visível apenas para P1)
        engine.setTurnPhase(TurnPhase.PREPARATION);
        MatchSnapshotDTO p1View = SnapshotFactory.createSnapshotForViewer(engine, player1.getUuid(), null, null, "TEST");
        MatchSnapshotDTO p2View = SnapshotFactory.createSnapshotForViewer(engine, player2.getUuid(), null, null, "TEST");

        // Assert Herói em BATTLE (P1) visível para ambos
        MatchSnapshotDTO.HeroSnapshotDTO p1BattleDto = p1View.getLocalPlayer().getHeroes().stream()
                .filter(h -> h.getUuid().equals(heroP1Battle.getUuid()))
                .findFirst().orElseThrow();
        assertEquals(ZoneId.BATTLE.name(), p1BattleDto.getZoneId());
        assertEquals(2, p1BattleDto.getCounters().size());
        assertEquals("POEIRA", p1BattleDto.getCounters().get(0).getCounterType());
        assertEquals("Poeira", p1BattleDto.getCounters().get(0).getDisplayName());
        assertEquals("counter_poeira", p1BattleDto.getCounters().get(0).getIconKey());
        assertEquals(3, p1BattleDto.getCounters().get(0).getQuantity());
        assertEquals("POLVORA", p1BattleDto.getCounters().get(1).getCounterType());
        assertEquals(2, p1BattleDto.getCounters().get(1).getQuantity());

        // Assert Herói em BENCH (P1) também visível para o oponente (P2)
        MatchSnapshotDTO.HeroSnapshotDTO oppBenchFromP2 = p2View.getOpponentPlayer().getHeroes().stream()
                .filter(h -> h.getUuid().equals(heroP1Bench.getUuid()))
                .findFirst().orElseThrow();
        assertEquals(ZoneId.BENCH.name(), oppBenchFromP2.getZoneId());
        assertEquals(1, oppBenchFromP2.getCounters().size());
        assertEquals("VENENO", oppBenchFromP2.getCounters().get(0).getCounterType());
        assertEquals(4, oppBenchFromP2.getCounters().get(0).getQuantity());

        // Assert Carta oculta em RESOLVE durante PREPARATION não vaza contadores ao oponente
        assertEquals(1, p1View.getLocalPlayer().getPlayedCardsInResolve().get(0).getCounters().size());
        assertTrue(p2View.getOpponentPlayer().getPlayedCardsInResolve().get(0).getCounters().isEmpty());
    }

    @Test
    @DisplayName("3. ModifyHeroCounterEffect via ResolutionQueue gera snapshot com contadores atualizados")
    void shouldApplyCountersThroughResolutionQueueAndEmitSnapshot() {
        // Arrange
        Card spell = new Card();
        spell.setUuid(UUID.randomUUID());
        spell.setOwnerId(player1.getUuid());
        spell.setCardName("Carga de Pólvora");
        spell.setCardType(CardType.SPELL);
        spell.setColor(Colors.RED);
        spell.setZoneId(ZoneId.RESOLVE);
        spell.setEffects(new ArrayList<>(List.of(
                new ModifyHeroCounterEffect(TurnPhase.RESOLUTION, ModifyHeroCounterEffect.CounterTargetScope.ACTIVE_ENEMY_HERO, CounterType.VENENO, 2),
                new ModifyHeroCounterEffect(TurnPhase.RESOLUTION, ModifyHeroCounterEffect.CounterTargetScope.ACTIVE_ALLY_HERO, CounterType.POLVORA, 3)
        )));

        engine.setTurnPhase(TurnPhase.RESOLUTION);
        int snapshotsBefore = engine.getSnapshotHistory().size();

        // Act
        spell.onPhaseChange(TurnPhase.RESOLUTION, engine.getResolutionQueue());
        engine.getResolutionQueue().resolveAll(engine);

        // Assert
        assertEquals(2, engine.getCounterCount(heroP2Battle, CounterType.VENENO));
        assertEquals(3, engine.getCounterCount(heroP1Battle, CounterType.POLVORA));
        assertTrue(engine.getSnapshotHistory().size() > snapshotsBefore);

        MatchSnapshotDTO latest = engine.getSnapshotHistory().get(engine.getSnapshotHistory().size() - 1);
        MatchSnapshotDTO.HeroSnapshotDTO p2Dto = latest.getOpponentPlayer().getHeroes().get(0);
        assertEquals(1, p2Dto.getCounters().size());
        assertEquals("VENENO", p2Dto.getCounters().get(0).getCounterType());
        assertEquals(2, p2Dto.getCounters().get(0).getQuantity());
    }

    @Test
    @DisplayName("4. Derrota do Herói em COMBAT_END e desanexo limpam contadores; Evolução preserva contadores")
    void shouldClearCountersOnHeroDeathAndDetachWhilePreservingOnEvolution() {
        // Arrange: contadores nos dois heróis e em uma carta anexada
        engine.addCounter(heroP1Battle, CounterType.POEIRA, 4);
        engine.addCounter(heroP2Battle, CounterType.VENENO, 3);

        Card attachment = new Card();
        attachment.setUuid(UUID.randomUUID());
        attachment.setOwnerId(player1.getUuid());
        attachment.setCardName("Amuleto Carregado");
        attachment.setCardType(CardType.ITEM);
        attachment.setZoneId(ZoneId.ATTACHED);
        attachment.setAttachedToCardId(heroP1Battle.getUuid());
        engine.addCounter(attachment, CounterType.POEIRA, 2);
        player1.getCurrentDeck().getCardList().add(attachment);

        // Act 1: Evolução do herói em campo deve preservar seus contadores
        HeroCard evolvedTemplate = createHero(player1.getUuid(), "Cavaleiro Onear, Ascendido", 5, 7, 4, 20);
        engine.evolveHero(player1, heroP1Battle.getUuid(), evolvedTemplate);
        assertEquals(4, engine.getCounterCount(heroP1Battle, CounterType.POEIRA));

        // Act 2: Consumo manual e morte do Herói inimigo no COMBAT_END
        assertEquals(3, engine.consumeAllCounters(heroP2Battle, CounterType.VENENO));
        assertEquals(0, engine.getCounterCount(heroP2Battle, CounterType.VENENO));

        engine.addCounter(heroP2Battle, CounterType.POLVORA, 2);
        heroP2Battle.setVidaAtual(0);
        engine.setTurnPhase(TurnPhase.COMBAT_END);
        engine.combatEnd();

        // Assert: Herói derrotado teve contadores limpos; Herói vivo manteve os contadores; anexo descartado foi limpo
        assertFalse(Boolean.TRUE.equals(heroP2Battle.getEstaVivo()));
        assertEquals(0, heroP2Battle.getTotalCounters());
        assertEquals(4, engine.getCounterCount(heroP1Battle, CounterType.POEIRA));

        engine.detachCard(attachment, ZoneId.DISCARD);
        assertEquals(0, attachment.getTotalCounters());
    }

    @Test
    @DisplayName("5. CounterType.fromKeyOrLabel resolve nomes com e sem acento")
    void shouldResolveCounterTypesFromKeysAndLabels() {
        assertEquals(CounterType.POEIRA, CounterType.fromKeyOrLabel("poeira"));
        assertEquals(CounterType.POEIRA, CounterType.fromKeyOrLabel("POEIRA"));
        assertEquals(CounterType.VENENO, CounterType.fromKeyOrLabel("Veneno"));
        assertEquals(CounterType.POLVORA, CounterType.fromKeyOrLabel("Pólvora"));
        assertEquals(CounterType.POLVORA, CounterType.fromKeyOrLabel("polvora"));
        assertEquals(CounterType.POLVORA, CounterType.fromKeyOrLabel("counter_polvora"));
        assertEquals(CounterType.COMBUSTAO, CounterType.fromKeyOrLabel("Combustão"));
        assertEquals(CounterType.COMBUSTAO, CounterType.fromKeyOrLabel("combustao"));
        assertNull(CounterType.fromKeyOrLabel("desconhecido"));
    }

    @Test
    @DisplayName("6. Resíduo Incendiário adiciona 5 Combustão; APENAS Magias de Fogo somam e zeram os contadores")
    void shouldAddFiveCombustionFromResiduoIncendiarioAndTriggerOnlyOnFireSpells() {
        // Arrange: Carrega "Resíduo Incendiário" do catálogo real (cards.json + CardFactory)
        CardFactory factory = new CardFactory();
        Card residuoTemplate = factory.getAllCatalogCards().stream()
                .filter(c -> "Resíduo Incendiário".equals(c.getCardName()))
                .findFirst()
                .orElseThrow();

        Card residuo = new Card();
        residuo.setUuid(UUID.randomUUID());
        residuo.setOwnerId(player1.getUuid());
        residuo.setCardName(residuoTemplate.getCardName());
        residuo.setCardType(residuoTemplate.getCardType());
        residuo.setColor(residuoTemplate.getColor());
        residuo.setItemType(residuoTemplate.getItemType());
        residuo.setEmpunhadura(residuoTemplate.getEmpunhadura());
        residuo.setAfinidades(new ArrayList<>(residuoTemplate.getAfinidades()));
        residuo.setDescricao(residuoTemplate.getDescricao());
        residuo.setZoneId(ZoneId.RESOLVE);
        CardFactory.buildCardLogic(residuo);

        assertEquals(CardType.SPELL, residuo.getCardType());
        assertEquals(Colors.BLUE, residuo.getColor());
        assertEquals(0, residuo.getEmpunhadura());
        assertTrue(residuo.getKeywords().contains("COMBUSTAO"));

        // Act 1: Resolve Resíduo Incendiário -> 5 Combustão no Herói Inimigo ativo
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        residuo.onPhaseChange(TurnPhase.RESOLUTION, engine.getResolutionQueue());
        engine.getResolutionQueue().resolveAll(engine);

        assertEquals(5, engine.getCounterCount(heroP2Battle, CounterType.COMBUSTAO));
        assertTrue(heroP2Battle.hasCounter(CounterType.COMBUSTAO));

        // Act 2: Magia NÃO-Fogo (ex: Cristal) NÃO deve consumir nem somar Combustão
        Card cristalSpell = new Card();
        cristalSpell.setUuid(UUID.randomUUID());
        cristalSpell.setOwnerId(player1.getUuid());
        cristalSpell.setCardName("Feitiço de Cristal Teste");
        cristalSpell.setCardType(CardType.SPELL);
        cristalSpell.setAfinidades(new ArrayList<>(List.of(Afinidade.CRISTAL)));
        cristalSpell.setEffects(new ArrayList<>(List.of(new MagicDamageEffect(4))));

        int hpAntesCristal = heroP2Battle.getVidaAtual(); // 15
        cristalSpell.onPhaseChange(TurnPhase.RESOLUTION, engine.getResolutionQueue());
        engine.getResolutionQueue().resolveAll(engine);

        // 4 dano mágico - 2 espírito de heroP2Battle = 2 de dano (sem os +5 de Combustão)
        assertEquals(hpAntesCristal - 2, heroP2Battle.getVidaAtual());
        assertEquals(5, engine.getCounterCount(heroP2Battle, CounterType.COMBUSTAO),
                "Magia que não é de Fogo não deve consumir marcadores de Combustão.");

        // Act 3: Habilidade de Herói com Afinidade Fogo (CardType.HERO, NÃO é SPELL) NÃO deve consumir Combustão
        heroP1Battle.setCardType(CardType.HERO);
        heroP1Battle.setAfinidades(new ArrayList<>(List.of(Afinidade.FOGO)));
        heroP1Battle.setEffects(new ArrayList<>(List.of(new EndOfCombatMagicDamageEffect(3))));
        int hpAntesHeroFire = heroP2Battle.getVidaAtual(); // 13
        engine.setTurnPhase(TurnPhase.COMBAT_END);
        heroP1Battle.onPhaseChange(TurnPhase.COMBAT_END, engine.getResolutionQueue());
        engine.getResolutionQueue().resolveAll(engine);

        // 3 - 2 espírito = 1 de dano
        assertEquals(hpAntesHeroFire - 1, heroP2Battle.getVidaAtual());
        assertEquals(5, engine.getCounterCount(heroP2Battle, CounterType.COMBUSTAO),
                "Habilidade de Herói de Fogo (não-SPELL) não deve consumir Combustão.");

        // Act 4: Magia de Fogo (SPELL + Afinidade.FOGO: "Bola de fogo") SOMA +5 ao dano e ZERA os contadores!
        Card bolaDeFogo = new Card();
        bolaDeFogo.setUuid(UUID.randomUUID());
        bolaDeFogo.setOwnerId(player1.getUuid());
        bolaDeFogo.setCardName("Bola de fogo");
        bolaDeFogo.setCardType(CardType.SPELL);
        bolaDeFogo.setAfinidades(new ArrayList<>(List.of(Afinidade.FOGO)));
        bolaDeFogo.setEffects(new ArrayList<>(List.of(new PlaySpellMagicDamagePlusSpiritEffect(4))));

        int hpAntesBolaDeFogo = heroP2Battle.getVidaAtual(); // 12
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        bolaDeFogo.onPhaseChange(TurnPhase.RESOLUTION, engine.getResolutionQueue());
        engine.getResolutionQueue().resolveAll(engine);

        // Dano bruto = 4 + 3 (Espírito heroP1Battle) + 5 (Combustão) = 12; mitigado por 2 (Espírito heroP2Battle) = 10
        assertEquals(hpAntesBolaDeFogo - 10, heroP2Battle.getVidaAtual());
        assertEquals(0, engine.getCounterCount(heroP2Battle, CounterType.COMBUSTAO),
                "Magia de Fogo deve zerar os contadores de Combustão após somar o dano.");
        assertFalse(heroP2Battle.getCounters().containsKey(CounterType.COMBUSTAO));
    }

    @Test
    @DisplayName("7. BOTH_ACTIVE_HEROES e múltiplos escopos anexam tokens simultaneamente no Herói atual aliado e no Herói inimigo atual")
    void shouldAttachCountersToBothCurrentPlayerHeroAndCurrentEnemyHeroSimultaneously() {
        // Arrange
        HeroCard heroP2Bench = createHero(player2.getUuid(), "Sentinela no Banco", 1, 3, 2, 12);
        heroP2Bench.setZoneId(ZoneId.BENCH);
        player2.getCurrentDeck().getHeroList().add(heroP2Bench);
        heroP1Bench.setZoneId(ZoneId.BENCH);

        Card dualSpell = new Card();
        dualSpell.setUuid(UUID.randomUUID());
        dualSpell.setOwnerId(player1.getUuid());
        dualSpell.setCardName("Convergência da Poeira");
        dualSpell.setCardType(CardType.SPELL);
        dualSpell.setColor(Colors.BLUE);
        dualSpell.setZoneId(ZoneId.RESOLVE);
        dualSpell.setEffects(new ArrayList<>(List.of(
                new ModifyHeroCounterEffect(
                        TurnPhase.RESOLUTION,
                        ModifyHeroCounterEffect.CounterTargetScope.BOTH_ACTIVE_HEROES,
                        CounterType.POEIRA,
                        3
                ),
                new ModifyHeroCounterEffect(
                        TurnPhase.RESOLUTION,
                        List.of(
                                ModifyHeroCounterEffect.CounterTargetScope.ACTIVE_ALLY_HERO,
                                ModifyHeroCounterEffect.CounterTargetScope.ACTIVE_ENEMY_HERO
                        ),
                        CounterType.POLVORA,
                        2
                )
        )));

        engine.setTurnPhase(TurnPhase.RESOLUTION);

        // Act
        dualSpell.onPhaseChange(TurnPhase.RESOLUTION, engine.getResolutionQueue());
        engine.getResolutionQueue().resolveAll(engine);

        // Assert: apenas os dois Heróis em combate recebem ambos os tokens
        assertEquals(3, engine.getCounterCount(heroP1Battle, CounterType.POEIRA));
        assertEquals(3, engine.getCounterCount(heroP2Battle, CounterType.POEIRA));
        assertEquals(2, engine.getCounterCount(heroP1Battle, CounterType.POLVORA));
        assertEquals(2, engine.getCounterCount(heroP2Battle, CounterType.POLVORA));

        // Heróis fora de combate permanecem sem contadores
        assertEquals(0, engine.getCounterCount(heroP1Bench, CounterType.POEIRA));
        assertEquals(0, engine.getCounterCount(heroP2Bench, CounterType.POEIRA));

        MatchSnapshotDTO snap = SnapshotFactory.createSnapshotForViewer(engine, player1.getUuid(), null, null, "TEST");
        MatchSnapshotDTO.HeroSnapshotDTO allyBattleDto = snap.getLocalPlayer().getHeroes().stream()
                .filter(h -> h.getUuid().equals(heroP1Battle.getUuid())).findFirst().orElseThrow();
        MatchSnapshotDTO.HeroSnapshotDTO enemyBattleDto = snap.getOpponentPlayer().getHeroes().stream()
                .filter(h -> h.getUuid().equals(heroP2Battle.getUuid())).findFirst().orElseThrow();
        assertEquals(2, allyBattleDto.getCounters().size());
        assertEquals(2, enemyBattleDto.getCounters().size());
    }

    @Test
    @DisplayName("8. Escopos diretos de fora de combate atingem apenas Heróis vivos na bancada (BENCH)")
    void shouldAttachCountersDirectlyToOutOfCombatHeroesOnly() {
        // Arrange
        heroP1Bench.setZoneId(ZoneId.BENCH);
        HeroCard heroP1BenchDead = createHero(player1.getUuid(), "Aliado Caído", 1, 2, 1, 0);
        heroP1BenchDead.setZoneId(ZoneId.BENCH);
        heroP1BenchDead.setEstaVivo(false);
        player1.getCurrentDeck().getHeroList().add(heroP1BenchDead);

        HeroCard heroP2Bench1 = createHero(player2.getUuid(), "Inimigo Banco 1", 1, 3, 2, 12);
        heroP2Bench1.setZoneId(ZoneId.BENCH);
        HeroCard heroP2Bench2 = createHero(player2.getUuid(), "Inimigo Banco 2", 2, 4, 1, 14);
        heroP2Bench2.setZoneId(ZoneId.BENCH);
        player2.getCurrentDeck().getHeroList().addAll(List.of(heroP2Bench1, heroP2Bench2));

        Card benchAction = new Card();
        benchAction.setUuid(UUID.randomUUID());
        benchAction.setOwnerId(player1.getUuid());
        benchAction.setCardName("Névoa de Retaguarda");
        benchAction.setCardType(CardType.ACTION);
        benchAction.setZoneId(ZoneId.RESOLVE);
        benchAction.setEffects(new ArrayList<>(List.of(
                new ModifyHeroCounterEffect(
                        TurnPhase.RESOLUTION,
                        ModifyHeroCounterEffect.CounterTargetScope.OUT_OF_COMBAT_ALLY_HEROES,
                        CounterType.POEIRA,
                        2
                ),
                new ModifyHeroCounterEffect(
                        TurnPhase.RESOLUTION,
                        ModifyHeroCounterEffect.CounterTargetScope.OUT_OF_COMBAT_ENEMY_HEROES,
                        CounterType.VENENO,
                        3
                ),
                new ModifyHeroCounterEffect(
                        TurnPhase.RESOLUTION,
                        ModifyHeroCounterEffect.CounterTargetScope.ALL_OUT_OF_COMBAT_HEROES,
                        CounterType.COMBUSTAO,
                        1
                )
        )));

        engine.setTurnPhase(TurnPhase.RESOLUTION);

        // Act
        benchAction.onPhaseChange(TurnPhase.RESOLUTION, engine.getResolutionQueue());
        engine.getResolutionQueue().resolveAll(engine);

        // Assert: Heróis em combate não são afetados
        assertEquals(0, heroP1Battle.getTotalCounters());
        assertEquals(0, heroP2Battle.getTotalCounters());
        assertEquals(0, heroP1BenchDead.getTotalCounters());

        // Assert: Aliados vivos fora de combate recebem POEIRA + COMBUSTAO
        assertEquals(2, engine.getCounterCount(heroP1Bench, CounterType.POEIRA));
        assertEquals(1, engine.getCounterCount(heroP1Bench, CounterType.COMBUSTAO));
        assertEquals(0, engine.getCounterCount(heroP1Bench, CounterType.VENENO));

        // Assert: Inimigos vivos fora de combate recebem VENENO + COMBUSTAO
        assertEquals(3, engine.getCounterCount(heroP2Bench1, CounterType.VENENO));
        assertEquals(1, engine.getCounterCount(heroP2Bench1, CounterType.COMBUSTAO));
        assertEquals(3, engine.getCounterCount(heroP2Bench2, CounterType.VENENO));
        assertEquals(1, engine.getCounterCount(heroP2Bench2, CounterType.COMBUSTAO));
    }

    @Test
    @DisplayName("9. Escolha interativa via PendingChoice anexa tokens em Herói fora de combate escolhido")
    void shouldAllowInteractivePendingChoiceTargetingOutOfCombatHeroes() {
        // Arrange
        heroP1Bench.setZoneId(ZoneId.BENCH);
        HeroCard heroP2Bench1 = createHero(player2.getUuid(), "Alvo Banco 1", 1, 3, 2, 12);
        heroP2Bench1.setZoneId(ZoneId.BENCH);
        HeroCard heroP2Bench2 = createHero(player2.getUuid(), "Alvo Banco 2", 2, 4, 1, 14);
        heroP2Bench2.setZoneId(ZoneId.BENCH);
        player2.getCurrentDeck().getHeroList().addAll(List.of(heroP2Bench1, heroP2Bench2));

        Card targetedSpell = new Card();
        targetedSpell.setUuid(UUID.randomUUID());
        targetedSpell.setOwnerId(player1.getUuid());
        targetedSpell.setCardName("Selo à Distância");
        targetedSpell.setCardType(CardType.SPELL);
        targetedSpell.setZoneId(ZoneId.RESOLVE);
        targetedSpell.setEffects(new ArrayList<>(List.of(
                new ModifyHeroCounterEffect(
                        TurnPhase.RESOLUTION,
                        ModifyHeroCounterEffect.CounterTargetScope.CHOOSE_OUT_OF_COMBAT_ENEMY_HERO,
                        CounterType.POLVORA,
                        4
                )
        )));

        engine.setTurnPhase(TurnPhase.RESOLUTION);

        // Act 1: Executa na fila até pausar aguardando escolha
        targetedSpell.onPhaseChange(TurnPhase.RESOLUTION, engine.getResolutionQueue());
        engine.getResolutionQueue().resolveAll(engine);

        assertTrue(engine.isWaitingForPlayerChoice(), "Deve pausar esperando escolha do Herói fora de combate.");
        MatchSnapshotDTO.PendingChoiceDTO pending = engine.getGameState().getPendingChoice();
        assertEquals(player1.getUuid(), pending.getChoosingPlayerId());
        assertEquals("ENEMY_HERO", pending.getTargetType());
        assertEquals(2, pending.getValidTargetUuids().size());
        assertFalse(pending.getValidTargetUuids().contains(heroP2Battle.getUuid()),
                "Herói inimigo em combate não deve aparecer como alvo de CHOOSE_OUT_OF_COMBAT_ENEMY_HERO.");
        assertTrue(pending.getValidTargetUuids().contains(heroP2Bench1.getUuid()));
        assertTrue(pending.getValidTargetUuids().contains(heroP2Bench2.getUuid()));

        // Act 2: Jogador escolhe heroP2Bench2
        engine.submitPlayerChoice(player1.getUuid(), heroP2Bench2.getUuid());

        // Assert
        assertFalse(engine.isWaitingForPlayerChoice());
        assertEquals(4, engine.getCounterCount(heroP2Bench2, CounterType.POLVORA));
        assertEquals(0, engine.getCounterCount(heroP2Bench1, CounterType.POLVORA));
        assertEquals(0, engine.getCounterCount(heroP2Battle, CounterType.POLVORA));
    }

    @Test
    @DisplayName("10. Heróis fora de combate e cartas anexadas a eles disparam contadores quando canTriggerFromBench = true")
    void shouldTriggerCounterEffectsFromBenchHeroesAndCardsAttachedToOutOfCombatHeroes() {
        // Arrange: Herói aliado na BENCH com triggerFromBench = true aplica Poeira no aliado ativo e inimigo ativo
        heroP1Bench.setZoneId(ZoneId.BENCH);
        heroP1Bench.setEffects(new ArrayList<>(List.of(
                new ModifyHeroCounterEffect(
                        TurnPhase.COMBAT_START,
                        ModifyHeroCounterEffect.CounterTargetScope.BOTH_ACTIVE_HEROES,
                        CounterType.POEIRA,
                        1,
                        true
                )
        )));

        // Carta anexada a um Herói inimigo fora de combate (BENCH) aplica Veneno no portador (SELF_OR_ATTACHED_HERO) no COMBAT_END
        HeroCard heroP2Bench = createHero(player2.getUuid(), "Alvo Marcado", 2, 3, 2, 14);
        heroP2Bench.setZoneId(ZoneId.BENCH);
        player2.getCurrentDeck().getHeroList().add(heroP2Bench);

        Card attachedCurse = new Card();
        attachedCurse.setUuid(UUID.randomUUID());
        attachedCurse.setOwnerId(player1.getUuid());
        attachedCurse.setCardName("Parasita de Poeira");
        attachedCurse.setCardType(CardType.ITEM);
        attachedCurse.setZoneId(ZoneId.ATTACHED);
        attachedCurse.setAttachedToCardId(heroP2Bench.getUuid());
        attachedCurse.setEffects(new ArrayList<>(List.of(
                new ModifyHeroCounterEffect(
                        TurnPhase.COMBAT_END,
                        ModifyHeroCounterEffect.CounterTargetScope.SELF_OR_ATTACHED_HERO,
                        CounterType.VENENO,
                        2,
                        true
                )
        )));
        player1.getCurrentDeck().getCardList().add(attachedCurse);

        // Act: Executa cadeia completa de combate (PREPARATION -> COMBAT_START -> RESOLUTION -> COMBAT -> COMBAT_END -> DISCARD)
        engine.submitPreparationCards(player1.getUuid(), List.of());
        engine.submitPreparationCards(player2.getUuid(), List.of());

        // Assert
        assertEquals(1, engine.getCounterCount(heroP1Battle, CounterType.POEIRA),
                "Herói aliado no banco com triggerFromBench deve aplicar contador no aliado ativo em COMBAT_START.");
        assertEquals(1, engine.getCounterCount(heroP2Battle, CounterType.POEIRA),
                "Herói aliado no banco com triggerFromBench deve aplicar contador no inimigo ativo em COMBAT_START.");
        assertEquals(2, engine.getCounterCount(heroP2Bench, CounterType.VENENO),
                "Carta anexada a Herói inimigo fora de combate deve disparar no COMBAT_END.");
    }

    @Test
    @DisplayName("11. Sistema de tokens executa em todas as etapas do turno (GAME_START, CHOICE, PREPARATION, COMBAT_START, RESOLUTION, COMBAT, COMBAT_END e DISCARD)")
    void shouldTriggerCountersAcrossAllTurnPhasesIncludingChoicePreparationCombatAndDiscardWithoutDoubleFiring() {
        // Arrange: Novo motor com partida completa iniciando em startGame()
        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        p1.setName("P1 Completo");
        Player p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        p2.setName("P2 Completo");

        HeroCard p1Main = createHero(p1.getUuid(), "Mestre de Turnos", 3, 4, 2, 20);
        HeroCard p1Bench = createHero(p1.getUuid(), "Suporte de Banco", 2, 3, 2, 16);
        HeroCard p2Main = createHero(p2.getUuid(), "Oponente Principal", 2, 3, 2, 20);
        HeroCard p2Bench = createHero(p2.getUuid(), "Oponente Banco", 1, 2, 2, 16);

        // Efeitos em TODAS as 8 etapas ativas do TurnPhase:
        p1Main.setEffects(new ArrayList<>(List.of(
                // 1. GAME_START: P1 escolhe um Herói inimigo para receber +1 POEIRA (pausa startGame até escolha)
                new ModifyHeroCounterEffect(
                        TurnPhase.GAME_START,
                        ModifyHeroCounterEffect.CounterTargetScope.CHOOSE_ENEMY_HERO,
                        CounterType.POEIRA,
                        1,
                        true
                ),
                // 2. CHOICE: todos os Heróis vivos recebem +1 POEIRA ao entrar na escolha
                new ModifyHeroCounterEffect(
                        TurnPhase.CHOICE,
                        ModifyHeroCounterEffect.CounterTargetScope.ALL_LIVING_HEROES,
                        CounterType.POEIRA,
                        1,
                        true
                ),
                // 3. PREPARATION: ambos os Heróis em combate recebem +1 POLVORA
                new ModifyHeroCounterEffect(
                        TurnPhase.PREPARATION,
                        ModifyHeroCounterEffect.CounterTargetScope.BOTH_ACTIVE_HEROES,
                        CounterType.POLVORA,
                        1
                ),
                // 4. COMBAT_START: Herói inimigo ativo recebe +1 COMBUSTAO
                new ModifyHeroCounterEffect(
                        TurnPhase.COMBAT_START,
                        ModifyHeroCounterEffect.CounterTargetScope.ACTIVE_ENEMY_HERO,
                        CounterType.COMBUSTAO,
                        1
                ),
                // 6. COMBAT: Herói inimigo fora de combate (escolhido via PendingChoice) recebe +2 VENENO
                new ModifyHeroCounterEffect(
                        TurnPhase.COMBAT,
                        ModifyHeroCounterEffect.CounterTargetScope.CHOOSE_OUT_OF_COMBAT_ENEMY_HERO,
                        CounterType.VENENO,
                        2
                ),
                // 7. COMBAT_END: Heróis aliados fora de combate recebem +1 COMBUSTAO
                new ModifyHeroCounterEffect(
                        TurnPhase.COMBAT_END,
                        ModifyHeroCounterEffect.CounterTargetScope.OUT_OF_COMBAT_ALLY_HEROES,
                        CounterType.COMBUSTAO,
                        1
                ),
                // 8. DISCARD: todos os aliados vivos recebem +1 VENENO (deve disparar exatamente 1x por fase DISCARD!)
                new ModifyHeroCounterEffect(
                        TurnPhase.DISCARD,
                        ModifyHeroCounterEffect.CounterTargetScope.ALL_ALLY_HEROES,
                        CounterType.VENENO,
                        1,
                        true
                )
        )));

        // 5. RESOLUTION: carta jogada adiciona +2 POLVORA em p1Bench (fora de combate)
        Card resolutionCard = new Card();
        resolutionCard.setUuid(UUID.randomUUID());
        resolutionCard.setOwnerId(p1.getUuid());
        resolutionCard.setCardName("Carga de Bancada");
        resolutionCard.setCardType(CardType.ACTION);
        resolutionCard.setColor(Colors.NEUTRAL);
        resolutionCard.setZoneId(ZoneId.DECK);
        resolutionCard.setEffects(new ArrayList<>(List.of(
                new ModifyHeroCounterEffect(
                        TurnPhase.RESOLUTION,
                        ModifyHeroCounterEffect.CounterTargetScope.OUT_OF_COMBAT_ALLY_HEROES,
                        CounterType.POLVORA,
                        2
                )
        )));

        List<Card> p1Cards = new ArrayList<>();
        p1Cards.add(resolutionCard);
        for (int i = 0; i < 6; i++) {
            Card filler = new Card();
            filler.setUuid(UUID.randomUUID());
            filler.setOwnerId(p1.getUuid());
            filler.setCardName("Ação Neutra P1 " + i);
            filler.setCardType(CardType.ACTION);
            filler.setColor(Colors.NEUTRAL);
            filler.setZoneId(ZoneId.DECK);
            p1Cards.add(filler);
        }

        List<Card> p2Cards = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Card filler = new Card();
            filler.setUuid(UUID.randomUUID());
            filler.setOwnerId(p2.getUuid());
            filler.setCardName("Ação Neutra P2 " + i);
            filler.setCardType(CardType.ACTION);
            filler.setColor(Colors.NEUTRAL);
            filler.setZoneId(ZoneId.DECK);
            p2Cards.add(filler);
        }

        p1.setCurrentDeck(new Deck("D1", p1Cards, new ArrayList<>(List.of(p1Main, p1Bench))));
        p2.setCurrentDeck(new Deck("D2", p2Cards, new ArrayList<>(List.of(p2Main, p2Bench))));

        GameEngine fullEngine = new GameEngine(p1, p2);

        // Act 1: startGame pausa em GAME_START aguardando escolha de CHOOSE_ENEMY_HERO
        fullEngine.startGame();
        assertEquals(TurnPhase.GAME_START, fullEngine.getTurnPhase());
        assertTrue(fullEngine.isWaitingForPlayerChoice());

        // Completa escolha de GAME_START -> deve finalizar GAME_START, entrar em CHOICE e disparar TurnPhase.CHOICE
        fullEngine.submitPlayerChoice(p1.getUuid(), p2Main.getUuid());
        assertEquals(TurnPhase.CHOICE, fullEngine.getTurnPhase());
        assertEquals(2, fullEngine.getCounterCount(p2Main, CounterType.POEIRA), "GAME_START (+1) + CHOICE (+1) = 2 POEIRA em p2Main.");
        assertEquals(1, fullEngine.getCounterCount(p1Main, CounterType.POEIRA), "CHOICE (+1 POEIRA).");
        assertEquals(1, fullEngine.getCounterCount(p1Bench, CounterType.POEIRA), "CHOICE (+1 POEIRA).");
        assertEquals(1, fullEngine.getCounterCount(p2Bench, CounterType.POEIRA), "CHOICE (+1 POEIRA).");

        // Act 2: Força iniciativa de P1 para escolher os Heróis e entrar em PREPARATION
        fullEngine.setActivePlayerId(p1.getUuid());
        fullEngine.getGameState().setActiveTurnPlayerId(p1.getUuid());
        fullEngine.submitHeroChoice(p1.getUuid(), p1Main.getUuid(), p2Main.getUuid());

        assertEquals(TurnPhase.PREPARATION, fullEngine.getTurnPhase());
        assertEquals(1, fullEngine.getCounterCount(p1Main, CounterType.POLVORA), "PREPARATION (+1 POLVORA em p1Main).");
        assertEquals(1, fullEngine.getCounterCount(p2Main, CounterType.POLVORA), "PREPARATION (+1 POLVORA em p2Main).");

        // Garante que resolutionCard está na mão de P1 para ser jogada
        resolutionCard.setZoneId(ZoneId.HAND);

        // Act 3: Ambos confirmam preparação -> roda COMBAT_START, RESOLUTION e pausa em COMBAT para escolher inimigo fora de combate!
        fullEngine.submitPreparationCards(p1.getUuid(), List.of(resolutionCard.getUuid()));
        fullEngine.submitPreparationCards(p2.getUuid(), List.of());

        assertEquals(TurnPhase.COMBAT, fullEngine.getTurnPhase(), "Deve pausar em COMBAT aguardando PendingChoice da etapa COMBAT.");
        assertTrue(fullEngine.isWaitingForPlayerChoice());
        assertEquals(1, fullEngine.getCounterCount(p2Main, CounterType.COMBUSTAO), "COMBAT_START (+1 COMBUSTAO em p2Main).");
        assertEquals(2, fullEngine.getCounterCount(p1Bench, CounterType.POLVORA), "RESOLUTION (+2 POLVORA em p1Bench fora de combate).");

        // Troca física em COMBAT deve ter ocorrido exatamente uma vez:
        // p1Main (4 ATK) vs p2Main (2 DEF) -> 2 dano em p2Main (20 -> 18)
        // p2Main (3 ATK) vs p1Main (2 DEF) -> 1 dano em p1Main (20 -> 19)
        assertEquals(18, p2Main.getVidaAtual());
        assertEquals(19, p1Main.getVidaAtual());

        // Act 4: Jogador resolve escolha da etapa COMBAT (escolhe p2Bench) -> retoma COMBAT (sem repetir dano físico!), executa COMBAT_END e entra em DISCARD!
        fullEngine.submitPlayerChoice(p1.getUuid(), p2Bench.getUuid());

        assertEquals(TurnPhase.DISCARD, fullEngine.getTurnPhase());
        assertEquals(18, p2Main.getVidaAtual(), "Troca física de COMBAT não pode ser repetida após retomar PendingChoice.");
        assertEquals(19, p1Main.getVidaAtual(), "Troca física de COMBAT não pode ser repetida após retomar PendingChoice.");
        assertEquals(2, fullEngine.getCounterCount(p2Bench, CounterType.VENENO), "COMBAT (+2 VENENO em p2Bench).");
        assertEquals(1, fullEngine.getCounterCount(p1Bench, CounterType.COMBUSTAO), "COMBAT_END (+1 COMBUSTAO em p1Bench).");
        assertEquals(1, fullEngine.getCounterCount(p1Main, CounterType.VENENO), "DISCARD deve aplicar +1 VENENO exatamente uma vez ao entrar na fase.");
        assertEquals(1, fullEngine.getCounterCount(p1Bench, CounterType.VENENO), "DISCARD deve aplicar +1 VENENO exatamente uma vez em p1Bench.");

        // Act 5: Ambos confirmam DISCARD -> não deve duplicar o efeito de entrada da fase DISCARD e deve avançar para novo Turno (CHOICE)
        fullEngine.submitDiscard(p1.getUuid(), List.of());
        assertEquals(1, fullEngine.getCounterCount(p1Main, CounterType.VENENO), "Confirmar DISCARD (P1) não deve duplicar o gatilho de TurnPhase.DISCARD.");

        fullEngine.submitDiscard(p2.getUuid(), List.of());
        assertEquals(TurnPhase.CHOICE, fullEngine.getTurnPhase(), "Após ambos confirmarem DISCARD, inicia novo turno em CHOICE.");
        assertEquals(1, fullEngine.getCounterCount(p1Main, CounterType.VENENO), "Confirmar DISCARD (P2) não deve duplicar o gatilho de TurnPhase.DISCARD.");
        // Novo Turno CHOICE disparou novamente (+1 POEIRA): p1Main passou de 1 -> 2
        assertEquals(2, fullEngine.getCounterCount(p1Main, CounterType.POEIRA), "Entrada no Turno 2 (CHOICE) deve disparar efeito de CHOICE novamente.");
    }
}

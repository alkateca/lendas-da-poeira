package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.counters.CounterConsumptionMode;
import com.alkateca.lendasdapoeira.counters.CounterRule;
import com.alkateca.lendasdapoeira.effects.combat.ConsumeCounterAndApplyEffect;
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
import java.util.Set;
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
        assertEquals(2, engine.addCounter(heroP1Battle, CounterType.SEDA, 2));
        assertEquals(5, engine.addCounter(heroP1Battle, CounterType.SEDA, 3));
        assertEquals(5, engine.addCounter(heroP1Battle, CounterType.SEDA, 0));
        assertEquals(5, engine.addCounter(heroP1Battle, CounterType.SEDA, -2));

        assertEquals(5, engine.getCounterCount(heroP1Battle, CounterType.SEDA));
        assertTrue(heroP1Battle.hasCounter(CounterType.SEDA));
        assertEquals(5, heroP1Battle.getTotalCounters());

        assertEquals(2, engine.removeCounter(heroP1Battle, CounterType.SEDA, 3));
        assertEquals(0, engine.removeCounter(heroP1Battle, CounterType.SEDA, 10));

        assertFalse(heroP1Battle.hasCounter(CounterType.SEDA));
        assertFalse(heroP1Battle.getCounters().containsKey(CounterType.SEDA));
        assertEquals(0, heroP1Battle.getTotalCounters());
    }

    @Test
    @DisplayName("2. Múltiplos tipos ativos (COMBUSTAO e SEDA) e serialização no MatchSnapshotDTO (BENCH, BATTLE e RESOLVE)")
    void shouldSerializeMultipleCounterTypesAcrossBenchBattleAndResolve() {
        engine.addCounter(heroP1Battle, CounterType.SEDA, 3);
        engine.addCounter(heroP1Battle, CounterType.COMBUSTAO, 2);
        engine.addCounter(heroP1Bench, CounterType.SEDA, 4);

        Card resolveCard = new Card();
        resolveCard.setUuid(UUID.randomUUID());
        resolveCard.setOwnerId(player1.getUuid());
        resolveCard.setCardName("Carretel Arcano");
        resolveCard.setCardType(CardType.SPELL);
        resolveCard.setColor(Colors.BLUE);
        resolveCard.setZoneId(ZoneId.RESOLVE);
        engine.addCounter(resolveCard, CounterType.SEDA, 1);
        engine.getGameState().getPlayer1PlayedCards().add(resolveCard);

        engine.setTurnPhase(TurnPhase.PREPARATION);
        MatchSnapshotDTO p1View = SnapshotFactory.createSnapshotForViewer(engine, player1.getUuid(), null, null, "TEST");
        MatchSnapshotDTO p2View = SnapshotFactory.createSnapshotForViewer(engine, player2.getUuid(), null, null, "TEST");

        MatchSnapshotDTO.HeroSnapshotDTO p1BattleDto = p1View.getLocalPlayer().getHeroes().stream()
                .filter(h -> h.getUuid().equals(heroP1Battle.getUuid()))
                .findFirst().orElseThrow();
        assertEquals(ZoneId.BATTLE.name(), p1BattleDto.getZoneId());
        assertEquals(2, p1BattleDto.getCounters().size());
        assertEquals("SEDA", p1BattleDto.getCounters().get(0).getCounterType());
        assertEquals("Seda", p1BattleDto.getCounters().get(0).getDisplayName());
        assertEquals("counter_seda", p1BattleDto.getCounters().get(0).getIconKey());
        assertEquals(3, p1BattleDto.getCounters().get(0).getQuantity());
        assertEquals("COMBUSTAO", p1BattleDto.getCounters().get(1).getCounterType());
        assertEquals(2, p1BattleDto.getCounters().get(1).getQuantity());

        MatchSnapshotDTO.HeroSnapshotDTO oppBenchFromP2 = p2View.getOpponentPlayer().getHeroes().stream()
                .filter(h -> h.getUuid().equals(heroP1Bench.getUuid()))
                .findFirst().orElseThrow();
        assertEquals(ZoneId.BENCH.name(), oppBenchFromP2.getZoneId());
        assertEquals(1, oppBenchFromP2.getCounters().size());
        assertEquals("SEDA", oppBenchFromP2.getCounters().get(0).getCounterType());
        assertEquals(4, oppBenchFromP2.getCounters().get(0).getQuantity());

        assertEquals(1, p1View.getLocalPlayer().getPlayedCardsInResolve().get(0).getCounters().size());
        assertTrue(p2View.getOpponentPlayer().getPlayedCardsInResolve().get(0).getCounters().isEmpty());
    }

    @Test
    @DisplayName("3. ModifyHeroCounterEffect via ResolutionQueue gera snapshot com contadores atualizados")
    void shouldApplyCountersThroughResolutionQueueAndEmitSnapshot() {
        Card spell = new Card();
        spell.setUuid(UUID.randomUUID());
        spell.setOwnerId(player1.getUuid());
        spell.setCardName("Tecelagem Ígnea");
        spell.setCardType(CardType.SPELL);
        spell.setColor(Colors.RED);
        spell.setZoneId(ZoneId.RESOLVE);
        spell.setEffects(new ArrayList<>(List.of(
                new ModifyHeroCounterEffect(TurnPhase.RESOLUTION, ModifyHeroCounterEffect.CounterTargetScope.ACTIVE_ENEMY_HERO, CounterType.COMBUSTAO, 2),
                new ModifyHeroCounterEffect(TurnPhase.RESOLUTION, ModifyHeroCounterEffect.CounterTargetScope.ACTIVE_ALLY_HERO, CounterType.SEDA, 3)
        )));

        engine.setTurnPhase(TurnPhase.RESOLUTION);
        int snapshotsBefore = engine.getSnapshotHistory().size();

        spell.onPhaseChange(TurnPhase.RESOLUTION, engine.getResolutionQueue());
        engine.getResolutionQueue().resolveAll(engine);

        assertEquals(2, engine.getCounterCount(heroP2Battle, CounterType.COMBUSTAO));
        assertEquals(3, engine.getCounterCount(heroP1Battle, CounterType.SEDA));
        assertTrue(engine.getSnapshotHistory().size() > snapshotsBefore);

        MatchSnapshotDTO latest = engine.getSnapshotHistory().get(engine.getSnapshotHistory().size() - 1);
        MatchSnapshotDTO.HeroSnapshotDTO p2Dto = latest.getOpponentPlayer().getHeroes().get(0);
        assertEquals(1, p2Dto.getCounters().size());
        assertEquals("COMBUSTAO", p2Dto.getCounters().get(0).getCounterType());
        assertEquals(2, p2Dto.getCounters().get(0).getQuantity());
    }

    @Test
    @DisplayName("4. Derrota do Herói em COMBAT_END e desanexo limpam contadores; Evolução preserva contadores")
    void shouldClearCountersOnHeroDeathAndDetachWhilePreservingOnEvolution() {
        engine.addCounter(heroP1Battle, CounterType.SEDA, 4);
        engine.addCounter(heroP2Battle, CounterType.COMBUSTAO, 3);

        Card attachment = new Card();
        attachment.setUuid(UUID.randomUUID());
        attachment.setOwnerId(player1.getUuid());
        attachment.setCardName("Amuleto Tecido");
        attachment.setCardType(CardType.ITEM);
        attachment.setZoneId(ZoneId.ATTACHED);
        attachment.setAttachedToCardId(heroP1Battle.getUuid());
        engine.addCounter(attachment, CounterType.SEDA, 2);
        player1.getCurrentDeck().getCardList().add(attachment);

        HeroCard evolvedTemplate = createHero(player1.getUuid(), "Cavaleiro Onear, Ascendido", 5, 7, 4, 20);
        engine.evolveHero(player1, heroP1Battle.getUuid(), evolvedTemplate);
        assertEquals(4, engine.getCounterCount(heroP1Battle, CounterType.SEDA));

        assertEquals(3, engine.consumeAllCounters(heroP2Battle, CounterType.COMBUSTAO));
        assertEquals(0, engine.getCounterCount(heroP2Battle, CounterType.COMBUSTAO));

        engine.addCounter(heroP2Battle, CounterType.SEDA, 2);
        heroP2Battle.setVidaAtual(0);
        engine.setTurnPhase(TurnPhase.COMBAT_END);
        engine.combatEnd();

        assertFalse(Boolean.TRUE.equals(heroP2Battle.getEstaVivo()));
        assertEquals(0, heroP2Battle.getTotalCounters());
        assertEquals(4, engine.getCounterCount(heroP1Battle, CounterType.SEDA));

        engine.detachCard(attachment, ZoneId.DISCARD);
        assertEquals(0, attachment.getTotalCounters());
    }

    @Test
    @DisplayName("5. CounterType mantém apenas COMBUSTAO e SEDA (sem Pólvora, Veneno ou Poeira)")
    void shouldResolveOnlyCombustaoAndSedaCounterTypes() {
        assertEquals(Set.of(CounterType.COMBUSTAO, CounterType.SEDA), Set.of(CounterType.values()));
        assertEquals(CounterType.COMBUSTAO, CounterType.fromKeyOrLabel("Combustão"));
        assertEquals(CounterType.COMBUSTAO, CounterType.fromKeyOrLabel("combustao"));
        assertEquals(CounterType.COMBUSTAO, CounterType.fromKeyOrLabel("counter_combustao"));
        assertEquals(CounterType.SEDA, CounterType.fromKeyOrLabel("Seda"));
        assertEquals(CounterType.SEDA, CounterType.fromKeyOrLabel("seda"));
        assertEquals(CounterType.SEDA, CounterType.fromKeyOrLabel("counter_seda"));

        assertNull(CounterType.fromKeyOrLabel("polvora"));
        assertNull(CounterType.fromKeyOrLabel("Pólvora"));
        assertNull(CounterType.fromKeyOrLabel("veneno"));
        assertNull(CounterType.fromKeyOrLabel("poeira"));
        assertNull(CounterType.fromKeyOrLabel("desconhecido"));
    }

    @Test
    @DisplayName("6. Resíduo Incendiário adiciona 5 Combustão; APENAS Magias de Fogo somam e zeram os contadores")
    void shouldAddFiveCombustionFromResiduoIncendiarioAndTriggerOnlyOnFireSpells() {
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

        engine.setTurnPhase(TurnPhase.RESOLUTION);
        residuo.onPhaseChange(TurnPhase.RESOLUTION, engine.getResolutionQueue());
        engine.getResolutionQueue().resolveAll(engine);

        assertEquals(5, engine.getCounterCount(heroP2Battle, CounterType.COMBUSTAO));
        assertTrue(heroP2Battle.hasCounter(CounterType.COMBUSTAO));

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

        assertEquals(hpAntesCristal - 2, heroP2Battle.getVidaAtual());
        assertEquals(5, engine.getCounterCount(heroP2Battle, CounterType.COMBUSTAO));

        heroP1Battle.setCardType(CardType.HERO);
        heroP1Battle.setAfinidades(new ArrayList<>(List.of(Afinidade.FOGO)));
        heroP1Battle.setEffects(new ArrayList<>(List.of(new EndOfCombatMagicDamageEffect(3))));
        int hpAntesHeroFire = heroP2Battle.getVidaAtual(); // 13
        engine.setTurnPhase(TurnPhase.COMBAT_END);
        heroP1Battle.onPhaseChange(TurnPhase.COMBAT_END, engine.getResolutionQueue());
        engine.getResolutionQueue().resolveAll(engine);

        assertEquals(hpAntesHeroFire - 1, heroP2Battle.getVidaAtual());
        assertEquals(5, engine.getCounterCount(heroP2Battle, CounterType.COMBUSTAO));

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

        assertEquals(hpAntesBolaDeFogo - 10, heroP2Battle.getVidaAtual());
        assertEquals(0, engine.getCounterCount(heroP2Battle, CounterType.COMBUSTAO));
        assertFalse(heroP2Battle.getCounters().containsKey(CounterType.COMBUSTAO));
    }

    @Test
    @DisplayName("7. BOTH_ACTIVE_HEROES e múltiplos escopos anexam tokens simultaneamente no Herói atual aliado e no Herói inimigo atual")
    void shouldAttachCountersToBothCurrentPlayerHeroAndCurrentEnemyHeroSimultaneously() {
        HeroCard heroP2Bench = createHero(player2.getUuid(), "Sentinela no Banco", 1, 3, 2, 12);
        heroP2Bench.setZoneId(ZoneId.BENCH);
        player2.getCurrentDeck().getHeroList().add(heroP2Bench);
        heroP1Bench.setZoneId(ZoneId.BENCH);

        Card dualSpell = new Card();
        dualSpell.setUuid(UUID.randomUUID());
        dualSpell.setOwnerId(player1.getUuid());
        dualSpell.setCardName("Convergência de Seda e Chamas");
        dualSpell.setCardType(CardType.SPELL);
        dualSpell.setColor(Colors.BLUE);
        dualSpell.setZoneId(ZoneId.RESOLVE);
        dualSpell.setEffects(new ArrayList<>(List.of(
                new ModifyHeroCounterEffect(
                        TurnPhase.RESOLUTION,
                        ModifyHeroCounterEffect.CounterTargetScope.BOTH_ACTIVE_HEROES,
                        CounterType.SEDA,
                        3
                ),
                new ModifyHeroCounterEffect(
                        TurnPhase.RESOLUTION,
                        List.of(
                                ModifyHeroCounterEffect.CounterTargetScope.ACTIVE_ALLY_HERO,
                                ModifyHeroCounterEffect.CounterTargetScope.ACTIVE_ENEMY_HERO
                        ),
                        CounterType.COMBUSTAO,
                        2
                )
        )));

        engine.setTurnPhase(TurnPhase.RESOLUTION);

        dualSpell.onPhaseChange(TurnPhase.RESOLUTION, engine.getResolutionQueue());
        engine.getResolutionQueue().resolveAll(engine);

        assertEquals(3, engine.getCounterCount(heroP1Battle, CounterType.SEDA));
        assertEquals(3, engine.getCounterCount(heroP2Battle, CounterType.SEDA));
        assertEquals(2, engine.getCounterCount(heroP1Battle, CounterType.COMBUSTAO));
        assertEquals(2, engine.getCounterCount(heroP2Battle, CounterType.COMBUSTAO));

        assertEquals(0, engine.getCounterCount(heroP1Bench, CounterType.SEDA));
        assertEquals(0, engine.getCounterCount(heroP2Bench, CounterType.SEDA));
    }

    @Test
    @DisplayName("8. Escopos diretos de fora de combate atingem apenas Heróis vivos na bancada (BENCH)")
    void shouldAttachCountersDirectlyToOutOfCombatHeroesOnly() {
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
        benchAction.setCardName("Fios de Retaguarda");
        benchAction.setCardType(CardType.ACTION);
        benchAction.setZoneId(ZoneId.RESOLVE);
        benchAction.setEffects(new ArrayList<>(List.of(
                new ModifyHeroCounterEffect(
                        TurnPhase.RESOLUTION,
                        ModifyHeroCounterEffect.CounterTargetScope.OUT_OF_COMBAT_ALLY_HEROES,
                        CounterType.SEDA,
                        2
                ),
                new ModifyHeroCounterEffect(
                        TurnPhase.RESOLUTION,
                        ModifyHeroCounterEffect.CounterTargetScope.OUT_OF_COMBAT_ENEMY_HEROES,
                        CounterType.COMBUSTAO,
                        3
                )
        )));

        engine.setTurnPhase(TurnPhase.RESOLUTION);
        benchAction.onPhaseChange(TurnPhase.RESOLUTION, engine.getResolutionQueue());
        engine.getResolutionQueue().resolveAll(engine);

        assertEquals(0, heroP1Battle.getTotalCounters());
        assertEquals(0, heroP2Battle.getTotalCounters());
        assertEquals(0, heroP1BenchDead.getTotalCounters());

        assertEquals(2, engine.getCounterCount(heroP1Bench, CounterType.SEDA));
        assertEquals(0, engine.getCounterCount(heroP1Bench, CounterType.COMBUSTAO));

        assertEquals(3, engine.getCounterCount(heroP2Bench1, CounterType.COMBUSTAO));
        assertEquals(3, engine.getCounterCount(heroP2Bench2, CounterType.COMBUSTAO));
    }

    @Test
    @DisplayName("9. Escolha interativa via PendingChoice anexa tokens em Herói fora de combate escolhido")
    void shouldAllowInteractivePendingChoiceTargetingOutOfCombatHeroes() {
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
                        CounterType.COMBUSTAO,
                        4
                )
        )));

        engine.setTurnPhase(TurnPhase.RESOLUTION);
        targetedSpell.onPhaseChange(TurnPhase.RESOLUTION, engine.getResolutionQueue());
        engine.getResolutionQueue().resolveAll(engine);

        assertTrue(engine.isWaitingForPlayerChoice());
        MatchSnapshotDTO.PendingChoiceDTO pending = engine.getGameState().getPendingChoice();
        assertEquals(player1.getUuid(), pending.getChoosingPlayerId());
        assertEquals("ENEMY_HERO", pending.getTargetType());
        assertEquals(2, pending.getValidTargetUuids().size());
        assertFalse(pending.getValidTargetUuids().contains(heroP2Battle.getUuid()));

        engine.submitPlayerChoice(player1.getUuid(), heroP2Bench2.getUuid());

        assertFalse(engine.isWaitingForPlayerChoice());
        assertEquals(4, engine.getCounterCount(heroP2Bench2, CounterType.COMBUSTAO));
        assertEquals(0, engine.getCounterCount(heroP2Bench1, CounterType.COMBUSTAO));
        assertEquals(0, engine.getCounterCount(heroP2Battle, CounterType.COMBUSTAO));
    }

    @Test
    @DisplayName("10. Heróis e cartas (RESOLVE + ATTACHED) geram tokens em TODAS as 8 etapas do turno, incluindo Hero em RESOLUTION com 0 ou várias cartas")
    void shouldGenerateTokensAcrossAllTurnPhasesByBothHeroesAndCardsWithoutDoubleFiring() {
        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        p1.setName("P1 Completo");
        Player p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        p2.setName("P2 Completo");

        HeroCard p1Main = createHero(p1.getUuid(), "Tecelão de Turnos", 3, 4, 2, 25);
        HeroCard p1Bench = createHero(p1.getUuid(), "Suporte de Banco", 2, 3, 2, 16);
        HeroCard p2Main = createHero(p2.getUuid(), "Oponente Principal", 2, 3, 2, 25);
        HeroCard p2Bench = createHero(p2.getUuid(), "Oponente Banco", 1, 2, 2, 16);

        // Herói principal gera tokens nas 8 fases, INCLUSIVE TurnPhase.RESOLUTION (1x por fase mesmo se 2 cartas forem jogadas!)
        p1Main.setEffects(new ArrayList<>(List.of(
                new ModifyHeroCounterEffect(TurnPhase.GAME_START, ModifyHeroCounterEffect.CounterTargetScope.CHOOSE_ENEMY_HERO, CounterType.COMBUSTAO, 1, true),
                new ModifyHeroCounterEffect(TurnPhase.CHOICE, ModifyHeroCounterEffect.CounterTargetScope.ALL_LIVING_HEROES, CounterType.SEDA, 1, true),
                new ModifyHeroCounterEffect(TurnPhase.PREPARATION, ModifyHeroCounterEffect.CounterTargetScope.BOTH_ACTIVE_HEROES, CounterType.SEDA, 1),
                new ModifyHeroCounterEffect(TurnPhase.COMBAT_START, ModifyHeroCounterEffect.CounterTargetScope.ACTIVE_ENEMY_HERO, CounterType.COMBUSTAO, 1),
                new ModifyHeroCounterEffect(TurnPhase.RESOLUTION, ModifyHeroCounterEffect.CounterTargetScope.ACTIVE_ALLY_HERO, CounterType.SEDA, 1),
                new ModifyHeroCounterEffect(TurnPhase.COMBAT, ModifyHeroCounterEffect.CounterTargetScope.CHOOSE_OUT_OF_COMBAT_ENEMY_HERO, CounterType.COMBUSTAO, 2),
                new ModifyHeroCounterEffect(TurnPhase.COMBAT_END, ModifyHeroCounterEffect.CounterTargetScope.OUT_OF_COMBAT_ALLY_HEROES, CounterType.SEDA, 1),
                new ModifyHeroCounterEffect(TurnPhase.DISCARD, ModifyHeroCounterEffect.CounterTargetScope.ALL_ALLY_HEROES, CounterType.SEDA, 1, true)
        )));

        // p1Bench também possui efeito em RESOLUTION (para validar Turno 2 com 0 cartas jogadas usando outro Herói ativo)
        p1Bench.setEffects(new ArrayList<>(List.of(
                new ModifyHeroCounterEffect(TurnPhase.RESOLUTION, ModifyHeroCounterEffect.CounterTargetScope.SELF_OR_ATTACHED_HERO, CounterType.SEDA, 1)
        )));

        // Carta já anexada (ATTACHED) gera SEDA em RESOLUTION e COMBAT_END (funciona por carta anexada em qualquer fase!)
        Card attachedSpool = new Card();
        attachedSpool.setUuid(UUID.randomUUID());
        attachedSpool.setOwnerId(p1.getUuid());
        attachedSpool.setCardName("Carretel Anexado");
        attachedSpool.setCardType(CardType.ITEM);
        attachedSpool.setZoneId(ZoneId.ATTACHED);
        attachedSpool.setAttachedToCardId(p1Bench.getUuid());
        attachedSpool.setEffects(new ArrayList<>(List.of(
                new ModifyHeroCounterEffect(TurnPhase.RESOLUTION, ModifyHeroCounterEffect.CounterTargetScope.SELF_OR_ATTACHED_HERO, CounterType.SEDA, 1, true),
                new ModifyHeroCounterEffect(TurnPhase.COMBAT_END, ModifyHeroCounterEffect.CounterTargetScope.SELF_OR_ATTACHED_HERO, CounterType.SEDA, 1, true)
        )));

        // Cartas jogadas em PREPARAÇÃO -> RESOLVE gerando em RESOLUTION e COMBAT_END (via canTriggerFromResolveZone)
        Card resolutionSpell1 = new Card();
        resolutionSpell1.setUuid(UUID.randomUUID());
        resolutionSpell1.setOwnerId(p1.getUuid());
        resolutionSpell1.setCardName("Fio Duplo");
        resolutionSpell1.setCardType(CardType.ACTION);
        resolutionSpell1.setColor(Colors.NEUTRAL);
        resolutionSpell1.setZoneId(ZoneId.HAND);
        resolutionSpell1.setEffects(new ArrayList<>(List.of(
                new ModifyHeroCounterEffect(TurnPhase.RESOLUTION, ModifyHeroCounterEffect.CounterTargetScope.OUT_OF_COMBAT_ALLY_HEROES, CounterType.SEDA, 2),
                new ModifyHeroCounterEffect(TurnPhase.COMBAT_END, ModifyHeroCounterEffect.CounterTargetScope.ACTIVE_ALLY_HERO, CounterType.SEDA, 1)
        )));

        Card resolutionSpell2 = new Card();
        resolutionSpell2.setUuid(UUID.randomUUID());
        resolutionSpell2.setOwnerId(p1.getUuid());
        resolutionSpell2.setCardName("Centelha Auxiliar");
        resolutionSpell2.setCardType(CardType.ACTION);
        resolutionSpell2.setColor(Colors.NEUTRAL);
        resolutionSpell2.setZoneId(ZoneId.HAND);
        resolutionSpell2.setEffects(new ArrayList<>(List.of(
                new ModifyHeroCounterEffect(TurnPhase.RESOLUTION, ModifyHeroCounterEffect.CounterTargetScope.ACTIVE_ENEMY_HERO, CounterType.COMBUSTAO, 1)
        )));

        List<Card> p1Cards = new ArrayList<>();
        p1Cards.add(attachedSpool);
        p1Cards.add(resolutionSpell1);
        p1Cards.add(resolutionSpell2);
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
        fullEngine.startGame();
        assertEquals(TurnPhase.GAME_START, fullEngine.getTurnPhase());
        assertTrue(fullEngine.isWaitingForPlayerChoice());

        // Completa GAME_START -> entra em CHOICE
        fullEngine.submitPlayerChoice(p1.getUuid(), p2Main.getUuid());
        assertEquals(TurnPhase.CHOICE, fullEngine.getTurnPhase());
        assertEquals(1, fullEngine.getCounterCount(p2Main, CounterType.COMBUSTAO));
        assertEquals(1, fullEngine.getCounterCount(p1Main, CounterType.SEDA));
        assertEquals(1, fullEngine.getCounterCount(p1Bench, CounterType.SEDA));

        fullEngine.setActivePlayerId(p1.getUuid());
        fullEngine.getGameState().setActiveTurnPlayerId(p1.getUuid());
        fullEngine.submitHeroChoice(p1.getUuid(), p1Main.getUuid(), p2Main.getUuid());

        assertEquals(TurnPhase.PREPARATION, fullEngine.getTurnPhase());
        assertEquals(2, fullEngine.getCounterCount(p1Main, CounterType.SEDA), "CHOICE (+1) + PREPARATION (+1) = 2 SEDA");
        assertEquals(2, fullEngine.getCounterCount(p2Main, CounterType.SEDA), "CHOICE (+1) + PREPARATION (+1) = 2 SEDA");

        // Joga 2 cartas em PREPARATION (também valida que p1Main em RESOLUTION dispara apenas 1x, não 2x!)
        fullEngine.submitPreparationCards(p1.getUuid(), List.of(resolutionSpell1.getUuid(), resolutionSpell2.getUuid()));
        fullEngine.submitPreparationCards(p2.getUuid(), List.of());

        assertEquals(TurnPhase.COMBAT, fullEngine.getTurnPhase());
        assertTrue(fullEngine.isWaitingForPlayerChoice());
        // p1Main: CHOICE (+1) + PREPARATION (+1) + RESOLUTION do Herói (+1, exatamente 1x mesmo com 2 cartas!) = 3 SEDA
        assertEquals(3, fullEngine.getCounterCount(p1Main, CounterType.SEDA));
        // p1Bench: CHOICE (+1) + attachedSpool RESOLUTION (+1) + resolutionSpell1 RESOLUTION (+2) = 4 SEDA
        assertEquals(4, fullEngine.getCounterCount(p1Bench, CounterType.SEDA));
        // p2Main: GAME_START (+1) + COMBAT_START (+1) + resolutionSpell2 (+1) = 3 COMBUSTAO
        assertEquals(3, fullEngine.getCounterCount(p2Main, CounterType.COMBUSTAO));

        // Conclui PendingChoice de COMBAT (p2Bench +2 COMBUSTAO) -> roda COMBAT_END e DISCARD
        fullEngine.submitPlayerChoice(p1.getUuid(), p2Bench.getUuid());
        assertEquals(TurnPhase.DISCARD, fullEngine.getTurnPhase());
        assertEquals(2, fullEngine.getCounterCount(p2Bench, CounterType.COMBUSTAO));
        // p1Main: 3 + resolutionSpell1 COMBAT_END (+1) + p1Main DISCARD (+1) = 5 SEDA
        assertEquals(5, fullEngine.getCounterCount(p1Main, CounterType.SEDA));
        // p1Bench: 4 + p1Main COMBAT_END (+1) + attachedSpool COMBAT_END (+1) + p1Main DISCARD (+1) = 7 SEDA
        assertEquals(7, fullEngine.getCounterCount(p1Bench, CounterType.SEDA));

        // Agora valida Turno 2 com 0 cartas jogadas (p1Bench ativo em BATTLE): Herói + carta anexada ainda devem disparar em RESOLUTION!
        fullEngine.submitDiscard(p1.getUuid(), List.of());
        fullEngine.submitDiscard(p2.getUuid(), List.of());
        assertEquals(TurnPhase.CHOICE, fullEngine.getTurnPhase());
        fullEngine.setActivePlayerId(p1.getUuid());
        fullEngine.getGameState().setActiveTurnPlayerId(p1.getUuid());
        fullEngine.submitHeroChoice(p1.getUuid(), p1Bench.getUuid(), p2Main.getUuid());

        int sedaBenchAntesCombat = fullEngine.getCounterCount(p1Bench, CounterType.SEDA);
        fullEngine.submitPreparationCards(p1.getUuid(), List.of());
        fullEngine.submitPreparationCards(p2.getUuid(), List.of());

        assertEquals(TurnPhase.DISCARD, fullEngine.getTurnPhase());
        int resolutionSedaSnapshotQty = fullEngine.getSnapshotHistory().stream()
                .filter(s -> s.getTurnPhase() == TurnPhase.RESOLUTION)
                .reduce((first, second) -> second)
                .orElseThrow()
                .getLocalPlayer().getHeroes().stream()
                .filter(h -> h.getUuid().equals(p1Bench.getUuid()))
                .findFirst().orElseThrow()
                .getCounters().stream()
                .filter(c -> "SEDA".equals(c.getCounterType()))
                .mapToInt(MatchSnapshotDTO.CardCounterDTO::getQuantity)
                .sum();
        assertEquals(sedaBenchAntesCombat + 2, resolutionSedaSnapshotQty,
                "Herói ativo (p1Bench +1) e carta anexada (+1) com efeito em RESOLUTION devem gerar tokens mesmo com 0 cartas jogadas.");
        assertEquals(sedaBenchAntesCombat + 4, fullEngine.getCounterCount(p1Bench, CounterType.SEDA),
                "Soma RESOLUTION (+2), attachedSpool COMBAT_END (+1) e p1Main DISCARD (+1) = +4 SEDA.");
    }

    @Test
    @DisplayName("11. SEDA amplia bônus de Defesa, Ataque, Espírito, Cura e Dano de cartas que consomem esses tokens")
    void shouldConsumeSedaToAmplifyDefenseAttackSpiritHealAndDamageEffects() {
        // Caso A: Defesa temporária aumentada por consumo de TODA a SEDA (+2 base DEF + 1 DEF por SEDA até COMBAT_END)
        engine.addCounter(heroP1Battle, CounterType.SEDA, 3);
        Card mantoDeSeda = new Card();
        mantoDeSeda.setUuid(UUID.randomUUID());
        mantoDeSeda.setOwnerId(player1.getUuid());
        mantoDeSeda.setCardName("Manto de Seda");
        mantoDeSeda.setCardType(CardType.SPELL);
        mantoDeSeda.setZoneId(ZoneId.RESOLVE);
        mantoDeSeda.setEffects(new ArrayList<>(List.of(
                new ConsumeCounterAndApplyEffect(
                        TurnPhase.RESOLUTION,
                        ModifyHeroCounterEffect.CounterTargetScope.ACTIVE_ALLY_HERO,
                        CounterType.SEDA,
                        CounterConsumptionMode.CONSUME_ALL,
                        0,
                        false,
                        ConsumeCounterAndApplyEffect.statBuffAction(
                                ConsumeCounterAndApplyEffect.StatBonusSpec.ofStats(
                                        0, 0,
                                        0, 0,
                                        2, 1,
                                        true
                                )
                        )
                )
        )));

        int defInicial = heroP1Battle.getDefesa(); // 2
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        mantoDeSeda.onPhaseChange(TurnPhase.RESOLUTION, engine.getResolutionQueue());
        engine.getResolutionQueue().resolveAll(engine);

        assertEquals(0, engine.getCounterCount(heroP1Battle, CounterType.SEDA));
        assertEquals(defInicial + 5, heroP1Battle.getDefesa(), "+2 base + 3 por SEDA = +5 Defesa até COMBAT_END");

        // No COMBAT_END o bônus temporário expira normalmente
        engine.setTurnPhase(TurnPhase.COMBAT_END);
        engine.combatEnd();
        assertEquals(defInicial, heroP1Battle.getDefesa(), "Bônus temporário ampliado por SEDA deve reverter no COMBAT_END.");

        // Caso B: Consumo parcial (CONSUME_UP_TO 2 de 4) ampliando Ataque permanente (+2 ATK por Seda consumida)
        engine.setTurnPhase(TurnPhase.CHOICE);
        engine.getGameState().setTurnPhase(TurnPhase.CHOICE);
        engine.setActivePlayerId(player1.getUuid());
        engine.getGameState().setActiveTurnPlayerId(player1.getUuid());
        heroP1Battle.setEstaAtivo(true);
        heroP1Battle.setZoneId(ZoneId.BENCH);
        heroP2Battle.setZoneId(ZoneId.BENCH);
        engine.submitHeroChoice(player1.getUuid(), heroP1Battle.getUuid(), heroP2Battle.getUuid());

        engine.addCounter(heroP1Battle, CounterType.SEDA, 4);
        int atkInicial = heroP1Battle.getAtaque(); // 5
        Card laminaTecida = new Card();
        laminaTecida.setUuid(UUID.randomUUID());
        laminaTecida.setOwnerId(player1.getUuid());
        laminaTecida.setCardName("Lâmina Tecida");
        laminaTecida.setCardType(CardType.ACTION);
        laminaTecida.setZoneId(ZoneId.RESOLVE);
        laminaTecida.setEffects(new ArrayList<>(List.of(
                new ConsumeCounterAndApplyEffect(
                        TurnPhase.RESOLUTION,
                        ModifyHeroCounterEffect.CounterTargetScope.ACTIVE_ALLY_HERO,
                        CounterType.SEDA,
                        CounterConsumptionMode.CONSUME_UP_TO,
                        2,
                        true,
                        ConsumeCounterAndApplyEffect.statBuffAction(
                                ConsumeCounterAndApplyEffect.StatBonusSpec.ofStats(
                                        0, 0,
                                        0, 2,
                                        0, 0,
                                        false
                                )
                        )
                )
        )));

        engine.setTurnPhase(TurnPhase.RESOLUTION);
        laminaTecida.onPhaseChange(TurnPhase.RESOLUTION, engine.getResolutionQueue());
        engine.getResolutionQueue().resolveAll(engine);

        assertEquals(2, engine.getCounterCount(heroP1Battle, CounterType.SEDA), "Deve consumir apenas até 2 dos 4 marcadores.");
        assertEquals(atkInicial + 4, heroP1Battle.getAtaque(), "+2 ATK x 2 Seda = +4 Ataque permanente");

        // Caso C: Custo exato (CONSUME_EXACT 3): não consome quando há apenas 2; ativa Cura + Dano Mágico quando há 3
        heroP1Battle.setVidaAtual(10); // max 15
        int hpP2Antes = heroP2Battle.getVidaAtual(); // 15
        Card ritualDaSeda = new Card();
        ritualDaSeda.setUuid(UUID.randomUUID());
        ritualDaSeda.setOwnerId(player1.getUuid());
        ritualDaSeda.setCardName("Ritual da Seda");
        ritualDaSeda.setCardType(CardType.SPELL);
        ritualDaSeda.setZoneId(ZoneId.RESOLVE);
        ritualDaSeda.setEffects(new ArrayList<>(List.of(
                new ConsumeCounterAndApplyEffect(
                        List.of(TurnPhase.RESOLUTION),
                        List.of(ModifyHeroCounterEffect.CounterTargetScope.ACTIVE_ALLY_HERO),
                        CounterType.SEDA,
                        CounterConsumptionMode.CONSUME_EXACT,
                        3,
                        true,
                        false,
                        false,
                        true,
                        List.of(
                                ConsumeCounterAndApplyEffect.healAction(1, 1, null),
                                ConsumeCounterAndApplyEffect.damageAction(
                                        ConsumeCounterAndApplyEffect.DamageKind.MAGIC,
                                        2,
                                        1,
                                        false,
                                        ModifyHeroCounterEffect.CounterTargetScope.ACTIVE_ENEMY_HERO
                                )
                        )
                )
        )));

        ritualDaSeda.onPhaseChange(TurnPhase.RESOLUTION, engine.getResolutionQueue());
        engine.getResolutionQueue().resolveAll(engine);
        assertEquals(2, engine.getCounterCount(heroP1Battle, CounterType.SEDA), "Sem saldo suficiente (2 < 3), não deve gastar Seda.");
        assertEquals(10, heroP1Battle.getVidaAtual());
        assertEquals(hpP2Antes, heroP2Battle.getVidaAtual());

        // Adiciona +1 SEDA (total = 3) e reexecuta
        engine.addCounter(heroP1Battle, CounterType.SEDA, 1);
        ritualDaSeda.onPhaseChange(TurnPhase.RESOLUTION, engine.getResolutionQueue());
        engine.getResolutionQueue().resolveAll(engine);

        assertEquals(0, engine.getCounterCount(heroP1Battle, CounterType.SEDA));
        assertEquals(14, heroP1Battle.getVidaAtual(), "Cura 1 + 1*3 = +4 HP (10 -> 14).");
        // Dano Mágico bruto = 2 + 1*3 = 5; mitigado por 2 Espírito de P2 = 3 dano
        assertEquals(hpP2Antes - 3, heroP2Battle.getVidaAtual());
    }

    @Test
    @DisplayName("12. CounterRuleRegistry permite plugar regra de novo token sem alterar cálculos do GameEngine")
    void shouldAllowDecoupledCounterRuleExtensionInRegistry() {
        engine.registerCounterRule(new CounterRule() {
            @Override
            public CounterType getCounterType() {
                return CounterType.SEDA;
            }

            @Override
            public int modifyPhysicalDamage(GameEngine eng, HeroCard attacker, HeroCard defender, int baseDamage, Card sourceCard) {
                return eng.getCounterCount(attacker, CounterType.SEDA);
            }
        });

        engine.addCounter(heroP1Battle, CounterType.SEDA, 3);
        // base 5 + 3 SEDA = 8; defesa 2 => 6 dano físico
        int dmg = engine.calculatePhysicalDamage(heroP1Battle, heroP2Battle, 5, null);
        assertEquals(6, dmg);
    }
}

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
}

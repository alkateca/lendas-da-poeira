package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.effects.combat.MagicDamageEffect;
import com.alkateca.lendasdapoeira.effects.combat.PlayActionPhysicalDamageEffect;
import com.alkateca.lendasdapoeira.effects.combat.PlaySpellBuffEffect;
import com.alkateca.lendasdapoeira.effects.reaction.ReactionCancelTriggeringCardEffect;
import com.alkateca.lendasdapoeira.effects.reaction.ReactionTriggerType;
import com.alkateca.lendasdapoeira.effects.reaction.TriggeredReactionEffect;
import com.alkateca.lendasdapoeira.effects.startOfCombat.StartOfCombatAlliesBuffAttackEffect;
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

class ImmediateAlternatingResolutionAndReactionTest {

    private Player player1;
    private Player player2;
    private HeroCard heroP1;
    private HeroCard heroP2;
    private GameEngine engine;

    @BeforeEach
    void setUp() {
        player1 = new Player();
        player1.setUuid(UUID.randomUUID());
        player1.setName("Jogador 1");

        player2 = new Player();
        player2.setUuid(UUID.randomUUID());
        player2.setName("Jogador 2");

        heroP1 = createHero(player1.getUuid(), "Mago da Poeira P1", Colors.RED, 2, 3, 1, 25);
        heroP2 = createHero(player2.getUuid(), "Sentinela da Poeira P2", Colors.BLUE, 2, 3, 1, 25);

        player1.setCurrentDeck(new Deck("Deck P1", new ArrayList<>(), new ArrayList<>(List.of(heroP1))));
        player2.setCurrentDeck(new Deck("Deck P2", new ArrayList<>(), new ArrayList<>(List.of(heroP2))));

        engine = new GameEngine(player1, player2);
        engine.setTurnPhase(TurnPhase.CHOICE);
        engine.getGameState().setTurnPhase(TurnPhase.CHOICE);
        engine.setActivePlayerId(player1.getUuid());
        engine.getGameState().setActiveTurnPlayerId(player1.getUuid());
    }

    private HeroCard createHero(UUID ownerId, String name, Colors color, int esp, int atk, int def, int hp) {
        HeroCard hero = new HeroCard();
        hero.setUuid(UUID.randomUUID());
        hero.setOwnerId(ownerId);
        hero.setCardName(name);
        hero.setCardType(CardType.HERO);
        hero.setColor(color);
        hero.setEspirito(esp);
        hero.setAtaque(atk);
        hero.setDefesa(def);
        hero.setVidaMaxima(hp);
        hero.setVidaAtual(hp);
        hero.setMaxWeaponSlots(2);
        hero.setClasse(new ArrayList<>(List.of(Classe.EMISSOR, Classe.TRANSFORMADOR)));
        hero.setAfinidades(new ArrayList<>(List.of(Afinidade.FOGO, Afinidade.AGUA)));
        hero.setRacas(new ArrayList<>());
        hero.setOrdem(new ArrayList<>());
        hero.setEffects(new ArrayList<>());
        hero.setEstaVivo(true);
        hero.setEstaAtivo(true);
        hero.setZoneId(ZoneId.BENCH);
        return hero;
    }

    private Card createHandCard(UUID ownerId, String name, CardType type, Colors color, com.alkateca.lendasdapoeira.effects.Effect... effects) {
        Card card = new Card();
        card.setUuid(UUID.randomUUID());
        card.setOwnerId(ownerId);
        card.setCardName(name);
        card.setCardType(type);
        card.setColor(color);
        card.setItemType(ItemType.NENHUM);
        card.setZoneId(ZoneId.HAND);
        card.setEffects(new ArrayList<>(List.of(effects)));
        return card;
    }

    @Test
    @DisplayName("1. PREPARATION existe apenas antes da 1ª carta; jogar cartas alterna imediatamente na RESOLUTION no limite de 2 por jogador")
    void shouldTransitionFromPreparationToImmediateAlternatingResolutionUpToTwoCardsEach() {
        heroP1.getEffects().add(new StartOfCombatAlliesBuffAttackEffect(2)); // +2 ATQ no COMBAT_START (3 -> 5)

        Card p1Card1 = createHandCard(player1.getUuid(), "Chama Inicial P1", CardType.SPELL, Colors.RED, new MagicDamageEffect(5)); // 5 - 2 ESP = 3 dano
        Card p1Card2 = createHandCard(player1.getUuid(), "Golpe Ardente P1", CardType.ACTION, Colors.RED, new PlayActionPhysicalDamageEffect(4)); // 4 - 1 DEF = 3 dano
        Card p2Card1 = createHandCard(player2.getUuid(), "Jato de Água P2", CardType.SPELL, Colors.BLUE, new MagicDamageEffect(6)); // 6 - 2 ESP = 4 dano
        Card p2Card2 = createHandCard(player2.getUuid(), "Estocada Azul P2", CardType.ACTION, Colors.BLUE, new PlayActionPhysicalDamageEffect(5)); // 5 - 1 DEF = 4 dano

        player1.getCurrentDeck().getCardList().addAll(List.of(p1Card1, p1Card2));
        player2.getCurrentDeck().getCardList().addAll(List.of(p2Card1, p2Card2));

        // 1. Escolha de heróis inicia em PREPARATION com prioridade para quem tem a iniciativa (P1)
        engine.submitHeroChoice(player1.getUuid(), heroP1.getUuid(), heroP2.getUuid());
        assertEquals(TurnPhase.PREPARATION, engine.getTurnPhase());
        assertEquals(player1.getUuid(), engine.getGameState().getEffectiveResolutionActivePlayerId());

        // P2 não pode jogar fora da sua vez
        assertThrows(IllegalStateException.class, () ->
                engine.submitResolutionCard(player2.getUuid(), p2Card1.getUuid())
        );

        // 2. Janela 1 de P1: dispara COMBAT_START uma única vez, entra em RESOLUTION e ativa p1Card1 imediatamente
        engine.submitResolutionCard(player1.getUuid(), p1Card1.getUuid());
        assertEquals(TurnPhase.RESOLUTION, engine.getTurnPhase());
        assertEquals(5, heroP1.getAtaque(), "COMBAT_START deve ter executado antes da 1ª carta.");
        assertEquals(22, heroP2.getVidaAtual(), "A 1ª carta de P1 deve ter resolvido imediatamente (25 - 3 = 22).");
        assertEquals(player2.getUuid(), engine.getGameState().getEffectiveResolutionActivePlayerId());
        assertEquals(1, engine.getRemainingResolutionWindowsForPlayer(player1.getUuid()));
        assertEquals(2, engine.getRemainingResolutionWindowsForPlayer(player2.getUuid()));

        // P1 não pode jogar duas vezes seguidas enquanto é a vez de P2
        assertThrows(IllegalStateException.class, () ->
                engine.submitResolutionCard(player1.getUuid(), p1Card2.getUuid())
        );

        // 3. Janela 1 de P2: ativa p2Card1 imediatamente e devolve prioridade para P1
        engine.submitResolutionCard(player2.getUuid(), p2Card1.getUuid());
        assertEquals(TurnPhase.RESOLUTION, engine.getTurnPhase());
        assertEquals(21, heroP1.getVidaAtual(), "A 1ª carta de P2 deve ter resolvido imediatamente (25 - 4 = 21).");
        assertEquals(player1.getUuid(), engine.getGameState().getEffectiveResolutionActivePlayerId());
        assertEquals(1, engine.getRemainingResolutionWindowsForPlayer(player2.getUuid()));

        // 4. Janela 2 de P1: ativa p1Card2 imediatamente e passa prioridade para a última janela de P2
        engine.submitResolutionCard(player1.getUuid(), p1Card2.getUuid());
        assertEquals(TurnPhase.RESOLUTION, engine.getTurnPhase());
        assertEquals(19, heroP2.getVidaAtual(), "A 2ª carta de P1 deve ter resolvido imediatamente (22 - 3 = 19).");
        assertEquals(0, engine.getRemainingResolutionWindowsForPlayer(player1.getUuid()));
        assertEquals(player2.getUuid(), engine.getGameState().getEffectiveResolutionActivePlayerId());

        // 5. Janela 2 de P2: ativa p2Card2 (21 - 4 = 17) e, como ambos usaram as 2 janelas,
        // avança automaticamente por COMBAT (P1 causa 5-1=4 em P2 -> 15; P2 causa 3-1=2 em P1 -> 15) -> COMBAT_END -> DISCARD!
        engine.submitResolutionCard(player2.getUuid(), p2Card2.getUuid());
        assertEquals(TurnPhase.DISCARD, engine.getTurnPhase());
        assertEquals(15, heroP1.getVidaAtual());
        assertEquals(15, heroP2.getVidaAtual());
    }

    @Test
    @DisplayName("2. Pular janela: jogador que pula a 1ª janela pula apenas aquela carta e ainda pode jogar normalmente na 2ª janela")
    void shouldSkipOnlyCurrentCardWindowWhenPassingAndAllowPlayingInSecondWindow() {
        Card p1SecondWindowCard = createHandCard(player1.getUuid(), "Magia Tardia P1", CardType.SPELL, Colors.RED, new MagicDamageEffect(6)); // 6 - 2 ESP = 4 dano
        Card p2FirstWindowCard = createHandCard(player2.getUuid(), "Magia Rápida P2", CardType.SPELL, Colors.BLUE, new MagicDamageEffect(5)); // 5 - 2 ESP = 3 dano

        player1.getCurrentDeck().getCardList().add(p1SecondWindowCard);
        player2.getCurrentDeck().getCardList().add(p2FirstWindowCard);

        engine.submitHeroChoice(player1.getUuid(), heroP1.getUuid(), heroP2.getUuid());
        assertEquals(TurnPhase.PREPARATION, engine.getTurnPhase());

        // P1 pula sua 1ª janela em PREPARATION -> avança para RESOLUTION e passa a vez para P2
        engine.passResolutionWindow(player1.getUuid());
        assertEquals(TurnPhase.RESOLUTION, engine.getTurnPhase());
        assertEquals(1, engine.getRemainingResolutionWindowsForPlayer(player1.getUuid()), "P1 ainda deve ter 1 janela restante.");
        assertEquals(0, engine.getCardsPlayedByPlayerInCurrentTurn(player1.getUuid()));
        assertEquals(player2.getUuid(), engine.getGameState().getEffectiveResolutionActivePlayerId());

        // P2 joga carta na sua 1ª janela -> resolve imediatamente e devolve a vez para a 2ª janela de P1
        engine.submitResolutionCard(player2.getUuid(), p2FirstWindowCard.getUuid());
        assertEquals(22, heroP1.getVidaAtual());
        assertEquals(player1.getUuid(), engine.getGameState().getEffectiveResolutionActivePlayerId());

        // P1 joga normalmente na sua 2ª janela!
        engine.submitResolutionCard(player1.getUuid(), p1SecondWindowCard.getUuid());
        assertEquals(21, heroP2.getVidaAtual());
        assertEquals(0, engine.getRemainingResolutionWindowsForPlayer(player1.getUuid()));
        assertEquals(1, engine.getCardsPlayedByPlayerInCurrentTurn(player1.getUuid()));
        assertEquals(player2.getUuid(), engine.getGameState().getEffectiveResolutionActivePlayerId());

        // P2 pula sua 2ª janela -> encerra RESOLUTION e executa o combate físico até DISCARD
        engine.passResolutionWindow(player2.getUuid());
        assertEquals(TurnPhase.DISCARD, engine.getTurnPhase());
    }

    @Test
    @DisplayName("3. Cartas de Reação: ativam em reação a efeito específico (LIFO), podem anular a carta inimiga e NÃO gastam o limite de 2 cartas do turno")
    void shouldTriggerReactionWindowResolveLifoAndNotConsumeTwoCardTurnLimit() {
        Card p1HeavySpell = createHandCard(player1.getUuid(), "Meteoro de Poeira", CardType.SPELL, Colors.RED, new MagicDamageEffect(10));
        Card p1SecondAction = createHandCard(player1.getUuid(), "Corte Rápido", CardType.ACTION, Colors.RED, new PlayActionPhysicalDamageEffect(4)); // 4 - 1 DEF = 3 dano

        // Carta de Reação de P2: reage quando o inimigo joga um Feitiço (ON_ENEMY_PLAY_SPELL) e cancela a carta inimiga!
        Card p2CounterReaction = createHandCard(
                player2.getUuid(),
                "Contra-Feitiço de Cristal",
                CardType.REACTION,
                Colors.BLUE,
                new TriggeredReactionEffect(ReactionTriggerType.ON_ENEMY_PLAY_SPELL, new ReactionCancelTriggeringCardEffect())
        );
        Card p2NormalCard1 = createHandCard(player2.getUuid(), "Lança de Gelo 1", CardType.SPELL, Colors.BLUE, new MagicDamageEffect(5)); // 5 - 2 ESP = 3 dano
        Card p2NormalCard2 = createHandCard(player2.getUuid(), "Lança de Gelo 2", CardType.SPELL, Colors.BLUE, new MagicDamageEffect(5)); // 5 - 2 ESP = 3 dano

        player1.getCurrentDeck().getCardList().addAll(List.of(p1HeavySpell, p1SecondAction));
        player2.getCurrentDeck().getCardList().addAll(List.of(p2CounterReaction, p2NormalCard1, p2NormalCard2));

        engine.submitHeroChoice(player1.getUuid(), heroP1.getUuid(), heroP2.getUuid());

        // 1. P1 joga "Meteoro de Poeira" (SPELL) na Janela 1 -> o motor pausa antes de resolver e abre REACTION_WINDOW para P2!
        engine.submitResolutionCard(player1.getUuid(), p1HeavySpell.getUuid());
        assertTrue(engine.isWaitingForPlayerChoice(), "Deve abrir janela de reação para P2 antes do feitiço de P1 resolver.");
        MatchSnapshotDTO.PendingChoiceDTO reactionPrompt = engine.getGameState().getPendingChoice();
        assertNotNull(reactionPrompt);
        assertEquals(player2.getUuid(), reactionPrompt.getChoosingPlayerId());
        assertEquals("REACTION_WINDOW", reactionPrompt.getTargetType());
        assertTrue(reactionPrompt.isCanSkip());
        assertTrue(reactionPrompt.getValidTargetUuids().contains(p2CounterReaction.getUuid()));
        assertEquals(25, heroP2.getVidaAtual(), "O feitiço ainda não causou dano enquanto aguarda a resposta de reação.");

        // 2. P2 escolhe ativar "Contra-Feitiço de Cristal" como Reação -> resolve em LIFO antes do Meteoro, anulando-o!
        engine.submitPlayerChoice(player2.getUuid(), p2CounterReaction.getUuid());
        assertFalse(engine.isWaitingForPlayerChoice());
        assertEquals(25, heroP2.getVidaAtual(), "Meteoro de Poeira foi anulado pela Reação e causou 0 de dano!");
        assertEquals(ZoneId.DISCARD, p1HeavySpell.getZoneId(), "A carta anulada foi enviada ao descarte.");

        // 3. Verifica que a Reação NÃO gastou nenhuma das 2 janelas nem o limite de 2 cartas de P2 no turno!
        assertEquals(2, engine.getRemainingResolutionWindowsForPlayer(player2.getUuid()), "Reação não gasta janela de resolução do turno.");
        assertEquals(0, engine.getCardsPlayedByPlayerInCurrentTurn(player2.getUuid()), "Reação não conta no limite de cartas ativas do turno.");
        assertEquals(player2.getUuid(), engine.getGameState().getEffectiveResolutionActivePlayerId());

        // 4. P2 ainda pode jogar suas 2 cartas normais nas Janelas 1 e 2!
        engine.submitResolutionCard(player2.getUuid(), p2NormalCard1.getUuid());
        assertEquals(22, heroP1.getVidaAtual());
        assertEquals(1, engine.getRemainingResolutionWindowsForPlayer(player2.getUuid()));
        assertEquals(1, engine.getCardsPlayedByPlayerInCurrentTurn(player2.getUuid()));

        // Janela 2 de P1
        engine.submitResolutionCard(player1.getUuid(), p1SecondAction.getUuid());
        assertEquals(22, heroP2.getVidaAtual());

        // Janela 2 de P2 (2ª carta normal de P2 no turno, além da reação já usada!)
        assertEquals(1, engine.getRemainingResolutionWindowsForPlayer(player2.getUuid()));
        assertEquals(1, engine.getCardsPlayedByPlayerInCurrentTurn(player2.getUuid()));
        engine.submitResolutionCard(player2.getUuid(), p2NormalCard2.getUuid());
        assertEquals(TurnPhase.DISCARD, engine.getTurnPhase());
        assertEquals(ZoneId.DISCARD, p2NormalCard2.getZoneId());
    }

    @Test
    @DisplayName("4. Carta de Reação defensiva (buff antes do dano) e restrição contra uso proativo fora de reação")
    void shouldApplyDefensiveReactionBeforeDamageAndForbidProactivePlayOfReactionOnlyCards() {
        Card p2ShieldReaction = createHandCard(
                player2.getUuid(),
                "Barreira Reativa",
                CardType.REACTION,
                Colors.BLUE,
                new TriggeredReactionEffect(ReactionTriggerType.ON_ENEMY_DAMAGE_EFFECT, new PlaySpellBuffEffect(3, 0, 0))
        );
        Card p1Spell = createHandCard(player1.getUuid(), "Raio Rubro", CardType.SPELL, Colors.RED, new MagicDamageEffect(6));

        player1.getCurrentDeck().getCardList().add(p1Spell);
        player2.getCurrentDeck().getCardList().add(p2ShieldReaction);

        // Coloca a iniciativa em P2 para testar que P2 NÃO pode jogar uma carta puramente de Reação na sua janela normal
        engine.getGameState().setActiveTurnPlayerId(player2.getUuid());
        engine.submitHeroChoice(player2.getUuid(), heroP2.getUuid(), heroP1.getUuid());

        assertThrows(IllegalArgumentException.class, () ->
                engine.submitResolutionCard(player2.getUuid(), p2ShieldReaction.getUuid())
        );

        // P2 passa sua 1ª janela -> P1 joga "Raio Rubro" (6 de Dano Mágico)
        engine.passResolutionWindow(player2.getUuid());
        engine.submitResolutionCard(player1.getUuid(), p1Spell.getUuid());

        // Abre janela de reação para P2 por ON_ENEMY_DAMAGE_EFFECT
        assertTrue(engine.isWaitingForPlayerChoice());
        engine.submitPlayerChoice(player2.getUuid(), p2ShieldReaction.getUuid());

        // Barreira Reativa concedeu +3 de Espírito (2 -> 5) ANTES do Raio Rubro resolver,
        // mitigando o dano de 6 para apenas 1 (6 - 5 = 1 -> Vida 25 -> 24)!
        assertEquals(5, heroP2.getEspirito());
        assertEquals(24, heroP2.getVidaAtual());
    }
}

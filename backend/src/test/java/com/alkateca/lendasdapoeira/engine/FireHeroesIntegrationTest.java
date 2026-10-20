package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.Afinidade;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FireHeroesIntegrationTest {

    @Test
    void testXamaDasCinzasEffects() {
        GameEngine engine = new GameEngine();
        GameState gameState = new GameState();
        engine.setGameState(gameState);
        engine.setCardsOnBoard(new ArrayList<>());

        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        Deck deck1 = new Deck();
        deck1.setHeroList(new ArrayList<>());
        deck1.setCardList(new ArrayList<>());
        p1.setCurrentDeck(deck1);
        engine.setPlayer1(p1);
        gameState.setPlayer1(p1);

        Player p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        Deck deck2 = new Deck();
        deck2.setHeroList(new ArrayList<>());
        deck2.setCardList(new ArrayList<>());
        p2.setCurrentDeck(deck2);
        engine.setPlayer2(p2);
        gameState.setPlayer2(p2);

        HeroCard xama = new HeroCard();
        xama.setUuid(UUID.randomUUID());
        xama.setCardName("Xamã das cinzas");
        xama.setOwnerId(p1.getUuid());
        xama.setEspirito(3);
        xama.setAtaque(4);
        xama.setDefesa(2);
        xama.setVidaMaxima(15);
        xama.setVidaAtual(15);
        xama.setEstaVivo(true);
        CardFactory.buildCardLogic(xama);
        deck1.getHeroList().add(xama);
        gameState.setActiveHeroPlayer1Id(xama.getUuid());

        HeroCard enemyHero = new HeroCard();
        enemyHero.setUuid(UUID.randomUUID());
        enemyHero.setCardName("Inimigo Teste");
        enemyHero.setOwnerId(p2.getUuid());
        enemyHero.setVidaMaxima(15);
        enemyHero.setVidaAtual(15);
        enemyHero.setEstaVivo(true);
        deck2.getHeroList().add(enemyHero);
        gameState.setActiveHeroPlayer2Id(enemyHero.getUuid());

        engine.setTurnPhase(TurnPhase.COMBAT_START);
        xama.getEffects().forEach(effect -> {
            if (effect.predicate(xama, TurnPhase.COMBAT_START)) {
                effect.resolve(engine, xama);
            }
        });

        assertEquals(1, xama.getDanoBonus(), "A aura de Xamã das cinzas deve conceder +1 de Dano Bônus.");
        assertEquals(8, enemyHero.getVidaAtual(), "A Bola de Fogo invocada no início do combate deve causar 7 de Dano Mágico (4 + 3 Espírito).");
        long discardFireballs = deck1.getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.DISCARD && "Bola de fogo".equalsIgnoreCase(c.getCardName()))
                .count();
        assertEquals(1, discardFireballs, "A Bola de Fogo invocada deve ir para o descarte.");
        assertTrue(deck1.getCardList().get(0).getAfinidades().contains(Afinidade.FOGO),
                "A Bola de Fogo invocada deve possuir Afinidade Fogo.");
    }

    @Test
    void testAlixDragaoFlamejanteEffects() {
        GameEngine engine = new GameEngine();
        GameState gameState = new GameState();
        engine.setGameState(gameState);
        engine.setCardsOnBoard(new ArrayList<>());

        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        Deck deck1 = new Deck();
        deck1.setHeroList(new ArrayList<>());
        deck1.setCardList(new ArrayList<>());
        p1.setCurrentDeck(deck1);
        engine.setPlayer1(p1);
        gameState.setPlayer1(p1);

        Player p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        Deck deck2 = new Deck();
        deck2.setHeroList(new ArrayList<>());
        deck2.setCardList(new ArrayList<>());
        p2.setCurrentDeck(deck2);
        engine.setPlayer2(p2);
        gameState.setPlayer2(p2);

        HeroCard alix = new HeroCard();
        alix.setUuid(UUID.randomUUID());
        alix.setCardName("Alix, Dragão Flamejante");
        alix.setOwnerId(p1.getUuid());
        alix.setEspirito(2);
        alix.setAtaque(5);
        alix.setDefesa(2);
        alix.setVidaMaxima(14);
        alix.setVidaAtual(14);
        alix.setEstaVivo(true);
        CardFactory.buildCardLogic(alix);
        deck1.getHeroList().add(alix);
        gameState.setActiveHeroPlayer1Id(alix.getUuid());

        HeroCard enemyHero = new HeroCard();
        enemyHero.setUuid(UUID.randomUUID());
        enemyHero.setCardName("Inimigo Teste");
        enemyHero.setOwnerId(p2.getUuid());
        enemyHero.setVidaMaxima(15);
        enemyHero.setVidaAtual(15);
        enemyHero.setEstaVivo(true);
        deck2.getHeroList().add(enemyHero);
        gameState.setActiveHeroPlayer2Id(enemyHero.getUuid());

        // Simula jogar Peitoral de brasas (Item - Armadura) na resolução
        Card peitoral = new Card();
        peitoral.setUuid(UUID.randomUUID());
        peitoral.setCardName("Peitoral de brasas");
        peitoral.setCardType(CardType.ITEM);
        peitoral.setItemType(ItemType.ARMADURA);
        peitoral.setOwnerId(p1.getUuid());
        gameState.getPlayer1PlayedCards().add(peitoral);

        // Fase RESOLUTION: Ao jogar Armadura -> Invocar: Bola de Fogo (4 + 2 Espírito = 6 de Dano Mágico)
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        alix.getEffects().forEach(effect -> {
            if (effect.predicate(alix, TurnPhase.RESOLUTION)) {
                effect.resolve(engine, alix);
            }
        });
        assertEquals(9, enemyHero.getVidaAtual(), "Alix deve invocar Bola de Fogo ao jogar Armadura, causando 6 de Dano Mágico (4 + 2 Espírito).");

        // Fase COMBAT_END: Final do Combate -> Cause 3 de Dano Mágico ao Herói Inimigo
        engine.setTurnPhase(TurnPhase.COMBAT_END);
        alix.getEffects().forEach(effect -> {
            if (effect.predicate(alix, TurnPhase.COMBAT_END)) {
                effect.resolve(engine, alix);
            }
        });
        assertEquals(6, enemyHero.getVidaAtual(), "Alix deve causar 3 de Dano Mágico no Final do Combate (9 - 3 = 6).");
    }

    @Test
    void testConstructoDasChamasSifEffects() {
        GameEngine engine = new GameEngine();
        GameState gameState = new GameState();
        engine.setGameState(gameState);
        engine.setCardsOnBoard(new ArrayList<>());

        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        Deck deck1 = new Deck();
        deck1.setHeroList(new ArrayList<>());
        deck1.setCardList(new ArrayList<>());
        p1.setCurrentDeck(deck1);
        engine.setPlayer1(p1);
        gameState.setPlayer1(p1);

        Player p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        Deck deck2 = new Deck();
        deck2.setHeroList(new ArrayList<>());
        deck2.setCardList(new ArrayList<>());
        p2.setCurrentDeck(deck2);
        engine.setPlayer2(p2);
        gameState.setPlayer2(p2);

        HeroCard sif = new HeroCard();
        sif.setUuid(UUID.randomUUID());
        sif.setCardName("Constructo das Chamas, Sif");
        sif.setOwnerId(p1.getUuid());
        sif.setEspirito(2);
        sif.setAtaque(7);
        sif.setDefesa(2);
        sif.setVidaMaxima(15);
        sif.setVidaAtual(15);
        sif.setEstaVivo(true);
        CardFactory.buildCardLogic(sif);
        deck1.getHeroList().add(sif);
        gameState.setActiveHeroPlayer1Id(sif.getUuid());

        // Coloca 3 Feitiços Fogo no Baralho para validar que apenas 2 são enviados ao descarte no Início da Partida
        for (int i = 0; i < 3; i++) {
            Card deckSpell = new Card();
            deckSpell.setUuid(UUID.randomUUID());
            deckSpell.setCardName("Bola de fogo");
            deckSpell.setCardType(CardType.SPELL);
            deckSpell.setAfinidades(Arrays.asList(Afinidade.FOGO));
            deckSpell.setOwnerId(p1.getUuid());
            deckSpell.setZoneId(ZoneId.DECK);
            deck1.getCardList().add(deckSpell);
        }

        // 1. Início da Partida (GAME_START): Move 2 Feitiços Fogo do Baralho para o Descarte
        engine.setTurnPhase(TurnPhase.GAME_START);
        sif.getEffects().forEach(effect -> {
            if (effect.predicate(sif, TurnPhase.GAME_START)) {
                effect.resolve(engine, sif);
            }
        });
        long deckCountAfterGameStart = deck1.getCardList().stream().filter(c -> c.getZoneId() == ZoneId.DECK).count();
        long discardCountAfterGameStart = deck1.getCardList().stream().filter(c -> c.getZoneId() == ZoneId.DISCARD).count();
        assertEquals(1, deckCountAfterGameStart, "Restam 1 Feitiço Fogo no baralho após enviar 2 no Início da Partida.");
        assertEquals(2, discardCountAfterGameStart, "Sif deve enviar 2 Feitiços Fogo do baralho para o descarte no Início da Partida.");

        // 2. Início do Combate (COMBAT_START): Move 2 Feitiços Fogo do Descarte para a Mão (sem fazer mill do baralho!)
        engine.setTurnPhase(TurnPhase.COMBAT_START);
        sif.getEffects().forEach(effect -> {
            if (effect.predicate(sif, TurnPhase.COMBAT_START)) {
                effect.resolve(engine, sif);
            }
        });
        long handCountAfterCombatStart = deck1.getCardList().stream().filter(c -> c.getZoneId() == ZoneId.HAND).count();
        long discardCountAfterCombatStart = deck1.getCardList().stream().filter(c -> c.getZoneId() == ZoneId.DISCARD).count();
        long deckCountAfterCombatStart = deck1.getCardList().stream().filter(c -> c.getZoneId() == ZoneId.DECK).count();
        assertEquals(2, handCountAfterCombatStart, "Sif deve adicionar 2 Feitiços Fogo do descarte para a mão no Início do Combate.");
        assertEquals(0, discardCountAfterCombatStart, "O descarte deve ficar vazio após puxar os 2 Feitiços Fogo para a mão.");
        assertEquals(1, deckCountAfterCombatStart, "O baralho não deve sofrer mill durante o Início do Combate.");
    }

    @Test
    void testBombaDeFumacaAndTemporaryBuffsExpiration() {
        GameEngine engine = new GameEngine();
        GameState gameState = new GameState();
        engine.setGameState(gameState);
        engine.setCardsOnBoard(new ArrayList<>());

        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        Deck deck1 = new Deck();
        deck1.setHeroList(new ArrayList<>());
        deck1.setCardList(new ArrayList<>());
        p1.setCurrentDeck(deck1);
        engine.setPlayer1(p1);
        gameState.setPlayer1(p1);

        Player p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        Deck deck2 = new Deck();
        deck2.setHeroList(new ArrayList<>());
        deck2.setCardList(new ArrayList<>());
        p2.setCurrentDeck(deck2);
        engine.setPlayer2(p2);
        gameState.setPlayer2(p2);

        HeroCard moyra = new HeroCard();
        moyra.setUuid(UUID.randomUUID());
        moyra.setCardName("Moyra, Aprendiz da Santa");
        moyra.setOwnerId(p1.getUuid());
        moyra.setEspirito(2);
        moyra.setAtaque(4);
        moyra.setDefesa(1);
        moyra.setVidaMaxima(13);
        moyra.setVidaAtual(13);
        moyra.setEstaVivo(true);
        CardFactory.buildCardLogic(moyra);
        deck1.getHeroList().add(moyra);
        gameState.setActiveHeroPlayer1Id(moyra.getUuid());

        HeroCard enemyHero = new HeroCard();
        enemyHero.setUuid(UUID.randomUUID());
        enemyHero.setCardName("Inimigo Teste");
        enemyHero.setOwnerId(p2.getUuid());
        enemyHero.setEspirito(2);
        enemyHero.setAtaque(5);
        enemyHero.setDefesa(4);
        enemyHero.setVidaMaxima(15);
        enemyHero.setVidaAtual(15);
        enemyHero.setEstaVivo(true);
        deck2.getHeroList().add(enemyHero);
        gameState.setActiveHeroPlayer2Id(enemyHero.getUuid());

        Card bomba = new Card();
        bomba.setUuid(UUID.randomUUID());
        bomba.setCardName("Bomba de fumaça");
        bomba.setCardType(CardType.ITEM);
        bomba.setItemType(ItemType.FERRAMENTA);
        bomba.setOwnerId(p1.getUuid());
        CardFactory.buildCardLogic(bomba);

        Card golpesPesados = new Card();
        golpesPesados.setUuid(UUID.randomUUID());
        golpesPesados.setCardName("Golpes Pesados");
        golpesPesados.setCardType(CardType.SPELL);
        golpesPesados.setOwnerId(p1.getUuid());
        CardFactory.buildCardLogic(golpesPesados);

        gameState.getPlayer1PlayedCards().add(bomba);
        gameState.getPlayer1PlayedCards().add(golpesPesados);

        // Na RESOLUTION:
        // - Bomba de fumaça equipa (+2 Ataque permanente enquanto anexada) e reduz Defesa inimiga em -2 (4 -> 2 temporário)
        // - Golpes Pesados dá +2 Ataque (igual ao Espírito 2) até o Final do Combate
        // - Moyra, Aprendiz da Santa recebe +3 Ataque até o Final do Combate por jogar 1 Magia
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        bomba.getEffects().forEach(eff -> {
            if (eff.predicate(bomba, TurnPhase.RESOLUTION)) eff.resolve(engine, bomba);
        });
        golpesPesados.getEffects().forEach(eff -> {
            if (eff.predicate(golpesPesados, TurnPhase.RESOLUTION)) eff.resolve(engine, golpesPesados);
        });
        moyra.getEffects().forEach(eff -> {
            if (eff.predicate(moyra, TurnPhase.RESOLUTION)) eff.resolve(engine, moyra);
        });

        assertEquals(11, moyra.getAtaque(), "Moyra deve ter 4 (base) + 2 (Bomba) + 2 (Golpes Pesados) + 3 (Passiva Magia) = 11 de Ataque durante o combate.");
        assertEquals(2, enemyHero.getDefesa(), "Inimigo deve sofrer -2 de Defesa de Bomba de fumaça durante o combate (4 -> 2).");

        // Ao encerrar o combate (COMBAT_END), os buffs/debuffs temporários expiram, mantendo apenas o +2 do item anexado
        engine.combatEnd();

        assertEquals(6, moyra.getAtaque(), "Após o Final do Combate, os buffs temporários (+2 e +3) expiram e permanece apenas 4 + 2 (Bomba anexada) = 6.");
        assertEquals(4, enemyHero.getDefesa(), "Após o Final do Combate, o debuff de -2 de Defesa de Bomba de fumaça deve expirar (voltando para 4).");
    }

    @Test
    void testImmediateSlotOrderOnPlayEffectsInResolutionQueue() {
        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        p1.setName("Jogador Alix");
        Deck deck1 = new Deck();
        deck1.setHeroList(new ArrayList<>());
        deck1.setCardList(new ArrayList<>());
        p1.setCurrentDeck(deck1);

        Player p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        p2.setName("Jogador Oponente");
        Deck deck2 = new Deck();
        deck2.setHeroList(new ArrayList<>());
        deck2.setCardList(new ArrayList<>());
        p2.setCurrentDeck(deck2);

        GameEngine engine = new GameEngine(p1, p2);
        GameState gameState = engine.getGameState();
        gameState.setActiveTurnPlayerId(p1.getUuid());

        HeroCard alix = new HeroCard();
        alix.setUuid(UUID.randomUUID());
        alix.setCardName("Alix, Dragão Flamejante");
        alix.setOwnerId(p1.getUuid());
        alix.setEspirito(2);
        alix.setAtaque(5);
        alix.setDefesa(2);
        alix.setVidaMaxima(14);
        alix.setVidaAtual(14);
        alix.setEstaVivo(true);
        CardFactory.buildCardLogic(alix);
        deck1.getHeroList().add(alix);
        gameState.setActiveHeroPlayer1Id(alix.getUuid());

        HeroCard enemyHero = new HeroCard();
        enemyHero.setUuid(UUID.randomUUID());
        enemyHero.setCardName("Inimigo Teste");
        enemyHero.setOwnerId(p2.getUuid());
        enemyHero.setEspirito(0);
        enemyHero.setAtaque(0);
        enemyHero.setDefesa(0);
        enemyHero.setVidaMaxima(20);
        enemyHero.setVidaAtual(20);
        enemyHero.setEstaVivo(true);
        deck2.getHeroList().add(enemyHero);
        gameState.setActiveHeroPlayer2Id(enemyHero.getUuid());

        // Slot 1 (P1): Peitoral de brasas (Armadura: +1 Espírito, +2 Defesa)
        Card peitoral = new Card();
        peitoral.setUuid(UUID.randomUUID());
        peitoral.setCardName("Peitoral de brasas");
        peitoral.setCardType(CardType.ITEM);
        peitoral.setItemType(ItemType.ARMADURA);
        peitoral.setOwnerId(p1.getUuid());
        peitoral.setZoneId(ZoneId.RESOLVE);
        CardFactory.buildCardLogic(peitoral);
        deck1.getCardList().add(peitoral);
        gameState.getPlayer1PlayedCards().add(peitoral);

        // Slot 2 (P2): Quimera (+2 Espírito ao Inimigo)
        // Como Alix joga antes no Slot 1, a Bola de Fogo invocada deve resolver ANTES da Quimera dar +2 Espírito ao inimigo!
        Card quimera = new Card();
        quimera.setUuid(UUID.randomUUID());
        quimera.setCardName("Quimera");
        quimera.setCardType(CardType.ITEM);
        quimera.setItemType(ItemType.JOIA);
        quimera.setOwnerId(p2.getUuid());
        quimera.setZoneId(ZoneId.RESOLVE);
        CardFactory.buildCardLogic(quimera);
        deck2.getCardList().add(quimera);
        gameState.getPlayer2PlayedCards().add(quimera);

        // Slot 3 (P1): Escudo ardente (Escudo, não é Armadura -> não deve re-invocar Bola de Fogo)
        Card escudo = new Card();
        escudo.setUuid(UUID.randomUUID());
        escudo.setCardName("Escudo ardente");
        escudo.setCardType(CardType.ITEM);
        escudo.setItemType(ItemType.ESCUDO);
        escudo.setOwnerId(p1.getUuid());
        escudo.setZoneId(ZoneId.RESOLVE);
        CardFactory.buildCardLogic(escudo);
        deck1.getCardList().add(escudo);
        gameState.getPlayer1PlayedCards().add(escudo);

        engine.resolution();

        // Peitoral de brasas deu +1 Espírito (2 -> 3) e Alix invocou 1 Bola de Fogo imediatamente no Slot 1:
        // Dano = 4 + 3 (Espírito de Alix) - 0 (Espírito do Inimigo antes da Quimera no Slot 2) = 7 de Dano Mágico (20 -> 13)!
        assertEquals(13, enemyHero.getVidaAtual(),
                "A Bola de Fogo de Alix deve resolver imediatamente após o Peitoral de brasas (Slot 1), antes da Quimera do oponente (Slot 2).");
    }
}

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
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorlessCardsIntegrationTest {

    @Test
    void testGarraEspectralEffectsAndMagicAttack() {
        GameEngine engine = new GameEngine();
        GameState gameState = new GameState();
        engine.setGameState(gameState);
        engine.setCardsOnBoard(new ArrayList<>());

        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        p1.setName("Jogador 1");
        Deck deck1 = new Deck();
        deck1.setHeroList(new ArrayList<>());
        deck1.setCardList(new ArrayList<>());
        p1.setCurrentDeck(deck1);
        engine.setPlayer1(p1);
        gameState.setPlayer1(p1);

        Player p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        p2.setName("Jogador 2");
        Deck deck2 = new Deck();
        deck2.setHeroList(new ArrayList<>());
        deck2.setCardList(new ArrayList<>());
        p2.setCurrentDeck(deck2);
        engine.setPlayer2(p2);
        gameState.setPlayer2(p2);

        HeroCard myHero = new HeroCard();
        myHero.setUuid(UUID.randomUUID());
        myHero.setCardName("Herói Atacante");
        myHero.setOwnerId(p1.getUuid());
        myHero.setEspirito(2);
        myHero.setAtaque(5);
        myHero.setDefesa(2);
        myHero.setVidaMaxima(15);
        myHero.setVidaAtual(15);
        myHero.setMaxWeaponSlots(2);
        myHero.setEstaVivo(true);
        deck1.getHeroList().add(myHero);
        gameState.setActiveHeroPlayer1Id(myHero.getUuid());

        HeroCard enemyHero = new HeroCard();
        enemyHero.setUuid(UUID.randomUUID());
        enemyHero.setCardName("Inimigo Blindado");
        enemyHero.setOwnerId(p2.getUuid());
        enemyHero.setEspirito(3);
        enemyHero.setAtaque(0);
        enemyHero.setDefesa(10); // Defesa alta que seria impenetrável por dano físico
        enemyHero.setVidaMaxima(20);
        enemyHero.setVidaAtual(20);
        enemyHero.setEstaVivo(true);
        deck2.getHeroList().add(enemyHero);
        gameState.setActiveHeroPlayer2Id(enemyHero.getUuid());

        Card garra = new Card();
        garra.setUuid(UUID.randomUUID());
        garra.setCardName("Garra espectral");
        garra.setCardType(CardType.ITEM);
        garra.setItemType(ItemType.ARMA);
        garra.setEmpunhadura(1);
        garra.setColor(Colors.NEUTRAL);
        garra.setOwnerId(p1.getUuid());
        garra.setZoneId(ZoneId.RESOLVE);
        CardFactory.buildCardLogic(garra);
        deck1.getCardList().add(garra);

        // 1. Ao equipar em RESOLUTION: concede +2 Ataque (5 -> 7) e reduz -2 Espírito do inimigo até o Final do Combate (3 -> 1)
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        garra.getEffects().forEach(eff -> {
            if (eff.predicate(garra, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, garra);
            }
        });

        assertEquals(7, myHero.getAtaque(), "Garra espectral deve conceder +2 de Ataque ao equipar.");
        assertEquals(1, enemyHero.getEspirito(), "Garra espectral deve reduzir o Espírito inimigo em 2 ao ser equipada (3 -> 1).");
        assertTrue(engine.hasMagicBasicAttacks(myHero.getUuid()), "Herói com Garra espectral deve causar Dano Mágico nos ataques.");

        // 2. Em COMBAT: Ataque (7) causa Dano Mágico mitigado pelo Espírito inimigo (1) em vez da Defesa (10) -> 6 de dano (20 -> 14)
        engine.combat();
        assertEquals(14, enemyHero.getVidaAtual(), "Ataque com Garra espectral deve ignorar a Defesa (10) e ser mitigado pelo Espírito (1), causando 6 de dano.");

        // 3. No Final do Combate, o debuff de -2 Espírito expira (1 -> 3), e no próximo COMBAT_START ativa novamente (-2 Espírito)
        engine.runCombatEndCleanup();
        assertEquals(3, enemyHero.getEspirito(), "Após o Final do Combate, o Espírito do inimigo deve voltar a 3.");

        engine.setTurnPhase(TurnPhase.COMBAT_START);
        garra.getEffects().forEach(eff -> {
            if (eff.predicate(garra, TurnPhase.COMBAT_START)) {
                eff.resolve(engine, garra);
            }
        });
        assertEquals(1, enemyHero.getEspirito(), "No Início do Combate com Garra espectral equipada, o Espírito inimigo deve ser reduzido em 2 novamente.");
    }

    @Test
    void testParaRaiosEstaticaAndRisadaSarcastica() {
        GameEngine engine = new GameEngine();
        GameState gameState = new GameState();
        engine.setGameState(gameState);
        engine.setCardsOnBoard(new ArrayList<>());

        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        p1.setName("Jogador 1");
        Deck deck1 = new Deck();
        deck1.setHeroList(new ArrayList<>());
        deck1.setCardList(new ArrayList<>());
        p1.setCurrentDeck(deck1);
        engine.setPlayer1(p1);
        gameState.setPlayer1(p1);

        Player p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        p2.setName("Jogador 2");
        Deck deck2 = new Deck();
        deck2.setHeroList(new ArrayList<>());
        deck2.setCardList(new ArrayList<>());
        p2.setCurrentDeck(deck2);
        engine.setPlayer2(p2);
        gameState.setPlayer2(p2);

        HeroCard myHero = new HeroCard();
        myHero.setUuid(UUID.randomUUID());
        myHero.setCardName("Mago Teste");
        myHero.setOwnerId(p1.getUuid());
        myHero.setEspirito(1);
        myHero.setAtaque(4);
        myHero.setDefesa(1);
        myHero.setVidaMaxima(15);
        myHero.setVidaAtual(15);
        myHero.setEstaVivo(true);
        deck1.getHeroList().add(myHero);
        gameState.setActiveHeroPlayer1Id(myHero.getUuid());

        HeroCard enemyHero = new HeroCard();
        enemyHero.setUuid(UUID.randomUUID());
        enemyHero.setCardName("Inimigo Teste");
        enemyHero.setOwnerId(p2.getUuid());
        enemyHero.setEspirito(1);
        enemyHero.setAtaque(4);
        enemyHero.setDefesa(1);
        enemyHero.setVidaMaxima(30);
        enemyHero.setVidaAtual(30);
        enemyHero.setEstaVivo(true);
        deck2.getHeroList().add(enemyHero);
        gameState.setActiveHeroPlayer2Id(enemyHero.getUuid());

        // Coloca 2 cópias de Estatica no DECK de P1
        Card estatica1 = new Card();
        estatica1.setUuid(UUID.randomUUID());
        estatica1.setCardName("Estatica");
        estatica1.setCardType(CardType.SPELL);
        estatica1.setColor(Colors.NEUTRAL);
        estatica1.setOwnerId(p1.getUuid());
        estatica1.setZoneId(ZoneId.DECK);
        CardFactory.buildCardLogic(estatica1);
        deck1.getCardList().add(estatica1);

        Card estatica2 = new Card();
        estatica2.setUuid(UUID.randomUUID());
        estatica2.setCardName("Estatica");
        estatica2.setCardType(CardType.SPELL);
        estatica2.setColor(Colors.NEUTRAL);
        estatica2.setOwnerId(p1.getUuid());
        estatica2.setZoneId(ZoneId.DECK);
        CardFactory.buildCardLogic(estatica2);
        deck1.getCardList().add(estatica2);

        Card paraRaios = new Card();
        paraRaios.setUuid(UUID.randomUUID());
        paraRaios.setCardName("Para-raios");
        paraRaios.setCardType(CardType.SPELL);
        paraRaios.setColor(Colors.NEUTRAL);
        paraRaios.setOwnerId(p1.getUuid());
        paraRaios.setZoneId(ZoneId.RESOLVE);
        CardFactory.buildCardLogic(paraRaios);
        deck1.getCardList().add(paraRaios);

        // Para-raios concede +1 Espírito (1 -> 2) e invoca Estatica1 do DECK, que por sua vez invoca Estatica2 do DECK e para (sem loop infinito!)
        // Cada Estatica causa (3 + 2 Espírito - 1 Espírito inimigo) = 4 de Dano Mágico -> 2x4 = 8 de Dano Mágico (30 -> 22)
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        paraRaios.getEffects().forEach(eff -> {
            if (eff.predicate(paraRaios, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, paraRaios);
            }
        });
        engine.getResolutionQueue().resolveAll(engine);

        assertEquals(2, myHero.getEspirito(), "Para-raios deve conceder +1 de Espírito até o Final do Combate.");
        assertEquals(22, enemyHero.getVidaAtual(), "As 2 cópias de Estatica no baralho devem causar 4 de dano cada (total 8), parando quando o baralho esgotar.");
        assertEquals(ZoneId.RESOLVE, estatica1.getZoneId(), "A 1ª Estatica invocada deve permanecer em resolução na mesa durante o combate.");
        assertEquals(ZoneId.RESOLVE, estatica2.getZoneId(), "A 2ª Estatica invocada deve permanecer em resolução na mesa durante o combate.");

        // Testa Risada Sarcástica: causa X + 2 de Dano Mágico (2 + 2 - 1 = 3 -> 22 -> 19) e adiciona Tristeza ao DECK inimigo no Final do Combate
        Card risada = new Card();
        risada.setUuid(UUID.randomUUID());
        risada.setCardName("Risada Sarcástica");
        risada.setCardType(CardType.THEURGY);
        risada.setColor(Colors.NEUTRAL);
        risada.setOwnerId(p1.getUuid());
        risada.setZoneId(ZoneId.RESOLVE);
        CardFactory.buildCardLogic(risada);
        deck1.getCardList().add(risada);

        risada.getEffects().forEach(eff -> {
            if (eff.predicate(risada, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, risada);
            }
        });

        assertEquals(19, enemyHero.getVidaAtual(), "Risada Sarcástica deve causar 3 de Dano Mágico (2 + 2 Espírito - 1 Espírito inimigo).");

        engine.runCombatEndCleanup();
        assertEquals(1, myHero.getEspirito(), "O buff de +1 Espírito do Para-raios deve expirar no Final do Combate.");
        long tristezaCount = deck2.getCardList().stream()
                .filter(c -> "Tristeza".equalsIgnoreCase(c.getCardName()) && c.getZoneId() == ZoneId.DECK)
                .count();
        assertEquals(1, tristezaCount, "Uma carta Tristeza deve ter sido adicionada ao DECK do inimigo no Final do Combate.");
    }

    @Test
    void testVendavalArcanoQuebraConvocacaoMirarNaCabecaAndBanir() {
        GameEngine engine = new GameEngine();
        GameState gameState = new GameState();
        engine.setGameState(gameState);
        engine.setCardsOnBoard(new ArrayList<>());

        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        p1.setName("Jogador 1");
        Deck deck1 = new Deck();
        deck1.setHeroList(new ArrayList<>());
        deck1.setCardList(new ArrayList<>());
        p1.setCurrentDeck(deck1);
        engine.setPlayer1(p1);
        gameState.setPlayer1(p1);

        Player p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        p2.setName("Jogador 2");
        Deck deck2 = new Deck();
        deck2.setHeroList(new ArrayList<>());
        deck2.setCardList(new ArrayList<>());
        p2.setCurrentDeck(deck2);
        engine.setPlayer2(p2);
        gameState.setPlayer2(p2);

        HeroCard myHero = new HeroCard();
        myHero.setUuid(UUID.randomUUID());
        myHero.setCardName("Herói Aliado");
        myHero.setOwnerId(p1.getUuid());
        myHero.setAtaque(0);
        myHero.setVidaMaxima(15);
        myHero.setVidaAtual(15);
        myHero.setEstaVivo(true);
        myHero.setEstaAtivo(false); // Já agiu
        deck1.getHeroList().add(myHero);
        gameState.setActiveHeroPlayer1Id(myHero.getUuid());

        HeroCard enemyActive = new HeroCard();
        enemyActive.setUuid(UUID.randomUUID());
        enemyActive.setCardName("Inimigo Ativo");
        enemyActive.setOwnerId(p2.getUuid());
        enemyActive.setAtaque(0);
        enemyActive.setVidaMaxima(15);
        enemyActive.setVidaAtual(15);
        enemyActive.setEstaVivo(true);
        enemyActive.setEstaAtivo(true);
        deck2.getHeroList().add(enemyActive);
        gameState.setActiveHeroPlayer2Id(enemyActive.getUuid());

        HeroCard enemyBench = new HeroCard();
        enemyBench.setUuid(UUID.randomUUID());
        enemyBench.setCardName("Inimigo Bancada");
        enemyBench.setOwnerId(p2.getUuid());
        enemyBench.setAtaque(0);
        enemyBench.setVidaMaxima(15);
        enemyBench.setVidaAtual(15);
        enemyBench.setEstaVivo(true);
        enemyBench.setEstaAtivo(true);
        deck2.getHeroList().add(enemyBench);

        // Equipa 1 item em enemyActive e 1 item em enemyBench para testar Vendaval Arcano
        Card itemActive = new Card();
        itemActive.setUuid(UUID.randomUUID());
        itemActive.setCardName("Lamina de Cristal");
        itemActive.setCardType(CardType.ITEM);
        itemActive.setItemType(ItemType.ARMA);
        itemActive.setEmpunhadura(1);
        itemActive.setOwnerId(p2.getUuid());
        itemActive.setAttachedToCardId(enemyActive.getUuid());
        itemActive.setZoneId(ZoneId.ATTACHED);
        deck2.getCardList().add(itemActive);

        Card itemBench = new Card();
        itemBench.setUuid(UUID.randomUUID());
        itemBench.setCardName("Broche de Cristal");
        itemBench.setCardType(CardType.ITEM);
        itemBench.setItemType(ItemType.JOIA);
        itemBench.setOwnerId(p2.getUuid());
        itemBench.setAttachedToCardId(enemyBench.getUuid());
        itemBench.setZoneId(ZoneId.ATTACHED);
        deck2.getCardList().add(itemBench);

        Card vendaval = new Card();
        vendaval.setUuid(UUID.randomUUID());
        vendaval.setCardName("Vendaval Arcano");
        vendaval.setCardType(CardType.SPELL);
        vendaval.setColor(Colors.NEUTRAL);
        vendaval.setOwnerId(p1.getUuid());
        vendaval.setZoneId(ZoneId.RESOLVE);
        CardFactory.buildCardLogic(vendaval);

        engine.setTurnPhase(TurnPhase.RESOLUTION);
        vendaval.getEffects().forEach(eff -> eff.resolve(engine, vendaval));

        assertEquals(ZoneId.DISCARD, itemActive.getZoneId(), "Vendaval Arcano deve descartar 1 item do Inimigo Ativo.");
        assertEquals(ZoneId.DISCARD, itemBench.getZoneId(), "Vendaval Arcano deve descartar 1 item do Inimigo na Bancada.");

        // Reequipa itemActive para testar Quebra!
        itemActive.setAttachedToCardId(enemyActive.getUuid());
        itemActive.setZoneId(ZoneId.ATTACHED);

        Card quebra = new Card();
        quebra.setUuid(UUID.randomUUID());
        quebra.setCardName("Quebra!");
        quebra.setCardType(CardType.SPELL);
        quebra.setColor(Colors.NEUTRAL);
        quebra.setOwnerId(p1.getUuid());
        quebra.setZoneId(ZoneId.RESOLVE);
        CardFactory.buildCardLogic(quebra);

        quebra.getEffects().forEach(eff -> eff.resolve(engine, quebra));
        assertNotNull(gameState.getPendingChoice(), "Quebra! deve pedir para escolher um Item equipado no Herói Inimigo.");
        engine.submitPlayerChoice(p1.getUuid(), itemActive.getUuid());
        assertEquals(ZoneId.DISCARD, itemActive.getZoneId(), "O Item escolhido por Quebra! deve ser descartado.");

        // Testa Convocação: altera o Estado Ativo de enemyBench (true -> false)
        gameState.setActiveHeroPlayer1Id(myHero.getUuid());
        gameState.setActiveHeroPlayer2Id(enemyActive.getUuid());
        engine.setTurnPhase(TurnPhase.RESOLUTION);

        Card convocacao = new Card();
        convocacao.setUuid(UUID.randomUUID());
        convocacao.setCardName("Convocação");
        convocacao.setCardType(CardType.ACTION);
        convocacao.setColor(Colors.NEUTRAL);
        convocacao.setOwnerId(p1.getUuid());
        convocacao.setZoneId(ZoneId.RESOLVE);
        CardFactory.buildCardLogic(convocacao);

        convocacao.getEffects().forEach(eff -> eff.resolve(engine, convocacao));
        assertNotNull(gameState.getPendingChoice(), "Convocação deve solicitar a escolha de um Herói.");
        engine.submitPlayerChoice(p1.getUuid(), enemyBench.getUuid());
        assertFalse(enemyBench.getEstaAtivo(), "Convocação deve inverter o Estado Ativo do Herói escolhido (true -> false).");

        // Testa Mirar na cabeça: 3 de Dano Direto no Herói Inimigo escolhido (enemyBench: 15 -> 12)
        gameState.setActiveHeroPlayer1Id(myHero.getUuid());
        gameState.setActiveHeroPlayer2Id(enemyActive.getUuid());
        engine.setTurnPhase(TurnPhase.RESOLUTION);

        Card mirar = new Card();
        mirar.setUuid(UUID.randomUUID());
        mirar.setCardName("Mirar na cabeça");
        mirar.setCardType(CardType.ACTION);
        mirar.setColor(Colors.NEUTRAL);
        mirar.setOwnerId(p1.getUuid());
        mirar.setZoneId(ZoneId.RESOLVE);
        CardFactory.buildCardLogic(mirar);

        mirar.getEffects().forEach(eff -> eff.resolve(engine, mirar));
        assertNotNull(gameState.getPendingChoice(), "Mirar na cabeça deve solicitar a escolha de um Herói Inimigo.");
        engine.submitPlayerChoice(p1.getUuid(), enemyBench.getUuid());
        assertEquals(12, enemyBench.getVidaAtual(), "Mirar na cabeça deve causar 3 de Dano Direto no Herói Inimigo escolhido (15 -> 12).");

        // Testa Banir!: escolhe Lamina de Cristal no descarte inimigo e exila tanto ela quanto outra cópia no DECK/HAND do inimigo
        Card copyInEnemyDeck = new Card();
        copyInEnemyDeck.setUuid(UUID.randomUUID());
        copyInEnemyDeck.setCardName("Lamina de Cristal");
        copyInEnemyDeck.setCardType(CardType.ITEM);
        copyInEnemyDeck.setOwnerId(p2.getUuid());
        copyInEnemyDeck.setZoneId(ZoneId.DECK);
        deck2.getCardList().add(copyInEnemyDeck);

        Card copyInEnemyHand = new Card();
        copyInEnemyHand.setUuid(UUID.randomUUID());
        copyInEnemyHand.setCardName("Lamina de Cristal");
        copyInEnemyHand.setCardType(CardType.ITEM);
        copyInEnemyHand.setOwnerId(p2.getUuid());
        copyInEnemyHand.setZoneId(ZoneId.HAND);
        deck2.getCardList().add(copyInEnemyHand);

        gameState.setActiveHeroPlayer1Id(myHero.getUuid());
        gameState.setActiveHeroPlayer2Id(enemyActive.getUuid());
        engine.setTurnPhase(TurnPhase.RESOLUTION);

        Card banir = new Card();
        banir.setUuid(UUID.randomUUID());
        banir.setCardName("Banir!");
        banir.setCardType(CardType.ACTION);
        banir.setColor(Colors.NEUTRAL);
        banir.setOwnerId(p1.getUuid());
        banir.setZoneId(ZoneId.RESOLVE);
        CardFactory.buildCardLogic(banir);

        banir.getEffects().forEach(eff -> eff.resolve(engine, banir));
        assertNotNull(gameState.getPendingChoice(), "Banir! deve solicitar a escolha de uma carta inimiga em jogo ou no descarte.");
        engine.submitPlayerChoice(p1.getUuid(), itemActive.getUuid());

        assertEquals(ZoneId.EXILE, itemActive.getZoneId(), "A carta escolhida no descarte deve ser exilada.");
        assertEquals(ZoneId.EXILE, copyInEnemyDeck.getZoneId(), "A cópia no baralho inimigo deve ser exilada.");
        assertEquals(ZoneId.EXILE, copyInEnemyHand.getZoneId(), "A cópia na mão inimiga deve ser exilada.");
    }
}

package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.Classe;
import com.alkateca.lendasdapoeira.enums.Colors;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.Ordem;
import com.alkateca.lendasdapoeira.enums.Raca;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class YellowThroneGuardiansIntegrationTest {

    @Test
    void testOraculoRealAndFortalecaSuaFe() {
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

        HeroCard oraculo = new HeroCard();
        oraculo.setUuid(UUID.randomUUID());
        oraculo.setCardName("Oráculo Real");
        oraculo.setOwnerId(p1.getUuid());
        oraculo.setEspirito(1);
        oraculo.setAtaque(6);
        oraculo.setDefesa(0);
        oraculo.setVidaMaxima(18);
        oraculo.setVidaAtual(18);
        oraculo.setEstaVivo(true);
        oraculo.setRacas(new ArrayList<>(Collections.singletonList(Raca.GUARDIOES_DO_TRONO)));
        oraculo.setClasse(new ArrayList<>(Collections.singletonList(Classe.TRANSFORMADOR)));
        CardFactory.buildCardLogic(oraculo);
        deck1.getHeroList().add(oraculo);
        gameState.setActiveHeroPlayer1Id(oraculo.getUuid());

        HeroCard portadorBench = new HeroCard();
        portadorBench.setUuid(UUID.randomUUID());
        portadorBench.setCardName("Portador da Chama");
        portadorBench.setOwnerId(p1.getUuid());
        portadorBench.setEspirito(0);
        portadorBench.setAtaque(5);
        portadorBench.setDefesa(0);
        portadorBench.setVidaMaxima(16);
        portadorBench.setVidaAtual(16);
        portadorBench.setEstaVivo(true);
        portadorBench.setRacas(new ArrayList<>(Collections.singletonList(Raca.GUARDIOES_DO_TRONO)));
        portadorBench.setClasse(new ArrayList<>(Collections.singletonList(Classe.TRANSFORMADOR)));
        CardFactory.buildCardLogic(portadorBench);
        deck1.getHeroList().add(portadorBench);

        HeroCard enemyHero = new HeroCard();
        enemyHero.setUuid(UUID.randomUUID());
        enemyHero.setCardName("Inimigo Teste");
        enemyHero.setOwnerId(p2.getUuid());
        enemyHero.setVidaMaxima(20);
        enemyHero.setVidaAtual(20);
        enemyHero.setEstaVivo(true);
        deck2.getHeroList().add(enemyHero);
        gameState.setActiveHeroPlayer2Id(enemyHero.getUuid());

        // Em COMBAT_START:
        // 1. Aura de Oráculo Real concede +2 Ataque e +1 Defesa a todos os Guardiões do Trono (Oráculo: 6->8 Atk, 0->1 Def; Portador: 5->7 Atk, 0->1 Def)
        // 2. Oráculo Real invoca Fortaleça sua Fé:
        //    - Seu Herói (Oráculo) recebe +2 Ataque até o Final do Combate (8->10 Atk)
        //    - Seus Transformadores recebem +1 Ataque até o Final da Partida (Oráculo: 10->11 Atk; Portador: 7->8 Atk)
        engine.setTurnPhase(TurnPhase.COMBAT_START);
        oraculo.getEffects().forEach(effect -> {
            if (effect.predicate(oraculo, TurnPhase.COMBAT_START)) {
                effect.resolve(engine, oraculo);
            }
        });
        engine.getResolutionQueue().resolveAll(engine);

        assertEquals(11, oraculo.getAtaque(), "Oráculo Real deve ter 11 de Ataque (6 base + 2 Aura + 2 temporário + 1 permanente de Transformador).");
        assertEquals(1, oraculo.getDefesa(), "Oráculo Real deve ter 1 de Defesa pela Aura.");
        assertEquals(8, portadorBench.getAtaque(), "Portador da Chama na bancada deve ter 8 de Ataque (5 base + 2 Aura + 1 permanente de Transformador).");
        assertEquals(1, portadorBench.getDefesa(), "Portador da Chama deve ter 1 de Defesa pela Aura.");

        // No Final do Combate, apenas o +2 temporário de Fortaleça sua Fé expira no Oráculo Real (11 -> 9), mantendo o +1 permanente!
        engine.runCombatEndCleanup();
        assertEquals(9, oraculo.getAtaque(), "Após o Final do Combate, Oráculo Real deve manter 9 de Ataque (6 base + 2 Aura + 1 permanente).");
        assertEquals(8, portadorBench.getAtaque(), "Após o Final do Combate, Portador da Chama deve manter 8 de Ataque.");
    }

    @Test
    void testPortadorDaChamaAndOrdemDivina() {
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

        HeroCard portador = new HeroCard();
        portador.setUuid(UUID.randomUUID());
        portador.setCardName("Portador da Chama");
        portador.setOwnerId(p1.getUuid());
        portador.setAtaque(5);
        portador.setDefesa(0);
        portador.setVidaMaxima(16);
        portador.setVidaAtual(10); // ferido
        portador.setEstaVivo(true);
        portador.setRacas(new ArrayList<>(Collections.singletonList(Raca.GUARDIOES_DO_TRONO)));
        portador.setClasse(new ArrayList<>(Collections.singletonList(Classe.TRANSFORMADOR)));
        CardFactory.buildCardLogic(portador);
        deck1.getHeroList().add(portador);
        gameState.setActiveHeroPlayer1Id(portador.getUuid());

        HeroCard enemyHero = new HeroCard();
        enemyHero.setUuid(UUID.randomUUID());
        enemyHero.setCardName("Inimigo Teste");
        enemyHero.setOwnerId(p2.getUuid());
        enemyHero.setVidaMaxima(20);
        enemyHero.setVidaAtual(20);
        enemyHero.setEstaVivo(true);
        deck2.getHeroList().add(enemyHero);
        gameState.setActiveHeroPlayer2Id(enemyHero.getUuid());

        Card cardDiscard1 = new Card();
        cardDiscard1.setUuid(UUID.randomUUID());
        cardDiscard1.setCardName("Ordem Divina");
        cardDiscard1.setCardType(CardType.THEURGY);
        cardDiscard1.setColor(Colors.YELLOW);
        cardDiscard1.setOwnerId(p1.getUuid());
        cardDiscard1.setZoneId(ZoneId.DISCARD);
        CardFactory.buildCardLogic(cardDiscard1);
        deck1.getCardList().add(cardDiscard1);

        Card cardDiscard2 = new Card();
        cardDiscard2.setUuid(UUID.randomUUID());
        cardDiscard2.setCardName("Disciplina");
        cardDiscard2.setCardType(CardType.ACTION);
        cardDiscard2.setColor(Colors.YELLOW);
        cardDiscard2.setOwnerId(p1.getUuid());
        cardDiscard2.setZoneId(ZoneId.DISCARD);
        CardFactory.buildCardLogic(cardDiscard2);
        deck1.getCardList().add(cardDiscard2);

        // Dispara Início do Combate do Portador da Chama
        engine.setTurnPhase(TurnPhase.COMBAT_START);
        portador.getEffects().forEach(effect -> {
            if (effect.predicate(portador, TurnPhase.COMBAT_START)) {
                effect.resolve(engine, portador);
            }
        });

        assertEquals(12, portador.getVidaAtual(), "Portador da Chama deve recuperar 2 de Vida no início do combate (10 -> 12).");
        assertNotNull(gameState.getPendingChoice(), "Deve solicitar escolha da 1ª carta do descarte.");
        engine.submitPlayerChoice(p1.getUuid(), cardDiscard1.getUuid());

        assertNotNull(gameState.getPendingChoice(), "Deve solicitar escolha da 2ª carta do descarte.");
        engine.submitPlayerChoice(p1.getUuid(), cardDiscard2.getUuid());

        assertEquals(ZoneId.HAND, cardDiscard1.getZoneId(), "A 1ª carta escolhida deve estar na mão.");
        assertEquals(ZoneId.HAND, cardDiscard2.getZoneId(), "A 2ª carta escolhida deve estar na mão.");

        // Agora testa Ordem Divina jogada por um Guardião do Trono escolhendo a si mesmo (cumulativo: +4 Ataque e +4 Cura)
        // Reconfigura os heróis ativos em batalha com Inimigo de 0 Ataque e 20 de Vida para validar a cura e o Ataque
        portador.setVidaAtual(12);
        enemyHero.setVidaAtual(20);
        enemyHero.setAtaque(0);
        gameState.setActiveHeroPlayer1Id(portador.getUuid());
        gameState.setActiveHeroPlayer2Id(enemyHero.getUuid());
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        cardDiscard1.getEffects().forEach(effect -> {
            if (effect.predicate(cardDiscard1, TurnPhase.RESOLUTION)) {
                effect.resolve(engine, cardDiscard1);
            }
        });
        assertNotNull(gameState.getPendingChoice(), "Ordem Divina deve pedir para escolher um Herói aliado.");
        assertEquals(7, portador.getAtaque(), "Antes da escolha, o bônus de Guardião do Trono já deve ter concedido +2 de Ataque (5 -> 7).");
        assertEquals(14, portador.getVidaAtual(), "Antes da escolha, o bônus de Guardião do Trono já deve ter curado 2 de Vida (12 -> 14).");

        // Ao submeter a escolha, Portador recebe mais +2 de Ataque (7 -> 9) e +2 de Vida (14 -> 16),
        // causa 9 de dano físico ao inimigo (20 -> 11), e no Final do Combate seu Ataque volta a 5!
        engine.submitPlayerChoice(p1.getUuid(), portador.getUuid());

        assertEquals(16, portador.getVidaAtual(), "Portador da Chama deve ter curado 4 pontos de Vida no total (12 -> 16).");
        assertEquals(11, enemyHero.getVidaAtual(), "Inimigo deve ter sofrido 9 de dano físico do Portador da Chama bufado em +4 (5 + 4 = 9).");
        assertEquals(5, portador.getAtaque(), "O bônus de +4 de Ataque da Ordem Divina deve expirar no Final do Combate.");
    }

    @Test
    void testConstructoFantasmagoricoSaqueRapidoArtefatoCortanteAndDisciplina() {
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

        HeroCard constructo = new HeroCard();
        constructo.setUuid(UUID.randomUUID());
        constructo.setCardName("Constructo Fantasmagórico Guardião Perpétuo");
        constructo.setOwnerId(p1.getUuid());
        constructo.setAtaque(6);
        constructo.setDefesa(0);
        constructo.setVidaMaxima(17);
        constructo.setVidaAtual(13); // ferido
        constructo.setEstaVivo(true);
        constructo.setRacas(new ArrayList<>(Arrays.asList(Raca.GUARDIOES_DO_TRONO, Raca.CONSTRUCTO)));
        constructo.setClasse(new ArrayList<>(Collections.singletonList(Classe.TRANSFORMADOR)));
        CardFactory.buildCardLogic(constructo);
        deck1.getHeroList().add(constructo);
        gameState.setActiveHeroPlayer1Id(constructo.getUuid());

        HeroCard enemyHero = new HeroCard();
        enemyHero.setUuid(UUID.randomUUID());
        enemyHero.setCardName("Inimigo Teste");
        enemyHero.setOwnerId(p2.getUuid());
        enemyHero.setAtaque(5);
        enemyHero.setDefesa(2);
        enemyHero.setVidaMaxima(20);
        enemyHero.setVidaAtual(20);
        enemyHero.setMaxWeaponSlots(2);
        enemyHero.setEstaVivo(true);
        deck2.getHeroList().add(enemyHero);
        gameState.setActiveHeroPlayer2Id(enemyHero.getUuid());

        // Testa GAME_START: Empunhadura Quádrupla
        engine.setTurnPhase(TurnPhase.GAME_START);
        constructo.getEffects().forEach(effect -> {
            if (effect.predicate(constructo, TurnPhase.GAME_START)) {
                effect.resolve(engine, constructo);
            }
        });
        assertEquals(4, constructo.getMaxWeaponSlots(), "Constructo Fantasmagórico deve ter Empunhadura Quádrupla (4 slots).");

        // Prepara Artefato Cortante na mão de P1 e Saque Rápido para jogar
        Card artefato = new Card();
        artefato.setUuid(UUID.randomUUID());
        artefato.setCardName("Artefato Cortante");
        artefato.setCardType(CardType.ITEM);
        artefato.setItemType(ItemType.ARMA);
        artefato.setEmpunhadura(2);
        artefato.setColor(Colors.YELLOW);
        artefato.setOwnerId(p1.getUuid());
        artefato.setZoneId(ZoneId.HAND);
        artefato.setOrdem(new ArrayList<>(Collections.singletonList(Ordem.GUARDAO_DO_TRONO)));
        CardFactory.buildCardLogic(artefato);
        deck1.getCardList().add(artefato);

        Card saqueRapido = new Card();
        saqueRapido.setUuid(UUID.randomUUID());
        saqueRapido.setCardName("Saque Rápido");
        saqueRapido.setCardType(CardType.THEURGY);
        saqueRapido.setColor(Colors.YELLOW);
        saqueRapido.setOwnerId(p1.getUuid());
        saqueRapido.setZoneId(ZoneId.RESOLVE);
        CardFactory.buildCardLogic(saqueRapido);
        deck1.getCardList().add(saqueRapido);
        gameState.getPlayer1PlayedCards().add(saqueRapido);

        // Equipa 2 itens no Herói Inimigo para testar Disciplina depois
        Card enemyItem1 = new Card();
        enemyItem1.setUuid(UUID.randomUUID());
        enemyItem1.setCardName("Lamina de Cristal");
        enemyItem1.setCardType(CardType.ITEM);
        enemyItem1.setItemType(ItemType.ARMA);
        enemyItem1.setEmpunhadura(1);
        enemyItem1.setOwnerId(p2.getUuid());
        enemyItem1.setZoneId(ZoneId.RESOLVE);
        CardFactory.buildCardLogic(enemyItem1);
        deck2.getCardList().add(enemyItem1);

        Card enemyItem2 = new Card();
        enemyItem2.setUuid(UUID.randomUUID());
        enemyItem2.setCardName("Escudo ardente");
        enemyItem2.setCardType(CardType.ITEM);
        enemyItem2.setItemType(ItemType.ESCUDO);
        enemyItem2.setOwnerId(p2.getUuid());
        enemyItem2.setZoneId(ZoneId.RESOLVE);
        CardFactory.buildCardLogic(enemyItem2);
        deck2.getCardList().add(enemyItem2);

        engine.setTurnPhase(TurnPhase.RESOLUTION);
        enemyItem1.getEffects().forEach(eff -> eff.resolve(engine, enemyItem1));
        enemyItem2.getEffects().forEach(eff -> eff.resolve(engine, enemyItem2));
        assertEquals(6, enemyHero.getAtaque(), "Inimigo deve ter 6 de Ataque com Lamina de Cristal.");
        assertEquals(3, enemyHero.getDefesa(), "Inimigo deve ter 3 de Defesa com Escudo ardente.");

        // Resolve Saque Rápido -> Escolhe Artefato Cortante da mão
        saqueRapido.getEffects().forEach(eff -> {
            if (eff.predicate(saqueRapido, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, saqueRapido);
            }
        });
        assertNotNull(gameState.getPendingChoice(), "Saque Rápido deve pedir para escolher uma Arma da mão.");
        engine.submitPlayerChoice(p1.getUuid(), artefato.getUuid());

        // Nota: submitPlayerChoice já retomou a fila e terminou o combate!
        // Vamos verificar:
        // Durante RESOLUTION:
        // - Constructo recebeu +3 de Ataque do Saque Rápido, +3 de Ataque e +1 de Defesa do Artefato Cortante, e +2 de Ataque e +2 de Vida do efeito Ao jogar Arma!
        // - No COMBAT_END: Artefato Cortante curou todos os aliados em 2! E os buffs temporários (+3 do Saque Rápido e +2 do Ao jogar Arma) expiraram, restando 6 base + 3 permanente do Artefato Cortante = 9 de Ataque!
        assertEquals(9, constructo.getAtaque(), "Após o combate, Constructo deve manter 9 de Ataque (6 base + 3 do Artefato Cortante equipado).");
        assertEquals(1, constructo.getDefesa(), "Constructo deve manter 1 de Defesa do Artefato Cortante equipado.");
        assertEquals(ZoneId.ATTACHED, artefato.getZoneId(), "Artefato Cortante deve permanecer equipado no Constructo.");

        // Agora testa Disciplina em um novo turno para desequipar as 2 cartas do Inimigo
        gameState.setActiveHeroPlayer1Id(constructo.getUuid());
        gameState.setActiveHeroPlayer2Id(enemyHero.getUuid());
        constructo.setVidaAtual(15);

        Card disciplina = new Card();
        disciplina.setUuid(UUID.randomUUID());
        disciplina.setCardName("Disciplina");
        disciplina.setCardType(CardType.ACTION);
        disciplina.setColor(Colors.YELLOW);
        disciplina.setOwnerId(p1.getUuid());
        disciplina.setZoneId(ZoneId.RESOLVE);
        CardFactory.buildCardLogic(disciplina);
        deck1.getCardList().add(disciplina);

        engine.setTurnPhase(TurnPhase.RESOLUTION);
        disciplina.getEffects().forEach(eff -> {
            if (eff.predicate(disciplina, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, disciplina);
            }
        });

        assertNotNull(gameState.getPendingChoice(), "Disciplina deve solicitar a 1ª carta inimiga para desequipar.");
        // Para testar apenas a resolução sem avançar a fase inteira, testamos via submitPlayerChoice mantendo fase
        engine.submitPlayerChoice(p1.getUuid(), enemyItem1.getUuid());
        assertNotNull(gameState.getPendingChoice(), "Disciplina deve solicitar a 2ª carta inimiga para desequipar.");
        engine.submitPlayerChoice(p1.getUuid(), enemyItem2.getUuid());
        assertNull(gameState.getPendingChoice(), "Após desequipar 2 cartas, não deve haver mais escolhas pendentes.");

        assertEquals(ZoneId.DISCARD, enemyItem1.getZoneId(), "A 1ª carta inimiga desequipada deve ir para o descarte.");
        assertEquals(ZoneId.DISCARD, enemyItem2.getZoneId(), "A 2ª carta inimiga desequipada deve ir para o descarte.");
        assertEquals(5, enemyHero.getAtaque(), "O Ataque do inimigo deve voltar a 5 após perder a Lamina de Cristal.");
        assertEquals(2, enemyHero.getDefesa(), "A Defesa do inimigo deve voltar a 2 após perder o Escudo ardente.");
    }
}

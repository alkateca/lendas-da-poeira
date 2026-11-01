package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NewSpellsActionsTheurgiesIntegrationTest {

    @Test
    void testFogoPurificadorConditionalDebuffByRace() {
        GameEngine engine = new GameEngine();
        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        engine.setPlayer1(p1);

        Player p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        engine.setPlayer2(p2);

        HeroCard enemyHero = new HeroCard();
        enemyHero.setUuid(UUID.randomUUID());
        enemyHero.setDefesa(5);
        enemyHero.setAtaque(5);
        enemyHero.setRacas(new ArrayList<>(java.util.List.of(Raca.CONSTRUCTO)));
        p2.getCurrentDeck().getHeroList().add(enemyHero);
        engine.getGameState().setActiveHeroPlayer2Id(enemyHero.getUuid());

        Card fogo = new Card();
        fogo.setUuid(UUID.randomUUID());
        fogo.setCardName("Fogo Purificador");
        fogo.setOwnerId(p1.getUuid());
        fogo.setDescricao("Se o inimigo for Constructo, recebe -2 de Defesa.");
        CardFactory.buildCardLogic(fogo);

        engine.setTurnPhase(TurnPhase.RESOLUTION);
        fogo.getEffects().forEach(eff -> {
            if (eff.predicate(fogo, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, fogo);
            }
        });

        assertEquals(3, enemyHero.getDefesa(), "A defesa do inimigo Construto deve ter reduzido em -2.");
    }

    @Test
    void testDancaDasMilLaminasSetSpiritAndRestore() {
        GameEngine engine = new GameEngine();
        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        engine.setPlayer1(p1);

        Player p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        engine.setPlayer2(p2);

        HeroCard enemyHero = new HeroCard();
        enemyHero.setUuid(UUID.randomUUID());
        enemyHero.setEspirito(6);
        p2.getCurrentDeck().getHeroList().add(enemyHero);
        engine.getGameState().setActiveHeroPlayer2Id(enemyHero.getUuid());

        Card danca = new Card();
        danca.setUuid(UUID.randomUUID());
        danca.setCardName("Dança das Mil Lâminas");
        danca.setOwnerId(p1.getUuid());
        danca.setDescricao("Define o Espírito inimigo para 1.");
        CardFactory.buildCardLogic(danca);

        engine.setTurnPhase(TurnPhase.RESOLUTION);
        danca.getEffects().forEach(eff -> {
            if (eff.predicate(danca, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, danca);
            }
        });

        assertEquals(1, enemyHero.getEspirito(), "O espírito do inimigo deve ser definido para 1 na resolução.");

        engine.runCombatEndCleanup();
        assertEquals(6, enemyHero.getEspirito(), "O espírito original deve ser restaurado após o final do combate.");
    }

    @Test
    void testVereditoInquisitorialConditionalDamageAndVulnerability() {
        GameEngine engine = new GameEngine();
        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        engine.setPlayer1(p1);

        Player p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        engine.setPlayer2(p2);

        HeroCard myHero = new HeroCard();
        myHero.setUuid(UUID.randomUUID());
        myHero.setVidaAtual(5);
        p1.getCurrentDeck().getHeroList().add(myHero);
        engine.getGameState().setActiveHeroPlayer1Id(myHero.getUuid());

        HeroCard enemyHero = new HeroCard();
        enemyHero.setUuid(UUID.randomUUID());
        enemyHero.setVidaAtual(15);
        enemyHero.setVulnerabilidade(0);
        p2.getCurrentDeck().getHeroList().add(enemyHero);
        engine.getGameState().setActiveHeroPlayer2Id(enemyHero.getUuid());

        Card veredito = new Card();
        veredito.setUuid(UUID.randomUUID());
        veredito.setCardName("Veredito Inquisitorial");
        veredito.setOwnerId(p1.getUuid());
        veredito.setDescricao("Se o inimigo tiver mais HP atual, cause 3 de dano direto e +1 de vulnerabilidade.");
        CardFactory.buildCardLogic(veredito);

        engine.setTurnPhase(TurnPhase.RESOLUTION);
        veredito.getEffects().forEach(eff -> {
            if (eff.predicate(veredito, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, veredito);
            }
        });

        assertEquals(12, enemyHero.getVidaAtual(), "O inimigo deve ter sofrido 3 de dano direto.");
        assertEquals(1, enemyHero.getVulnerabilidade(), "O inimigo deve ter recebido +1 de vulnerabilidade.");
    }

    @Test
    void testPlayTheurgyEqualizeEnemyAttack() {
        GameEngine engine = new GameEngine();
        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        engine.setPlayer1(p1);

        Player p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        engine.setPlayer2(p2);

        HeroCard myHero = new HeroCard();
        myHero.setUuid(UUID.randomUUID());
        myHero.setAtaque(4);
        p1.getCurrentDeck().getHeroList().add(myHero);
        engine.getGameState().setActiveHeroPlayer1Id(myHero.getUuid());

        HeroCard enemyHero = new HeroCard();
        enemyHero.setUuid(UUID.randomUUID());
        enemyHero.setAtaque(10);
        p2.getCurrentDeck().getHeroList().add(enemyHero);
        engine.getGameState().setActiveHeroPlayer2Id(enemyHero.getUuid());

        Card liturgia = new Card();
        liturgia.setUuid(UUID.randomUUID());
        liturgia.setCardName("Liturgia da Balança");
        liturgia.setOwnerId(p1.getUuid());
        liturgia.setDescricao("Equaliza o ataque do inimigo se for maior.");
        CardFactory.buildCardLogic(liturgia);

        engine.setTurnPhase(TurnPhase.RESOLUTION);
        liturgia.getEffects().forEach(eff -> {
            if (eff.predicate(liturgia, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, liturgia);
            }
        });

        assertEquals(4, enemyHero.getAtaque(), "O ataque do inimigo deve ser reduzido para 4, igualando o do aliado.");

        engine.runCombatEndCleanup();
        assertEquals(10, enemyHero.getAtaque(), "O ataque original do inimigo deve ser restaurado no fim do combate.");
    }
}

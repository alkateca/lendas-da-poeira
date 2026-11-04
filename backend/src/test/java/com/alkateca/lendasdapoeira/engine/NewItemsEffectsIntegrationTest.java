package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class NewItemsEffectsIntegrationTest {

    @Test
    void testBolsaDeArmadilhasAndTransferEffect() {
        GameEngine engine = new GameEngine();
        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        p1.setName("Mago 1");
        engine.setPlayer1(p1);

        Player p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        p2.setName("Mago 2");
        engine.setPlayer2(p2);

        HeroCard h1 = new HeroCard();
        h1.setUuid(UUID.randomUUID());
        h1.setCardName("Herói 1");
        h1.setEstaVivo(true);
        h1.setVidaAtual(10);
        h1.setEstaAtivo(true);
        h1.setMaxWeaponSlots(4);
        p1.getCurrentDeck().getHeroList().add(h1);
        engine.getGameState().setActiveHeroPlayer1Id(h1.getUuid());

        HeroCard h2 = new HeroCard();
        h2.setUuid(UUID.randomUUID());
        h2.setCardName("Herói 2");
        h2.setEstaVivo(true);
        h2.setEstaAtivo(true);
        h2.setMaxWeaponSlots(4);
        h2.setVidaAtual(10);
        p1.getCurrentDeck().getHeroList().add(h2);

        Card bolsa = new Card();
        bolsa.setUuid(UUID.randomUUID());
        bolsa.setCardName("Bolsa de Armadilhas do Mestre Caçador");
        bolsa.setOwnerId(p1.getUuid());
        bolsa.setItemType(ItemType.FERRAMENTA);
        bolsa.setEmpunhadura(0);
        bolsa.setDescricao("Equipar: +2 de Ataque e +2 de Defesa.\nInício do Combate: Jogue uma Ferramenta do baralho.\nFinal do Combate: Anexe a outro Herói Aliado ativo.");
        CardFactory.buildCardLogic(bolsa);

        bolsa.setAttachedToCardId(h1.getUuid());
        bolsa.setZoneId(ZoneId.ATTACHED);
        p1.getCurrentDeck().getCardList().add(bolsa);

        engine.setTurnPhase(TurnPhase.COMBAT_END);
        bolsa.getEffects().forEach(eff -> {
            if (eff.predicate(bolsa, TurnPhase.COMBAT_END)) {
                eff.resolve(engine, bolsa);
            }
        });

        assertEquals(h2.getUuid(), bolsa.getAttachedToCardId(), "A bolsa deve ter sido transferida para o segundo herói aliado ativo.");
    }

    @Test
    void testBastaoDoMongeBonusDamageAndDetach() {
        GameEngine engine = new GameEngine();
        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        engine.setPlayer1(p1);

        HeroCard h1 = new HeroCard();
        h1.setUuid(UUID.randomUUID());
        h1.setDanoBonus(0);
        h1.setMaxWeaponSlots(2);
        p1.getCurrentDeck().getHeroList().add(h1);
        engine.getGameState().setActiveHeroPlayer1Id(h1.getUuid());

        Card bastao = new Card();
        bastao.setUuid(UUID.randomUUID());
        bastao.setCardName("Bastão do Monge Incandescente");
        bastao.setOwnerId(p1.getUuid());
        bastao.setItemType(ItemType.ARMA);
        bastao.setEmpunhadura(1);
        bastao.setDescricao("Equipar: +2 de Dano Bônus.");
        CardFactory.buildCardLogic(bastao);
        p1.getCurrentDeck().getCardList().add(bastao);

        engine.setTurnPhase(TurnPhase.RESOLUTION);
        bastao.getEffects().forEach(eff -> {
            if (eff.predicate(bastao, TurnPhase.RESOLUTION)) {
                eff.resolve(engine, bastao);
            }
        });

        assertEquals(2, h1.getDanoBonus(), "O herói deve ter recebido +2 de dano bônus.");

        bastao.getEffects().forEach(eff -> eff.onDetach(engine, bastao, h1));
        assertEquals(0, h1.getDanoBonus(), "O dano bônus deve ser revertido ao desanexar.");
    }

    @Test
    void testPlacasDeDraconianoBuffPerDeadAlly() {
        GameEngine engine = new GameEngine();
        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        engine.setPlayer1(p1);

        HeroCard h1 = new HeroCard();
        h1.setUuid(UUID.randomUUID());
        h1.setEspirito(2);
        h1.setAtaque(5);
        h1.setEstaVivo(true);
        h1.setVidaAtual(10);
        p1.getCurrentDeck().getHeroList().add(h1);
        engine.getGameState().setActiveHeroPlayer1Id(h1.getUuid());

        HeroCard deadHero = new HeroCard();
        deadHero.setUuid(UUID.randomUUID());
        deadHero.setEstaVivo(false);
        deadHero.setVidaAtual(0);
        p1.getCurrentDeck().getHeroList().add(deadHero);

        HeroCard deadHero2 = new HeroCard();
        deadHero2.setUuid(UUID.randomUUID());
        deadHero2.setEstaVivo(false);
        deadHero2.setVidaAtual(-2);
        p1.getCurrentDeck().getHeroList().add(deadHero2);

        Card placas = new Card();
        placas.setUuid(UUID.randomUUID());
        placas.setCardName("Placas de Cristal Draconiano");
        placas.setOwnerId(p1.getUuid());
        placas.setDescricao("Início do Combate: Receba +1 de Espírito e +2 de Ataque para cada herói aliado morto.");
        CardFactory.buildCardLogic(placas);

        engine.setTurnPhase(TurnPhase.COMBAT_START);
        placas.getEffects().forEach(eff -> {
            if (eff.predicate(placas, TurnPhase.COMBAT_START)) {
                eff.resolve(engine, placas);
            }
        });

        assertEquals(4, h1.getEspirito(), "Espírito deve aumentar em +2 (1 por cada 2 mortos).");
        assertEquals(9, h1.getAtaque(), "Ataque deve aumentar em +4 (2 por cada 2 mortos).");
    }

    @Test
    void testFilacteriaDrainPerZombieAlly() {
        GameEngine engine = new GameEngine();
        Player p1 = new Player();
        p1.setUuid(UUID.randomUUID());
        engine.setPlayer1(p1);

        Player p2 = new Player();
        p2.setUuid(UUID.randomUUID());
        engine.setPlayer2(p2);

        HeroCard myHero = new HeroCard();
        myHero.setUuid(UUID.randomUUID());
        myHero.setVidaAtual(10);
        myHero.setVidaMaxima(20);
        myHero.setEstaVivo(true);
        p1.getCurrentDeck().getHeroList().add(myHero);
        engine.getGameState().setActiveHeroPlayer1Id(myHero.getUuid());

        HeroCard zombie1 = new HeroCard();
        zombie1.setUuid(UUID.randomUUID());
        zombie1.setEstaVivo(true);
        zombie1.setVidaAtual(5);
        zombie1.setOrdem(new ArrayList<>(java.util.List.of(Ordem.ZUMBI)));
        p1.getCurrentDeck().getHeroList().add(zombie1);

        HeroCard enemyHero = new HeroCard();
        enemyHero.setUuid(UUID.randomUUID());
        enemyHero.setVidaAtual(15);
        p2.getCurrentDeck().getHeroList().add(enemyHero);
        engine.getGameState().setActiveHeroPlayer2Id(enemyHero.getUuid());

        Card filacteria = new Card();
        filacteria.setUuid(UUID.randomUUID());
        filacteria.setCardName("Filactéria das Areias Negras");
        filacteria.setOwnerId(p1.getUuid());
        filacteria.setDescricao("Final do Combate: Cure 1 e cause 1 de dano direto por aliado Zumbi.");
        CardFactory.buildCardLogic(filacteria);

        engine.setTurnPhase(TurnPhase.COMBAT_END);
        filacteria.getEffects().forEach(eff -> {
            if (eff.predicate(filacteria, TurnPhase.COMBAT_END)) {
                eff.resolve(engine, filacteria);
            }
        });

        assertEquals(11, myHero.getVidaAtual(), "O herói aliado deve ter recuperado 1 de vida por 1 Zumbi.");
        assertEquals(14, enemyHero.getVidaAtual(), "O herói inimigo deve ter sofrido 1 de dano direto.");
    }
}

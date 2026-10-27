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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GrilhoesPesadosIntegrationTest {

    @Test
    void testGrilhoesPesadosWhenEnemyNotWoundedUnequipsOnlyOneItem() {
        // Arrange
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

        HeroCard p1Hero = new HeroCard();
        p1Hero.setUuid(UUID.randomUUID());
        p1Hero.setEstaVivo(true);
        p1Hero.setZoneId(ZoneId.BATTLE);
        deck1.getHeroList().add(p1Hero);
        gameState.setActiveHeroPlayer1Id(p1Hero.getUuid());

        // Herói inimigo NÃO ferido (vidaAtual == vidaMaxima)
        HeroCard enemyHero = new HeroCard();
        enemyHero.setUuid(UUID.randomUUID());
        enemyHero.setCardName("Defensor Não Ferido");
        enemyHero.setOwnerId(p2.getUuid());
        enemyHero.setVidaMaxima(20);
        enemyHero.setVidaAtual(20);
        enemyHero.setEstaVivo(true);
        enemyHero.setZoneId(ZoneId.BATTLE);
        deck2.getHeroList().add(enemyHero);
        gameState.setActiveHeroPlayer2Id(enemyHero.getUuid());

        // Equipa 2 itens no herói inimigo
        Card item1 = createItem("Espada de Teste 1", p2.getUuid(), enemyHero.getUuid());
        Card item2 = createItem("Espada de Teste 2", p2.getUuid(), enemyHero.getUuid());
        deck2.getCardList().add(item1);
        deck2.getCardList().add(item2);

        Card grilhoes = new Card();
        grilhoes.setUuid(UUID.randomUUID());
        grilhoes.setCardName("Grilhões Pesados");
        grilhoes.setCardType(CardType.ACTION);
        grilhoes.setColor(Colors.BLACK);
        grilhoes.setOwnerId(p1.getUuid());
        grilhoes.setZoneId(ZoneId.RESOLVE);
        CardFactory.buildCardLogic(grilhoes);

        engine.setTurnPhase(TurnPhase.RESOLUTION);

        // Act: Enfileira e resolve o efeito
        grilhoes.getEffects().forEach(eff -> engine.getResolutionQueue().enqueue(eff, grilhoes));
        engine.getResolutionQueue().resolveAll(engine);

        // Assert: Solicita 1 escolha de desequipar
        assertNotNull(gameState.getPendingChoice(), "Deve solicitar escolha para desequipar 1 item.");
        engine.submitPlayerChoice(p1.getUuid(), item1.getUuid());

        assertEquals(ZoneId.DISCARD, item1.getZoneId(), "O primeiro item deve ser desanexado para o descarte.");
        assertEquals(ZoneId.ATTACHED, item2.getZoneId(), "O segundo item deve continuar anexado.");
        assertNull(gameState.getPendingChoice(), "Não deve solicitar segunda escolha quando o herói não está ferido.");
    }

    @Test
    void testGrilhoesPesadosWhenEnemyWoundedUnequipsTwoItems() {
        // Arrange
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

        HeroCard p1Hero = new HeroCard();
        p1Hero.setUuid(UUID.randomUUID());
        p1Hero.setEstaVivo(true);
        p1Hero.setZoneId(ZoneId.BATTLE);
        deck1.getHeroList().add(p1Hero);
        gameState.setActiveHeroPlayer1Id(p1Hero.getUuid());

        // Herói inimigo FERIDO (vidaAtual < vidaMaxima)
        HeroCard enemyHero = new HeroCard();
        enemyHero.setUuid(UUID.randomUUID());
        enemyHero.setCardName("Defensor Ferido");
        enemyHero.setOwnerId(p2.getUuid());
        enemyHero.setVidaMaxima(20);
        enemyHero.setVidaAtual(12); // Ferido!
        enemyHero.setEstaVivo(true);
        enemyHero.setZoneId(ZoneId.BATTLE);
        deck2.getHeroList().add(enemyHero);
        gameState.setActiveHeroPlayer2Id(enemyHero.getUuid());

        // Equipa 2 itens no herói inimigo
        Card item1 = createItem("Espada de Teste 1", p2.getUuid(), enemyHero.getUuid());
        Card item2 = createItem("Espada de Teste 2", p2.getUuid(), enemyHero.getUuid());
        deck2.getCardList().add(item1);
        deck2.getCardList().add(item2);

        Card grilhoes = new Card();
        grilhoes.setUuid(UUID.randomUUID());
        grilhoes.setCardName("Grilhões Pesados");
        grilhoes.setCardType(CardType.ACTION);
        grilhoes.setColor(Colors.BLACK);
        grilhoes.setOwnerId(p1.getUuid());
        grilhoes.setZoneId(ZoneId.RESOLVE);
        CardFactory.buildCardLogic(grilhoes);

        engine.setTurnPhase(TurnPhase.RESOLUTION);

        // Act: Enfileira e resolve o efeito
        grilhoes.getEffects().forEach(eff -> engine.getResolutionQueue().enqueue(eff, grilhoes));
        engine.getResolutionQueue().resolveAll(engine);

        // Assert 1: Primeira escolha (item 1)
        assertNotNull(gameState.getPendingChoice(), "Deve solicitar a primeira escolha de item.");
        engine.submitPlayerChoice(p1.getUuid(), item1.getUuid());
        assertEquals(ZoneId.DISCARD, item1.getZoneId(), "O item 1 deve estar no descarte.");

        // Assert 2: Segunda escolha (item 2, por estar ferido)
        assertNotNull(gameState.getPendingChoice(), "Deve solicitar a segunda escolha de item por estar ferido.");
        engine.submitPlayerChoice(p1.getUuid(), item2.getUuid());
        assertEquals(ZoneId.DISCARD, item2.getZoneId(), "O item 2 deve estar no descarte.");
    }

    private Card createItem(String name, UUID ownerId, UUID attachedToHeroId) {
        Card item = new Card();
        item.setUuid(UUID.randomUUID());
        item.setCardName(name);
        item.setCardType(CardType.ITEM);
        item.setItemType(ItemType.ARMA);
        item.setOwnerId(ownerId);
        item.setZoneId(ZoneId.ATTACHED);
        item.setAttachedToCardId(attachedToHeroId);
        return item;
    }
}

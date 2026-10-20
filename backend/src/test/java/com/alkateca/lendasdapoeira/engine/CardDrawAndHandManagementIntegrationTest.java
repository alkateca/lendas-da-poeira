package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CardDrawAndHandManagementIntegrationTest {

    private Player createPlayerWithDeckAndHeroes(String name) {
        Player player = new Player();
        player.setUuid(UUID.randomUUID());
        player.setName(name);

        Deck deck = new Deck();
        deck.setHeroList(new ArrayList<>());
        deck.setCardList(new ArrayList<>());

        // 3 Heróis vivos
        for (int i = 1; i <= 3; i++) {
            HeroCard hero = new HeroCard();
            hero.setUuid(UUID.randomUUID());
            hero.setCardName("Herói " + name + " " + i);
            hero.setOwnerId(player.getUuid());
            hero.setVidaMaxima(20);
            hero.setVidaAtual(20);
            hero.setAtaque(5);
            hero.setDefesa(2);
            hero.setEspirito(2);
            hero.setEstaVivo(true);
            hero.setEstaAtivo(true);
            deck.getHeroList().add(hero);
        }

        // 15 cartas de baralho
        for (int i = 1; i <= 15; i++) {
            Card card = new Card();
            card.setUuid(UUID.randomUUID());
            card.setCardName("Carta " + name + " " + i);
            card.setOwnerId(player.getUuid());
            card.setCardType(CardType.SPELL);
            card.setItemType(ItemType.NENHUM);
            card.setZoneId(ZoneId.DECK);
            deck.getCardList().add(card);
        }

        player.setCurrentDeck(deck);
        return player;
    }

    private long countHandCards(Player player) {
        return player.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.HAND)
                .count();
    }

    @Test
    @DisplayName("Issue #1: Valida que a mão inicial tem 5 cartas e não há compra duplicada em submitHeroChoice ou discard")
    void testInitialHandAndNoDuplicatedDrawAcrossTurns() {
        // Arrange
        Player p1 = createPlayerWithDeckAndHeroes("Jogador 1");
        Player p2 = createPlayerWithDeckAndHeroes("Jogador 2");
        GameEngine engine = new GameEngine(p1, p2);

        // Act 1: Início da partida
        engine.startGame();

        // Assert 1: Após startGame, cada jogador deve possuir exatamente 5 cartas na mão
        assertEquals(5, countHandCards(p1), "Jogador 1 deve iniciar com exatamente 5 cartas.");
        assertEquals(5, countHandCards(p2), "Jogador 2 deve iniciar com exatamente 5 cartas.");
        assertEquals(TurnPhase.CHOICE, engine.getTurnPhase());

        // Act 2: Jogador ativo escolhe heróis para o combate
        UUID activePlayerId = engine.getGameState().getActiveTurnPlayerId();
        HeroCard ownHero = (activePlayerId.equals(p1.getUuid()) ? p1 : p2).getCurrentDeck().getHeroList().get(0);
        HeroCard enemyHero = (activePlayerId.equals(p1.getUuid()) ? p2 : p1).getCurrentDeck().getHeroList().get(0);

        engine.submitHeroChoice(activePlayerId, ownHero.getUuid(), enemyHero.getUuid());

        // Assert 2: submitHeroChoice NÃO deve comprar mais 5 cartas incondicionais
        assertEquals(5, countHandCards(p1), "Jogador 1 deve manter 5 cartas após submitHeroChoice.");
        assertEquals(5, countHandCards(p2), "Jogador 2 deve manter 5 cartas após submitHeroChoice.");
        assertEquals(TurnPhase.PREPARATION, engine.getTurnPhase());

        // Act 3: Jogadores alocam cartas secretas na fase PREPARATION
        List<Card> p1Hand = p1.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.HAND)
                .toList();
        List<Card> p2Hand = p2.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.HAND)
                .toList();

        // P1 joga 2 cartas da mão
        List<UUID> p1Played = List.of(p1Hand.get(0).getUuid(), p1Hand.get(1).getUuid());
        engine.submitPreparationCards(p1.getUuid(), p1Played);

        // P2 joga 1 carta da mão (dispara runAutomatedCombatChain automaticamente)
        List<UUID> p2Played = List.of(p2Hand.get(0).getUuid());
        engine.submitPreparationCards(p2.getUuid(), p2Played);

        // Assert 3: Combate automático resolve e atinge fase DISCARD
        assertEquals(TurnPhase.DISCARD, engine.getTurnPhase());
        assertEquals(3, countHandCards(p1), "Jogador 1 jogou 2 cartas, deve ter 3 na mão.");
        assertEquals(4, countHandCards(p2), "Jogador 2 jogou 1 carta, deve ter 4 na mão.");

        // Act 4: Fase de descarte (P1 não descarta nada; P2 descarta 1 carta restante)
        List<Card> p2RemainingHand = p2.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.HAND)
                .toList();

        engine.submitDiscard(p1.getUuid(), List.of());
        engine.submitDiscard(p2.getUuid(), List.of(p2RemainingHand.get(0).getUuid()));

        // Assert 4: Ao confirmar o descarte de ambos, discard() executa refillHandToFive e avança para CHOICE
        assertEquals(TurnPhase.CHOICE, engine.getTurnPhase(), "Fase deve avançar para CHOICE para o Turno 2.");
        assertEquals(5, countHandCards(p1), "Jogador 1 deve ser recomposto para exatamente 5 cartas.");
        assertEquals(5, countHandCards(p2), "Jogador 2 deve ser recomposto para exatamente 5 cartas.");

        // Act 5: Turno 2 - Segundo jogador faz submitHeroChoice
        UUID nextActivePlayerId = engine.getGameState().getActiveTurnPlayerId();
        HeroCard nextOwnHero = (nextActivePlayerId.equals(p1.getUuid()) ? p1 : p2).getCurrentDeck().getHeroList().get(1);
        HeroCard nextEnemyHero = (nextActivePlayerId.equals(p1.getUuid()) ? p2 : p1).getCurrentDeck().getHeroList().get(1);

        engine.submitHeroChoice(nextActivePlayerId, nextOwnHero.getUuid(), nextEnemyHero.getUuid());

        // Assert 5: No Turno 2, a mão continua exatamente em 5 cartas sem compras duplicadas
        assertEquals(5, countHandCards(p1), "Jogador 1 continua com 5 cartas no Turno 2.");
        assertEquals(5, countHandCards(p2), "Jogador 2 continua com 5 cartas no Turno 2.");
        assertEquals(TurnPhase.PREPARATION, engine.getTurnPhase());
    }
}

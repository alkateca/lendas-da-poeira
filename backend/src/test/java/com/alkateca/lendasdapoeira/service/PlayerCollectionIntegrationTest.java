package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerCollectionIntegrationTest {

    @Test
    void testStarterCollectionAndDeckOwnershipValidation() {
        // Arrange (Preparação)
        DeckValidatorService validatorService = new DeckValidatorService();
        Player player = new Player(UUID.randomUUID(), "TestPlayer", null);

        HeroCard starterHero = new HeroCard();
        starterHero.setCardName("Moyra, Aprendiz da Santa");

        Card bolaDeFogo = new Card();
        bolaDeFogo.setCardName("Bola de fogo");

        Deck starterDeck = new Deck();
        starterDeck.setHeroList(List.of(starterHero));
        starterDeck.setCardList(List.of(bolaDeFogo)); // Concede apenas 1 cópia inicial

        // Act 1 (Ação: Conceder Kit Inicial)
        validatorService.ensureStarterCollection(player, starterDeck, List.of());

        // Assert 1 (Validação do Kit Inicial)
        assertEquals(1, player.getOwnedHeroCount("Moyra, Aprendiz da Santa"), "O jogador deve possuir 1 cópia do herói inicial.");
        assertEquals(1, player.getOwnedCardCount("Bola de fogo"), "O jogador deve possuir exatamente 1 cópia de Bola de fogo.");

        // Act & Assert 2 (Tentativa de usar 2 cópias possuindo apenas 1 na coleção)
        Deck invalidCopiesDeck = new Deck();
        invalidCopiesDeck.setHeroList(List.of(starterHero));
        invalidCopiesDeck.setCardList(List.of(bolaDeFogo, bolaDeFogo));

        assertThrows(IllegalArgumentException.class, () -> {
            validatorService.validateDeckOwnership(player, invalidCopiesDeck);
        }, "Deve rejeitar baralho que utiliza 2 cópias quando o jogador possui apenas 1.");

        // Act & Assert 3 (Tentativa de usar Herói não pertencente à coleção)
        HeroCard unownedHero = new HeroCard();
        unownedHero.setCardName("Kael, Domador de Feras");

        Deck unownedHeroDeck = new Deck();
        unownedHeroDeck.setHeroList(List.of(unownedHero));
        unownedHeroDeck.setCardList(List.of(bolaDeFogo));

        assertThrows(IllegalArgumentException.class, () -> {
            validatorService.validateDeckOwnership(player, unownedHeroDeck);
        }, "Deve rejeitar baralho contendo herói não adquirido na coleção.");
    }
}

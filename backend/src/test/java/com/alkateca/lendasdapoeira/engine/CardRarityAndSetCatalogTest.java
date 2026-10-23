package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.DTO.CardDTO;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.CardRarity;
import com.alkateca.lendasdapoeira.enums.CardSet;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CardRarityAndSetCatalogTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Garante que todas as cartas do catálogo possuem raridade e set mapeados corretamente")
    void shouldLoadAllCardsWithRarityAndSet() throws Exception {
        InputStream stream = new ClassPathResource("cards.json").getInputStream();
        List<Card> cards = objectMapper.readValue(stream, new TypeReference<List<Card>>() {});

        assertNotNull(cards);
        assertFalse(cards.isEmpty());

        for (Card card : cards) {
            assertNotNull(card.getRarity(), "Carta " + card.getCardName() + " deve possuir raridade");
            assertEquals(CardRarity.COMUM, card.getRarity(), "Carta " + card.getCardName() + " deve ser COMUM");
            assertNotNull(card.getCardSet(), "Carta " + card.getCardName() + " deve possuir cardSet");
            assertEquals(CardSet.BASE, card.getCardSet(), "Carta " + card.getCardName() + " deve ser do set BASE");
        }
    }

    @Test
    @DisplayName("Garante que todos os heróis do catálogo possuem raridade e set mapeados corretamente")
    void shouldLoadAllHeroesWithRarityAndSet() throws Exception {
        InputStream stream = new ClassPathResource("heros.json").getInputStream();
        List<HeroCard> heroes = objectMapper.readValue(stream, new TypeReference<List<HeroCard>>() {});

        assertNotNull(heroes);
        assertFalse(heroes.isEmpty());

        for (HeroCard hero : heroes) {
            assertNotNull(hero.getRarity(), "Herói " + hero.getCardName() + " deve possuir raridade");
            assertEquals(CardRarity.COMUM, hero.getRarity(), "Herói " + hero.getCardName() + " deve ser COMUM");
            assertNotNull(hero.getCardSet(), "Herói " + hero.getCardName() + " deve possuir cardSet");
            assertEquals(CardSet.BASE, hero.getCardSet(), "Herói " + hero.getCardName() + " deve ser do set BASE");
        }
    }

    @Test
    @DisplayName("Valida mapeamento de CardSnapshotDTO com rarity e cardSet")
    void shouldMapRarityAndCardSetInSnapshotFactory() {
        Card card = new Card();
        card.setCardName("Teste Carta");
        card.setRarity(CardRarity.LENDARIA);
        card.setCardSet(CardSet.BASE);

        MatchSnapshotDTO.CardSnapshotDTO snapshot = SnapshotFactory.toCardDTO(card);

        assertEquals("LENDARIA", snapshot.getRarity());
        assertEquals("BASE", snapshot.getCardSet());
    }

    @Test
    @DisplayName("Valida suporte de campos no CardDTO")
    void shouldSupportRarityAndSetInCardDTO() {
        CardDTO dto = new CardDTO();
        dto.setRarity(CardRarity.RARA.name());
        dto.setCardSet(CardSet.BASE.name());

        assertEquals("RARA", dto.getRarity());
        assertEquals("BASE", dto.getCardSet());
    }
}

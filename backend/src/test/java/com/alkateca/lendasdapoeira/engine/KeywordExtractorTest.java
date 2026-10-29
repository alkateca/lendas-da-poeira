package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.ItemType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeywordExtractorTest {

    @Test
    @DisplayName("Deve extrair EMPUNHADURA e gatilhos de arma")
    void testExtractWeaponKeywords() {
        Card card = new Card();
        card.setItemType(ItemType.ARMA);
        card.setEmpunhadura(2);
        card.setDescricao("Arma - Empunhadura Dupla\nInício do Combate: O Herói Inimigo sofre 3 de Dano Direto.");

        List<String> keywords = KeywordExtractor.extract(card);

        assertTrue(keywords.contains("EMPUNHADURA"));
        assertTrue(keywords.contains("INICIO_DO_COMBATE"));
        assertTrue(keywords.contains("DANO_DIRETO"));
    }

    @Test
    @DisplayName("Deve extrair modificadores de combate em HeroCard")
    void testExtractHeroKeywords() {
        HeroCard hero = new HeroCard();
        hero.setDanoBonus(2);
        hero.setVulnerabilidade(1);
        hero.setDescricao("Aura: Seus Aliados recebem +1 de Ataque.\nFinal do Combate: Cause 5 de Dano Mágico.");

        List<String> keywords = KeywordExtractor.extract(hero);

        assertTrue(keywords.contains("DANO_BONUS"));
        assertTrue(keywords.contains("VULNERABILIDADE"));
        assertTrue(keywords.contains("AURA"));
        assertTrue(keywords.contains("FINAL_DO_COMBATE"));
        assertTrue(keywords.contains("DANO_MAGICO"));
    }

    @Test
    @DisplayName("Deve manter keywords pré-existentes e evitar duplicatas")
    void testPreExistingKeywords() {
        Card card = new Card();
        card.setKeywords(List.of("INVOCAR"));
        card.setDescricao("Invocar: Bola de Fogo.");

        List<String> keywords = KeywordExtractor.extract(card);

        assertTrue(keywords.contains("INVOCAR"));
        // Não deve duplicar
        long count = keywords.stream().filter(k -> k.equals("INVOCAR")).count();
        org.junit.jupiter.api.Assertions.assertEquals(1, count);
    }
}

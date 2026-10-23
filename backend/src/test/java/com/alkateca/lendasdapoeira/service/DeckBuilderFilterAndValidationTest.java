package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.*;
import com.alkateca.lendasdapoeira.service.DeckValidatorService.CatalogFilterCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeckBuilderFilterAndValidationTest {

    private DeckValidatorService validatorService;
    private List<Card> sampleCatalog;

    private HeroCard redFireHero;
    private HeroCard whiteWildcardHero;
    private HeroCard greenGoblinHero;
    private HeroCard blueKnightHero;

    private Card fireSpellCard;
    private Card blueKnightSword;
    private Card whiteCrystalRapier;
    private Card neutralToolCard;
    private Card MoyraLiberationCard;

    @BeforeEach
    void setUp() {
        validatorService = new DeckValidatorService();

        // Heróis de teste
        redFireHero = new HeroCard();
        redFireHero.setUuid(UUID.randomUUID());
        redFireHero.setCardName("Xamã das cinzas");
        redFireHero.setColor(Colors.RED);
        redFireHero.setAfinidades(List.of(Afinidade.FOGO));
        redFireHero.setClasse(List.of(Classe.CRIADOR));
        redFireHero.setRacas(List.of(Raca.HUMANO));

        whiteWildcardHero = new HeroCard();
        whiteWildcardHero.setUuid(UUID.randomUUID());
        whiteWildcardHero.setCardName("Moyra, Aprendiz da Santa");
        whiteWildcardHero.setColor(Colors.WHITE);
        whiteWildcardHero.setOrdem(List.of(Ordem.CAVALEIRO));
        whiteWildcardHero.setClasse(List.of(Classe.EMISSOR));

        greenGoblinHero = new HeroCard();
        greenGoblinHero.setUuid(UUID.randomUUID());
        greenGoblinHero.setCardName("Esquadrão Goblin");
        greenGoblinHero.setColor(Colors.GREEN);
        greenGoblinHero.setRacas(List.of(Raca.GOBLIN));

        blueKnightHero = new HeroCard();
        blueKnightHero.setUuid(UUID.randomUUID());
        blueKnightHero.setCardName("Cavaleiro Onear");
        blueKnightHero.setColor(Colors.BLUE);
        blueKnightHero.setOrdem(List.of(Ordem.CAVALEIRO));

        // Cartas do Catálogo
        fireSpellCard = new Card();
        fireSpellCard.setCardName("Bola de Fogo");
        fireSpellCard.setCardType(CardType.SPELL);
        fireSpellCard.setColor(Colors.RED);
        fireSpellCard.setAfinidades(List.of(Afinidade.FOGO));
        fireSpellCard.setDescricao("Causa 4 mais seu Espírito de Dano Mágico.");

        blueKnightSword = new Card();
        blueKnightSword.setCardName("Espada Real de Onear");
        blueKnightSword.setCardType(CardType.ITEM);
        blueKnightSword.setItemType(ItemType.ARMA);
        blueKnightSword.setEmpunhadura(2);
        blueKnightSword.setColor(Colors.BLUE);
        blueKnightSword.setOrdem(List.of(Ordem.CAVALEIRO));
        blueKnightSword.setDescricao("Equipar: +4 de Ataque para Cavaleiros.");

        whiteCrystalRapier = new Card();
        whiteCrystalRapier.setCardName("Rapieira de Cristal");
        whiteCrystalRapier.setCardType(CardType.ITEM);
        whiteCrystalRapier.setItemType(ItemType.ARMA);
        whiteCrystalRapier.setEmpunhadura(1);
        whiteCrystalRapier.setColor(Colors.WHITE);
        whiteCrystalRapier.setDescricao("Equipar: +3 de Ataque. Final do Combate: 5 de Dano Mágico.");

        neutralToolCard = new Card();
        neutralToolCard.setCardName("Bomba de fumaça");
        neutralToolCard.setCardType(CardType.ITEM);
        neutralToolCard.setItemType(ItemType.FERRAMENTA);
        neutralToolCard.setEmpunhadura(0);
        neutralToolCard.setColor(Colors.NEUTRAL);
        neutralToolCard.setDescricao("Reduz a Defesa inimiga em 2.");

        MoyraLiberationCard = new Card();
        MoyraLiberationCard.setCardName("Sagração da Santa das Lâminas");
        MoyraLiberationCard.setCardType(CardType.THEURGY);
        MoyraLiberationCard.setColor(Colors.WHITE);
        MoyraLiberationCard.setLiberationCard(true);
        MoyraLiberationCard.setTargetBaseHeroName("Moyra, Aprendiz da Santa");
        MoyraLiberationCard.setEvolvesIntoHeroName("Moyra, Santa das Laminas");
        MoyraLiberationCard.setDescricao("Missão: Evolui Moyra para Santa das Lâminas.");

        sampleCatalog = List.of(
                fireSpellCard,
                blueKnightSword,
                whiteCrystalRapier,
                neutralToolCard,
                MoyraLiberationCard
        );
    }

    @Test
    @DisplayName("1. Filtragem Dinâmica por Heróis: ao escolher 1 herói, oculta cartas incompatíveis; ao desmarcar, todas retornam")
    void shouldFilterDynamicallyWhenSelectingAndRemovingHeroes() {
        List<HeroCard> selectedHeroes = new ArrayList<>();
        CatalogFilterCriteria criteria = new CatalogFilterCriteria();

        // Sem heróis selecionados: todas as 5 cartas aparecem
        List<Card> initialResult = validatorService.filterCatalogCards(sampleCatalog, selectedHeroes, criteria);
        assertEquals(5, initialResult.size());

        // Seleciona 1º Herói (Vermelho com Afinidade Fogo):
        // Deve manter apenas Bola de Fogo (RED + FOGO) e Bomba de fumaça (NEUTRAL), ocultando Azul e Branca!
        selectedHeroes.add(redFireHero);
        List<Card> afterFirstHero = validatorService.filterCatalogCards(sampleCatalog, selectedHeroes, criteria);
        assertEquals(2, afterFirstHero.size());
        assertTrue(afterFirstHero.contains(fireSpellCard));
        assertTrue(afterFirstHero.contains(neutralToolCard));
        assertFalse(afterFirstHero.contains(blueKnightSword));
        assertFalse(afterFirstHero.contains(whiteCrystalRapier));

        // Se adicionar o Coringa Branco (Moyra) junto com o Herói Vermelho:
        // A regra do Coringa Branco restringe o baralho apenas a cartas WHITE e NEUTRAL (bloqueando Bola de Fogo RED e liberando Rapieira WHITE + Liberação da Moyra)
        selectedHeroes.add(whiteWildcardHero);
        List<Card> afterAddingWhiteHero = validatorService.filterCatalogCards(sampleCatalog, selectedHeroes, criteria);
        assertEquals(3, afterAddingWhiteHero.size());
        assertTrue(afterAddingWhiteHero.contains(whiteCrystalRapier));
        assertTrue(afterAddingWhiteHero.contains(neutralToolCard));
        assertTrue(afterAddingWhiteHero.contains(MoyraLiberationCard));
        assertFalse(afterAddingWhiteHero.contains(fireSpellCard));

        // Ao desmarcar todos os heróis, todas as 5 cartas retornam imediatamente para a listagem!
        selectedHeroes.clear();
        List<Card> afterClearingHeroes = validatorService.filterCatalogCards(sampleCatalog, selectedHeroes, criteria);
        assertEquals(5, afterClearingHeroes.size());
    }

    @Test
    @DisplayName("2. Campo de Busca Textual: deve filtrar por nome ou palavra-chave na descrição ignorando acentos e caixa")
    void shouldFilterByTextSearchInNameAndDescription() {
        CatalogFilterCriteria criteria = new CatalogFilterCriteria();

        // Busca pelo nome parcial sem acento ("fumaca")
        criteria.setSearchQuery("fumaca");
        List<Card> byName = validatorService.filterCatalogCards(sampleCatalog, List.of(), criteria);
        assertEquals(1, byName.size());
        assertEquals("Bomba de fumaça", byName.get(0).getCardName());

        // Busca por texto no efeito ("Dano Magico") -> deve achar Bola de Fogo e Rapieira de Cristal
        criteria.setSearchQuery("dano magico");
        List<Card> byDescription = validatorService.filterCatalogCards(sampleCatalog, List.of(), criteria);
        assertEquals(2, byDescription.size());
        assertTrue(byDescription.contains(fireSpellCard));
        assertTrue(byDescription.contains(whiteCrystalRapier));
    }

    @Test
    @DisplayName("3. Seletores Cumulativos de Atributos: Cor, Tipo, Subtipo, Ordem, Afinidade e Empunhadura")
    void shouldFilterByCumulativeAttributeSelectors() {
        CatalogFilterCriteria criteria = new CatalogFilterCriteria();

        // Filtro isolado: Subtipo ARMA -> acha Espada Real de Onear (Emp 2) e Rapieira de Cristal (Emp 1)
        criteria.setItemType(ItemType.ARMA);
        List<Card> weapons = validatorService.filterCatalogCards(sampleCatalog, List.of(), criteria);
        assertEquals(2, weapons.size());

        // Filtro cumulativo: Subtipo ARMA + Empunhadura 2 + Ordem CAVALEIRO + Cor BLUE -> acha apenas Espada Real de Onear
        criteria.setEmpunhadura(2);
        criteria.setOrdem(Ordem.CAVALEIRO);
        criteria.setColor(Colors.BLUE);
        List<Card> twoHandedBlueKnightWeapon = validatorService.filterCatalogCards(sampleCatalog, List.of(), criteria);
        assertEquals(1, twoHandedBlueKnightWeapon.size());
        assertEquals("Espada Real de Onear", twoHandedBlueKnightWeapon.get(0).getCardName());

        // Limpar Filtros (novo objeto CatalogFilterCriteria padrão) restaura o catálogo respeitando os heróis atuais
        CatalogFilterCriteria clearedCriteria = new CatalogFilterCriteria();
        List<Card> restoredForBlueKnight = validatorService.filterCatalogCards(
                sampleCatalog,
                List.of(blueKnightHero),
                clearedCriteria
        );
        // Com Cavaleiro Onear (BLUE + Ordem CAVALEIRO), permite Espada Real de Onear (BLUE/CAVALEIRO) e Bomba de fumaça (NEUTRAL)
        assertEquals(2, restoredForBlueKnight.size());
        assertTrue(restoredForBlueKnight.contains(blueKnightSword));
        assertTrue(restoredForBlueKnight.contains(neutralToolCard));
    }

    @Test
    @DisplayName("4. Validação Completa do Deck: exige 3 heróis, 30 cartas compatíveis e no máximo 1 Carta de Liberação válida")
    void shouldValidateCompleteDeckWithLiberationCard() {
        HeroCard h1 = whiteWildcardHero;
        HeroCard h2 = new HeroCard();
        h2.setCardName("Isenora, Santa das Laminas");
        h2.setColor(Colors.WHITE);
        HeroCard h3 = new HeroCard();
        h3.setCardName("Naelis, Grande Artesã de Cristais");
        h3.setColor(Colors.WHITE);

        List<Card> thirtyCards = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            Card c = new Card();
            c.setCardName("Carta Neutra " + (i / 3)); // 10 grupos de 3 cópias (respeita limite de 3 cópias)
            c.setCardType(CardType.ITEM);
            c.setColor(Colors.NEUTRAL);
            thirtyCards.add(c);
        }

        Deck validDeck = new Deck("Deck Santa das Lâminas", thirtyCards, List.of(h1, h2, h3), List.of(MoyraLiberationCard));
        DeckValidatorService.ValidationResult result = validatorService.validateDeck(validDeck);
        assertTrue(result.isValidForPlay, result.message);
    }
}

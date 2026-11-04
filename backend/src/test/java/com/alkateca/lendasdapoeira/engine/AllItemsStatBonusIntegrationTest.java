package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.effects.combat.EquipBuffEffect;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AllItemsStatBonusIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Garante que todos os itens de cards.json possuem lógica de status válida e refletem em Card e CardSnapshotDTO")
    void testAllCatalogItemsStatBonusesAndDtoMapping() throws Exception {
        // Arrange
        InputStream stream = new ClassPathResource("cards.json").getInputStream();
        List<Card> allCards = objectMapper.readValue(stream, new TypeReference<List<Card>>() {});
        List<Card> items = allCards.stream()
                .filter(c -> c.getCardType() == CardType.ITEM)
                .collect(Collectors.toList());

        assertNotNull(items);
        assertFalse(items.isEmpty(), "O catálogo cards.json deve conter itens cadastrados.");

        int itemsWithBuffCount = 0;

        for (Card item : items) {
            // Act: Injeta os efeitos via CardFactory
            CardFactory.buildCardLogic(item);

            // Assert: Todo item é equipável
            assertTrue(item.isEquipable(), "O item " + item.getCardName() + " deve ser equipável.");

            // Mapeia para CardSnapshotDTO
            MatchSnapshotDTO.CardSnapshotDTO dto = SnapshotFactory.toCardDTO(item);

            assertEquals(item.isEquipable(), dto.isEquipable(),
                    "Snapshot DTO de " + item.getCardName() + " deve manter isEquipable.");
            assertEquals(item.getBonusEspirito(), dto.getBonusEspirito(),
                    "Snapshot DTO de " + item.getCardName() + " deve manter bonusEspirito.");
            assertEquals(item.getBonusAtaque(), dto.getBonusAtaque(),
                    "Snapshot DTO de " + item.getCardName() + " deve manter bonusAtaque.");
            assertEquals(item.getBonusDefesa(), dto.getBonusDefesa(),
                    "Snapshot DTO de " + item.getCardName() + " deve manter bonusDefesa.");
            assertEquals(item.getActivePhaseKeys(), dto.getActivePhaseKeys(),
                    "Snapshot DTO de " + item.getCardName() + " deve manter activePhaseKeys.");

            // Valida soma de EquipBuffEffect
            List<EquipBuffEffect> buffEffects = item.getEffects().stream()
                    .filter(e -> e instanceof EquipBuffEffect)
                    .map(e -> (EquipBuffEffect) e)
                    .collect(Collectors.toList());

            if (!buffEffects.isEmpty()) {
                itemsWithBuffCount++;
                int expectedEsp = buffEffects.stream().mapToInt(EquipBuffEffect::getBonusEspirito).sum();
                int expectedAtq = buffEffects.stream().mapToInt(EquipBuffEffect::getBonusAtaque).sum();
                int expectedDef = buffEffects.stream().mapToInt(EquipBuffEffect::getBonusDefesa).sum();

                assertEquals(expectedEsp, item.getBonusEspirito(),
                        "Bônus de Espírito de " + item.getCardName() + " deve coincidir com EquipBuffEffect.");
                assertEquals(expectedAtq, item.getBonusAtaque(),
                        "Bônus de Ataque de " + item.getCardName() + " deve coincidir com EquipBuffEffect.");
                assertEquals(expectedDef, item.getBonusDefesa(),
                        "Bônus de Defesa de " + item.getCardName() + " deve coincidir com EquipBuffEffect.");
            }
        }

        assertTrue(itemsWithBuffCount >= 20, "Devem existir itens com EquipBuffEffect configurados no catálogo.");
    }

    @Test
    @DisplayName("Garante que a resolução de equipar e desequipar aplica e reverte os bônus de status no HeroCard")
    void testEquipAndDetachStatModifiersOnHero() throws Exception {
        // Arrange
        InputStream stream = new ClassPathResource("cards.json").getInputStream();
        List<Card> allCards = objectMapper.readValue(stream, new TypeReference<List<Card>>() {});
        List<Card> items = allCards.stream()
                .filter(c -> c.getCardType() == CardType.ITEM)
                .collect(Collectors.toList());

        for (Card item : items) {
            CardFactory.buildCardLogic(item);

            List<EquipBuffEffect> buffEffects = item.getEffects().stream()
                    .filter(e -> e instanceof EquipBuffEffect)
                    .map(e -> (EquipBuffEffect) e)
                    .collect(Collectors.toList());

            if (buffEffects.isEmpty()) {
                continue;
            }

            GameEngine engine = new GameEngine();

            Player p1 = new Player();
            p1.setUuid(UUID.randomUUID());
            p1.setName("Hero P1");
            engine.setPlayer1(p1);

            Player p2 = new Player();
            p2.setUuid(UUID.randomUUID());
            p2.setName("Hero P2");
            engine.setPlayer2(p2);

            HeroCard hero = new HeroCard();
            hero.setUuid(UUID.randomUUID());
            hero.setCardName("Herói de Teste");
            hero.setEstaVivo(true);
            hero.setEstaAtivo(true);
            hero.setMaxWeaponSlots(10); // Margem ampla para não barrar armas com empunhadura
            hero.setEspirito(10);
            hero.setAtaque(10);
            hero.setDefesa(10);
            hero.setVidaMaxima(20);
            hero.setVidaAtual(20);

            p1.getCurrentDeck().getHeroList().add(hero);
            engine.getGameState().setActiveHeroPlayer1Id(hero.getUuid());

            item.setOwnerId(p1.getUuid());
            p1.getCurrentDeck().getCardList().add(item);

            int baseEsp = hero.getEspirito();
            int baseAtk = hero.getAtaque();
            int baseDef = hero.getDefesa();
            int baseMaxHp = hero.getVidaMaxima();

            int sumBonusEsp = buffEffects.stream().mapToInt(EquipBuffEffect::getBonusEspirito).sum();
            int sumBonusAtk = buffEffects.stream().mapToInt(EquipBuffEffect::getBonusAtaque).sum();
            int sumBonusDef = buffEffects.stream().mapToInt(EquipBuffEffect::getBonusDefesa).sum();
            int sumBonusHp = buffEffects.stream().mapToInt(EquipBuffEffect::getBonusVidaMaxima).sum();

            // Act 1: Simula fase RESOLUTION para equipar
            engine.setTurnPhase(TurnPhase.RESOLUTION);
            for (Effect effect : item.getEffects()) {
                if (effect instanceof EquipBuffEffect && effect.predicate(item, TurnPhase.RESOLUTION)) {
                    effect.resolve(engine, item);
                }
            }

            // Assert 1: Atributos aumentados no Herói
            assertEquals(baseEsp + sumBonusEsp, hero.getEspirito(),
                    "Espírito do herói ao equipar " + item.getCardName() + " deve receber o bônus.");
            assertEquals(baseAtk + sumBonusAtk, hero.getAtaque(),
                    "Ataque do herói ao equipar " + item.getCardName() + " deve receber o bônus.");
            assertEquals(baseDef + sumBonusDef, hero.getDefesa(),
                    "Defesa do herói ao equipar " + item.getCardName() + " deve receber o bônus.");
            assertEquals(baseMaxHp + sumBonusHp, hero.getVidaMaxima(),
                    "Vida Máxima do herói ao equipar " + item.getCardName() + " deve receber o bônus.");

            // Act 2: Desequipar o item
            for (Effect effect : item.getEffects()) {
                if (effect instanceof EquipBuffEffect) {
                    ((EquipBuffEffect) effect).onDetach(engine, item, hero);
                }
            }

            // Assert 2: Atributos restaurados ao valor original
            assertEquals(baseEsp, hero.getEspirito(),
                    "Espírito do herói após desequipar " + item.getCardName() + " deve retornar ao base.");
            assertEquals(baseAtk, hero.getAtaque(),
                    "Ataque do herói após desequipar " + item.getCardName() + " deve retornar ao base.");
            assertEquals(baseDef, hero.getDefesa(),
                    "Defesa do herói após desequipar " + item.getCardName() + " deve retornar ao base.");
            assertEquals(baseMaxHp, hero.getVidaMaxima(),
                    "Vida Máxima do herói após desequipar " + item.getCardName() + " deve retornar ao base.");
        }
    }

    @Test
    @DisplayName("Valida valores nominais exatos de bônus dos itens clássicos de Lendas da Poeira")
    void testSpecificKnownItemsStatValues() {
        assertItemStats("Quimera", 2, 2, 2);
        assertItemStats("Broche de Cristal", 2, 0, 2);
        assertItemStats("Lamina de Cristal", 0, 1, 0);
        assertItemStats("Dragast", 2, 2, 0);
        assertItemStats("Rapieira de Cristal", 0, 3, 0);
        assertItemStats("Garra espectral", 0, 2, 0);
        assertItemStats("Machado de Gelo", 0, 2, 2);
        assertItemStats("Grande Machado Sombrio", 0, 4, 2);
        assertItemStats("Artefato Cortante", 0, 3, 1);
        assertItemStats("Sica", 2, 2, 0);
        assertItemStats("Sino do Caçador", 1, 1, 0);
        assertItemStats("Arpão terrestre", 0, 3, 2);
        assertItemStats("Bomba de fumaça", 0, 2, 0);
        assertItemStats("Peitoral de brasas", 2, 0, 2);
        assertItemStats("Escudo ardente", 2, 0, 2);
        assertItemStats("Espada do Carrasco", 0, 3, 0);
        assertItemStats("Cutelo do Suplício", 0, 2, 0);
        assertItemStats("Cetro do Julgamento Perpétuo", 1, 2, 0);
        assertItemStats("Bolsa de Armadilhas do Mestre Caçador", 0, 2, 2);
        assertItemStats("Escamas do Dragão Eterno", 2, 0, 3);
        assertItemStats("Incensário Purificador", 1, 0, 1);
        assertItemStats("Baluarte de Espelhos do Dragão", 0, 0, 1);
        assertItemStats("Placas de Cristal Draconiano", 0, 0, 2);
        assertItemStats("Filactéria das Areias Negras", 2, 0, 2);
        assertItemStats("Amuleto de Ossos da Fábrica", 1, 1, 0);
        assertItemStats("Manto de Ferro do Algoz", 0, 0, 1);
        assertItemStats("Tomo das Sentenças Antigas", 1, 0, 1);
        assertItemStats("Alabarda Cerimonial do Trono", 1, 1, 0);
        assertItemStats("Glaive da Guarda Eterna", 0, 2, 0);
        assertItemStats("Manto Cerimonial do Trono", 0, 0, 1);
        assertItemStats("Escudo da Devoção Cega", 0, 0, 1);
        assertItemStats("Baluarte da Tundra Ancestral", 0, 0, 3);
        assertItemStats("Couraça da Tundra Profunda", 0, 0, 2);
        assertItemStats("Garras Congeladas", 0, 2, 1);
        assertItemStats("Núcleo de Invenções do Rei Goblin", 1, 1, 0);
        assertItemStats("Bateria de Cristal de Poeira", 1, 0, 1);
        assertItemStats("Chave Mecânica de Precisão", 0, 2, 1);
        assertItemStats("Engrenagem de Ressonância", 1, 1, 0);
    }

    private void assertItemStats(String itemName, int expectedEsp, int expectedAtq, int expectedDef) {
        Card card = new Card();
        card.setCardName(itemName);
        card.setCardType(CardType.ITEM);
        CardFactory.buildCardLogic(card);

        assertTrue(card.isEquipable(), "A carta " + itemName + " deve ser equipável.");
        assertEquals(expectedEsp, card.getBonusEspirito(), "Espírito inválido para " + itemName);
        assertEquals(expectedAtq, card.getBonusAtaque(), "Ataque inválido para " + itemName);
        assertEquals(expectedDef, card.getBonusDefesa(), "Defesa inválida para " + itemName);

        MatchSnapshotDTO.CardSnapshotDTO dto = SnapshotFactory.toCardDTO(card);
        assertEquals(expectedEsp, dto.getBonusEspirito(), "DTO Espírito inválido para " + itemName);
        assertEquals(expectedAtq, dto.getBonusAtaque(), "DTO Ataque inválido para " + itemName);
        assertEquals(expectedDef, dto.getBonusDefesa(), "DTO Defesa inválida para " + itemName);
        assertTrue(dto.isEquipable(), "DTO deve ser equipável para " + itemName);
    }
}

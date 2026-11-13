package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.effects.combat.EquipBuffEffect;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SingleSlotEquipmentReplacementTest {

    private Player player1;
    private Player player2;
    private HeroCard heroP1;
    private HeroCard heroP2;
    private GameEngine engine;

    @BeforeEach
    void setUp() {
        player1 = new Player();
        player1.setUuid(UUID.randomUUID());
        player1.setName("Jogador 1");

        player2 = new Player();
        player2.setUuid(UUID.randomUUID());
        player2.setName("Jogador 2");

        heroP1 = createHero(player1.getUuid(), "Cavaleiro Onear", Colors.BLUE, 4, 4, 4, 25, 2);
        heroP2 = createHero(player2.getUuid(), "Inquisidor", Colors.YELLOW, 3, 3, 3, 25, 2);

        player1.setCurrentDeck(new Deck("Deck P1", new ArrayList<>(), new ArrayList<>(List.of(heroP1))));
        player2.setCurrentDeck(new Deck("Deck P2", new ArrayList<>(), new ArrayList<>(List.of(heroP2))));

        engine = new GameEngine(player1, player2);
        engine.setTurnPhase(TurnPhase.CHOICE);
        engine.getGameState().setTurnPhase(TurnPhase.CHOICE);
        engine.setActivePlayerId(player1.getUuid());
        engine.getGameState().setActiveTurnPlayerId(player1.getUuid());
        engine.submitHeroChoice(player1.getUuid(), heroP1.getUuid(), heroP2.getUuid());
    }

    private HeroCard createHero(UUID ownerId, String name, Colors color, int esp, int atk, int def, int hp, int maxWeaponSlots) {
        HeroCard hero = new HeroCard();
        hero.setUuid(UUID.randomUUID());
        hero.setOwnerId(ownerId);
        hero.setCardName(name);
        hero.setCardType(CardType.HERO);
        hero.setColor(color);
        hero.setEspirito(esp);
        hero.setAtaque(atk);
        hero.setDefesa(def);
        hero.setVidaMaxima(hp);
        hero.setVidaAtual(hp);
        hero.setMaxWeaponSlots(maxWeaponSlots);
        hero.setEstaVivo(true);
        hero.setEstaAtivo(true);
        return hero;
    }

    private Card createEquipmentCard(UUID ownerId, String name, ItemType itemType, int bonusEsp, int bonusAtk, int bonusDef, int bonusHp, String descricao) {
        Card card = new Card();
        card.setUuid(UUID.randomUUID());
        card.setOwnerId(ownerId);
        card.setCardName(name);
        card.setCardType(CardType.ITEM);
        card.setItemType(itemType);
        card.setEmpunhadura(itemType == ItemType.ARMA ? 1 : 0);
        card.setColor(Colors.BLUE);
        card.setDescricao(descricao);
        card.setZoneId(ZoneId.HAND);
        card.setEffects(new ArrayList<>(List.of(new EquipBuffEffect(bonusEsp, bonusAtk, bonusDef, bonusHp))));
        return card;
    }

    @Test
    @DisplayName("1. Anexar novo PET desanexa o PET anterior, reverte seus atributos e envia para DISCARD ou EXILE")
    void shouldReplaceExistingPetAndRevertItsStatsWhenNewPetIsEquipped() {
        Card pet1 = createEquipmentCard(player1.getUuid(), "Corvo de Cristal", ItemType.PET, 2, 1, 0, 0, "Pet\nSeu Herói recebe +2 de Espírito e +1 de Ataque.");
        Card pet2 = createEquipmentCard(player1.getUuid(), "Lobo de Poeira", ItemType.PET, 0, 3, 2, 0, "Pet\nSeu Herói recebe +3 de Ataque e +2 de Defesa.\nExilar.");
        Card pet3 = createEquipmentCard(player1.getUuid(), "Golem Mascote", ItemType.PET, 1, 0, 4, 0, "Pet\nSeu Herói recebe +1 de Espírito e +4 de Defesa.");
        player1.getCurrentDeck().getCardList().addAll(List.of(pet1, pet2, pet3));

        // Equipa o 1º Pet
        pet1.getEffects().get(0).resolve(engine, pet1);
        assertEquals(ZoneId.ATTACHED, pet1.getZoneId());
        assertEquals(heroP1.getUuid(), pet1.getAttachedToCardId());
        assertEquals(6, heroP1.getEspirito());
        assertEquals(5, heroP1.getAtaque());
        assertEquals(4, heroP1.getDefesa());
        assertEquals(1, engine.getUsedItemTypeCountForHero(heroP1.getUuid(), ItemType.PET));

        // Permissão na preparação mesmo já possuindo um Pet anexado
        assertTrue(engine.canPlayCardInPreparation(player1, heroP1, pet2));

        // Equipa o 2º Pet -> deve desanexar o 1º Pet (para DISCARD) e reverter os buffs do 1º
        pet2.getEffects().get(0).resolve(engine, pet2);
        assertEquals(ZoneId.DISCARD, pet1.getZoneId());
        assertNull(pet1.getAttachedToCardId());
        assertEquals(ZoneId.ATTACHED, pet2.getZoneId());
        assertEquals(heroP1.getUuid(), pet2.getAttachedToCardId());
        assertEquals(4, heroP1.getEspirito()); // 6 - 2 + 0 = 4
        assertEquals(7, heroP1.getAtaque());   // 5 - 1 + 3 = 7
        assertEquals(6, heroP1.getDefesa());   // 4 - 0 + 2 = 6
        assertEquals(1, engine.getUsedItemTypeCountForHero(heroP1.getUuid(), ItemType.PET));

        // Equipa o 3º Pet -> deve desanexar o 2º Pet (para EXILE pois tem "Exilar.")
        pet3.getEffects().get(0).resolve(engine, pet3);
        assertEquals(ZoneId.EXILE, pet2.getZoneId());
        assertNull(pet2.getAttachedToCardId());
        assertEquals(ZoneId.ATTACHED, pet3.getZoneId());
        assertEquals(heroP1.getUuid(), pet3.getAttachedToCardId());
        assertEquals(5, heroP1.getEspirito()); // 4 - 0 + 1 = 5
        assertEquals(4, heroP1.getAtaque());   // 7 - 3 + 0 = 4
        assertEquals(8, heroP1.getDefesa());   // 6 - 2 + 4 = 8
        assertEquals(1, engine.getUsedItemTypeCountForHero(heroP1.getUuid(), ItemType.PET));
    }

    @Test
    @DisplayName("2. ARMADURA, ESCUDO e PET possuem slots independentes e substituem apenas o equipamento do mesmo tipo")
    void shouldMaintainIndependentSlotsForArmorShieldAndPetAndReplaceSameType() {
        Card armor1 = createEquipmentCard(player1.getUuid(), "Cota de Malha", ItemType.ARMADURA, 0, 0, 2, 0, "Armadura");
        Card armor2 = createEquipmentCard(player1.getUuid(), "Couraça Real", ItemType.ARMADURA, 1, 0, 4, 0, "Armadura");
        Card shield1 = createEquipmentCard(player1.getUuid(), "Broquel", ItemType.ESCUDO, 0, 0, 1, 0, "Escudo");
        Card shield2 = createEquipmentCard(player1.getUuid(), "Escudo Torre", ItemType.ESCUDO, 0, 0, 3, 0, "Escudo");
        Card pet1 = createEquipmentCard(player1.getUuid(), "Falcão", ItemType.PET, 0, 2, 0, 0, "Pet");

        player1.getCurrentDeck().getCardList().addAll(List.of(armor1, armor2, shield1, shield2, pet1));

        // Equipa 1 Armadura, 1 Escudo e 1 Pet simultaneamente
        armor1.getEffects().get(0).resolve(engine, armor1);
        shield1.getEffects().get(0).resolve(engine, shield1);
        pet1.getEffects().get(0).resolve(engine, pet1);

        assertEquals(ZoneId.ATTACHED, armor1.getZoneId());
        assertEquals(ZoneId.ATTACHED, shield1.getZoneId());
        assertEquals(ZoneId.ATTACHED, pet1.getZoneId());
        assertEquals(6, heroP1.getAtaque()); // 4 + 2 (pet1)
        assertEquals(7, heroP1.getDefesa()); // 4 + 2 (armor1) + 1 (shield1)

        // Permite jogar nova Armadura e novo Escudo nas janelas alternadas de resolução
        assertDoesNotThrow(() -> {
            engine.submitResolutionCard(player1.getUuid(), armor2.getUuid());
            engine.passResolutionWindow(player2.getUuid());
            engine.submitResolutionCard(player1.getUuid(), shield2.getUuid());
            engine.passResolutionWindow(player2.getUuid());
        });

        // Após a cadeia de combate, armor1 e shield1 foram substituídos por armor2 e shield2, e pet1 continua anexado
        assertEquals(ZoneId.DISCARD, armor1.getZoneId());
        assertNull(armor1.getAttachedToCardId());
        assertEquals(ZoneId.ATTACHED, armor2.getZoneId());
        assertEquals(heroP1.getUuid(), armor2.getAttachedToCardId());

        assertEquals(ZoneId.DISCARD, shield1.getZoneId());
        assertNull(shield1.getAttachedToCardId());
        assertEquals(ZoneId.ATTACHED, shield2.getZoneId());
        assertEquals(heroP1.getUuid(), shield2.getAttachedToCardId());

        assertEquals(ZoneId.ATTACHED, pet1.getZoneId());
        assertEquals(heroP1.getUuid(), pet1.getAttachedToCardId());

        assertEquals(5, heroP1.getEspirito()); // 4 + 1 (armor2)
        assertEquals(6, heroP1.getAtaque());   // 4 + 2 (pet1)
        assertEquals(11, heroP1.getDefesa());  // 4 + 4 (armor2) + 3 (shield2)
    }

    @Test
    @DisplayName("3. Cartas com CardType.PET possuem aparência equipável (equipable=true) e anexam visualmente em attachedCards do Herói")
    void shouldTreatCardTypePetAsEquipableAndIncludeInHeroAttachedCardsSnapshot() {
        Card petCardType = new Card();
        petCardType.setUuid(UUID.randomUUID());
        petCardType.setOwnerId(player1.getUuid());
        petCardType.setCardName("Salamandra de Brasas");
        petCardType.setCardType(CardType.PET);
        petCardType.setColor(Colors.BLUE);
        petCardType.setZoneId(ZoneId.HAND);
        petCardType.setEffects(new ArrayList<>(List.of(new EquipBuffEffect(1, 2, 1, 0))));
        player1.getCurrentDeck().getCardList().add(petCardType);

        assertTrue(petCardType.isEquipable());
        assertEquals(ItemType.PET, petCardType.getItemType());

        MatchSnapshotDTO.CardSnapshotDTO cardDto = SnapshotFactory.toCardDTO(petCardType);
        assertTrue(cardDto.isEquipable());
        assertEquals("PET", cardDto.getCardType());
        assertEquals("PET", cardDto.getItemType());
        assertEquals(1, cardDto.getBonusEspirito());
        assertEquals(2, cardDto.getBonusAtaque());
        assertEquals(1, cardDto.getBonusDefesa());

        engine.submitPreparationCards(player1.getUuid(), List.of(petCardType.getUuid()));
        engine.submitPreparationCards(player2.getUuid(), List.of());

        assertEquals(ZoneId.ATTACHED, petCardType.getZoneId());
        assertEquals(heroP1.getUuid(), petCardType.getAttachedToCardId());

        MatchSnapshotDTO snapshot = SnapshotFactory.createSnapshot(engine, null, null, "TEST");
        MatchSnapshotDTO.HeroSnapshotDTO heroSnapshot = snapshot.getLocalPlayer().getHeroes().get(0);
        assertEquals(1, heroSnapshot.getAttachedCards().size());
        assertEquals("Salamandra de Brasas", heroSnapshot.getAttachedCards().get(0).getCardName());
        assertTrue(heroSnapshot.getAttachedCards().get(0).isEquipable());
    }
}

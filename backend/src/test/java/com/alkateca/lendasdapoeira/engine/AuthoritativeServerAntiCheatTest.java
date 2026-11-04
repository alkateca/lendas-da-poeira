package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.effects.combat.PlayActionSetWeaponSlotsEffect;
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

class AuthoritativeServerAntiCheatTest {

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

        heroP1 = createHero(player1.getUuid(), "Cavaleiro Onear", Colors.BLUE, 2, 3, 2, 20, 2);
        heroP1.setOrdem(new ArrayList<>(List.of(Ordem.CAVALEIRO)));
        heroP1.setAfinidades(new ArrayList<>(List.of(Afinidade.CRISTAL)));
        heroP1.setClasse(new ArrayList<>(List.of(Classe.TRANSFORMADOR)));
        heroP1.setRacas(new ArrayList<>());

        heroP2 = createHero(player2.getUuid(), "Xamã das cinzas", Colors.RED, 3, 2, 2, 20, 2);
        heroP2.setAfinidades(new ArrayList<>(List.of(Afinidade.FOGO)));
        heroP2.setClasse(new ArrayList<>(List.of(Classe.EMISSOR)));
        heroP2.setRacas(new ArrayList<>());

        List<Card> p1Cards = new ArrayList<>();
        List<Card> p2Cards = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Card c1 = createActionCard(player1.getUuid(), "Golpe de Espada " + i, Colors.BLUE);
            c1.setZoneId(i < 4 ? ZoneId.HAND : ZoneId.DECK);
            p1Cards.add(c1);

            Card c2 = createActionCard(player2.getUuid(), "Chama Rápida " + i, Colors.RED);
            c2.setZoneId(i < 4 ? ZoneId.HAND : ZoneId.DECK);
            p2Cards.add(c2);
        }

        player1.setCurrentDeck(new Deck("Deck P1", p1Cards, new ArrayList<>(List.of(heroP1))));
        player2.setCurrentDeck(new Deck("Deck P2", p2Cards, new ArrayList<>(List.of(heroP2))));

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
        hero.setZoneId(ZoneId.BENCH);
        return hero;
    }

    private Card createActionCard(UUID ownerId, String name, Colors color) {
        Card card = new Card();
        card.setUuid(UUID.randomUUID());
        card.setOwnerId(ownerId);
        card.setCardName(name);
        card.setCardType(CardType.ACTION);
        card.setColor(color);
        card.setItemType(ItemType.NENHUM);
        card.setEffects(new ArrayList<>());
        return card;
    }

    private Card createWeaponCard(UUID ownerId, String name, Colors color, int empunhadura) {
        Card card = new Card();
        card.setUuid(UUID.randomUUID());
        card.setOwnerId(ownerId);
        card.setCardName(name);
        card.setCardType(CardType.ITEM);
        card.setItemType(ItemType.ARMA);
        card.setEmpunhadura(empunhadura);
        card.setColor(color);
        CardFactory.buildCardLogic(card);
        return card;
    }

    @Test
    @DisplayName("1. Anti-Cheat: Deve rejeitar UUIDs duplicados ou nulos na preparação sem alterar o estado do jogo")
    void shouldRejectDuplicateOrNullCardUuidsInPreparationWithoutMutatingState() {
        // Arrange
        Card handCard = player1.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.HAND)
                .findFirst()
                .orElseThrow();
        List<UUID> duplicatePayload = List.of(handCard.getUuid(), handCard.getUuid());

        List<UUID> nullPayload = new ArrayList<>();
        nullPayload.add(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                engine.submitPreparationCards(player1.getUuid(), duplicatePayload)
        );
        assertEquals(ZoneId.HAND, handCard.getZoneId(), "A carta não pode ter sua zona alterada quando o payload é rejeitado");
        assertTrue(engine.getGameState().getPlayer1PlayedCards().isEmpty(), "A pilha de resolução deve permanecer vazia");
        assertFalse(engine.isPlayer1Ready(), "O jogador não deve ser marcado como pronto após payload inválido");

        assertThrows(IllegalArgumentException.class, () ->
                engine.submitPreparationCards(player1.getUuid(), nullPayload)
        );
        assertFalse(engine.isPlayer1Ready());
    }

    @Test
    @DisplayName("2. Anti-Cheat: Deve rejeitar cartas fora da mão (DECK, DISCARD ou do oponente) mantendo atomicidade")
    void shouldRejectCardsNotInHandAndPreserveValidCardsInHand() {
        // Arrange
        List<Card> p1HandCards = player1.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.HAND)
                .toList();
        Card validHandCard = p1HandCards.get(0);
        Card deckCard = player1.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.DECK)
                .findFirst()
                .orElseThrow();
        Card discardCard = p1HandCards.get(1);
        discardCard.setZoneId(ZoneId.DISCARD);

        Card opponentHandCard = player2.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.HAND)
                .findFirst()
                .orElseThrow();

        // Act & Assert: 1ª carta válida + 2ª carta no DECK -> nenhuma das duas deve sair da zona original!
        assertThrows(IllegalArgumentException.class, () ->
                engine.submitPreparationCards(player1.getUuid(), List.of(validHandCard.getUuid(), deckCard.getUuid()))
        );
        assertEquals(ZoneId.HAND, validHandCard.getZoneId(), "Carta válida no mesmo payload deve permanecer em HAND (atomicidade)");
        assertEquals(ZoneId.DECK, deckCard.getZoneId());
        assertTrue(engine.getGameState().getPlayer1PlayedCards().isEmpty());
        assertFalse(engine.isPlayer1Ready());

        // Act & Assert: Carta no DISCARD
        assertThrows(IllegalArgumentException.class, () ->
                engine.submitPreparationCards(player1.getUuid(), List.of(discardCard.getUuid()))
        );
        assertEquals(ZoneId.DISCARD, discardCard.getZoneId());

        // Act & Assert: Carta da mão do oponente
        assertThrows(IllegalArgumentException.class, () ->
                engine.submitPreparationCards(player1.getUuid(), List.of(opponentHandCard.getUuid()))
        );
        assertEquals(ZoneId.HAND, opponentHandCard.getZoneId());
    }

    @Test
    @DisplayName("3. Limite Variável de Cartas por Turno: bloqueia acima de 2 por padrão e permite quando há modificador dinâmico")
    void shouldEnforceVariablePreparationCardLimitAndAllowDynamicModifiers() {
        // Arrange
        List<Card> handCards = player1.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.HAND)
                .limit(3)
                .toList();
        assertEquals(3, handCards.size());
        List<UUID> threeCardIds = handCards.stream().map(Card::getUuid).toList();

        // Act & Assert: Limite base (2) deve rejeitar 3 cartas
        assertEquals(2, engine.getMaxPreparationCardsForPlayer(player1.getUuid()));
        assertThrows(IllegalArgumentException.class, () ->
                engine.submitPreparationCards(player1.getUuid(), threeCardIds)
        );
        assertTrue(handCards.stream().allMatch(c -> c.getZoneId() == ZoneId.HAND));
        assertFalse(engine.isPlayer1Ready());

        // Arrange: Aplica modificador dinâmico de +1 carta permitida neste turno para o Jogador 1
        engine.addTurnPreparationCardLimitModifier(player1.getUuid(), 1);
        assertEquals(3, engine.getMaxPreparationCardsForPlayer(player1.getUuid()));

        // Act: Agora o envio das 3 cartas deve ser aceito
        assertDoesNotThrow(() ->
                engine.submitPreparationCards(player1.getUuid(), threeCardIds)
        );

        // Assert
        assertTrue(engine.isPlayer1Ready());
        assertEquals(3, engine.getGameState().getPlayer1PlayedCards().size());
        assertTrue(handCards.stream().allMatch(c -> c.getZoneId() == ZoneId.RESOLVE));

        // Cleanup reseta o modificador dinâmico para o padrão (2)
        engine.runCombatEndCleanup();
        assertEquals(2, engine.getMaxPreparationCardsForPlayer(player1.getUuid()));
    }

    @Test
    @DisplayName("4. Keyword INVOCAR: cartas invocadas na pilha de resolução não consomem nem são bloqueadas pelo limite da preparação")
    void shouldAllowInvokedCardsInResolutionWithoutCountingAgainstPreparationLimit() {
        // Arrange: Simula uma carta invocada automaticamente (ex: via INVOCAR) antes/durante a preparação
        Card invokedSpell = engine.invokeCard(player1, heroP1, "Bola de fogo");
        assertNotNull(invokedSpell);
        assertTrue(engine.getInvokedCardIdsInCurrentCombat().contains(invokedSpell.getUuid()));

        List<Card> manualHandCards = player1.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.HAND)
                .limit(2)
                .toList();
        assertEquals(2, manualHandCards.size());

        // Act & Assert 1: Jogador não pode forjar o reenvio do UUID da carta já invocada
        assertThrows(IllegalArgumentException.class, () ->
                engine.submitPreparationCards(player1.getUuid(), List.of(invokedSpell.getUuid()))
        );

        // Act 2: Jogador envia suas 2 cartas manuais normalmente além da carta já invocada
        engine.submitPreparationCards(
                player1.getUuid(),
                List.of(manualHandCards.get(0).getUuid(), manualHandCards.get(1).getUuid())
        );

        // Assert 2: A pilha contém a carta invocada + as 2 cartas jogadas da mão (total 3), mas o contador manual marca 2
        assertEquals(3, engine.getGameState().getPlayer1PlayedCards().size());
        assertTrue(engine.getGameState().getPlayer1PlayedCards().contains(invokedSpell));
        assertEquals(2, engine.getCardsPlayedByPlayerInCurrentTurn(player1.getUuid()));
        assertEquals(3, engine.getCardsPlayedOrInvokedByPlayerInCurrentCombat(player1.getUuid()));
    }

    @Test
    @DisplayName("5. Validação de Empunhadura (maxWeaponSlots): bloqueia excesso de armas e respeita aumento de slots na preparação")
    void shouldRejectWeaponsExceedingHeroMaxWeaponSlotsAndSupportSlotIncreasingEffects() {
        // Arrange: Adiciona 1 arma de empunhadura 2 e 1 arma de empunhadura 1 na mão do Jogador 1 (Herói tem maxWeaponSlots = 2)
        Card greatsword = createWeaponCard(player1.getUuid(), "Espada Bastarda", Colors.BLUE, 2);
        greatsword.setZoneId(ZoneId.HAND);
        Card rapier = createWeaponCard(player1.getUuid(), "Rapieira de Cristal", Colors.BLUE, 1);
        rapier.setZoneId(ZoneId.HAND);
        player1.getCurrentDeck().getCardList().addAll(List.of(greatsword, rapier));

        // Act & Assert 1: Tentar jogar ambas no mesmo turno (empunhadura total 3 > 2) deve falhar sem mutar estado
        assertThrows(IllegalArgumentException.class, () ->
                engine.submitPreparationCards(player1.getUuid(), List.of(greatsword.getUuid(), rapier.getUuid()))
        );
        assertEquals(ZoneId.HAND, greatsword.getZoneId());
        assertEquals(ZoneId.HAND, rapier.getZoneId());
        assertFalse(engine.isPlayer1Ready());

        // Arrange 2: Carta que aumenta os slots de arma para 3 na resolução ("Estilo Secreto do Caçador")
        Card tripleWieldCard = createActionCard(player1.getUuid(), "Estilo Secreto do Caçador", Colors.BLUE);
        tripleWieldCard.setZoneId(ZoneId.HAND);
        tripleWieldCard.setEffects(new ArrayList<>(List.of(new PlayActionSetWeaponSlotsEffect(3))));
        player1.getCurrentDeck().getCardList().add(tripleWieldCard);

        // Equipa previamente a Rapieira (ocupando 1/2 slots)
        rapier.setZoneId(ZoneId.ATTACHED);
        rapier.setAttachedToCardId(heroP1.getUuid());
        assertEquals(1, engine.getUsedWeaponSlotsForHero(heroP1.getUuid()));

        // Act & Assert 2: Tentar jogar a Espada de empunhadura 2 sozinha (1 usado + 2 = 3 > 2) deve ser rejeitado
        assertThrows(IllegalArgumentException.class, () ->
                engine.submitPreparationCards(player1.getUuid(), List.of(greatsword.getUuid()))
        );
        assertEquals(ZoneId.HAND, greatsword.getZoneId());

        // Act & Assert 3: Jogar "Estilo Secreto do Caçador" (slots -> 3) junto com a Espada de empunhadura 2 (1 + 2 <= 3) é permitido!
        assertDoesNotThrow(() ->
                engine.submitPreparationCards(player1.getUuid(), List.of(tripleWieldCard.getUuid(), greatsword.getUuid()))
        );
        assertTrue(engine.isPlayer1Ready());
    }

    @Test
    @DisplayName("6. Validação de Compatibilidade: deve rejeitar cartas com Cor, Afinidade, Ordem ou Classe incompatíveis com o Herói/Equipe")
    void shouldRejectCardsIncompatibleWithHeroOrTeam() {
        // Arrange: Jogador 1 tem apenas Cavaleiro Onear (BLUE, Ordem CAVALEIRO, Afinidade CRISTAL, Classe LUTADOR)
        Card illegalRedFireCard = createActionCard(player1.getUuid(), "Explosão Ignea", Colors.RED);
        illegalRedFireCard.setAfinidades(new ArrayList<>(List.of(Afinidade.FOGO)));
        illegalRedFireCard.setZoneId(ZoneId.HAND);

        Card illegalClassCard = createActionCard(player1.getUuid(), "Forja Runica", Colors.BLUE);
        illegalClassCard.setClasse(new ArrayList<>(List.of(Classe.CRIADOR)));
        illegalClassCard.setZoneId(ZoneId.HAND);

        Card illegalOrderCard = createActionCard(player1.getUuid(), "Decreto do Trono", Colors.BLUE);
        illegalOrderCard.setOrdem(new ArrayList<>(List.of(Ordem.GUARDIAO_DO_TRONO)));
        illegalOrderCard.setZoneId(ZoneId.HAND);

        player1.getCurrentDeck().getCardList().addAll(List.of(illegalRedFireCard, illegalClassCard, illegalOrderCard));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                engine.submitPreparationCards(player1.getUuid(), List.of(illegalRedFireCard.getUuid()))
        );
        assertEquals(ZoneId.HAND, illegalRedFireCard.getZoneId());

        assertThrows(IllegalArgumentException.class, () ->
                engine.submitPreparationCards(player1.getUuid(), List.of(illegalClassCard.getUuid()))
        );
        assertEquals(ZoneId.HAND, illegalClassCard.getZoneId());

        assertThrows(IllegalArgumentException.class, () ->
                engine.submitPreparationCards(player1.getUuid(), List.of(illegalOrderCard.getUuid()))
        );
        assertEquals(ZoneId.HAND, illegalOrderCard.getZoneId());
        assertFalse(engine.isPlayer1Ready());
    }

    @Test
    @DisplayName("7. Validação de Participante e Fase DISCARD: deve rejeitar jogador externo e descartes inválidos/duplicados")
    void shouldRejectInvalidDiscardAndUnauthorizedParticipantActions() {
        // Arrange: Jogador intruso não pertencente à partida
        UUID intruderId = UUID.randomUUID();

        // Act & Assert 1: Intruso não pode submeter preparação
        assertThrows(IllegalArgumentException.class, () ->
                engine.submitPreparationCards(intruderId, List.of())
        );

        // Avança legalmente para a fase DISCARD
        engine.submitPreparationCards(player1.getUuid(), List.of());
        engine.submitPreparationCards(player2.getUuid(), List.of());
        assertEquals(TurnPhase.DISCARD, engine.getTurnPhase());

        Card validHandCard = player1.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.HAND)
                .findFirst()
                .orElseThrow();
        Card deckCard = player1.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.DECK)
                .findFirst()
                .orElseThrow();

        // Act & Assert 2: Descarte com UUID duplicado ou carta no DECK deve falhar sem descartar nenhuma carta
        assertThrows(IllegalArgumentException.class, () ->
                engine.submitDiscard(player1.getUuid(), List.of(validHandCard.getUuid(), validHandCard.getUuid()))
        );
        assertEquals(ZoneId.HAND, validHandCard.getZoneId());
        assertFalse(engine.isPlayer1Ready());

        assertThrows(IllegalArgumentException.class, () ->
                engine.submitDiscard(player1.getUuid(), List.of(validHandCard.getUuid(), deckCard.getUuid()))
        );
        assertEquals(ZoneId.HAND, validHandCard.getZoneId());
        assertEquals(ZoneId.DECK, deckCard.getZoneId());
        assertFalse(engine.isPlayer1Ready());

        // Act & Assert 3: Descarte válido funciona normalmente
        assertDoesNotThrow(() ->
                engine.submitDiscard(player1.getUuid(), List.of(validHandCard.getUuid()))
        );
        assertEquals(ZoneId.DISCARD, validHandCard.getZoneId());
        assertTrue(engine.isPlayer1Ready());
    }
}

package com.alkateca.lendasdapoeira.controller;

import com.alkateca.lendasdapoeira.DTO.MatchDtos.*;
import com.alkateca.lendasdapoeira.engine.CardFactory;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.Colors;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.ZoneId;
import com.alkateca.lendasdapoeira.service.MatchmakingService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class MatchController {

    private final MatchmakingService matchmakingService;
    private final com.alkateca.lendasdapoeira.service.PlayerStorageService playerStorageService;
    private final com.alkateca.lendasdapoeira.service.PresetDeckService presetDeckService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MatchController(
            MatchmakingService matchmakingService,
            com.alkateca.lendasdapoeira.service.PlayerStorageService playerStorageService,
            com.alkateca.lendasdapoeira.service.PresetDeckService presetDeckService) {
        this.matchmakingService = matchmakingService;
        this.playerStorageService = playerStorageService;
        this.presetDeckService = presetDeckService;
    }

    @PostMapping("/matches/start-pve")
    public ResponseEntity<MatchmakingStatusResponseDTO> startPveMatch(
            @RequestParam(required = false) UUID playerId,
            @RequestBody(required = false) JoinQueueRequestDTO request) {

        UUID finalPlayerId = (request != null && request.getPlayerId() != null)
                ? request.getPlayerId()
                : (playerId != null ? playerId : UUID.randomUUID());

        String finalPlayerName = (request != null && request.getPlayerName() != null)
                ? request.getPlayerName()
                : "Viajante da Poeira";

        Deck incomingDeck = (request != null) ? request.getCurrentDeck() : null;

        Player humanPlayer = buildPlayerWithDefaultOrSavedDeck(finalPlayerId, finalPlayerName, incomingDeck);
        Player machinePlayer = presetDeckService.createRandomBotPlayer();

        MatchmakingStatusResponseDTO response = matchmakingService.startPveMatch(humanPlayer, machinePlayer);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/matchmaking/join")
    public ResponseEntity<MatchmakingStatusResponseDTO> joinMatchmaking(@RequestBody JoinQueueRequestDTO request) {
        Player player = buildPlayerWithDefaultOrSavedDeck(
                request.getPlayerId(),
                request.getPlayerName(),
                request.getCurrentDeck()
        );
        MatchmakingStatusResponseDTO response = matchmakingService.joinQueue(player);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/matchmaking/status")
    public ResponseEntity<MatchmakingStatusResponseDTO> checkMatchmakingStatus(@RequestParam UUID playerId) {
        return ResponseEntity.ok(matchmakingService.checkStatus(playerId));
    }

    @PostMapping("/matchmaking/leave")
    public ResponseEntity<Void> leaveMatchmaking(@RequestParam UUID playerId) {
        matchmakingService.leaveQueue(playerId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/matches/{matchId}/confirm-action")
    public ResponseEntity<MatchTimelineResponseDTO> confirmAction(
            @PathVariable UUID matchId,
            @RequestBody PlayerActionRequestDTO request) {
        try {
            MatchTimelineResponseDTO timeline = matchmakingService.processPlayerAction(matchId, request);
            return ResponseEntity.ok(timeline);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/matches/{matchId}/state")
    public ResponseEntity<MatchTimelineResponseDTO> getMatchState(
            @PathVariable UUID matchId,
            @RequestParam UUID viewerId,
            @RequestParam(defaultValue = "0") long sinceSequence) {
        try {
            MatchTimelineResponseDTO timeline = matchmakingService.getMatchStateSince(matchId, viewerId, sinceSequence);
            return ResponseEntity.ok(timeline);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Player buildPlayerWithDefaultOrSavedDeck(UUID playerId, String playerName, Deck incomingDeck) {
        UUID finalPlayerId = (playerId != null) ? playerId : UUID.randomUUID();
        String finalName = (playerName != null && !playerName.isBlank()) ? playerName : "Viajante da Poeira";

        Deck deck = (incomingDeck != null && incomingDeck.getHeroList() != null && !incomingDeck.getHeroList().isEmpty())
                ? incomingDeck
                : null;

        if (deck == null && playerStorageService != null) {
            Player savedPlayer = playerStorageService.getPlayer(finalPlayerId);
            if (savedPlayer != null) {
                if (savedPlayer.getName() != null && !savedPlayer.getName().isBlank()) {
                    finalName = savedPlayer.getName();
                }
                if (savedPlayer.getCurrentDeck() != null
                        && savedPlayer.getCurrentDeck().getHeroList() != null
                        && !savedPlayer.getCurrentDeck().getHeroList().isEmpty()) {
                    deck = savedPlayer.getCurrentDeck();
                } else if (savedPlayer.getDecks() != null && !savedPlayer.getDecks().isEmpty()) {
                    deck = savedPlayer.getDecks().get(savedPlayer.getDecks().size() - 1);
                }
            }
        }

        if (deck == null) {
            deck = createDefaultDeckFromJsonOrFallback();
        }

        List<HeroCard> catalogHeroes = presetDeckService != null ? presetDeckService.getCatalogHeroes() : List.of();
        List<Card> catalogCards = presetDeckService != null ? presetDeckService.getCatalogCards() : List.of();

        List<HeroCard> readyHeroes = new ArrayList<>();
        for (HeroCard hero : deck.getHeroList()) {
            HeroCard template = catalogHeroes.stream()
                    .filter(ch -> ch.getCardName() != null && ch.getCardName().equalsIgnoreCase(hero.getCardName()))
                    .findFirst()
                    .orElse(hero);

            HeroCard copy = new HeroCard();
            copy.setUuid(UUID.randomUUID());
            copy.setOwnerId(finalPlayerId);
            copy.setCardName(template.getCardName());
            copy.setCardType(CardType.HERO);
            copy.setColor(template.getColor());
            copy.setDescricao(template.getDescricao());
            copy.setEspirito(template.getEspirito());
            copy.setAtaque(template.getAtaque());
            copy.setDefesa(template.getDefesa());
            copy.setVidaMaxima(template.getVidaMaxima() > 0 ? template.getVidaMaxima() : 15);
            copy.setVidaAtual(copy.getVidaMaxima());
            copy.setMaxWeaponSlots(template.getMaxWeaponSlots() > 0 ? template.getMaxWeaponSlots() : 2);
            copy.setReducaoDano(template.getReducaoDano());
            copy.setDanoBonus(template.getDanoBonus());
            copy.setVulnerabilidade(template.getVulnerabilidade());
            copy.setRacas(template.getRacas() != null ? new ArrayList<>(template.getRacas()) : new ArrayList<>());
            copy.setAfinidades(template.getAfinidades() != null ? new ArrayList<>(template.getAfinidades()) : new ArrayList<>());
            copy.setOrdem(template.getOrdem() != null ? new ArrayList<>(template.getOrdem()) : new ArrayList<>());
            copy.setClasse(template.getClasse() != null ? new ArrayList<>(template.getClasse()) : new ArrayList<>());
            copy.setZoneId(ZoneId.BENCH);
            copy.setEstaVivo(true);
            copy.setEstaAtivo(true);
            CardFactory.buildCardLogic(copy);
            readyHeroes.add(copy);
        }

        List<Card> readyCards = new ArrayList<>();
        for (Card card : deck.getCardList()) {
            Card template = catalogCards.stream()
                    .filter(cc -> cc.getCardName() != null && cc.getCardName().equalsIgnoreCase(card.getCardName()))
                    .findFirst()
                    .orElse(card);

            Card copy = new Card();
            copy.setUuid(UUID.randomUUID());
            copy.setOwnerId(finalPlayerId);
            copy.setCardName(template.getCardName());
            copy.setCardType(template.getCardType());
            copy.setColor(template.getColor());
            copy.setItemType(template.getItemType());
            copy.setEmpunhadura(template.getEmpunhadura());
            copy.setDescricao(template.getDescricao());
            copy.setRacas(template.getRacas() != null ? new ArrayList<>(template.getRacas()) : new ArrayList<>());
            copy.setAfinidades(template.getAfinidades() != null ? new ArrayList<>(template.getAfinidades()) : new ArrayList<>());
            copy.setOrdem(template.getOrdem() != null ? new ArrayList<>(template.getOrdem()) : new ArrayList<>());
            copy.setClasse(template.getClasse() != null ? new ArrayList<>(template.getClasse()) : new ArrayList<>());
            copy.setZoneId(ZoneId.DECK);
            CardFactory.buildCardLogic(copy);
            readyCards.add(copy);
        }

        Deck readyDeck = new Deck(deck.getDeckName() != null ? deck.getDeckName() : "Deck de Batalha", readyCards, readyHeroes);
        return new Player(finalPlayerId, finalName, readyDeck);
    }

    private Deck createDefaultDeckFromJsonOrFallback() {
        List<HeroCard> heroes = new ArrayList<>();
        List<Card> cards = new ArrayList<>();

        try {
            InputStream heroesStream = new ClassPathResource("heros.json").getInputStream();
            List<HeroCard> allHeroes = objectMapper.readValue(heroesStream, new TypeReference<List<HeroCard>>() {});
            Collections.shuffle(allHeroes);
            for (int i = 0; i < Math.min(3, allHeroes.size()); i++) {
                heroes.add(allHeroes.get(i));
            }
        } catch (Exception e) {
            System.out.println("Aviso: Usando Heróis padrão de fallback (" + e.getMessage() + ")");
        }

        try {
            InputStream cardsStream = new ClassPathResource("cards.json").getInputStream();
            List<Card> allCards = objectMapper.readValue(cardsStream, new TypeReference<List<Card>>() {});
            if (!allCards.isEmpty()) {
                Collections.shuffle(allCards);
                for (int i = 0; i < 30; i++) {
                    Card template = allCards.get(i % allCards.size());
                    Card copy = new Card();
                    copy.setCardName(template.getCardName());
                    copy.setCardType(template.getCardType());
                    copy.setColor(template.getColor());
                    copy.setItemType(template.getItemType());
                    copy.setEmpunhadura(template.getEmpunhadura());
                    copy.setDescricao(template.getDescricao());
                    copy.setRacas(template.getRacas() != null ? new ArrayList<>(template.getRacas()) : new ArrayList<>());
                    copy.setAfinidades(template.getAfinidades() != null ? new ArrayList<>(template.getAfinidades()) : new ArrayList<>());
                    copy.setOrdem(template.getOrdem() != null ? new ArrayList<>(template.getOrdem()) : new ArrayList<>());
                    copy.setClasse(template.getClasse() != null ? new ArrayList<>(template.getClasse()) : new ArrayList<>());
                    cards.add(copy);
                }
            }
        } catch (Exception e) {
            System.out.println("Aviso: Usando Cartas padrão de fallback (" + e.getMessage() + ")");
        }

        if (heroes.size() < 3) {
            heroes.clear();
            String[] defaultHeroNames = {
                    "Enamor,O Grande Cavaleiro",
                    "Moyra, Aprendiz da Santa",
                    "Isenora, Santa das Laminas"
            };
            for (String name : defaultHeroNames) {
                HeroCard h = new HeroCard();
                h.setCardName(name);
                h.setColor(Colors.WHITE);
                h.setEspirito(3);
                h.setAtaque(4);
                h.setDefesa(2);
                h.setVidaMaxima(15);
                h.setVidaAtual(15);
                h.setMaxWeaponSlots(2);
                h.setDescricao("Herói de Batalha");
                heroes.add(h);
            }
        }

        if (cards.isEmpty()) {
            String[] defaultCardNames = {
                    "Lamina de Cristal", "Broche de Cristal", "Contra Ataque",
                    "Golpes Pesados", "Impulso de Poder", "Rapieira de Cristal"
            };
            for (int i = 0; i < 30; i++) {
                String name = defaultCardNames[i % defaultCardNames.length];
                Card c = new Card();
                c.setCardName(name);
                boolean isItem = name.contains("Cristal");
                c.setCardType(isItem ? CardType.ITEM : CardType.SPELL);
                c.setItemType(name.contains("Lamina") || name.contains("Rapieira") ? ItemType.ARMA : ItemType.NENHUM);
                c.setEmpunhadura(c.getItemType() == ItemType.ARMA ? 1 : 0);
                c.setColor(Colors.WHITE);
                c.setDescricao("Carta de Batalha: " + name);
                cards.add(c);
            }
        }

        return new Deck("Deck Sorteado", cards, heroes);
    }
}
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
import com.alkateca.lendasdapoeira.service.DeckValidatorService;
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
    private final DeckValidatorService deckValidatorService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MatchController(
            MatchmakingService matchmakingService,
            com.alkateca.lendasdapoeira.service.PlayerStorageService playerStorageService,
            com.alkateca.lendasdapoeira.service.PresetDeckService presetDeckService,
            DeckValidatorService deckValidatorService) {
        this.matchmakingService = matchmakingService;
        this.playerStorageService = playerStorageService;
        this.presetDeckService = presetDeckService;
        this.deckValidatorService = deckValidatorService;
    }

    @PostMapping("/matches/start-pve")
    public ResponseEntity<MatchmakingStatusResponseDTO> startPveMatch(
            @RequestParam(required = false) UUID playerId,
            @RequestBody(required = false) JoinQueueRequestDTO request) {

        if (request != null && request.getCurrentDeck() != null) {
            Player dbPlayer = playerStorageService.findById(request.getPlayerId());
            deckValidatorService.validateDeckOwnership(dbPlayer, request.getCurrentDeck());
        }

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
        if (request.getCurrentDeck() != null) {
            Player dbPlayer = playerStorageService.findById(request.getPlayerId());
            deckValidatorService.validateDeckOwnership(dbPlayer, request.getCurrentDeck());
        }

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
            if (request.getMatchId() != null && !matchId.equals(request.getMatchId())) {
                return ResponseEntity.badRequest().build();
            }
            request.setMatchId(matchId);
            MatchTimelineResponseDTO timeline = matchmakingService.processPlayerAction(matchId, request);
            return ResponseEntity.ok(timeline);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping({"/matches/{matchId}/forfeit", "/matches/{matchId}/leave"})
    public ResponseEntity<java.util.Map<String, Object>> forfeitOrLeaveMatch(
            @PathVariable UUID matchId,
            @RequestParam(required = false) UUID playerId,
            @RequestBody(required = false) java.util.Map<String, String> body) {

        UUID resolvedPlayerId = playerId;
        if (resolvedPlayerId == null && body != null && body.get("playerId") != null) {
            resolvedPlayerId = UUID.fromString(body.get("playerId"));
        }

        java.util.Map<String, Object> result = matchmakingService.forfeitOrLeaveMatch(matchId, resolvedPlayerId);
        return ResponseEntity.ok(result);
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

    // =========================================================================
    // ENDPOINTS DO SALÃO DE PARTIDAS (LOBBY PVP) E DESAFIOS DIRETOS
    // =========================================================================

    @PostMapping("/lobby/join")
    public ResponseEntity<LobbyStateResponseDTO> joinLobby(@RequestBody JoinQueueRequestDTO request) {
        if (request.getCurrentDeck() != null) {
            Player dbPlayer = playerStorageService.findById(request.getPlayerId());
            deckValidatorService.validateDeckOwnership(dbPlayer, request.getCurrentDeck());
        }

        Player player = buildPlayerWithDefaultOrSavedDeck(
                request.getPlayerId(),
                request.getPlayerName(),
                request.getCurrentDeck()
        );
        return ResponseEntity.ok(matchmakingService.joinOrHeartbeatLobby(player));
    }

    @GetMapping("/lobby/state")
    public ResponseEntity<LobbyStateResponseDTO> getLobbyState(@RequestParam UUID playerId) {
        return ResponseEntity.ok(matchmakingService.getLobbyState(playerId));
    }

    @PostMapping("/lobby/leave")
    public ResponseEntity<Void> leaveLobby(@RequestParam UUID playerId) {
        matchmakingService.leaveLobby(playerId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/lobby/challenge")
    public ResponseEntity<DirectChallengeDTO> sendChallenge(@RequestBody ChallengeRequestDTO request) {
        try {
            if (request.getChallengerDeck() != null) {
                Player dbPlayer = playerStorageService.findById(request.getChallengerId());
                deckValidatorService.validateDeckOwnership(dbPlayer, request.getChallengerDeck());
            }
            Player challenger = buildPlayerWithDefaultOrSavedDeck(
                    request.getChallengerId(),
                    request.getChallengerName(),
                    request.getChallengerDeck()
            );
            DirectChallengeDTO dto = matchmakingService.sendDirectChallenge(challenger, request.getTargetPlayerId());
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/lobby/challenge/{challengeId}/accept")
    public ResponseEntity<MatchmakingStatusResponseDTO> acceptChallenge(
            @PathVariable UUID challengeId,
            @RequestBody AcceptChallengeRequestDTO request) {
        try {
            if (request.getTargetDeck() != null) {
                Player dbPlayer = playerStorageService.findById(request.getTargetPlayerId());
                deckValidatorService.validateDeckOwnership(dbPlayer, request.getTargetDeck());
            }
            Player targetPlayer = buildPlayerWithDefaultOrSavedDeck(
                    request.getTargetPlayerId(),
                    request.getTargetPlayerName(),
                    request.getTargetDeck()
            );
            MatchmakingStatusResponseDTO matchResponse = matchmakingService.acceptDirectChallenge(challengeId, targetPlayer);
            return ResponseEntity.ok(matchResponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/lobby/challenge/{challengeId}/cancel")
    public ResponseEntity<Void> cancelChallenge(
            @PathVariable UUID challengeId,
            @RequestParam(required = false) UUID playerId) {
        matchmakingService.cancelDirectChallenge(challengeId, playerId);
        return ResponseEntity.ok().build();
    }

    private Player buildPlayerWithDefaultOrSavedDeck(UUID playerId, String playerName, Deck incomingDeck) {
        UUID finalPlayerId = (playerId != null) ? playerId : UUID.randomUUID();
        String finalName = (playerName != null && !playerName.isBlank()) ? playerName : "Viajante da Poeira";

        Deck deck = (incomingDeck != null && incomingDeck.getHeroList() != null && !incomingDeck.getHeroList().isEmpty())
                ? incomingDeck
                : null;

        Player savedPlayer = (playerStorageService != null) ? playerStorageService.getPlayer(finalPlayerId) : null;
        if (savedPlayer != null) {
            if (savedPlayer.getName() != null && !savedPlayer.getName().isBlank()) {
                finalName = savedPlayer.getName();
            }
            if (deck == null) {
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
            copy.setRarity(template.getRarity());
            copy.setCardSet(template.getCardSet());
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
            copy.setRarity(template.getRarity());
            copy.setCardSet(template.getCardSet());
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

        List<Card> readyLiberations = new ArrayList<>();
        if (deck.getLiberationList() != null) {
            for (Card libCard : deck.getLiberationList()) {
                if (libCard == null) continue;
                Card template = catalogCards.stream()
                        .filter(cc -> cc.getCardName() != null && cc.getCardName().equalsIgnoreCase(libCard.getCardName()))
                        .findFirst()
                        .orElse(libCard);

                Card copy = new Card();
                copy.setUuid(UUID.randomUUID());
                copy.setOwnerId(finalPlayerId);
                copy.setCardName(template.getCardName());
                copy.setCardType(template.getCardType() != null ? template.getCardType() : CardType.ACTION);
                copy.setColor(template.getColor() != null ? template.getColor() : Colors.NEUTRAL);
                copy.setRarity(template.getRarity());
                copy.setCardSet(template.getCardSet());
                copy.setItemType(template.getItemType() != null ? template.getItemType() : ItemType.NENHUM);
                copy.setEmpunhadura(template.getEmpunhadura());
                copy.setDescricao(template.getDescricao());
                copy.setUnica( true );
                copy.setLiberationCard(true);
                copy.setMissionType(template.getMissionType());
                copy.setMissionTargetValue(template.getMissionTargetValue());
                copy.setMissionCurrentProgress(0);
                copy.setTargetBaseHeroName(template.getTargetBaseHeroName());
                copy.setEvolvesIntoHeroName(template.getEvolvesIntoHeroName());
                copy.setRacas(template.getRacas() != null ? new ArrayList<>(template.getRacas()) : new ArrayList<>());
                copy.setAfinidades(template.getAfinidades() != null ? new ArrayList<>(template.getAfinidades()) : new ArrayList<>());
                copy.setOrdem(template.getOrdem() != null ? new ArrayList<>(template.getOrdem()) : new ArrayList<>());
                copy.setClasse(template.getClasse() != null ? new ArrayList<>(template.getClasse()) : new ArrayList<>());
                copy.setZoneId(ZoneId.LIBERATION);
                CardFactory.buildCardLogic(copy);
                readyLiberations.add(copy);
            }
        }

        Deck readyDeck = new Deck(
                deck.getDeckName() != null ? deck.getDeckName() : "Deck de Batalha",
                readyCards,
                readyHeroes,
                readyLiberations
        );
        Player matchPlayer = new Player(finalPlayerId, finalName, readyDeck);
        if (savedPlayer != null) {
            matchPlayer.setPermanentElo(savedPlayer.getPermanentElo() > 0 ? savedPlayer.getPermanentElo() : 1000);
            matchPlayer.setWeeklyElo(savedPlayer.getWeeklyElo() > 0 ? savedPlayer.getWeeklyElo() : 1000);
            matchPlayer.setLastWeeklyResetWeek(savedPlayer.getLastWeeklyResetWeek());
            matchPlayer.setGold(savedPlayer.getGold());
        }
        return matchPlayer;
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
                    copy.setRarity(template.getRarity());
                    copy.setCardSet(template.getCardSet());
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
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<java.util.Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
    }
}

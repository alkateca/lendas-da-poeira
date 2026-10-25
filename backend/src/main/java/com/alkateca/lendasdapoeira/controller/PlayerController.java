package com.alkateca.lendasdapoeira.controller;

import com.alkateca.lendasdapoeira.engine.CardFactory;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.service.DeckValidatorService;
import com.alkateca.lendasdapoeira.service.EloRatingService;
import com.alkateca.lendasdapoeira.service.PlayerStorageService;
import com.alkateca.lendasdapoeira.service.PresetDeckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/players")
@CrossOrigin(origins = "*")
public class PlayerController {

    @Autowired
    private PlayerStorageService playerStorageService;

    @Autowired
    private DeckValidatorService deckValidatorService;

    @Autowired
    private PresetDeckService presetDeckService;

    @Autowired
    private CardFactory cardFactory;

    @Autowired
    private EloRatingService eloRatingService;

    @PostMapping("/login")
    public ResponseEntity<Player> login(@RequestBody Map<String, String> payload) {
        String nick = payload.get("nick");
        if (nick == null || nick.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Player player = playerStorageService.loginOrRegister(nick);
        deckValidatorService.ensureStarterCollection(player, presetDeckService.createDefaultDeck(), cardFactory.getAllCatalogCards());
        playerStorageService.savePlayer(player);
        if (eloRatingService.ensurePlayerEloInitializedAndCurrentWeek(player)) {
            playerStorageService.savePlayer(player);
        }
        return ResponseEntity.ok(player);
    }

    @GetMapping("/{playerId}")
    public ResponseEntity<Player> getPlayerProfile(@PathVariable UUID playerId) {
        Player player = playerStorageService.getPlayer(playerId);
        if (player == null) return ResponseEntity.notFound().build();
        if (eloRatingService.ensurePlayerEloInitializedAndCurrentWeek(player)) {
            playerStorageService.savePlayer(player);
        }
        return ResponseEntity.ok(player);
    }

    @PostMapping(path = {"/{playerId}/decks", "/{uuid}/decks"})
    public ResponseEntity<Player> saveOrUpdateDeck(
            @PathVariable(value = "playerId", required = false) UUID playerId,
            @PathVariable(value = "uuid", required = false) UUID uuid,
            @RequestBody Deck incomingDeck) {

        UUID targetId = (playerId != null) ? playerId : uuid;
        Player player = playerStorageService.getPlayer(targetId);
        if (player == null) return ResponseEntity.notFound().build();

        deckValidatorService.validateDeckOwnership(player, incomingDeck);

        if (player.getDecks() == null) {
            player.setDecks(new ArrayList<>());
        }

        if (incomingDeck.getUuid() == null) {
            incomingDeck.setUuid(UUID.randomUUID());
            player.getDecks().add(incomingDeck);
        } else {
            player.getDecks().removeIf(d -> d.getUuid().equals(incomingDeck.getUuid()));
            player.getDecks().add(incomingDeck);
        }

        playerStorageService.savePlayer(player);
        return ResponseEntity.ok(player);
    }

    @GetMapping("/catalog")
    public ResponseEntity<Map<String, Object>> getFullCatalog() {
        return ResponseEntity.ok(Map.of(
                "heroes", presetDeckService.getCatalogHeroes(),
                "cards", presetDeckService.getCatalogCards()
        ));
    }

    @PutMapping("/{playerId}/active-deck/{deckId}")
    public ResponseEntity<Player> setActiveDeck(
            @PathVariable UUID playerId,
            @PathVariable UUID deckId) {

        Player player = playerStorageService.getPlayer(playerId);
        if (player == null) return ResponseEntity.notFound().build();

        Deck chosen = player.getDecks() != null ? player.getDecks().stream()
                .filter(d -> deckId.equals(d.getUuid()))
                .findFirst()
                .orElse(null) : null;

        if (chosen == null) {
            return ResponseEntity.badRequest().build();
        }

        DeckValidatorService.ValidationResult check = deckValidatorService.validateDeck(chosen);
        if (!check.isValidForPlay) {
            return ResponseEntity.badRequest().build();
        }

        player.setCurrentDeck(chosen);
        playerStorageService.savePlayer(player);
        return ResponseEntity.ok(player);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}

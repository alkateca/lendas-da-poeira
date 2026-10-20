package com.alkateca.lendasdapoeira.controller;

import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.service.PlayerStorageService;
import com.alkateca.lendasdapoeira.service.DeckValidatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/players")
@CrossOrigin(origins = "*")
public class PlayerController {

    @Autowired
    private PlayerStorageService storageService;

    @Autowired
    private DeckValidatorService validatorService;

    @PostMapping("/login")
    public ResponseEntity<Player> login(@RequestBody Map<String, String> payload) {
        String nick = payload.get("nick");
        if (nick == null || nick.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Player player = storageService.loginOrRegister(nick);
        return ResponseEntity.ok(player);
    }

    @PostMapping("/{playerId}/decks")
    public ResponseEntity<Player> saveOrUpdateDeck(@PathVariable UUID playerId, @RequestBody Deck deck) {
        Player player = storageService.getPlayer(playerId);
        if (player == null) return ResponseEntity.notFound().build();

        if (deck.getUuid() == null) {
            deck.setUuid(UUID.randomUUID());
            player.getDecks().add(deck);
        } else {
            player.getDecks().removeIf(d -> d.getUuid().equals(deck.getUuid()));
            player.getDecks().add(deck);
        }

        storageService.saveOrUpdatePlayer(player);
        return ResponseEntity.ok(player);
    }

    // Injete o PresetDeckService no PlayerController.java:
    @Autowired
    private com.alkateca.lendasdapoeira.service.PresetDeckService presetDeckService;

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

        Player player = storageService.getPlayer(playerId);
        if (player == null) return ResponseEntity.notFound().build();

        Deck chosen = player.getDecks().stream()
                .filter(d -> deckId.equals(d.getUuid()))
                .findFirst()
                .orElse(null);

        if (chosen == null) {
            return ResponseEntity.badRequest().build();
        }

        DeckValidatorService.ValidationResult check = validatorService.validateDeck(chosen);
        if (!check.isValidForPlay) {
            return ResponseEntity.badRequest().build();
        }

        player.setCurrentDeck(chosen);
        storageService.saveOrUpdatePlayer(player);
        return ResponseEntity.ok(player);
    }
}
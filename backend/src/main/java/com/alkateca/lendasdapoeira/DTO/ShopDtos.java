package com.alkateca.lendasdapoeira.DTO;

import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.Player;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShopDtos {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PurchaseRequestDTO {
        private UUID playerId;
        private String productId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevealedCardDTO {
        private String cardName;
        private String cardType; // "HERO", "SPELL", "ITEM", "ACTION", "THEURGY"
        private String color;
        private String rarity;   // "COMUM", "INCOMUM", "RARA", "LENDARIA"
        private String descricao;
        private boolean isHero;
        private int espirito;
        private int ataque;
        private int defesa;
        private int vidaMaxima;
        private int ownedCopiesAfterPull;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseResponseDTO {
        private String productId;
        private String productType;
        private String message;
        private Player updatedPlayer;
        private List<RevealedCardDTO> openedBoosterCards = new ArrayList<>();
        private Deck unlockedDeck;
    }
}
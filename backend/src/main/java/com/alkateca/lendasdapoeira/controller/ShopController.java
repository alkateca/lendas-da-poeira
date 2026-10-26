package com.alkateca.lendasdapoeira.controller;

import com.alkateca.lendasdapoeira.DTO.ShopDtos.PurchaseRequestDTO;
import com.alkateca.lendasdapoeira.DTO.ShopDtos.PurchaseResponseDTO;
import com.alkateca.lendasdapoeira.entity.MarketListing;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.entity.Product;
import com.alkateca.lendasdapoeira.service.MarketplaceService;
import com.alkateca.lendasdapoeira.service.PlayerStorageService;
import com.alkateca.lendasdapoeira.service.ShopService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/shop")
@CrossOrigin(origins = "*")
public class ShopController {

    private final ShopService shopService;
    private final PlayerStorageService playerStorageService;
    private final MarketplaceService marketplaceService;

    public ShopController(
            ShopService shopService,
            PlayerStorageService playerStorageService,
            MarketplaceService marketplaceService) {
        this.shopService = shopService;
        this.playerStorageService = playerStorageService;
        this.marketplaceService = marketplaceService;
    }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getProducts() {
        return ResponseEntity.ok(shopService.listProducts());
    }

    @PostMapping("/buy")
    public ResponseEntity<?> buyProduct(
            @RequestBody PurchaseRequestDTO request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        try {
            PurchaseResponseDTO response = marketplaceService.purchaseCatalogProductAtomically(
                    request.getPlayerId(),
                    request.getProductId(),
                    idempotencyKey
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // =========================================================================
    // ENDPOINTS DO MERCADO P2P (CARTAS AVULSAS DOS JOGADORES)
    // =========================================================================

    @GetMapping("/listings")
    public ResponseEntity<List<MarketListing>> getActiveListings() {
        return ResponseEntity.ok(marketplaceService.listActiveListings());
    }

    @PostMapping("/listings")
    public ResponseEntity<?> createListing(
            @RequestBody CreateListingRequestDTO request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        try {
            MarketListing listing = marketplaceService.createListing(
                    request.getSellerId(),
                    request.getCardName(),
                    request.isHeroCard(),
                    request.getPrice(),
                    idempotencyKey
            );
            Player updatedSeller = playerStorageService.findById(request.getSellerId());
            return ResponseEntity.ok(new MarketTransactionResponseDTO(
                    "Carta '" + listing.getCardName() + "' anunciada no mercado por " + listing.getPrice() + " Gold!",
                    listing,
                    updatedSeller
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/listings/{listingId}/buy")
    public ResponseEntity<?> buyListing(
            @PathVariable UUID listingId,
            @RequestBody ListingActionRequestDTO request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        try {
            MarketListing listing = marketplaceService.buyListing(
                    request.getPlayerId(),
                    listingId,
                    idempotencyKey
            );
            Player updatedBuyer = playerStorageService.findById(request.getPlayerId());
            return ResponseEntity.ok(new MarketTransactionResponseDTO(
                    "Carta '" + listing.getCardName() + "' adquirida com sucesso por " + listing.getPrice() + " Gold!",
                    listing,
                    updatedBuyer
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/listings/{listingId}/cancel")
    public ResponseEntity<?> cancelListing(
            @PathVariable UUID listingId,
            @RequestBody ListingActionRequestDTO request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        try {
            MarketListing listing = marketplaceService.cancelListing(
                    request.getPlayerId(),
                    listingId,
                    idempotencyKey
            );
            Player updatedSeller = playerStorageService.findById(request.getPlayerId());
            return ResponseEntity.ok(new MarketTransactionResponseDTO(
                    "Anúncio cancelado. '" + listing.getCardName() + "' retornou para sua coleção.",
                    listing,
                    updatedSeller
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateListingRequestDTO {
        private UUID sellerId;
        private String cardName;
        private boolean heroCard;
        private int price;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListingActionRequestDTO {
        private UUID playerId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketTransactionResponseDTO {
        private String message;
        private MarketListing listing;
        private Player player;
    }
}

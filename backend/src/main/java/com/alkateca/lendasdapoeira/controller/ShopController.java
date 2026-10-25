package com.alkateca.lendasdapoeira.controller;

import com.alkateca.lendasdapoeira.DTO.ShopDtos.PurchaseRequestDTO;
import com.alkateca.lendasdapoeira.DTO.ShopDtos.PurchaseResponseDTO;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.entity.Product;
import com.alkateca.lendasdapoeira.service.PlayerStorageService;
import com.alkateca.lendasdapoeira.service.ShopService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shop")
@CrossOrigin(origins = "*")
public class ShopController {

    private final ShopService shopService;
    private final PlayerStorageService playerStorageService;

    public ShopController(ShopService shopService, PlayerStorageService playerStorageService) {
        this.shopService = shopService;
        this.playerStorageService = playerStorageService;
    }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getProducts() {
        return ResponseEntity.ok(shopService.listProducts());
    }

    @PostMapping("/buy")
    public ResponseEntity<?> buyProduct(@RequestBody PurchaseRequestDTO request) {
        try {
            Player player = playerStorageService.findById(request.getPlayerId());
            if (player == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Jogador não encontrado."));
            }

            PurchaseResponseDTO response = shopService.purchaseProduct(player, request.getProductId());
            playerStorageService.savePlayer(player);

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
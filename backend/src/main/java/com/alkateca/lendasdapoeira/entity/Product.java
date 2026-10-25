package com.alkateca.lendasdapoeira.entity;

import com.alkateca.lendasdapoeira.enums.CardSet;
import com.alkateca.lendasdapoeira.enums.ProductType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    private String id;
    private String name;
    private String description;
    private ProductType type;
    private int price;
    private CardSet cardSet;
    private int cardsPerBooster; // Usado quando type == BOOSTER
    private double heroSlotProbability; // Chance do último slot do booster vir um Herói (ex: 0.25)
    private String presetDeckId; // Usado quando type == DECK
}
package com.alkateca.lendasdapoeira.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MarketListing {

    public enum ListingStatus {
        ACTIVE, SOLD, CANCELLED
    }

    private UUID id;
    private UUID sellerId;
    private String sellerName;
    private String cardName;
    private boolean heroCard;
    private int price;
    private ListingStatus status;
    private long createdAtEpochMs;
    private UUID buyerId;
}

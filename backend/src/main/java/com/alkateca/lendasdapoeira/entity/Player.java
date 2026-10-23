package com.alkateca.lendasdapoeira.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class Player {

    public static final int DEFAULT_INITIAL_ELO = 1000;

    private UUID uuid;
    private String name;
    private Deck currentDeck;
    private List<Deck> decks = new ArrayList<>();

    // Sistema de Ranqueamento Duplo
    private int permanentElo = DEFAULT_INITIAL_ELO;
    private int weeklyElo = DEFAULT_INITIAL_ELO;
    private String lastWeeklyResetWeek;

    // Construtor compatível com o restante do sistema
    public Player(UUID uuid, String name, Deck currentDeck) {
        this.uuid = uuid;
        this.name = name;
        this.currentDeck = currentDeck;
        this.permanentElo = DEFAULT_INITIAL_ELO;
        this.weeklyElo = DEFAULT_INITIAL_ELO;
    }
}

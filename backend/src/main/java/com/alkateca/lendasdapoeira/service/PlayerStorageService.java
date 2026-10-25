package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.entity.Player;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Service
public class PlayerStorageService {

    private final ObjectMapper mapper = new ObjectMapper();
    private final File dbFile = new File("data/players.json");
    private Map<UUID, Player> playerDatabase = new HashMap<>();

    @PostConstruct
    public void init() {
        try {
            File dir = new File("data");
            if (!dir.exists()) dir.mkdir();
            if (dbFile.exists()) {
                List<Player> list = mapper.readValue(dbFile, new TypeReference<List<Player>>() {});
                for (Player p : list) {
                    playerDatabase.put(p.getUuid(), p);
                }
                System.out.println("Base de dados de jogadores carregada: " + playerDatabase.size() + " perfis.");
            }
        } catch (IOException e) {
            System.err.println("Erro ao carregar players.json: " + e.getMessage());
        }
    }

    public synchronized void saveToDisk() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(dbFile, new ArrayList<>(playerDatabase.values()));
        } catch (IOException e) {
            System.err.println("Erro ao salvar players.json: " + e.getMessage());
        }
    }

    public Player loginOrRegister(String nick) {
        Optional<Player> existing = playerDatabase.values().stream()
                .filter(p -> p.getName().equalsIgnoreCase(nick.trim()))
                .findFirst();

        if (existing.isPresent()) {
            return existing.get();
        }

        Player newPlayer = new Player();
        newPlayer.setUuid(UUID.randomUUID());
        newPlayer.setName(nick.trim());
        newPlayer.setDecks(new ArrayList<>());

        playerDatabase.put(newPlayer.getUuid(), newPlayer);
        saveToDisk();
        return newPlayer;
    }

    public Player getPlayer(UUID uuid) {
        return playerDatabase.get(uuid);
    }

    public Player findById(UUID uuid) {
        return getPlayer(uuid);
    }

    public void saveOrUpdatePlayer(Player player) {
        playerDatabase.put(player.getUuid(), player);
        saveToDisk();
    }

    public void savePlayer(Player player) {
        saveOrUpdatePlayer(player);
    }

    public synchronized void resetAllPlayersWeeklyEloIfNeeded(java.util.function.Function<Player, Boolean> resetCheck) {
        boolean anyChanged = false;
        for (Player player : playerDatabase.values()) {
            if (Boolean.TRUE.equals(resetCheck.apply(player))) {
                anyChanged = true;
            }
        }
        if (anyChanged) {
            saveToDisk();
        }
    }


}

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
    private final File dbFile;
    private boolean diskPersistenceEnabled = false;
    private Map<UUID, Player> playerDatabase = new HashMap<>();

    public PlayerStorageService() {
        this.dbFile = resolveDefaultDbFile();
    }

    public PlayerStorageService(File customDbFile, boolean diskPersistenceEnabled) {
        this.dbFile = customDbFile != null ? customDbFile : resolveDefaultDbFile();
        this.diskPersistenceEnabled = diskPersistenceEnabled;
    }

    private static File resolveDefaultDbFile() {
        File backendDir = new File("backend");
        if (backendDir.exists() && backendDir.isDirectory()) {
            return new File(backendDir, "data/players.json");
        }
        return new File("data/players.json");
    }

    private boolean isBotPlayer(Player player) {
        if (player == null || player.getUuid() == null) return true;
        if (PresetDeckService.PLAYER_1_UUID.equals(player.getUuid())
                || PresetDeckService.PLAYER_2_UUID.equals(player.getUuid())) {
            return true;
        }
        return player.getName() != null && player.getName().startsWith("\uD83E\uDD16 Bot");
    }

    @PostConstruct
    public synchronized void init() {
        this.diskPersistenceEnabled = true;
        try {
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            if (dbFile.exists()) {
                List<Player> list = mapper.readValue(dbFile, new TypeReference<List<Player>>() {});
                boolean removedBot = false;
                for (Player p : list) {
                    if (isBotPlayer(p)) {
                        removedBot = true;
                        continue;
                    }
                    playerDatabase.put(p.getUuid(), p);
                }
                if (removedBot) {
                    saveToDisk();
                }
                System.out.println("Base de dados de jogadores carregada: " + playerDatabase.size() + " perfis.");
            }
        } catch (IOException e) {
            System.err.println("Erro ao carregar players.json: " + e.getMessage());
        }
    }

    public synchronized void saveToDisk() {
        if (!diskPersistenceEnabled) return;
        try {
            File parentDir = dbFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(dbFile, new ArrayList<>(playerDatabase.values()));
        } catch (IOException e) {
            System.err.println("Erro ao salvar players.json: " + e.getMessage());
        }
    }

    public synchronized Player loginOrRegister(String nick) {
        Optional<Player> existing = playerDatabase.values().stream()
                .filter(p -> p.getName() != null && p.getName().equalsIgnoreCase(nick.trim()))
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

    public synchronized Player getPlayer(UUID uuid) {
        return playerDatabase.get(uuid);
    }

    public synchronized Player findById(UUID uuid) {
        return getPlayer(uuid);
    }

    public synchronized void saveOrUpdatePlayer(Player player) {
        if (isBotPlayer(player)) return;
        playerDatabase.put(player.getUuid(), player);
        saveToDisk();
    }

    public synchronized void savePlayer(Player player) {
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

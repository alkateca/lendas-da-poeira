package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.DTO.MatchDtos.MatchmakingStatusResponseDTO;
import com.alkateca.lendasdapoeira.DTO.story.*;
import com.alkateca.lendasdapoeira.engine.CardFactory;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.Colors;
import com.alkateca.lendasdapoeira.enums.ZoneId;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StoryMissionManagerService {

    private final StoryMissionLoaderService loaderService;
    private final StoryTriggerEvaluator triggerEvaluator;
    private final MatchmakingService matchmakingService;
    private final PresetDeckService presetDeckService;
    private final PlayerStorageService playerStorageService;

    private final Map<String, Set<String>> completedMissionsByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, StoryMissionDefinition> activeMissionByMatchId = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> triggeredDialoguesByMatchId = new ConcurrentHashMap<>();

    public StoryMissionManagerService(
            StoryMissionLoaderService loaderService,
            StoryTriggerEvaluator triggerEvaluator,
            MatchmakingService matchmakingService,
            PresetDeckService presetDeckService,
            PlayerStorageService playerStorageService
    ) {
        this.loaderService = loaderService;
        this.triggerEvaluator = triggerEvaluator;
        this.matchmakingService = matchmakingService;
        this.presetDeckService = presetDeckService;
        this.playerStorageService = playerStorageService;
    }

    public List<StoryMissionSummaryDTO> getMissionsCatalogForPlayer(String playerId) {
        Set<String> completed = (playerId != null)
                ? completedMissionsByPlayer.getOrDefault(playerId, Collections.emptySet())
                : Collections.emptySet();

        List<StoryMissionDefinition> allMissions = loaderService.listAllMissions();

        return allMissions.stream().map(mission -> {
            boolean isCompleted = completed.contains(mission.id());
            boolean isUnlocked = mission.capitulo() <= 1
                    || isPreviousChapterCompleted(allMissions, completed, mission);
            return StoryMissionSummaryDTO.fromDefinition(mission, isUnlocked, isCompleted);
        }).toList();
    }

    private boolean isPreviousChapterCompleted(
            List<StoryMissionDefinition> allMissions,
            Set<String> completedIds,
            StoryMissionDefinition current
    ) {
        return allMissions.stream()
                .filter(m -> m.cor().equalsIgnoreCase(current.cor()) && m.capitulo() == current.capitulo() - 1)
                .findFirst()
                .map(prev -> completedIds.contains(prev.id()))
                .orElse(true);
    }

    /**
     * Instancia os Players humano e máquina com o CardFactory e inicia a partida PvE real no MatchmakingService.
     */
    public StartStoryMissionResponseDTO startStoryMatch(
            StoryMissionDefinition mission,
            UUID playerId,
            String playerName
    ) {
        Player savedPlayer = (playerStorageService != null) ? playerStorageService.getPlayer(playerId) : null;
        String finalPlayerName = (savedPlayer != null && savedPlayer.getName() != null && !savedPlayer.getName().isBlank())
                ? savedPlayer.getName()
                : playerName;

        // 1. Constrói o baralho temático do jogador para a cor da missão
        Deck playerStoryDeck = buildStoryDeckForPlayer(playerId, mission);
        Player humanPlayer = new Player(playerId, finalPlayerName, playerStoryDeck);
        if (savedPlayer != null) {
            humanPlayer.setPermanentElo(savedPlayer.getPermanentElo() > 0 ? savedPlayer.getPermanentElo() : 1000);
            humanPlayer.setWeeklyElo(savedPlayer.getWeeklyElo() > 0 ? savedPlayer.getWeeklyElo() : 1000);
            humanPlayer.setGold(savedPlayer.getGold());
        }

        // 2. Constrói o baralho WHITE da máquina ("Ecos da Memória")
        UUID botUuid = UUID.randomUUID();
        Deck aiWhiteDeck = buildWhiteArchiveDeckForAi(botUuid, mission);
        Player machinePlayer = new Player(botUuid, "Ecos da Memória (WHITE)", aiWhiteDeck);

        // 3. Inicia a partida PvE através do MatchmakingService existente
        MatchmakingStatusResponseDTO matchStatus = matchmakingService.startPveMatch(humanPlayer, machinePlayer);
        UUID matchId = matchStatus.getMatchId();

        if (matchId != null) {
            activeMissionByMatchId.put(matchId, mission);
            triggeredDialoguesByMatchId.put(matchId, ConcurrentHashMap.newKeySet());
        }

        List<Object> snapshots = matchStatus.getInitialSnapshots() != null
                ? new ArrayList<>(matchStatus.getInitialSnapshots())
                : List.of();

        return new StartStoryMissionResponseDTO(
                matchId != null ? matchId.toString() : UUID.randomUUID().toString(),
                playerId.toString(),
                snapshots,
                mission
        );
    }

    public StartStoryMissionResponseDTO startStoryMatch(
            StoryMissionDefinition mission,
            String playerId,
            String playerName
    ) {
        UUID uuid;
        try {
            uuid = (playerId != null && !playerId.isBlank()) ? UUID.fromString(playerId) : UUID.randomUUID();
        } catch (IllegalArgumentException e) {
            uuid = UUID.nameUUIDFromBytes(playerId.getBytes());
        }
        return startStoryMatch(mission, uuid, playerName);
    }

    /**
     * Monta o baralho do jogador filtrando os Heróis e Cartas da cor da missão no catálogo oficial.
     */
    private Deck buildStoryDeckForPlayer(UUID ownerId, StoryMissionDefinition mission) {
        Colors targetColor = parseColorOrDefault(mission.cor(), Colors.RED);
        List<HeroCard> catalogHeroes = presetDeckService != null ? presetDeckService.getCatalogHeroes() : List.of();
        List<Card> catalogCards = presetDeckService != null ? presetDeckService.getCatalogCards() : List.of();

        List<HeroCard> colorHeroes = catalogHeroes.stream()
                .filter(h -> h.getColor() == targetColor)
                .limit(3)
                .toList();

        if (colorHeroes.size() < 3 && !catalogHeroes.isEmpty()) {
            colorHeroes = catalogHeroes.stream().limit(3).toList();
        }

        List<Card> colorCards = catalogCards.stream()
                .filter(c -> c.getColor() == targetColor || c.getColor() == Colors.NEUTRAL)
                .toList();

        if (colorCards.isEmpty()) {
            colorCards = catalogCards;
        }

        List<HeroCard> readyHeroes = colorHeroes.stream()
                .map(template -> cloneAndBuildHero(template, ownerId))
                .toList();

        List<Card> readyCards = new ArrayList<>();
        if (!colorCards.isEmpty()) {
            for (int i = 0; i < 30; i++) {
                Card template = colorCards.get(i % colorCards.size());
                readyCards.add(cloneAndBuildCard(template, ownerId));
            }
        }

        return new Deck(
                "Registo Memorial - " + mission.cor(),
                readyCards,
                readyHeroes,
                new ArrayList<>()
        );
    }

    /**
     * Monta o baralho da IA usando a lista aiDeck do YAML ou os Guardiões WHITE dos Arquivos
     * (Alucinação Cintilante, Alka e Leone).
     */
    private Deck buildWhiteArchiveDeckForAi(UUID botOwnerId, StoryMissionDefinition mission) {
        List<HeroCard> catalogHeroes = presetDeckService != null ? presetDeckService.getCatalogHeroes() : List.of();
        List<Card> catalogCards = presetDeckService != null ? presetDeckService.getCatalogCards() : List.of();

        List<String> requestedHeroes = (mission.aiDeck() != null && mission.aiDeck().heros() != null)
                ? mission.aiDeck().heros()
                : List.of();

        List<HeroCard> selectedHeroTemplates = new ArrayList<>();

        // Tenta encontrar por UUID ou Nome definido no YAML
        for (String ref : requestedHeroes) {
            catalogHeroes.stream()
                    .filter(h -> (h.getUuid() != null && h.getUuid().toString().equalsIgnoreCase(ref))
                            || (h.getCardName() != null && h.getCardName().equalsIgnoreCase(ref)))
                    .findFirst()
                    .ifPresent(selectedHeroTemplates::add);
        }

        // Preenche com os Heróis WHITE dos Arquivos (Alucinação Cintilante, Alka, Leone) se faltar
        if (selectedHeroTemplates.size() < 3) {
            List<HeroCard> whiteHeroes = catalogHeroes.stream()
                    .filter(h -> h.getColor() == Colors.WHITE && !selectedHeroTemplates.contains(h))
                    .toList();
            for (HeroCard wh : whiteHeroes) {
                if (selectedHeroTemplates.size() >= 3) break;
                selectedHeroTemplates.add(wh);
            }
        }

        // Último fallback caso o catálogo não tenha 3 heróis WHITE
        if (selectedHeroTemplates.size() < 3 && !catalogHeroes.isEmpty()) {
            for (HeroCard anyHero : catalogHeroes) {
                if (selectedHeroTemplates.size() >= 3) break;
                if (!selectedHeroTemplates.contains(anyHero)) {
                    selectedHeroTemplates.add(anyHero);
                }
            }
        }

        List<HeroCard> readyBotHeroes = selectedHeroTemplates.stream()
                .map(template -> cloneAndBuildHero(template, botOwnerId))
                .toList();

        // Seleciona cartas do YAML ou cartas WHITE/NEUTRAL do catálogo
        List<Card> selectedCardTemplates = new ArrayList<>();
        List<String> requestedCards = (mission.aiDeck() != null && mission.aiDeck().cards() != null)
                ? mission.aiDeck().cards()
                : List.of();

        for (String cardRef : requestedCards) {
            catalogCards.stream()
                    .filter(c -> (c.getUuid() != null && c.getUuid().toString().equalsIgnoreCase(cardRef))
                            || (c.getCardName() != null && c.getCardName().equalsIgnoreCase(cardRef)))
                    .findFirst()
                    .ifPresent(selectedCardTemplates::add);
        }

        if (selectedCardTemplates.isEmpty()) {
            selectedCardTemplates = catalogCards.stream()
                    .filter(c -> c.getColor() == Colors.WHITE || c.getColor() == Colors.NEUTRAL)
                    .toList();
        }
        if (selectedCardTemplates.isEmpty()) {
            selectedCardTemplates = catalogCards;
        }

        List<Card> readyBotCards = new ArrayList<>();
        if (!selectedCardTemplates.isEmpty()) {
            for (int i = 0; i < 30; i++) {
                Card template = selectedCardTemplates.get(i % selectedCardTemplates.size());
                readyBotCards.add(cloneAndBuildCard(template, botOwnerId));
            }
        }

        return new Deck(
                "Ecos da Memória (WHITE)",
                readyBotCards,
                readyBotHeroes,
                new ArrayList<>()
        );
    }

    private HeroCard cloneAndBuildHero(HeroCard template, UUID ownerId) {
        HeroCard copy = new HeroCard();
        copy.setUuid(UUID.randomUUID());
        copy.setOwnerId(ownerId);
        copy.setCardName(template.getCardName());
        copy.setCardType(CardType.HERO);
        copy.setColor(template.getColor());
        copy.setRarity(template.getRarity());
        copy.setCardSet(template.getCardSet());
        copy.setDescricao(template.getDescricao());
        copy.setEspirito(template.getEspirito());
        copy.setAtaque(template.getAtaque());
        copy.setDefesa(template.getDefesa());
        copy.setVidaMaxima(template.getVidaMaxima() > 0 ? template.getVidaMaxima() : 15);
        copy.setVidaAtual(copy.getVidaMaxima());
        copy.setMaxWeaponSlots(template.getMaxWeaponSlots() > 0 ? template.getMaxWeaponSlots() : 2);
        copy.setReducaoDano(template.getReducaoDano());
        copy.setDanoBonus(template.getDanoBonus());
        copy.setVulnerabilidade(template.getVulnerabilidade());
        copy.setRacas(template.getRacas() != null ? new ArrayList<>(template.getRacas()) : new ArrayList<>());
        copy.setAfinidades(template.getAfinidades() != null ? new ArrayList<>(template.getAfinidades()) : new ArrayList<>());
        copy.setOrdem(template.getOrdem() != null ? new ArrayList<>(template.getOrdem()) : new ArrayList<>());
        copy.setClasse(template.getClasse() != null ? new ArrayList<>(template.getClasse()) : new ArrayList<>());
        copy.setZoneId(ZoneId.BENCH);
        copy.setEstaVivo(true);
        copy.setEstaAtivo(true);
        CardFactory.buildCardLogic(copy);
        return copy;
    }

    private Card cloneAndBuildCard(Card template, UUID ownerId) {
        Card copy = new Card();
        copy.setUuid(UUID.randomUUID());
        copy.setOwnerId(ownerId);
        copy.setCardName(template.getCardName());
        copy.setCardType(template.getCardType());
        copy.setColor(template.getColor());
        copy.setRarity(template.getRarity());
        copy.setCardSet(template.getCardSet());
        copy.setItemType(template.getItemType());
        copy.setEmpunhadura(template.getEmpunhadura());
        copy.setDescricao(template.getDescricao());
        copy.setRacas(template.getRacas() != null ? new ArrayList<>(template.getRacas()) : new ArrayList<>());
        copy.setAfinidades(template.getAfinidades() != null ? new ArrayList<>(template.getAfinidades()) : new ArrayList<>());
        copy.setOrdem(template.getOrdem() != null ? new ArrayList<>(template.getOrdem()) : new ArrayList<>());
        copy.setClasse(template.getClasse() != null ? new ArrayList<>(template.getClasse()) : new ArrayList<>());
        copy.setZoneId(ZoneId.DECK);
        CardFactory.buildCardLogic(copy);
        return copy;
    }

    private Colors parseColorOrDefault(String colorStr, Colors fallback) {
        if (colorStr == null || colorStr.isBlank()) return fallback;
        try {
            return Colors.valueOf(colorStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    public boolean markMissionAsCompleted(String playerId, String missionId) {
        if (playerId == null || playerId.isBlank()) return false;
        return completedMissionsByPlayer
                .computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet())
                .add(missionId);
    }

    public List<ScriptedDialogueDTO> checkCardPlayedTriggers(UUID matchId, String cardName) {
        StoryMissionDefinition mission = activeMissionByMatchId.get(matchId);
        Set<String> triggered = triggeredDialoguesByMatchId.get(matchId);
        if (mission == null || triggered == null) return List.of();
        return triggerEvaluator.evaluateCardPlayed(mission, triggered, cardName);
    }

    public List<ScriptedDialogueDTO> checkCardPlayedTriggers(String matchId, String cardName) {
        if (matchId == null) return List.of();
        try {
            return checkCardPlayedTriggers(UUID.fromString(matchId), cardName);
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    public List<ScriptedDialogueDTO> checkHeroDeathTriggers(UUID matchId, String deadHeroName) {
        StoryMissionDefinition mission = activeMissionByMatchId.get(matchId);
        Set<String> triggered = triggeredDialoguesByMatchId.get(matchId);
        if (mission == null || triggered == null) return List.of();
        return triggerEvaluator.evaluateHeroDeath(mission, triggered, deadHeroName);
    }

    public List<ScriptedDialogueDTO> checkHeroDeathTriggers(String matchId, String deadHeroName) {
        if (matchId == null) return List.of();
        try {
            return checkHeroDeathTriggers(UUID.fromString(matchId), deadHeroName);
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }
}

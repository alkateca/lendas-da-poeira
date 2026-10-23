package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.engine.CardFactory;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.ZoneId;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;

@Service
public class PresetDeckService {

    public static final UUID PLAYER_1_UUID = UUID.fromString("a1b2c3d4-0000-0000-0000-000000000001");
    public static final UUID PLAYER_2_UUID = UUID.fromString("a1b2c3d4-0000-0000-0000-000000000002");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    private List<HeroCard> catalogHeroes = new ArrayList<>();
    private List<Card> catalogCards = new ArrayList<>();

    private static class BotDeckTemplate {
        final String botDisplayName;
        final String deckName;
        final List<String> heroNames;
        final List<String> cardNames;

        BotDeckTemplate(String botDisplayName, String deckName, List<String> heroNames, List<String> cardNames) {
            this.botDisplayName = botDisplayName;
            this.deckName = deckName;
            this.heroNames = heroNames;
            this.cardNames = cardNames;
        }
    }

    private final List<BotDeckTemplate> botDeckPool = new ArrayList<>();

    @PostConstruct
    public void loadJsonCatalogs() {
        try {
            InputStream heroesStream = new ClassPathResource("heros.json").getInputStream();
            this.catalogHeroes = objectMapper.readValue(heroesStream, new TypeReference<List<HeroCard>>() {});

            InputStream cardsStream = new ClassPathResource("cards.json").getInputStream();
            this.catalogCards = objectMapper.readValue(cardsStream, new TypeReference<List<Card>>() {});

            initBotDeckPool();
            System.out.println("Catálogo carregado: " + catalogHeroes.size() + " heróis, "
                    + catalogCards.size() + " cartas e " + botDeckPool.size() + " baralhos de Bot.");
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar heros.json ou cards.json: " + e.getMessage(), e);
        }
    }

    public List<HeroCard> getCatalogHeroes() {
        return this.catalogHeroes;
    }

    public List<Card> getCatalogCards() {
        return this.catalogCards;
    }

    /**
     * Inicializa a lista de baralhos de 30 cartas (máx. 3 cópias de cada) para a Máquina (Bot)
     */
    private void initBotDeckPool() {
        botDeckPool.clear();

        // 1. BOT AZUL: Cavaleiros de Cristal (30 cartas)
        botDeckPool.add(new BotDeckTemplate(
                "🤖 Bot - Ordem de Cristal",
                "Cavaleiros de Cristal",
                List.of("Isenora, Santa das Laminas", "Cavaleiro Onear", "Enamor,O Grande Cavaleiro"),
                List.of(
                        "Quimera", "Quimera", "Quimera",
                        "Broche de Cristal", "Broche de Cristal", "Broche de Cristal",
                        "Lamina de Cristal", "Lamina de Cristal", "Lamina de Cristal",
                        "Dragast", "Dragast", "Dragast",
                        "Rapieira de Cristal", "Rapieira de Cristal", "Rapieira de Cristal",
                        "Contra Ataque", "Contra Ataque", "Contra Ataque",
                        "Ataque Mágico", "Ataque Mágico", "Ataque Mágico",
                        "Golpes Pesados", "Golpes Pesados", "Golpes Pesados",
                        "Ponto Final", "Ponto Final",
                        "Auto Defesa Magica", "Auto Defesa Magica",
                        "Determinação Cristalina", "Impulso de Poder"
                )
        ));

        // 2. BOT VERMELHO: Caçadores e Chamas (30 cartas)
        botDeckPool.add(new BotDeckTemplate(
                "🤖 Bot - Caçadores das Cinzas",
                "Arsenal dos Caçadores",
                List.of("Xia, Caçador de Bestas", "Xamã das cinzas", "Alix, Dragão Flamejante"),
                List.of(
                        "Sica", "Sica", "Sica",
                        "Sino do Caçador", "Sino do Caçador", "Sino do Caçador",
                        "Arpão terrestre", "Arpão terrestre", "Arpão terrestre",
                        "Bomba de fumaça", "Bomba de fumaça", "Bomba de fumaça",
                        "Tesouro Cristalino dos Caçadores", "Tesouro Cristalino dos Caçadores", "Tesouro Cristalino dos Caçadores",
                        "Afie suas Lâminas", "Afie suas Lâminas",
                        "Estilo Secreto do Caçador", "Estilo Secreto do Caçador",
                        "Corte seus Pescoços", "Corte seus Pescoços",
                        "Peitoral de brasas", "Peitoral de brasas", "Peitoral de brasas",
                        "Escudo ardente", "Escudo ardente",
                        "Muralha de Fogo", "Muralha de Fogo",
                        "Cauterizar", "Cauterizar"
                )
        ));

        // 2B. BOT VERMELHO: Magos de Fogo (30 cartas)
        botDeckPool.add(new BotDeckTemplate(
                "🤖 Bot - Círculo das Chamas",
                "Magos de Fogo",
                List.of("Xamã das cinzas", "Alix, Dragão Flamejante", "Constructo das Chamas, Sif"),
                List.of(
                        "Bola de fogo", "Bola de fogo", "Bola de fogo",
                        "Maestria com Chamas", "Maestria com Chamas", "Maestria com Chamas",
                        "Alma em Chamas", "Alma em Chamas", "Alma em Chamas",
                        "Muralha de Fogo", "Muralha de Fogo", "Muralha de Fogo",
                        "Cauterizar", "Cauterizar", "Cauterizar",
                        "Peitoral de brasas", "Peitoral de brasas", "Peitoral de brasas",
                        "Escudo ardente", "Escudo ardente", "Escudo ardente",
                        "Tesouro Cristalino dos Caçadores", "Tesouro Cristalino dos Caçadores", "Tesouro Cristalino dos Caçadores",
                        "Sica", "Sica", "Sica",
                        "Ração de emergencia", "Ração de emergencia", "Ração de emergencia"
                )
        ));

        // 3. BOT PRETO: Legião Necromante / Zumbis (30 cartas)
        botDeckPool.add(new BotDeckTemplate(
                "🤖 Bot - Legião Necromante",
                "Horda das Areias",
                List.of("Rainha Goblin", "Quimera Carniceira", "Necromante das Areias"),
                List.of(
                        "Homunculo Carniceiro", "Homunculo Carniceiro", "Homunculo Carniceiro",
                        "Quimera Negra", "Quimera Negra", "Quimera Negra",
                        "Atmosfera Pesada", "Atmosfera Pesada", "Atmosfera Pesada",
                        "Ritos Fúnebres", "Ritos Fúnebres", "Ritos Fúnebres",
                        "Exumação", "Exumação", "Exumação",
                        "Garra espectral", "Garra espectral", "Garra espectral",
                        "Bola de fogo", "Bola de fogo", "Bola de fogo",
                        "Ração de emergencia", "Ração de emergencia", "Ração de emergencia",
                        "Mirar na cabeça", "Mirar na cabeça", "Mirar na cabeça",
                        "Estrela do amanhã", "Convocação", "Banir!"
                )
        ));

        // 4. BOT VERDE: Constructos Gélidos (30 cartas)
        botDeckPool.add(new BotDeckTemplate(
                "🤖 Bot - Constructos Gélidos",
                "Fúria de Fenrir",
                List.of("Kael, Cavaleiro Bestial", "Hati, Constructo Gélido", "Fenrir, o Grande Devorador"),
                List.of(
                        "Machado de Gelo", "Machado de Gelo", "Machado de Gelo",
                        "Grande Machado Sombrio", "Grande Machado Sombrio", "Grande Machado Sombrio",
                        "Barreira de Gelo", "Barreira de Gelo", "Barreira de Gelo",
                        "Mordida Glacial", "Mordida Glacial", "Mordida Glacial",
                        "Luz da Lua", "Luz da Lua", "Luz da Lua",
                        "Quebra!", "Quebra!", "Quebra!",
                        "Vendaval Arcano", "Vendaval Arcano", "Vendaval Arcano",
                        "Garra espectral", "Garra espectral", "Garra espectral",
                        "Ração de emergencia", "Ração de emergencia", "Ração de emergencia",
                        "Mirar na cabeça", "Mirar na cabeça", "Mirar na cabeça"
                )
        ));
    }

    /**
     * Sorteia aleatoriamente um dos baralhos da máquina para enfrentar o jogador
     */
    public Player createRandomBotPlayer() {
        BotDeckTemplate chosenTemplate = botDeckPool.get(random.nextInt(botDeckPool.size()));
        Deck botDeck = buildDeckForPlayer(PLAYER_2_UUID, chosenTemplate.heroNames, chosenTemplate.cardNames);
        botDeck.setUuid(UUID.randomUUID());
        botDeck.setDeckName(chosenTemplate.deckName);

        Player bot = new Player(PLAYER_2_UUID, chosenTemplate.botDisplayName, botDeck);
        bot.setDecks(List.of(botDeck));
        System.out.println("🎲 [SORTEIO BOT] Máquina jogará com: " + chosenTemplate.botDisplayName
                + " (" + chosenTemplate.deckName + ")");
        return bot;
    }

    /**
     * Prepara o Jogador Humano com o baralho que ele escolheu (ou montou no Deckbuilder),
     * injetando os efeitos via CardFactory e preparando as zonas BENCH e DECK.
     */
    public Player prepareHumanPlayerForMatch(Player storedPlayer, UUID chosenDeckId) {
        if (storedPlayer == null) {
            return createPresetPlayer1();
        }

        Deck sourceDeck = null;

        // 1. Procura pelo UUID do baralho enviado na requisição
        if (chosenDeckId != null && storedPlayer.getDecks() != null) {
            sourceDeck = storedPlayer.getDecks().stream()
                    .filter(d -> chosenDeckId.equals(d.getUuid()))
                    .findFirst()
                    .orElse(null);
        }

        // 2. Se não enviou UUID específico, usa o currentDeck do jogador
        if (sourceDeck == null && storedPlayer.getCurrentDeck() != null) {
            sourceDeck = storedPlayer.getCurrentDeck();
        }

        // 3. Se ainda não tiver currentDeck, procura o primeiro baralho válido (3 heróis e 30 cartas) da lista
        if (sourceDeck == null && storedPlayer.getDecks() != null) {
            sourceDeck = storedPlayer.getDecks().stream()
                    .filter(d -> d.getHeroList() != null && d.getHeroList().size() == 3
                            && d.getCardList() != null && d.getCardList().size() == 30)
                    .findFirst()
                    .orElse(null);
        }

        // 4. Caso o jogador ainda não tenha montado nenhum baralho válido de 30 cartas, usa o preset padrão com o nome/UUID dele
        if (sourceDeck == null || sourceDeck.getHeroList() == null || sourceDeck.getHeroList().size() != 3
                || sourceDeck.getCardList() == null || sourceDeck.getCardList().size() != 30) {
            BotDeckTemplate fallback = botDeckPool.get(0);
            Deck fallbackDeck = buildDeckForPlayer(storedPlayer.getUuid(), fallback.heroNames, fallback.cardNames);
            fallbackDeck.setDeckName("Deck Inicial Cristal");
            return new Player(storedPlayer.getUuid(), storedPlayer.getName(), fallbackDeck);
        }

        // Hidrata o baralho escolhido pelo jogador com instâncias limpas e CardFactory.buildCardLogic
        Deck matchDeck = new Deck();
        matchDeck.setUuid(sourceDeck.getUuid());
        matchDeck.setDeckName(sourceDeck.getDeckName());

        List<HeroCard> hydratedHeroes = new ArrayList<>();
        for (Card hObj : sourceDeck.getHeroList()) {
            HeroCard template = catalogHeroes.stream()
                    .filter(ch -> ch.getCardName().equalsIgnoreCase(hObj.getCardName()))
                    .findFirst()
                    .orElse((HeroCard) hObj);
            HeroCard instance = cloneHero(template, storedPlayer.getUuid());
            CardFactory.buildCardLogic(instance);
            hydratedHeroes.add(instance);
        }

        List<Card> hydratedCards = new ArrayList<>();
        for (Card cObj : sourceDeck.getCardList()) {
            Card template = catalogCards.stream()
                    .filter(cc -> cc.getCardName().equalsIgnoreCase(cObj.getCardName()))
                    .findFirst()
                    .orElse(cObj);
            Card instance = cloneCard(template, storedPlayer.getUuid());
            CardFactory.buildCardLogic(instance);
            hydratedCards.add(instance);
        }

        matchDeck.setHeroList(hydratedHeroes);
        matchDeck.setCardList(hydratedCards);

        return new Player(storedPlayer.getUuid(), storedPlayer.getName(), matchDeck);
    }

    public Player createPresetPlayer1() {
        BotDeckTemplate t = botDeckPool.get(0);
        Deck p1Deck = buildDeckForPlayer(PLAYER_1_UUID, t.heroNames, t.cardNames);
        p1Deck.setDeckName(t.deckName);
        Player player1 = new Player(PLAYER_1_UUID, "Jogador 1 (Cristal)", p1Deck);
        player1.setDecks(List.of(p1Deck));
        return player1;
    }

    public Player createPresetPlayer2() {
        return createRandomBotPlayer();
    }

    private Deck buildDeckForPlayer(UUID ownerId, List<String> heroNames, List<String> cardNames) {
        Deck deck = new Deck();
        List<HeroCard> heroList = new ArrayList<>();
        List<Card> cardList = new ArrayList<>();

        for (String hName : heroNames) {
            HeroCard template = catalogHeroes.stream()
                    .filter(h -> h.getCardName().equalsIgnoreCase(hName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Herói não encontrado no JSON: " + hName));

            HeroCard instance = cloneHero(template, ownerId);
            CardFactory.buildCardLogic(instance);
            heroList.add(instance);
        }

        for (String cName : cardNames) {
            Card template = catalogCards.stream()
                    .filter(c -> c.getCardName().equalsIgnoreCase(cName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Carta não encontrada no JSON: " + cName));

            Card instance = cloneCard(template, ownerId);
            CardFactory.buildCardLogic(instance);
            cardList.add(instance);
        }

        deck.setHeroList(heroList);
        deck.setCardList(cardList);
        return deck;
    }

    private HeroCard cloneHero(HeroCard src, UUID ownerId) {
        HeroCard copy = new HeroCard();
        copy.setUuid(UUID.randomUUID());
        copy.setOwnerId(ownerId);
        copy.setCardName(src.getCardName());
        copy.setCardType(src.getCardType());
        copy.setColor(src.getColor());
        copy.setRarity(src.getRarity());
        copy.setCardSet(src.getCardSet());
        copy.setDescricao(src.getDescricao());
        copy.setEspirito(src.getEspirito());
        copy.setAtaque(src.getAtaque());
        copy.setDefesa(src.getDefesa());
        copy.setVidaMaxima(src.getVidaMaxima());
        copy.setVidaAtual(src.getVidaMaxima());
        copy.setMaxWeaponSlots(src.getMaxWeaponSlots());
        copy.setReducaoDano(src.getReducaoDano());
        copy.setDanoBonus(src.getDanoBonus());
        copy.setVulnerabilidade(src.getVulnerabilidade());
        copy.setRacas(src.getRacas() != null ? new ArrayList<>(src.getRacas()) : new ArrayList<>());
        copy.setAfinidades(src.getAfinidades() != null ? new ArrayList<>(src.getAfinidades()) : new ArrayList<>());
        copy.setOrdem(src.getOrdem() != null ? new ArrayList<>(src.getOrdem()) : new ArrayList<>());
        copy.setClasse(src.getClasse() != null ? new ArrayList<>(src.getClasse()) : new ArrayList<>());
        copy.setEstaVivo(true);
        copy.setEstaAtivo(true);
        copy.setZoneId(ZoneId.BENCH);
        return copy;
    }

    private Card cloneCard(Card src, UUID ownerId) {
        Card copy = new Card();
        copy.setUuid(UUID.randomUUID());
        copy.setOwnerId(ownerId);
        copy.setCardName(src.getCardName());
        copy.setCardType(src.getCardType());
        copy.setColor(src.getColor());
        copy.setRarity(src.getRarity());
        copy.setCardSet(src.getCardSet());
        copy.setUnica(src.isUnica());
        copy.setItemType(src.getItemType());
        copy.setEmpunhadura(src.getEmpunhadura());
        copy.setDescricao(src.getDescricao());
        copy.setRacas(src.getRacas() != null ? new ArrayList<>(src.getRacas()) : new ArrayList<>());
        copy.setAfinidades(src.getAfinidades() != null ? new ArrayList<>(src.getAfinidades()) : new ArrayList<>());
        copy.setOrdem(src.getOrdem() != null ? new ArrayList<>(src.getOrdem()) : new ArrayList<>());
        copy.setClasse(src.getClasse() != null ? new ArrayList<>(src.getClasse()) : new ArrayList<>());
        copy.setZoneId(ZoneId.DECK);
        return copy;
    }
}
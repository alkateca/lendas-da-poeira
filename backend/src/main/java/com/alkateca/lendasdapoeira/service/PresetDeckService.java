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
     * Inicializa os 10 baralhos oficiais (2 para cada uma das 5 cores: Azul, Vermelho, Preto, Verde e Amarelo)
     * seguindo fielmente todas as regras de construção de baralhos do jogador (3 Heróis da mesma cor,
     * 30 cartas compatíveis com os Heróis, limite de 3 cópias para cartas normais e 1 para únicas).
     */
    private void initBotDeckPool() {
        botDeckPool.clear();

        // 1. AZUL A: Cavaleiros de Cristal (30 cartas)
        botDeckPool.add(new BotDeckTemplate(
                "🤖 Bot - Cavaleiros de Cristal",
                "Cavaleiros de Cristal",
                List.of("Enamor,O Grande Cavaleiro", "Cavaleiro Onear", "Ageus, Constructo Cristalino"),
                List.of(
                        "Quimera", "Quimera", "Quimera",
                        "Dragast", "Dragast", "Dragast",
                        "Fragmento Afiado", "Fragmento Afiado", "Fragmento Afiado",
                        "Baluarte de Espelhos do Dragão", "Baluarte de Espelhos do Dragão", "Baluarte de Espelhos do Dragão",
                        "Placas de Cristal Draconiano", "Placas de Cristal Draconiano", "Placas de Cristal Draconiano",
                        "Dragão de Cristal", "Dragão de Cristal", "Dragão de Cristal",
                        "Contra Ataque", "Contra Ataque", "Contra Ataque",
                        "Voto dos Cavaleiros Caídos", "Voto dos Cavaleiros Caídos", "Voto dos Cavaleiros Caídos",
                        "Golpes Pesados", "Golpes Pesados", "Golpes Pesados",
                        "Determinação Cristalina", "Determinação Cristalina", "Determinação Cristalina"
                )
        ));

        // 2. AZUL B: Santas de Cristal (30 cartas)
        botDeckPool.add(new BotDeckTemplate(
                "🤖 Bot - Santas de Cristal",
                "Santas das Lâminas",
                List.of("Isenora, Santa das Laminas", "Moyra, Aprendiz da Santa", "Naelis, Grande Artesã de Cristais"),
                List.of(
                        "Coração de Cristal Puro", "Coração de Cristal Puro", "Coração de Cristal Puro",
                        "Broche de Cristal", "Broche de Cristal", "Broche de Cristal",
                        "Lamina de Cristal", "Lamina de Cristal", "Lamina de Cristal",
                        "Rapieira de Cristal", "Rapieira de Cristal", "Rapieira de Cristal",
                        "Dança das Mil Lâminas", "Dança das Mil Lâminas", "Dança das Mil Lâminas",
                        "Massacre Cristalino", "Massacre Cristalino", "Massacre Cristalino",
                        "Ataque Mágico", "Ataque Mágico", "Ataque Mágico",
                        "Ponto Final", "Ponto Final", "Ponto Final",
                        "Auto Defesa Magica", "Auto Defesa Magica", "Auto Defesa Magica",
                        "Impulso de Poder", "Impulso de Poder", "Impulso de Poder"
                )
        ));

        // 3. VERMELHO A: Caçadores das Cinzas (30 cartas)
        botDeckPool.add(new BotDeckTemplate(
                "🤖 Bot - Caçadores das Cinzas",
                "Caçadores das Cinzas",
                List.of("Xia, Caçador de Bestas", "Asahum, Caçadora de Bestas", "Constructo de Combate, Arsenal Vivo"),
                List.of(
                        "Sica", "Sica", "Sica",
                        "Arpão terrestre", "Arpão terrestre", "Arpão terrestre",
                        "Sino do Caçador", "Sino do Caçador", "Sino do Caçador",
                        "Bomba de fumaça", "Bomba de fumaça", "Bomba de fumaça",
                        "Tesouro Cristalino dos Caçadores", "Tesouro Cristalino dos Caçadores", "Tesouro Cristalino dos Caçadores",
                        "Rede de Contenção de Feras", "Rede de Contenção de Feras", "Rede de Contenção de Feras",
                        "Bolsa de Armadilhas do Mestre Caçador", "Bolsa de Armadilhas do Mestre Caçador", "Bolsa de Armadilhas do Mestre Caçador",
                        "Afie suas Lâminas", "Afie suas Lâminas", "Afie suas Lâminas",
                        "Estilo Secreto do Caçador", "Estilo Secreto do Caçador", "Estilo Secreto do Caçador",
                        "Corte seus Pescoços", "Corte seus Pescoços", "Corte seus Pescoços"
                )
        ));

        // 4. VERMELHO B: Círculo das Chamas (30 cartas)
        botDeckPool.add(new BotDeckTemplate(
                "🤖 Bot - Círculo das Chamas",
                "Círculo das Chamas",
                List.of("Alix, Dragão Flamejante", "Xamã das cinzas", "Constructo das Chamas, Sif"),
                List.of(
                        "Escamas do Dragão Eterno", "Escamas do Dragão Eterno", "Escamas do Dragão Eterno",
                        "Peitoral de brasas", "Peitoral de brasas", "Peitoral de brasas",
                        "Escudo ardente", "Escudo ardente", "Escudo ardente",
                        "Bastão do Monge Incandescente", "Bastão do Monge Incandescente", "Bastão do Monge Incandescente",
                        "Incensário Purificador", "Incensário Purificador", "Incensário Purificador",
                        "Muralha de Fogo", "Muralha de Fogo", "Muralha de Fogo",
                        "Cauterizar", "Cauterizar", "Cauterizar",
                        "Fogo Purificador", "Fogo Purificador", "Fogo Purificador",
                        "Manto das Brasas Vivas", "Manto das Brasas Vivas", "Manto das Brasas Vivas",
                        "Meditação das Cinzas", "Meditação das Cinzas", "Meditação das Cinzas"
                )
        ));

        // 5. PRETO A: Legião Necromante (30 cartas)
        botDeckPool.add(new BotDeckTemplate(
                "🤖 Bot - Legião Necromante",
                "Legião Necromante",
                List.of("Rainha Goblin", "Necromante das Areias", "Quimera Carniceira"),
                List.of(
                        "Filactéria das Areias Negras", "Filactéria das Areias Negras", "Filactéria das Areias Negras",
                        "Homunculo Carniceiro", "Homunculo Carniceiro", "Homunculo Carniceiro",
                        "Amuleto de Ossos da Fábrica", "Amuleto de Ossos da Fábrica", "Amuleto de Ossos da Fábrica",
                        "Quimera Negra", "Quimera Negra", "Quimera Negra",
                        "Manoplas do Necromante", "Manoplas do Necromante", "Manoplas do Necromante",
                        "Atmosfera Pesada", "Atmosfera Pesada", "Atmosfera Pesada",
                        "Peste das Areias Negras", "Peste das Areias Negras", "Peste das Areias Negras",
                        "Ritos Fúnebres", "Ritos Fúnebres", "Ritos Fúnebres",
                        "Exumação", "Exumação", "Exumação",
                        "Pilhagem de Cadáveres", "Pilhagem de Cadáveres", "Pilhagem de Cadáveres"
                )
        ));

        // 6. PRETO B: Ordem dos Carrascos (30 cartas)
        botDeckPool.add(new BotDeckTemplate(
                "🤖 Bot - Ordem dos Carrascos",
                "Ordem dos Carrascos",
                List.of("Chefe Carrasco", "Izeref, Executor da Ordem Necrótica", "Constructo Carniceiro"),
                List.of(
                        "Espada do Carrasco", "Espada do Carrasco", "Espada do Carrasco",
                        "Cutelo do Suplício", "Cutelo do Suplício", "Cutelo do Suplício",
                        "Manto de Ferro do Algoz", "Manto de Ferro do Algoz", "Manto de Ferro do Algoz",
                        "Correntes do Cadafalso", "Correntes do Cadafalso", "Correntes do Cadafalso",
                        "Seguindo Ordens", "Seguindo Ordens", "Seguindo Ordens",
                        "Grilhões Pesados", "Grilhões Pesados", "Grilhões Pesados",
                        "Cheiro de Sangue", "Cheiro de Sangue", "Cheiro de Sangue",
                        "Arrastar para o Cadafalso", "Arrastar para o Cadafalso", "Arrastar para o Cadafalso",
                        "Colheita dos Condenados", "Colheita dos Condenados", "Colheita dos Condenados",
                        "Sentença da Guilhotina", "Sentença da Guilhotina", "Sentença da Guilhotina"
                )
        ));

        // 7. VERDE A: Lobos da Tundra (30 cartas)
        botDeckPool.add(new BotDeckTemplate(
                "🤖 Bot - Lobos da Tundra",
                "Lobos da Tundra",
                List.of("Fenrir, o Grande Devorador", "Skoll, Espreitador Solar", "Hati, Constructo Gélido"),
                List.of(
                        "Baluarte da Tundra Ancestral", "Baluarte da Tundra Ancestral", "Baluarte da Tundra Ancestral",
                        "Couraça da Tundra Profunda", "Couraça da Tundra Profunda", "Couraça da Tundra Profunda",
                        "Machado de Gelo", "Machado de Gelo", "Machado de Gelo",
                        "Grande Machado Sombrio", "Grande Machado Sombrio", "Grande Machado Sombrio",
                        "Garras Congeladas", "Garras Congeladas", "Garras Congeladas",
                        "Barreira de Gelo", "Barreira de Gelo", "Barreira de Gelo",
                        "Mordida Glacial", "Mordida Glacial", "Mordida Glacial",
                        "Uivo da Matilha Gélida", "Uivo da Matilha Gélida", "Uivo da Matilha Gélida",
                        "Luz da Lua", "Luz da Lua", "Luz da Lua",
                        "Chamado do Domador", "Chamado do Domador", "Chamado do Domador"
                )
        ));

        // 8. VERDE B: Sociedade Goblin (30 cartas)
        botDeckPool.add(new BotDeckTemplate(
                "🤖 Bot - Sociedade Goblin",
                "Sociedade Goblin",
                List.of("Rei Goblin", "Esquadrão Goblin", "Traidor Goblin"),
                List.of(
                        "Núcleo de Invenções do Rei Goblin", "Núcleo de Invenções do Rei Goblin", "Núcleo de Invenções do Rei Goblin",
                        "Bateria de Cristal de Poeira", "Bateria de Cristal de Poeira", "Bateria de Cristal de Poeira",
                        "Chave Mecânica de Precisão", "Chave Mecânica de Precisão", "Chave Mecânica de Precisão",
                        "Engrenagem de Ressonância", "Engrenagem de Ressonância", "Engrenagem de Ressonância",
                        "Descarga de Poeira Elétrica", "Descarga de Poeira Elétrica", "Descarga de Poeira Elétrica",
                        "Circuito de Sobrecarga", "Circuito de Sobrecarga", "Circuito de Sobrecarga",
                        "Campo de Força de Cristal", "Campo de Força de Cristal", "Campo de Força de Cristal",
                        "Engenharia Rápida", "Engenharia Rápida", "Engenharia Rápida",
                        "Recalibração de Energia", "Recalibração de Energia", "Recalibração de Energia",
                        "Conferência dos Sábios Goblins", "Conferência dos Sábios Goblins", "Conferência dos Sábios Goblins"
                )
        ));

        // 9. AMARELO A: Inquisição do Trono (30 cartas)
        botDeckPool.add(new BotDeckTemplate(
                "🤖 Bot - Inquisição do Trono",
                "Inquisição do Trono",
                List.of("Grande Inquisidor Vael, Voz do Trono", "Justiçar Elyon, Balança Imutável", "Constructo Arquivista"),
                List.of(
                        "Cetro do Julgamento Perpétuo", "Cetro do Julgamento Perpétuo", "Cetro do Julgamento Perpétuo",
                        "Selo da Confissão", "Selo da Confissão", "Selo da Confissão",
                        "Tomo das Sentenças Antigas", "Tomo das Sentenças Antigas", "Tomo das Sentenças Antigas",
                        "Édito contra a Ganância", "Édito contra a Ganância", "Édito contra a Ganância",
                        "Retribuição Dogmática", "Retribuição Dogmática", "Retribuição Dogmática",
                        "Interdito Sagrado", "Interdito Sagrado", "Interdito Sagrado",
                        "Decreto de Excomunhão", "Decreto de Excomunhão", "Decreto de Excomunhão",
                        "Liturgia da Balança", "Liturgia da Balança", "Liturgia da Balança",
                        "Bênção do Rei Imóvel", "Bênção do Rei Imóvel", "Bênção do Rei Imóvel",
                        "Veredito Inquisitorial", "Veredito Inquisitorial", "Veredito Inquisitorial"
                )
        ));

        // 10. AMARELO B: Guarda Real do Trono (30 cartas)
        botDeckPool.add(new BotDeckTemplate(
                "🤖 Bot - Guarda Real do Trono",
                "Guarda Real do Trono",
                List.of("Oráculo Real", "Portador da Chama", "Constructo Fantasmagórico Guardião Perpétuo"),
                List.of(
                        "Artefato Cortante", "Artefato Cortante", "Artefato Cortante",
                        "Alabarda Cerimonial do Trono", "Alabarda Cerimonial do Trono", "Alabarda Cerimonial do Trono",
                        "Glaive da Guarda Eterna", "Glaive da Guarda Eterna", "Glaive da Guarda Eterna",
                        "Manto Cerimonial do Trono", "Manto Cerimonial do Trono", "Manto Cerimonial do Trono",
                        "Escudo da Devoção Cega", "Escudo da Devoção Cega", "Escudo da Devoção Cega",
                        "Fortaleça sua Fé", "Fortaleça sua Fé", "Fortaleça sua Fé",
                        "Saque Rápido", "Saque Rápido", "Saque Rápido",
                        "Ordem Divina", "Ordem Divina", "Ordem Divina",
                        "Disciplina", "Disciplina", "Disciplina",
                        "Fervor Penitencial", "Fervor Penitencial", "Fervor Penitencial"
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

    public Deck createDefaultDeck() {
        BotDeckTemplate t = botDeckPool.isEmpty() ? null : botDeckPool.get(0);
        if (t != null) {
            Deck p1Deck = buildDeckForPlayer(PLAYER_1_UUID, t.heroNames, t.cardNames);
            p1Deck.setDeckName(t.deckName);
            return p1Deck;
        }
        return new Deck();
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

        Map<String, Integer> copyCounts = new HashMap<>();
        DeckValidatorService validator = new DeckValidatorService();

        for (String cName : cardNames) {
            Card template = catalogCards.stream()
                    .filter(c -> c.getCardName().equalsIgnoreCase(cName))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Carta não encontrada no JSON: " + cName));

            int currentCopies = copyCounts.getOrDefault(template.getCardName(), 0);
            int maxAllowed = template.isUnica() ? 1 : 3;
            if (currentCopies >= maxAllowed) {
                continue;
            }
            copyCounts.put(template.getCardName(), currentCopies + 1);

            Card instance = cloneCard(template, ownerId);
            CardFactory.buildCardLogic(instance);
            cardList.add(instance);
        }

        // Caso alguma carta seja marcada como única (máx. 1 cópia), completa até 30 com cartas compatíveis
        if (cardList.size() < 30) {
            for (Card candidate : catalogCards) {
                if (cardList.size() >= 30) break;
                if (candidate.isLiberationCard() || !validator.isCardCompatibleWithHeroes(candidate, heroList)) {
                    continue;
                }
                int maxAllowed = candidate.isUnica() ? 1 : 3;
                while (cardList.size() < 30 && copyCounts.getOrDefault(candidate.getCardName(), 0) < maxAllowed) {
                    copyCounts.put(candidate.getCardName(), copyCounts.getOrDefault(candidate.getCardName(), 0) + 1);
                    Card instance = cloneCard(candidate, ownerId);
                    CardFactory.buildCardLogic(instance);
                    cardList.add(instance);
                }
            }
        }

        deck.setHeroList(heroList);
        deck.setCardList(cardList);
        return deck;
    }

    public List<Deck> getAllBotDecks(UUID ownerId) {
        UUID targetOwner = ownerId != null ? ownerId : PLAYER_2_UUID;
        List<Deck> decks = new ArrayList<>();
        for (BotDeckTemplate t : botDeckPool) {
            Deck d = buildDeckForPlayer(targetOwner, t.heroNames, t.cardNames);
            d.setUuid(UUID.randomUUID());
            d.setDeckName(t.deckName);
            decks.add(d);
        }
        return decks;
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

    public Deck getRandomPresetDeck() {
        if (botDeckPool.isEmpty()) {
            return new Deck();
        }
        BotDeckTemplate chosen = botDeckPool.get(random.nextInt(botDeckPool.size()));
        Deck deck = buildDeckForPlayer(PLAYER_1_UUID, chosen.heroNames, chosen.cardNames);
        deck.setUuid(UUID.randomUUID());
        deck.setDeckName(chosen.deckName);
        return deck;
    }

    public Deck getPresetDeckById(String presetDeckId, UUID ownerId) {
        if (botDeckPool.isEmpty()) {
            return new Deck();
        }
        int index = 0;
        if (presetDeckId != null && presetDeckId.startsWith("PRESET_")) {
            try {
                index = Integer.parseInt(presetDeckId.replace("PRESET_", ""));
            } catch (NumberFormatException ignored) {
                index = 0;
            }
        }
        BotDeckTemplate chosen = botDeckPool.get(Math.max(0, Math.min(index, botDeckPool.size() - 1)));
        UUID targetOwner = ownerId != null ? ownerId : PLAYER_1_UUID;
        Deck deck = buildDeckForPlayer(targetOwner, chosen.heroNames, chosen.cardNames);
        deck.setUuid(UUID.randomUUID());
        deck.setDeckName(chosen.deckName);
        return deck;
    }
}
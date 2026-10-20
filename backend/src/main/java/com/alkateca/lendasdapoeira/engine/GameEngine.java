package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;
import lombok.*;

import java.util.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class GameEngine {

    private GameState gameState;
    private Player player1;
    private Player player2;
    private List<Card> cardsOnBoard;
    private ResolutionQueue resolutionQueue = new ResolutionQueue();
    private UUID activePlayerId;
    private boolean isPlayer1Ready = false;
    private boolean isPlayer2Ready = false;

    private java.util.function.Consumer<UUID> pendingChoiceCallback;

    public boolean isWaitingForPlayerChoice() {
        return this.gameState.getPendingChoice() != null && this.pendingChoiceCallback != null;
    }

    private TurnPhase turnPhase;

    // Histórico de snapshots para a timeline do Front-End
    private List<MatchSnapshotDTO> snapshotHistory = new ArrayList<>();

    // Ações de limpeza de buffs/debuffs temporários que expiram ao final do combate
    private List<Runnable> combatEndCleanupActions = new ArrayList<>();

    public void registerCombatEndCleanup(Runnable cleanupAction) {
        if (cleanupAction != null) {
            this.combatEndCleanupActions.add(cleanupAction);
        }
    }

    public void runCombatEndCleanup() {
        List<Runnable> actions = new ArrayList<>(this.combatEndCleanupActions);
        this.combatEndCleanupActions.clear();
        for (Runnable action : actions) {
            action.run();
        }
    }

    public GameEngine(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.cardsOnBoard = new ArrayList<>();
        // Inicialização correta do GameState para evitar NPE
        this.gameState = new GameState();
        this.gameState.setPlayer1(player1);
        this.gameState.setPlayer2(player2);
    }

    private void drawInitialHand(Player player) {
        int drawn = 0;

        while (drawn < 5) {
            Card cardToDraw = player.getCurrentDeck().getCardList().stream()
                    .filter(c -> c.getZoneId() == ZoneId.DECK)
                    .findFirst()
                    .orElse(null);

            if (cardToDraw != null) {
                cardToDraw.setZoneId(ZoneId.HAND);
                drawn++;
            } else {
                return;
            }
        }

        System.out.println("Jogador " + player.getName() + " comprou " + drawn + " carta(s).");
    }

    public void startGame() {
        System.out.println("--- INICIANDO A PARTIDA ---");

        Random random = new Random();
        boolean player1Starts = random.nextBoolean();

        if (player1Starts) {
            this.activePlayerId = player1.getUuid();
            if (this.gameState != null) this.gameState.setActiveTurnPlayerId(player1.getUuid());
            System.out.println("=> Sorteio: " + player1.getName() + " ganhou no cara ou coroa e começa jogando!");
        } else {
            this.activePlayerId = player2.getUuid();
            if (this.gameState != null) this.gameState.setActiveTurnPlayerId(player2.getUuid());
            System.out.println("=> Sorteio: " + player2.getName() + " ganhou no cara ou coroa e começa jogando!");
        }

        player1.getCurrentDeck().shuffleDeck();
        player2.getCurrentDeck().shuffleDeck();

        drawInitialHand(player1);
        drawInitialHand(player2);

        for(Card heroCard : player1.getCurrentDeck().getHeroList()){
            heroCard.setZoneId(ZoneId.BENCH);
        }

        for(Card heroCard : player2.getCurrentDeck().getHeroList()){
            heroCard.setZoneId(ZoneId.BENCH);
        }

        this.turnPhase = TurnPhase.GAME_START;
        if (this.gameState != null) {
            this.gameState.setTurnPhase(TurnPhase.GAME_START);
        }
        for (Card heroCard : player1.getCurrentDeck().getHeroList()) {
            heroCard.onPhaseChange(TurnPhase.GAME_START, resolutionQueue);
        }
        for (Card heroCard : player2.getCurrentDeck().getHeroList()) {
            heroCard.onPhaseChange(TurnPhase.GAME_START, resolutionQueue);
        }
        resolutionQueue.resolveAll(this);

        changePhase(TurnPhase.CHOICE);
    }

    public void choice() {
        changePhase(TurnPhase.PREPARATION);
        triggerActiveHeroesEffects(TurnPhase.PREPARATION);
        resolutionQueue.resolveAll(this);
    }

    public void preparation() {
        changePhase(TurnPhase.COMBAT_START);
    }

    public void combatStart(){
        triggerActiveHeroesEffects(TurnPhase.COMBAT_START);
        resolutionQueue.resolveAll(this);
        changePhase(TurnPhase.RESOLUTION);
    }

    public void resolution() {
        // 1. Muda para RESOLUTION primeiro para tirar a foto revelando as cartas nos 4 slots centrais!
        changePhase(TurnPhase.RESOLUTION);

        enqueueInitialResolutionCards();

        // 2. Resolve cada efeito gerando 1 foto (MatchSnapshotDTO) por efeito!
        resolutionQueue.resolveAll(this);

        // 3. Move cada carta resolvida para ATTACHED (se for ITEM), EXILE (se tiver Exilar.) ou DISCARD
        moveResolvedCardsToDestination(gameState.getPlayer1PlayedCards());
        moveResolvedCardsToDestination(gameState.getPlayer2PlayedCards());

        gameState.getPlayer1PlayedCards().clear();
        gameState.getPlayer2PlayedCards().clear();

        changePhase(TurnPhase.COMBAT);
    }


    public void discard() {
        refillHandToFive(player1);
        refillHandToFive(player2);

        if (gameState.getActiveTurnPlayerId().equals(gameState.getPlayer1().getUuid())) {
            gameState.setActiveTurnPlayerId(gameState.getPlayer2().getUuid());
        } else {
            gameState.setActiveTurnPlayerId(gameState.getPlayer1().getUuid());
        }

        changePhase(TurnPhase.CHOICE);
    }

    public void gameOver() {
        System.out.println("--- PARTIDA ENCERRADA ---");
    }


    private void changePhase(TurnPhase newPhase) {
        this.turnPhase = newPhase;
        if (this.gameState != null) {
            this.gameState.setTurnPhase(newPhase);
        }
        // Captura snapshot da mudança de fase
        captureSnapshot(null, null, "PHASE_CHANGE");
    }

    private void checkBoardRefresh(Player player) {
        // Correção: Verifica se existem heróis VIVOS que precisam ter o estado de ação refeito
        boolean hasActiveHeroes = player.getCurrentDeck().getHeroList().stream()
                .filter(c -> c instanceof HeroCard && c.getZoneId() == ZoneId.BENCH && ((HeroCard) c).getEstaVivo())
                .anyMatch(c -> ((HeroCard) c).getEstaAtivo());

        if (!hasActiveHeroes) {
            player.getCurrentDeck().getHeroList().stream()
                    .filter(c -> c instanceof HeroCard && c.getZoneId() == ZoneId.BENCH && ((HeroCard) c).getEstaVivo())
                    .forEach(c -> ((HeroCard) c).setEstaAtivo(true));
        }
    }

    // 1. SUBSTITUIR moveResolvedCardsToDestination no GameEngine.java:
    private void moveResolvedCardsToDestination(List<Card> playedCards) {
        UUID activeP1HeroId = gameState.getActiveHeroPlayer1Id();
        UUID activeP2HeroId = gameState.getActiveHeroPlayer2Id();

        for (Card card : playedCards) {
            // Se um efeito durante a RESOLUTION já anexou a carta a um Herói (qualquer CardType!)
            if (card.getAttachedToCardId() != null) {
                card.setZoneId(ZoneId.ATTACHED);
            }
            // Caso seja um ITEM sem EquipBuffEffect (ex: só tem efeito de Final de Combate como Dragão de Cristal)
            else if (card.getCardType() == com.alkateca.lendasdapoeira.enums.CardType.ITEM) {
                boolean isP1Owner = card.getOwnerId().equals(player1.getUuid());
                UUID defaultHeroId = isP1Owner ? activeP1HeroId : activeP2HeroId;
                if (defaultHeroId != null) {
                    card.setAttachedToCardId(defaultHeroId);
                    card.setZoneId(ZoneId.ATTACHED);
                } else {
                    card.setZoneId(ZoneId.DISCARD);
                }
            } else if (card.getDescricao() != null && card.getDescricao().contains("Exilar.")) {
                card.setZoneId(ZoneId.EXILE);
            } else {
                card.setZoneId(ZoneId.DISCARD);
            }
        }
    }

    // 2. SUBSTITUIR triggerActiveHeroesEffects no GameEngine.java:
    // Garante que apenas os itens/cartas anexados aos Heróis ATIVOS na batalha disparem COMBAT_START e COMBAT_END!
    private void triggerActiveHeroesEffects(TurnPhase phase) {
        HeroCard p1Hero = getActiveHero(player1, gameState.getActiveHeroPlayer1Id());
        HeroCard p2Hero = getActiveHero(player2, gameState.getActiveHeroPlayer2Id());

        if (p1Hero != null) p1Hero.onPhaseChange(phase, resolutionQueue);
        if (p2Hero != null) p2Hero.onPhaseChange(phase, resolutionQueue);

        if (phase == TurnPhase.COMBAT_END) {
            List<Card> allHeroes = new ArrayList<>();
            allHeroes.addAll(player1.getCurrentDeck().getHeroList());
            allHeroes.addAll(player2.getCurrentDeck().getHeroList());

            for (Card hCard : allHeroes) {
                if (hCard instanceof HeroCard && hCard.getZoneId() == ZoneId.BENCH && Boolean.TRUE.equals(((HeroCard) hCard).getEstaVivo())) {
                    if (hCard.getEffects() != null) {
                        hCard.getEffects().stream()
                                .filter(eff -> eff instanceof com.alkateca.lendasdapoeira.effects.endOfCombat.EndOfCombatDeadOrderBuffEffect
                                        && eff.predicate(hCard, phase))
                                .forEach(eff -> resolutionQueue.enqueue(eff, hCard));
                    }
                }
            }
        }

        List<Card> allCards = new ArrayList<>();
        allCards.addAll(player1.getCurrentDeck().getCardList());
        allCards.addAll(player2.getCurrentDeck().getCardList());

        UUID activeP1HeroId = gameState.getActiveHeroPlayer1Id();
        UUID activeP2HeroId = gameState.getActiveHeroPlayer2Id();

        allCards.stream()
                .filter(c -> c.getZoneId() == ZoneId.ATTACHED
                        && c.getAttachedToCardId() != null
                        && (c.getAttachedToCardId().equals(activeP1HeroId) || c.getAttachedToCardId().equals(activeP2HeroId))
                        && c.getEffects() != null)
                .forEach(attachedCard -> attachedCard.getEffects().stream()
                        .filter(eff -> eff.predicate(attachedCard, phase))
                        .forEach(eff -> resolutionQueue.enqueue(eff, attachedCard)));
    }

    // 3. ADICIONAR estes métodos públicos no GameEngine.java para gerenciar Desanexar e Duração:

    /**
     * Desanexa uma carta de um Herói, revertendo todos os seus efeitos (onDetach) e movendo-a para a zona de destino.
     */
    public void detachCard(Card card, ZoneId destinationZone) {
        if (card == null) return;

        if (card.getZoneId() == ZoneId.ATTACHED && card.getAttachedToCardId() != null) {
            HeroCard hostHero = findHeroById(card.getAttachedToCardId());
            if (hostHero != null && card.getEffects() != null) {
                for (com.alkateca.lendasdapoeira.effects.Effect effect : card.getEffects()) {
                    effect.onDetach(this, card, hostHero);
                }
            }
        }

        card.setAttachedToCardId(null);
        card.setTurnsRemaining(null);

        if (destinationZone == ZoneId.DISCARD && card.getDescricao() != null && card.getDescricao().contains("Exilar.")) {
            card.setZoneId(ZoneId.EXILE);
        } else {
            card.setZoneId(destinationZone);
        }
    }

    /**
     * Decrementa a duração (turnsRemaining) das cartas anexadas no final do combate e desanexa as que chegarem a 0.
     */
    private void processAttachedCardsDuration() {
        List<Card> allCards = new ArrayList<>();
        allCards.addAll(player1.getCurrentDeck().getCardList());
        allCards.addAll(player2.getCurrentDeck().getCardList());

        for (Card card : allCards) {
            if (card.getZoneId() == ZoneId.ATTACHED && card.getTurnsRemaining() != null) {
                int remaining = card.getTurnsRemaining() - 1;
                card.setTurnsRemaining(remaining);
                if (remaining <= 0) {
                    detachCard(card, ZoneId.DISCARD);
                }
            }
        }
    }

    public HeroCard findHeroById(UUID heroId) {
        if (heroId == null) return null;
        HeroCard h1 = getActiveHero(player1, heroId);
        if (h1 != null) return h1;
        return getActiveHero(player2, heroId);
    }

    public int getUsedWeaponSlotsForHero(UUID heroId) {
        if (heroId == null) return 0;
        List<Card> allCards = new ArrayList<>();
        allCards.addAll(player1.getCurrentDeck().getCardList());
        allCards.addAll(player2.getCurrentDeck().getCardList());

        return allCards.stream()
                .filter(c -> c.getZoneId() == ZoneId.ATTACHED
                        && heroId.equals(c.getAttachedToCardId())
                        && c.getItemType() == com.alkateca.lendasdapoeira.enums.ItemType.ARMA)
                .mapToInt(Card::getEmpunhadura)
                .sum();
    }

    private HeroCard getActiveHero(Player player, UUID heroId) {
        if (heroId == null) return null;
        return (HeroCard) player.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(heroId))
                .findFirst()
                .orElse(null);
    }

    public void captureSnapshot(Card sourceCard, com.alkateca.lendasdapoeira.effects.Effect effect, String eventType) {
        MatchSnapshotDTO snapshot = SnapshotFactory.createSnapshot(this, sourceCard, effect, eventType);
        this.snapshotHistory.add(snapshot);
    }

    // =========================================================================
    // MÉTODOS DE AÇÃO DOS JOGADORES E MATEMÁTICA DE COMBATE (DENTRO DO GAMEENGINE)
    // =========================================================================

    /**
     * 1. Executado quando o jogador ativo clica no Botão Principal na TurnPhase.CHOICE
     */
    public void submitHeroChoice(UUID playerId, UUID ownHeroId, UUID enemyHeroId) {
        if (this.turnPhase != TurnPhase.CHOICE) {
            throw new IllegalStateException("Ação inválida fora da fase CHOICE.");
        }
        if (!playerId.equals(this.gameState.getActiveTurnPlayerId())) {
            throw new IllegalStateException("Apenas o jogador ativo do turno pode escolher os Heróis.");
        }

        boolean isP1 = playerId.equals(player1.getUuid());
        Player activePlayer = isP1 ? player1 : player2;
        Player opponentPlayer = isP1 ? player2 : player1;

        HeroCard ownHero = getActiveHero(activePlayer, ownHeroId);
        HeroCard enemyHero = getActiveHero(opponentPlayer, enemyHeroId);

        if (ownHero == null || !Boolean.TRUE.equals(ownHero.getEstaVivo()) || !Boolean.TRUE.equals(ownHero.getEstaAtivo())) {
            throw new IllegalArgumentException("Seu Herói escolhido é inválido, está morto ou já atuou.");
        }
        if (enemyHero == null || !Boolean.TRUE.equals(enemyHero.getEstaVivo())) {
            throw new IllegalArgumentException("O Herói inimigo escolhido é inválido ou está morto.");
        }

        // Move ambos os Heróis da BENCH para a zona central BATTLE
        ownHero.setZoneId(ZoneId.BATTLE);
        enemyHero.setZoneId(ZoneId.BATTLE);

        if (isP1) {
            this.gameState.setActiveHeroPlayer1Id(ownHero.getUuid());
            this.gameState.setActiveHeroPlayer2Id(enemyHero.getUuid());
        } else {
            this.gameState.setActiveHeroPlayer2Id(ownHero.getUuid());
            this.gameState.setActiveHeroPlayer1Id(enemyHero.getUuid());
        }

        this.isPlayer1Ready = false;
        this.isPlayer2Ready = false;

        // Compra até 5 cartas no início da preparação/escolha
        drawInitialHand(player1);
        drawInitialHand(player2);

        choice(); // Avança para TurnPhase.PREPARATION e tira a foto
    }

    /**
     * 2. Executado quando cada jogador clica no Botão Principal na TurnPhase.PREPARATION
     */
    public void submitPreparationCards(UUID playerId, List<UUID> playedCardUuids) {
        if (this.turnPhase != TurnPhase.PREPARATION) {
            throw new IllegalStateException("Ação inválida fora da fase PREPARATION.");
        }

        boolean isP1 = playerId.equals(player1.getUuid());
        Player player = isP1 ? player1 : player2;
        List<Card> targetPlayedList = isP1 ? gameState.getPlayer1PlayedCards() : gameState.getPlayer2PlayedCards();

        targetPlayedList.clear();

        if (playedCardUuids != null) {
            for (UUID cardId : playedCardUuids) {
                Card cardInHand = player.getCurrentDeck().getCardList().stream()
                        .filter(c -> c.getUuid().equals(cardId) && c.getZoneId() == ZoneId.HAND)
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Carta não encontrada na mão: " + cardId));

                cardInHand.setZoneId(ZoneId.RESOLVE);
                targetPlayedList.add(cardInHand);
            }
        }

        if (isP1) {
            this.isPlayer1Ready = true;
        } else {
            this.isPlayer2Ready = true;
        }

        // Captura foto mostrando cartas ocultas alocadas na zona RESOLVE
        captureSnapshot(null, null, "PLAYER_PREPARATION_READY");

        // Se ambos confirmaram no Botão Principal, dispara a cadeia automática do combate!
        if (this.isPlayer1Ready && this.isPlayer2Ready) {
            this.isPlayer1Ready = false;
            this.isPlayer2Ready = false;
            runAutomatedCombatChain();
        }
    }

    /**
     * Executa em cadeia todas as fases automáticas gerando uma foto por efeito e por fase
     */
    public void runAutomatedCombatChain() {
        preparation(); // Muda para TurnPhase.COMBAT_START
        triggerActiveHeroesEffects(TurnPhase.COMBAT_START);
        resumeCombatChainFromCurrentPhase();
    }


    public void resumeCombatChainFromCurrentPhase() {
        // 1. Se estava em COMBAT_START, termina a fila de COMBAT_START
        if (this.turnPhase == TurnPhase.COMBAT_START) {
            resolutionQueue.resolveAll(this);
            if (isWaitingForPlayerChoice()) return; // Pausou no meio do COMBAT_START!

            // Avança para RESOLUTION e enfileira as 4 cartas base dos jogadores
            changePhase(TurnPhase.RESOLUTION);
            enqueueInitialResolutionCards();
        }

        // 2. Se está em RESOLUTION, resolve a fila (incluindo cartas extras jogadas por efeitos!)
        if (this.turnPhase == TurnPhase.RESOLUTION) {
            resolutionQueue.resolveAll(this);
            if (isWaitingForPlayerChoice()) return; // Pausou esperando o jogador escolher uma carta/herói!

            // Todas as cartas (as 4 iniciais + as extras inseridas por efeitos) já resolveram!
            moveResolvedCardsToDestination(gameState.getPlayer1PlayedCards());
            moveResolvedCardsToDestination(gameState.getPlayer2PlayedCards());
            gameState.getPlayer1PlayedCards().clear();
            gameState.getPlayer2PlayedCards().clear();

            changePhase(TurnPhase.COMBAT);
        }

        // 3. Matemática do Combate Físico
        if (this.turnPhase == TurnPhase.COMBAT) {
            combat(); // Calcula dano físico e muda para TurnPhase.COMBAT_END
            triggerActiveHeroesEffects(TurnPhase.COMBAT_END);
        }

        // 4. Se está em COMBAT_END, resolve os efeitos de fim de combate
        if (this.turnPhase == TurnPhase.COMBAT_END) {
            resolutionQueue.resolveAll(this);
            if (isWaitingForPlayerChoice()) return; // Pausou para escolha no fim do combate!

            processAttachedCardsDuration();
            runCombatEndCleanup();
            evaluateHeroesDeathAndReturnToBench(player1);
            evaluateHeroesDeathAndReturnToBench(player2);

            gameState.setActiveHeroPlayer1Id(null);
            gameState.setActiveHeroPlayer2Id(null);

            checkBoardRefresh(player1);
            checkBoardRefresh(player2);

            captureSnapshot(null, null, "COMBAT_END_CLEANUP");

            if (checkGameOver()) {
                changePhase(TurnPhase.GAME_OVER);
            } else {
                changePhase(TurnPhase.DISCARD);
            }
        }
    }

    private void enqueueInitialResolutionCards() {
        boolean p1HasInitiative = gameState.getActiveTurnPlayerId() == null
                || gameState.getActiveTurnPlayerId().equals(gameState.getPlayer1().getUuid());
        List<Card> firstPlayerCards = p1HasInitiative
                ? gameState.getPlayer1PlayedCards()
                : gameState.getPlayer2PlayedCards();

        List<Card> secondPlayerCards = p1HasInitiative
                ? gameState.getPlayer2PlayedCards()
                : gameState.getPlayer1PlayedCards();

        int maxCards = Math.max(firstPlayerCards.size(), secondPlayerCards.size());

        for (int i = 0; i < maxCards; i++) {
            if (i < firstPlayerCards.size()) {
                Card c1 = firstPlayerCards.get(i);
                if (c1.getEffects() != null) {
                    c1.getEffects().stream()
                            .filter(effect -> effect.predicate(c1, TurnPhase.RESOLUTION))
                            .forEach(effect -> resolutionQueue.enqueue(effect, c1));
                }
            }
            if (i < secondPlayerCards.size()) {
                Card c2 = secondPlayerCards.get(i);
                if (c2.getEffects() != null) {
                    c2.getEffects().stream()
                            .filter(effect -> effect.predicate(c2, TurnPhase.RESOLUTION))
                            .forEach(effect -> resolutionQueue.enqueue(effect, c2));
                }
            }
        }

        HeroCard p1Hero = getActiveHero(player1, gameState.getActiveHeroPlayer1Id());
        HeroCard p2Hero = getActiveHero(player2, gameState.getActiveHeroPlayer2Id());
        if (p1Hero != null) p1Hero.onPhaseChange(TurnPhase.RESOLUTION, resolutionQueue);
        if (p2Hero != null) p2Hero.onPhaseChange(TurnPhase.RESOLUTION, resolutionQueue);
    }
    public boolean hasMagicBasicAttacks(UUID heroId) {
        if (heroId == null) return false;
        List<Card> allCards = new ArrayList<>();
        if (player1 != null && player1.getCurrentDeck() != null && player1.getCurrentDeck().getCardList() != null) {
            allCards.addAll(player1.getCurrentDeck().getCardList());
        }
        if (player2 != null && player2.getCurrentDeck() != null && player2.getCurrentDeck().getCardList() != null) {
            allCards.addAll(player2.getCurrentDeck().getCardList());
        }

        return allCards.stream()
                .filter(c -> c.getZoneId() == ZoneId.ATTACHED
                        && heroId.equals(c.getAttachedToCardId())
                        && c.getEffects() != null)
                .anyMatch(c -> c.getEffects().stream()
                        .anyMatch(eff -> eff instanceof com.alkateca.lendasdapoeira.effects.combat.ConvertAttacksToMagicDamageEffect));
    }

    /**
     * 3. Matemática do Combate Físico (TurnPhase.COMBAT)
     */
    public void combat() {
        HeroCard p1Hero = getActiveHero(player1, gameState.getActiveHeroPlayer1Id());
        HeroCard p2Hero = getActiveHero(player2, gameState.getActiveHeroPlayer2Id());

        if (p1Hero != null && p2Hero != null) {
            int damageToP2;
            if (hasMagicBasicAttacks(p1Hero.getUuid())) {
                int p1RawMagic = p1Hero.getAtaque() + p1Hero.getDanoBonus();
                damageToP2 = Math.max(0, p1RawMagic - p2Hero.getEspirito());
            } else {
                // Cálculo de Dano Físico de P1 em P2: (Ataque + DanoBônus + VulnerabilidadeAlvo) - (DefesaAlvo + ReduçãoDanoAlvo)
                int p1RawDamage = p1Hero.getAtaque() + p1Hero.getDanoBonus() + p2Hero.getVulnerabilidade();
                int p2Mitigation = p2Hero.getDefesa() + p2Hero.getReducaoDano();
                damageToP2 = Math.max(0, p1RawDamage - p2Mitigation);
            }

            int damageToP1;
            if (hasMagicBasicAttacks(p2Hero.getUuid())) {
                int p2RawMagic = p2Hero.getAtaque() + p2Hero.getDanoBonus();
                damageToP1 = Math.max(0, p2RawMagic - p1Hero.getEspirito());
            } else {
                // Cálculo de Dano Físico de P2 em P1
                int p2RawDamage = p2Hero.getAtaque() + p2Hero.getDanoBonus() + p1Hero.getVulnerabilidade();
                int p1Mitigation = p1Hero.getDefesa() + p1Hero.getReducaoDano();
                damageToP1 = Math.max(0, p2RawDamage - p1Mitigation);
            }

            // A vida pode ficar abaixo de 0 durante o combate (game_architecture.md)
            p2Hero.setVidaAtual(p2Hero.getVidaAtual() - damageToP2);
            p1Hero.setVidaAtual(p1Hero.getVidaAtual() - damageToP1);

            // Tira a foto do impacto do combate físico antes de ir para COMBAT_END
            captureSnapshot(p1Hero, null, "PHYSICAL_COMBAT_EXCHANGE");
        }

        changePhase(TurnPhase.COMBAT_END);
    }

    /**
     * 4. Final do Combate e Verificação Estrita de Morte (TurnPhase.COMBAT_END)
     */
    public void combatEnd() {
        triggerActiveHeroesEffects(TurnPhase.COMBAT_END);
        resolutionQueue.resolveAll(this);

        processAttachedCardsDuration();
        runCombatEndCleanup();

        // Regra 4 de game_architecture.md: Ao final de COMBAT_END, contabiliza se algum Herói morreu (vidaAtual <= 0)
        evaluateHeroesDeathAndReturnToBench(player1);
        evaluateHeroesDeathAndReturnToBench(player2);

        gameState.setActiveHeroPlayer1Id(null);
        gameState.setActiveHeroPlayer2Id(null);

        checkBoardRefresh(player1);
        checkBoardRefresh(player2);

        // Foto após atualizar estaVivo = false e retornar os Heróis para a BENCH
        captureSnapshot(null, null, "COMBAT_END_CLEANUP");

        if (checkGameOver()) {
            changePhase(TurnPhase.GAME_OVER);
        } else {
            changePhase(TurnPhase.DISCARD);
        }
    }

    private void refillHandToFive(Player player) {
        long currentHandSize = player.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.HAND)
                .count();

        int drawn = 0;
        while (currentHandSize < 5) {
            Card cardToDraw = player.getCurrentDeck().getCardList().stream()
                    .filter(c -> c.getZoneId() == ZoneId.DECK)
                    .findFirst()
                    .orElse(null);

            if (cardToDraw != null) {
                cardToDraw.setZoneId(ZoneId.HAND);
                currentHandSize++;
                drawn++;
            } else {
                break; // Baralho acabou (fadiga)
            }
        }
        if (drawn > 0) {
            System.out.println("Jogador " + player.getName() + " comprou " + drawn + " carta(s) até completar 5 na mão.");
        }
    }

    private void evaluateHeroesDeathAndReturnToBench(Player player) {
        for (Card c : player.getCurrentDeck().getHeroList()) {
            if (c instanceof HeroCard) {
                HeroCard hero = (HeroCard) c;
                if (hero.getZoneId() == ZoneId.BATTLE) {
                    hero.setEstaAtivo(false); // Já atuou neste ciclo
                    hero.setZoneId(ZoneId.BENCH); // Retorna para a bancada
                }
                if (hero.getVidaAtual() <= 0) {
                    hero.setEstaVivo(false); // Permanece na BENCH, mas marcado como morto
                    hero.setEstaAtivo(false);
                }
            }
        }
    }

    private boolean checkGameOver() {
        boolean p1AllDead = player1.getCurrentDeck().getHeroList().stream()
                .filter(c -> c instanceof HeroCard)
                .noneMatch(c -> Boolean.TRUE.equals(((HeroCard) c).getEstaVivo()));

        boolean p2AllDead = player2.getCurrentDeck().getHeroList().stream()
                .filter(c -> c instanceof HeroCard)
                .noneMatch(c -> Boolean.TRUE.equals(((HeroCard) c).getEstaVivo()));

        return p1AllDead || p2AllDead;
    }

    /**
     * 5. Executado quando cada jogador clica no Botão Principal na TurnPhase.DISCARD
     */
    public void submitDiscard(UUID playerId, List<UUID> discardCardUuids) {
        if (this.turnPhase != TurnPhase.DISCARD) {
            throw new IllegalStateException("Ação inválida fora da fase DISCARD.");
        }

        boolean isP1 = playerId.equals(player1.getUuid());

        // Evita duplo envio do mesmo jogador
        if ((isP1 && this.isPlayer1Ready) || (!isP1 && this.isPlayer2Ready)) {
            return;
        }

        Player player = isP1 ? player1 : player2;

        if (discardCardUuids != null) {
            for (UUID cardId : discardCardUuids) {
                player.getCurrentDeck().getCardList().stream()
                        .filter(c -> c.getUuid().equals(cardId) && c.getZoneId() == ZoneId.HAND)
                        .findFirst()
                        .ifPresent(card -> card.setZoneId(ZoneId.DISCARD));
            }
        }

        if (isP1) {
            this.isPlayer1Ready = true;
        } else {
            this.isPlayer2Ready = true;
        }

        captureSnapshot(null, null, "PLAYER_DISCARD_CONFIRMED");

        // Quando AMBOS confirmam o descarte, compra até 5 cartas, inverte a iniciativa e segue a partida!
        if (this.isPlayer1Ready && this.isPlayer2Ready) {
            this.isPlayer1Ready = false;
            this.isPlayer2Ready = false;
            discard();
        }
    }

    /**
     * Invoca uma carta pelo nome (de qualquer zona do baralho ou criando uma nova instância se não existir no deck),
     * resolve seus efeitos de entrada (RESOLUTION) imediatamente na fila atual e a move para a zona de destino (ex: ATTACHED).
     */
    public Card invokeCard(Player owner, HeroCard targetHero, String cardName) {
        if (owner == null || cardName == null) return null;

        // 1. Procura a carta priorizando DECK primeiro (para não remover cartas da mão ou da fila de resolução)
        Card invokedCard = null;
        if (owner.getCurrentDeck() != null && owner.getCurrentDeck().getCardList() != null) {
            invokedCard = owner.getCurrentDeck().getCardList().stream()
                    .filter(c -> cardName.equalsIgnoreCase(c.getCardName()) && c.getZoneId() == ZoneId.DECK)
                    .findFirst()
                    .orElse(null);
        }

        // 2. Se a carta não estiver no DECK, instancia a carta, injeta os efeitos via CardFactory e registra no deck do jogador!
        if (invokedCard == null) {
            Card templateInDeck = null;
            if (owner.getCurrentDeck() != null && owner.getCurrentDeck().getCardList() != null) {
                templateInDeck = owner.getCurrentDeck().getCardList().stream()
                        .filter(c -> cardName.equalsIgnoreCase(c.getCardName()))
                        .findFirst()
                        .orElse(null);
            }

            invokedCard = new Card();
            invokedCard.setUuid(UUID.randomUUID());
            invokedCard.setCardName(cardName);
            invokedCard.setOwnerId(owner.getUuid());

            if (templateInDeck != null) {
                invokedCard.setCardType(templateInDeck.getCardType());
                invokedCard.setColor(templateInDeck.getColor());
                invokedCard.setItemType(templateInDeck.getItemType());
                invokedCard.setEmpunhadura(templateInDeck.getEmpunhadura());
                invokedCard.setDescricao(templateInDeck.getDescricao());
                invokedCard.setRacas(templateInDeck.getRacas() != null ? new ArrayList<>(templateInDeck.getRacas()) : new ArrayList<>());
                invokedCard.setAfinidades(templateInDeck.getAfinidades() != null ? new ArrayList<>(templateInDeck.getAfinidades()) : new ArrayList<>());
                invokedCard.setOrdem(templateInDeck.getOrdem() != null ? new ArrayList<>(templateInDeck.getOrdem()) : new ArrayList<>());
                invokedCard.setClasse(templateInDeck.getClasse() != null ? new ArrayList<>(templateInDeck.getClasse()) : new ArrayList<>());
            }

            CardFactory.buildCardLogic(invokedCard);
            boolean hasEquipEffect = invokedCard.getEffects() != null && invokedCard.getEffects().stream()
                    .anyMatch(e -> e.getClass().getSimpleName().startsWith("Equip"));
            if (hasEquipEffect && invokedCard.getCardType() == null) {
                invokedCard.setCardType(com.alkateca.lendasdapoeira.enums.CardType.ITEM);
                if ("Rapieira de Cristal".equalsIgnoreCase(cardName) || "Dragast".equalsIgnoreCase(cardName)) {
                    invokedCard.setItemType(com.alkateca.lendasdapoeira.enums.ItemType.ARMA);
                    invokedCard.setEmpunhadura(1);
                }
            } else if (invokedCard.getCardType() == null) {
                invokedCard.setCardType(com.alkateca.lendasdapoeira.enums.CardType.SPELL);
            }

            if ("Bola de fogo".equalsIgnoreCase(cardName)) {
                invokedCard.setCardType(com.alkateca.lendasdapoeira.enums.CardType.SPELL);
                if (invokedCard.getColor() == null) {
                    invokedCard.setColor(com.alkateca.lendasdapoeira.enums.Colors.NEUTRAL);
                }
                if (invokedCard.getAfinidades() == null || invokedCard.getAfinidades().isEmpty()) {
                    invokedCard.setAfinidades(new ArrayList<>(List.of(com.alkateca.lendasdapoeira.enums.Afinidade.FOGO)));
                }
                if (invokedCard.getDescricao() == null) {
                    invokedCard.setDescricao("Afinidade Fogo\nO Herói Inimigo Sofre 4 mais seu Espírito de Dano Mágico.");
                }
            } else if ("Fortaleça sua Fé".equalsIgnoreCase(cardName)) {
                invokedCard.setCardType(com.alkateca.lendasdapoeira.enums.CardType.THEURGY);
                if (invokedCard.getColor() == null) {
                    invokedCard.setColor(com.alkateca.lendasdapoeira.enums.Colors.YELLOW);
                }
                if (invokedCard.getOrdem() == null || invokedCard.getOrdem().isEmpty()) {
                    invokedCard.setOrdem(new ArrayList<>(List.of(com.alkateca.lendasdapoeira.enums.Ordem.GUARDAO_DO_TRONO)));
                }
                if (invokedCard.getDescricao() == null) {
                    invokedCard.setDescricao("Guardião do Trono\nSeu Herói Recebe +2 de Ataque até o Final do Combate.\nSeus Transformadores Recebem +1 de Ataque até o Final da Partida.");
                }
            } else if ("Estatica".equalsIgnoreCase(cardName)) {
                invokedCard.setCardType(com.alkateca.lendasdapoeira.enums.CardType.SPELL);
                if (invokedCard.getColor() == null) {
                    invokedCard.setColor(com.alkateca.lendasdapoeira.enums.Colors.NEUTRAL);
                }
                if (invokedCard.getAfinidades() == null || invokedCard.getAfinidades().isEmpty()) {
                    invokedCard.setAfinidades(new ArrayList<>(List.of(com.alkateca.lendasdapoeira.enums.Afinidade.AR)));
                }
                if (invokedCard.getDescricao() == null) {
                    invokedCard.setDescricao("Afinidade Ar\nO Herói Inimigo Sofre 3 mais seu Espírito de Dano Mágico. Invocar: Estatica.");
                }
            }

            if (owner.getCurrentDeck() != null && owner.getCurrentDeck().getCardList() != null) {
                owner.getCurrentDeck().getCardList().add(invokedCard);
            }
        }

        // 3. Coloca na zona RESOLVE temporariamente e executa seus efeitos de resolução IMEDIATAMENTE
        invokedCard.setZoneId(ZoneId.RESOLVE);
        if (targetHero != null && invokedCard.getCardType() == com.alkateca.lendasdapoeira.enums.CardType.ITEM) {
            invokedCard.setAttachedToCardId(targetHero.getUuid());
        }

        boolean isP1Owner = owner.getUuid().equals(player1.getUuid());
        List<Card> ownerPlayedList = isP1Owner
                ? gameState.getPlayer1PlayedCards()
                : gameState.getPlayer2PlayedCards();
        boolean addedTempToPlayed = false;
        if (ownerPlayedList != null && !ownerPlayedList.contains(invokedCard)) {
            ownerPlayedList.add(invokedCard);
            addedTempToPlayed = true;
        }

        if (invokedCard.getEffects() != null) {
            for (com.alkateca.lendasdapoeira.effects.Effect effect : invokedCard.getEffects()) {
                // Mesmo que a partida esteja em COMBAT_START ou GAME_START,
                // ao "Invocar" uma carta nós disparamos os efeitos de "Ao Jogar/Equipar" (TurnPhase.RESOLUTION) dela!
                if (effect.predicate(invokedCard, TurnPhase.RESOLUTION)) {
                    effect.resolve(this, invokedCard);
                    captureSnapshot(invokedCard, effect, "EFFECT_RESOLVED");
                }
            }
        }

        if (addedTempToPlayed) {
            ownerPlayedList.remove(invokedCard);
        }

        // 4. Move a carta invocada para o destino final (ATTACHED no Herói se for Item/Anexo, ou DISCARD/EXILE)
        if (invokedCard.getAttachedToCardId() != null) {
            invokedCard.setZoneId(ZoneId.ATTACHED);
        } else if (invokedCard.getCardType() == com.alkateca.lendasdapoeira.enums.CardType.ITEM && targetHero != null) {
            invokedCard.setAttachedToCardId(targetHero.getUuid());
            invokedCard.setZoneId(ZoneId.ATTACHED);
        } else if (invokedCard.getDescricao() != null && invokedCard.getDescricao().contains("Exilar.")) {
            invokedCard.setZoneId(ZoneId.EXILE);
        } else {
            invokedCard.setZoneId(ZoneId.DISCARD);
        }

        return invokedCard;
    }

    /**
     * Chamado por qualquer Effect que precise que o jogador escolha uma carta ou Herói na tela!
     */
    public void requestPlayerChoice(
            UUID choosingPlayerId,
            Card sourceCard,
            String targetType,
            String promptMessage,
            List<UUID> validTargetUuids,
            boolean canSkip,
            java.util.function.Consumer<UUID> onTargetChosen) {

        // Se não houver nenhum alvo válido na zona (ex: descarte vazio), não trava o jogo
        if (validTargetUuids == null || validTargetUuids.isEmpty()) {
            System.out.println("-> Nenhum alvo válido disponível para " + sourceCard.getCardName() + ". Efeito ignorado.");
            return;
        }

        MatchSnapshotDTO.PendingChoiceDTO choiceDto = new MatchSnapshotDTO.PendingChoiceDTO();
        choiceDto.setChoosingPlayerId(choosingPlayerId);
        choiceDto.setSourceCardUuid(sourceCard.getUuid());
        choiceDto.setSourceCardName(sourceCard.getCardName());
        choiceDto.setTargetType(targetType);
        choiceDto.setPromptMessage(promptMessage);
        choiceDto.setValidTargetUuids(validTargetUuids);
        choiceDto.setCanSkip(canSkip);

        this.gameState.setPendingChoice(choiceDto);
        this.pendingChoiceCallback = onTargetChosen;
    }

    /**
     * Recebe o clique do jogador vindo do Front-End, executa a ação na carta/herói escolhido
     * e retoma a ResolutionQueue exatamente de onde parou!
     */
    public void submitPlayerChoice(UUID playerId, UUID selectedTargetUuid) {
        if (!isWaitingForPlayerChoice()) {
            throw new IllegalStateException("Não há nenhuma escolha pendente neste momento.");
        }

        MatchSnapshotDTO.PendingChoiceDTO choice = this.gameState.getPendingChoice();
        if (!playerId.equals(choice.getChoosingPlayerId())) {
            throw new IllegalStateException("Apenas o jogador dono do efeito pode realizar esta escolha.");
        }

        if (selectedTargetUuid != null && !choice.getValidTargetUuids().contains(selectedTargetUuid)) {
            throw new IllegalArgumentException("O alvo selecionado não é válido para este efeito.");
        }

        // Limpa o bloqueio antes de rodar o callback (pois o callback pode encadear outra escolha!)
        java.util.function.Consumer<UUID> callback = this.pendingChoiceCallback;
        this.gameState.setPendingChoice(null);
        this.pendingChoiceCallback = null;

        // Se selectedTargetUuid for null (clicou em "Pular Efeito"), apenas não executa o callback
        if (selectedTargetUuid != null || !choice.isCanSkip()) {
            callback.accept(selectedTargetUuid);
        }

        // Tira foto do resultado imediato da escolha e retoma a cadeia de combate!
        captureSnapshot(null, null, "CHOICE_RESOLVED");
        resumeCombatChainFromCurrentPhase();
    }

    /**
     * Joga uma carta extra (vinda da Mão, Descarte ou Baralho por efeito) diretamente na Zona RESOLVE,
     * fazendo-a aparecer visualmente junto das 4 cartas dos jogadores e enfileirando os efeitos dela!
     */
    public void playCardIntoResolution(Card cardToPlay) {
        if (cardToPlay == null) return;

        cardToPlay.setZoneId(ZoneId.RESOLVE);

        boolean isP1 = cardToPlay.getOwnerId().equals(player1.getUuid());
        List<Card> ownerPlayedList = isP1
                ? gameState.getPlayer1PlayedCards()
                : gameState.getPlayer2PlayedCards();

        if (!ownerPlayedList.contains(cardToPlay)) {
            ownerPlayedList.add(cardToPlay);
        }

        // Enfileira os efeitos de TurnPhase.RESOLUTION da nova carta no topo da fila (em ordem reversa para manter a sequência original)
        if (cardToPlay.getEffects() != null) {
            List<com.alkateca.lendasdapoeira.effects.Effect> resEffects = new ArrayList<>();
            for (com.alkateca.lendasdapoeira.effects.Effect eff : cardToPlay.getEffects()) {
                if (eff.predicate(cardToPlay, TurnPhase.RESOLUTION)) {
                    resEffects.add(eff);
                }
            }
            for (int i = resEffects.size() - 1; i >= 0; i--) {
                resolutionQueue.enqueueFirst(resEffects.get(i), cardToPlay);
            }
        }

        HeroCard ownerHero = isP1
                ? getActiveHero(player1, gameState.getActiveHeroPlayer1Id())
                : getActiveHero(player2, gameState.getActiveHeroPlayer2Id());
        if (ownerHero != null && ownerHero.getEffects() != null) {
            ownerHero.getEffects().stream()
                    .filter(eff -> eff instanceof com.alkateca.lendasdapoeira.effects.combat.OnPlayWeaponBuffAndHealEffect
                            && eff.predicate(ownerHero, TurnPhase.RESOLUTION))
                    .forEach(eff -> resolutionQueue.enqueue(eff, ownerHero));
        }
    }

    public void gamePhase(){
        switch (turnPhase) {
            case GAME_START:
                startGame();
                break;
            case CHOICE:
                break;
            case PREPARATION:
                choice();
                break;
            case COMBAT_START:
                combatStart();
                break;
            case RESOLUTION:
                resolution();
                break;
            case COMBAT:
                combat();
                break;
            case COMBAT_END:
                combatEnd();
                break;
            case DISCARD:
                discard();
                break;
            case GAME_OVER:
                gameOver();
                break;
        }
    }
}
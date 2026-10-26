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

    private UUID winnerPlayerId;
    private UUID forfeitedByPlayerId;
    private String winReason;
    private boolean pvpMatch = false;

    private java.util.function.Consumer<UUID> pendingChoiceCallback;

    public boolean isWaitingForPlayerChoice() {
        return this.gameState.getPendingChoice() != null && this.pendingChoiceCallback != null;
    }

    private TurnPhase turnPhase;

    // Histórico de snapshots para a timeline do Front-End
    private List<MatchSnapshotDTO> snapshotHistory = new ArrayList<>();
    private long snapshotSequenceCounter = 0L;

    public long nextSnapshotSequence() {
        return ++this.snapshotSequenceCounter;
    }

    // Ações de limpeza de buffs/debuffs temporários que expiram ao final do combate
    private List<Runnable> combatEndCleanupActions = new ArrayList<>();

    // Rastreia as cartas jogadas que já resolveram na fase RESOLUTION atual e as cartas que foram apenas invocadas
    private List<Card> resolvedCardsInCurrentCombat = new ArrayList<>();
    private Set<UUID> invokedCardIdsInCurrentCombat = new HashSet<>();
    private final Map<UUID, Integer> combatEffectiveHealingByPlayer = new HashMap<>();
    private final Map<UUID, Integer> maxHeroAttackInCurrentCombat = new HashMap<>();
    private final Set<UUID> doubleAttackHeroIdsInCurrentCombat = new HashSet<>();

    public void enableDoubleAttackForCurrentCombat(UUID heroId) {
        if (heroId != null) {
            this.doubleAttackHeroIdsInCurrentCombat.add(heroId);
        }
    }

    public boolean hasDoubleAttackInCurrentCombat(UUID heroId) {
        return heroId != null && this.doubleAttackHeroIdsInCurrentCombat.contains(heroId);
    }

    public int getEffectiveHealingInCurrentCombat(UUID playerId) {
        if (playerId == null) return 0;
        updateCombatMetricsFromBoardState(null);
        return this.combatEffectiveHealingByPlayer.getOrDefault(playerId, 0);
    }

    public int getMaxHeroAttackInCurrentCombat(UUID heroId) {
        if (heroId == null) return 0;
        HeroCard hero = findHeroById(heroId);
        int currentAtk = hero != null ? hero.getAtaque() : 0;
        return Math.max(currentAtk, this.maxHeroAttackInCurrentCombat.getOrDefault(heroId, 0));
    }

    public int getGoblinEffectBonus(Card sourceCard) {
        if (sourceCard == null || sourceCard.getRacas() == null
                || !sourceCard.getRacas().contains(com.alkateca.lendasdapoeira.enums.Raca.GOBLIN)) {
            return 0;
        }
        boolean isP1 = player1 != null && sourceCard.getOwnerId() != null && sourceCard.getOwnerId().equals(player1.getUuid());
        Player owner = isP1 ? player1 : player2;
        if (owner == null || owner.getCurrentDeck() == null || owner.getCurrentDeck().getHeroList() == null) {
            return 0;
        }
        return owner.getCurrentDeck().getHeroList().stream()
                .filter(h -> h != null
                        && Boolean.TRUE.equals(h.getEstaVivo())
                        && h.getVidaAtual() > 0
                        && h.getEffects() != null)
                .flatMap(h -> h.getEffects().stream())
                .filter(eff -> eff instanceof com.alkateca.lendasdapoeira.effects.combat.AuraGoblinEffectAmplifierEffect)
                .mapToInt(eff -> ((com.alkateca.lendasdapoeira.effects.combat.AuraGoblinEffectAmplifierEffect) eff).getBonusEffectValue())
                .sum();
    }

    public void markCardAsResolved(Card card) {
        if (card != null && !resolvedCardsInCurrentCombat.contains(card)) {
            resolvedCardsInCurrentCombat.add(card);
        }
    }

    public List<Card> getResolvedPlayedCardsForPlayer(UUID playerId) {
        if (playerId == null || gameState == null) return Collections.emptyList();
        boolean isP1 = player1 != null && playerId.equals(player1.getUuid());
        List<Card> playedCards = isP1
                ? gameState.getPlayer1PlayedCards()
                : gameState.getPlayer2PlayedCards();
        if (playedCards == null) return Collections.emptyList();

        if (!resolvedCardsInCurrentCombat.isEmpty()) {
            return playedCards.stream()
                    .filter(c -> resolvedCardsInCurrentCombat.contains(c) && !invokedCardIdsInCurrentCombat.contains(c.getUuid()))
                    .toList();
        }
        return playedCards.stream()
                .filter(c -> !invokedCardIdsInCurrentCombat.contains(c.getUuid()))
                .toList();
    }

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
        this.resolvedCardsInCurrentCombat.clear();
        this.invokedCardIdsInCurrentCombat.clear();
        this.combatEffectiveHealingByPlayer.clear();
        this.maxHeroAttackInCurrentCombat.clear();
        this.doubleAttackHeroIdsInCurrentCombat.clear();
    }

    // =========================================================================
    // MÉTRICAS E ESTATÍSTICAS ACUMULADAS DA PARTIDA
    // =========================================================================
    private int turnsElapsed = 1;
    private final Map<UUID, Integer> lastRecordedHeroHp = new HashMap<>();
    private final Map<UUID, MatchSnapshotDTO.PlayerMatchStatsDTO> playerStatsMap = new HashMap<>();
    private boolean rewardsGranted = false;

    private void initMatchMetrics() {
        this.turnsElapsed = 1;
        this.lastRecordedHeroHp.clear();
        this.playerStatsMap.clear();
        this.rewardsGranted = false;

        if (player1 != null) {
            playerStatsMap.put(player1.getUuid(), new MatchSnapshotDTO.PlayerMatchStatsDTO(
                    player1.getUuid(), player1.getName(), false, 0, 0, 0, 0, 0, 0, 0
            ));
            recordInitialHeroesHp(player1);
        }
        if (player2 != null) {
            playerStatsMap.put(player2.getUuid(), new MatchSnapshotDTO.PlayerMatchStatsDTO(
                    player2.getUuid(), player2.getName(), false, 0, 0, 0, 0, 0, 0, 0
            ));
            recordInitialHeroesHp(player2);
        }
    }

    private void recordInitialHeroesHp(Player player) {
        if (player.getCurrentDeck() == null || player.getCurrentDeck().getHeroList() == null) return;
        for (HeroCard hero : player.getCurrentDeck().getHeroList()) {
            if (hero != null) {
                lastRecordedHeroHp.put(hero.getUuid(), hero.getVidaAtual());
            }
        }
    }

    /**
     * Atualiza automaticamente Dano Causado, Dano Sofrido e Cura Realizada comparando o HP atual
     * dos Heróis com o estado anterior a cada snapshot.
     */
    public void updateCombatMetricsFromBoardState(Card sourceCard) {
        if (player1 == null || player2 == null) return;
        if (playerStatsMap.isEmpty()) {
            initMatchMetrics();
        }

        updateHeroHpDeltasForPlayer(player1, player2, sourceCard);
        updateHeroHpDeltasForPlayer(player2, player1, sourceCard);
    }

    private void updateHeroHpDeltasForPlayer(Player owner, Player opponent, Card sourceCard) {
        if (owner.getCurrentDeck() == null || owner.getCurrentDeck().getHeroList() == null) return;

        MatchSnapshotDTO.PlayerMatchStatsDTO ownerStats = playerStatsMap.get(owner.getUuid());
        MatchSnapshotDTO.PlayerMatchStatsDTO opponentStats = playerStatsMap.get(opponent.getUuid());
        if (ownerStats == null || opponentStats == null) return;

        for (HeroCard hero : owner.getCurrentDeck().getHeroList()) {
            if (hero == null) continue;

            int currentAtk = hero.getAtaque();
            int prevMaxAtk = maxHeroAttackInCurrentCombat.getOrDefault(hero.getUuid(), currentAtk);
            if (currentAtk > prevMaxAtk || !maxHeroAttackInCurrentCombat.containsKey(hero.getUuid())) {
                maxHeroAttackInCurrentCombat.put(hero.getUuid(), currentAtk);
            }

            int currentHp = hero.getVidaAtual();
            int previousHp = lastRecordedHeroHp.getOrDefault(hero.getUuid(), hero.getVidaMaxima());
            int delta = currentHp - previousHp;

            if (delta < 0) {
                int dmg = Math.abs(delta);
                ownerStats.setTotalDamageTaken(ownerStats.getTotalDamageTaken() + dmg);

                // Se foi auto-dano explícito de uma carta de Ação/Item do próprio jogador (e não troca física)
                boolean isSelfInflicted = sourceCard != null
                        && !(sourceCard instanceof HeroCard)
                        && owner.getUuid().equals(sourceCard.getOwnerId());

                if (isSelfInflicted) {
                    ownerStats.setTotalDamageDealt(ownerStats.getTotalDamageDealt() + dmg);
                } else {
                    opponentStats.setTotalDamageDealt(opponentStats.getTotalDamageDealt() + dmg);
                    if (this.turnPhase != null) {
                        trackMissionProgress(opponent.getUuid(), "DEAL_DAMAGE", dmg);
                    }
                }
            } else if (delta > 0) {
                ownerStats.setTotalHealingDone(ownerStats.getTotalHealingDone() + delta);
                if (this.turnPhase == TurnPhase.COMBAT_START
                        || this.turnPhase == TurnPhase.RESOLUTION
                        || this.turnPhase == TurnPhase.COMBAT
                        || this.turnPhase == TurnPhase.COMBAT_END) {
                    combatEffectiveHealingByPlayer.put(
                            owner.getUuid(),
                            combatEffectiveHealingByPlayer.getOrDefault(owner.getUuid(), 0) + delta
                    );
                }
            }

            lastRecordedHeroHp.put(hero.getUuid(), currentHp);
        }
    }

    public void incrementCardsPlayed(UUID playerId, int count) {
        if (playerId == null || count <= 0) return;
        MatchSnapshotDTO.PlayerMatchStatsDTO stats = playerStatsMap.get(playerId);
        if (stats != null) {
            stats.setCardsPlayed(stats.getCardsPlayed() + count);
        }
    }

    public MatchSnapshotDTO.MatchResultSummaryDTO buildMatchResultSummary() {
        if (player1 == null || player2 == null) return null;
        if (this.turnPhase != TurnPhase.GAME_OVER && this.winnerPlayerId == null) {
            return null;
        }
        if (playerStatsMap.isEmpty()) initMatchMetrics();

        finalizePlayerScore(player1, player2);
        finalizePlayerScore(player2, player1);
        if (this.winnerPlayerId != null) {
            this.rewardsGranted = true;
        }

        MatchSnapshotDTO.PlayerMatchStatsDTO p1Stats = playerStatsMap.get(player1.getUuid());
        MatchSnapshotDTO.PlayerMatchStatsDTO p2Stats = playerStatsMap.get(player2.getUuid());

        String winnerName = null;
        if (winnerPlayerId != null) {
            winnerName = winnerPlayerId.equals(player1.getUuid()) ? player1.getName() : player2.getName();
        }

        return new MatchSnapshotDTO.MatchResultSummaryDTO(
                this.winnerPlayerId,
                winnerName,
                this.winReason != null ? this.winReason : "ELIMINATION",
                this.turnsElapsed,
                p1Stats,
                p2Stats
        );
    }

    private void finalizePlayerScore(Player player, Player opponent) {
        MatchSnapshotDTO.PlayerMatchStatsDTO stats = playerStatsMap.get(player.getUuid());
        if (stats == null) return;

        boolean isWin = this.winnerPlayerId != null && this.winnerPlayerId.equals(player.getUuid());
        stats.setWinner(isWin);

        int alive = (int) player.getCurrentDeck().getHeroList().stream()
                .filter(h -> h != null && Boolean.TRUE.equals(h.getEstaVivo()))
                .count();
        int defeatedEnemyHeroes = (int) opponent.getCurrentDeck().getHeroList().stream()
                .filter(h -> h != null && !Boolean.TRUE.equals(h.getEstaVivo()))
                .count();

        stats.setHeroesAlive(alive);
        stats.setHeroesDefeated(defeatedEnemyHeroes);

        int score = stats.getTotalDamageDealt() + stats.getTotalHealingDone();
        stats.setMatchScore(score);

        // Cálculo de Recompensa de Poeira
        boolean playerForfeited = this.forfeitedByPlayerId != null && this.forfeitedByPlayerId.equals(player.getUuid());
        int totalEarned = (!playerForfeited && isWin) ? (this.pvpMatch ? 150 : 50) : 0;

        if (!this.rewardsGranted && totalEarned > 0) {
            player.addGold(totalEarned);
        }

        stats.setPoeiraEarned(totalEarned);
        stats.setTotalPoeira(player.getGold());
    }

    public GameEngine(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.cardsOnBoard = new ArrayList<>();
        // Inicialização correta do GameState para evitar NPE
        this.gameState = new GameState();
        this.gameState.setPlayer1(player1);
        this.gameState.setPlayer2(player2);
        initMatchMetrics();
    }

    private void drawInitialHand(Player player) {
        refillHandToFive(player);
    }

    public void startGame() {
        System.out.println("--- INICIANDO A PARTIDA ---");
        initMatchMetrics();

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

        initLiberationZoneForPlayer(player1);
        initLiberationZoneForPlayer(player2);

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

        // Finaliza o destino de itens invocados no GAME_START (ex: Naelis -> Rapieira de Cristal)
        moveResolvedCardsToDestination(gameState.getPlayer1PlayedCards());
        moveResolvedCardsToDestination(gameState.getPlayer2PlayedCards());
        gameState.getPlayer1PlayedCards().clear();
        gameState.getPlayer2PlayedCards().clear();
        resolvedCardsInCurrentCombat.clear();
        invokedCardIdsInCurrentCombat.clear();

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
        // 1. Muda para RESOLUTION para revelar as cartas nos 4 slots centrais
        changePhase(TurnPhase.RESOLUTION);

        enqueueInitialResolutionCards();

        // 2. Resolve todos os efeitos da pilha
        resolutionQueue.resolveAll(this);

        // 3. Apenas Magias, Ações e Teurgias saem da mesa agora (indo para Descarte ou Exílio)
        // Os Itens (Armas/Joias) continuam visíveis na mesa durante o Combate Físico (COMBAT)!
        moveSpellsAndActionsToDestination(gameState.getPlayer1PlayedCards());
        moveSpellsAndActionsToDestination(gameState.getPlayer2PlayedCards());

        resolvedCardsInCurrentCombat.clear();
        invokedCardIdsInCurrentCombat.clear();

        changePhase(TurnPhase.COMBAT);
    }

    private void moveSpellsAndActionsToDestination(List<Card> playedCards) {
        if (playedCards == null) return;
        Iterator<Card> it = playedCards.iterator();
        while (it.hasNext()) {
            Card card = it.next();
            if (card.getCardType() != com.alkateca.lendasdapoeira.enums.CardType.ITEM) {
                if (card.getDescricao() != null && card.getDescricao().contains("Exilar.")) {
                    card.setZoneId(com.alkateca.lendasdapoeira.enums.ZoneId.EXILE);
                } else {
                    card.setZoneId(com.alkateca.lendasdapoeira.enums.ZoneId.DISCARD);
                }
                it.remove(); // Remove da lista de resolução imediatamente
            }
        }
    }

    private void moveItemsToAttachedDestination(List<Card> playedCards) {
        if (playedCards == null || playedCards.isEmpty()) return;
        UUID activeP1HeroId = gameState.getActiveHeroPlayer1Id();
        UUID activeP2HeroId = gameState.getActiveHeroPlayer2Id();

        for (Card card : playedCards) {
            if (card.getCardType() == com.alkateca.lendasdapoeira.enums.CardType.ITEM) {
                boolean isP1Owner = card.getOwnerId().equals(player1.getUuid());
                UUID defaultHeroId = isP1Owner ? activeP1HeroId : activeP2HeroId;
                if (card.getAttachedToCardId() == null && defaultHeroId != null) {
                    card.setAttachedToCardId(defaultHeroId);
                }
                card.setZoneId(com.alkateca.lendasdapoeira.enums.ZoneId.ATTACHED);
            }
        }
    }

    public void discard() {
        refillHandToFive(player1);
        refillHandToFive(player2);

        if (gameState.getActiveTurnPlayerId().equals(gameState.getPlayer1().getUuid())) {
            gameState.setActiveTurnPlayerId(gameState.getPlayer2().getUuid());
        } else {
            gameState.setActiveTurnPlayerId(gameState.getPlayer1().getUuid());
        }

        this.turnsElapsed++;
        trackMissionProgress(player1.getUuid(), "SURVIVE_TURNS", 1);
        trackMissionProgress(player2.getUuid(), "SURVIVE_TURNS", 1);
        changePhase(TurnPhase.CHOICE);
    }

    /**
     * Processa a desistência (W.O.) de um jogador, declarando o oponente vencedor e encerrando a partida.
     */
    public void forfeitMatch(UUID quittingPlayerId) {
        if (this.turnPhase == TurnPhase.GAME_OVER) {
            return;
        }

        boolean isP1Quitting = player1 != null && player1.getUuid().equals(quittingPlayerId);
        Player quittingPlayer = isP1Quitting ? player1 : player2;
        Player winnerPlayer = isP1Quitting ? player2 : player1;

        this.forfeitedByPlayerId = quittingPlayer != null ? quittingPlayer.getUuid() : quittingPlayerId;
        this.winnerPlayerId = winnerPlayer != null ? winnerPlayer.getUuid() : null;
        this.winReason = "FORFEIT";

        // Limpa qualquer escolha pendente para não travar a tela do oponente
        if (this.gameState != null) {
            this.gameState.setPendingChoice(null);
            this.gameState.setTurnPhase(TurnPhase.GAME_OVER);
        }
        this.pendingChoiceCallback = null;
        this.turnPhase = TurnPhase.GAME_OVER;

        // Marca os heróis do desistente como inativos/derrotados visualmente
        if (quittingPlayer != null && quittingPlayer.getCurrentDeck() != null && quittingPlayer.getCurrentDeck().getHeroList() != null) {
            for (Card c : quittingPlayer.getCurrentDeck().getHeroList()) {
                if (c instanceof HeroCard hero) {
                    hero.setEstaVivo(false);
                    hero.setEstaAtivo(false);
                }
            }
        }

        gameOver();
        captureSnapshot(null, null, "MATCH_FORFEITED");
    }

    public void gameOver() {
        System.out.println("--- PARTIDA ENCERRADA ---");
        if (this.turnPhase != TurnPhase.GAME_OVER) {
            this.turnPhase = TurnPhase.GAME_OVER;
            if (this.gameState != null) {
                this.gameState.setTurnPhase(TurnPhase.GAME_OVER);
            }
        }
        buildMatchResultSummary();
    }


    private void changePhase(TurnPhase newPhase) {
        this.turnPhase = newPhase;
        if (this.gameState != null) {
            this.gameState.setTurnPhase(newPhase);
        }
        if (newPhase == TurnPhase.GAME_OVER) {
            gameOver();
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

        if (phase == TurnPhase.COMBAT_START || phase == TurnPhase.COMBAT_END) {
            List<Card> allHeroes = new ArrayList<>();
            allHeroes.addAll(player1.getCurrentDeck().getHeroList());
            allHeroes.addAll(player2.getCurrentDeck().getHeroList());

            for (Card hCard : allHeroes) {
                if (hCard instanceof HeroCard && hCard.getZoneId() == ZoneId.BENCH && Boolean.TRUE.equals(((HeroCard) hCard).getEstaVivo())) {
                    if (hCard.getEffects() != null) {
                        hCard.getEffects().stream()
                                .filter(eff -> eff.canTriggerFromBench() && eff.predicate(hCard, phase))
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
        updateCombatMetricsFromBoardState(sourceCard);
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
            incrementCardsPlayed(playerId, targetPlayedList.size());
            for (Card playedCard : targetPlayedList) {
                trackCardPlayedMissions(playerId, playedCard);
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
        // 1. Se estava em COMBAT_START, termina a fila de COMBAT_START (incluindo cartas invocadas no início do combate)
        if (this.turnPhase == TurnPhase.COMBAT_START) {
            resolutionQueue.resolveAll(this);
            if (isWaitingForPlayerChoice()) return;

            // Equipa itens que já foram invocados no COMBAT_START para que seus buffs valham na RESOLUTION,
            // mas MANTÉM todas as cartas na lista visual (player1PlayedCards / player2PlayedCards) até o fim!
            attachItemsAlreadyInvokedInCombatStart(gameState.getPlayer1PlayedCards());
            attachItemsAlreadyInvokedInCombatStart(gameState.getPlayer2PlayedCards());

            changePhase(TurnPhase.RESOLUTION);
            enqueueInitialResolutionCards();
        }

        // 2. Se está em RESOLUTION, resolve a fila (incluindo cartas e tokens invocados encadeados!)
        if (this.turnPhase == TurnPhase.RESOLUTION) {
            resolutionQueue.resolveAll(this);
            if (isWaitingForPlayerChoice()) return;

            // Anexa os itens aos heróis para que contem no combate físico (COMBAT),
            // porém NÃO limpa player1PlayedCards/player2PlayedCards ainda para que persistam na mesa!
            attachItemsAlreadyInvokedInCombatStart(gameState.getPlayer1PlayedCards());
            attachItemsAlreadyInvokedInCombatStart(gameState.getPlayer2PlayedCards());

            changePhase(TurnPhase.COMBAT);
        }

        // 3. Matemática do Combate Físico
        if (this.turnPhase == TurnPhase.COMBAT) {
            combat(); // Calcula dano físico e muda para TurnPhase.COMBAT_END
            triggerActiveHeroesEffects(TurnPhase.COMBAT_END);
        }

        // 4. Se está em COMBAT_END, resolve os efeitos de fim de combate, anexa os itens nos heróis ainda em BATTLE e depois retorna-os à BENCH
        if (this.turnPhase == TurnPhase.COMBAT_END) {
            resolutionQueue.resolveAll(this);
            if (isWaitingForPlayerChoice()) return;

            // Move as cartas da fila de resolução para ATTACHED/DISCARD/EXILE com os heróis ainda no slot de combate
            moveResolvedCardsToDestination(gameState.getPlayer1PlayedCards());
            moveResolvedCardsToDestination(gameState.getPlayer2PlayedCards());
            gameState.getPlayer1PlayedCards().clear();
            gameState.getPlayer2PlayedCards().clear();

            // Snapshot dedicado: os itens saem da fila de resolução e deslizam para os heróis ainda em BATTLE
            captureSnapshot(null, null, "COMBAT_END_ITEMS_ATTACHED");

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

    /**
     * Garante que itens jogados/invocados já fiquem vinculados como ATTACHED no Herói durante o combate
     * sem removê-los da lista visual da pilha de resolução até o fim da sequência de combate.
     */
    private void attachItemsAlreadyInvokedInCombatStart(List<Card> playedCards) {
        if (playedCards == null) return;
        UUID activeP1HeroId = gameState.getActiveHeroPlayer1Id();
        UUID activeP2HeroId = gameState.getActiveHeroPlayer2Id();

        for (Card card : playedCards) {
            if (card.getAttachedToCardId() != null) {
                card.setZoneId(ZoneId.ATTACHED);
            } else if (card.getCardType() == com.alkateca.lendasdapoeira.enums.CardType.ITEM) {
                boolean isP1Owner = card.getOwnerId().equals(player1.getUuid());
                UUID defaultHeroId = isP1Owner ? activeP1HeroId : activeP2HeroId;
                if (defaultHeroId != null) {
                    card.setAttachedToCardId(defaultHeroId);
                    card.setZoneId(ZoneId.ATTACHED);
                }
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

        // Filtra apenas as cartas de PREPARATION que ainda não foram resolvidas/invocadas no COMBAT_START
        List<Card> firstPlayerPending = firstPlayerCards.stream()
                .filter(c -> !resolvedCardsInCurrentCombat.contains(c) && !invokedCardIdsInCurrentCombat.contains(c.getUuid()))
                .toList();

        List<Card> secondPlayerPending = secondPlayerCards.stream()
                .filter(c -> !resolvedCardsInCurrentCombat.contains(c) && !invokedCardIdsInCurrentCombat.contains(c.getUuid()))
                .toList();

        int maxCards = Math.max(firstPlayerPending.size(), secondPlayerPending.size());

        for (int i = 0; i < maxCards; i++) {
            if (i < firstPlayerPending.size()) {
                enqueueCardAndHeroOnPlayResolution(firstPlayerPending.get(i), false);
            }
            if (i < secondPlayerPending.size()) {
                enqueueCardAndHeroOnPlayResolution(secondPlayerPending.get(i), false);
            }
        }
    }

    /**
     * Enfileira uma carta jogada e, imediatamente após seus efeitos de RESOLUTION,
     * enfileira os efeitos de TurnPhase.RESOLUTION ("Ao jogar") do Herói ativo de seu dono.
     */
    private void enqueueCardAndHeroOnPlayResolution(Card card, boolean atFront) {
        if (card == null) return;

        com.alkateca.lendasdapoeira.effects.Effect markResolvedStep = new com.alkateca.lendasdapoeira.effects.Effect() {
            @Override
            public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
                return turnPhase == TurnPhase.RESOLUTION;
            }

            @Override
            public boolean shouldCaptureSnapshot() {
                return false;
            }

            @Override
            public void resolve(GameEngine gameEngine, Card sourceCard) {
                gameEngine.markCardAsResolved(sourceCard);
            }
        };

        boolean isP1 = player1 != null && card.getOwnerId() != null && card.getOwnerId().equals(player1.getUuid());
        HeroCard ownerHero = isP1
                ? getActiveHero(player1, gameState.getActiveHeroPlayer1Id())
                : getActiveHero(player2, gameState.getActiveHeroPlayer2Id());

        List<com.alkateca.lendasdapoeira.effects.Effect> cardEffects = new ArrayList<>();
        if (card.getEffects() != null) {
            for (com.alkateca.lendasdapoeira.effects.Effect eff : card.getEffects()) {
                if (eff.predicate(card, TurnPhase.RESOLUTION)) {
                    cardEffects.add(eff);
                }
            }
        }

        List<com.alkateca.lendasdapoeira.effects.Effect> heroOnPlayEffects = new ArrayList<>();
        if (ownerHero != null && ownerHero.getEffects() != null) {
            for (com.alkateca.lendasdapoeira.effects.Effect eff : ownerHero.getEffects()) {
                if (eff.predicate(ownerHero, TurnPhase.RESOLUTION)) {
                    heroOnPlayEffects.add(eff);
                }
            }
        }

        if (!atFront) {
            resolutionQueue.enqueue(markResolvedStep, card);
            for (com.alkateca.lendasdapoeira.effects.Effect eff : cardEffects) {
                resolutionQueue.enqueue(eff, card);
            }
            for (com.alkateca.lendasdapoeira.effects.Effect eff : heroOnPlayEffects) {
                resolutionQueue.enqueue(eff, ownerHero);
            }
        } else {
            for (int i = heroOnPlayEffects.size() - 1; i >= 0; i--) {
                resolutionQueue.enqueueFirst(heroOnPlayEffects.get(i), ownerHero);
            }
            for (int i = cardEffects.size() - 1; i >= 0; i--) {
                resolutionQueue.enqueueFirst(cardEffects.get(i), card);
            }
            resolutionQueue.enqueueFirst(markResolvedStep, card);
        }
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

    public int calculatePhysicalDamage(HeroCard attacker, HeroCard defender, int basePhysicalDamage) {
        if (defender == null) return 0;
        int bonusDamage = attacker != null ? attacker.getDanoBonus() : 0;
        int rawDamage = basePhysicalDamage + bonusDamage;
        int totalMitigation = defender.getDefesa() + defender.getReducaoDano();
        return Math.max(0, (rawDamage - totalMitigation) + defender.getVulnerabilidade());
    }

    public int calculateMagicDamage(HeroCard attacker, HeroCard defender, int baseMagicDamage) {
        if (defender == null) return 0;
        int bonusDamage = attacker != null ? attacker.getDanoBonus() : 0;
        int rawMagic = baseMagicDamage + bonusDamage;
        int magicMitigation = defender.getEspirito();
        return Math.max(0, (rawMagic - magicMitigation) + defender.getVulnerabilidade());
    }

    public int calculateDirectDamage(HeroCard attacker, HeroCard defender, int baseDirectDamage) {
        if (defender == null) return 0;
        int bonusDamage = attacker != null ? attacker.getDanoBonus() : 0;
        int rawDirect = baseDirectDamage + bonusDamage + defender.getVulnerabilidade();
        return Math.max(0, rawDirect);
    }

    public int calculateBasicAttackDamage(HeroCard attacker, HeroCard defender) {
        if (attacker == null || defender == null) return 0;
        if (hasMagicBasicAttacks(attacker.getUuid())) {
            // --- CÁLCULO DE DANO MÁGICO (ou Ataques Básicos Mágicos) ---
            return calculateMagicDamage(attacker, defender, attacker.getAtaque());
        }
        // --- CÁLCULO DE DANO FÍSICO ---
        return calculatePhysicalDamage(attacker, defender, attacker.getAtaque());
    }

    /**
     * 3. Matemática do Combate Físico (TurnPhase.COMBAT)
     */
    public void combat() {
        HeroCard p1Hero = getActiveHero(player1, gameState.getActiveHeroPlayer1Id());
        HeroCard p2Hero = getActiveHero(player2, gameState.getActiveHeroPlayer2Id());

        if (p1Hero != null && p2Hero != null) {
            int p1Attacks = hasDoubleAttackInCurrentCombat(p1Hero.getUuid()) ? 2 : 1;
            int p2Attacks = hasDoubleAttackInCurrentCombat(p2Hero.getUuid()) ? 2 : 1;

            int damageToP2 = calculateBasicAttackDamage(p1Hero, p2Hero) * p1Attacks;
            int damageToP1 = calculateBasicAttackDamage(p2Hero, p1Hero) * p2Attacks;

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

        // AGORA SIM: No fim do combate, os itens que estavam na mesa deslizam e se anexam aos heróis!
        moveItemsToAttachedDestination(gameState.getPlayer1PlayedCards());
        moveItemsToAttachedDestination(gameState.getPlayer2PlayedCards());
        gameState.getPlayer1PlayedCards().clear();
        gameState.getPlayer2PlayedCards().clear();

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
        int newlyDeadAllies = 0;

        for (Card c : player.getCurrentDeck().getHeroList()) {
            if (c instanceof HeroCard hero) {
                if (hero.getZoneId() == ZoneId.BATTLE) {
                    hero.setEstaAtivo(false); // Já atuou neste ciclo
                    hero.setZoneId(ZoneId.BENCH); // Retorna para a bancada
                }
                if (hero.getVidaAtual() <= 0) {
                    boolean wasAlive = Boolean.TRUE.equals(hero.getEstaVivo());
                    hero.setEstaVivo(false);
                    hero.setEstaAtivo(false);

                    // Regra: Quando o personagem morre, todas as cartas anexadas a ele são descartadas!
                    detachAllCardsFromHero(hero.getUuid(), ZoneId.DISCARD);

                    if (wasAlive) {
                        newlyDeadAllies++;
                        UUID killerPlayerId = player.getUuid().equals(player1.getUuid())
                                ? player2.getUuid()
                                : player1.getUuid();
                        trackMissionProgress(killerPlayerId, "DEFEAT_HERO", 1);
                    }
                }
            }
        }

        if (newlyDeadAllies > 0) {
            trackMissionProgress(player.getUuid(), "ALLY_HEROES_DEAD", newlyDeadAllies);
        }
    }

    /**
     * Desanexa todas as cartas vinculadas a um Herói específico (usado quando o Herói morre).
     */
    public void detachAllCardsFromHero(UUID heroId, ZoneId destinationZone) {
        if (heroId == null) return;
        List<Card> allCards = new ArrayList<>();
        if (player1 != null && player1.getCurrentDeck() != null && player1.getCurrentDeck().getCardList() != null) {
            allCards.addAll(player1.getCurrentDeck().getCardList());
        }
        if (player2 != null && player2.getCurrentDeck() != null && player2.getCurrentDeck().getCardList() != null) {
            allCards.addAll(player2.getCurrentDeck().getCardList());
        }

        for (Card card : allCards) {
            if (card.getZoneId() == ZoneId.ATTACHED && heroId.equals(card.getAttachedToCardId())) {
                detachCard(card, destinationZone);
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

        if (p1AllDead || p2AllDead) {
            this.winReason = "ELIMINATION";
            if (p1AllDead && !p2AllDead) {
                this.winnerPlayerId = player2.getUuid();
            } else if (p2AllDead && !p1AllDead) {
                this.winnerPlayerId = player1.getUuid();
            }
            return true;
        }
        return false;
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

    public void captureSpawnSnapshot(
            Card invokerCard,
            Card spawnedCard,
            com.alkateca.lendasdapoeira.effects.Effect effect,
            String eventType,
            UUID targetHeroId,
            String targetZone,
            Integer targetSlotIndex,
            boolean tokenCreated,
            boolean zoneFull,
            String customLogMessage) {
        updateCombatMetricsFromBoardState(invokerCard);
        MatchSnapshotDTO snapshot = SnapshotFactory.createSpawnSnapshot(
                this, invokerCard, spawnedCard, effect, eventType,
                targetHeroId, targetZone, targetSlotIndex, tokenCreated, zoneFull, customLogMessage
        );
        this.snapshotHistory.add(snapshot);
    }

    /**
     * Invoca uma carta pelo nome (do baralho ou criando um token dinâmico se não existir no deck),
     * valida limites de zona/slots, dispara o evento explícito CARD_SPAWNED / TOKEN_SUMMONED
     * e resolve seus efeitos de entrada (RESOLUTION) imediatamente.
     */
    /**
     * Regra de Invocação:
     * 1. Busca a carta no Baralho (DECK) -> Mão (HAND) -> Descarte (DISCARD).
     * 2. Apenas se não existir em nenhuma dessas zonas, instancia um Token dinâmico.
     * 3. Aloca a carta na pilha de resolução (playerPlayedCards) e enfileira seus efeitos no início da ResolutionQueue (LIFO encadeado).
     * 4. Mantém a carta visível na mesa durante toda a sequência de combate (só é recolhida no COMBAT_END).
     */
    public Card invokeCard(Player owner, HeroCard targetHero, String cardName) {
        return invokeCard(owner, targetHero, targetHero, cardName);
    }

    public Card invokeCard(Player owner, Card invokerCard, HeroCard targetHero, String cardName) {
        if (owner == null || cardName == null) return null;

        Card invokedCard = null;
        boolean isDynamicToken = false;

        // 1. Regra de busca de Invocação: Baralho (DECK) -> Mão (HAND) -> Descarte (DISCARD)
        if (owner.getCurrentDeck() != null && owner.getCurrentDeck().getCardList() != null) {
            List<Card> deckCards = owner.getCurrentDeck().getCardList();

            // 1º Prioridade: Baralho (DECK)
            invokedCard = deckCards.stream()
                    .filter(c -> cardName.equalsIgnoreCase(c.getCardName()) && c.getZoneId() == ZoneId.DECK)
                    .findFirst()
                    .orElse(null);

            // 2º Prioridade: Mão (HAND)
            if (invokedCard == null) {
                invokedCard = deckCards.stream()
                        .filter(c -> cardName.equalsIgnoreCase(c.getCardName()) && c.getZoneId() == ZoneId.HAND)
                        .findFirst()
                        .orElse(null);
            }

            // 3º Prioridade: Descarte (DISCARD)
            if (invokedCard == null) {
                invokedCard = deckCards.stream()
                        .filter(c -> cardName.equalsIgnoreCase(c.getCardName()) && c.getZoneId() == ZoneId.DISCARD)
                        .findFirst()
                        .orElse(null);
            }
        }

        // 2. Se não encontrou em DECK, HAND ou DISCARD, cria um Token dinâmico
        if (invokedCard == null) {
            isDynamicToken = true;
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
                invokedCard.setRarity(templateInDeck.getRarity());
                invokedCard.setCardSet(templateInDeck.getCardSet());
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

            if ("Rapieira de Cristal".equalsIgnoreCase(cardName) && invokedCard.getDescricao() == null) {
                invokedCard.setColor(com.alkateca.lendasdapoeira.enums.Colors.WHITE);
                invokedCard.setDescricao("Equipar: +3 de Ataque.\nFinal do Combate: Causa 5 de Dano Mágico.");
            } else if ("Dragast".equalsIgnoreCase(cardName) && invokedCard.getDescricao() == null) {
                invokedCard.setColor(com.alkateca.lendasdapoeira.enums.Colors.WHITE);
                invokedCard.setDescricao("Equipar: +2 de Espírito e +2 de Ataque.\nFinal do Combate: Cura 2 de Vida.");
            } else if ("Bola de fogo".equalsIgnoreCase(cardName)) {
                invokedCard.setCardType(com.alkateca.lendasdapoeira.enums.CardType.SPELL);
                if (invokedCard.getColor() == null) invokedCard.setColor(com.alkateca.lendasdapoeira.enums.Colors.NEUTRAL);
                if (invokedCard.getAfinidades() == null || invokedCard.getAfinidades().isEmpty()) {
                    invokedCard.setAfinidades(new ArrayList<>(List.of(com.alkateca.lendasdapoeira.enums.Afinidade.FOGO)));
                }
                if (invokedCard.getDescricao() == null) {
                    invokedCard.setDescricao("Afinidade Fogo\nO Herói Inimigo Sofre 4 mais seu Espírito de Dano Mágico.");
                }
            } else if ("Fortaleça sua Fé".equalsIgnoreCase(cardName)) {
                invokedCard.setCardType(com.alkateca.lendasdapoeira.enums.CardType.THEURGY);
                if (invokedCard.getColor() == null) invokedCard.setColor(com.alkateca.lendasdapoeira.enums.Colors.YELLOW);
                if (invokedCard.getOrdem() == null || invokedCard.getOrdem().isEmpty()) {
                    invokedCard.setOrdem(new ArrayList<>(List.of(com.alkateca.lendasdapoeira.enums.Ordem.GUARDAO_DO_TRONO)));
                }
                if (invokedCard.getDescricao() == null) {
                    invokedCard.setDescricao("Guardião do Trono\nSeu Herói Recebe +2 de Ataque até o Final do Combate.\nSeus Transformadores Recebem +1 de Ataque até o Final da Partida.");
                }
            } else if ("Estatica".equalsIgnoreCase(cardName)) {
                invokedCard.setCardType(com.alkateca.lendasdapoeira.enums.CardType.SPELL);
                if (invokedCard.getColor() == null) invokedCard.setColor(com.alkateca.lendasdapoeira.enums.Colors.NEUTRAL);
                if (invokedCard.getAfinidades() == null || invokedCard.getAfinidades().isEmpty()) {
                    invokedCard.setAfinidades(new ArrayList<>(List.of(com.alkateca.lendasdapoeira.enums.Afinidade.AR)));
                }
                if (invokedCard.getDescricao() == null) {
                    invokedCard.setDescricao("Afinidade Ar\nO Herói Inimigo Sofre 3 mais seu Espírito de Dano Mágico. Invocar: Estatica.");
                }
            } else if ("Ritos Fúnebres".equalsIgnoreCase(cardName)) {
                invokedCard.setCardType(com.alkateca.lendasdapoeira.enums.CardType.ACTION);
                if (invokedCard.getColor() == null) invokedCard.setColor(com.alkateca.lendasdapoeira.enums.Colors.BLACK);
                if (invokedCard.getDescricao() == null) {
                    invokedCard.setDescricao("Zumbi\nEscolha um Herói: Recupere 5 de Vida.\nSe estiver Morto e sua vida ficar acima de 0: O reviva.");
                }
            } else if ("Vendaval Arcano".equalsIgnoreCase(cardName)) {
                invokedCard.setCardType(com.alkateca.lendasdapoeira.enums.CardType.SPELL);
                if (invokedCard.getColor() == null) invokedCard.setColor(com.alkateca.lendasdapoeira.enums.Colors.NEUTRAL);
                if (invokedCard.getAfinidades() == null || invokedCard.getAfinidades().isEmpty()) {
                    invokedCard.setAfinidades(new ArrayList<>(List.of(com.alkateca.lendasdapoeira.enums.Afinidade.AR)));
                }
                if (invokedCard.getDescricao() == null) {
                    invokedCard.setDescricao("Afinidade Ar\nDescarte um Item Aleatório de cada Herói Inimigo.");
                }
            } else if ("Barreira de Gelo".equalsIgnoreCase(cardName)) {
                invokedCard.setCardType(com.alkateca.lendasdapoeira.enums.CardType.SPELL);
                if (invokedCard.getColor() == null) invokedCard.setColor(com.alkateca.lendasdapoeira.enums.Colors.GREEN);
                if (invokedCard.getAfinidades() == null || invokedCard.getAfinidades().isEmpty()) {
                    invokedCard.setAfinidades(new ArrayList<>(List.of(com.alkateca.lendasdapoeira.enums.Afinidade.AGUA)));
                }
                if (invokedCard.getDescricao() == null) {
                    invokedCard.setDescricao("Afinidade Agua\nEnquanto estiver Equipada: Seus Heróis Recebem +1 de Espírito e +2 de Defesa.");
                }
            } else if ("Tristeza".equalsIgnoreCase(cardName)) {
                invokedCard.setCardType(com.alkateca.lendasdapoeira.enums.CardType.ACTION);
                if (invokedCard.getColor() == null) invokedCard.setColor(com.alkateca.lendasdapoeira.enums.Colors.NEUTRAL);
                if (invokedCard.getDescricao() == null) {
                    invokedCard.setDescricao("Exilar.");
                }
            }

            if (owner.getCurrentDeck() != null && owner.getCurrentDeck().getCardList() != null) {
                owner.getCurrentDeck().getCardList().add(invokedCard);
            }
        }

        // 3. Verifica limites físicos da zona/slot para armas (ItemType.ARMA)
        if (targetHero != null && invokedCard.getCardType() == com.alkateca.lendasdapoeira.enums.CardType.ITEM) {
            if (invokedCard.getItemType() == com.alkateca.lendasdapoeira.enums.ItemType.ARMA) {
                int usedSlots = getUsedWeaponSlotsForHero(targetHero.getUuid());
                int requiredSlots = Math.max(1, invokedCard.getEmpunhadura());
                if (usedSlots + requiredSlots > targetHero.getMaxWeaponSlots()) {
                    invokedCard.setAttachedToCardId(null);
                    invokedCard.setZoneId(ZoneId.DISCARD);
                    captureSpawnSnapshot(
                            targetHero,
                            invokedCard,
                            null,
                            "SPAWN_FAILED_ZONE_FULL",
                            targetHero.getUuid(),
                            ZoneId.DISCARD.name(),
                            -1,
                            isDynamicToken,
                            true,
                            "⚠️ Limite de empunhadura atingido em " + targetHero.getCardName() + "! " + invokedCard.getCardName() + " foi para o Descarte."
                    );
                    return invokedCard;
                }
            }
            invokedCard.setAttachedToCardId(targetHero.getUuid());
        }

        // 4. Aloca a carta na zona RESOLVE e na lista de cartas em resolução do jogador (persistindo até o fim do combate!)
        invokedCard.setZoneId(ZoneId.RESOLVE);
        invokedCardIdsInCurrentCombat.add(invokedCard.getUuid());

        boolean isP1Owner = owner.getUuid().equals(player1.getUuid());
        List<Card> ownerPlayedList = isP1Owner
                ? gameState.getPlayer1PlayedCards()
                : gameState.getPlayer2PlayedCards();

        if (ownerPlayedList != null && !ownerPlayedList.contains(invokedCard)) {
            ownerPlayedList.add(invokedCard);
        }

        int slotIndex = ownerPlayedList != null ? Math.max(0, ownerPlayedList.indexOf(invokedCard)) : 0;
        String spawnEventType = isDynamicToken ? "TOKEN_SUMMONED" : "CARD_SPAWNED";
        String destinationZone = (invokedCard.getCardType() == com.alkateca.lendasdapoeira.enums.CardType.ITEM && targetHero != null)
                ? ZoneId.ATTACHED.name()
                : ZoneId.RESOLVE.name();

        // 5. Dispara o snapshot explícito da carta surgindo na pilha de resolução
        Card sourceInvoker = invokerCard != null ? invokerCard : targetHero;
        captureSpawnSnapshot(
                sourceInvoker,
                invokedCard,
                null,
                spawnEventType,
                targetHero != null ? targetHero.getUuid() : null,
                destinationZone,
                slotIndex,
                isDynamicToken,
                false,
                "✨ Invocado: " + invokedCard.getCardName() + (targetHero != null ? " (" + targetHero.getCardName() + ")" : "")
        );

        // 6. Enfileira os efeitos da carta invocada na frente da ResolutionQueue (pilha encadeada LIFO)
        // para que sejam resolvidos em ordem pelo loop principal da ResolutionQueue!
        enqueueInvokeEffectsAtFront(invokedCard);

        return invokedCard;
    }

    /**
     * Enfileira os efeitos de entrada (RESOLUTION) de uma carta invocada no topo da ResolutionQueue,
     * funcionando em qualquer fase (GAME_START, COMBAT_START ou RESOLUTION).
     */
    private void enqueueInvokeEffectsAtFront(Card invokedCard) {
        if (invokedCard == null) return;

        if (this.turnPhase == TurnPhase.COMBAT_START || this.turnPhase == TurnPhase.RESOLUTION) {
            enqueueCardAndHeroOnPlayResolution(invokedCard, true);
            return;
        }

        // Mantém comportamento simples apenas no GAME_START (onde não há Herói ativo em BATTLE)
        List<com.alkateca.lendasdapoeira.effects.Effect> cardEffects = new ArrayList<>();
        if (invokedCard.getEffects() != null) {
            for (com.alkateca.lendasdapoeira.effects.Effect eff : invokedCard.getEffects()) {
                if (eff.predicate(invokedCard, TurnPhase.RESOLUTION)) {
                    cardEffects.add(eff);
                }
            }
        }
        for (int i = cardEffects.size() - 1; i >= 0; i--) {
            resolutionQueue.enqueueFirst(cardEffects.get(i), invokedCard);
        }
    }

    /**
     * Insere uma carta invocada na posição exata de execução dentro da pilha de resolução:
     * - Em COMBAT_START / GAME_START: entra no início da lista (antes das cartas de PREPARATION),
     *   mas logo após outras cartas que já tenham sido invocadas neste mesmo COMBAT_START.
     * - Em RESOLUTION: entra imediatamente após a carta que acabou de resolver e antes das próximas.
     */
    private void insertCardIntoResolutionStack(List<Card> ownerPlayedList, Card cardToInsert) {
        if (ownerPlayedList == null || cardToInsert == null || ownerPlayedList.contains(cardToInsert)) {
            return;
        }

        int insertIndex = 0;
        for (int i = 0; i < ownerPlayedList.size(); i++) {
            Card existing = ownerPlayedList.get(i);
            boolean alreadyResolvedOrInvoked = resolvedCardsInCurrentCombat.contains(existing)
                    || (invokedCardIdsInCurrentCombat.contains(existing.getUuid())
                        && !existing.getUuid().equals(cardToInsert.getUuid()));

            if (alreadyResolvedOrInvoked) {
                insertIndex = i + 1;
            } else {
                break;
            }
        }

        ownerPlayedList.add(insertIndex, cardToInsert);
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

        if (ownerPlayedList != null && !ownerPlayedList.contains(cardToPlay)) {
            ownerPlayedList.add(cardToPlay);
        }

        incrementCardsPlayed(cardToPlay.getOwnerId(), 1);
        trackCardPlayedMissions(cardToPlay.getOwnerId(), cardToPlay);

        int slotIndex = Math.max(0, ownerPlayedList.indexOf(cardToPlay));
        UUID activeHeroId = isP1 ? gameState.getActiveHeroPlayer1Id() : gameState.getActiveHeroPlayer2Id();
        HeroCard activeHero = findHeroById(activeHeroId);

        captureSpawnSnapshot(
                activeHero,
                cardToPlay,
                null,
                "CARD_SPAWNED",
                activeHeroId,
                ZoneId.RESOLVE.name(),
                slotIndex,
                false,
                false,
                "✨ Carta colocada em resolução: " + cardToPlay.getCardName()
        );

        enqueueCardAndHeroOnPlayResolution(cardToPlay, true);
    }

    // =========================================================================
    // SISTEMA DE LIBERAÇÃO (CARTAS DE MISSÃO / EVOLUÇÃO DE HERÓI)
    // =========================================================================

    /**
     * Inicializa as cartas da liberationList do jogador na zona especial ZoneId.LIBERATION
     * fora das 30 cartas do baralho principal.
     */
    public void initLiberationZoneForPlayer(Player player) {
        if (player == null || player.getCurrentDeck() == null) return;
        if (player.getCurrentDeck().getLiberationList() == null) {
            player.getCurrentDeck().setLiberationList(new ArrayList<>());
        }

        for (Card libCard : player.getCurrentDeck().getLiberationList()) {
            if (libCard == null) continue;
            if (libCard.getUuid() == null) libCard.setUuid(UUID.randomUUID());
            libCard.setOwnerId(player.getUuid());
            libCard.setLiberationCard(true);
            libCard.setZoneId(ZoneId.LIBERATION);
            libCard.setMissionCurrentProgress(0);
            if (libCard.getEffects() == null || libCard.getEffects().isEmpty()) {
                CardFactory.buildCardLogic(libCard);
            }
        }
    }

    /**
     * Avalia e incrementa o progresso das cartas na ZoneId.LIBERATION do jogador.
     * Ao atingir missionTargetValue, move a carta para a mão (ZoneId.HAND).
     */
    public void trackMissionProgress(UUID playerId, String missionType, int progressDelta) {
        if (playerId == null || missionType == null || progressDelta <= 0) return;
        Player player = (player1 != null && playerId.equals(player1.getUuid()))
                ? player1
                : ((player2 != null && playerId.equals(player2.getUuid())) ? player2 : null);

        if (player == null || player.getCurrentDeck() == null || player.getCurrentDeck().getLiberationList() == null) {
            return;
        }

        List<Card> liberationCards = player.getCurrentDeck().getLiberationList();
        for (Card card : liberationCards) {
            if (card == null || card.getZoneId() != ZoneId.LIBERATION || !card.isLiberationCard()) {
                continue;
            }
            if (card.getMissionType() == null || !card.getMissionType().equalsIgnoreCase(missionType)) {
                continue;
            }

            int target = Math.max(1, card.getMissionTargetValue());
            int previous = card.getMissionCurrentProgress();
            int updated = Math.min(target, previous + progressDelta);

            if (updated > previous) {
                card.setMissionCurrentProgress(updated);
                captureSnapshot(card, null, "MISSION_PROGRESS");

                if (updated >= target) {
                    liberateCardToHand(player, card);
                }
            }
        }
    }

    /**
     * Notifica todos os gatilhos de missão associados ao ato de jogar uma carta.
     */
    private void trackCardPlayedMissions(UUID playerId, Card playedCard) {
        if (playerId == null || playedCard == null) return;
        trackMissionProgress(playerId, "PLAY_CARD", 1);

        if (playedCard.getCardType() != null) {
            trackMissionProgress(playerId, "PLAY_" + playedCard.getCardType().name(), 1);
        }
        if (playedCard.getAfinidades() != null) {
            for (var afinidade : playedCard.getAfinidades()) {
                if (afinidade != null) {
                    trackMissionProgress(playerId, "PLAY_AFFINITY_" + afinidade.name(), 1);
                }
            }
        }
    }

    /**
     * Libera efetivamente uma carta da ZoneId.LIBERATION para a Mão (ZoneId.HAND) do jogador.
     */
    public void liberateCardToHand(Player player, Card card) {
        if (player == null || card == null) return;
        card.setZoneId(ZoneId.HAND);
        card.setOwnerId(player.getUuid());

        // Garante que a carta liberada faça parte da cardList ativa para poder ser jogada na PREPARATION
        List<Card> deckCards = player.getCurrentDeck().getCardList();
        if (deckCards != null && !deckCards.contains(card)) {
            deckCards.add(card);
        }

        System.out.println("✨ Carta de Liberação [" + card.getCardName() + "] adicionada à mão de " + player.getName());
        captureSnapshot(card, null, "CARD_LIBERATED");
    }

    /**
     * Substitui/Evolui um Herói em campo por sua forma evoluída preservando o UUID,
     * a proporção de vida atual, os equipamentos anexados (ATTACHED) e os modificadores permanentes.
     */
    public void evolveHero(Player owner, UUID targetHeroUuid, HeroCard evolvedTemplate) {
        evolveHero(owner, targetHeroUuid, evolvedTemplate, null);
    }

    public void evolveHero(Player owner, UUID targetHeroUuid, HeroCard evolvedTemplate, Card sourceLiberationCard) {
        if (owner == null || targetHeroUuid == null || evolvedTemplate == null) return;
        HeroCard targetHero = findHeroById(targetHeroUuid);
        if (targetHero == null || !Boolean.TRUE.equals(targetHero.getEstaVivo())) return;

        String oldName = targetHero.getCardName();

        // Calcula o dano sofrido ou a proporção de vida para preservar o estado de saúde proporcionalmente
        int oldMaxHp = Math.max(1, targetHero.getVidaMaxima());
        int oldCurrentHp = Math.max(1, targetHero.getVidaAtual());
        double healthRatio = (double) oldCurrentHp / (double) oldMaxHp;

        int newMaxHp = evolvedTemplate.getVidaMaxima() > 0
                ? evolvedTemplate.getVidaMaxima()
                : oldMaxHp + 5;
        int newCurrentHp = Math.max(1, (int) Math.round(newMaxHp * healthRatio));

        // Calcula os bônus de equipamentos já anexados a este Herói para somá-los aos novos atributos base
        int attachedSpiritBonus = 0;
        int attachedAttackBonus = 0;
        int attachedDefenseBonus = 0;

        if (owner.getCurrentDeck() != null && owner.getCurrentDeck().getCardList() != null) {
            for (Card c : owner.getCurrentDeck().getCardList()) {
                if (c.getZoneId() == ZoneId.ATTACHED && targetHeroUuid.equals(c.getAttachedToCardId()) && c.getEffects() != null) {
                    for (com.alkateca.lendasdapoeira.effects.Effect eff : c.getEffects()) {
                        if (eff instanceof com.alkateca.lendasdapoeira.effects.combat.EquipBuffEffect equipBuff) {
                            attachedSpiritBonus += equipBuff.getSpiritBuff();
                            attachedAttackBonus += equipBuff.getAttackBuff();
                            attachedDefenseBonus += equipBuff.getDefenseBuff();
                        }
                    }
                }
            }
        }

        // Atualiza os atributos do Herói in-place (mantendo o UUID para preservar todos os vínculos e anexos)
        targetHero.setCardName(evolvedTemplate.getCardName());
        if (evolvedTemplate.getColor() != null) targetHero.setColor(evolvedTemplate.getColor());
        if (evolvedTemplate.getDescricao() != null) targetHero.setDescricao(evolvedTemplate.getDescricao());

        targetHero.setEspirito(Math.max(targetHero.getEspirito(), evolvedTemplate.getEspirito() + attachedSpiritBonus));
        targetHero.setAtaque(Math.max(targetHero.getAtaque(), evolvedTemplate.getAtaque() + attachedAttackBonus));
        targetHero.setDefesa(Math.max(targetHero.getDefesa(), evolvedTemplate.getDefesa() + attachedDefenseBonus));
        targetHero.setVidaMaxima(newMaxHp);
        targetHero.setVidaAtual(newCurrentHp);
        targetHero.setMaxWeaponSlots(Math.max(targetHero.getMaxWeaponSlots(), evolvedTemplate.getMaxWeaponSlots()));
        targetHero.setReducaoDano(targetHero.getReducaoDano() + evolvedTemplate.getReducaoDano());
        targetHero.setDanoBonus(targetHero.getDanoBonus() + evolvedTemplate.getDanoBonus());

        if (evolvedTemplate.getRacas() != null && !evolvedTemplate.getRacas().isEmpty()) {
            targetHero.setRacas(new ArrayList<>(evolvedTemplate.getRacas()));
        }
        if (evolvedTemplate.getAfinidades() != null && !evolvedTemplate.getAfinidades().isEmpty()) {
            targetHero.setAfinidades(new ArrayList<>(evolvedTemplate.getAfinidades()));
        }
        if (evolvedTemplate.getOrdem() != null && !evolvedTemplate.getOrdem().isEmpty()) {
            targetHero.setOrdem(new ArrayList<>(evolvedTemplate.getOrdem()));
        }
        if (evolvedTemplate.getClasse() != null && !evolvedTemplate.getClasse().isEmpty()) {
            targetHero.setClasse(new ArrayList<>(evolvedTemplate.getClasse()));
        }

        if (evolvedTemplate.getEffects() != null && !evolvedTemplate.getEffects().isEmpty()) {
            targetHero.setEffects(new ArrayList<>(evolvedTemplate.getEffects()));
        } else {
            CardFactory.buildCardLogic(targetHero);
        }

        // Atualiza o registro de HP para que o ganho de vida máxima da evolução não seja confundido com cura comum
        lastRecordedHeroHp.put(targetHero.getUuid(), targetHero.getVidaAtual());

        // Se a carta de liberação que originou a evolução não tiver "Exilar." na descrição, garante seu exílio
        if (sourceLiberationCard != null && (sourceLiberationCard.getDescricao() == null || !sourceLiberationCard.getDescricao().contains("Exilar."))) {
            sourceLiberationCard.setDescricao((sourceLiberationCard.getDescricao() != null ? sourceLiberationCard.getDescricao() + "\n" : "") + "Exilar.");
        }

        MatchSnapshotDTO snapshot = SnapshotFactory.createSnapshot(this, targetHero, null, "HERO_EVOLVED");
        snapshot.getTriggerContext().setPreviousHeroName(oldName);
        snapshot.getTriggerContext().setEvolvedHeroName(targetHero.getCardName());
        snapshot.getTriggerContext().setTargetHeroId(targetHero.getUuid());
        snapshot.getTriggerContext().setOwnerPlayerId(owner.getUuid());
        snapshot.getTriggerContext().setLogMessage("🔥 EVOLUÇÃO! " + oldName + " evoluiu para " + targetHero.getCardName() + "!");
        this.snapshotHistory.add(snapshot);
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
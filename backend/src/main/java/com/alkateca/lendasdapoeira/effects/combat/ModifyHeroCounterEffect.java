package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CounterType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Efeito genérico e extensível para adicionar ou remover contadores (tokens/marcadores)
 * em Heróis alvo — ativos em combate, ambos simultaneamente, fora de combate (bancada)
 * ou escolhidos via {@code PendingChoice} — em qualquer etapa do turno ({@link TurnPhase}).
 */
public class ModifyHeroCounterEffect implements Effect {

    public enum CounterTargetScope {
        SELF_OR_ATTACHED_HERO,
        ACTIVE_ALLY_HERO,
        ACTIVE_ENEMY_HERO,
        BOTH_ACTIVE_HEROES,
        OUT_OF_COMBAT_ALLY_HEROES,
        OUT_OF_COMBAT_ENEMY_HEROES,
        ALL_OUT_OF_COMBAT_HEROES,
        ALL_ALLY_HEROES,
        ALL_ENEMY_HEROES,
        ALL_LIVING_HEROES,
        CHOOSE_ALLY_HERO,
        CHOOSE_ENEMY_HERO,
        CHOOSE_ANY_HERO,
        CHOOSE_OUT_OF_COMBAT_ALLY_HERO,
        CHOOSE_OUT_OF_COMBAT_ENEMY_HERO,
        CHOOSE_OUT_OF_COMBAT_ANY_HERO;

        public boolean isInteractiveChoice() {
            return this == CHOOSE_ALLY_HERO
                    || this == CHOOSE_ENEMY_HERO
                    || this == CHOOSE_ANY_HERO
                    || this == CHOOSE_OUT_OF_COMBAT_ALLY_HERO
                    || this == CHOOSE_OUT_OF_COMBAT_ENEMY_HERO
                    || this == CHOOSE_OUT_OF_COMBAT_ANY_HERO;
        }
    }

    private final Set<TurnPhase> triggerPhases;
    private final List<CounterTargetScope> targetScopes;
    private final CounterType counterType;
    private final int delta;
    private final boolean triggerFromBench;
    private final boolean canSkipChoice;

    public ModifyHeroCounterEffect(
            TurnPhase triggerPhase,
            CounterTargetScope targetScope,
            CounterType counterType,
            int delta) {
        this(
                triggerPhase,
                targetScope,
                counterType,
                delta,
                isOutOfCombatTurnPhase(triggerPhase),
                false
        );
    }

    public ModifyHeroCounterEffect(CounterTargetScope targetScope, CounterType counterType, int delta) {
        this(TurnPhase.RESOLUTION, targetScope, counterType, delta, false, false);
    }

    public ModifyHeroCounterEffect(
            TurnPhase triggerPhase,
            CounterTargetScope targetScope,
            CounterType counterType,
            int delta,
            boolean triggerFromBench) {
        this(triggerPhase, targetScope, counterType, delta, triggerFromBench, false);
    }

    public ModifyHeroCounterEffect(
            TurnPhase triggerPhase,
            CounterTargetScope targetScope,
            CounterType counterType,
            int delta,
            boolean triggerFromBench,
            boolean canSkipChoice) {
        this(
                List.of(triggerPhase != null ? triggerPhase : TurnPhase.RESOLUTION),
                List.of(targetScope != null ? targetScope : CounterTargetScope.ACTIVE_ALLY_HERO),
                counterType,
                delta,
                triggerFromBench,
                canSkipChoice
        );
    }

    public ModifyHeroCounterEffect(
            TurnPhase triggerPhase,
            Collection<CounterTargetScope> targetScopes,
            CounterType counterType,
            int delta) {
        this(
                List.of(triggerPhase != null ? triggerPhase : TurnPhase.RESOLUTION),
                targetScopes,
                counterType,
                delta,
                isOutOfCombatTurnPhase(triggerPhase),
                false
        );
    }

    public ModifyHeroCounterEffect(
            TurnPhase triggerPhase,
            Collection<CounterTargetScope> targetScopes,
            CounterType counterType,
            int delta,
            boolean triggerFromBench,
            boolean canSkipChoice) {
        this(
                List.of(triggerPhase != null ? triggerPhase : TurnPhase.RESOLUTION),
                targetScopes,
                counterType,
                delta,
                triggerFromBench,
                canSkipChoice
        );
    }

    public ModifyHeroCounterEffect(
            Collection<TurnPhase> triggerPhases,
            Collection<CounterTargetScope> targetScopes,
            CounterType counterType,
            int delta,
            boolean triggerFromBench,
            boolean canSkipChoice) {
        EnumSet<TurnPhase> phases = EnumSet.noneOf(TurnPhase.class);
        if (triggerPhases != null) {
            for (TurnPhase phase : triggerPhases) {
                if (phase != null) {
                    phases.add(phase);
                }
            }
        }
        if (phases.isEmpty()) {
            phases.add(TurnPhase.RESOLUTION);
        }
        this.triggerPhases = Collections.unmodifiableSet(phases);

        List<CounterTargetScope> scopes = new ArrayList<>();
        if (targetScopes != null) {
            for (CounterTargetScope scope : targetScopes) {
                if (scope != null) {
                    scopes.add(scope);
                }
            }
        }
        if (scopes.isEmpty()) {
            scopes.add(CounterTargetScope.ACTIVE_ALLY_HERO);
        }
        this.targetScopes = Collections.unmodifiableList(scopes);
        this.counterType = counterType;
        this.delta = delta;
        this.triggerFromBench = triggerFromBench;
        this.canSkipChoice = canSkipChoice;
    }

    private static boolean isOutOfCombatTurnPhase(TurnPhase phase) {
        return phase == TurnPhase.GAME_START
                || phase == TurnPhase.CHOICE
                || phase == TurnPhase.DISCARD;
    }

    public CounterTargetScope getTargetScope() {
        return targetScopes.get(0);
    }

    public List<CounterTargetScope> getTargetScopes() {
        return targetScopes;
    }

    public Set<TurnPhase> getTriggerPhases() {
        return triggerPhases;
    }

    public CounterType getCounterType() {
        return counterType;
    }

    public int getDelta() {
        return delta;
    }

    @Override
    public boolean canTriggerFromBench() {
        return triggerFromBench;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase != null && triggerPhases.contains(turnPhase);
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        if (gameEngine == null || sourceCard == null || counterType == null || delta == 0) {
            return;
        }
        if (gameEngine.getPlayer1() == null || gameEngine.getPlayer2() == null) {
            return;
        }
        UUID ownerId = resolveOwnerId(gameEngine, sourceCard);
        if (ownerId == null) {
            return;
        }

        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = ownerId.equals(gameEngine.getPlayer1().getUuid());
        Player allyPlayer = isPlayer1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();
        Player enemyPlayer = isPlayer1 ? gameEngine.getPlayer2() : gameEngine.getPlayer1();
        UUID activeAllyId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();
        UUID activeEnemyId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                : gameEngine.getGameState().getActiveHeroPlayer1Id();

        Map<UUID, HeroCard> autoTargets = new LinkedHashMap<>();
        List<CounterTargetScope> interactiveScopes = new ArrayList<>();

        for (CounterTargetScope scope : targetScopes) {
            if (scope.isInteractiveChoice()) {
                interactiveScopes.add(scope);
            } else {
                collectTargetsForScope(scope, sourceCard, allyPlayer, enemyPlayer, activeAllyId, activeEnemyId, autoTargets);
            }
        }

        for (HeroCard target : autoTargets.values()) {
            applyCounterChange(gameEngine, target);
        }

        if (!interactiveScopes.isEmpty()) {
            processInteractiveScopes(
                    gameEngine,
                    sourceCard,
                    allyPlayer,
                    enemyPlayer,
                    activeAllyId,
                    activeEnemyId,
                    interactiveScopes,
                    0
            );
        }
    }

    private void processInteractiveScopes(
            GameEngine gameEngine,
            Card sourceCard,
            Player allyPlayer,
            Player enemyPlayer,
            UUID activeAllyId,
            UUID activeEnemyId,
            List<CounterTargetScope> interactiveScopes,
            int index) {
        if (index >= interactiveScopes.size()) {
            return;
        }

        CounterTargetScope scope = interactiveScopes.get(index);
        List<HeroCard> candidates = resolveChoiceCandidates(scope, allyPlayer, enemyPlayer, activeAllyId, activeEnemyId);
        List<UUID> candidateUuids = new ArrayList<>();
        for (HeroCard hero : candidates) {
            if (hero != null && hero.getUuid() != null) {
                candidateUuids.add(hero.getUuid());
            }
        }

        if (candidateUuids.isEmpty()) {
            processInteractiveScopes(gameEngine, sourceCard, allyPlayer, enemyPlayer, activeAllyId, activeEnemyId, interactiveScopes, index + 1);
            return;
        }

        gameEngine.requestPlayerChoice(
                allyPlayer.getUuid(),
                sourceCard,
                resolveChoiceTargetType(scope),
                buildChoicePromptMessage(scope),
                candidateUuids,
                canSkipChoice,
                chosenHeroUuid -> {
                    if (chosenHeroUuid != null) {
                        HeroCard chosenHero = gameEngine.findHeroById(chosenHeroUuid);
                        if (chosenHero != null && Boolean.TRUE.equals(chosenHero.getEstaVivo())) {
                            applyCounterChange(gameEngine, chosenHero);
                        }
                    }
                    processInteractiveScopes(
                            gameEngine,
                            sourceCard,
                            allyPlayer,
                            enemyPlayer,
                            activeAllyId,
                            activeEnemyId,
                            interactiveScopes,
                            index + 1
                    );
                }
        );
    }

    private void applyCounterChange(GameEngine gameEngine, HeroCard target) {
        if (target == null || !Boolean.TRUE.equals(target.getEstaVivo())) {
            return;
        }
        if (delta > 0) {
            gameEngine.addCounter(target, counterType, delta);
        } else {
            gameEngine.removeCounter(target, counterType, -delta);
        }
    }

    private UUID resolveOwnerId(GameEngine engine, Card sourceCard) {
        if (sourceCard.getOwnerId() != null) {
            return sourceCard.getOwnerId();
        }
        if (sourceCard instanceof HeroCard hero) {
            if (findHeroInPlayer(engine.getPlayer1(), hero.getUuid()) != null) {
                return engine.getPlayer1().getUuid();
            }
            if (findHeroInPlayer(engine.getPlayer2(), hero.getUuid()) != null) {
                return engine.getPlayer2().getUuid();
            }
        }
        return null;
    }

    private void collectTargetsForScope(
            CounterTargetScope scope,
            Card sourceCard,
            Player allyPlayer,
            Player enemyPlayer,
            UUID activeAllyId,
            UUID activeEnemyId,
            Map<UUID, HeroCard> out) {
        switch (scope) {
            case SELF_OR_ATTACHED_HERO -> {
                if (sourceCard instanceof HeroCard selfHero) {
                    addIfLiving(selfHero, out);
                } else if (sourceCard.getAttachedToCardId() != null) {
                    HeroCard host = findHeroInPlayer(allyPlayer, sourceCard.getAttachedToCardId());
                    if (host == null) {
                        host = findHeroInPlayer(enemyPlayer, sourceCard.getAttachedToCardId());
                    }
                    addIfLiving(host, out);
                } else {
                    addIfLiving(resolveActiveHero(allyPlayer, activeAllyId), out);
                }
            }
            case ACTIVE_ALLY_HERO -> addIfLiving(resolveActiveHero(allyPlayer, activeAllyId), out);
            case ACTIVE_ENEMY_HERO -> addIfLiving(resolveActiveHero(enemyPlayer, activeEnemyId), out);
            case BOTH_ACTIVE_HEROES -> {
                addIfLiving(resolveActiveHero(allyPlayer, activeAllyId), out);
                addIfLiving(resolveActiveHero(enemyPlayer, activeEnemyId), out);
            }
            case OUT_OF_COMBAT_ALLY_HEROES -> addOutOfCombatHeroes(allyPlayer, activeAllyId, out);
            case OUT_OF_COMBAT_ENEMY_HEROES -> addOutOfCombatHeroes(enemyPlayer, activeEnemyId, out);
            case ALL_OUT_OF_COMBAT_HEROES -> {
                addOutOfCombatHeroes(allyPlayer, activeAllyId, out);
                addOutOfCombatHeroes(enemyPlayer, activeEnemyId, out);
            }
            case ALL_ALLY_HEROES -> addLivingHeroes(allyPlayer, out);
            case ALL_ENEMY_HEROES -> addLivingHeroes(enemyPlayer, out);
            case ALL_LIVING_HEROES -> {
                addLivingHeroes(allyPlayer, out);
                addLivingHeroes(enemyPlayer, out);
            }
            default -> {
                // Escopos interativos são tratados por processInteractiveScopes
            }
        }
    }

    private List<HeroCard> resolveChoiceCandidates(
            CounterTargetScope scope,
            Player allyPlayer,
            Player enemyPlayer,
            UUID activeAllyId,
            UUID activeEnemyId) {
        Map<UUID, HeroCard> candidates = new LinkedHashMap<>();
        switch (scope) {
            case CHOOSE_ALLY_HERO -> addLivingHeroes(allyPlayer, candidates);
            case CHOOSE_ENEMY_HERO -> addLivingHeroes(enemyPlayer, candidates);
            case CHOOSE_ANY_HERO -> {
                addLivingHeroes(allyPlayer, candidates);
                addLivingHeroes(enemyPlayer, candidates);
            }
            case CHOOSE_OUT_OF_COMBAT_ALLY_HERO -> addOutOfCombatHeroes(allyPlayer, activeAllyId, candidates);
            case CHOOSE_OUT_OF_COMBAT_ENEMY_HERO -> addOutOfCombatHeroes(enemyPlayer, activeEnemyId, candidates);
            case CHOOSE_OUT_OF_COMBAT_ANY_HERO -> {
                addOutOfCombatHeroes(allyPlayer, activeAllyId, candidates);
                addOutOfCombatHeroes(enemyPlayer, activeEnemyId, candidates);
            }
            default -> {
            }
        }
        return new ArrayList<>(candidates.values());
    }

    private String resolveChoiceTargetType(CounterTargetScope scope) {
        return switch (scope) {
            case CHOOSE_ALLY_HERO, CHOOSE_OUT_OF_COMBAT_ALLY_HERO -> "ALLY_HERO";
            case CHOOSE_ENEMY_HERO, CHOOSE_OUT_OF_COMBAT_ENEMY_HERO -> "ENEMY_HERO";
            default -> "HERO";
        };
    }

    private String buildChoicePromptMessage(CounterTargetScope scope) {
        String action = delta > 0
                ? ("aplicar +" + delta)
                : ("remover " + (-delta));
        String targetDesc = switch (scope) {
            case CHOOSE_ALLY_HERO -> "um Herói aliado";
            case CHOOSE_ENEMY_HERO -> "um Herói inimigo";
            case CHOOSE_OUT_OF_COMBAT_ALLY_HERO -> "um Herói aliado fora de combate";
            case CHOOSE_OUT_OF_COMBAT_ENEMY_HERO -> "um Herói inimigo fora de combate";
            case CHOOSE_OUT_OF_COMBAT_ANY_HERO -> "um Herói fora de combate";
            default -> "um Herói";
        };
        return "Escolha " + targetDesc + " para " + action + " marcador(es) de " + counterType.getDisplayName();
    }

    private HeroCard resolveActiveHero(Player player, UUID activeHeroId) {
        HeroCard byId = findHeroInPlayer(player, activeHeroId);
        if (byId != null) {
            return byId;
        }
        if (activeHeroId == null && player != null && player.getCurrentDeck() != null && player.getCurrentDeck().getHeroList() != null) {
            for (HeroCard hero : player.getCurrentDeck().getHeroList()) {
                if (hero != null && Boolean.TRUE.equals(hero.getEstaVivo()) && hero.getZoneId() == ZoneId.BATTLE) {
                    return hero;
                }
            }
        }
        return null;
    }

    private HeroCard findHeroInPlayer(Player player, UUID heroId) {
        if (player == null || heroId == null || player.getCurrentDeck() == null || player.getCurrentDeck().getHeroList() == null) {
            return null;
        }
        for (HeroCard hero : player.getCurrentDeck().getHeroList()) {
            if (hero != null && heroId.equals(hero.getUuid())) {
                return hero;
            }
        }
        return null;
    }

    private boolean isHeroInCombat(HeroCard hero, UUID activeHeroId) {
        if (hero == null) return false;
        return hero.getZoneId() == ZoneId.BATTLE
                || (activeHeroId != null && activeHeroId.equals(hero.getUuid()));
    }

    private void addOutOfCombatHeroes(Player player, UUID activeHeroId, Map<UUID, HeroCard> out) {
        if (player == null || player.getCurrentDeck() == null || player.getCurrentDeck().getHeroList() == null) {
            return;
        }
        for (HeroCard hero : player.getCurrentDeck().getHeroList()) {
            if (hero != null && Boolean.TRUE.equals(hero.getEstaVivo()) && !isHeroInCombat(hero, activeHeroId)) {
                addIfLiving(hero, out);
            }
        }
    }

    private void addLivingHeroes(Player player, Map<UUID, HeroCard> out) {
        if (player == null || player.getCurrentDeck() == null || player.getCurrentDeck().getHeroList() == null) {
            return;
        }
        for (HeroCard hero : player.getCurrentDeck().getHeroList()) {
            addIfLiving(hero, out);
        }
    }

    private void addIfLiving(HeroCard hero, Map<UUID, HeroCard> out) {
        if (hero != null && hero.getUuid() != null && Boolean.TRUE.equals(hero.getEstaVivo())) {
            out.putIfAbsent(hero.getUuid(), hero);
        }
    }
}

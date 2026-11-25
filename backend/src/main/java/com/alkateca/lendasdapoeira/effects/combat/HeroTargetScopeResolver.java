package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Resolvedor compartilhado de escopos de Heróis alvo (ativos, ambos simultâneos, fora de combate
 * e escolhas interativas via {@code PendingChoice}) reutilizado por efeitos baseados em contadores.
 */
public final class HeroTargetScopeResolver {

    private HeroTargetScopeResolver() {
    }

    public static void resolveHeroTargets(
            GameEngine gameEngine,
            Card sourceCard,
            List<ModifyHeroCounterEffect.CounterTargetScope> scopes,
            boolean canSkipChoice,
            Function<ModifyHeroCounterEffect.CounterTargetScope, String> promptBuilder,
            Consumer<HeroCard> perHeroAction,
            Runnable onAllScopesFinished) {
        if (gameEngine == null || sourceCard == null || scopes == null || scopes.isEmpty() || perHeroAction == null) {
            if (onAllScopesFinished != null) {
                onAllScopesFinished.run();
            }
            return;
        }
        if (gameEngine.getPlayer1() == null || gameEngine.getPlayer2() == null || gameEngine.getGameState() == null) {
            return;
        }

        UUID ownerId = resolveOwnerId(gameEngine, sourceCard);
        if (ownerId == null) {
            return;
        }

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
        List<ModifyHeroCounterEffect.CounterTargetScope> interactiveScopes = new ArrayList<>();

        for (ModifyHeroCounterEffect.CounterTargetScope scope : scopes) {
            if (scope == null) {
                continue;
            }
            if (scope.isInteractiveChoice()) {
                interactiveScopes.add(scope);
            } else {
                collectTargetsForScope(scope, sourceCard, allyPlayer, enemyPlayer, activeAllyId, activeEnemyId, autoTargets);
            }
        }

        for (HeroCard target : autoTargets.values()) {
            perHeroAction.accept(target);
        }

        if (interactiveScopes.isEmpty()) {
            if (onAllScopesFinished != null) {
                onAllScopesFinished.run();
            }
            return;
        }

        processInteractiveScopes(
                gameEngine,
                sourceCard,
                allyPlayer,
                enemyPlayer,
                activeAllyId,
                activeEnemyId,
                interactiveScopes,
                0,
                canSkipChoice,
                promptBuilder,
                perHeroAction,
                onAllScopesFinished
        );
    }

    public static List<HeroCard> collectImmediateTargets(
            GameEngine gameEngine,
            Card sourceCard,
            ModifyHeroCounterEffect.CounterTargetScope scope) {
        if (gameEngine == null || sourceCard == null || scope == null || scope.isInteractiveChoice()) {
            return List.of();
        }
        UUID ownerId = resolveOwnerId(gameEngine, sourceCard);
        if (ownerId == null || gameEngine.getPlayer1() == null || gameEngine.getPlayer2() == null || gameEngine.getGameState() == null) {
            return List.of();
        }

        boolean isPlayer1 = ownerId.equals(gameEngine.getPlayer1().getUuid());
        Player allyPlayer = isPlayer1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();
        Player enemyPlayer = isPlayer1 ? gameEngine.getPlayer2() : gameEngine.getPlayer1();
        UUID activeAllyId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();
        UUID activeEnemyId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                : gameEngine.getGameState().getActiveHeroPlayer1Id();

        Map<UUID, HeroCard> targets = new LinkedHashMap<>();
        collectTargetsForScope(scope, sourceCard, allyPlayer, enemyPlayer, activeAllyId, activeEnemyId, targets);
        return new ArrayList<>(targets.values());
    }

    public static UUID resolveOwnerId(GameEngine engine, Card sourceCard) {
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

    private static void processInteractiveScopes(
            GameEngine gameEngine,
            Card sourceCard,
            Player allyPlayer,
            Player enemyPlayer,
            UUID activeAllyId,
            UUID activeEnemyId,
            List<ModifyHeroCounterEffect.CounterTargetScope> interactiveScopes,
            int index,
            boolean canSkipChoice,
            Function<ModifyHeroCounterEffect.CounterTargetScope, String> promptBuilder,
            Consumer<HeroCard> perHeroAction,
            Runnable onAllScopesFinished) {
        if (index >= interactiveScopes.size()) {
            if (onAllScopesFinished != null) {
                onAllScopesFinished.run();
            }
            return;
        }

        ModifyHeroCounterEffect.CounterTargetScope scope = interactiveScopes.get(index);
        List<HeroCard> candidates = resolveChoiceCandidates(scope, allyPlayer, enemyPlayer, activeAllyId, activeEnemyId);
        List<UUID> candidateUuids = new ArrayList<>();
        for (HeroCard hero : candidates) {
            if (hero != null && hero.getUuid() != null) {
                candidateUuids.add(hero.getUuid());
            }
        }

        if (candidateUuids.isEmpty()) {
            processInteractiveScopes(
                    gameEngine, sourceCard, allyPlayer, enemyPlayer, activeAllyId, activeEnemyId,
                    interactiveScopes, index + 1, canSkipChoice, promptBuilder, perHeroAction, onAllScopesFinished
            );
            return;
        }

        String prompt = promptBuilder != null
                ? promptBuilder.apply(scope)
                : defaultPromptMessage(scope);

        gameEngine.requestPlayerChoice(
                allyPlayer.getUuid(),
                sourceCard,
                resolveChoiceTargetType(scope),
                prompt,
                candidateUuids,
                canSkipChoice,
                chosenHeroUuid -> {
                    if (chosenHeroUuid != null) {
                        HeroCard chosenHero = gameEngine.findHeroById(chosenHeroUuid);
                        if (chosenHero != null && Boolean.TRUE.equals(chosenHero.getEstaVivo())) {
                            perHeroAction.accept(chosenHero);
                        }
                    }
                    processInteractiveScopes(
                            gameEngine, sourceCard, allyPlayer, enemyPlayer, activeAllyId, activeEnemyId,
                            interactiveScopes, index + 1, canSkipChoice, promptBuilder, perHeroAction, onAllScopesFinished
                    );
                }
        );
    }

    public static void collectTargetsForScope(
            ModifyHeroCounterEffect.CounterTargetScope scope,
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

    private static List<HeroCard> resolveChoiceCandidates(
            ModifyHeroCounterEffect.CounterTargetScope scope,
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

    public static String resolveChoiceTargetType(ModifyHeroCounterEffect.CounterTargetScope scope) {
        return switch (scope) {
            case CHOOSE_ALLY_HERO, CHOOSE_OUT_OF_COMBAT_ALLY_HERO -> "ALLY_HERO";
            case CHOOSE_ENEMY_HERO, CHOOSE_OUT_OF_COMBAT_ENEMY_HERO -> "ENEMY_HERO";
            default -> "HERO";
        };
    }

    public static String describeScopeTarget(ModifyHeroCounterEffect.CounterTargetScope scope) {
        return switch (scope) {
            case CHOOSE_ALLY_HERO -> "um Herói aliado";
            case CHOOSE_ENEMY_HERO -> "um Herói inimigo";
            case CHOOSE_OUT_OF_COMBAT_ALLY_HERO -> "um Herói aliado fora de combate";
            case CHOOSE_OUT_OF_COMBAT_ENEMY_HERO -> "um Herói inimigo fora de combate";
            case CHOOSE_OUT_OF_COMBAT_ANY_HERO -> "um Herói fora de combate";
            default -> "um Herói";
        };
    }

    private static String defaultPromptMessage(ModifyHeroCounterEffect.CounterTargetScope scope) {
        return "Escolha " + describeScopeTarget(scope);
    }

    private static HeroCard resolveActiveHero(Player player, UUID activeHeroId) {
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

    private static HeroCard findHeroInPlayer(Player player, UUID heroId) {
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

    private static boolean isHeroInCombat(HeroCard hero, UUID activeHeroId) {
        if (hero == null) return false;
        return hero.getZoneId() == ZoneId.BATTLE
                || (activeHeroId != null && activeHeroId.equals(hero.getUuid()));
    }

    private static void addOutOfCombatHeroes(Player player, UUID activeHeroId, Map<UUID, HeroCard> out) {
        if (player == null || player.getCurrentDeck() == null || player.getCurrentDeck().getHeroList() == null) {
            return;
        }
        for (HeroCard hero : player.getCurrentDeck().getHeroList()) {
            if (hero != null && Boolean.TRUE.equals(hero.getEstaVivo()) && !isHeroInCombat(hero, activeHeroId)) {
                addIfLiving(hero, out);
            }
        }
    }

    private static void addLivingHeroes(Player player, Map<UUID, HeroCard> out) {
        if (player == null || player.getCurrentDeck() == null || player.getCurrentDeck().getHeroList() == null) {
            return;
        }
        for (HeroCard hero : player.getCurrentDeck().getHeroList()) {
            addIfLiving(hero, out);
        }
    }

    private static void addIfLiving(HeroCard hero, Map<UUID, HeroCard> out) {
        if (hero != null && hero.getUuid() != null && Boolean.TRUE.equals(hero.getEstaVivo())) {
            out.putIfAbsent(hero.getUuid(), hero);
        }
    }
}

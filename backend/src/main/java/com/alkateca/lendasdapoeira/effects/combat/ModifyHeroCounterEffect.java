package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.CounterType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
    private final boolean triggerOnResolutionPhaseEntry;

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
                canSkipChoice,
                true
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
                false,
                true
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
                canSkipChoice,
                true
        );
    }

    public ModifyHeroCounterEffect(
            Collection<TurnPhase> triggerPhases,
            Collection<CounterTargetScope> targetScopes,
            CounterType counterType,
            int delta,
            boolean triggerFromBench,
            boolean canSkipChoice) {
        this(triggerPhases, targetScopes, counterType, delta, triggerFromBench, canSkipChoice, true);
    }

    public ModifyHeroCounterEffect(
            Collection<TurnPhase> triggerPhases,
            Collection<CounterTargetScope> targetScopes,
            CounterType counterType,
            int delta,
            boolean triggerFromBench,
            boolean canSkipChoice,
            boolean triggerOnResolutionPhaseEntry) {
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
        this.triggerOnResolutionPhaseEntry = triggerOnResolutionPhaseEntry;
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
    public boolean canTriggerOnResolutionPhaseEntry() {
        return triggerOnResolutionPhaseEntry && triggerPhases.contains(TurnPhase.RESOLUTION);
    }

    @Override
    public boolean canTriggerFromResolveZone(TurnPhase turnPhase) {
        return turnPhase != null
                && turnPhase != TurnPhase.RESOLUTION
                && triggerPhases.contains(turnPhase);
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

        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");
        HeroTargetScopeResolver.resolveHeroTargets(
                gameEngine,
                sourceCard,
                targetScopes,
                canSkipChoice,
                this::buildChoicePromptMessage,
                target -> applyCounterChange(gameEngine, target),
                null
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

    private String buildChoicePromptMessage(CounterTargetScope scope) {
        String action = delta > 0
                ? ("aplicar +" + delta)
                : ("remover " + (-delta));
        return "Escolha " + HeroTargetScopeResolver.describeScopeTarget(scope)
                + " para " + action + " marcador(es) de " + counterType.getDisplayName();
    }
}

package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.counters.CounterConsumptionMode;
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
 * Efeito genérico e desacoplado para cartas e Heróis que consomem contadores
 * (como {@link CounterType#SEDA} ou futuros tokens) em qualquer {@link TurnPhase}
 * para ampliar seus efeitos (bônus de Ataque, Defesa, Espírito, Dano Bônus,
 * Redução de Dano, Vulnerabilidade, Cura ou Dano Mágico/Físico/Direto).
 */
public class ConsumeCounterAndApplyEffect implements Effect {

    @FunctionalInterface
    public interface CounterEffectAction {
        void apply(GameEngine gameEngine, Card sourceCard, HeroCard tokenSourceHero, int consumedCount);
    }

    public enum DamageKind {
        PHYSICAL,
        MAGIC,
        DIRECT
    }

    /**
     * Configura um modificador escalável por tokens consumidos (temporário até COMBAT_END ou permanente).
     */
    public record StatBonusSpec(
            int baseEspirito, int perTokenEspirito,
            int baseAtaque, int perTokenAtaque,
            int baseDefesa, int perTokenDefesa,
            int baseDanoBonus, int perTokenDanoBonus,
            int baseReducaoDano, int perTokenReducaoDano,
            int baseVulnerabilidade, int perTokenVulnerabilidade,
            boolean temporaryUntilCombatEnd,
            ModifyHeroCounterEffect.CounterTargetScope beneficiaryScope
    ) {
        public static StatBonusSpec ofStats(
                int baseEsp, int perTokenEsp,
                int baseAtk, int perTokenAtk,
                int baseDef, int perTokenDef,
                boolean temporaryUntilCombatEnd) {
            return new StatBonusSpec(
                    baseEsp, perTokenEsp,
                    baseAtk, perTokenAtk,
                    baseDef, perTokenDef,
                    0, 0,
                    0, 0,
                    0, 0,
                    temporaryUntilCombatEnd,
                    null
            );
        }

        public static StatBonusSpec ofStats(
                int baseEsp, int perTokenEsp,
                int baseAtk, int perTokenAtk,
                int baseDef, int perTokenDef,
                boolean temporaryUntilCombatEnd,
                ModifyHeroCounterEffect.CounterTargetScope beneficiaryScope) {
            return new StatBonusSpec(
                    baseEsp, perTokenEsp,
                    baseAtk, perTokenAtk,
                    baseDef, perTokenDef,
                    0, 0,
                    0, 0,
                    0, 0,
                    temporaryUntilCombatEnd,
                    beneficiaryScope
            );
        }
    }

    private final Set<TurnPhase> triggerPhases;
    private final List<ModifyHeroCounterEffect.CounterTargetScope> tokenSourceScopes;
    private final CounterType counterType;
    private final CounterConsumptionMode consumptionMode;
    private final int requestedAmount;
    private final boolean requireAtLeastOneConsumed;
    private final boolean triggerFromBench;
    private final boolean canSkipChoice;
    private final boolean triggerOnResolutionPhaseEntry;
    private final List<CounterEffectAction> actions;

    public ConsumeCounterAndApplyEffect(
            TurnPhase triggerPhase,
            ModifyHeroCounterEffect.CounterTargetScope tokenSourceScope,
            CounterType counterType,
            CounterConsumptionMode consumptionMode,
            int requestedAmount,
            boolean requireAtLeastOneConsumed,
            CounterEffectAction action) {
        this(
                List.of(triggerPhase != null ? triggerPhase : TurnPhase.RESOLUTION),
                List.of(tokenSourceScope != null ? tokenSourceScope : ModifyHeroCounterEffect.CounterTargetScope.ACTIVE_ALLY_HERO),
                counterType,
                consumptionMode,
                requestedAmount,
                requireAtLeastOneConsumed,
                isOutOfCombatTurnPhase(triggerPhase),
                false,
                true,
                action != null ? List.of(action) : List.of()
        );
    }

    public ConsumeCounterAndApplyEffect(
            Collection<TurnPhase> triggerPhases,
            Collection<ModifyHeroCounterEffect.CounterTargetScope> tokenSourceScopes,
            CounterType counterType,
            CounterConsumptionMode consumptionMode,
            int requestedAmount,
            boolean requireAtLeastOneConsumed,
            boolean triggerFromBench,
            boolean canSkipChoice,
            boolean triggerOnResolutionPhaseEntry,
            Collection<CounterEffectAction> actions) {
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

        List<ModifyHeroCounterEffect.CounterTargetScope> scopes = new ArrayList<>();
        if (tokenSourceScopes != null) {
            for (ModifyHeroCounterEffect.CounterTargetScope scope : tokenSourceScopes) {
                if (scope != null) {
                    scopes.add(scope);
                }
            }
        }
        if (scopes.isEmpty()) {
            scopes.add(ModifyHeroCounterEffect.CounterTargetScope.ACTIVE_ALLY_HERO);
        }
        this.tokenSourceScopes = Collections.unmodifiableList(scopes);
        this.counterType = counterType;
        this.consumptionMode = consumptionMode != null ? consumptionMode : CounterConsumptionMode.CONSUME_ALL;
        this.requestedAmount = Math.max(0, requestedAmount);
        this.requireAtLeastOneConsumed = requireAtLeastOneConsumed;
        this.triggerFromBench = triggerFromBench;
        this.canSkipChoice = canSkipChoice;
        this.triggerOnResolutionPhaseEntry = triggerOnResolutionPhaseEntry;
        this.actions = actions != null ? List.copyOf(actions) : List.of();
    }

    private static boolean isOutOfCombatTurnPhase(TurnPhase phase) {
        return phase == TurnPhase.GAME_START
                || phase == TurnPhase.CHOICE
                || phase == TurnPhase.DISCARD;
    }

    public static CounterEffectAction statBuffAction(StatBonusSpec spec) {
        return (engine, sourceCard, tokenSourceHero, consumedCount) -> {
            if (spec == null) return;
            int espDelta = spec.baseEspirito() + (consumedCount * spec.perTokenEspirito());
            int atkDelta = spec.baseAtaque() + (consumedCount * spec.perTokenAtaque());
            int defDelta = spec.baseDefesa() + (consumedCount * spec.perTokenDefesa());
            int dmgBonusDelta = spec.baseDanoBonus() + (consumedCount * spec.perTokenDanoBonus());
            int dmgRedDelta = spec.baseReducaoDano() + (consumedCount * spec.perTokenReducaoDano());
            int vulnDelta = spec.baseVulnerabilidade() + (consumedCount * spec.perTokenVulnerabilidade());

            if (espDelta == 0 && atkDelta == 0 && defDelta == 0
                    && dmgBonusDelta == 0 && dmgRedDelta == 0 && vulnDelta == 0) {
                return;
            }

            List<HeroCard> beneficiaries = spec.beneficiaryScope() == null
                    ? (tokenSourceHero != null ? List.of(tokenSourceHero) : List.of())
                    : HeroTargetScopeResolver.collectImmediateTargets(engine, sourceCard, spec.beneficiaryScope());

            for (HeroCard beneficiary : beneficiaries) {
                if (beneficiary == null || !Boolean.TRUE.equals(beneficiary.getEstaVivo())) {
                    continue;
                }
                applyStatDeltas(beneficiary, espDelta, atkDelta, defDelta, dmgBonusDelta, dmgRedDelta, vulnDelta);
                if (spec.temporaryUntilCombatEnd()) {
                    engine.registerCombatEndCleanup(() ->
                            applyStatDeltas(beneficiary, -espDelta, -atkDelta, -defDelta, -dmgBonusDelta, -dmgRedDelta, -vulnDelta)
                    );
                }
                System.out.println("-> " + beneficiary.getCardName() + " ampliado por tokens (ESP: " + espDelta
                        + ", ATQ: " + atkDelta + ", DEF: " + defDelta
                        + (spec.temporaryUntilCombatEnd() ? " até COMBAT_END" : " permanente") + ").");
            }
        };
    }

    public static CounterEffectAction healAction(
            int baseHeal,
            int perTokenHeal,
            ModifyHeroCounterEffect.CounterTargetScope beneficiaryScope) {
        return (engine, sourceCard, tokenSourceHero, consumedCount) -> {
            int totalHeal = Math.max(0, baseHeal + (consumedCount * perTokenHeal));
            if (totalHeal <= 0) return;

            List<HeroCard> beneficiaries = beneficiaryScope == null
                    ? (tokenSourceHero != null ? List.of(tokenSourceHero) : List.of())
                    : HeroTargetScopeResolver.collectImmediateTargets(engine, sourceCard, beneficiaryScope);

            for (HeroCard beneficiary : beneficiaries) {
                if (beneficiary == null || !Boolean.TRUE.equals(beneficiary.getEstaVivo())) {
                    continue;
                }
                int oldHp = beneficiary.getVidaAtual();
                int newHp = Math.min(beneficiary.getVidaMaxima(), oldHp + totalHeal);
                beneficiary.setVidaAtual(newHp);
                System.out.println("-> " + beneficiary.getCardName() + " recuperou " + (newHp - oldHp)
                        + " de Vida por consumo de tokens! Vida restante: " + beneficiary.getVidaAtual());
            }
        };
    }

    public static CounterEffectAction damageAction(
            DamageKind damageKind,
            int baseDamage,
            int perTokenDamage,
            boolean addAttackerSpirit,
            ModifyHeroCounterEffect.CounterTargetScope targetScope) {
        return (engine, sourceCard, tokenSourceHero, consumedCount) -> {
            if (damageKind == null) return;
            HeroCard attacker = tokenSourceHero;
            if (attacker == null) {
                List<HeroCard> allyActive = HeroTargetScopeResolver.collectImmediateTargets(
                        engine, sourceCard, ModifyHeroCounterEffect.CounterTargetScope.ACTIVE_ALLY_HERO);
                attacker = allyActive.isEmpty() ? null : allyActive.get(0);
            }
            int spirit = (addAttackerSpirit && attacker != null) ? attacker.getEspirito() : 0;
            int rawDamage = Math.max(0, baseDamage + (consumedCount * perTokenDamage) + spirit);

            ModifyHeroCounterEffect.CounterTargetScope resolvedScope = targetScope != null
                    ? targetScope
                    : ModifyHeroCounterEffect.CounterTargetScope.ACTIVE_ENEMY_HERO;
            List<HeroCard> defenders = HeroTargetScopeResolver.collectImmediateTargets(engine, sourceCard, resolvedScope);

            for (HeroCard defender : defenders) {
                if (defender == null || !Boolean.TRUE.equals(defender.getEstaVivo())) {
                    continue;
                }
                int finalDamage = switch (damageKind) {
                    case PHYSICAL -> engine.calculatePhysicalDamage(attacker, defender, rawDamage, sourceCard);
                    case MAGIC -> engine.calculateMagicDamage(attacker, defender, rawDamage, sourceCard);
                    case DIRECT -> engine.calculateDirectDamage(attacker, defender, rawDamage, sourceCard);
                };
                defender.setVidaAtual(defender.getVidaAtual() - finalDamage);
                System.out.println("-> " + defender.getCardName() + " sofreu " + finalDamage
                        + " de dano (" + damageKind.name() + ") ampliado por tokens! Vida restante: " + defender.getVidaAtual());
            }
        };
    }

    private static void applyStatDeltas(
            HeroCard hero,
            int espDelta,
            int atkDelta,
            int defDelta,
            int dmgBonusDelta,
            int dmgRedDelta,
            int vulnDelta) {
        if (espDelta != 0) hero.setEspirito(hero.getEspirito() + espDelta);
        if (atkDelta != 0) hero.setAtaque(hero.getAtaque() + atkDelta);
        if (defDelta != 0) hero.setDefesa(hero.getDefesa() + defDelta);
        if (dmgBonusDelta != 0) hero.setDanoBonus(hero.getDanoBonus() + dmgBonusDelta);
        if (dmgRedDelta != 0) hero.setReducaoDano(hero.getReducaoDano() + dmgRedDelta);
        if (vulnDelta != 0) hero.setVulnerabilidade(hero.getVulnerabilidade() + vulnDelta);
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
        if (gameEngine == null || sourceCard == null || counterType == null || actions.isEmpty()) {
            return;
        }
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");
        HeroTargetScopeResolver.resolveHeroTargets(
                gameEngine,
                sourceCard,
                tokenSourceScopes,
                canSkipChoice,
                this::buildChoicePromptMessage,
                tokenSourceHero -> {
                    if (tokenSourceHero == null || !Boolean.TRUE.equals(tokenSourceHero.getEstaVivo())) {
                        return;
                    }
                    int consumed = gameEngine.consumeCounters(
                            tokenSourceHero, counterType, consumptionMode, requestedAmount);
                    if (requireAtLeastOneConsumed && consumed <= 0) {
                        return;
                    }
                    for (CounterEffectAction action : actions) {
                        action.apply(gameEngine, sourceCard, tokenSourceHero, consumed);
                    }
                },
                null
        );
    }

    private String buildChoicePromptMessage(ModifyHeroCounterEffect.CounterTargetScope scope) {
        return "Escolha " + HeroTargetScopeResolver.describeScopeTarget(scope)
                + " para consumir marcador(es) de " + counterType.getDisplayName();
    }
}

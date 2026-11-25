package com.alkateca.lendasdapoeira.counters;

import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.CounterType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Registro central de regras por {@link CounterType}, permitindo estender novos tokens
 * seguindo o princípio Aberto/Fechado sem acoplar suas regras diretamente ao {@link GameEngine}.
 */
public class CounterRuleRegistry {

    private final Map<CounterType, CounterRule> rules = new EnumMap<>(CounterType.class);

    public CounterRuleRegistry() {
    }

    public static CounterRuleRegistry createDefault() {
        CounterRuleRegistry registry = new CounterRuleRegistry();
        registry.register(new CombustaoCounterRule());
        registry.register(new SedaCounterRule());
        return registry;
    }

    public void register(CounterRule rule) {
        if (rule != null && rule.getCounterType() != null) {
            rules.put(rule.getCounterType(), rule);
        }
    }

    public CounterRule getRule(CounterType type) {
        return type != null ? rules.get(type) : null;
    }

    public Collection<CounterRule> getRegisteredRules() {
        return Collections.unmodifiableCollection(rules.values());
    }

    public int resolveMagicDamageBonus(GameEngine engine, HeroCard attacker, HeroCard defender, int baseDamage, Card sourceCard) {
        int totalBonus = 0;
        for (CounterRule rule : rules.values()) {
            totalBonus += rule.modifyMagicDamage(engine, attacker, defender, baseDamage, sourceCard);
        }
        return totalBonus;
    }

    public int resolvePhysicalDamageBonus(GameEngine engine, HeroCard attacker, HeroCard defender, int baseDamage, Card sourceCard) {
        int totalBonus = 0;
        for (CounterRule rule : rules.values()) {
            totalBonus += rule.modifyPhysicalDamage(engine, attacker, defender, baseDamage, sourceCard);
        }
        return totalBonus;
    }

    public int resolveDirectDamageBonus(GameEngine engine, HeroCard attacker, HeroCard defender, int baseDamage, Card sourceCard) {
        int totalBonus = 0;
        for (CounterRule rule : rules.values()) {
            totalBonus += rule.modifyDirectDamage(engine, attacker, defender, baseDamage, sourceCard);
        }
        return totalBonus;
    }

    public void onTurnPhase(GameEngine engine, TurnPhase phase) {
        if (engine == null || phase == null) {
            return;
        }
        for (CounterRule rule : rules.values()) {
            rule.onTurnPhase(engine, phase);
        }
    }
}

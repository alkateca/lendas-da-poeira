package com.alkateca.lendasdapoeira.counters;

import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.CounterType;

/**
 * Regra do marcador {@link CounterType#COMBUSTAO}:
 * soma o total acumulado ao dano bruto de Magias de Fogo contra o portador e zera os marcadores.
 */
public class CombustaoCounterRule implements CounterRule {

    @Override
    public CounterType getCounterType() {
        return CounterType.COMBUSTAO;
    }

    @Override
    public int modifyMagicDamage(GameEngine engine, HeroCard attacker, HeroCard defender, int baseDamage, Card sourceCard) {
        if (engine == null || defender == null || !engine.isFireSpell(sourceCard)) {
            return 0;
        }
        int combustionValue = engine.consumeAllCounters(defender, CounterType.COMBUSTAO);
        if (combustionValue > 0) {
            System.out.println("-> Combustão detonada por magia de fogo (" + sourceCard.getCardName()
                    + "): +" + combustionValue + " de dano mágico e marcadores zerados em " + defender.getCardName() + "!");
        }
        return combustionValue;
    }
}

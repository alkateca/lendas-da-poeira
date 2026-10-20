package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class ConvertAttacksToMagicDamageEffect implements Effect {

    public ConvertAttacksToMagicDamageEffect() {
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return false;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito passivo de " + sourceCard.getCardName() + " ativo: Ataques causam Dano Mágico em vez de Físico.");
    }
}

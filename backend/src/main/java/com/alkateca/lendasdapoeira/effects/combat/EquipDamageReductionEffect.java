package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

public class EquipDamageReductionEffect implements Effect {

    private final int damageReduction;

    public EquipDamageReductionEffect(int damageReduction) {
        this.damageReduction = damageReduction;
    }

    public int getDamageReduction() {
        return damageReduction;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId() != null
                && sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        java.util.UUID targetHeroId = sourceCard.getAttachedToCardId();
        if (targetHeroId == null) {
            targetHeroId = isPlayer1
                    ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                    : gameEngine.getGameState().getActiveHeroPlayer2Id();
        }

        com.alkateca.lendasdapoeira.entity.Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        final java.util.UUID finalTargetHeroId = targetHeroId;
        HeroCard targetHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(finalTargetHeroId))
                .findFirst()
                .orElse(null);

        if (targetHero != null) {
            gameEngine.replaceExistingSingleSlotItemIfNecessary(targetHero.getUuid(), sourceCard);
            sourceCard.setAttachedToCardId(targetHero.getUuid());
            sourceCard.setZoneId(ZoneId.ATTACHED);
            targetHero.setReducaoDano(targetHero.getReducaoDano() + damageReduction);

            System.out.println("-> " + targetHero.getCardName() + " recebeu +" + damageReduction +
                    " de Redução de Dano! Vida restante: " + targetHero.getVidaAtual());
        }
    }

    @Override
    public void onDetach(GameEngine gameEngine, Card sourceCard, HeroCard targetHero) {
        if (targetHero != null) {
            targetHero.setReducaoDano(targetHero.getReducaoDano() - damageReduction);
            System.out.println("-> " + sourceCard.getCardName() + " desanexada de " + targetHero.getCardName() +
                    " (-" + damageReduction + " de Redução de Dano removido)!");
        }
    }
}

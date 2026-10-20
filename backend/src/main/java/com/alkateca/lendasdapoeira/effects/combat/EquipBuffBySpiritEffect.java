package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

public class EquipBuffBySpiritEffect implements Effect {

    private int appliedBuff = 0;

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");
        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        java.util.UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();
        com.alkateca.lendasdapoeira.entity.Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId)).findFirst().orElse(null);

        if (myHero != null) {
            sourceCard.setAttachedToCardId(myHero.getUuid());
            sourceCard.setZoneId(ZoneId.ATTACHED);

            this.appliedBuff = myHero.getEspirito();
            myHero.setAtaque(myHero.getAtaque() + this.appliedBuff);
            System.out.println("-> " + myHero.getCardName() + " recebeu +" + this.appliedBuff +
                    " de Ataque baseado no Espírito atual ao equipar " + sourceCard.getCardName() + "!");
        }
    }

    @Override
    public void onDetach(GameEngine gameEngine, Card sourceCard, HeroCard targetHero) {
        if (targetHero != null && this.appliedBuff != 0) {
            targetHero.setAtaque(targetHero.getAtaque() - this.appliedBuff);
            System.out.println("-> " + sourceCard.getCardName() + " foi desanexado de " + targetHero.getCardName() +
                    "! Bônus de Ataque removido (-" + this.appliedBuff + ")");
            this.appliedBuff = 0;
        }
    }
}

package com.alkateca.lendasdapoeira.effects.startOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.UUID;

public class StartOfCombatInvokeIfNotEquippedEffect implements Effect {

    private final String cardName;

    public StartOfCombatInvokeIfNotEquippedEffect(String cardName) {
        this.cardName = cardName;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_START;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();

        Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        if (myHero != null) {
            boolean alreadyEquipped = myPlayer.getCurrentDeck().getCardList().stream()
                    .anyMatch(c -> c.getZoneId() == ZoneId.ATTACHED
                            && myHero.getUuid().equals(c.getAttachedToCardId())
                            && cardName.equalsIgnoreCase(c.getCardName()));

            if (!alreadyEquipped) {
                System.out.println("-> " + myHero.getCardName() + " não possui " + cardName + " equipado. Invocando: " + cardName + "!");
                gameEngine.invokeCard(myPlayer, sourceCard, myHero, cardName);
            } else {
                System.out.println("-> " + myHero.getCardName() + " já possui " + cardName + " equipado.");
            }
        }
    }
}
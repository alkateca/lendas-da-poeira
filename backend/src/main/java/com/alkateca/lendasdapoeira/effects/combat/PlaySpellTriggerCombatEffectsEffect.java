package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlaySpellTriggerCombatEffectsEffect implements Effect {
    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        Player myPlayer = isPlayer1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();
        UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();

        if (myHeroId == null || myPlayer == null || myPlayer.getCurrentDeck() == null) {
            return;
        }

        List<Card> sourcesToTrigger = new ArrayList<>();

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        if (myHero != null) {
            sourcesToTrigger.add(myHero);
        }

        for (Card c : myPlayer.getCurrentDeck().getCardList()) {
            if (c.getZoneId() == ZoneId.ATTACHED && myHeroId.equals(c.getAttachedToCardId())) {
                sourcesToTrigger.add(c);
            }
        }

        for (Card card : sourcesToTrigger) {
            if (card.getEffects() != null) {
                for (Effect effect : card.getEffects()) {
                    if (effect.predicate(card, TurnPhase.COMBAT_START) || effect.predicate(card, TurnPhase.COMBAT_END)) {
                        effect.resolve(gameEngine, card);
                    }
                }
            }
        }
        System.out.println("-> Habilidades de Início e Final de combate ativadas prematuramente!");
    }
}

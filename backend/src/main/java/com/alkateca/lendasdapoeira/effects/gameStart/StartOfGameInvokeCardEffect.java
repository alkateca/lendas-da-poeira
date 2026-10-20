package com.alkateca.lendasdapoeira.effects.gameStart;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.UUID;

public class StartOfGameInvokeCardEffect implements Effect {

    private final String cardName;

    public StartOfGameInvokeCardEffect(String cardName) {
        this.cardName = cardName;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.GAME_START;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado no Início da Partida!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        Player myPlayer = isPlayer1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();

        HeroCard targetHero = null;
        if (sourceCard instanceof HeroCard) {
            targetHero = (HeroCard) sourceCard;
        } else if (sourceCard.getAttachedToCardId() != null) {
            targetHero = gameEngine.findHeroById(sourceCard.getAttachedToCardId());
        } else {
            UUID myHeroId = isPlayer1
                    ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                    : gameEngine.getGameState().getActiveHeroPlayer2Id();
            targetHero = gameEngine.findHeroById(myHeroId);
        }

        Card invoked = gameEngine.invokeCard(myPlayer, targetHero, cardName);
        if (invoked != null) {
            System.out.println("-> Invocou " + invoked.getCardName() + " no Início da Partida!");
        }
    }
}

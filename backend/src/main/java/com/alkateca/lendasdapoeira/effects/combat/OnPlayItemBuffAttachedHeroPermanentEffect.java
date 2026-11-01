package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.List;
import java.util.UUID;

public class OnPlayItemBuffAttachedHeroPermanentEffect implements Effect {

    private final int bonusEspirito;
    private final int bonusAtaque;
    private final int bonusDefesa;

    public OnPlayItemBuffAttachedHeroPermanentEffect(int bonusEspirito, int bonusAtaque, int bonusDefesa) {
        this.bonusEspirito = bonusEspirito;
        this.bonusAtaque = bonusAtaque;
        this.bonusDefesa = bonusDefesa;
    }

    public int getBonusEspirito() {
        return bonusEspirito;
    }

    public int getBonusAtaque() {
        return bonusAtaque;
    }

    public int getBonusDefesa() {
        return bonusDefesa;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (myPlayer == null) {
            return;
        }

        Card recentlyPlayedItem = findRecentlyResolvedItem(gameEngine, myPlayer.getUuid(), sourceCard, isPlayer1);
        if (recentlyPlayedItem == null || recentlyPlayedItem.getCardType() != CardType.ITEM) {
            return;
        }

        UUID targetHeroId = sourceCard.getAttachedToCardId();
        if (targetHeroId == null && gameEngine.getGameState() != null) {
            targetHeroId = isPlayer1
                    ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                    : gameEngine.getGameState().getActiveHeroPlayer2Id();
        }

        HeroCard attachedHero = gameEngine.findHeroById(targetHeroId);
        if (attachedHero != null && Boolean.TRUE.equals(attachedHero.getEstaVivo())) {
            attachedHero.setEspirito(attachedHero.getEspirito() + bonusEspirito);
            attachedHero.setAtaque(attachedHero.getAtaque() + bonusAtaque);
            attachedHero.setDefesa(attachedHero.getDefesa() + bonusDefesa);

            System.out.println("-> " + attachedHero.getCardName() + " recebeu permanentemente +"
                    + bonusEspirito + " ESP, +" + bonusAtaque + " ATQ e +" + bonusDefesa
                    + " DEF ao jogar " + recentlyPlayedItem.getCardName()
                    + "! Vida restante: " + attachedHero.getVidaAtual());
        }
    }

    private Card findRecentlyResolvedItem(GameEngine gameEngine, UUID ownerId, Card sourceCard, boolean isPlayer1) {
        List<Card> resolvedCards = gameEngine.getResolvedCardsInCurrentCombat();
        if (resolvedCards != null && !resolvedCards.isEmpty()) {
            for (int i = resolvedCards.size() - 1; i >= 0; i--) {
                Card c = resolvedCards.get(i);
                if (c != null && ownerId.equals(c.getOwnerId()) && !c.getUuid().equals(sourceCard.getUuid())) {
                    return c;
                }
            }
        }
        if (gameEngine.getGameState() != null) {
            List<Card> playedCards = isPlayer1
                    ? gameEngine.getGameState().getPlayer1PlayedCards()
                    : gameEngine.getGameState().getPlayer2PlayedCards();
            if (playedCards != null) {
                for (int i = playedCards.size() - 1; i >= 0; i--) {
                    Card c = playedCards.get(i);
                    if (c != null && !c.getUuid().equals(sourceCard.getUuid()) && c.getCardType() == CardType.ITEM) {
                        return c;
                    }
                }
            }
        }
        return null;
    }
}

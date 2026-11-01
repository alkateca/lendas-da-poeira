package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.List;
import java.util.UUID;

public class OnPlayItemTypeBuffEffect implements Effect {

    private final ItemType triggerItemType;
    private final int bonusEspirito;
    private final int bonusAtaque;
    private final int bonusDefesa;
    private final boolean temporary;

    public OnPlayItemTypeBuffEffect(ItemType triggerItemType, int bonusEspirito, int bonusAtaque, int bonusDefesa, boolean temporary) {
        this.triggerItemType = triggerItemType;
        this.bonusEspirito = bonusEspirito;
        this.bonusAtaque = bonusAtaque;
        this.bonusDefesa = bonusDefesa;
        this.temporary = temporary;
    }

    public ItemType getTriggerItemType() {
        return triggerItemType;
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

    public boolean isTemporary() {
        return temporary;
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

        Player ownerPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (ownerPlayer == null) {
            return;
        }

        Card recentlyResolvedCard = findRecentlyResolvedCard(gameEngine, ownerPlayer.getUuid(), sourceCard, isPlayer1);
        if (recentlyResolvedCard == null || recentlyResolvedCard.getItemType() != triggerItemType) {
            return;
        }

        UUID targetHeroId = sourceCard.getAttachedToCardId();
        if (targetHeroId == null && gameEngine.getGameState() != null) {
            targetHeroId = isPlayer1
                    ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                    : gameEngine.getGameState().getActiveHeroPlayer2Id();
        }

        HeroCard targetHero = gameEngine.findHeroById(targetHeroId);
        if (targetHero != null && Boolean.TRUE.equals(targetHero.getEstaVivo())) {
            targetHero.setEspirito(targetHero.getEspirito() + bonusEspirito);
            targetHero.setAtaque(targetHero.getAtaque() + bonusAtaque);
            targetHero.setDefesa(targetHero.getDefesa() + bonusDefesa);

            if (temporary) {
                gameEngine.registerCombatEndCleanup(() -> {
                    targetHero.setEspirito(targetHero.getEspirito() - bonusEspirito);
                    targetHero.setAtaque(targetHero.getAtaque() - bonusAtaque);
                    targetHero.setDefesa(targetHero.getDefesa() - bonusDefesa);
                });
            }

            System.out.println("-> " + targetHero.getCardName() + " recebeu +" + bonusEspirito + " ESP, +"
                    + bonusAtaque + " ATQ, +" + bonusDefesa + " DEF ao resolver " + recentlyResolvedCard.getCardName()
                    + "! Vida restante: " + targetHero.getVidaAtual());
        }
    }

    private Card findRecentlyResolvedCard(GameEngine gameEngine, UUID ownerId, Card sourceCard, boolean isPlayer1) {
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
                    if (c != null && !c.getUuid().equals(sourceCard.getUuid()) && c.getItemType() == triggerItemType) {
                        return c;
                    }
                }
            }
        }
        return null;
    }
}

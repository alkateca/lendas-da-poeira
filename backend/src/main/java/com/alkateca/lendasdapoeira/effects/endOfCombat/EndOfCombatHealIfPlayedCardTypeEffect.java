package com.alkateca.lendasdapoeira.effects.endOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class EndOfCombatHealIfPlayedCardTypeEffect implements Effect {

    private final CardType requiredCardType;
    private final int healAmount;

    public EndOfCombatHealIfPlayedCardTypeEffect(CardType requiredCardType, int healAmount) {
        this.requiredCardType = requiredCardType;
        this.healAmount = healAmount;
    }

    public CardType getRequiredCardType() {
        return requiredCardType;
    }

    public int getHealAmount() {
        return healAmount;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_END;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (myPlayer == null || myPlayer.getCurrentDeck() == null) {
            return;
        }

        UUID targetHeroId = sourceCard.getAttachedToCardId();
        if (targetHeroId == null && gameEngine.getGameState() != null) {
            targetHeroId = isPlayer1
                    ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                    : gameEngine.getGameState().getActiveHeroPlayer2Id();
        }

        final UUID finalHeroId = targetHeroId;
        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(finalHeroId))
                .findFirst()
                .orElse(null);

        if (myHero == null || !Boolean.TRUE.equals(myHero.getEstaVivo())) {
            return;
        }

        boolean playedRequiredType = hasPlayedCardTypeInCurrentCombat(gameEngine, myPlayer);

        if (playedRequiredType) {
            int oldHp = myHero.getVidaAtual();
            int newHp = Math.min(myHero.getVidaMaxima(), oldHp + healAmount);
            int recovered = Math.max(0, newHp - oldHp);
            myHero.setVidaAtual(newHp);

            System.out.println("-> " + myHero.getCardName() + " recuperou " + recovered
                    + " pontos de vida (jogou carta do tipo " + requiredCardType + ")! Vida restante: "
                    + myHero.getVidaAtual());
        } else {
            System.out.println("-> Nenhuma carta do tipo " + requiredCardType
                    + " foi jogada neste combate. Nenhuma cura aplicada.");
        }
    }

    private boolean hasPlayedCardTypeInCurrentCombat(GameEngine gameEngine, Player myPlayer) {
        List<Card> resolvedPlayed = gameEngine.getResolvedPlayedCardsForPlayer(myPlayer.getUuid());
        if (resolvedPlayed != null && resolvedPlayed.stream().anyMatch(c -> c != null && c.getCardType() == requiredCardType)) {
            return true;
        }

        Set<UUID> playedIds = gameEngine.getPlayedCardIdsInCurrentTurn()
                .getOrDefault(myPlayer.getUuid(), Collections.emptySet());
        if (!playedIds.isEmpty() && myPlayer.getCurrentDeck().getCardList() != null) {
            return myPlayer.getCurrentDeck().getCardList().stream()
                    .anyMatch(c -> c != null && playedIds.contains(c.getUuid()) && c.getCardType() == requiredCardType);
        }

        return false;
    }
}

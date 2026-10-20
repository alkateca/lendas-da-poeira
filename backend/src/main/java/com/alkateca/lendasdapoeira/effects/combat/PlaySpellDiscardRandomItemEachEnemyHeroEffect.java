package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class PlaySpellDiscardRandomItemEachEnemyHeroEffect implements Effect {

    public PlaySpellDiscardRandomItemEachEnemyHeroEffect() {
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        Player enemyPlayer = isPlayer1
                ? gameEngine.getPlayer2()
                : gameEngine.getPlayer1();

        if (enemyPlayer == null || enemyPlayer.getCurrentDeck() == null || enemyPlayer.getCurrentDeck().getHeroList() == null) {
            return;
        }

        List<Card> allCards = new ArrayList<>();
        if (gameEngine.getPlayer1() != null && gameEngine.getPlayer1().getCurrentDeck() != null
                && gameEngine.getPlayer1().getCurrentDeck().getCardList() != null) {
            allCards.addAll(gameEngine.getPlayer1().getCurrentDeck().getCardList());
        }
        if (gameEngine.getPlayer2() != null && gameEngine.getPlayer2().getCurrentDeck() != null
                && gameEngine.getPlayer2().getCurrentDeck().getCardList() != null) {
            allCards.addAll(gameEngine.getPlayer2().getCurrentDeck().getCardList());
        }

        int discardedCount = 0;
        for (Card hCard : enemyPlayer.getCurrentDeck().getHeroList()) {
            if (hCard instanceof HeroCard) {
                HeroCard enemyHero = (HeroCard) hCard;
                if (Boolean.TRUE.equals(enemyHero.getEstaVivo())) {
                    List<Card> equippedItems = allCards.stream()
                            .filter(c -> c.getZoneId() == ZoneId.ATTACHED
                                    && enemyHero.getUuid().equals(c.getAttachedToCardId())
                                    && c.getCardType() == CardType.ITEM)
                            .collect(Collectors.toList());

                    if (!equippedItems.isEmpty()) {
                        int randomIndex = ThreadLocalRandom.current().nextInt(equippedItems.size());
                        Card randomItem = equippedItems.get(randomIndex);
                        gameEngine.detachCard(randomItem, ZoneId.DISCARD);
                        discardedCount++;
                        System.out.println("-> Item aleatório " + randomItem.getCardName() +
                                " foi descartado de " + enemyHero.getCardName() + "!");
                    }
                }
            }
        }

        if (discardedCount == 0) {
            System.out.println("-> Nenhum Herói Inimigo possuía Itens equipados para descartar.");
        }
    }
}

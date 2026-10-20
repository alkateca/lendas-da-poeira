package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.UUID;

public class DiscardEnemyWeaponEffect implements Effect {
    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        Player enemyPlayer = isPlayer1
                ? gameEngine.getPlayer2()
                : gameEngine.getPlayer1();

        UUID enemyHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                : gameEngine.getGameState().getActiveHeroPlayer1Id();

        if (enemyHeroId == null || enemyPlayer == null || enemyPlayer.getCurrentDeck() == null) {
            return;
        }

        Card weaponToDiscard = enemyPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.ATTACHED
                        && enemyHeroId.equals(c.getAttachedToCardId())
                        && c.getItemType() == ItemType.ARMA)
                .findFirst()
                .orElse(null);

        if (weaponToDiscard != null) {
            gameEngine.detachCard(weaponToDiscard, ZoneId.DISCARD);
            System.out.println("-> Arma " + weaponToDiscard.getCardName() + " do inimigo descartada!");
        }
    }
}

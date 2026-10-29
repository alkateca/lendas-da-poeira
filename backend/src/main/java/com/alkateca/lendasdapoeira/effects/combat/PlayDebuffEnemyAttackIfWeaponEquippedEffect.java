package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayDebuffEnemyAttackIfWeaponEquippedEffect implements Effect {

    private final int attackDebuff;

    public PlayDebuffEnemyAttackIfWeaponEquippedEffect(int attackDebuff) {
        this.attackDebuff = attackDebuff;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        UUID enemyHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                : gameEngine.getGameState().getActiveHeroPlayer1Id();

        Player enemyPlayer = isPlayer1 ? gameEngine.getPlayer2() : gameEngine.getPlayer1();

        if (enemyPlayer == null || enemyHeroId == null) {
            return;
        }

        HeroCard enemyHero = (HeroCard) enemyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(enemyHeroId))
                .findFirst()
                .orElse(null);

        if (enemyHero == null) {
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

        boolean hasWeaponEquipped = allCards.stream()
                .anyMatch(c -> c != null
                        && c.getZoneId() == ZoneId.ATTACHED
                        && enemyHeroId.equals(c.getAttachedToCardId())
                        && c.getItemType() == ItemType.ARMA);

        if (hasWeaponEquipped) {
            int prevAtk = enemyHero.getAtaque();
            enemyHero.setAtaque(prevAtk - attackDebuff);
            int actualReduction = prevAtk - enemyHero.getAtaque();
            if (actualReduction > 0) {
                gameEngine.registerCombatEndCleanup(() -> {
                    enemyHero.setAtaque(enemyHero.getAtaque() + actualReduction);
                    System.out.println("-> Ataque de " + enemyHero.getCardName() +
                            " restaurado no Final do Combate: " + enemyHero.getAtaque());
                });
            }
            System.out.println("-> " + enemyHero.getCardName() + " possui Arma equipada e recebeu -" +
                    attackDebuff + " de Ataque até o Final do Combate! Ataque atual: " + enemyHero.getAtaque());
        }
    }
}

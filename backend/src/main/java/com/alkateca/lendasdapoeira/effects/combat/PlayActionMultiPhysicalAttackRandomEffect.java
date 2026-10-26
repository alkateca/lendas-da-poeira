package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayActionMultiPhysicalAttackRandomEffect implements Effect {

    private final Random random = new Random();

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
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

        Player enemyPlayer = isPlayer1
                ? gameEngine.getPlayer2()
                : gameEngine.getPlayer1();

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId)).findFirst().orElse(null);

        if (myHero == null || myHeroId == null) {
            return;
        }

        List<Card> equippedWeapons = myPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.ATTACHED
                        && myHeroId.equals(c.getAttachedToCardId())
                        && c.getItemType() == ItemType.ARMA)
                .collect(Collectors.toList());

        List<HeroCard> livingEnemies = enemyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(c -> c instanceof HeroCard)
                .map(c -> (HeroCard) c)
                .filter(h -> Boolean.TRUE.equals(h.getEstaVivo()))
                .collect(Collectors.toList());

        for (Card weapon : equippedWeapons) {
            if (!livingEnemies.isEmpty()) {
                HeroCard targetEnemy = livingEnemies.get(random.nextInt(livingEnemies.size()));
                int damage = gameEngine.calculateBasicAttackDamage(myHero, targetEnemy);
                targetEnemy.setVidaAtual(targetEnemy.getVidaAtual() - damage);
                System.out.println("-> Ataque com " + weapon.getCardName() + " contra " +
                        targetEnemy.getCardName() + " causou " + damage + " de dano! Vida restante: " + targetEnemy.getVidaAtual());
            }
        }

        for (Card weapon : equippedWeapons) {
            gameEngine.detachCard(weapon, ZoneId.DISCARD);
            System.out.println("-> Arma " + weapon.getCardName() + " descartada de " + myHero.getCardName() + "!");
        }
    }
}

package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class OnPlayWeaponBuffAndHealEffect implements Effect {

    private final int bonusAtaque;
    private final int healAmount;
    private final Set<UUID> processedWeaponIds = new HashSet<>();

    public OnPlayWeaponBuffAndHealEffect(int bonusAtaque, int healAmount) {
        this.bonusAtaque = bonusAtaque;
        this.healAmount = healAmount;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public boolean shouldCaptureSnapshot() {
        return false;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();

        if (myHeroId == null || !myHeroId.equals(sourceCard.getUuid())) {
            return;
        }

        List<Card> playedCards = gameEngine.getResolvedPlayedCardsForPlayer(sourceCard.getOwnerId());

        if (playedCards == null) {
            return;
        }

        List<Card> newWeapons = playedCards.stream()
                .filter(c -> c.getItemType() == ItemType.ARMA && !processedWeaponIds.contains(c.getUuid()))
                .collect(Collectors.toList());

        if (newWeapons.isEmpty()) {
            return;
        }

        Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        if (myHero == null) {
            return;
        }

        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        int weaponCount = newWeapons.size();
        for (Card weapon : newWeapons) {
            processedWeaponIds.add(weapon.getUuid());
        }

        int totalAtk = bonusAtaque * weaponCount;
        int totalHeal = healAmount * weaponCount;

        myHero.setAtaque(myHero.getAtaque() + totalAtk);
        int newLife = Math.min(myHero.getVidaAtual() + totalHeal, myHero.getVidaMaxima());
        myHero.setVidaAtual(newLife);

        gameEngine.registerCombatEndCleanup(() -> {
            myHero.setAtaque(myHero.getAtaque() - totalAtk);
            processedWeaponIds.clear();
            System.out.println("-> Buff temporário de " + sourceCard.getCardName() + " (-" + totalAtk + " Ataque) expirou no Final do Combate.");
        });

        System.out.println("-> Ao jogar Arma (x" + weaponCount + "), " + myHero.getCardName() +
                " recebeu +" + totalAtk + " de Ataque até o Final do Combate e curou para " + myHero.getVidaAtual() + " de Vida!");

        gameEngine.captureSnapshot(sourceCard, this, "EFFECT_RESOLVED");
    }
}

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

        Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (myPlayer == null || myPlayer.getCurrentDeck() == null) {
            return;
        }

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        if (myHero == null || !Boolean.TRUE.equals(myHero.getEstaVivo())) {
            return;
        }

        List<Card> resolvedSoFar = gameEngine.getResolvedPlayedCardsForPlayer(sourceCard.getOwnerId());
        List<Card> allPlayedInCombat = isPlayer1
                ? gameEngine.getGameState().getPlayer1PlayedCards()
                : gameEngine.getGameState().getPlayer2PlayedCards();

        // Inicializa os IDs de armas que já estavam equipadas em turnos anteriores para não contá-las novamente
        if (processedWeaponIds.isEmpty() && myPlayer.getCurrentDeck().getCardList() != null) {
            myPlayer.getCurrentDeck().getCardList().stream()
                    .filter(c -> c.getZoneId() == com.alkateca.lendasdapoeira.enums.ZoneId.ATTACHED
                            && myHeroId.equals(c.getAttachedToCardId())
                            && c.getItemType() == ItemType.ARMA
                            && (allPlayedInCombat == null || !allPlayedInCombat.contains(c)))
                    .forEach(c -> processedWeaponIds.add(c.getUuid()));

            gameEngine.registerCombatEndCleanup(processedWeaponIds::clear);
        }

        // Considera armas que já resolveram na pilha (incluindo armas invocadas/jogadas por efeitos)
        List<Card> newWeapons = (allPlayedInCombat == null ? java.util.Collections.<Card>emptyList() : allPlayedInCombat).stream()
                .filter(c -> c.getItemType() == ItemType.ARMA && !processedWeaponIds.contains(c.getUuid()))
                .filter(c -> (resolvedSoFar != null && resolvedSoFar.contains(c))
                          || myHeroId.equals(c.getAttachedToCardId()))
                .collect(Collectors.toList());

        if (newWeapons.isEmpty()) {
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
            System.out.println("-> Buff temporário de " + sourceCard.getCardName() + " (-" + totalAtk + " Ataque) expirou no Final do Combate.");
        });

        System.out.println("-> Ao jogar Arma (x" + weaponCount + "), " + myHero.getCardName() +
                " recebeu +" + totalAtk + " de Ataque até o Final do Combate e curou para " + myHero.getVidaAtual() + " de Vida!");

        gameEngine.captureSnapshot(sourceCard, this, "EFFECT_RESOLVED");
    }
}

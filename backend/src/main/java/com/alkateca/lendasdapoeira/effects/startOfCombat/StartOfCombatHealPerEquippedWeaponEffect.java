package com.alkateca.lendasdapoeira.effects.startOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.ArrayList;
import java.util.List;

public class StartOfCombatHealPerEquippedWeaponEffect implements Effect {

    private final int healPerWeapon;

    public StartOfCombatHealPerEquippedWeaponEffect(int healPerWeapon) {
        this.healPerWeapon = healPerWeapon;
    }

    public int getHealPerWeapon() {
        return healPerWeapon;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_START;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId() != null
                && sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        java.util.UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();

        com.alkateca.lendasdapoeira.entity.Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        if (myHero == null || !Boolean.TRUE.equals(myHero.getEstaVivo())) {
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

        int weaponCount = (int) allCards.stream()
                .filter(c -> c != null
                        && c.getZoneId() == ZoneId.ATTACHED
                        && myHero.getUuid().equals(c.getAttachedToCardId())
                        && c.getItemType() == ItemType.ARMA)
                .count();

        int totalHeal = weaponCount * healPerWeapon;

        if (totalHeal > 0) {
            int oldHp = myHero.getVidaAtual();
            int newHp = Math.min(oldHp + totalHeal, myHero.getVidaMaxima());
            int recovered = Math.max(0, newHp - oldHp);
            myHero.setVidaAtual(newHp);

            System.out.println("-> " + myHero.getCardName() + " recuperou " + recovered +
                    " pontos de vida (" + weaponCount + " arma(s) equipada(s))! Vida restante: " + myHero.getVidaAtual());
        } else {
            System.out.println("-> Nenhuma arma equipada em " + myHero.getCardName() + ". Nenhuma cura aplicada.");
        }
    }
}

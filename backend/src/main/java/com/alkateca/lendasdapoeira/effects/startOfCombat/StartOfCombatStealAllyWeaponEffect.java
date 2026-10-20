package com.alkateca.lendasdapoeira.effects.startOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.UUID;

public class StartOfCombatStealAllyWeaponEffect implements Effect {

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_START;
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

        if (myPlayer == null || myPlayer.getCurrentDeck() == null || myPlayer.getCurrentDeck().getCardList() == null) {
            return;
        }

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        if (myHero == null) return;

        int freeSlots = myHero.getMaxWeaponSlots() - gameEngine.getUsedWeaponSlotsForHero(myHero.getUuid());

        Card allyWeapon = myPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.ATTACHED
                        && c.getItemType() == ItemType.ARMA
                        && c.getAttachedToCardId() != null
                        && !c.getAttachedToCardId().equals(myHero.getUuid())
                        && c.getEmpunhadura() <= freeSlots)
                .findFirst()
                .orElse(null);

        if (allyWeapon != null) {
            gameEngine.detachCard(allyWeapon, ZoneId.RESOLVE);
            if (allyWeapon.getEffects() != null) {
                for (Effect effect : allyWeapon.getEffects()) {
                    if (effect.predicate(allyWeapon, TurnPhase.RESOLUTION)) {
                        effect.resolve(gameEngine, allyWeapon);
                    }
                }
            }
            allyWeapon.setAttachedToCardId(myHero.getUuid());
            allyWeapon.setZoneId(ZoneId.ATTACHED);
            System.out.println("-> " + myHero.getCardName() + " desequipou a Arma Aliada " +
                    allyWeapon.getCardName() + " e equipou-a em si mesmo!");
        } else {
            System.out.println("-> Nenhuma Arma Aliada disponível para equipar em " + myHero.getCardName() + ".");
        }
    }
}

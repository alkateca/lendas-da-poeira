package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.UUID;

public class EquipGlobalBuffEffect implements Effect {
    private final int bonusAtaque;

    public EquipGlobalBuffEffect(int bonusAtaque) {
        this.bonusAtaque = bonusAtaque;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();

        if (myHeroId == null && sourceCard.getAttachedToCardId() != null) {
            myHeroId = sourceCard.getAttachedToCardId();
        }

        UUID finalMyHeroId = myHeroId;
        HeroCard targetHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(finalMyHeroId))
                .findFirst()
                .orElse(null);

        if (targetHero != null) {
            if (sourceCard.getItemType() == ItemType.ARMA && sourceCard.getEmpunhadura() > 0) {
                int currentUsedSlots = gameEngine.getUsedWeaponSlotsForHero(targetHero.getUuid());
                if (currentUsedSlots + sourceCard.getEmpunhadura() > targetHero.getMaxWeaponSlots()) {
                    sourceCard.setAttachedToCardId(null);
                    System.out.println("-> Falha ao equipar " + sourceCard.getCardName() + ": Slots de arma insuficientes em " + targetHero.getCardName());
                    return;
                }
            }
            sourceCard.setAttachedToCardId(targetHero.getUuid());
            sourceCard.setZoneId(ZoneId.ATTACHED);
        }

        for (Card card : myPlayer.getCurrentDeck().getHeroList()) {
            HeroCard hero = (HeroCard) card;
            hero.setAtaque(hero.getAtaque() + bonusAtaque);
            System.out.println("-> " + hero.getCardName() + " recebeu +" + bonusAtaque + " de Ataque global!");
        }
    }

    @Override
    public void onDetach(GameEngine gameEngine, Card sourceCard, HeroCard targetHero) {
        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (myPlayer != null && myPlayer.getCurrentDeck() != null && myPlayer.getCurrentDeck().getHeroList() != null) {
            for (Card card : myPlayer.getCurrentDeck().getHeroList()) {
                HeroCard hero = (HeroCard) card;
                hero.setAtaque(hero.getAtaque() - bonusAtaque);
            }
            System.out.println("-> " + sourceCard.getCardName() + " foi desanexado! Bônus global de Ataque removido.");
        }
    }
}

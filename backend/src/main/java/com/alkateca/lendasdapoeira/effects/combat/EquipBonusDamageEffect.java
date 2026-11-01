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

public class EquipBonusDamageEffect implements Effect {

    private final int bonusDamage;

    public EquipBonusDamageEffect(int bonusDamage) {
        this.bonusDamage = bonusDamage;
    }

    public int getBonusDamage() {
        return bonusDamage;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId() != null
                && sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        UUID allyHeroId = sourceCard.getAttachedToCardId();
        if (allyHeroId == null && gameEngine.getGameState() != null) {
            allyHeroId = isPlayer1
                    ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                    : gameEngine.getGameState().getActiveHeroPlayer2Id();
        }

        Player allyPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        final UUID targetHeroId = allyHeroId;
        HeroCard targetHero = (HeroCard) allyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(targetHeroId))
                .findFirst()
                .orElse(null);

        if (targetHero == null) {
            return;
        }

        if (sourceCard.getItemType() == ItemType.ARMA) {
            boolean alreadyCountedInAttached = sourceCard.getZoneId() == ZoneId.ATTACHED
                    && targetHero.getUuid().equals(sourceCard.getAttachedToCardId());
            int occupiedSlots = gameEngine.getUsedWeaponSlotsForHero(targetHero.getUuid());
            int requiredSlots = alreadyCountedInAttached ? 0 : Math.max(1, sourceCard.getEmpunhadura());

            if (occupiedSlots + requiredSlots > targetHero.getMaxWeaponSlots()) {
                sourceCard.setAttachedToCardId(null);
                sourceCard.setZoneId(ZoneId.DISCARD);
                System.out.println("-> Limite de empunhadura excedido em " + targetHero.getCardName()
                        + "! " + sourceCard.getCardName() + " foi enviada para o Descarte.");
                return;
            }
        }

        sourceCard.setAttachedToCardId(targetHero.getUuid());
        sourceCard.setZoneId(ZoneId.ATTACHED);
        targetHero.setDanoBonus(targetHero.getDanoBonus() + bonusDamage);

        System.out.println("-> " + sourceCard.getCardName() + " anexada a " + targetHero.getCardName()
                + " (+" + bonusDamage + " Dano Bônus)! Vida restante: " + targetHero.getVidaAtual());
    }

    @Override
    public void onDetach(GameEngine gameEngine, Card sourceCard, HeroCard targetHero) {
        if (targetHero != null) {
            targetHero.setDanoBonus(targetHero.getDanoBonus() - bonusDamage);
            System.out.println("-> " + sourceCard.getCardName() + " desanexada de " + targetHero.getCardName()
                    + " (-" + bonusDamage + " Dano Bônus removido)!");
        }
    }
}

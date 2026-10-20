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
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayWeaponFromHandWithConditionalBuffEffect implements Effect {

    private final int bonusAtaque;

    public PlayWeaponFromHandWithConditionalBuffEffect(int bonusAtaque) {
        this.bonusAtaque = bonusAtaque;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (myPlayer == null || myPlayer.getCurrentDeck() == null || myPlayer.getCurrentDeck().getCardList() == null) {
            return;
        }

        UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        if (myHero == null) {
            return;
        }

        int usedSlots = gameEngine.getUsedWeaponSlotsForHero(myHero.getUuid());
        int freeSlots = Math.max(0, myHero.getMaxWeaponSlots() - usedSlots);

        List<UUID> validTargetUuids = myPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.HAND
                        && c.getItemType() == ItemType.ARMA
                        && c.getEmpunhadura() <= freeSlots
                        && !c.getUuid().equals(sourceCard.getUuid()))
                .map(Card::getUuid)
                .collect(Collectors.toList());

        gameEngine.requestPlayerChoice(
                myPlayer.getUuid(),
                sourceCard,
                "HAND",
                "Escolha 1 Arma da sua mão para jogar e conceder +" + bonusAtaque + " de Ataque",
                validTargetUuids,
                false,
                chosenCardUuid -> {
                    if (chosenCardUuid == null) return;
                    Card chosenCard = myPlayer.getCurrentDeck().getCardList().stream()
                            .filter(c -> c.getUuid().equals(chosenCardUuid) && c.getZoneId() == ZoneId.HAND)
                            .findFirst()
                            .orElse(null);

                    if (chosenCard != null) {
                        System.out.println("-> " + myPlayer.getName() + " jogou a Arma " + chosenCard.getCardName() +
                                " da mão via " + sourceCard.getCardName() + "!");
                        myHero.setAtaque(myHero.getAtaque() + bonusAtaque);
                        gameEngine.registerCombatEndCleanup(() -> {
                            myHero.setAtaque(myHero.getAtaque() - bonusAtaque);
                            System.out.println("-> Buff temporário de " + sourceCard.getCardName() + " expirou em " + myHero.getCardName() + ".");
                        });
                        System.out.println("-> " + myHero.getCardName() + " recebeu +" + bonusAtaque + " de Ataque até o Final do Combate!");
                        gameEngine.playCardIntoResolution(chosenCard);
                    }
                }
        );
    }
}

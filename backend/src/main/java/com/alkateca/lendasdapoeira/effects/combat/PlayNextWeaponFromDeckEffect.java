package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

public class PlayNextWeaponFromDeckEffect implements Effect {

    private final int empunhaduraRequerida;

    public PlayNextWeaponFromDeckEffect(int empunhaduraRequerida) {
        this.empunhaduraRequerida = empunhaduraRequerida;
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

        Card weaponToPlay = myPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.DECK
                        && c.getItemType() == ItemType.ARMA
                        && c.getEmpunhadura() == empunhaduraRequerida)
                .findFirst()
                .orElse(null);

        if (weaponToPlay != null) {
            System.out.println("-> Jogando a próxima Arma de Empunhadura " + empunhaduraRequerida +
                    " do baralho: " + weaponToPlay.getCardName() + "!");
            gameEngine.playCardIntoResolution(weaponToPlay);
        } else {
            System.out.println("-> Nenhuma Arma de Empunhadura " + empunhaduraRequerida + " encontrada no baralho.");
        }
    }
}

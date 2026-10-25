package com.alkateca.lendasdapoeira.effects.startOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.UUID;

public class StartOfCombatPlayNextCardByTypeEffect implements Effect {
    private final ItemType typeToPlay;

    public StartOfCombatPlayNextCardByTypeEffect(ItemType typeToPlay) {
        this.typeToPlay = typeToPlay;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_START;
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

        Card cardToPlay = myPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.DECK && c.getItemType() == typeToPlay)
                .findFirst()
                .orElse(null);

        if (cardToPlay != null) {
            System.out.println("-> " + sourceCard.getCardName() + " jogou a próxima carta do tipo " +
                    typeToPlay.name() + " do baralho: " + cardToPlay.getCardName() + "!");

            gameEngine.playCardIntoResolution(cardToPlay);
        } else {
            System.out.println("-> Nenhuma carta do tipo " + typeToPlay.name() + " encontrada no baralho.");
        }
    }
}

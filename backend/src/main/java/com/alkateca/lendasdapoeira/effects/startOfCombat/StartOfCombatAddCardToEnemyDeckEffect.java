package com.alkateca.lendasdapoeira.effects.startOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.CardFactory;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.Colors;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.ArrayList;
import java.util.UUID;

public class StartOfCombatAddCardToEnemyDeckEffect implements Effect {

    private final String cardNameToAdd;

    public StartOfCombatAddCardToEnemyDeckEffect(String cardNameToAdd) {
        this.cardNameToAdd = cardNameToAdd;
    }

    public String getCardNameToAdd() {
        return cardNameToAdd;
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

        com.alkateca.lendasdapoeira.entity.Player enemyPlayer = isPlayer1
                ? gameEngine.getPlayer2()
                : gameEngine.getPlayer1();

        if (enemyPlayer == null || enemyPlayer.getCurrentDeck() == null
                || enemyPlayer.getCurrentDeck().getCardList() == null) {
            return;
        }

        Card newCard = new Card();
        newCard.setUuid(UUID.randomUUID());
        newCard.setCardName(cardNameToAdd);
        newCard.setOwnerId(enemyPlayer.getUuid());
        newCard.setZoneId(ZoneId.DECK);
        newCard.setItemType(ItemType.NENHUM);
        newCard.setEmpunhadura(0);
        newCard.setRacas(new ArrayList<>());
        newCard.setAfinidades(new ArrayList<>());
        newCard.setOrdem(new ArrayList<>());
        newCard.setClasse(new ArrayList<>());

        if ("Tristeza".equalsIgnoreCase(cardNameToAdd)) {
            newCard.setCardType(CardType.ACTION);
            newCard.setColor(Colors.NEUTRAL);
            newCard.setDescricao("Exilar.");
        }

        CardFactory.buildCardLogic(newCard);

        enemyPlayer.getCurrentDeck().getCardList().add(newCard);

        System.out.println("-> A carta " + newCard.getCardName() + " foi adicionada ao baralho de " +
                enemyPlayer.getName() + "!");
    }
}

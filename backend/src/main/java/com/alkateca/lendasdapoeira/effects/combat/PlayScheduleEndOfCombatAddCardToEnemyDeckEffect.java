package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.CardFactory;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.Colors;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.ArrayList;
import java.util.UUID;

public class PlayScheduleEndOfCombatAddCardToEnemyDeckEffect implements Effect {

    private final String cardNameToAdd;
    private boolean scheduledForCombatEnd = false;

    public PlayScheduleEndOfCombatAddCardToEnemyDeckEffect(String cardNameToAdd) {
        this.cardNameToAdd = cardNameToAdd;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION || turnPhase == TurnPhase.COMBAT_END;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        Player enemyPlayer = isPlayer1
                ? gameEngine.getPlayer2()
                : gameEngine.getPlayer1();

        if (enemyPlayer == null || enemyPlayer.getCurrentDeck() == null || enemyPlayer.getCurrentDeck().getCardList() == null) {
            return;
        }

        if (gameEngine.getTurnPhase() == TurnPhase.RESOLUTION) {
            scheduledForCombatEnd = true;
            System.out.println("Efeito de " + sourceCard.getCardName() +
                    " agendou para o Final do Combate: Adicionar " + cardNameToAdd + " ao baralho inimigo!");
            gameEngine.registerCombatEndCleanup(() -> {
                addCardToEnemyDeck(enemyPlayer, sourceCard);
                scheduledForCombatEnd = false;
            });
        } else if (gameEngine.getTurnPhase() == TurnPhase.COMBAT_END && !scheduledForCombatEnd) {
            System.out.println("Efeito de " + sourceCard.getCardName() + " ativado no Final do Combate!");
            addCardToEnemyDeck(enemyPlayer, sourceCard);
        }
    }

    private void addCardToEnemyDeck(Player enemyPlayer, Card sourceCard) {
        Card newCard = new Card();
        newCard.setUuid(UUID.randomUUID());
        newCard.setCardName(cardNameToAdd);
        newCard.setCardType(CardType.ACTION);
        newCard.setColor(Colors.NEUTRAL);
        newCard.setItemType(ItemType.NENHUM);
        newCard.setEmpunhadura(0);
        newCard.setOwnerId(enemyPlayer.getUuid());
        newCard.setZoneId(ZoneId.DECK);
        newCard.setRacas(new ArrayList<>());
        newCard.setOrdem(new ArrayList<>());
        newCard.setClasse(new ArrayList<>());
        newCard.setAfinidades(new ArrayList<>());

        if ("Tristeza".equalsIgnoreCase(cardNameToAdd)) {
            newCard.setDescricao("Quem bate esquece, quem apanha não.\nExilar.");
        }

        CardFactory.buildCardLogic(newCard);
        enemyPlayer.getCurrentDeck().getCardList().add(newCard);

        System.out.println("-> Final do Combate (" + sourceCard.getCardName() + "): Uma carta '" +
                cardNameToAdd + "' foi adicionada ao baralho (DECK) do inimigo!");
    }
}

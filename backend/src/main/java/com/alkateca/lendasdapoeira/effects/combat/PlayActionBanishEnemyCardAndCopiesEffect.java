package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayActionBanishEnemyCardAndCopiesEffect implements Effect {

    public PlayActionBanishEnemyCardAndCopiesEffect() {
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        Player myPlayer = isPlayer1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();
        Player enemyPlayer = isPlayer1 ? gameEngine.getPlayer2() : gameEngine.getPlayer1();

        if (myPlayer == null || enemyPlayer == null || enemyPlayer.getCurrentDeck() == null
                || enemyPlayer.getCurrentDeck().getCardList() == null) {
            return;
        }

        List<UUID> validTargetUuids = enemyPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.ATTACHED || c.getZoneId() == ZoneId.DISCARD)
                .map(Card::getUuid)
                .collect(Collectors.toList());

        gameEngine.requestPlayerChoice(
                myPlayer.getUuid(),
                sourceCard,
                "ENEMY_IN_PLAY_OR_DISCARD",
                "Escolha uma carta Inimiga Em Jogo ou no Descarte para exilar todas as suas cópias",
                validTargetUuids,
                false,
                chosenCardUuid -> {
                    if (chosenCardUuid == null) return;
                    Card chosenCard = enemyPlayer.getCurrentDeck().getCardList().stream()
                            .filter(c -> c.getUuid().equals(chosenCardUuid)
                                    && (c.getZoneId() == ZoneId.ATTACHED || c.getZoneId() == ZoneId.DISCARD))
                            .findFirst()
                            .orElse(null);

                    if (chosenCard != null && chosenCard.getCardName() != null) {
                        String targetCardName = chosenCard.getCardName();
                        int exiledCount = 0;

                        for (Card c : enemyPlayer.getCurrentDeck().getCardList()) {
                            if (targetCardName.equalsIgnoreCase(c.getCardName()) && c.getZoneId() != ZoneId.EXILE) {
                                if (c.getZoneId() == ZoneId.ATTACHED) {
                                    gameEngine.detachCard(c, ZoneId.EXILE);
                                } else {
                                    c.setZoneId(ZoneId.EXILE);
                                }
                                exiledCount++;
                            }
                        }

                        System.out.println("-> " + sourceCard.getCardName() + " exilou " + exiledCount +
                                " cópia(s) de '" + targetCardName + "' do oponente!");
                    }
                }
        );
    }
}

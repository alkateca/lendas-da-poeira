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

        // Alvos válidos: cartas inimigas anexadas em campo (ATTACHED) ou na pilha de resolução ainda não resolvidas (RESOLVE)
        List<UUID> validTargetUuids = enemyPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.ATTACHED
                        || (c.getZoneId() == ZoneId.RESOLVE && !gameEngine.getResolvedCardsInCurrentCombat().contains(c)))
                .map(Card::getUuid)
                .collect(Collectors.toList());

        gameEngine.requestPlayerChoice(
                myPlayer.getUuid(),
                sourceCard,
                "ENEMY_IN_PLAY_OR_RESOLVE",
                "Escolha 1 carta Inimiga em jogo ou na fila de resolução para descartar e anular",
                validTargetUuids,
                false,
                chosenCardUuid -> {
                    if (chosenCardUuid == null) return;

                    Card chosenCard = enemyPlayer.getCurrentDeck().getCardList().stream()
                            .filter(c -> c.getUuid().equals(chosenCardUuid)
                                    && (c.getZoneId() == ZoneId.ATTACHED || c.getZoneId() == ZoneId.RESOLVE))
                            .findFirst()
                            .orElse(null);

                    if (chosenCard != null) {
                        gameEngine.cancelOrDiscardCardInPlay(chosenCard);
                        System.out.println("-> " + sourceCard.getCardName() + " retirou '"
                                + chosenCard.getCardName() + "' de jogo/resolução e a enviou para o descarte!");
                    }
                }
        );
    }
}

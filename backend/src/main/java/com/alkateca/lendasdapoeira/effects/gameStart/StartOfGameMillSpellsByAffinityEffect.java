package com.alkateca.lendasdapoeira.effects.gameStart;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.List;
import java.util.stream.Collectors;

public class StartOfGameMillSpellsByAffinityEffect implements Effect {

    private final int count;
    private final String affinity;
    private boolean applied = false;

    public StartOfGameMillSpellsByAffinityEffect(int count, String affinity) {
        this.count = count;
        this.affinity = affinity;
    }

    @Override
    public boolean shouldCaptureSnapshot() {
        return false;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return !applied && turnPhase == TurnPhase.GAME_START;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        if (applied) {
            return;
        }
        applied = true;

        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado no Início da Partida!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (myPlayer == null || myPlayer.getCurrentDeck() == null || myPlayer.getCurrentDeck().getCardList() == null) {
            return;
        }

        List<Card> matchingSpellsInDeck = myPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.DECK
                        && c.getCardType() == CardType.SPELL
                        && ((c.getAfinidades() != null && c.getAfinidades().stream().anyMatch(a ->
                                a.name().equalsIgnoreCase(affinity) || a.getNomeFormatado().equalsIgnoreCase(affinity)))
                            || (c.getDescricao() != null && c.getDescricao().toLowerCase().contains("afinidade " + affinity.toLowerCase()))))
                .limit(count)
                .collect(Collectors.toList());

        if (!matchingSpellsInDeck.isEmpty()) {
            for (Card spell : matchingSpellsInDeck) {
                spell.setZoneId(ZoneId.DISCARD);
                System.out.println("-> " + sourceCard.getCardName() + " enviou " + spell.getCardName() +
                        " (Afinidade " + affinity + ") do baralho para o descarte!");
            }
            gameEngine.captureSnapshot(sourceCard, this, "EFFECT_RESOLVED");
        }
    }
}

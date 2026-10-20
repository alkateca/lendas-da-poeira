package com.alkateca.lendasdapoeira.effects.startOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.List;
import java.util.stream.Collectors;

public class StartOfCombatTutorSpellFromDiscardToHandEffect implements Effect {

    private final int count;
    private final String affinity;

    public StartOfCombatTutorSpellFromDiscardToHandEffect(int count, String affinity) {
        this.count = count;
        this.affinity = affinity;
    }

    @Override
    public boolean shouldCaptureSnapshot() {
        return false;
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

        List<Card> matchingSpellsInDiscard = myPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.DISCARD
                        && c.getCardType() == CardType.SPELL
                        && ((c.getAfinidades() != null && c.getAfinidades().stream().anyMatch(a ->
                                a.name().equalsIgnoreCase(affinity) || a.getNomeFormatado().equalsIgnoreCase(affinity)))
                            || (c.getDescricao() != null && c.getDescricao().toLowerCase().contains("afinidade " + affinity.toLowerCase()))))
                .limit(count)
                .collect(Collectors.toList());

        if (!matchingSpellsInDiscard.isEmpty()) {
            for (Card spell : matchingSpellsInDiscard) {
                spell.setZoneId(ZoneId.HAND);
                System.out.println("-> " + sourceCard.getCardName() + " adicionou " + spell.getCardName() +
                        " (Afinidade " + affinity + ") do descarte para a mão!");
            }
            gameEngine.captureSnapshot(sourceCard, this, "EFFECT_RESOLVED");
        }
    }
}

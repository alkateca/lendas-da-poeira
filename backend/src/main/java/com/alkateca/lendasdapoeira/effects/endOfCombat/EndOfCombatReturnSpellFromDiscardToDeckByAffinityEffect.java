package com.alkateca.lendasdapoeira.effects.endOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.Afinidade;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.text.Normalizer;
import java.util.List;

public class EndOfCombatReturnSpellFromDiscardToDeckByAffinityEffect implements Effect {

    private final int count;
    private final String affinity;

    public EndOfCombatReturnSpellFromDiscardToDeckByAffinityEffect(int count, String affinity) {
        this.count = count;
        this.affinity = affinity;
    }

    public int getCount() {
        return count;
    }

    public String getAffinity() {
        return affinity;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_END;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId() != null
                && sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (myPlayer == null || myPlayer.getCurrentDeck() == null
                || myPlayer.getCurrentDeck().getCardList() == null || count <= 0) {
            return;
        }

        List<Card> spellsToReturn = myPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c != null
                        && c.getZoneId() == ZoneId.DISCARD
                        && c.getCardType() == CardType.SPELL
                        && hasMatchingAffinity(c, affinity))
                .limit(count)
                .toList();

        if (spellsToReturn.isEmpty()) {
            System.out.println("-> Nenhum feitiço com afinidade " + affinity + " no descarte para retornar ao baralho.");
            return;
        }

        for (Card spell : spellsToReturn) {
            spell.setZoneId(ZoneId.DECK);
            System.out.println("-> " + spell.getCardName() + " retornou do descarte para o baralho de "
                    + myPlayer.getName() + "!");
        }
    }

    private boolean hasMatchingAffinity(Card card, String targetAffinity) {
        if (card.getAfinidades() == null || targetAffinity == null) return false;
        String normalizedTarget = normalizeText(targetAffinity);
        for (Afinidade af : card.getAfinidades()) {
            if (af == null) continue;
            if (af.name().equalsIgnoreCase(targetAffinity)
                    || normalizeText(af.name()).equalsIgnoreCase(normalizedTarget)
                    || normalizeText(af.toString()).equalsIgnoreCase(normalizedTarget)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeText(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .trim();
    }
}

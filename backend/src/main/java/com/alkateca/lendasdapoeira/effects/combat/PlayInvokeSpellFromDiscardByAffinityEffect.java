package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.Afinidade;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.text.Normalizer;
import java.util.List;
import java.util.UUID;

public class PlayInvokeSpellFromDiscardByAffinityEffect implements Effect {

    private final String affinity;

    public PlayInvokeSpellFromDiscardByAffinityEffect(String affinity) {
        this.affinity = affinity;
    }

    public String getAffinity() {
        return affinity;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId() != null
                && sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();

        Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (myPlayer == null || myPlayer.getCurrentDeck() == null
                || myPlayer.getCurrentDeck().getCardList() == null) {
            return;
        }

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        List<Card> validSpells = myPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c != null
                        && c.getZoneId() == ZoneId.DISCARD
                        && c.getCardType() == CardType.SPELL
                        && hasMatchingAffinity(c, affinity))
                .toList();

        if (validSpells.isEmpty()) {
            System.out.println("-> Nenhum feitiço com afinidade " + affinity + " encontrado no descarte.");
            return;
        }

        List<UUID> validTargetUuids = validSpells.stream()
                .map(Card::getUuid)
                .toList();

        gameEngine.requestPlayerChoice(
                myPlayer.getUuid(),
                sourceCard,
                "DISCARD_SPELL",
                "Escolha 1 Feitiço de afinidade " + affinity + " do seu Descarte para Invocar:",
                validTargetUuids,
                false,
                chosenCardUuid -> {
                    if (chosenCardUuid == null) return;
                    Card chosenCard = myPlayer.getCurrentDeck().getCardList().stream()
                            .filter(c -> chosenCardUuid.equals(c.getUuid()))
                            .findFirst()
                            .orElse(null);

                    if (chosenCard != null) {
                        System.out.println("-> " + chosenCard.getCardName() + " foi escolhido no descarte e invocado!");
                        gameEngine.invokeCard(myPlayer, sourceCard, myHero, chosenCard.getCardName());
                    }
                }
        );
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

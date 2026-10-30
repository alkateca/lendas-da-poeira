package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AuraOnDiscardCardBuffAlliesEffect implements Effect {

    private final String targetCardName;
    private final int spiritBuff;
    private final int attackBuff;
    private final int defenseBuff;
    private final Set<UUID> processedDiscardedCardIds = new HashSet<>();

    public AuraOnDiscardCardBuffAlliesEffect(String targetCardName, int spiritBuff, int attackBuff, int defenseBuff) {
        this.targetCardName = targetCardName;
        this.spiritBuff = spiritBuff;
        this.attackBuff = attackBuff;
        this.defenseBuff = defenseBuff;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.DISCARD;
    }

    @Override
    public boolean canTriggerFromBench() {
        return true;
    }

    @Override
    public boolean shouldCaptureSnapshot() {
        return false;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        if (sourceCard instanceof HeroCard sourceHero && !Boolean.TRUE.equals(sourceHero.getEstaVivo())) {
            return;
        }

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        Player enemyPlayer = isPlayer1
                ? gameEngine.getPlayer2()
                : gameEngine.getPlayer1();

        if (myPlayer == null || myPlayer.getCurrentDeck() == null || myPlayer.getCurrentDeck().getHeroList() == null) {
            return;
        }

        List<Card> allCards = new ArrayList<>();
        if (myPlayer.getCurrentDeck().getCardList() != null) {
            allCards.addAll(myPlayer.getCurrentDeck().getCardList());
        }
        if (enemyPlayer != null && enemyPlayer.getCurrentDeck() != null && enemyPlayer.getCurrentDeck().getCardList() != null) {
            allCards.addAll(enemyPlayer.getCurrentDeck().getCardList());
        }

        // Remove do rastreio cartas que eventualmente tenham saído do descarte (ex: revividas/retornadas)
        Set<UUID> currentDiscardIds = new HashSet<>();
        for (Card c : allCards) {
            if (c != null && c.getZoneId() == ZoneId.DISCARD) {
                currentDiscardIds.add(c.getUuid());
            }
        }
        processedDiscardedCardIds.retainAll(currentDiscardIds);

        // Contabiliza quantas novas cópias da carta alvo ("Tristeza") entraram no descarte
        int newlyDiscardedMatches = 0;
        for (Card c : allCards) {
            if (c != null
                    && c.getZoneId() == ZoneId.DISCARD
                    && targetCardName.equalsIgnoreCase(c.getCardName())
                    && processedDiscardedCardIds.add(c.getUuid())) {
                newlyDiscardedMatches++;
            }
        }

        if (newlyDiscardedMatches <= 0) {
            return;
        }

        for (int i = 0; i < newlyDiscardedMatches; i++) {
            System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

            for (HeroCard allyHero : myPlayer.getCurrentDeck().getHeroList()) {
                if (allyHero != null && Boolean.TRUE.equals(allyHero.getEstaVivo())) {
                    if (spiritBuff != 0) {
                        allyHero.setEspirito(allyHero.getEspirito() + spiritBuff);
                    }
                    if (attackBuff != 0) {
                        allyHero.setAtaque(allyHero.getAtaque() + attackBuff);
                    }
                    if (defenseBuff != 0) {
                        allyHero.setDefesa(allyHero.getDefesa() + defenseBuff);
                    }
                    System.out.println("-> " + allyHero.getCardName() + " recebeu +" + attackBuff
                            + " de Ataque permanente!");
                }
            }
        }
    }
}

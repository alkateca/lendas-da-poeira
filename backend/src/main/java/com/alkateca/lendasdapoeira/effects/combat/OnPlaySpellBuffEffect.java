package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class OnPlaySpellBuffEffect implements Effect {

    private final CardType targetCardType;
    private final int bonusEspirito;
    private final int bonusAtaque;
    private final int bonusDefesa;
    private final boolean temporary;

    private final Set<UUID> processedCardIds = new HashSet<>();
    private HeroCard buffedHero;
    private int appliedEsp = 0;
    private int appliedAtk = 0;
    private int appliedDef = 0;
    private boolean activeBuff = false;

    public OnPlaySpellBuffEffect(int bonusEspirito, int bonusAtaque, int bonusDefesa) {
        this(CardType.SPELL, bonusEspirito, bonusAtaque, bonusDefesa, false);
    }

    public OnPlaySpellBuffEffect(int bonusEspirito, int bonusAtaque, int bonusDefesa, boolean temporary) {
        this(CardType.SPELL, bonusEspirito, bonusAtaque, bonusDefesa, temporary);
    }

    public OnPlaySpellBuffEffect(CardType targetCardType, int bonusEspirito, int bonusAtaque, int bonusDefesa) {
        this(targetCardType, bonusEspirito, bonusAtaque, bonusDefesa, false);
    }

    public OnPlaySpellBuffEffect(CardType targetCardType, int bonusEspirito, int bonusAtaque, int bonusDefesa, boolean temporary) {
        this.targetCardType = targetCardType;
        this.bonusEspirito = bonusEspirito;
        this.bonusAtaque = bonusAtaque;
        this.bonusDefesa = bonusDefesa;
        this.temporary = temporary;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public boolean shouldCaptureSnapshot() {
        return false;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        if (gameEngine.getTurnPhase() == TurnPhase.COMBAT_END) {
            revertBuff();
            return;
        }

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();

        if (myHeroId == null || !myHeroId.equals(sourceCard.getUuid())) {
            return;
        }

        List<Card> playedCards = gameEngine.getResolvedPlayedCardsForPlayer(sourceCard.getOwnerId());
        List<Card> newMatchingCards = playedCards.stream()
                .filter(c -> c.getCardType() == targetCardType && !processedCardIds.contains(c.getUuid()))
                .toList();

        int matchCount = newMatchingCards.size();
        if (matchCount > 0) {
            for (Card c : newMatchingCards) {
                processedCardIds.add(c.getUuid());
            }
            gameEngine.registerCombatEndCleanup(processedCardIds::clear);

            com.alkateca.lendasdapoeira.entity.Player myPlayer = isPlayer1
                    ? gameEngine.getPlayer1()
                    : gameEngine.getPlayer2();

            HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                    .filter(h -> h.getUuid().equals(myHeroId))
                    .findFirst()
                    .orElse(null);

            if (myHero != null) {
                int goblinBonus = gameEngine.getGoblinEffectBonus(sourceCard);
                int perCardEsp = bonusEspirito > 0 ? bonusEspirito + goblinBonus : bonusEspirito;
                int perCardAtk = bonusAtaque > 0 ? bonusAtaque + goblinBonus : bonusAtaque;
                int perCardDef = bonusDefesa > 0 ? bonusDefesa + goblinBonus : bonusDefesa;

                int totalEsp = perCardEsp * matchCount;
                int totalAtk = perCardAtk * matchCount;
                int totalDef = perCardDef * matchCount;
                myHero.setEspirito(myHero.getEspirito() + totalEsp);
                myHero.setAtaque(myHero.getAtaque() + totalAtk);
                myHero.setDefesa(myHero.getDefesa() + totalDef);

                if (temporary) {
                    this.buffedHero = myHero;
                    this.appliedEsp += totalEsp;
                    this.appliedAtk += totalAtk;
                    this.appliedDef += totalDef;
                    this.activeBuff = true;
                    gameEngine.registerCombatEndCleanup(this::revertBuff);
                }

                System.out.println("Efeito de " + sourceCard.getCardName() + " ativado (Ao jogar " + targetCardType.name() + " x" + matchCount + "): +ESP " + totalEsp + ", +ATQ " + totalAtk + ", +DEF " + totalDef + "!");
                gameEngine.captureSnapshot(sourceCard, this, "EFFECT_RESOLVED");
            }
        }
    }

    private void revertBuff() {
        if (temporary && activeBuff && buffedHero != null) {
            buffedHero.setEspirito(buffedHero.getEspirito() - appliedEsp);
            buffedHero.setAtaque(buffedHero.getAtaque() - appliedAtk);
            buffedHero.setDefesa(buffedHero.getDefesa() - appliedDef);
            System.out.println("-> Buff temporário de " + buffedHero.getCardName() + " expirou no Final do Combate.");
            appliedEsp = 0;
            appliedAtk = 0;
            appliedDef = 0;
            activeBuff = false;
            buffedHero = null;
        }
    }
}

package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import java.util.List;

public class OnPlaySpellBuffEffect implements Effect {

    private final int bonusEspirito;
    private final int bonusAtaque;
    private final int bonusDefesa;
    private final boolean temporary;

    private HeroCard buffedHero;
    private int appliedEsp = 0;
    private int appliedAtk = 0;
    private int appliedDef = 0;
    private boolean activeBuff = false;

    public OnPlaySpellBuffEffect(int bonusEspirito, int bonusAtaque, int bonusDefesa) {
        this(bonusEspirito, bonusAtaque, bonusDefesa, false);
    }

    public OnPlaySpellBuffEffect(int bonusEspirito, int bonusAtaque, int bonusDefesa, boolean temporary) {
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
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        if (gameEngine.getTurnPhase() == TurnPhase.COMBAT_END) {
            revertBuff();
            return;
        }

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        List<Card> playedCards = isPlayer1
                ? gameEngine.getGameState().getPlayer1PlayedCards()
                : gameEngine.getGameState().getPlayer2PlayedCards();

        long spellCount = playedCards.stream()
                .filter(c -> c.getCardType() == CardType.SPELL)
                .count();

        if (spellCount > 0) {
            java.util.UUID myHeroId = isPlayer1
                    ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                    : gameEngine.getGameState().getActiveHeroPlayer2Id();

            if (myHeroId == null || !myHeroId.equals(sourceCard.getUuid())) {
                return;
            }

            com.alkateca.lendasdapoeira.entity.Player myPlayer = isPlayer1
                    ? gameEngine.getPlayer1()
                    : gameEngine.getPlayer2();

            HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                    .filter(h -> h.getUuid().equals(myHeroId))
                    .findFirst()
                    .orElse(null);

            if (myHero != null) {
                int totalEsp = bonusEspirito * (int) spellCount;
                int totalAtk = bonusAtaque * (int) spellCount;
                int totalDef = bonusDefesa * (int) spellCount;
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

                System.out.println("Efeito de " + sourceCard.getCardName() + " ativado (Ao jogar Magia x" + spellCount + "): +" + totalAtk + " Ataque!");
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

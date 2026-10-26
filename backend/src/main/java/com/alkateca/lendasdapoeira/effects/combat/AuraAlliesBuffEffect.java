package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class AuraAlliesBuffEffect implements Effect {

    private final int bonusEspirito;
    private final int bonusAtaque;
    private final int bonusDefesa;
    private boolean applied = false;

    public AuraAlliesBuffEffect(int bonusEspirito, int bonusAtaque, int bonusDefesa) {
        this.bonusEspirito = bonusEspirito;
        this.bonusAtaque = bonusAtaque;
        this.bonusDefesa = bonusDefesa;
    }

    @Override
    public boolean canTriggerFromBench() {
        return true;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return !applied && (turnPhase == TurnPhase.GAME_START
                || turnPhase == TurnPhase.COMBAT_START
                || turnPhase == TurnPhase.RESOLUTION);
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        if (applied) return;
        System.out.println("Aura de " + sourceCard.getCardName() + " ativada!");
        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        com.alkateca.lendasdapoeira.entity.Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (myPlayer != null && myPlayer.getCurrentDeck() != null && myPlayer.getCurrentDeck().getHeroList() != null) {
            for (Card card : myPlayer.getCurrentDeck().getHeroList()) {
                HeroCard hero = (HeroCard) card;
                hero.setEspirito(hero.getEspirito() + bonusEspirito);
                hero.setAtaque(hero.getAtaque() + bonusAtaque);
                hero.setDefesa(hero.getDefesa() + bonusDefesa);
            }
            applied = true;
            System.out.println("-> Todos os aliados receberam o buff de Aura!");
        }
    }
}

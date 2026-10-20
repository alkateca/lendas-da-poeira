package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.Ordem;
import com.alkateca.lendasdapoeira.enums.Raca;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class AuraRaceBuffEffect implements Effect {

    private final Raca targetRaca;
    private final int bonusEspirito;
    private final int bonusAtaque;
    private final int bonusDefesa;
    private boolean applied = false;

    public AuraRaceBuffEffect(Raca targetRaca, int bonusEspirito, int bonusAtaque, int bonusDefesa) {
        this.targetRaca = targetRaca;
        this.bonusEspirito = bonusEspirito;
        this.bonusAtaque = bonusAtaque;
        this.bonusDefesa = bonusDefesa;
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
        Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (myPlayer != null && myPlayer.getCurrentDeck() != null && myPlayer.getCurrentDeck().getHeroList() != null) {
            for (Card card : myPlayer.getCurrentDeck().getHeroList()) {
                if (card instanceof HeroCard) {
                    HeroCard hero = (HeroCard) card;
                    if (matchesTargetRaca(hero)) {
                        hero.setEspirito(hero.getEspirito() + bonusEspirito);
                        hero.setAtaque(hero.getAtaque() + bonusAtaque);
                        hero.setDefesa(hero.getDefesa() + bonusDefesa);
                        System.out.println("-> " + hero.getCardName() + " recebeu buff de Aura de "
                                + sourceCard.getCardName() + "! (+Atk: " + bonusAtaque + ", +Def: " + bonusDefesa + ")");
                    }
                }
            }
            applied = true;
        }
    }

    private boolean matchesTargetRaca(HeroCard hero) {
        boolean hasRaca = hero.getRacas() != null && hero.getRacas().contains(targetRaca);
        boolean hasThroneOrder = targetRaca == Raca.GUARDIOES_DO_TRONO
                && hero.getOrdem() != null
                && hero.getOrdem().contains(Ordem.GUARDAO_DO_TRONO);
        return hasRaca || hasThroneOrder;
    }
}

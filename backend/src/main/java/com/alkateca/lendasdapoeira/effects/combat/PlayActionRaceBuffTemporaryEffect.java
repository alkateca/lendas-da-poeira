package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.Raca;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class PlayActionRaceBuffTemporaryEffect implements Effect {

    private final Raca targetRace;
    private final int bonusEspirito;
    private final int bonusAtaque;
    private final int bonusDefesa;

    public PlayActionRaceBuffTemporaryEffect(Raca targetRace, int bonusEspirito, int bonusAtaque, int bonusDefesa) {
        this.targetRace = targetRace;
        this.bonusEspirito = bonusEspirito;
        this.bonusAtaque = bonusAtaque;
        this.bonusDefesa = bonusDefesa;
    }

    public Raca getTargetRace() {
        return targetRace;
    }

    public int getBonusEspirito() {
        return bonusEspirito;
    }

    public int getBonusAtaque() {
        return bonusAtaque;
    }

    public int getBonusDefesa() {
        return bonusDefesa;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (myPlayer == null || myPlayer.getCurrentDeck() == null
                || myPlayer.getCurrentDeck().getHeroList() == null) {
            return;
        }

        int bonusGoblin = gameEngine.getGoblinEffectBonus(sourceCard);
        int finalEsp = bonusEspirito > 0 ? bonusEspirito + bonusGoblin : bonusEspirito;
        int finalAtk = bonusAtaque > 0 ? bonusAtaque + bonusGoblin : bonusAtaque;
        int finalDef = bonusDefesa > 0 ? bonusDefesa + bonusGoblin : bonusDefesa;

        for (HeroCard allyHero : myPlayer.getCurrentDeck().getHeroList()) {
            if (allyHero != null
                    && Boolean.TRUE.equals(allyHero.getEstaVivo())
                    && allyHero.getVidaAtual() > 0
                    && allyHero.getRacas() != null
                    && allyHero.getRacas().contains(targetRace)) {

                allyHero.setEspirito(allyHero.getEspirito() + finalEsp);
                allyHero.setAtaque(allyHero.getAtaque() + finalAtk);
                allyHero.setDefesa(allyHero.getDefesa() + finalDef);

                gameEngine.registerCombatEndCleanup(() -> {
                    allyHero.setEspirito(allyHero.getEspirito() - finalEsp);
                    allyHero.setAtaque(allyHero.getAtaque() - finalAtk);
                    allyHero.setDefesa(allyHero.getDefesa() - finalDef);
                });

                System.out.println("-> " + allyHero.getCardName() + " (" + targetRace + ") recebeu +" +
                        finalEsp + " ESP, +" + finalAtk + " ATQ e +" + finalDef +
                        " DEF até o final do combate! Vida restante: " + allyHero.getVidaAtual());
            }
        }
    }
}

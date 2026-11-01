package com.alkateca.lendasdapoeira.effects.startOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.UUID;

public class StartOfCombatBuffPerDeadAllyHeroEffect implements Effect {

    private final int bonusEspirito;
    private final int bonusAtaque;
    private final int bonusDefesa;

    public StartOfCombatBuffPerDeadAllyHeroEffect(int bonusEspirito, int bonusAtaque, int bonusDefesa) {
        this.bonusEspirito = bonusEspirito;
        this.bonusAtaque = bonusAtaque;
        this.bonusDefesa = bonusDefesa;
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
        return turnPhase == TurnPhase.COMBAT_START;
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
                || myPlayer.getCurrentDeck().getHeroList() == null) {
            return;
        }

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        if (myHero == null) {
            return;
        }

        int deadCount = (int) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h != null && (h.getVidaAtual() <= 0 || !Boolean.TRUE.equals(h.getEstaVivo())))
                .count();

        if (deadCount > 0) {
            int totalEsp = bonusEspirito * deadCount;
            int totalAtk = bonusAtaque * deadCount;
            int totalDef = bonusDefesa * deadCount;

            myHero.setEspirito(myHero.getEspirito() + totalEsp);
            myHero.setAtaque(myHero.getAtaque() + totalAtk);
            myHero.setDefesa(myHero.getDefesa() + totalDef);

            gameEngine.registerCombatEndCleanup(() -> {
                myHero.setEspirito(myHero.getEspirito() - totalEsp);
                myHero.setAtaque(myHero.getAtaque() - totalAtk);
                myHero.setDefesa(myHero.getDefesa() - totalDef);
            });

            System.out.println("-> " + myHero.getCardName() + " recebeu +" + totalEsp + " ESP, +"
                    + totalAtk + " ATQ e +" + totalDef + " DEF por " + deadCount
                    + " herói(s) aliado(s) morto(s)! Vida restante: " + myHero.getVidaAtual());
        } else {
            System.out.println("-> Nenhum herói aliado morto. Nenhum bónus aplicado.");
        }
    }
}

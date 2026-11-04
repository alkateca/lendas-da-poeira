package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class PlayActionHealAlliesIfHeroActiveEffect implements Effect {

    private final String requiredHeroName;
    private final int healAmount;

    public PlayActionHealAlliesIfHeroActiveEffect(String requiredHeroName, int healAmount) {
        this.requiredHeroName = requiredHeroName;
        this.healAmount = healAmount;
    }

    public String getRequiredHeroName() {
        return requiredHeroName;
    }

    public int getHealAmount() {
        return healAmount;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        java.util.UUID myHeroId = isPlayer1
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

        if (myHero != null && requiredHeroName != null
                && requiredHeroName.equalsIgnoreCase(myHero.getCardName())) {

            int totalHeal = healAmount + gameEngine.getGoblinEffectBonus(sourceCard);

            for (HeroCard allyHero : myPlayer.getCurrentDeck().getHeroList()) {
                if (allyHero != null && Boolean.TRUE.equals(allyHero.getEstaVivo())) {
                    int oldHp = allyHero.getVidaAtual();
                    int newHp = Math.min(allyHero.getVidaMaxima(), oldHp + totalHeal);
                    int recovered = Math.max(0, newHp - oldHp);
                    allyHero.setVidaAtual(newHp);

                    System.out.println("-> " + allyHero.getCardName() + " recuperou " + recovered +
                            " pontos de vida (" + requiredHeroName + " ativo)! Vida restante: " +
                            allyHero.getVidaAtual());
                }
            }
        } else {
            System.out.println("-> O Herói Aliado ativo não é " + requiredHeroName + ". Nenhuma cura aplicada.");
        }
    }
}

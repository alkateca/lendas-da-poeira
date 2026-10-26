package com.alkateca.lendasdapoeira.effects.startOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.UUID;

public class StartOfCombatSelfDamageToHealHeroEffect implements Effect {

    private final int selfDirectDamage;
    private final int healAmount;
    private final String targetHeroNamePrefix;

    public StartOfCombatSelfDamageToHealHeroEffect(int selfDirectDamage, int healAmount, String targetHeroNamePrefix) {
        this.selfDirectDamage = selfDirectDamage;
        this.healAmount = healAmount;
        this.targetHeroNamePrefix = targetHeroNamePrefix;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_START;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");
        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();

        Player allyPlayer = isPlayer1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();

        HeroCard myHero = (HeroCard) allyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        if (myHero != null) {
            myHero.setVidaAtual(myHero.getVidaAtual() - selfDirectDamage);
            System.out.println("-> " + myHero.getCardName() + " sofreu " + selfDirectDamage +
                    " de Dano Direto! Vida restante: " + myHero.getVidaAtual());
        }

        HeroCard targetAlly = allyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h != null
                        && Boolean.TRUE.equals(h.getEstaVivo())
                        && h.getCardName() != null
                        && h.getCardName().startsWith(targetHeroNamePrefix))
                .findFirst()
                .orElse(null);

        if (targetAlly != null) {
            int newLife = Math.min(targetAlly.getVidaAtual() + healAmount, targetAlly.getVidaMaxima());
            targetAlly.setVidaAtual(newLife);
            System.out.println("-> " + targetAlly.getCardName() + " recuperou até " + healAmount +
                    " de Vida! Vida restante: " + targetAlly.getVidaAtual());
        } else {
            System.out.println("-> Aliado " + targetHeroNamePrefix + " não está disponível para receber a cura.");
        }
    }
}

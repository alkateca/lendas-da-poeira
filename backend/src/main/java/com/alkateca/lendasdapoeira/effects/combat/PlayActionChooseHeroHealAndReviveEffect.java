package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayActionChooseHeroHealAndReviveEffect implements Effect {

    private final int healAmount;

    public PlayActionChooseHeroHealAndReviveEffect(int healAmount) {
        this.healAmount = healAmount;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        Player myPlayer = isPlayer1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();

        if (myPlayer == null || myPlayer.getCurrentDeck() == null || myPlayer.getCurrentDeck().getHeroList() == null) {
            return;
        }

        List<UUID> validHeroUuids = myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(c -> c instanceof HeroCard)
                .map(Card::getUuid)
                .collect(Collectors.toList());

        gameEngine.requestPlayerChoice(
                myPlayer.getUuid(),
                sourceCard,
                "ALLY_HERO",
                "Escolha um Herói aliado para recuperar " + healAmount + " de Vida (revive se a Vida ficar acima de 0)",
                validHeroUuids,
                false,
                chosenHeroUuid -> {
                    if (chosenHeroUuid == null) return;
                    HeroCard chosenHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                            .filter(c -> c instanceof HeroCard && c.getUuid().equals(chosenHeroUuid))
                            .findFirst()
                            .orElse(null);

                    if (chosenHero != null) {
                        boolean wasDead = !Boolean.TRUE.equals(chosenHero.getEstaVivo());
                        int healedLife = Math.min(chosenHero.getVidaAtual() + healAmount, chosenHero.getVidaMaxima());
                        chosenHero.setVidaAtual(healedLife);

                        if (wasDead && chosenHero.getVidaAtual() > 0) {
                            chosenHero.setEstaVivo(true);
                            chosenHero.setEstaAtivo(true);
                            if (chosenHero.getZoneId() != ZoneId.BATTLE) {
                                chosenHero.setZoneId(ZoneId.BENCH);
                            }
                            System.out.println("-> " + chosenHero.getCardName() + " recuperou " + healAmount +
                                    " de Vida e foi REVIVIDO com " + chosenHero.getVidaAtual() + " de Vida!");
                        } else {
                            System.out.println("-> " + chosenHero.getCardName() + " recuperou " + healAmount +
                                    " de Vida! Vida restante: " + chosenHero.getVidaAtual());
                        }
                    }
                }
        );
    }
}

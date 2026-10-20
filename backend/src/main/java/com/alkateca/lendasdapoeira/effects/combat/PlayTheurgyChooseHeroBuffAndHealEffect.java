package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.Ordem;
import com.alkateca.lendasdapoeira.enums.Raca;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlayTheurgyChooseHeroBuffAndHealEffect implements Effect {

    private final int bonusAtaque;
    private final int healAmount;

    public PlayTheurgyChooseHeroBuffAndHealEffect(int bonusAtaque, int healAmount) {
        this.bonusAtaque = bonusAtaque;
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

        UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        if (myHero != null && isGuardiaoDoTrono(myHero)) {
            myHero.setAtaque(myHero.getAtaque() + bonusAtaque);
            gameEngine.registerCombatEndCleanup(() -> {
                myHero.setAtaque(myHero.getAtaque() - bonusAtaque);
                System.out.println("-> Buff adicional de Guardião do Trono (" + sourceCard.getCardName() + ") expirou em " + myHero.getCardName() + ".");
            });
            int newLife = Math.min(myHero.getVidaAtual() + healAmount, myHero.getVidaMaxima());
            myHero.setVidaAtual(newLife);
            System.out.println("-> Jogado por Guardião do Trono (" + myHero.getCardName() + "): +" + bonusAtaque +
                    " de Ataque até o Final do Combate e curou " + healAmount + " de Vida (Vida: " + myHero.getVidaAtual() + ")!");
        }

        List<UUID> validHeroUuids = myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(c -> c instanceof HeroCard && Boolean.TRUE.equals(((HeroCard) c).getEstaVivo()))
                .map(Card::getUuid)
                .collect(Collectors.toList());

        gameEngine.requestPlayerChoice(
                myPlayer.getUuid(),
                sourceCard,
                "ALLY_HERO",
                "Escolha um Herói aliado para receber +" + bonusAtaque + " de Ataque até o Final do Combate e curar " + healAmount + " pontos",
                validHeroUuids,
                false,
                chosenHeroUuid -> {
                    if (chosenHeroUuid == null) return;
                    HeroCard chosenHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                            .filter(c -> c instanceof HeroCard && c.getUuid().equals(chosenHeroUuid))
                            .findFirst()
                            .orElse(null);

                    if (chosenHero != null && Boolean.TRUE.equals(chosenHero.getEstaVivo())) {
                        chosenHero.setAtaque(chosenHero.getAtaque() + bonusAtaque);
                        gameEngine.registerCombatEndCleanup(() -> {
                            chosenHero.setAtaque(chosenHero.getAtaque() - bonusAtaque);
                            System.out.println("-> Buff temporário de " + sourceCard.getCardName() + " expirou em " + chosenHero.getCardName() + ".");
                        });
                        int healedLife = Math.min(chosenHero.getVidaAtual() + healAmount, chosenHero.getVidaMaxima());
                        chosenHero.setVidaAtual(healedLife);
                        System.out.println("-> " + chosenHero.getCardName() + " foi escolhido por " + sourceCard.getCardName() +
                                ": +" + bonusAtaque + " de Ataque até o Final do Combate e curou para " + chosenHero.getVidaAtual() + " de Vida!");
                    }
                }
        );
    }

    private boolean isGuardiaoDoTrono(HeroCard hero) {
        boolean hasRaca = hero.getRacas() != null && hero.getRacas().contains(Raca.GUARDIOES_DO_TRONO);
        boolean hasOrdem = hero.getOrdem() != null && hero.getOrdem().contains(Ordem.GUARDAO_DO_TRONO);
        return hasRaca || hasOrdem;
    }
}

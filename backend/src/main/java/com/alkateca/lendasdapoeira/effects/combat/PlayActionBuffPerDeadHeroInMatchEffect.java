package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.ArrayList;
import java.util.List;

public class PlayActionBuffPerDeadHeroInMatchEffect implements Effect {

    private final int bonusAtaquePerDead;
    private final int bonusDefesaPerDead;

    public PlayActionBuffPerDeadHeroInMatchEffect(int bonusAtaquePerDead, int bonusDefesaPerDead) {
        this.bonusAtaquePerDead = bonusAtaquePerDead;
        this.bonusDefesaPerDead = bonusDefesaPerDead;
    }

    public int getBonusAtaquePerDead() {
        return bonusAtaquePerDead;
    }

    public int getBonusDefesaPerDead() {
        return bonusDefesaPerDead;
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

        com.alkateca.lendasdapoeira.entity.Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        if (myHero == null) {
            return;
        }

        List<HeroCard> allHeroes = new ArrayList<>();
        if (gameEngine.getPlayer1() != null && gameEngine.getPlayer1().getCurrentDeck() != null
                && gameEngine.getPlayer1().getCurrentDeck().getHeroList() != null) {
            allHeroes.addAll(gameEngine.getPlayer1().getCurrentDeck().getHeroList());
        }
        if (gameEngine.getPlayer2() != null && gameEngine.getPlayer2().getCurrentDeck() != null
                && gameEngine.getPlayer2().getCurrentDeck().getHeroList() != null) {
            allHeroes.addAll(gameEngine.getPlayer2().getCurrentDeck().getHeroList());
        }

        int deadHeroesCount = (int) allHeroes.stream()
                .filter(h -> h != null && (!Boolean.TRUE.equals(h.getEstaVivo()) || h.getVidaAtual() <= 0))
                .count();

        if (deadHeroesCount > 0) {
            int totalBonusAtaque = deadHeroesCount * bonusAtaquePerDead;
            int totalBonusDefesa = deadHeroesCount * bonusDefesaPerDead;

            myHero.setAtaque(myHero.getAtaque() + totalBonusAtaque);
            myHero.setDefesa(myHero.getDefesa() + totalBonusDefesa);

            gameEngine.registerCombatEndCleanup(() -> {
                myHero.setAtaque(myHero.getAtaque() - totalBonusAtaque);
                myHero.setDefesa(myHero.getDefesa() - totalBonusDefesa);
            });

            System.out.println("-> " + myHero.getCardName() + " recebeu +" + totalBonusAtaque +
                    " de Ataque e +" + totalBonusDefesa + " de Defesa até o final do combate (" +
                    deadHeroesCount + " herói(s) morto(s) na partida)! Vida restante: " + myHero.getVidaAtual());
        } else {
            System.out.println("-> Nenhum herói morto na partida. Nenhum bónus concedido.");
        }
    }
}

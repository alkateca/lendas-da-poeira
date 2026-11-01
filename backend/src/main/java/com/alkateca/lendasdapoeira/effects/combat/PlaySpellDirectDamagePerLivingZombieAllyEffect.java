package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.Raca;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class PlaySpellDirectDamagePerLivingZombieAllyEffect implements Effect {

    private final int directDamagePerZombie;

    public PlaySpellDirectDamagePerLivingZombieAllyEffect(int directDamagePerZombie) {
        this.directDamagePerZombie = directDamagePerZombie;
    }

    public int getDirectDamagePerZombie() {
        return directDamagePerZombie;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        java.util.UUID enemyHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                : gameEngine.getGameState().getActiveHeroPlayer1Id();

        com.alkateca.lendasdapoeira.entity.Player enemyPlayer = isPlayer1
                ? gameEngine.getPlayer2()
                : gameEngine.getPlayer1();

        HeroCard enemyHero = (HeroCard) enemyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(enemyHeroId))
                .findFirst()
                .orElse(null);

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

        if (myPlayer.getCurrentDeck() == null || myPlayer.getCurrentDeck().getHeroList() == null) {
            return;
        }

        int livingZombieCount = (int) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h != null
                        && Boolean.TRUE.equals(h.getEstaVivo())
                        && h.getVidaAtual() > 0
                        && isZombieHero(h))
                .count();

        int totalDamage = livingZombieCount * directDamagePerZombie;

        if (totalDamage > 0 && enemyHero != null) {
            int finalDamage = gameEngine.calculateDirectDamage(myHero, enemyHero, totalDamage);
            enemyHero.setVidaAtual(enemyHero.getVidaAtual() - finalDamage);

            System.out.println("-> " + enemyHero.getCardName() + " sofreu " + finalDamage
                    + " de dano direto (" + livingZombieCount + " aliado(s) Zumbi vivo(s))! Vida restante: "
                    + enemyHero.getVidaAtual());
        } else {
            System.out.println("-> Nenhum aliado Zumbi vivo ou alvo inválido. Nenhum dano direto causado.");
        }
    }

    private boolean isZombieHero(HeroCard hero) {
        boolean hasZombieRace = hero.getRacas() != null && hero.getRacas().contains(Raca.ZUMBI);
        boolean hasZombieOrder = hero.getOrdem() != null && hero.getOrdem().stream()
                .anyMatch(o -> o != null && o.name().equalsIgnoreCase("ZUMBI"));
        return hasZombieRace || hasZombieOrder;
    }
}

package com.alkateca.lendasdapoeira.effects.startOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.UUID;

public class StartOfCombatBuffStatsByEnemyMissingHealthEffect implements Effect {

    private final int maxBonus;
    private int appliedBonus = 0;

    public StartOfCombatBuffStatsByEnemyMissingHealthEffect(int maxBonus) {
        this.maxBonus = maxBonus;
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
        UUID enemyHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                : gameEngine.getGameState().getActiveHeroPlayer1Id();

        Player myPlayer = isPlayer1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();
        Player enemyPlayer = isPlayer1 ? gameEngine.getPlayer2() : gameEngine.getPlayer1();

        if (myPlayer == null || enemyPlayer == null || myHeroId == null || enemyHeroId == null) {
            return;
        }

        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId))
                .findFirst()
                .orElse(null);

        HeroCard enemyHero = (HeroCard) enemyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(enemyHeroId))
                .findFirst()
                .orElse(null);

        if (myHero != null && enemyHero != null) {
            int missingHealth = Math.max(0, enemyHero.getVidaMaxima() - enemyHero.getVidaAtual());
            int bonus = Math.min(this.maxBonus, missingHealth);

            if (bonus > 0) {
                this.appliedBonus = bonus;
                myHero.setEspirito(myHero.getEspirito() + bonus);
                myHero.setAtaque(myHero.getAtaque() + bonus);
                myHero.setDefesa(myHero.getDefesa() + bonus);

                System.out.println("-> " + myHero.getCardName() + " recebeu +" + bonus +
                        " de Espírito, Ataque e Defesa até o Final do Combate baseado na Vida perdida de " +
                        enemyHero.getCardName() + "!");

                gameEngine.registerCombatEndCleanup(() -> {
                    if (this.appliedBonus > 0) {
                        myHero.setEspirito(myHero.getEspirito() - this.appliedBonus);
                        myHero.setAtaque(myHero.getAtaque() - this.appliedBonus);
                        myHero.setDefesa(myHero.getDefesa() - this.appliedBonus);
                        System.out.println("-> Buff temporário de " + sourceCard.getCardName() +
                                " (-" + this.appliedBonus + " ESP/ATQ/DEF) expirou no Final do Combate.");
                        this.appliedBonus = 0;
                    }
                });
            } else {
                System.out.println("-> " + enemyHero.getCardName() + " não possui dano sofrido (+0 de bônus).");
            }
        }
    }

    @Override
    public void onDetach(GameEngine gameEngine, Card sourceCard, HeroCard targetHero) {
        if (targetHero != null && this.appliedBonus > 0) {
            targetHero.setEspirito(targetHero.getEspirito() - this.appliedBonus);
            targetHero.setAtaque(targetHero.getAtaque() - this.appliedBonus);
            targetHero.setDefesa(targetHero.getDefesa() - this.appliedBonus);
            System.out.println("-> " + sourceCard.getCardName() + " foi desanexado de " + targetHero.getCardName() +
                    "! Buff temporário removido (-" + this.appliedBonus + " ESP/ATQ/DEF).");
            this.appliedBonus = 0;
        }
    }
}

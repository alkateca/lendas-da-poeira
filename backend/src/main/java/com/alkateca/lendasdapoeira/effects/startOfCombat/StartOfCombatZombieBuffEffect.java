package com.alkateca.lendasdapoeira.effects.startOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.Raca;

public class StartOfCombatZombieBuffEffect implements Effect {

    private final int espiritoBuff;
    private final int ataqueBuff;
    private final int healAmount;

    public StartOfCombatZombieBuffEffect(int espiritoBuff, int ataqueBuff, int healAmount) {
        this.espiritoBuff = espiritoBuff;
        this.ataqueBuff = ataqueBuff;
        this.healAmount = healAmount;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_START;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        java.util.UUID activeHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();

        com.alkateca.lendasdapoeira.entity.Player allyPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        HeroCard activeHero = (HeroCard) allyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(activeHeroId))
                .findFirst()
                .orElse(null);

        if (activeHero != null) {
            long zombieCount = allyPlayer.getCurrentDeck().getHeroList().stream()
                    .filter(c -> c instanceof HeroCard)
                    .map(c -> (HeroCard) c)
                    .filter(h -> Boolean.TRUE.equals(h.getEstaVivo()) && h.getRacas() != null && h.getRacas().contains(Raca.ZUMBI))
                    .count();

            if (zombieCount > 0) {
                int totalEspiritoBuff = espiritoBuff * (int) zombieCount;
                int totalAtaqueBuff = ataqueBuff * (int) zombieCount;
                int totalHeal = healAmount * (int) zombieCount;

                activeHero.setEspirito(activeHero.getEspirito() + totalEspiritoBuff);
                activeHero.setAtaque(activeHero.getAtaque() + totalAtaqueBuff);
                
                int currentLife = activeHero.getVidaAtual();
                int maxLife = activeHero.getVidaMaxima();
                int newLife = Math.min(currentLife + totalHeal, maxLife);
                activeHero.setVidaAtual(newLife);

                System.out.println("-> " + activeHero.getCardName() + " recebeu +" + totalEspiritoBuff + " Espírito, +" + totalAtaqueBuff + " Ataque e recuperou " + (newLife - currentLife) + " de Vida por " + zombieCount + " aliado(s) Zumbi(s)!");
            } else {
                System.out.println("-> Nenhum aliado Zumbi encontrado.");
            }
        }
    }
}

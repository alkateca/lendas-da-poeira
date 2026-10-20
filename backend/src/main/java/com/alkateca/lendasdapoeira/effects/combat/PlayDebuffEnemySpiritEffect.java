package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.UUID;

public class PlayDebuffEnemySpiritEffect implements Effect {

    private final int spiritDebuff;
    private HeroCard debuffedHero;
    private boolean activeDebuff = false;

    public PlayDebuffEnemySpiritEffect(int spiritDebuff) {
        this.spiritDebuff = spiritDebuff;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        if (gameEngine.getTurnPhase() == TurnPhase.COMBAT_END) {
            revertDebuff();
            return;
        }

        if (sourceCard.getItemType() == ItemType.ARMA && sourceCard.getZoneId() != ZoneId.ATTACHED) {
            return;
        }

        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");
        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        UUID enemyHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                : gameEngine.getGameState().getActiveHeroPlayer1Id();
        Player enemyPlayer = isPlayer1
                ? gameEngine.getPlayer2()
                : gameEngine.getPlayer1();

        HeroCard enemyHero = (HeroCard) enemyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> enemyHeroId != null
                        ? h.getUuid().equals(enemyHeroId)
                        : Boolean.TRUE.equals(((HeroCard) h).getEstaVivo()))
                .findFirst()
                .orElse(null);

        if (enemyHero != null) {
            enemyHero.setEspirito(enemyHero.getEspirito() - spiritDebuff);
            this.debuffedHero = enemyHero;
            this.activeDebuff = true;
            gameEngine.registerCombatEndCleanup(this::revertDebuff);
            System.out.println("-> O Espírito de " + enemyHero.getCardName() + " foi reduzido em " + spiritDebuff +
                    " até o Final do Combate! Espírito atual: " + enemyHero.getEspirito());
        }
    }

    private void revertDebuff() {
        if (activeDebuff && debuffedHero != null) {
            debuffedHero.setEspirito(debuffedHero.getEspirito() + spiritDebuff);
            System.out.println("-> Debuff de Espírito (-" + spiritDebuff + ") de " + debuffedHero.getCardName() +
                    " expirou no Final do Combate! Espírito restaurado: " + debuffedHero.getEspirito());
            activeDebuff = false;
            debuffedHero = null;
        }
    }
}

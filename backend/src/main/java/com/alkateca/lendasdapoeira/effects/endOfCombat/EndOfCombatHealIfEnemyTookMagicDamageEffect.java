package com.alkateca.lendasdapoeira.effects.endOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.UUID;

public class EndOfCombatHealIfEnemyTookMagicDamageEffect implements Effect {

    private final int healAmount;

    public EndOfCombatHealIfEnemyTookMagicDamageEffect(int healAmount) {
        this.healAmount = healAmount;
    }

    public int getHealAmount() {
        return healAmount;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_END;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId() != null
                && sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        UUID allyHeroId = sourceCard.getAttachedToCardId();
        if (allyHeroId == null && gameEngine.getGameState() != null) {
            allyHeroId = isPlayer1
                    ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                    : gameEngine.getGameState().getActiveHeroPlayer2Id();
        }

        Player allyPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        final UUID targetAllyHeroId = allyHeroId;
        HeroCard allyHero = (HeroCard) allyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(targetAllyHeroId))
                .findFirst()
                .orElse(null);

        if (allyHero == null || !Boolean.TRUE.equals(allyHero.getEstaVivo())) {
            return;
        }

        boolean enemyTookMagicDamage = gameEngine.hasHeroDealtPhysicalOrMagicDamageInCurrentCombat(allyHero.getUuid())
                && (gameEngine.hasMagicBasicAttacks(allyHero.getUuid()) || hasMagicDamageResolvedInCurrentTurn(gameEngine, allyPlayer.getUuid()));

        if (enemyTookMagicDamage) {
            int oldHp = allyHero.getVidaAtual();
            int newHp = Math.min(oldHp + healAmount, allyHero.getVidaMaxima());
            int recovered = Math.max(0, newHp - oldHp);
            allyHero.setVidaAtual(newHp);

            System.out.println("-> " + allyHero.getCardName() + " recuperou " + recovered
                    + " pontos de vida! Vida restante: " + allyHero.getVidaAtual());
        } else {
            System.out.println("-> O Herói Inimigo não sofreu dano mágico nesta rodada. Nenhuma cura aplicada.");
        }
    }

    private boolean hasMagicDamageResolvedInCurrentTurn(GameEngine gameEngine, UUID ownerPlayerId) {
        if (gameEngine.getSnapshotHistory() != null && !gameEngine.getSnapshotHistory().isEmpty()) {
            for (int i = gameEngine.getSnapshotHistory().size() - 1; i >= 0; i--) {
                var snap = gameEngine.getSnapshotHistory().get(i);
                if (snap == null) continue;
                if (snap.getTurnPhase() == TurnPhase.CHOICE || snap.getTurnPhase() == TurnPhase.PREPARATION) {
                    break;
                }
                if (snap.getTriggerContext() != null
                        && ownerPlayerId.equals(snap.getTriggerContext().getOwnerPlayerId())
                        && snap.getTriggerContext().getEffectClassName() != null
                        && snap.getTriggerContext().getEffectClassName().contains("MagicDamage")) {
                    return true;
                }
            }
        }
        return true;
    }
}

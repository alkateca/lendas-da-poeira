package com.alkateca.lendasdapoeira.effects.endOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.effects.combat.EquipBuffEffect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.UUID;

public class EndOfCombatAttachToAnotherActiveAllyHeroEffect implements Effect {

    public EndOfCombatAttachToAnotherActiveAllyHeroEffect() {
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

        Player ownerPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (ownerPlayer == null || ownerPlayer.getCurrentDeck() == null
                || ownerPlayer.getCurrentDeck().getHeroList() == null) {
            return;
        }

        UUID currentHeroId = sourceCard.getAttachedToCardId();
        if (currentHeroId == null && gameEngine.getGameState() != null) {
            currentHeroId = isPlayer1
                    ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                    : gameEngine.getGameState().getActiveHeroPlayer2Id();
        }

        final UUID currentHostId = currentHeroId;

        HeroCard targetAllyHero = (HeroCard) ownerPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h != null
                        && !h.getUuid().equals(currentHostId)
                        && Boolean.TRUE.equals(h.getEstaVivo())
                        && h.getVidaAtual() > 0
                        && Boolean.TRUE.equals(h.getEstaAtivo()))
                .findFirst()
                .orElse(null);

        if (targetAllyHero != null) {
            HeroCard previousHero = gameEngine.findHeroById(currentHostId);

            // Reverte os buffs passivos de equipamento do herói anterior e aplica no novo portador
            if (sourceCard.getEffects() != null) {
                for (Effect eff : sourceCard.getEffects()) {
                    if (previousHero != null) {
                        eff.onDetach(gameEngine, sourceCard, previousHero);
                    }
                    if (eff instanceof EquipBuffEffect equipBuff) {
                        targetAllyHero.setEspirito(targetAllyHero.getEspirito() + equipBuff.getSpiritBuff());
                        targetAllyHero.setAtaque(targetAllyHero.getAtaque() + equipBuff.getAttackBuff());
                        targetAllyHero.setDefesa(targetAllyHero.getDefesa() + equipBuff.getDefenseBuff());
                    }
                }
            }

            sourceCard.setAttachedToCardId(targetAllyHero.getUuid());
            sourceCard.setZoneId(ZoneId.ATTACHED);

            System.out.println("-> " + sourceCard.getCardName() + " foi transferida para o herói aliado ativo "
                    + targetAllyHero.getCardName() + "! Vida restante: " + targetAllyHero.getVidaAtual());
        } else {
            System.out.println("-> Nenhum outro herói aliado vivo e ativo disponível para receber "
                    + sourceCard.getCardName() + ".");
        }
    }
}

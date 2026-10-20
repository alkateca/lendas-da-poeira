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

public class EquipBuffEffect implements Effect {

    private final int bonusEspirito;
    private final int bonusAtaque;
    private final int bonusDefesa;
    private final int bonusVidaMaxima;
    private final boolean targetEnemy;
    private final Integer durationTurns; // null = Permanente enquanto anexado | X = dura X turnos

    // Construtor padrão (100% compatível com o CardFactory atual: aliado e permanente)
    public EquipBuffEffect(int bonusEspirito, int bonusAtaque, int bonusDefesa, int bonusVidaMaxima) {
        this(bonusEspirito, bonusAtaque, bonusDefesa, bonusVidaMaxima, false, null);
    }

    // Construtor com duração para o próprio Herói (ex: Buff anexável por X turnos)
    public EquipBuffEffect(int bonusEspirito, int bonusAtaque, int bonusDefesa, int bonusVidaMaxima, Integer durationTurns) {
        this(bonusEspirito, bonusAtaque, bonusDefesa, bonusVidaMaxima, false, durationTurns);
    }

    // Construtor completo (Permite anexar em Inimigo para Debuffs e definir duração em turnos)
    public EquipBuffEffect(int bonusEspirito, int bonusAtaque, int bonusDefesa, int bonusVidaMaxima, boolean targetEnemy, Integer durationTurns) {
        this.bonusEspirito = bonusEspirito;
        this.bonusAtaque = bonusAtaque;
        this.bonusDefesa = bonusDefesa;
        this.bonusVidaMaxima = bonusVidaMaxima;
        this.targetEnemy = targetEnemy;
        this.durationTurns = durationTurns;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        HeroCard targetHero;
        if (targetEnemy) {
            UUID enemyHeroId = isPlayer1
                    ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                    : gameEngine.getGameState().getActiveHeroPlayer1Id();

            Player enemyPlayer = isPlayer1
                    ? gameEngine.getPlayer2()
                    : gameEngine.getPlayer1();

            targetHero = (HeroCard) enemyPlayer.getCurrentDeck().getHeroList().stream()
                    .filter(h -> h.getUuid().equals(enemyHeroId))
                    .findFirst()
                    .orElse(null);
        } else {
            UUID myHeroId = isPlayer1
                    ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                    : gameEngine.getGameState().getActiveHeroPlayer2Id();

            if (myHeroId == null && sourceCard.getAttachedToCardId() != null) {
                myHeroId = sourceCard.getAttachedToCardId();
            }

            Player myPlayer = isPlayer1
                    ? gameEngine.getPlayer1()
                    : gameEngine.getPlayer2();

            UUID finalMyHeroId = myHeroId;
            targetHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                    .filter(h -> h.getUuid().equals(finalMyHeroId))
                    .findFirst()
                    .orElse(null);
        }

        if (targetHero == null) {
            return;
        }

        // Validação de Empunhadura (game_architecture.md Seção 2): Apenas para ARMA
        if (sourceCard.getItemType() == ItemType.ARMA && sourceCard.getEmpunhadura() > 0) {
            int currentUsedSlots = gameEngine.getUsedWeaponSlotsForHero(targetHero.getUuid());
            if (currentUsedSlots + sourceCard.getEmpunhadura() > targetHero.getMaxWeaponSlots()) {
                sourceCard.setAttachedToCardId(null);
                System.out.println("-> Falha ao equipar " + sourceCard.getCardName() + ": Slots de arma insuficientes em " + targetHero.getCardName());
                return;
            }
        }

        // Registra o vínculo de anexo e a duração na carta
        sourceCard.setAttachedToCardId(targetHero.getUuid());
        sourceCard.setTurnsRemaining(this.durationTurns);
        sourceCard.setZoneId(ZoneId.ATTACHED);

        // Aplica os modificadores no Herói alvo
        targetHero.setEspirito(targetHero.getEspirito() + bonusEspirito);
        targetHero.setAtaque(targetHero.getAtaque() + bonusAtaque);
        targetHero.setDefesa(targetHero.getDefesa() + bonusDefesa);
        targetHero.setVidaMaxima(targetHero.getVidaMaxima() + bonusVidaMaxima);
        if (bonusVidaMaxima > 0) {
            targetHero.setVidaAtual(targetHero.getVidaAtual() + bonusVidaMaxima);
        }

        System.out.println("-> " + targetHero.getCardName() + " recebeu anexo " + sourceCard.getCardName() +
                "! (Esp: " + bonusEspirito + ", Atk: " + bonusAtaque + ", Def: " + bonusDefesa +
                ", MaxVida: " + bonusVidaMaxima + ", Turnos: " + (durationTurns == null ? "Permanente" : durationTurns) + ")");
    }

    @Override
    public void onDetach(GameEngine gameEngine, Card sourceCard, HeroCard targetHero) {
        if (targetHero != null) {
            targetHero.setEspirito(targetHero.getEspirito() - bonusEspirito);
            targetHero.setAtaque(targetHero.getAtaque() - bonusAtaque);
            targetHero.setDefesa(targetHero.getDefesa() - bonusDefesa);
            targetHero.setVidaMaxima(targetHero.getVidaMaxima() - bonusVidaMaxima);
            if (targetHero.getVidaAtual() > targetHero.getVidaMaxima()) {
                targetHero.setVidaAtual(targetHero.getVidaMaxima());
            }

            System.out.println("-> " + sourceCard.getCardName() + " foi desanexado de " + targetHero.getCardName() +
                    "! Modificadores removidos (Esp: " + (-bonusEspirito) + ", Atk: " + (-bonusAtaque) +
                    ", Def: " + (-bonusDefesa) + ", MaxVida: " + (-bonusVidaMaxima) + ")");
        }
    }
}
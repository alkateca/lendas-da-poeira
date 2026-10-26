package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.UUID;

public class PlayPermanentStatModifierEffect implements Effect {

    private final int deltaEspirito;
    private final int deltaAtaque;
    private final int deltaDefesa;
    private final boolean targetEnemy;
    private final boolean affectAllHeroes;

    public PlayPermanentStatModifierEffect(int deltaEspirito, int deltaAtaque, int deltaDefesa, boolean targetEnemy, boolean affectAllHeroes) {
        this.deltaEspirito = deltaEspirito;
        this.deltaAtaque = deltaAtaque;
        this.deltaDefesa = deltaDefesa;
        this.targetEnemy = targetEnemy;
        this.affectAllHeroes = affectAllHeroes;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");
        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        Player targetPlayer;
        UUID activeHeroId;
        if (targetEnemy) {
            targetPlayer = isPlayer1 ? gameEngine.getPlayer2() : gameEngine.getPlayer1();
            activeHeroId = isPlayer1
                    ? gameEngine.getGameState().getActiveHeroPlayer2Id()
                    : gameEngine.getGameState().getActiveHeroPlayer1Id();
        } else {
            targetPlayer = isPlayer1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();
            activeHeroId = isPlayer1
                    ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                    : gameEngine.getGameState().getActiveHeroPlayer2Id();
        }

        if (targetPlayer == null || targetPlayer.getCurrentDeck() == null || targetPlayer.getCurrentDeck().getHeroList() == null) {
            return;
        }

        if (affectAllHeroes) {
            for (Card c : targetPlayer.getCurrentDeck().getHeroList()) {
                if (c instanceof HeroCard hero && Boolean.TRUE.equals(hero.getEstaVivo())) {
                    applyModifiers(hero);
                }
            }
        } else {
            HeroCard activeHero = (HeroCard) targetPlayer.getCurrentDeck().getHeroList().stream()
                    .filter(h -> activeHeroId != null && h.getUuid().equals(activeHeroId))
                    .findFirst()
                    .orElse(null);
            if (activeHero != null) {
                applyModifiers(activeHero);
            }
        }
    }

    private void applyModifiers(HeroCard hero) {
        hero.setEspirito(hero.getEspirito() + deltaEspirito);
        hero.setAtaque(hero.getAtaque() + deltaAtaque);
        hero.setDefesa(hero.getDefesa() + deltaDefesa);
        System.out.println("-> " + hero.getCardName() + " recebeu modificador permanente (Esp: " + deltaEspirito +
                ", Atk: " + deltaAtaque + ", Def: " + deltaDefesa + ") até o Final da Partida!");
    }
}

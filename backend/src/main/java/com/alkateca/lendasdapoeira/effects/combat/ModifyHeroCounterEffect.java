package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CounterType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Efeito genérico e extensível para adicionar ou remover contadores em Heróis alvo
 * em qualquer gatilho de fase ({@link TurnPhase}).
 */
public class ModifyHeroCounterEffect implements Effect {

    public enum CounterTargetScope {
        SELF_OR_ATTACHED_HERO,
        ACTIVE_ALLY_HERO,
        ACTIVE_ENEMY_HERO,
        ALL_ALLY_HEROES,
        ALL_ENEMY_HEROES,
        ALL_LIVING_HEROES
    }

    private final TurnPhase triggerPhase;
    private final CounterTargetScope targetScope;
    private final CounterType counterType;
    private final int delta;

    public ModifyHeroCounterEffect(
            TurnPhase triggerPhase,
            CounterTargetScope targetScope,
            CounterType counterType,
            int delta) {
        this.triggerPhase = triggerPhase != null ? triggerPhase : TurnPhase.RESOLUTION;
        this.targetScope = targetScope != null ? targetScope : CounterTargetScope.ACTIVE_ALLY_HERO;
        this.counterType = counterType;
        this.delta = delta;
    }

    public ModifyHeroCounterEffect(CounterTargetScope targetScope, CounterType counterType, int delta) {
        this(TurnPhase.RESOLUTION, targetScope, counterType, delta);
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == this.triggerPhase;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        if (gameEngine == null || sourceCard == null || counterType == null || delta == 0) {
            return;
        }
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        List<HeroCard> targets = resolveTargets(gameEngine, sourceCard);
        for (HeroCard target : targets) {
            if (target == null || !Boolean.TRUE.equals(target.getEstaVivo())) {
                continue;
            }
            if (delta > 0) {
                gameEngine.addCounter(target, counterType, delta);
            } else {
                gameEngine.removeCounter(target, counterType, -delta);
            }
        }
    }

    private List<HeroCard> resolveTargets(GameEngine engine, Card sourceCard) {
        List<HeroCard> targets = new ArrayList<>();
        if (sourceCard.getOwnerId() == null || engine.getPlayer1() == null || engine.getPlayer2() == null) {
            return targets;
        }

        boolean isPlayer1 = sourceCard.getOwnerId().equals(engine.getPlayer1().getUuid());
        Player allyPlayer = isPlayer1 ? engine.getPlayer1() : engine.getPlayer2();
        Player enemyPlayer = isPlayer1 ? engine.getPlayer2() : engine.getPlayer1();
        UUID activeAllyId = isPlayer1
                ? engine.getGameState().getActiveHeroPlayer1Id()
                : engine.getGameState().getActiveHeroPlayer2Id();
        UUID activeEnemyId = isPlayer1
                ? engine.getGameState().getActiveHeroPlayer2Id()
                : engine.getGameState().getActiveHeroPlayer1Id();

        switch (targetScope) {
            case SELF_OR_ATTACHED_HERO -> {
                if (sourceCard instanceof HeroCard selfHero) {
                    targets.add(selfHero);
                } else if (sourceCard.getAttachedToCardId() != null) {
                    HeroCard host = findHeroInPlayer(allyPlayer, sourceCard.getAttachedToCardId());
                    if (host == null) {
                        host = findHeroInPlayer(enemyPlayer, sourceCard.getAttachedToCardId());
                    }
                    if (host != null) targets.add(host);
                } else {
                    HeroCard activeAlly = findHeroInPlayer(allyPlayer, activeAllyId);
                    if (activeAlly != null) targets.add(activeAlly);
                }
            }
            case ACTIVE_ALLY_HERO -> {
                HeroCard activeAlly = findHeroInPlayer(allyPlayer, activeAllyId);
                if (activeAlly != null) targets.add(activeAlly);
            }
            case ACTIVE_ENEMY_HERO -> {
                HeroCard activeEnemy = findHeroInPlayer(enemyPlayer, activeEnemyId);
                if (activeEnemy != null) targets.add(activeEnemy);
            }
            case ALL_ALLY_HEROES -> addLivingHeroes(allyPlayer, targets);
            case ALL_ENEMY_HEROES -> addLivingHeroes(enemyPlayer, targets);
            case ALL_LIVING_HEROES -> {
                addLivingHeroes(allyPlayer, targets);
                addLivingHeroes(enemyPlayer, targets);
            }
        }
        return targets;
    }

    private HeroCard findHeroInPlayer(Player player, UUID heroId) {
        if (player == null || heroId == null || player.getCurrentDeck() == null || player.getCurrentDeck().getHeroList() == null) {
            return null;
        }
        for (HeroCard hero : player.getCurrentDeck().getHeroList()) {
            if (hero != null && heroId.equals(hero.getUuid())) {
                return hero;
            }
        }
        return null;
    }

    private void addLivingHeroes(Player player, List<HeroCard> out) {
        if (player == null || player.getCurrentDeck() == null || player.getCurrentDeck().getHeroList() == null) {
            return;
        }
        for (HeroCard hero : player.getCurrentDeck().getHeroList()) {
            if (hero != null && Boolean.TRUE.equals(hero.getEstaVivo())) {
                out.add(hero);
            }
        }
    }
}

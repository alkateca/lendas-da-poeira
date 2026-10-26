package com.alkateca.lendasdapoeira.effects.gameStart;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.Raca;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class StartOfGameGoblinAllyCountBuffEffect implements Effect {

    private final int attackPerGoblin;
    private final int defensePerGoblin;
    private boolean appliedBase = false;
    private int lastGoblinCount = 0;
    private int appliedAmplifierBonusPerGoblin = 0;

    public StartOfGameGoblinAllyCountBuffEffect(int attackPerGoblin, int defensePerGoblin) {
        this.attackPerGoblin = attackPerGoblin;
        this.defensePerGoblin = defensePerGoblin;
    }

    @Override
    public boolean canTriggerFromBench() {
        return true;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        if (!appliedBase) {
            return turnPhase == TurnPhase.GAME_START || turnPhase == TurnPhase.COMBAT_START;
        }
        return turnPhase == TurnPhase.COMBAT_START || turnPhase == TurnPhase.COMBAT_END;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        if (!(sourceCard instanceof HeroCard selfHero)) return;

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        Player allyPlayer = isPlayer1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();
        if (allyPlayer == null || allyPlayer.getCurrentDeck() == null || allyPlayer.getCurrentDeck().getHeroList() == null) {
            return;
        }

        int currentAmplifier = gameEngine.getGoblinEffectBonus(sourceCard);

        if (!appliedBase) {
            System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");
            int goblinCount = (int) allyPlayer.getCurrentDeck().getHeroList().stream()
                    .filter(h -> h != null
                            && Boolean.TRUE.equals(h.getEstaVivo())
                            && h.getRacas() != null
                            && h.getRacas().contains(Raca.GOBLIN))
                    .count();

            this.lastGoblinCount = goblinCount;
            this.appliedAmplifierBonusPerGoblin = currentAmplifier;
            this.appliedBase = true;

            if (goblinCount > 0) {
                int totalAtk = (attackPerGoblin + currentAmplifier) * goblinCount;
                int totalDef = (defensePerGoblin + currentAmplifier) * goblinCount;
                selfHero.setAtaque(selfHero.getAtaque() + totalAtk);
                selfHero.setDefesa(selfHero.getDefesa() + totalDef);
                System.out.println("-> " + selfHero.getCardName() + " recebeu +" + totalAtk + " de Ataque e +" +
                        totalDef + " de Defesa por " + goblinCount + " aliado(s) Goblin!");
            }
            return;
        }

        if (currentAmplifier != appliedAmplifierBonusPerGoblin && lastGoblinCount > 0) {
            int deltaPerGoblin = currentAmplifier - appliedAmplifierBonusPerGoblin;
            int deltaTotal = deltaPerGoblin * lastGoblinCount;
            selfHero.setAtaque(selfHero.getAtaque() + deltaTotal);
            selfHero.setDefesa(selfHero.getDefesa() + deltaTotal);
            appliedAmplifierBonusPerGoblin = currentAmplifier;
            System.out.println("-> Aura de Rei Goblin atualizada em " + selfHero.getCardName() + ": ajuste de " + deltaTotal + " em ATQ/DEF.");
        }
    }
}

package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.enums.Ordem;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class PlaySpellOrderBuffEffect implements Effect {
    private final Ordem ordemRequired;
    private final int bonusEspirito;
    private final int bonusAtaque;
    private final int bonusDefesa;

    private HeroCard buffedHero;
    private boolean activeBuff = false;

    public PlaySpellOrderBuffEffect(Ordem ordemRequired, int bonusEspirito, int bonusAtaque, int bonusDefesa) {
        this.ordemRequired = ordemRequired;
        this.bonusEspirito = bonusEspirito;
        this.bonusAtaque = bonusAtaque;
        this.bonusDefesa = bonusDefesa;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        if (gameEngine.getTurnPhase() == TurnPhase.COMBAT_END) {
            revertBuff();
            return;
        }

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        java.util.UUID myHeroId = isPlayer1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();
        com.alkateca.lendasdapoeira.entity.Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();
        HeroCard myHero = (HeroCard) myPlayer.getCurrentDeck().getHeroList().stream()
                .filter(h -> h.getUuid().equals(myHeroId)).findFirst().orElse(null);

        if (myHero != null && myHero.getOrdem() != null && myHero.getOrdem().contains(ordemRequired)) {
            myHero.setEspirito(myHero.getEspirito() + bonusEspirito);
            myHero.setAtaque(myHero.getAtaque() + bonusAtaque);
            myHero.setDefesa(myHero.getDefesa() + bonusDefesa);
            this.buffedHero = myHero;
            this.activeBuff = true;
            gameEngine.registerCombatEndCleanup(this::revertBuff);
            System.out.println("-> " + myHero.getCardName() + " recebeu buff mágico condicional de Ordem (" + ordemRequired.getNomeFormatado() + ") até o Final do Combate!");
        }
    }

    private void revertBuff() {
        if (activeBuff && buffedHero != null) {
            buffedHero.setEspirito(buffedHero.getEspirito() - bonusEspirito);
            buffedHero.setAtaque(buffedHero.getAtaque() - bonusAtaque);
            buffedHero.setDefesa(buffedHero.getDefesa() - bonusDefesa);
            System.out.println("-> Buff condicional de Ordem (" + ordemRequired.getNomeFormatado() + ") de " + buffedHero.getCardName() + " expirou no Final do Combate.");
            activeBuff = false;
            buffedHero = null;
        }
    }
}

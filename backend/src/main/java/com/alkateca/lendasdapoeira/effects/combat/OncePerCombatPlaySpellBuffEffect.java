package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import java.util.List;

public class OncePerCombatPlaySpellBuffEffect implements Effect {

    private final int espiritoBuff;
    private final int ataqueBuff;
    private final int defesaBuff;
    private boolean alreadyTriggeredThisCombat = false;

    public OncePerCombatPlaySpellBuffEffect(int espiritoBuff, int ataqueBuff, int defesaBuff) {
        this.espiritoBuff = espiritoBuff;
        this.ataqueBuff = ataqueBuff;
        this.defesaBuff = defesaBuff;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        // Resetamos a flag no fim do combate, ativamos na resolução
        if (turnPhase == TurnPhase.COMBAT_END) {
            alreadyTriggeredThisCombat = false;
            return false;
        }
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        if (alreadyTriggeredThisCombat) return;

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());

        List<Card> playedCards = isPlayer1 
                ? gameEngine.getGameState().getPlayer1PlayedCards() 
                : gameEngine.getGameState().getPlayer2PlayedCards();

        boolean playedSpell = playedCards.stream()
                .anyMatch(c -> c.getCardType() == CardType.SPELL); // MAGIA/FEITICO

        if (playedSpell) {
            java.util.UUID activeHeroId = isPlayer1
                    ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                    : gameEngine.getGameState().getActiveHeroPlayer2Id();

            Player allyPlayer = isPlayer1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();

            if (activeHeroId != null && activeHeroId.equals(sourceCard.getUuid())) {
                HeroCard activeHero = (HeroCard) allyPlayer.getCurrentDeck().getHeroList().stream()
                        .filter(h -> h.getUuid().equals(activeHeroId))
                        .findFirst()
                        .orElse(null);

                if (activeHero != null) {
                    activeHero.setEspirito(activeHero.getEspirito() + espiritoBuff);
                    activeHero.setAtaque(activeHero.getAtaque() + ataqueBuff);
                    activeHero.setDefesa(activeHero.getDefesa() + defesaBuff);
                    alreadyTriggeredThisCombat = true;
                    
                    System.out.println("Efeito de " + sourceCard.getCardName() + " ativado (Ao jogar Magia): +" + ataqueBuff + " Ataque!");
                }
            }
        }
    }
}

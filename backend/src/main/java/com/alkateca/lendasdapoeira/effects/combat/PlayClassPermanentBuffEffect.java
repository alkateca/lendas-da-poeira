package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.Classe;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

public class PlayClassPermanentBuffEffect implements Effect {

    private final Classe targetClasse;
    private final int bonusEspirito;
    private final int bonusAtaque;
    private final int bonusDefesa;

    public PlayClassPermanentBuffEffect(Classe targetClasse, int bonusEspirito, int bonusAtaque, int bonusDefesa) {
        this.targetClasse = targetClasse;
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
        System.out.println("Efeito de " + sourceCard.getCardName() + " ativado!");

        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        Player myPlayer = isPlayer1
                ? gameEngine.getPlayer1()
                : gameEngine.getPlayer2();

        if (myPlayer == null || myPlayer.getCurrentDeck() == null || myPlayer.getCurrentDeck().getHeroList() == null) {
            return;
        }

        int buffedCount = 0;
        for (Card card : myPlayer.getCurrentDeck().getHeroList()) {
            if (card instanceof HeroCard) {
                HeroCard hero = (HeroCard) card;
                if (Boolean.TRUE.equals(hero.getEstaVivo())
                        && hero.getClasse() != null
                        && hero.getClasse().contains(targetClasse)) {
                    hero.setEspirito(hero.getEspirito() + bonusEspirito);
                    hero.setAtaque(hero.getAtaque() + bonusAtaque);
                    hero.setDefesa(hero.getDefesa() + bonusDefesa);
                    buffedCount++;
                    System.out.println("-> " + hero.getCardName() + " (" + targetClasse.getNomeFormatado() + ") recebeu +"
                            + bonusAtaque + " de Ataque até o Final da Partida!");
                }
            }
        }

        if (buffedCount == 0) {
            System.out.println("-> Nenhum Herói aliado da classe " + targetClasse.getNomeFormatado() + " disponível.");
        }
    }
}

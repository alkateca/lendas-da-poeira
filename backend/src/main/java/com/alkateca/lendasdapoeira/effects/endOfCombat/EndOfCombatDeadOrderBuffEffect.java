package com.alkateca.lendasdapoeira.effects.endOfCombat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.Ordem;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class EndOfCombatDeadOrderBuffEffect implements Effect {

    private final Ordem ordemAlvo;
    private final int espiritoBuff;
    private final int ataqueBuff;
    private final int defesaBuff;
    private final Set<UUID> processedDeadHeroIds = new HashSet<>();

    public EndOfCombatDeadOrderBuffEffect(Ordem ordemAlvo, int espiritoBuff, int ataqueBuff, int defesaBuff) {
        this.ordemAlvo = ordemAlvo;
        this.espiritoBuff = espiritoBuff;
        this.ataqueBuff = ataqueBuff;
        this.defesaBuff = defesaBuff;
    }

    @Override
    public boolean canTriggerFromBench() {
        return true;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.COMBAT_END;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        boolean isPlayer1 = sourceCard.getOwnerId().equals(gameEngine.getPlayer1().getUuid());
        Player allyPlayer = isPlayer1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();

        List<HeroCard> deadHeroesThisTurn = allyPlayer.getCurrentDeck().getHeroList().stream()
                .filter(c -> c instanceof HeroCard)
                .map(c -> (HeroCard) c)
                .filter(h -> h.getVidaAtual() <= 0
                        && Boolean.TRUE.equals(h.getEstaVivo())
                        && !processedDeadHeroIds.contains(h.getUuid())
                        && h.getOrdem() != null
                        && h.getOrdem().contains(ordemAlvo))
                .collect(Collectors.toList());

        if (!deadHeroesThisTurn.isEmpty()) {
            int numberOfDeaths = deadHeroesThisTurn.size();
            for (HeroCard deadHero : deadHeroesThisTurn) {
                processedDeadHeroIds.add(deadHero.getUuid());
            }

            List<HeroCard> survivingTargetHeroes = allyPlayer.getCurrentDeck().getHeroList().stream()
                    .filter(c -> c instanceof HeroCard)
                    .map(c -> (HeroCard) c)
                    .filter(h -> h.getVidaAtual() > 0
                            && Boolean.TRUE.equals(h.getEstaVivo())
                            && h.getOrdem() != null
                            && h.getOrdem().contains(ordemAlvo))
                    .collect(Collectors.toList());

            int totalEspiritoBuff = espiritoBuff * numberOfDeaths;
            int totalAtaqueBuff = ataqueBuff * numberOfDeaths;
            int totalDefesaBuff = defesaBuff * numberOfDeaths;

            for (HeroCard survivor : survivingTargetHeroes) {
                survivor.setEspirito(survivor.getEspirito() + totalEspiritoBuff);
                survivor.setAtaque(survivor.getAtaque() + totalAtaqueBuff);
                survivor.setDefesa(survivor.getDefesa() + totalDefesaBuff);
            }

            System.out.println("Efeito de " + sourceCard.getCardName() + ": " + numberOfDeaths + " " + ordemAlvo.getNomeFormatado() + "(s) morreram! Buff aplicado aos outros Cavaleiros.");
        }
    }
}

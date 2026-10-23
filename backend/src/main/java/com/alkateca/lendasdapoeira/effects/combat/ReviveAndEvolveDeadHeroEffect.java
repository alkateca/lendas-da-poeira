package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.CardFactory;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Efeito de Liberação: Revive um personagem aliado morto (à escolha do jogador se houver mais de um),
 * altera seu nome para a forma evoluída, adiciona status extras (Espírito, Ataque, Defesa, Vida),
 * e HERDA todos os efeitos e status que o personagem morto já possuía (sem cartas anexadas, que foram descartadas na morte).
 */
public class ReviveAndEvolveDeadHeroEffect implements Effect {

    private final String evolvedName;
    private final int bonusSpirit;
    private final int bonusAttack;
    private final int bonusDefense;
    private final int bonusMaxHp;
    private final String extraDescription;

    public ReviveAndEvolveDeadHeroEffect(
            String evolvedName,
            int bonusSpirit,
            int bonusAttack,
            int bonusDefense,
            int bonusMaxHp,
            String extraDescription) {
        this.evolvedName = evolvedName;
        this.bonusSpirit = bonusSpirit;
        this.bonusAttack = bonusAttack;
        this.bonusDefense = bonusDefense;
        this.bonusMaxHp = bonusMaxHp;
        this.extraDescription = extraDescription;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public boolean shouldCaptureSnapshot() {
        return false; // O snapshot HERO_EVOLVED já é capturado na conclusão
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        if (gameEngine == null || sourceCard == null) return;

        boolean isP1 = gameEngine.getPlayer1() != null
                && gameEngine.getPlayer1().getUuid().equals(sourceCard.getOwnerId());
        Player owner = isP1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();
        if (owner == null || owner.getCurrentDeck() == null) return;

        List<HeroCard> deadAllies = owner.getCurrentDeck().getHeroList().stream()
                .filter(h -> h != null && !Boolean.TRUE.equals(h.getEstaVivo()))
                .toList();

        if (deadAllies.isEmpty()) return;

        if (deadAllies.size() == 1) {
            applyReviveAndEvolution(gameEngine, owner, deadAllies.get(0));
        } else {
            List<UUID> validDeadHeroIds = deadAllies.stream().map(HeroCard::getUuid).toList();
            gameEngine.requestPlayerChoice(
                    owner.getUuid(),
                    sourceCard,
                    "ALLY_HERO",
                    "Escolha 1 Herói aliado morto para Reviver e Evoluir",
                    validDeadHeroIds,
                    false,
                    chosenHeroUuid -> {
                        HeroCard chosenDead = gameEngine.findHeroById(chosenHeroUuid);
                        if (chosenDead != null) {
                            applyReviveAndEvolution(gameEngine, owner, chosenDead);
                        }
                    }
            );
        }
    }

    private void applyReviveAndEvolution(GameEngine gameEngine, Player owner, HeroCard deadHero) {
        String oldName = deadHero.getCardName();

        // Garante que qualquer resquício de anexo seja removido
        gameEngine.detachAllCardsFromHero(deadHero.getUuid(), com.alkateca.lendasdapoeira.enums.ZoneId.DISCARD);

        // Preserva os efeitos originais do morto e concatena os novos efeitos da evolução (se existirem no CardFactory)
        List<Effect> inheritedEffects = deadHero.getEffects() != null
                ? new ArrayList<>(deadHero.getEffects())
                : new ArrayList<>();

        HeroCard evoTemplate = new HeroCard();
        evoTemplate.setCardName(evolvedName);
        CardFactory.buildCardLogic(evoTemplate);
        if (evoTemplate.getEffects() != null) {
            for (Effect newEff : evoTemplate.getEffects()) {
                boolean alreadyHasSameClass = inheritedEffects.stream()
                        .anyMatch(oldEff -> oldEff.getClass().equals(newEff.getClass()));
                if (!alreadyHasSameClass) {
                    inheritedEffects.add(newEff);
                }
            }
        }

        // Herda os status do morto + soma os status extras da Liberação
        deadHero.setCardName(evolvedName);
        deadHero.setEspirito(deadHero.getEspirito() + bonusSpirit);
        deadHero.setAtaque(deadHero.getAtaque() + bonusAttack);
        deadHero.setDefesa(deadHero.getDefesa() + bonusDefense);
        deadHero.setVidaMaxima(deadHero.getVidaMaxima() + bonusMaxHp);
        deadHero.setVidaAtual(deadHero.getVidaMaxima()); // Revive com a vida cheia (já somada ao bônus)
        deadHero.setEstaVivo(true);
        deadHero.setEstaAtivo(true);
        deadHero.setEffects(inheritedEffects);

        if (extraDescription != null && !extraDescription.isBlank()) {
            String prevDesc = deadHero.getDescricao() != null ? deadHero.getDescricao() + "\n" : "";
            deadHero.setDescricao(prevDesc + extraDescription);
        }

        gameEngine.getLastRecordedHeroHp().put(deadHero.getUuid(), deadHero.getVidaAtual());

        var snapshot = com.alkateca.lendasdapoeira.engine.SnapshotFactory.createSnapshot(gameEngine, deadHero, this, "HERO_EVOLVED");
        snapshot.getTriggerContext().setPreviousHeroName(oldName);
        snapshot.getTriggerContext().setEvolvedHeroName(deadHero.getCardName());
        snapshot.getTriggerContext().setTargetHeroId(deadHero.getUuid());
        snapshot.getTriggerContext().setOwnerPlayerId(owner.getUuid());
        snapshot.getTriggerContext().setLogMessage("🔥 RESSURREIÇÃO E EVOLUÇÃO! " + oldName + " renasceu como " + deadHero.getCardName() + "!");
        gameEngine.getSnapshotHistory().add(snapshot);
    }
}

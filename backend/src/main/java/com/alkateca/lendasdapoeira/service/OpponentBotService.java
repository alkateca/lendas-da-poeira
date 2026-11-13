package com.alkateca.lendasdapoeira.service;

import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OpponentBotService {

    /**
     * Verifica se o Bot (Player 2) precisa agir na fase atual e executa a jogada automaticamente.
     */
    public void playBotTurnIfNeeded(GameEngine engine) {
        if (engine == null || engine.getTurnPhase() == TurnPhase.GAME_OVER) {
            return;
        }

        Player botPlayer = engine.getPlayer2();
        Player humanPlayer = engine.getPlayer1();
        UUID botId = botPlayer.getUuid();

        // 1. Se estiver na fase CHOICE e a iniciativa for do Bot, ele escolhe os Heróis e avança para PREPARATION
        if (engine.getTurnPhase() == TurnPhase.CHOICE
                && botId.equals(engine.getGameState().getActiveTurnPlayerId())) {
            executeBotHeroChoice(engine, botPlayer, humanPlayer);
        }

        // 2. Na fase PREPARATION ou RESOLUTION, sempre que a janela de resolução for do Bot, ele joga 1 carta (ou passa)
        while ((engine.getTurnPhase() == TurnPhase.PREPARATION || engine.getTurnPhase() == TurnPhase.RESOLUTION)
                && !engine.isWaitingForPlayerChoice()
                && botId.equals(engine.getGameState().getEffectiveResolutionActivePlayerId())) {
            executeBotPreparation(engine, botPlayer);
        }

        // 3. Na fase DISCARD (simultânea), o Bot descarta imediatamente sem precisar esperar o Jogador 1
        if (engine.getTurnPhase() == TurnPhase.DISCARD && !engine.isPlayer2Ready()) {
            executeBotDiscard(engine, botPlayer);

            // Se o Jogador 1 já tinha confirmado antes, o submitDiscard acima já virou o turno para CHOICE.
            // Caso a nova iniciativa em CHOICE seja do Bot, ele já escolhe os Heróis!
            if (engine.getTurnPhase() == TurnPhase.CHOICE
                    && botId.equals(engine.getGameState().getActiveTurnPlayerId())) {
                executeBotHeroChoice(engine, botPlayer, humanPlayer);
            }
        }
    }

    /**
     * Bot na TurnPhase.CHOICE:
     * Escolhe o seu Herói vivo/ativo com maior Vida Atual e foca no Herói do jogador humano com menor Vida Atual.
     */
    private void executeBotHeroChoice(GameEngine engine, Player botPlayer, Player humanPlayer) {
        HeroCard botHero = botPlayer.getCurrentDeck().getHeroList().stream()
                .filter(c -> c instanceof HeroCard)
                .map(c -> (HeroCard) c)
                .filter(h -> h.getZoneId() == ZoneId.BENCH
                        && Boolean.TRUE.equals(h.getEstaVivo())
                        && Boolean.TRUE.equals(h.getEstaAtivo()))
                .max(Comparator.comparingInt(HeroCard::getVidaAtual))
                .orElse(null);

        HeroCard targetHumanHero = humanPlayer.getCurrentDeck().getHeroList().stream()
                .filter(c -> c instanceof HeroCard)
                .map(c -> (HeroCard) c)
                .filter(h -> h.getZoneId() == ZoneId.BENCH
                        && Boolean.TRUE.equals(h.getEstaVivo()))
                .min(Comparator.comparingInt(HeroCard::getVidaAtual))
                .orElse(null);

        if (botHero != null && targetHumanHero != null) {
            System.out.println("🤖 [BOT] Escolheu para o combate: " + botHero.getCardName()
                    + " VS " + targetHumanHero.getCardName());
            engine.submitHeroChoice(botPlayer.getUuid(), botHero.getUuid(), targetHumanHero.getUuid());
        }
    }

    /**
     * Bot na TurnPhase.PREPARATION:
     * Seleciona até 2 cartas da sua mão respeitando o limite de empunhadura (maxWeaponSlots).
     */
    private void executeBotPreparation(GameEngine engine, Player botPlayer) {
        List<Card> botHand = botPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.HAND)
                .collect(Collectors.toList());

        UUID activeBotHeroId = engine.getGameState().getActiveHeroPlayer2Id();
        HeroCard activeBotHero = botPlayer.getCurrentDeck().getHeroList().stream()
                .filter(c -> c instanceof HeroCard && c.getUuid().equals(activeBotHeroId))
                .map(c -> (HeroCard) c)
                .findFirst()
                .orElse(null);

        int maxSlots = activeBotHero != null ? activeBotHero.getMaxWeaponSlots() : 2;
        int currentUsedSlots = activeBotHero != null ? engine.getUsedWeaponSlotsForHero(activeBotHero.getUuid()) : 0;

        UUID chosenCardUuid = null;

        for (Card candidate : botHand) {
            if (candidate.isReactionOnlyCard()) {
                continue;
            }
            if (activeBotHero != null && !engine.isCardCompatibleForPlay(botPlayer, activeBotHero, candidate)) {
                continue;
            }

            if (activeBotHero != null && !engine.canHeroPlayCard(activeBotHero, candidate)) {
                continue;
            }

            if (candidate.getItemType() == ItemType.ARMA) {
                int emp = Math.max(1, candidate.getEmpunhadura());
                if (currentUsedSlots + emp <= maxSlots) {
                    chosenCardUuid = candidate.getUuid();
                    break;
                }
            } else {
                chosenCardUuid = candidate.getUuid();
                break;
            }
        }

        System.out.println("🤖 [BOT] Jogou na janela de resolução: " + (chosenCardUuid != null ? chosenCardUuid : "PASSOU"));
        engine.submitResolutionCard(botPlayer.getUuid(), chosenCardUuid);
    }

    /**
     * Bot na TurnPhase.DISCARD:
     * O descarte é opcional (0 a X cartas).
     * O Bot só descarta cartas se tiver múltiplas armas na mão que excedam os slots,
     * caso contrário passa sem descartar nenhuma (0 cartas).
     */
    private void executeBotDiscard(GameEngine engine, Player botPlayer) {
        List<Card> botHand = botPlayer.getCurrentDeck().getCardList().stream()
                .filter(c -> c.getZoneId() == ZoneId.HAND)
                .collect(Collectors.toList());

        List<UUID> toDiscard = new ArrayList<>();

        // Conta quantas armas o Bot tem paradas na mão
        List<Card> weaponsInHand = botHand.stream()
                .filter(c -> c.getItemType() == ItemType.ARMA)
                .collect(Collectors.toList());

        // Se tiver mais de 2 armas acumuladas na mão, descarta as excedentes; senão descarta 0 cartas
        if (weaponsInHand.size() > 2) {
            for (int i = 2; i < weaponsInHand.size(); i++) {
                toDiscard.add(weaponsInHand.get(i).getUuid());
            }
        }

        System.out.println("🤖 [BOT] Confirmou fase DISCARD descartando " + toDiscard.size() + " carta(s).");
        engine.submitDiscard(botPlayer.getUuid(), toDiscard);
    }
}
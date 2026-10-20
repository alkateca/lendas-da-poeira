package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SnapshotFactory {
    private static long sequenceCounter = 0;

    public static MatchSnapshotDTO createSnapshot(GameEngine engine, Card sourceCard, Effect effect, String eventType) {
        MatchSnapshotDTO snapshot = new MatchSnapshotDTO();
        snapshot.setSnapshotSequence(++sequenceCounter);
        snapshot.setTurnPhase(engine.getTurnPhase());
        snapshot.setActiveTurnPlayerId(engine.getGameState().getActiveTurnPlayerId());

        boolean waiting = engine.getTurnPhase() == TurnPhase.CHOICE ||
                engine.getTurnPhase() == TurnPhase.PREPARATION ||
                engine.getTurnPhase() == TurnPhase.DISCARD ||
                engine.isWaitingForPlayerChoice();
        snapshot.setWaitingForMainButton(waiting);

        MatchSnapshotDTO.TriggerContextDTO trigger = new MatchSnapshotDTO.TriggerContextDTO();
        trigger.setEventType(eventType);
        if (sourceCard != null) {
            trigger.setSourceCardUuid(sourceCard.getUuid());
            trigger.setSourceCardName(sourceCard.getCardName());
        }
        if (effect != null) {
            trigger.setEffectClassName(effect.getClass().getSimpleName());
        }

        if ("EFFECT_RESOLVED".equals(eventType) && sourceCard != null) {
            String descResumida = sourceCard.getDescricao() != null ? sourceCard.getDescricao().replace("\n", " | ") : "";
            trigger.setLogMessage("⚡ Efeito ativado: " + sourceCard.getCardName() + " (" + descResumida + ")");
        } else if ("CHOICE_REQUESTED".equals(eventType) && engine.getGameState().getPendingChoice() != null) {
            trigger.setLogMessage("🎯 Escolha necessária: " + engine.getGameState().getPendingChoice().getPromptMessage());
        } else if ("PHYSICAL_COMBAT_EXCHANGE".equals(eventType)) {
            trigger.setLogMessage("⚔️ Combate Físico realizado entre os Heróis ativos!");
        } else {
            trigger.setLogMessage("Fase atual: " + engine.getTurnPhase());
        }
        snapshot.setTriggerContext(trigger);

        // Guarda o pedido de escolha pendente (se a ResolutionQueue tiver pausado aguardando input do jogador)
        snapshot.setPendingChoice(engine.getGameState().getPendingChoice());

        // Guarda a mão completa de ambos no histórico interno; adaptSnapshotForViewer oculta a mão do oponente ao enviar
        snapshot.setLocalPlayer(buildPlayerView(engine.getPlayer1(), engine));
        snapshot.setOpponentPlayer(buildPlayerView(engine.getPlayer2(), engine));

        return snapshot;
    }

    private static MatchSnapshotDTO.PlayerViewDTO buildPlayerView(Player player, GameEngine engine) {
        MatchSnapshotDTO.PlayerViewDTO view = new MatchSnapshotDTO.PlayerViewDTO();
        view.setPlayerId(player.getUuid());
        view.setName(player.getName());

        boolean isP1 = player.equals(engine.getPlayer1());
        UUID activeHeroId = isP1
                ? engine.getGameState().getActiveHeroPlayer1Id()
                : engine.getGameState().getActiveHeroPlayer2Id();
        view.setActiveHeroId(activeHeroId);

        if (player.getCurrentDeck() != null) {
            List<Card> allDeckCards = player.getCurrentDeck().getCardList();

            view.setDeckCount((int) allDeckCards.stream().filter(c -> c.getZoneId() == ZoneId.DECK).count());
            view.setHandCount((int) allDeckCards.stream().filter(c -> c.getZoneId() == ZoneId.HAND).count());

            view.setHand(allDeckCards.stream()
                    .filter(c -> c.getZoneId() == ZoneId.HAND)
                    .map(SnapshotFactory::toCardDTO)
                    .collect(Collectors.toList()));

            // Cartas anexadas (ATTACHED) para vincular ao Herói ativo ou na bancada
            List<Card> allAttachedCardsInGame = new ArrayList<>();
            if (engine.getPlayer1().getCurrentDeck() != null) {
                allAttachedCardsInGame.addAll(engine.getPlayer1().getCurrentDeck().getCardList().stream()
                        .filter(c -> c.getZoneId() == ZoneId.ATTACHED)
                        .collect(Collectors.toList()));
            }
            if (engine.getPlayer2().getCurrentDeck() != null) {
                allAttachedCardsInGame.addAll(engine.getPlayer2().getCurrentDeck().getCardList().stream()
                        .filter(c -> c.getZoneId() == ZoneId.ATTACHED)
                        .collect(Collectors.toList()));
            }

            view.setHeroes(player.getCurrentDeck().getHeroList().stream()
                    .map(h -> toHeroDTO(h, allAttachedCardsInGame))
                    .collect(Collectors.toList()));

            view.setDiscardPile(allDeckCards.stream()
                    .filter(c -> c.getZoneId() == ZoneId.DISCARD)
                    .map(SnapshotFactory::toCardDTO)
                    .collect(Collectors.toList()));

            view.setExilePile(allDeckCards.stream()
                    .filter(c -> c.getZoneId() == ZoneId.EXILE)
                    .map(SnapshotFactory::toCardDTO)
                    .collect(Collectors.toList()));
        }

        List<Card> played = isP1
                ? engine.getGameState().getPlayer1PlayedCards()
                : engine.getGameState().getPlayer2PlayedCards();

        boolean revealed = isResolutionPhase(engine.getTurnPhase());
        view.setPlayedCardsInResolve(played.stream()
                .map(c -> toCardVisibleDTO(c, engine.getTurnPhase(), revealed))
                .collect(Collectors.toList()));

        return view;
    }

    private static boolean isResolutionPhase(TurnPhase phase) {
        return phase == TurnPhase.COMBAT_START ||
                phase == TurnPhase.RESOLUTION ||
                phase == TurnPhase.COMBAT ||
                phase == TurnPhase.COMBAT_END;
    }

    private static MatchSnapshotDTO.HeroSnapshotDTO toHeroDTO(
            Card card,
            List<Card> allAttachedCardsInGame) {

        if (!(card instanceof HeroCard)) return null;
        HeroCard hero = (HeroCard) card;
        MatchSnapshotDTO.HeroSnapshotDTO dto = new MatchSnapshotDTO.HeroSnapshotDTO();
        dto.setUuid(hero.getUuid());
        dto.setCardName(hero.getCardName());
        dto.setColor(hero.getColor() != null ? hero.getColor().name() : "NEUTRAL");
        dto.setDescricao(hero.getDescricao());
        dto.setZoneId(hero.getZoneId() != null ? hero.getZoneId().name() : "BENCH");
        dto.setEspirito(hero.getEspirito());
        dto.setAtaque(hero.getAtaque());
        dto.setDefesa(hero.getDefesa());
        dto.setVidaMaxima(hero.getVidaMaxima());
        dto.setVidaAtual(hero.getVidaAtual());
        dto.setMaxWeaponSlots(hero.getMaxWeaponSlots());
        dto.setReducaoDano(hero.getReducaoDano());
        dto.setDanoBonus(hero.getDanoBonus());
        dto.setVulnerabilidade(hero.getVulnerabilidade());

        if (hero.getRacas() != null) {
            dto.setRacas(hero.getRacas().stream().map(Object::toString).collect(Collectors.toList()));
        }
        if (hero.getAfinidades() != null) {
            dto.setAfinidades(hero.getAfinidades().stream().map(Object::toString).collect(Collectors.toList()));
        }
        if (hero.getOrdem() != null) {
            dto.setOrdem(hero.getOrdem().stream().map(Object::toString).collect(Collectors.toList()));
        }
        if (hero.getClasse() != null) {
            dto.setClasse(hero.getClasse().stream().map(Object::toString).collect(Collectors.toList()));
        }

        dto.setEstaVivo(hero.getEstaVivo());
        dto.setEstaAtivo(hero.getEstaAtivo());

        // Filtra apenas as cartas anexadas especificamente a este Herói (sejam aliadas ou inimigas)
        List<Card> heroAttachments = allAttachedCardsInGame.stream()
                .filter(c -> hero.getUuid().equals(c.getAttachedToCardId()))
                .collect(Collectors.toList());

        dto.setAttachedCards(heroAttachments.stream()
                .map(SnapshotFactory::toCardDTO)
                .collect(Collectors.toList()));

        int weaponSlotsUsed = heroAttachments.stream()
                .filter(c -> c.getItemType() == ItemType.ARMA)
                .mapToInt(Card::getEmpunhadura)
                .sum();
        dto.setUsedWeaponSlots(weaponSlotsUsed);

        return dto;
    }

    public static MatchSnapshotDTO.CardSnapshotDTO toCardDTO(Card card) {
        MatchSnapshotDTO.CardSnapshotDTO dto = new MatchSnapshotDTO.CardSnapshotDTO();
        dto.setUuid(card.getUuid());
        dto.setCardName(card.getCardName());
        dto.setCardType(card.getCardType() != null ? card.getCardType().name() : "UNKNOWN");
        dto.setColor(card.getColor() != null ? card.getColor().name() : "NEUTRAL");
        dto.setItemType(card.getItemType() != null ? card.getItemType().name() : "NENHUM");
        dto.setEmpunhadura(card.getEmpunhadura());
        dto.setDescricao(card.getDescricao());
        dto.setZoneId(card.getZoneId() != null ? card.getZoneId().name() : "DECK");
        dto.setFaceUp(true);
        return dto;
    }

    private static MatchSnapshotDTO.CardSnapshotDTO toCardVisibleDTO(Card card, TurnPhase phase, boolean revealed) {
        MatchSnapshotDTO.CardSnapshotDTO dto = toCardDTO(card);
        if (phase == TurnPhase.PREPARATION && !revealed) {
            dto.setFaceUp(false);
        }
        return dto;
    }

    public static MatchSnapshotDTO createSnapshotForViewer(
            GameEngine engine,
            UUID viewerPlayerId,
            Card sourceCard,
            Effect effect,
            String eventType) {
        MatchSnapshotDTO raw = createSnapshot(engine, sourceCard, effect, eventType);
        return adaptSnapshotForViewer(raw, viewerPlayerId);
    }

    public static MatchSnapshotDTO adaptSnapshotForViewer(MatchSnapshotDTO original, UUID viewerPlayerId) {
        MatchSnapshotDTO adapted = new MatchSnapshotDTO();
        adapted.setSnapshotSequence(original.getSnapshotSequence());
        adapted.setTurnPhase(original.getTurnPhase());
        adapted.setActiveTurnPlayerId(original.getActiveTurnPlayerId());
        adapted.setWaitingForMainButton(original.isWaitingForMainButton());
        adapted.setTriggerContext(original.getTriggerContext());
        adapted.setPendingChoice(original.getPendingChoice());

        boolean isP1Viewer = original.getLocalPlayer().getPlayerId().equals(viewerPlayerId);
        MatchSnapshotDTO.PlayerViewDTO myView = clonePlayerView(isP1Viewer ? original.getLocalPlayer() : original.getOpponentPlayer(), true);
        MatchSnapshotDTO.PlayerViewDTO oppView = clonePlayerView(isP1Viewer ? original.getOpponentPlayer() : original.getLocalPlayer(), false);

        adapted.setLocalPlayer(myView);
        adapted.setOpponentPlayer(oppView);
        return adapted;
    }

    private static MatchSnapshotDTO.PlayerViewDTO clonePlayerView(MatchSnapshotDTO.PlayerViewDTO src, boolean isViewerOwner) {
        MatchSnapshotDTO.PlayerViewDTO copy = new MatchSnapshotDTO.PlayerViewDTO();
        copy.setPlayerId(src.getPlayerId());
        copy.setName(src.getName());
        copy.setDeckCount(src.getDeckCount());
        copy.setHandCount(src.getHandCount());
        copy.setActiveHeroId(src.getActiveHeroId());
        copy.setHeroes(src.getHeroes());
        copy.setDiscardPile(src.getDiscardPile());
        copy.setExilePile(src.getExilePile());

        // Mão: visível apenas para o dono
        copy.setHand(isViewerOwner ? new ArrayList<>(src.getHand()) : List.of());

        // Cartas na zona RESOLVE durante PREPARATION: visíveis para o dono, ocultas para o oponente
        copy.setPlayedCardsInResolve(src.getPlayedCardsInResolve().stream().map(c -> {
            MatchSnapshotDTO.CardSnapshotDTO cardCopy = new MatchSnapshotDTO.CardSnapshotDTO();
            cardCopy.setUuid(c.getUuid());
            cardCopy.setZoneId(c.getZoneId());
            boolean showFace = isViewerOwner || c.isFaceUp();
            cardCopy.setFaceUp(showFace);
            cardCopy.setCardName(showFace ? c.getCardName() : "Carta Oculta");
            cardCopy.setCardType(showFace ? c.getCardType() : "HIDDEN");
            cardCopy.setColor(showFace ? c.getColor() : "NEUTRAL");
            cardCopy.setItemType(showFace ? c.getItemType() : "NENHUM");
            cardCopy.setEmpunhadura(showFace ? c.getEmpunhadura() : 0);
            cardCopy.setDescricao(showFace ? c.getDescricao() : "");
            return cardCopy;
        }).collect(Collectors.toList()));

        return copy;
    }
}
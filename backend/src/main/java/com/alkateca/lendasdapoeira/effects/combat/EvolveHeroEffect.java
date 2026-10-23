package com.alkateca.lendasdapoeira.effects.combat;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.engine.CardFactory;
import com.alkateca.lendasdapoeira.engine.GameEngine;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.Colors;
import com.alkateca.lendasdapoeira.enums.TurnPhase;

import java.util.ArrayList;

public class EvolveHeroEffect implements Effect {

    private final String baseHeroName;
    private final String evolvedHeroName;
    private final int evolvedSpirit;
    private final int evolvedAttack;
    private final int evolvedDefense;
    private final int evolvedMaxHp;
    private final int evolvedWeaponSlots;
    private final Colors evolvedColor;
    private final String evolvedDescription;
    private final boolean requireActiveHeroMatch;

    public EvolveHeroEffect(
            String baseHeroName,
            String evolvedHeroName,
            int evolvedSpirit,
            int evolvedAttack,
            int evolvedDefense,
            int evolvedMaxHp,
            int evolvedWeaponSlots,
            Colors evolvedColor,
            String evolvedDescription) {
        this(baseHeroName, evolvedHeroName, evolvedSpirit, evolvedAttack, evolvedDefense, evolvedMaxHp, evolvedWeaponSlots, evolvedColor, evolvedDescription, true);
    }

    public EvolveHeroEffect(
            String baseHeroName,
            String evolvedHeroName,
            int evolvedSpirit,
            int evolvedAttack,
            int evolvedDefense,
            int evolvedMaxHp,
            int evolvedWeaponSlots,
            Colors evolvedColor,
            String evolvedDescription,
            boolean requireActiveHeroMatch) {
        this.baseHeroName = baseHeroName;
        this.evolvedHeroName = evolvedHeroName;
        this.evolvedSpirit = evolvedSpirit;
        this.evolvedAttack = evolvedAttack;
        this.evolvedDefense = evolvedDefense;
        this.evolvedMaxHp = evolvedMaxHp;
        this.evolvedWeaponSlots = evolvedWeaponSlots;
        this.evolvedColor = evolvedColor;
        this.evolvedDescription = evolvedDescription;
        this.requireActiveHeroMatch = requireActiveHeroMatch;
    }

    public boolean isRequireActiveHeroMatch() {
        return requireActiveHeroMatch;
    }

    @Override
    public boolean predicate(Card sourceCard, TurnPhase turnPhase) {
        return turnPhase == TurnPhase.RESOLUTION;
    }

    @Override
    public boolean shouldCaptureSnapshot() {
        // O próprio evolveHero no GameEngine já dispara o snapshot explícito HERO_EVOLVED
        return false;
    }

    @Override
    public void resolve(GameEngine gameEngine, Card sourceCard) {
        if (gameEngine == null || sourceCard == null) return;

        boolean isP1 = gameEngine.getPlayer1() != null
                && gameEngine.getPlayer1().getUuid().equals(sourceCard.getOwnerId());
        Player owner = isP1 ? gameEngine.getPlayer1() : gameEngine.getPlayer2();
        if (owner == null || owner.getCurrentDeck() == null) return;

        java.util.UUID activeHeroId = isP1
                ? gameEngine.getGameState().getActiveHeroPlayer1Id()
                : gameEngine.getGameState().getActiveHeroPlayer2Id();
        HeroCard activeHero = gameEngine.findHeroById(activeHeroId);

        // 1. Procura o Herói base: se requireActiveHeroMatch for true, valida diretamente o Herói ativo
        HeroCard targetHero = null;
        if (requireActiveHeroMatch) {
            if (activeHero == null || !Boolean.TRUE.equals(activeHero.getEstaVivo())) {
                return;
            }
            if (baseHeroName != null && !baseHeroName.isBlank()
                    && !baseHeroName.equalsIgnoreCase(activeHero.getCardName())) {
                return;
            }
            targetHero = activeHero;
        } else {
            if (baseHeroName != null && !baseHeroName.isBlank()) {
                targetHero = owner.getCurrentDeck().getHeroList().stream()
                        .filter(h -> h != null && Boolean.TRUE.equals(h.getEstaVivo())
                                && baseHeroName.equalsIgnoreCase(h.getCardName()))
                        .findFirst()
                        .orElse(null);
            }
            if (targetHero == null) {
                targetHero = activeHero;
            }
            if (targetHero == null || !Boolean.TRUE.equals(targetHero.getEstaVivo())) {
                return;
            }
        }

        // 2. Constrói o template do Herói Evoluído e injeta suas passivas via CardFactory
        HeroCard evolvedTemplate = new HeroCard();
        evolvedTemplate.setCardName(evolvedHeroName);
        evolvedTemplate.setEspirito(evolvedSpirit);
        evolvedTemplate.setAtaque(evolvedAttack);
        evolvedTemplate.setDefesa(evolvedDefense);
        evolvedTemplate.setVidaMaxima(evolvedMaxHp);
        evolvedTemplate.setMaxWeaponSlots(evolvedWeaponSlots);
        evolvedTemplate.setColor(evolvedColor != null ? evolvedColor : targetHero.getColor());
        evolvedTemplate.setDescricao(evolvedDescription);
        evolvedTemplate.setRacas(targetHero.getRacas() != null ? new ArrayList<>(targetHero.getRacas()) : new ArrayList<>());
        evolvedTemplate.setAfinidades(targetHero.getAfinidades() != null ? new ArrayList<>(targetHero.getAfinidades()) : new ArrayList<>());
        evolvedTemplate.setOrdem(targetHero.getOrdem() != null ? new ArrayList<>(targetHero.getOrdem()) : new ArrayList<>());
        evolvedTemplate.setClasse(targetHero.getClasse() != null ? new ArrayList<>(targetHero.getClasse()) : new ArrayList<>());

        CardFactory.buildCardLogic(evolvedTemplate);

        // 3. Executa a evolução no motor de regras
        gameEngine.evolveHero(owner, targetHero.getUuid(), evolvedTemplate, sourceCard);
    }
}

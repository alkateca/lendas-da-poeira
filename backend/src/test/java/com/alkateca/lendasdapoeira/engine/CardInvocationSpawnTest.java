package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.ZoneId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CardInvocationSpawnTest {

    private Player createPlayerWithHero(String playerName, String heroName) {
        Player p = new Player();
        p.setUuid(UUID.randomUUID());
        p.setName(playerName);

        HeroCard hero = new HeroCard();
        hero.setUuid(UUID.randomUUID());
        hero.setCardName(heroName);
        hero.setOwnerId(p.getUuid());
        hero.setVidaMaxima(15);
        hero.setVidaAtual(15);
        hero.setAtaque(3);
        hero.setDefesa(2);
        hero.setEspirito(2);
        hero.setMaxWeaponSlots(1); // Limite de 1 slot de arma para testar zona cheia
        hero.setEstaVivo(true);
        hero.setEstaAtivo(true);
        hero.setZoneId(ZoneId.BENCH);
        CardFactory.buildCardLogic(hero);

        Deck deck = new Deck("Deck Teste", new ArrayList<>(), new ArrayList<>(List.of(hero)));
        p.setCurrentDeck(deck);
        return p;
    }

    @Test
    void shouldEmitTokenSummonedEventAndApplyWeaponBuffs() {
        Player p1 = createPlayerWithHero("Jogador 1", "Naelis, Grande Artesã de Cristais");
        Player p2 = createPlayerWithHero("Jogador 2", "Alka, Lutador do Grupo dos Heróis");

        GameEngine engine = new GameEngine(p1, p2);
        engine.startGame();

        HeroCard naelis = p1.getCurrentDeck().getHeroList().get(0);

        // Rapieira de Cristal concede +3 de Ataque (3 base + 3 = 6)
        assertEquals(6, naelis.getAtaque());
        assertEquals(1, engine.getUsedWeaponSlotsForHero(naelis.getUuid()));

        // Verifica que o evento TOKEN_SUMMONED foi emitido no histórico de snapshots
        boolean hasSpawnSnapshot = engine.getSnapshotHistory().stream()
                .anyMatch(s -> s.getTriggerContext() != null
                        && "TOKEN_SUMMONED".equals(s.getTriggerContext().getEventType())
                        && "Rapieira de Cristal".equals(s.getTriggerContext().getSpawnedCardName()));
        assertTrue(hasSpawnSnapshot, "Deve emitir snapshot explícito TOKEN_SUMMONED para a Rapieira de Cristal");
    }

    @Test
    void shouldHandleZoneFullWhenWeaponSlotsExceeded() {
        Player p1 = createPlayerWithHero("Jogador 1", "Naelis, Grande Artesã de Cristais");
        Player p2 = createPlayerWithHero("Jogador 2", "Alka, Lutador do Grupo dos Heróis");

        GameEngine engine = new GameEngine(p1, p2);
        engine.startGame(); // Naelis já equipa 1 Rapieira de Cristal (ocupando 1/1 slot)

        HeroCard naelis = p1.getCurrentDeck().getHeroList().get(0);
        assertEquals(1, engine.getUsedWeaponSlotsForHero(naelis.getUuid()));

        // Tenta invocar uma segunda arma ("Dragast", empunhadura 1) com os slots cheios (1/1)
        Card overflowWeapon = engine.invokeCard(p1, naelis, "Dragast");

        assertNotNull(overflowWeapon);
        assertEquals(ZoneId.DISCARD, overflowWeapon.getZoneId(), "Arma excedente deve ir para o Descarte quando os slots estão cheios");
        assertNull(overflowWeapon.getAttachedToCardId());

        boolean hasZoneFullEvent = engine.getSnapshotHistory().stream()
                .anyMatch(s -> s.getTriggerContext() != null
                        && "SPAWN_FAILED_ZONE_FULL".equals(s.getTriggerContext().getEventType())
                        && s.getTriggerContext().isZoneFull());
        assertTrue(hasZoneFullEvent, "Deve emitir evento SPAWN_FAILED_ZONE_FULL quando os slots de arma estiverem cheios");
    }
}

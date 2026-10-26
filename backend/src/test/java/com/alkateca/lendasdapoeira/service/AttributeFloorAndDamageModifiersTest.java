package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.entity.Deck;
import com.alkateca.lendasdapoeira.entity.HeroCard;
import com.alkateca.lendasdapoeira.entity.Player;
import com.alkateca.lendasdapoeira.enums.CardType;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.enums.TurnPhase;
import com.alkateca.lendasdapoeira.enums.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AttributeFloorAndDamageModifiersTest {

    private GameEngine engine;
    private Player p1;
    private Player p2;
    private HeroCard p1Hero;
    private HeroCard p2Hero;

    @BeforeEach
    void setUp() {
        p1 = new Player(UUID.randomUUID(), "Jogador 1", new Deck("D1", new ArrayList<>(), new ArrayList<>()));
        p2 = new Player(UUID.randomUUID(), "Jogador 2", new Deck("D2", new ArrayList<>(), new ArrayList<>()));

        p1Hero = new HeroCard();
        p1Hero.setUuid(UUID.randomUUID());
        p1Hero.setCardName("Atacante");
        p1Hero.setOwnerId(p1.getUuid());
        p1Hero.setEspirito(2);
        p1Hero.setAtaque(5);
        p1Hero.setDefesa(2);
        p1Hero.setVidaMaxima(20);
        p1Hero.setVidaAtual(20);
        p1Hero.setMaxWeaponSlots(2);
        p1Hero.setEstaVivo(true);
        p1Hero.setEstaAtivo(true);
        p1Hero.setZoneId(ZoneId.BATTLE);
        p1.getCurrentDeck().getHeroList().add(p1Hero);

        p2Hero = new HeroCard();
        p2Hero.setUuid(UUID.randomUUID());
        p2Hero.setCardName("Defensor");
        p2Hero.setOwnerId(p2.getUuid());
        p2Hero.setEspirito(1);
        p2Hero.setAtaque(4);
        p2Hero.setDefesa(1);
        p2Hero.setVidaMaxima(20);
        p2Hero.setVidaAtual(20);
        p2Hero.setMaxWeaponSlots(2);
        p2Hero.setEstaVivo(true);
        p2Hero.setEstaAtivo(true);
        p2Hero.setZoneId(ZoneId.BATTLE);
        p2.getCurrentDeck().getHeroList().add(p2Hero);

        engine = new GameEngine(p1, p2);
        engine.getGameState().setActiveHeroPlayer1Id(p1Hero.getUuid());
        engine.getGameState().setActiveHeroPlayer2Id(p2Hero.getUuid());
    }

    @Test
    @DisplayName("Debuffs nunca devem deixar Defesa ou Espírito negativos nem amplificar dano indevidamente")
    void shouldClampDefenseAndSpiritAtZeroWhenDebuffed() {
        // Arrange
        // p2Hero tem Defesa = 1 e Espírito = 1.
        // Bomba de fumaça aplica -2 de Defesa; Garra espectral aplica -2 de Espírito.
        Card bomba = new Card();
        bomba.setUuid(UUID.randomUUID());
        bomba.setCardName("Bomba de fumaça");
        bomba.setCardType(CardType.ITEM);
        bomba.setItemType(ItemType.FERRAMENTA);
        bomba.setOwnerId(p1.getUuid());
        CardFactory.buildCardLogic(bomba);

        Card garra = new Card();
        garra.setUuid(UUID.randomUUID());
        garra.setCardName("Garra espectral");
        garra.setCardType(CardType.ITEM);
        garra.setItemType(ItemType.ARMA);
        garra.setEmpunhadura(1);
        garra.setOwnerId(p1.getUuid());
        CardFactory.buildCardLogic(garra);

        // Act
        engine.setTurnPhase(TurnPhase.RESOLUTION);
        bomba.getEffects().forEach(eff -> {
            if (eff.predicate(bomba, TurnPhase.RESOLUTION)) eff.resolve(engine, bomba);
        });
        garra.getEffects().forEach(eff -> {
            if (eff.predicate(garra, TurnPhase.RESOLUTION)) eff.resolve(engine, garra);
        });

        // Assert
        assertEquals(0, p2Hero.getDefesa(), "A Defesa não pode ficar negativa (1 - 2 deve parar em 0).");
        assertEquals(0, p2Hero.getEspirito(), "O Espírito não pode ficar negativo (1 - 2 deve parar em 0).");
    }

    @Test
    @DisplayName("Deve contabilizar Dano Bônus, Vulnerabilidade e Redução de Dano no combate físico e permitir vida negativa antes do COMBAT_END")
    void shouldApplyBonusDamageVulnerabilityAndDamageReductionCorrectly() {
        // Arrange
        // P1: Ataque = 6, DanoBônus = 3 (Total Bruto = 9), Defesa = 2, ReduçãoDano = 2 (Mitigação = 4)
        p1Hero.setAtaque(6);
        p1Hero.setDanoBonus(3);
        p1Hero.setDefesa(2);
        p1Hero.setReducaoDano(2);
        p1Hero.setVulnerabilidade(0);

        // P2: Vida = 5, Defesa = 2, ReduçãoDano = 1 (Mitigação = 3), Vulnerabilidade = 2, Ataque = 5, DanoBônus = 1
        p2Hero.setVidaAtual(5);
        p2Hero.setDefesa(2);
        p2Hero.setReducaoDano(1);
        p2Hero.setVulnerabilidade(2);
        p2Hero.setAtaque(5);
        p2Hero.setDanoBonus(1);

        // Act
        // Dano de P1 em P2 = (6 ATQ + 3 Bônus + 2 Vulnerabilidade de P2) - (2 DEF + 1 Redução de P2) = 11 - 3 = 8
        // Vida de P2: 5 - 8 = -3 (fica negativa durante COMBAT)
        // Dano de P2 em P1 = (5 ATQ + 1 Bônus + 0 Vulnerabilidade de P1) - (2 DEF + 2 Redução de P1) = 6 - 4 = 2
        // Vida de P1: 20 - 2 = 18
        engine.combat();

        // Assert (Fase COMBAT -> antes da checagem de morte do final de COMBAT_END)
        assertEquals(-3, p2Hero.getVidaAtual(), "A vida pode ficar abaixo de 0 (-3) após a troca física.");
        assertEquals(18, p1Hero.getVidaAtual(), "P1 deve sofrer apenas 2 de dano graças à Defesa (2) + Redução de Dano (2).");

        // Act 2: Finaliza COMBAT_END para processar a morte
        engine.combatEnd();

        // Assert 2
        assertFalse(p2Hero.getEstaVivo(), "Ao final de COMBAT_END, o Herói com vida <= 0 (-3) deve ser marcado como morto.");
        assertTrue(p1Hero.getEstaVivo(), "P1 continua vivo com 18 de vida.");
    }
}
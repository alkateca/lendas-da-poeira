package com.alkateca.lendasdapoeira.engine;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.core.io.ClassPathResource;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.Collections;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.effects.combat.*;
import com.alkateca.lendasdapoeira.effects.endOfCombat.*;
import com.alkateca.lendasdapoeira.effects.startOfCombat.StartOfCombatInvokeCardEffect;
import com.alkateca.lendasdapoeira.effects.startOfCombat.StartOfCombatInvokeIfNotEquippedEffect;
import com.alkateca.lendasdapoeira.effects.startOfCombat.StartOfCombatMagicDamageEffect;
import com.alkateca.lendasdapoeira.effects.startOfCombat.StartOfCombatZombieBuffEffect;
import com.alkateca.lendasdapoeira.effects.gameStart.StartOfGameInvokeCardEffect;
import com.alkateca.lendasdapoeira.effects.startOfCombat.StartOfCombatPlayNextCardByTypeEffect;
import com.alkateca.lendasdapoeira.effects.endOfCombat.EndOfCombatReturnAttachedCardsToDeckEffect;
import com.alkateca.lendasdapoeira.effects.gameStart.StartOfGameSetWeaponSlotsEffect;
import com.alkateca.lendasdapoeira.effects.startOfCombat.StartOfCombatStealAllyWeaponEffect;
import com.alkateca.lendasdapoeira.effects.startOfCombat.StartOfCombatTutorSpellFromDiscardToHandEffect;
import com.alkateca.lendasdapoeira.effects.startOfCombat.StartOfCombatHealAndReturnDiscardToHandEffect;
import com.alkateca.lendasdapoeira.effects.gameStart.StartOfGameMillSpellsByAffinityEffect;
import java.util.Arrays;

import com.alkateca.lendasdapoeira.effects.startOfCombat.StartOfCombatDebuffEnemySpiritEffect;
import com.alkateca.lendasdapoeira.effects.gameStart.StartOfGameDebuffEnemyDefenseEffect;
import com.alkateca.lendasdapoeira.enums.Classe;
import com.alkateca.lendasdapoeira.enums.ItemType;
import com.alkateca.lendasdapoeira.entity.Card;
import com.alkateca.lendasdapoeira.enums.Ordem;
import com.alkateca.lendasdapoeira.enums.Raca;

import java.util.ArrayList;
import java.util.List;

@Component
public class CardFactory {

    @Autowired(required = false)
    private com.alkateca.lendasdapoeira.service.PresetDeckService presetDeckService;

    public CardFactory() {}

    public CardFactory(com.alkateca.lendasdapoeira.service.PresetDeckService presetDeckService) {
        this.presetDeckService = presetDeckService;
    }

    public List<Card> getAllCatalogCards() {
        if (presetDeckService != null && presetDeckService.getCatalogCards() != null && !presetDeckService.getCatalogCards().isEmpty()) {
            return presetDeckService.getCatalogCards();
        }
        try {
            InputStream cardsStream = new ClassPathResource("cards.json").getInputStream();
            return new ObjectMapper().readValue(cardsStream, new TypeReference<List<Card>>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }


    public static Card buildCardLogic(Card rawCardFromJson) {

        // Garante que a lista de efeitos nunca seja nula
        List<Effect> effects = new ArrayList<>();

        if (rawCardFromJson.getCardName() == null) {
            rawCardFromJson.setEffects(effects);
            return rawCardFromJson;
        }

        switch (rawCardFromJson.getCardName()) {

            // ==========================================
            // HERÓIS
            // ==========================================
            case "Esquadrão Goblin":
                effects.add(new OncePerCombatPlaySpellBuffEffect(com.alkateca.lendasdapoeira.enums.CardType.SPELL, 1, 0, 0));
                effects.add(new OncePerCombatPlaySpellBuffEffect(com.alkateca.lendasdapoeira.enums.CardType.ITEM, 0, 1, 0));
                effects.add(new OncePerCombatPlaySpellBuffEffect(com.alkateca.lendasdapoeira.enums.CardType.ACTION, 0, 0, 1));
                effects.add(new EndOfCombatHealEffect(2));
                break;
            case "Alucinação Cintlante":
            case "Rei Goblin":
            case "Traidor Goblin":
            case "Rainha Goblin":
                effects.add(new EndOfCombatHealAlliesEffect(2));
                break;
            case "Quimera Carniceira":
                effects.add(new StartOfCombatZombieBuffEffect(1, 1, 2));
                break;
            case "Necromante das Areias":
                effects.add(new StartOfCombatInvokeCardEffect("Ritos Fúnebres"));
                break;
            case "Isenora, Santa das Laminas":
                effects.add(new AuraAlliesBuffEffect(1, 1, 1));
                effects.add(new EndOfCombatMagicDamageEffect(5));
                break;
            case "Moyra, Aprendiz da Santa":
                effects.add(new StartOfCombatMagicDamageEffect(3));
                effects.add(new OnPlaySpellBuffEffect(0, 3, 0, true));
                break;
            case "Naelis, Grande Artesã de Cristais":
                effects.add(new StartOfGameInvokeCardEffect("Rapieira de Cristal"));
                break;
            case "Alka, Lutador do Grupo dos Heróis":
            case "Leone, Clériga do Grupo dos Heróis":
                break;
            case "Enamor,O Grande Cavaleiro":
                effects.add(new StartOfCombatInvokeIfNotEquippedEffect("Dragast"));
                break;
            case "Ageus, Constructo Cristalino":
                effects.add(new EndOfCombatDeadOrderBuffEffect(Ordem.CAVALEIRO, 2, 0, 2));
                break;
            case "Cavaleiro Onear":
                effects.add(new OncePerCombatPlaySpellBuffEffect(0, 2, 0));
                break;
            case "Moyra, Santa das Laminas":
                effects.add(new StartOfCombatDebuffEnemySpiritEffect(2));
                effects.add(new OnPlaySpellBuffEffect(0, 2, 0));
                effects.add(new EndOfCombatMagicDamageEffect(5));
                break;
            case "Heróis Lendários dos Goblin":
                effects.add(new OnPlaySpellBuffEffect(com.alkateca.lendasdapoeira.enums.CardType.SPELL, 1, 0, 0));
                effects.add(new OnPlaySpellBuffEffect(com.alkateca.lendasdapoeira.enums.CardType.ITEM, 0, 1, 0));
                effects.add(new OnPlaySpellBuffEffect(com.alkateca.lendasdapoeira.enums.CardType.ACTION, 0, 0, 1));
                effects.add(new EndOfCombatHealAlliesEffect(3));
                break;
            case "Kael, Domador de Feras":
            case "Behemoth-29A":
            case "Rapinária-7B3":
            case "Kael, Cavaleiro Bestial":
            case "Hati, Constructo Gélido":
            case "Skoll, Espreitador Solar":
            case "Fenrir, o Grande Devorador":
                break;
            case "Oráculo Real":
                effects.add(new AuraRaceBuffEffect(Raca.GUARDIOES_DO_TRONO, 0, 2, 1));
                effects.add(new StartOfCombatInvokeCardEffect("Fortaleça sua Fé"));
                break;
            case "Portador da Chama":
                effects.add(new StartOfCombatHealAndReturnDiscardToHandEffect(2, 2));
                break;
            case "Constructo Fantasmagórico Guardião Perpétuo":
                effects.add(new StartOfGameSetWeaponSlotsEffect(4));
                effects.add(new OnPlayWeaponBuffAndHealEffect(2, 2));
                break;
            case "Xia, Caçador de Bestas":
                effects.add(new StartOfCombatPlayNextCardByTypeEffect(ItemType.FERRAMENTA));
                break;
            case "Asahum, Caçadora de Bestas":
                effects.add(new EndOfCombatReturnAttachedCardsToDeckEffect(Arrays.asList(ItemType.JOIA, ItemType.FERRAMENTA)));
                break;
            case "Constructo de Combate, Arsenal Vivo":
                effects.add(new StartOfGameSetWeaponSlotsEffect(4));
                effects.add(new StartOfCombatStealAllyWeaponEffect());
                break;
            case "Xamã das cinzas":
                effects.add(new AuraAlliesBonusDamageEffect(1));
                effects.add(new StartOfCombatInvokeCardEffect("Bola de fogo"));
                break;
            case "Alix, Dragão Flamejante":
                effects.add(new EndOfCombatMagicDamageEffect(3));
                effects.add(new OnPlayCardTypeInvokeCardEffect(ItemType.ARMADURA, "Bola de fogo"));
                break;
            case "Constructo das Chamas, Sif":
                effects.add(new StartOfGameMillSpellsByAffinityEffect(2, "Fogo"));
                effects.add(new StartOfCombatTutorSpellFromDiscardToHandEffect(2, "Fogo"));
                break;

            // ==========================================
            // ITENS (Armas, Joias, Ferramentas, etc)
            // ==========================================
            case "Quimera":
                effects.add(new EquipBuffEffect(2, 2, 2, 0));
                effects.add(new EndOfCombatHealBySpiritEffect());
                break;
            case "Broche de Cristal":
                effects.add(new EquipBuffEffect(2, 0, 2, 0));
                break;
            case "Lamina de Cristal":
                effects.add(new EquipBuffEffect(0, 1, 0, 0));
                break;
            case "Dragão de Cristal":
                effects.add(new EndOfCombatMagicDamageEffect(6));
                break;
            case "Homunculo Carniceiro":
            case "Quimera Negra":
                break;
            case "Dragast":
                effects.add(new EquipBuffEffect(2, 2, 0, 0));
                effects.add(new EndOfCombatHealEffect(2));
                break;
            case "Fragmento Afiado":
                effects.add(new EquipGlobalBuffEffect(2));
                effects.add(new OnEquipHealEffect(2));
                break;
            case "Rapieira de Cristal":
                effects.add(new EquipBuffEffect(0, 3, 0, 0));
                effects.add(new EndOfCombatMagicDamageEffect(5));
                break;
            case "Garra espectral":
                effects.add(new EquipBuffEffect(0, 2, 0, 0));
                effects.add(new PlayDebuffEnemySpiritEffect(2));
                effects.add(new StartOfCombatDebuffEnemySpiritEffect(2));
                effects.add(new ConvertAttacksToMagicDamageEffect());
                break;
            case "Machado de Gelo":
            case "Grande Machado Sombrio":
                break;
            case "Artefato Cortante":
                effects.add(new EquipBuffEffect(0, 3, 1, 0));
                effects.add(new EndOfCombatHealAlliesEffect(2));
                break;
            case "Sica":
                effects.add(new EquipBuffEffect(2, 2, 0, 0));
                break;
            case "Sino do Caçador":
                effects.add(new EquipBuffEffect(1, 1, 0, 0));
                effects.add(new PlayCardFromHandByTypeEffect(ItemType.FERRAMENTA));
                effects.add(new PlayNextWeaponFromDeckEffect(1));
                break;
            case "Arpão terrestre":
                effects.add(new EquipBuffEffect(0, 3, 2, 0));
                effects.add(new DrawNextCardByTypeEffect(ItemType.FERRAMENTA));
                break;
            case "Bomba de fumaça":
                effects.add(new EquipBuffEffect(0, 2, 0, 0));
                effects.add(new PlayDebuffEnemyDefenseEffect(2));
                break;
            case "Tesouro Cristalino dos Caçadores":
                effects.add(new EquipBuffBySpiritEffect());
                break;
            case "Peitoral de brasas":
                effects.add(new EquipBuffEffect(2, 0, 2, 0));
                break;
            case "Escudo ardente":
                effects.add(new EquipBuffEffect(2, 0, 2, 0));
                break;
            case "Estrela do amanhã":
                effects.add(new EndOfCombatReviveAndExileEffect(13));
                break;

            // ==========================================
            // FEITIÇOS (Spells)
            // ==========================================
            case "Bola de fogo":
            case "Bola de Fogo":
                effects.add(new PlaySpellMagicDamagePlusSpiritEffect(4));
                break;
            case "Maestria com Chamas":
                effects.add(new PlaySpellInvokeCardEffect("Bola de fogo"));
                effects.add(new PlaySpellInvokeCardEffect("Bola de fogo"));
                effects.add(new PlaySpellInvokeCardEffect("Bola de fogo"));
                break;
            case "Estatica":
                effects.add(new PlaySpellMagicDamagePlusSpiritEffect(3));
                effects.add(new PlaySpellInvokeExistingFromDeckEffect("Estatica"));
                break;
            case "Para-raios":
                effects.add(new PlaySpellBuffEffect(1, 0, 0));
                effects.add(new PlaySpellInvokeCardEffect("Estatica"));
                break;
            case "Atmosfera Pesada":
                break;
            case "Vendaval Arcano":
                effects.add(new PlaySpellDiscardRandomItemEachEnemyHeroEffect());
                break;
            case "Quebra!":
                effects.add(new PlaySpellChooseAndDiscardEnemyItemEffect());
                break;
            case "Barreira de Gelo":
            case "Mordida Glacial":
                break;
            case "Contra Ataque":
                effects.add(new DiscardEnemyWeaponEffect());
                effects.add(new PlaySpellBuffEffect(0, 3, 0));
                effects.add(new PlaySpellOrderBuffEffect(Ordem.CAVALEIRO, 1, 0, 1));
                break;
            case "Ataque Mágico":
                effects.add(new PlaySpellSetEnemyDefenseToZeroEffect());
                break;
            case "Golpes Pesados":
                effects.add(new PlaySpellBuffBySpiritEffect());
                break;
            case "Massacre Cristalino":
                effects.add(new PlaySpellSetEnemySpiritEffect());
                effects.add(new PlaySpellTriggerCombatEffectsEffect());
                break;
            case "Ponto Final":
                effects.add(new PlaySpellDoubleSpiritToAttackEffect());
                break;
            case "Auto Defesa Magica":
                effects.add(new PlaySpellBuffBySpiritEffect());
                effects.add(new PlaySpellReduceEnemyDefenseByTheirSpiritEffect());
                break;
            case "Muralha de Fogo":
                effects.add(new PlaySpellMagicDamageBySpiritPlusDefenseEffect());
                break;
            case "Cauterizar":
                effects.add(new PlaySpellHealBySpiritEffect());
                break;

            // ==========================================
            // AÇÕES (Actions)
            // ==========================================
            case "Disciplina":
                effects.add(new PlayActionUnequipEnemyCardsWithSelfDamageEffect(2, 2));
                break;
            case "Ração de emergencia":
                effects.add(new DrawCardsEffect(2));
                effects.add(new PlayCardFromHandEffect());
                break;
            case "Determinação Cristalina":
                effects.add(new PlaySpellBuffEffect(0, 1, 0));
                effects.add(new PlayFromDiscardEffect());
                break;
            case "Convocação":
                effects.add(new PlayActionToggleHeroActiveStateEffect());
                break;
            case "Mirar na cabeça":
                effects.add(new PlayActionChooseEnemyHeroDirectDamageEffect(3));
                break;
            case "Ritos Fúnebres":
            case "Exumação":
                break;
            case "Banir!":
                effects.add(new PlayActionBanishEnemyCardAndCopiesEffect());
                break;
            case "Impulso de Poder":
                effects.add(new PlayActionBonusDamageEffect(2));
                break;
            case "Tristeza":
            case "Luz da Lua":
                break;
            case "Afie suas Lâminas":
                effects.add(new PlaySpellBuffEffect(0, 4, 4));
                break;
            case "Estilo Secreto do Caçador":
                effects.add(new PlayActionSetWeaponSlotsEffect(3));
                effects.add(new PlayWeaponFromHandEffect(1));
                break;
            case "Corte seus Pescoços":
                effects.add(new PlayActionMultiPhysicalAttackRandomEffect());
                break;

            // ==========================================
            // TEURGIAS (Theurgies)
            // ==========================================
            case "Alma em Chamas":
                effects.add(new EquipBuffEffect(2, 3, 0, 0));
                effects.add(new EndOfCombatHealEffect(2));
                break;
            case "Risada Sarcástica":
                effects.add(new PlaySpellMagicDamagePlusSpiritEffect(2));
                effects.add(new PlayScheduleEndOfCombatAddCardToEnemyDeckEffect("Tristeza"));
                break;
            case "Fortaleça sua Fé":
                effects.add(new PlaySpellBuffEffect(0, 2, 0));
                effects.add(new PlayClassPermanentBuffEffect(Classe.TRANSFORMADOR, 0, 1, 0));
                break;
            case "Saque Rápido":
                effects.add(new PlayWeaponFromHandWithConditionalBuffEffect(3));
                break;
            case "Ordem Divina":
                effects.add(new PlayTheurgyChooseHeroBuffAndHealEffect(2, 2));
                break;

            // ==========================================
            // CARTAS DE LIBERAÇÃO (Missão / Evolução)
            // ==========================================
            case "Sagração da Santa das Lâminas":
                rawCardFromJson.setLiberationCard(true);
                if (rawCardFromJson.getMissionType() == null) rawCardFromJson.setMissionType("PLAY_SPELL");
                if (rawCardFromJson.getMissionTargetValue() <= 0) rawCardFromJson.setMissionTargetValue(2);
                rawCardFromJson.setTargetBaseHeroName("Moyra, Aprendiz da Santa");
                rawCardFromJson.setEvolvesIntoHeroName("Moyra, Santa das Laminas");
                // 1º Efeito: Se jogada por Moyra, substitui por Moyra, Santa das Lâminas
                effects.add(new EvolveHeroEffect(
                        "Moyra, Aprendiz da Santa",
                        "Moyra, Santa das Laminas",
                        5, 6, 3, 22, 3,
                        com.alkateca.lendasdapoeira.enums.Colors.WHITE,
                        "Início do Combate: -2 Espírito inimigo.\nAo jogar Feitiço: +2 Ataque.\nFinal do Combate: 5 Dano Mágico."
                ));
                // 2º Efeito: Joga 1 carta da mão à sua escolha!
                effects.add(new PlayCardFromHandEffect());
                break;

            case "Ascensão da Horda Lendária":
                rawCardFromJson.setLiberationCard(true);
                if (rawCardFromJson.getMissionType() == null) rawCardFromJson.setMissionType("ALLY_HEROES_DEAD");
                if (rawCardFromJson.getMissionTargetValue() <= 0) rawCardFromJson.setMissionTargetValue(2);
                rawCardFromJson.setEvolvesIntoHeroName("Heróis Lendários dos Goblin");
                // Ao morrerem 2 aliados, vai para a mão. Quando jogada, revive 1 aliado morto com +2 ESP, +3 ATQ, +2 DEF, +8 HP e muda o nome!
                effects.add(new ReviveAndEvolveDeadHeroEffect(
                        "Heróis Lendários dos Goblin",
                        2, 3, 2, 8,
                        "Evoluído: Herda habilidades originais e cura 3 em todos os aliados no fim do combate."
                ));
                break;

            case "Despertar do Cavaleiro Bestial":
                rawCardFromJson.setLiberationCard(true);
                if (rawCardFromJson.getMissionType() == null) rawCardFromJson.setMissionType("DEAL_DAMAGE");
                if (rawCardFromJson.getMissionTargetValue() <= 0) rawCardFromJson.setMissionTargetValue(10);
                rawCardFromJson.setTargetBaseHeroName("Kael, Domador de Feras");
                rawCardFromJson.setEvolvesIntoHeroName("Kael, Cavaleiro Bestial");
                if (rawCardFromJson.getDescricao() == null) {
                    rawCardFromJson.setDescricao("Missão (Causar 10 de Dano): Libera para a mão.\nEvolui seu Herói para Kael, Cavaleiro Bestial. Exilar.");
                }
                effects.add(new EvolveHeroEffect(
                        "Kael, Domador de Feras",
                        "Kael, Cavaleiro Bestial",
                        4, 7, 4, 25, 3,
                        com.alkateca.lendasdapoeira.enums.Colors.RED,
                        "Forma Evoluída: Atributos de combate ampliados e 3 slots de arma."
                ));
                break;

            default:
                // Cartas sem nome mapeado passarão por aqui sem adicionar efeitos.
                break;
        }

        // Injeta a lista de efeitos na carta
        rawCardFromJson.setEffects(effects);

        return rawCardFromJson;
    }
}
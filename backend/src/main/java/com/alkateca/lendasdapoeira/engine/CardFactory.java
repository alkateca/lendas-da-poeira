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
import com.alkateca.lendasdapoeira.effects.startOfCombat.*;
import com.alkateca.lendasdapoeira.effects.gameStart.*;
import java.util.Arrays;

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
                break;
            case "Rei Goblin":
                effects.add(new AuraGoblinEffectAmplifierEffect(1));
                effects.add(new EndOfCombatHealEffect(2));
                break;
            case "Traidor Goblin":
                effects.add(new StartOfGameGoblinAllyCountBuffEffect(1, 1));
                break;
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
                effects.add(new EndOfCombatExtraAttackIfHighAttackEffect(10));
                break;
            case "Leone, Clériga do Grupo dos Heróis":
                effects.add(new StartOfCombatHealAlliesEffect(3));
                effects.add(new EndOfCombatDirectDamageByCombatHealingEffect());
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
                if (rawCardFromJson instanceof com.alkateca.lendasdapoeira.entity.HeroCard hero) {
                    hero.setColor(com.alkateca.lendasdapoeira.enums.Colors.GREEN);
                }
                effects.add(new OnPlaySpellBuffEffect(com.alkateca.lendasdapoeira.enums.CardType.SPELL, 1, 0, 0));
                effects.add(new OnPlaySpellBuffEffect(com.alkateca.lendasdapoeira.enums.CardType.ITEM, 0, 1, 0));
                effects.add(new OnPlaySpellBuffEffect(com.alkateca.lendasdapoeira.enums.CardType.ACTION, 0, 0, 1));
                effects.add(new EndOfCombatHealAlliesEffect(3));
                break;
            case "Kael, Domador de Feras":
                effects.add(new StartOfCombatConstructoBuffAndHealEffect(2, 2));
                break;
            case "Behemoth-29A":
                effects.add(new StartOfCombatSelfDamageToHealHeroEffect(3, 3, "Kael"));
                break;
            case "Rapinária-7B3":
                effects.add(new StartOfCombatInvokeCardEffect("Vendaval Arcano"));
                effects.add(new StartOfCombatInvokeCardEffect("Barreira de Gelo"));
                break;
            case "Kael, Cavaleiro Bestial":
                if (rawCardFromJson instanceof com.alkateca.lendasdapoeira.entity.HeroCard evolvedHero) {
                    if (evolvedHero.getRacas() != null && !evolvedHero.getRacas().contains(Raca.CONSTRUCTO)) {
                        evolvedHero.getRacas().add(Raca.CONSTRUCTO);
                    }
                }
                effects.add(new StartOfCombatConstructoBuffAndHealEffect(2, 2));
                effects.add(new EndOfCombatAllEnemiesDirectDamageEffect(2));
                break;
            case "Hati, Constructo Gélido":
                effects.add(new StartOfCombatBonusAttackVsClassEffect(Classe.EMISSOR, 3));
                effects.add(new EndOfCombatTransferDeadEnemyVulnerabilityEffect());
                break;
            case "Skoll, Espreitador Solar":
                effects.add(new StartOfCombatRandomEnemyVulnerabilityEffect(1));
                effects.add(new StartOfCombatBuffAttackByEnemyVulnerabilityEffect());
                break;
            case "Fenrir, o Grande Devorador":
                effects.add(new StartOfCombatDoubleAttackByVulnerabilityEffect(5));
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
            case "Chefe Carrasco":
                effects.add(new StartOfCombatInvokeIfNotEquippedEffect("Espada do Carrasco"));
                effects.add(new StartOfCombatZeroEnemyDefenseIfWoundedEffect());
                break;
            case "Izeref, Executor da Ordem Necrótica":
                effects.add(new AuraEnemyDeadBelowZeroEffect());
                break;
            case "Constructo Carniceiro":
                effects.add(new AuraBonusDamageTakenByEnemyEffect(1));
                break;
            case "Grande Inquisidor Vael, Voz do Trono":
                effects.add(new AuraEnemyDirectDamageOnOpponentInvokeEffect(1));
                effects.add(new StartOfCombatEnemyVulnerabilityIfAttachedCountEffect(2, 1));
                break;
            case "Justiçar Elyon, Balança Imutável":
                effects.add(new EndOfCombatHealAndMagicDamagePerTwoEnemyPlayedCardsEffect(3, 3));
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
                effects.add(new EndOfCombatHealEffect(1));
                effects.add(new EndOfCombatAllEnemiesDirectDamageEffect(1, false));
                break;
            case "Quimera Negra":
                effects.add(new StartOfCombatDebuffEnemyStatsEffect(1, 1, 1, 1));
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
                effects.add(new EquipBuffEffect(0, 2, 2, 0));
                effects.add(new OnEquipEnemyVulnerabilityEffect(1, false));
                break;
            case "Grande Machado Sombrio":
                effects.add(new EquipBuffEffect(0, 4, 2, 0));
                effects.add(new OnEquipEnemyVulnerabilityEffect(1, true));
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
            case "Manoplas do necromante":
            case "Manoplas do Necromante":
                effects.add(new StartOfCombatBuffStatsByEnemyMissingHealthEffect(3));
                break;
            case "Espada do Carrasco":
                effects.add(new EquipBuffEffect(0, 3, 0, 0));
                effects.add(new EndOfCombatHealIfEnemyDefeatedEffect(5));
                break;
            case "Cutelo do Suplício":
                effects.add(new EquipBuffEffect(0, 2, 0, 0));
                effects.add(new StartOfCombatBuffAttackIfEnemyWoundedEffect(2));
                break;
            case "Selo da Confissão":
                effects.add(new EquipBuffEffect(0, 0, 0, 0));
                effects.add(new EndOfCombatHealEffect(3));
                effects.add(new EndOfCombatEnemyVulnerabilityIfDealtDamageEffect(1));
                break;
            case "Cetro do Julgamento Perpétuo":
                effects.add(new EquipBuffEffect(1, 2, 0, 0));
                effects.add(new StartOfCombatBuffIfEnemyHigherMaxHpEffect(2, 2));
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
                effects.add(new PlayPermanentStatModifierEffect(-1, 0, 0, true, true));
                break;
            case "Vendaval Arcano":
                effects.add(new PlaySpellDiscardRandomItemEachEnemyHeroEffect());
                break;
            case "Quebra!":
                effects.add(new PlaySpellChooseAndDiscardEnemyItemEffect());
                break;
            case "Barreira de Gelo":
                effects.add(new EquipGlobalBuffEffect(1, 0, 2));
                break;
            case "Mordida Glacial":
                effects.add(new PlayPermanentStatModifierEffect(1, 2, 0, false, false));
                effects.add(new OnEquipEnemyVulnerabilityEffect(1, false));
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
                effects.add(new PlayActionChooseHeroHealAndReviveEffect(5));
                break;
            case "Exumação":
                effects.add(new PlayPermanentStatModifierEffect(-1, -1, -1, false, false));
                effects.add(new PlayFromDiscardEffect());
                break;
            case "Banir!":
                effects.add(new PlayActionBanishEnemyCardAndCopiesEffect());
                break;
            case "Impulso de Poder":
                effects.add(new PlayActionBonusDamageEffect(2));
                break;
            case "Tristeza":
                break;
            case "Luz da Lua":
                effects.add(new PlaySpellBuffEffect(0, 2, 0));
                effects.add(new PlayActionHealByCurrentAttackEffect());
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
            case "Seguindo ordens":
            case "Seguindo Ordens":
                effects.add(new PlayActionChooseEnemyHeroOutOfCombatDirectDamageEffect(1));
                effects.add(new PlayActionToggleEnemyHeroActiveStateEffect());
                break;
            case "Grilhões Pesados":
            case "Grilhões pesados":
            case "Girlhões pesados":
                effects.add(new PlayActionUnequipEnemyItemConditionalWoundedEffect(1, 2));
                break;
            case "Cheiro de Sangue":
                effects.add(new PlayActionPhysicalDamageEffect(3));
                effects.add(new PlayActionDirectDamageIfEnemyWoundedEffect(2));
                break;
            case "Arrastar para o Cadafalso":
                effects.add(new PlayActionChooseWoundedEnemyHeroOutOfCombatDirectDamageEffect(2));
                effects.add(new OnEquipEnemyVulnerabilityEffect(1, false));
                break;
            case "Colheita dos Condenados":
                effects.add(new PlayActionDrainPerWoundedEnemyEffect(2, 1));
                break;
            case "Sentença da Guilhotina":
                effects.add(new PlayActionEnemyVulnerabilityIfWoundedEffect(2));
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
            case "Édito contra a Ganância":
                effects.add(new PlaySpellMagicDamagePlusSpiritEffect(2));
                effects.add(new PlayTheurgyDirectDamageAndDebuffIfOpponentDrewCardsEffect(3, 2));
                break;
            case "Retribuição Dogmática":
                effects.add(new PlaySpellBuffEffect(0, 0, 2));
                effects.add(new PlayTheurgyMagicDamagePerOpponentPlayedOrInvokedCardEffect(2));
                break;
            case "Interdito Sagrado":
                effects.add(new PlayZeroEnemyBonusDamageEffect());
                effects.add(new PlayDebuffEnemyAttackIfWeaponEquippedEffect(2));
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
                        1, 10, 2, 16, 2,
                        com.alkateca.lendasdapoeira.enums.Colors.GREEN,
                        "Início do Combate: Seus Constructos recebem +2 de Ataque e recuperam 2 pontos de Vida.\nFinal do Combate: Cause 2 de Dano Direto aos Heróis Inimigos."
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
import Phaser from 'phaser';
import { BOARD_LAYOUT, getCardVisualTheme } from './BoardCoordinates.js';
import { formatCardDescriptionWithKeywords } from './keywordsConfig.js';
import PhantomUIKit from '../ui/PhantomUIKit.js';
import { API_BASE_URL } from './apiConfig.js';

/**
 * Configuração centralizada de artes e dados das cartas, heróis, pets e tokens (line arts P&B).
 */

export function toSnakeCaseKey(str) {
    if (!str || str === 'Carta Oculta') return null;
    return String(str)
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/[^a-zA-Z0-9]+/g, '_')
        .replace(/^_+|_+$/g, '')
        .toLowerCase();
}

export function toCamelCaseKey(str) {
    if (!str || str === 'Carta Oculta') return null;
    const camel = String(str)
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/[^a-zA-Z0-9 ]/g, '')
        .split(' ')
        .filter(Boolean)
        .map((w, i) => (i === 0 ? w.toLowerCase() : w.charAt(0).toUpperCase() + w.slice(1).toLowerCase()))
        .join('');
    const aliases = {
        brocheDeCristal: 'brocheCristal',
        dragaoDeCristal: 'dragaoCristal'
    };
    return aliases[camel] || camel;
}

export const CARD_ART_KEYS = [
    "afie_suas_laminas",
    "ageus_constructo_cristalino",
    "alabarda_cerimonial_do_trono",
    "alevyr_da_forja",
    "alix_dragao_flamejante",
    "alka_lutador_do_grupo_dos_herois",
    "alma_em_chamas",
    "alucinacao_cintlante",
    "amuleto_de_ossos_da_fabrica",
    "armadura_antichamas",
    "arpao_terrestre",
    "arrastar_para_o_cadafalso",
    "artefato_cortante",
    "asahum_cacadora_de_bestas",
    "ascensao_da_horda_lendaria",
    "ataque_magico",
    "atmosfera_pesada",
    "auto_defesa_magica",
    "baluarte_da_tundra_ancestral",
    "baluarte_de_espelhos_do_dragao",
    "banir",
    "barreira_de_gelo",
    "bastao_do_monge_incandescente",
    "bateria_de_cristal_de_poeira",
    "behemoth_29a",
    "bencao_do_rei_imovel",
    "bola_de_fogo",
    "bolsa_de_armadilhas_do_mestre_cacador",
    "bomba_de_fumaca",
    "broche_de_cristal",
    "broquel_explosivo",
    "campo_de_forca_de_cristal",
    "cauterizar",
    "cavaleiro_onear",
    "cetro_do_julgamento_perpetuo",
    "chamado_do_domador",
    "chave_mecanica_de_precisao",
    "chefe_carrasco",
    "cheiro_de_sangue",
    "circuito_de_sobrecarga",
    "colheita_dos_condenados",
    "combustao",
    "conferencia_dos_sabios_goblins",
    "constructo_arquivista",
    "constructo_carniceiro",
    "constructo_da_caldeira",
    "constructo_das_chamas_sif",
    "constructo_de_combate_arsenal_vivo",
    "constructo_fantasmagorico_guardiao_perpetuo",
    "contra_ataque",
    "convocacao",
    "coracao_da_forja",
    "coracao_de_cristal_puro",
    "correntes_do_cadafalso",
    "corte_seus_pescocos",
    "couraca_da_tundra_profunda",
    "cutelo_do_suplicio",
    "danca_das_mil_laminas",
    "decreto_de_excomunhao",
    "descarga_de_poeira_eletrica",
    "despertar_do_cavaleiro_bestial",
    "determinacao_cristalina",
    "disciplina",
    "dragao_de_cristal",
    "dragast",
    "edito_contra_a_ganancia",
    "enamor_o_grande_cavaleiro",
    "engenharia_rapida",
    "engrenagem_de_ressonancia",
    "escamas_do_dragao_eterno",
    "escudo_ardente",
    "escudo_da_devocao_cega",
    "escudo_improvisado",
    "espada_do_carrasco",
    "esquadrao_goblin",
    "estatica",
    "estilo_secreto_do_cacador",
    "estrela_do_amanha",
    "exumacao",
    "fenrir_o_grande_devorador",
    "fervor_penitencial",
    "filacteria_das_areias_negras",
    "fogo_purificador",
    "fortaleca_sua_fe",
    "fragmento_afiado",
    "garra_espectral",
    "garras_congeladas",
    "glaive_da_guarda_eterna",
    "golpes_pesados",
    "grande_inquisidor_vael_voz_do_trono",
    "grande_machado_sombrio",
    "grilhoes_pesados",
    "hati_constructo_gelido",
    "herois_lendarios_dos_goblin",
    "homunculo_carniceiro",
    "impulso_de_poder",
    "incensario_purificador",
    "interdito_sagrado",
    "isenora_santa_das_laminas",
    "izeref_executor_da_ordem_necrotica",
    "justicar_elyon_balanca_imutavel",
    "kael_cavaleiro_bestial",
    "kael_domador_de_feras",
    "lamina_de_cristal",
    "leone_cleriga_do_grupo_dos_herois",
    "liturgia_da_balanca",
    "luz_da_lua",
    "machado_de_gelo",
    "maestria_com_chamas",
    "manoplas_do_necromante",
    "manto_cerimonial_do_trono",
    "manto_das_brasas_vivas",
    "manto_de_ferro_do_algoz",
    "martelo_bigorna",
    "massacre_cristalino",
    "meditacao_das_cinzas",
    "mestra_da_forja",
    "mirar_na_cabeca",
    "mordida_glacial",
    "moyra_aprendiz_da_santa",
    "moyra_santa_das_laminas",
    "muralha_de_fogo",
    "naelis_grande_artesa_de_cristais",
    "nao_apresse_um_artista",
    "necromante_das_areias",
    "nucleo_de_invencoes_do_rei_goblin",
    "oraculo_real",
    "ordem_divina",
    "para_raios",
    "peitoral_de_brasas",
    "peste_das_areias_negras",
    "pet_canideo",
    "pet_felino",
    "pet_tartaruga",
    "pilhagem_de_cadaveres",
    "placas_de_cristal_draconiano",
    "polir_o_armamento",
    "ponto_final",
    "portador_da_chama",
    "quebra",
    "quimera",
    "quimera_carniceira",
    "quimera_negra",
    "racao_de_emergencia",
    "rainha_goblin",
    "rapieira_de_cristal",
    "rapinaria_7b3",
    "recalibracao_de_energia",
    "rede_de_contencao_de_feras",
    "rei_goblin",
    "residuo_incendiario",
    "retribuicao_dogmatica",
    "risada_sarcastica",
    "ritos_funebres",
    "sagracao_da_santa_das_laminas",
    "saque_rapido",
    "seda",
    "seguindo_ordens",
    "selo_da_confissao",
    "sentenca_da_guilhotina",
    "sica",
    "sino_do_cacador",
    "skoll_espreitador_solar",
    "tesouro_cristalino_dos_cacadores",
    "token_combustao",
    "token_seda",
    "tomo_das_sentencas_antigas",
    "traidor_goblin",
    "tristeza",
    "uivo_da_matilha_gelida",
    "vendaval_arcano",
    "veredito_inquisitorial",
    "voto_dos_cavaleiros_caidos",
    "xama_das_cinzas",
    "xia_cacador_de_bestas"
];

export const CARD_ART_FILES = {
    "Alucinação Cintlante": "alucinacao_cintlante.png",
    "Esquadrão Goblin": "esquadrao_goblin.png",
    "Rei Goblin": "rei_goblin.png",
    "Traidor Goblin": "traidor_goblin.png",
    "Rainha Goblin": "rainha_goblin.png",
    "Quimera Carniceira": "quimera_carniceira.png",
    "Necromante das Areias": "necromante_das_areias.png",
    "Isenora, Santa das Laminas": "isenora_santa_das_laminas.png",
    "Moyra, Aprendiz da Santa": "moyra_aprendiz_da_santa.png",
    "Naelis, Grande Artesã de Cristais": "naelis_grande_artesa_de_cristais.png",
    "Alka, Lutador do Grupo dos Heróis": "alka_lutador_do_grupo_dos_herois.png",
    "Leone, Clériga do Grupo dos Heróis": "leone_cleriga_do_grupo_dos_herois.png",
    "Enamor,O Grande Cavaleiro": "enamor_o_grande_cavaleiro.png",
    "Ageus, Constructo Cristalino": "ageus_constructo_cristalino.png",
    "Cavaleiro Onear": "cavaleiro_onear.png",
    "Moyra, Santa das Laminas": "moyra_santa_das_laminas.png",
    "Heróis Lendários dos Goblin": "herois_lendarios_dos_goblin.png",
    "Kael, Domador de Feras": "kael_domador_de_feras.png",
    "Behemoth-29A": "behemoth_29a.png",
    "Rapinária-7B3": "rapinaria_7b3.png",
    "Kael, Cavaleiro Bestial": "kael_cavaleiro_bestial.png",
    "Hati, Constructo Gélido": "hati_constructo_gelido.png",
    "Skoll, Espreitador Solar": "skoll_espreitador_solar.png",
    "Fenrir, o Grande Devorador": "fenrir_o_grande_devorador.png",
    "Oráculo Real": "oraculo_real.png",
    "Portador da Chama": "portador_da_chama.png",
    "Constructo Fantasmagórico Guardião Perpétuo": "constructo_fantasmagorico_guardiao_perpetuo.png",
    "Xia, Caçador de Bestas": "xia_cacador_de_bestas.png",
    "Asahum, Caçadora de Bestas": "asahum_cacadora_de_bestas.png",
    "Constructo de Combate, Arsenal Vivo": "constructo_de_combate_arsenal_vivo.png",
    "Xamã das cinzas": "xama_das_cinzas.png",
    "Alix, Dragão Flamejante": "alix_dragao_flamejante.png",
    "Constructo das Chamas, Sif": "constructo_das_chamas_sif.png",
    "Chefe Carrasco": "chefe_carrasco.png",
    "Izeref, Executor da Ordem Necrótica": "izeref_executor_da_ordem_necrotica.png",
    "Constructo Carniceiro": "constructo_carniceiro.png",
    "Grande Inquisidor Vael, Voz do Trono": "grande_inquisidor_vael_voz_do_trono.png",
    "Justiçar Elyon, Balança Imutável": "justicar_elyon_balanca_imutavel.png",
    "Constructo Arquivista": "constructo_arquivista.png",
    "Alevyr da Forja": "alevyr_da_forja.png",
    "Constructo da Caldeira": "constructo_da_caldeira.png",
    "Mestra da Forja": "mestra_da_forja.png",
    "Quimera": "quimera.png",
    "Broche de Cristal": "broche_de_cristal.png",
    "Lamina de Cristal": "lamina_de_cristal.png",
    "Dragão de Cristal": "dragao_de_cristal.png",
    "Homunculo Carniceiro": "homunculo_carniceiro.png",
    "Quimera Negra": "quimera_negra.png",
    "Dragast": "dragast.png",
    "Fragmento Afiado": "fragmento_afiado.png",
    "Rapieira de Cristal": "rapieira_de_cristal.png",
    "Garra espectral": "garra_espectral.png",
    "Machado de Gelo": "machado_de_gelo.png",
    "Grande Machado Sombrio": "grande_machado_sombrio.png",
    "Artefato Cortante": "artefato_cortante.png",
    "Bola de fogo": "bola_de_fogo.png",
    "Maestria com Chamas": "maestria_com_chamas.png",
    "Estatica": "estatica.png",
    "Para-raios": "para_raios.png",
    "Atmosfera Pesada": "atmosfera_pesada.png",
    "Vendaval Arcano": "vendaval_arcano.png",
    "Quebra!": "quebra.png",
    "Barreira de Gelo": "barreira_de_gelo.png",
    "Mordida Glacial": "mordida_glacial.png",
    "Contra Ataque": "contra_ataque.png",
    "Ataque Mágico": "ataque_magico.png",
    "Golpes Pesados": "golpes_pesados.png",
    "Massacre Cristalino": "massacre_cristalino.png",
    "Ponto Final": "ponto_final.png",
    "Auto Defesa Magica": "auto_defesa_magica.png",
    "Risada Sarcástica": "risada_sarcastica.png",
    "Alma em Chamas": "alma_em_chamas.png",
    "Fortaleça sua Fé": "fortaleca_sua_fe.png",
    "Saque Rápido": "saque_rapido.png",
    "Disciplina": "disciplina.png",
    "Ração de emergencia": "racao_de_emergencia.png",
    "Determinação Cristalina": "determinacao_cristalina.png",
    "Convocação": "convocacao.png",
    "Mirar na cabeça": "mirar_na_cabeca.png",
    "Ritos Fúnebres": "ritos_funebres.png",
    "Exumação": "exumacao.png",
    "Banir!": "banir.png",
    "Impulso de Poder": "impulso_de_poder.png",
    "Tristeza": "tristeza.png",
    "Luz da Lua": "luz_da_lua.png",
    "Ordem Divina": "ordem_divina.png",
    "Sica": "sica.png",
    "Sino do Caçador": "sino_do_cacador.png",
    "Arpão terrestre": "arpao_terrestre.png",
    "Bomba de fumaça": "bomba_de_fumaca.png",
    "Tesouro Cristalino dos Caçadores": "tesouro_cristalino_dos_cacadores.png",
    "Afie suas Lâminas": "afie_suas_laminas.png",
    "Estilo Secreto do Caçador": "estilo_secreto_do_cacador.png",
    "Corte seus Pescoços": "corte_seus_pescocos.png",
    "Peitoral de brasas": "peitoral_de_brasas.png",
    "Escudo ardente": "escudo_ardente.png",
    "Muralha de Fogo": "muralha_de_fogo.png",
    "Cauterizar": "cauterizar.png",
    "Estrela do amanhã": "estrela_do_amanha.png",
    "Espada do Carrasco": "espada_do_carrasco.png",
    "Seguindo Ordens": "seguindo_ordens.png",
    "Grilhões Pesados": "grilhoes_pesados.png",
    "Manoplas do Necromante": "manoplas_do_necromante.png",
    "Cheiro de Sangue": "cheiro_de_sangue.png",
    "Arrastar para o Cadafalso": "arrastar_para_o_cadafalso.png",
    "Colheita dos Condenados": "colheita_dos_condenados.png",
    "Sentença da Guilhotina": "sentenca_da_guilhotina.png",
    "Cutelo do Suplício": "cutelo_do_suplicio.png",
    "Édito contra a Ganância": "edito_contra_a_ganancia.png",
    "Retribuição Dogmática": "retribuicao_dogmatica.png",
    "Interdito Sagrado": "interdito_sagrado.png",
    "Selo da Confissão": "selo_da_confissao.png",
    "Cetro do Julgamento Perpétuo": "cetro_do_julgamento_perpetuo.png",
    "Bolsa de Armadilhas do Mestre Caçador": "bolsa_de_armadilhas_do_mestre_cacador.png",
    "Rede de Contenção de Feras": "rede_de_contencao_de_feras.png",
    "Escamas do Dragão Eterno": "escamas_do_dragao_eterno.png",
    "Bastão do Monge Incandescente": "bastao_do_monge_incandescente.png",
    "Fogo Purificador": "fogo_purificador.png",
    "Meditação das Cinzas": "meditacao_das_cinzas.png",
    "Incensário Purificador": "incensario_purificador.png",
    "Manto das Brasas Vivas": "manto_das_brasas_vivas.png",
    "Baluarte de Espelhos do Dragão": "baluarte_de_espelhos_do_dragao.png",
    "Placas de Cristal Draconiano": "placas_de_cristal_draconiano.png",
    "Voto dos Cavaleiros Caídos": "voto_dos_cavaleiros_caidos.png",
    "Coração de Cristal Puro": "coracao_de_cristal_puro.png",
    "Dança das Mil Lâminas": "danca_das_mil_laminas.png",
    "Filactéria das Areias Negras": "filacteria_das_areias_negras.png",
    "Peste das Areias Negras": "peste_das_areias_negras.png",
    "Amuleto de Ossos da Fábrica": "amuleto_de_ossos_da_fabrica.png",
    "Pilhagem de Cadáveres": "pilhagem_de_cadaveres.png",
    "Manto de Ferro do Algoz": "manto_de_ferro_do_algoz.png",
    "Correntes do Cadafalso": "correntes_do_cadafalso.png",
    "Decreto de Excomunhão": "decreto_de_excomunhao.png",
    "Liturgia da Balança": "liturgia_da_balanca.png",
    "Tomo das Sentenças Antigas": "tomo_das_sentencas_antigas.png",
    "Bênção do Rei Imóvel": "bencao_do_rei_imovel.png",
    "Veredito Inquisitorial": "veredito_inquisitorial.png",
    "Alabarda Cerimonial do Trono": "alabarda_cerimonial_do_trono.png",
    "Glaive da Guarda Eterna": "glaive_da_guarda_eterna.png",
    "Manto Cerimonial do Trono": "manto_cerimonial_do_trono.png",
    "Fervor Penitencial": "fervor_penitencial.png",
    "Escudo da Devoção Cega": "escudo_da_devocao_cega.png",
    "Baluarte da Tundra Ancestral": "baluarte_da_tundra_ancestral.png",
    "Couraça da Tundra Profunda": "couraca_da_tundra_profunda.png",
    "Uivo da Matilha Gélida": "uivo_da_matilha_gelida.png",
    "Chamado do Domador": "chamado_do_domador.png",
    "Garras Congeladas": "garras_congeladas.png",
    "Núcleo de Invenções do Rei Goblin": "nucleo_de_invencoes_do_rei_goblin.png",
    "Bateria de Cristal de Poeira": "bateria_de_cristal_de_poeira.png",
    "Chave Mecânica de Precisão": "chave_mecanica_de_precisao.png",
    "Engrenagem de Ressonância": "engrenagem_de_ressonancia.png",
    "Descarga de Poeira Elétrica": "descarga_de_poeira_eletrica.png",
    "Circuito de Sobrecarga": "circuito_de_sobrecarga.png",
    "Campo de Força de Cristal": "campo_de_forca_de_cristal.png",
    "Engenharia Rápida": "engenharia_rapida.png",
    "Recalibração de Energia": "recalibracao_de_energia.png",
    "Conferência dos Sábios Goblins": "conferencia_dos_sabios_goblins.png",
    "Martelo & Bigorna": "martelo_bigorna.png",
    "Escudo Improvisado": "escudo_improvisado.png",
    "Broquel Explosivo": "broquel_explosivo.png",
    "Armadura Antichamas": "armadura_antichamas.png",
    "Polir o Armamento": "polir_o_armamento.png",
    "Não Apresse um Artista": "nao_apresse_um_artista.png",
    "Coração da Forja": "coracao_da_forja.png",
    "Resíduo Incendiário": "residuo_incendiario.png",
    "Sagração da Santa das Lâminas": "sagracao_da_santa_das_laminas.png",
    "Ascensão da Horda Lendária": "ascensao_da_horda_lendaria.png",
    "Despertar do Cavaleiro Bestial": "despertar_do_cavaleiro_bestial.png",
    "Bola de Fogo": "bola_de_fogo.png",
    "Pet Canídeo": "pet_canideo.png",
    "Pet Felino": "pet_felino.png",
    "Pet Tartaruga": "pet_tartaruga.png",
    "Token de Seda": "token_seda.png",
    "Seda": "token_seda.png",
    "Token de Combustão": "token_combustao.png",
    "Combustão": "token_combustao.png"
};

export const FALLBACK_HEROES_BY_NAME = new Map(
    [{"uuid":"10000000-0000-0000-0000-000000000003","cardName":"Alucinação Cintlante","cardType":"HERO","color":"WHITE","rarity":"COMUM","cardSet":"BASE","descricao":"Aura: Um erro.","espirito":3,"ataque":3,"defesa":3,"vidaMaxima":20,"vidaAtual":20,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":[],"ordem":[],"classe":[],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["AURA"],"imagem":"alucinacao_cintlante.png"},{"uuid":"10000000-0000-0000-0000-000000000004","cardName":"Esquadrão Goblin","cardType":"HERO","color":"GREEN","rarity":"COMUM","cardSet":"BASE","descricao":"Uma vez por Combate: Ao jogar Magia, recebe +1 de Espírito.\nAo jogar Item: Recebe +1 de Ataque.\nAo jogar Ação: Recebe +1 de Defesa.\nFinal do Combate: Recupere 2 de Vida.","espirito":2,"ataque":4,"defesa":2,"vidaMaxima":12,"vidaAtual":12,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":["Goblin"],"afinidades":[],"ordem":[],"classe":["Emissor","Transformador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["UMA_VEZ_POR_COMBATE","FINAL_DO_COMBATE"],"imagem":"esquadrao_goblin.png"},{"uuid":"10000000-0000-0000-0000-000000000005","cardName":"Rei Goblin","cardType":"HERO","color":"GREEN","rarity":"COMUM","cardSet":"BASE","descricao":"Aura: Seus Goblins recebem +1 em seus efeitos.","espirito":3,"ataque":2,"defesa":1,"vidaMaxima":12,"vidaAtual":12,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":["Goblin"],"afinidades":[],"ordem":[],"classe":["Emissor","Transformador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["AURA"],"imagem":"rei_goblin.png"},{"uuid":"10000000-0000-0000-0000-000000000006","cardName":"Traidor Goblin","cardType":"HERO","color":"GREEN","rarity":"COMUM","cardSet":"BASE","descricao":"Início da Partida: Recebe +1 de Ataque e +1 de Defesa para cada Aliado Goblin.","espirito":0,"ataque":6,"defesa":1,"vidaMaxima":12,"vidaAtual":12,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":["Goblin"],"afinidades":[],"ordem":[],"classe":["Emissor","Transformador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["INICIO_DA_PARTIDA"],"imagem":"traidor_goblin.png"},{"uuid":"10000000-0000-0000-0000-000000000007","cardName":"Rainha Goblin","cardType":"HERO","color":"BLACK","rarity":"COMUM","cardSet":"BASE","descricao":"Final do Combate: Cure seus Aliados em 2.","espirito":3,"ataque":3,"defesa":2,"vidaMaxima":12,"vidaAtual":12,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":["Goblin"],"afinidades":[],"ordem":["Zumbi"],"classe":["Transformador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["FINAL_DO_COMBATE"],"imagem":"rainha_goblin.png"},{"uuid":"10000000-0000-0000-0000-000000000008","cardName":"Quimera Carniceira","cardType":"HERO","color":"BLACK","rarity":"COMUM","cardSet":"BASE","descricao":"Início do Combate: Para cada Aliado Zumbi: Recebe +1 de Espírito e +1 de Ataque e recupere 2 de Vida.","espirito":0,"ataque":5,"defesa":2,"vidaMaxima":14,"vidaAtual":14,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":["Constructo"],"afinidades":[],"ordem":["Zumbi"],"classe":["Transformador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["INICIO_DO_COMBATE"],"imagem":"quimera_carniceira.png"},{"uuid":"10000000-0000-0000-0000-000000000009","cardName":"Necromante das Areias","cardType":"HERO","color":"BLACK","rarity":"COMUM","cardSet":"BASE","descricao":"Início do Combate: Invocar: Ritos Fúnebres.","espirito":3,"ataque":3,"defesa":2,"vidaMaxima":15,"vidaAtual":15,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":[],"ordem":["Zumbi"],"classe":["Emissor","Transformador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["INICIO_DO_COMBATE","INVOCAR"],"imagem":"necromante_das_areias.png"},{"uuid":"10000000-0000-0000-0000-000000000010","cardName":"Isenora, Santa das Laminas","cardType":"HERO","color":"BLUE","rarity":"COMUM","cardSet":"BASE","descricao":"Aura: Seus Aliados recebem +1 de Espírito, +1 de Ataque e +1 de Defesa.\nFinal do Combate: Cause 5 de Dano Mágico ao Herói Inimigo.","espirito":2,"ataque":5,"defesa":1,"vidaMaxima":13,"vidaAtual":13,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":["Cristal"],"ordem":["Cristal"],"classe":["Criador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["AURA","FINAL_DO_COMBATE","DANO_MAGICO"],"imagem":"isenora_santa_das_laminas.png"},{"uuid":"10000000-0000-0000-0000-000000000011","cardName":"Moyra, Aprendiz da Santa","cardType":"HERO","color":"BLUE","rarity":"COMUM","cardSet":"BASE","descricao":"Início do Combate: Cause 3 de Dano Mágico ao Herói Inimigo.\nAo jogar Magia: Recebe +3 de Ataque até o Final do Combate.","espirito":1,"ataque":4,"defesa":0,"vidaMaxima":13,"vidaAtual":13,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":["Cristal"],"ordem":["Cristal"],"classe":["Criador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["INICIO_DO_COMBATE","DANO_MAGICO","FINAL_DO_COMBATE"],"imagem":"moyra_aprendiz_da_santa.png"},{"uuid":"10000000-0000-0000-0000-000000000012","cardName":"Naelis, Grande Artesã de Cristais","cardType":"HERO","color":"BLUE","rarity":"COMUM","cardSet":"BASE","descricao":"Início da Partida: Invoque: Rapieira de Cristal.","espirito":1,"ataque":4,"defesa":0,"vidaMaxima":12,"vidaAtual":12,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":["Cristal"],"ordem":["Cristal"],"classe":["Criador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["INICIO_DA_PARTIDA","INVOCAR"],"imagem":"naelis_grande_artesa_de_cristais.png"},{"uuid":"10000000-0000-0000-0000-000000000013","cardName":"Alka, Lutador do Grupo dos Heróis","cardType":"HERO","color":"WHITE","rarity":"COMUM","cardSet":"BASE","descricao":"Final do Combate: Se durante o Combate seu Ataque foi igual ou superior a 10: Realize um Ataque extra.","espirito":1,"ataque":7,"defesa":3,"vidaMaxima":14,"vidaAtual":14,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":[],"ordem":[],"classe":["Criador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["FINAL_DO_COMBATE"],"imagem":"alka_lutador_do_grupo_dos_herois.png"},{"uuid":"10000000-0000-0000-0000-000000000014","cardName":"Leone, Clériga do Grupo dos Heróis","cardType":"HERO","color":"WHITE","rarity":"COMUM","cardSet":"BASE","descricao":"Início do Combate: Cure seus Aliados em 2.\nFinal do Combate: Cause Dano Direto ao Herói Inimigo equivalente à soma da Cura do Combate.","espirito":1,"ataque":3,"defesa":3,"vidaMaxima":15,"vidaAtual":15,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":["Ar"],"ordem":[],"classe":["Transformador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["INICIO_DO_COMBATE","FINAL_DO_COMBATE","DANO_DIRETO"],"imagem":"leone_cleriga_do_grupo_dos_herois.png"},{"uuid":"10000000-0000-0000-0000-000000000015","cardName":"Enamor,O Grande Cavaleiro","cardType":"HERO","color":"BLUE","rarity":"COMUM","cardSet":"BASE","descricao":"Início do Combate: Se não estiver com Dragast Equipada: Invocar: Dragast.","espirito":1,"ataque":5,"defesa":3,"vidaMaxima":14,"vidaAtual":14,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":["Cristal"],"ordem":["Cavaleiro"],"classe":["Criador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["INICIO_DO_COMBATE","INVOCAR"],"imagem":"enamor_o_grande_cavaleiro.png"},{"uuid":"10000000-0000-0000-0000-000000000016","cardName":"Ageus, Constructo Cristalino","cardType":"HERO","color":"BLUE","rarity":"COMUM","cardSet":"BASE","descricao":"Aura: Quando um Cavaleiro Aliado morre, seus outros Cavaleiros Aliados vivos recebem +2 de Espírito e +2 de Defesa.","espirito":2,"ataque":7,"defesa":3,"vidaMaxima":15,"vidaAtual":15,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":["Constructo"],"afinidades":["Cristal"],"ordem":["Cavaleiro"],"classe":["Criador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["AURA","MORTO","VIVO"],"imagem":"ageus_constructo_cristalino.png"},{"uuid":"10000000-0000-0000-0000-000000000017","cardName":"Cavaleiro Onear","cardType":"HERO","color":"BLUE","rarity":"COMUM","cardSet":"BASE","descricao":"Uma vez por Combate: Ao jogar Magia, recebe +2 de Ataque.","espirito":2,"ataque":7,"defesa":3,"vidaMaxima":15,"vidaAtual":15,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":["Cristal"],"ordem":["Cavaleiro"],"classe":["Criador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["UMA_VEZ_POR_COMBATE"],"imagem":"cavaleiro_onear.png"},{"uuid":"10000000-0000-0000-0000-000000000018","cardName":"Moyra, Santa das Laminas","cardType":"HERO","color":"BLUE","rarity":"COMUM","cardSet":"BASE","descricao":"Início do Combate: O Herói Inimigo recebe -2 de Espírito.\nAo jogar Magia: Recebe +2 de Ataque.\nFinal do Combate: Cause 5 de Dano Mágico ao Herói Inimigo.","espirito":2,"ataque":5,"defesa":2,"vidaMaxima":14,"vidaAtual":14,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":["Cristal"],"ordem":["Cristal"],"classe":["Criador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["INICIO_DO_COMBATE","FINAL_DO_COMBATE","DANO_MAGICO"],"imagem":"moyra_santa_das_laminas.png"},{"uuid":"10000000-0000-0000-0000-000000000019","cardName":"Heróis Lendários dos Goblin","cardType":"HERO","color":"GREEN","rarity":"COMUM","cardSet":"BASE","descricao":"Ao jogar Magia: Recebe +1 de Espírito.\nAo jogar Item: Recebe +1 de Ataque.\nAo jogar Ação: Recebe +1 de Defesa.\nFinal do Combate: Cure seus Aliados em 3.","espirito":3,"ataque":5,"defesa":3,"vidaMaxima":12,"vidaAtual":12,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":["Goblin"],"afinidades":[],"ordem":[],"classe":["Emissor","Transformador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["FINAL_DO_COMBATE"],"imagem":"herois_lendarios_dos_goblin.png"},{"uuid":"10000000-0000-0000-0000-000000000020","cardName":"Kael, Domador de Feras","cardType":"HERO","color":"GREEN","rarity":"COMUM","cardSet":"BASE","descricao":"Início do Combate: Seus Constructos recebem +2 de Ataque e recuperam 2 pontos de Vida.","espirito":1,"ataque":5,"defesa":2,"vidaMaxima":15,"vidaAtual":15,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":[],"ordem":[],"classe":["Transformador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["INICIO_DO_COMBATE"],"imagem":"kael_domador_de_feras.png"},{"uuid":"10000000-0000-0000-0000-000000000021","cardName":"Behemoth-29A","cardType":"HERO","color":"GREEN","rarity":"COMUM","cardSet":"BASE","descricao":"Início do Combate: Sofre 3 de Dano Direto para Curar Kael, Domador de Feras, em 3 pontos.","espirito":0,"ataque":4,"defesa":4,"vidaMaxima":17,"vidaAtual":17,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":["Constructo"],"afinidades":["Terra"],"ordem":[],"classe":["Criador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["INICIO_DO_COMBATE","DANO_DIRETO"],"imagem":"behemoth_29a.png"},{"uuid":"10000000-0000-0000-0000-000000000022","cardName":"Rapinária-7B3","cardType":"HERO","color":"GREEN","rarity":"COMUM","cardSet":"BASE","descricao":"Início do Combate: Invocar: Vendaval Arcano\nInvocar: Barreira de Gelo.","espirito":0,"ataque":4,"defesa":0,"vidaMaxima":12,"vidaAtual":12,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":["Constructo"],"afinidades":["Ar","Agua"],"ordem":[],"classe":["Criador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["INICIO_DO_COMBATE","INVOCAR"],"imagem":"rapinaria_7b3.png"},{"uuid":"10000000-0000-0000-0000-000000000023","cardName":"Kael, Cavaleiro Bestial","cardType":"HERO","color":"GREEN","rarity":"COMUM","cardSet":"BASE","descricao":"Início do Combate: Seus Constructos recebem +2 de Ataque e recuperam 2 pontos de Vida.\nFinal do Combate: Cause 2 de Dano Direto aos Heróis Inimigos.","espirito":1,"ataque":10,"defesa":2,"vidaMaxima":16,"vidaAtual":16,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":["Constructo"],"afinidades":["Agua","Ar","Terra"],"ordem":[],"classe":["Transformador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["INICIO_DO_COMBATE","FINAL_DO_COMBATE","DANO_DIRETO"],"imagem":"kael_cavaleiro_bestial.png"},{"uuid":"10000000-0000-0000-0000-000000000024","cardName":"Hati, Constructo Gélido","cardType":"HERO","color":"GREEN","rarity":"COMUM","cardSet":"BASE","descricao":"Aura: Recebe +3 de Ataque contra Emissores.\nAura: Quando um Herói Inimigo morre, transfere sua Vulnerabilidade para outro Herói Inimigo.","espirito":1,"ataque":6,"defesa":1,"vidaMaxima":14,"vidaAtual":14,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":["Constructo"],"afinidades":["Agua"],"ordem":[],"classe":["Criador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["AURA","MORTO","VULNERABILIDADE"],"imagem":"hati_constructo_gelido.png"},{"uuid":"10000000-0000-0000-0000-000000000025","cardName":"Skoll, Espreitador Solar","cardType":"HERO","color":"GREEN","rarity":"COMUM","cardSet":"BASE","descricao":"Início do Combate: Aumente a Vulnerabilidade de um Herói Inimigo em 1.\nInício do Combate: Recebe +X de Ataque, onde X é a Vulnerabilidade do Herói Inimigo.","espirito":0,"ataque":7,"defesa":2,"vidaMaxima":13,"vidaAtual":13,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":["Constructo"],"afinidades":["Agua"],"ordem":[],"classe":["Criador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["INICIO_DO_COMBATE","VULNERABILIDADE"],"imagem":"skoll_espreitador_solar.png"},{"uuid":"10000000-0000-0000-0000-000000000026","cardName":"Fenrir, o Grande Devorador","cardType":"HERO","color":"GREEN","rarity":"COMUM","cardSet":"BASE","descricao":"Início da Partida: Seus Constructos recebem +2 de Ataque e +1 de Defesa.\nInício do Combate: Recebe Ataque Duplo se a Vulnerabilidade do Herói Inimigo for maior que 5.","espirito":2,"ataque":7,"defesa":1,"vidaMaxima":15,"vidaAtual":15,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":["Constructo"],"afinidades":["Agua"],"ordem":[],"classe":["Criador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["INICIO_DA_PARTIDA","INICIO_DO_COMBATE","VULNERABILIDADE"],"imagem":"fenrir_o_grande_devorador.png"},{"uuid":"10000000-0000-0000-0000-000000000027","cardName":"Oráculo Real","cardType":"HERO","color":"YELLOW","rarity":"COMUM","cardSet":"BASE","descricao":"Aura: Seus Guardiões do Trono recebem +2 de Ataque e +1 de Defesa.\nInício do Combate: Invocar: Fortaleça sua Fé.","espirito":1,"ataque":6,"defesa":0,"vidaMaxima":18,"vidaAtual":18,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":[],"ordem":["Guardião do Trono"],"classe":["Transformador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["AURA","INICIO_DO_COMBATE","INVOCAR"],"imagem":"oraculo_real.png"},{"uuid":"10000000-0000-0000-0000-000000000028","cardName":"Portador da Chama","cardType":"HERO","color":"YELLOW","rarity":"COMUM","cardSet":"BASE","descricao":"Início do Combate: Recupere 2 de Vida e adicione até duas cartas do seu descarte para sua mão.","espirito":0,"ataque":5,"defesa":0,"vidaMaxima":16,"vidaAtual":16,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":[],"ordem":["Guardião do Trono"],"classe":["Transformador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["INICIO_DO_COMBATE"],"imagem":"portador_da_chama.png"},{"uuid":"10000000-0000-0000-0000-000000000029","cardName":"Constructo Fantasmagórico Guardião Perpétuo","cardType":"HERO","color":"YELLOW","rarity":"COMUM","cardSet":"BASE","descricao":"Empunhadura Quádrupla.\nAo jogar Arma: Recebe +2 de Ataque até o Final do Combate e recupere 2 pontos de Vida.","espirito":0,"ataque":6,"defesa":0,"vidaMaxima":17,"vidaAtual":17,"maxWeaponSlots":4,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":["Constructo"],"afinidades":[],"ordem":["Guardião do Trono"],"classe":["Transformador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["EMPUNHADURA","FINAL_DO_COMBATE"],"imagem":"constructo_fantasmagorico_guardiao_perpetuo.png"},{"uuid":"10000000-0000-0000-0000-000000000030","cardName":"Xia, Caçador de Bestas","cardType":"HERO","color":"RED","rarity":"COMUM","cardSet":"BASE","descricao":"Início do Combate: Jogue a próxima Ferramenta do seu baralho.","espirito":1,"ataque":8,"defesa":2,"vidaMaxima":16,"vidaAtual":16,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":[],"ordem":[],"classe":["Criador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["INICIO_DO_COMBATE"],"imagem":"xia_cacador_de_bestas.png"},{"uuid":"10000000-0000-0000-0000-000000000031","cardName":"Asahum, Caçadora de Bestas","cardType":"HERO","color":"RED","rarity":"COMUM","cardSet":"BASE","descricao":"Final do Combate: As Jóias e Ferramentas anexadas em Asahum, Caçadora de Bestas retornam para o baralho.","espirito":2,"ataque":8,"defesa":1,"vidaMaxima":16,"vidaAtual":16,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":[],"ordem":[],"classe":["Criador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["FINAL_DO_COMBATE"],"imagem":"asahum_cacadora_de_bestas.png"},{"uuid":"10000000-0000-0000-0000-000000000032","cardName":"Constructo de Combate, Arsenal Vivo","cardType":"HERO","color":"RED","rarity":"COMUM","cardSet":"BASE","descricao":"Empunhadura Quádrupla.\nInício do Combate: Desequipe uma Arma Aliada e Equipe-a em Constructo de Combate, Arsenal Vivo.","espirito":1,"ataque":8,"defesa":1,"vidaMaxima":16,"vidaAtual":16,"maxWeaponSlots":4,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":["Constructo"],"afinidades":[],"ordem":[],"classe":["Criador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["EMPUNHADURA","INICIO_DO_COMBATE"],"imagem":"constructo_de_combate_arsenal_vivo.png"},{"uuid":"10000000-0000-0000-0000-000000000033","cardName":"Xamã das cinzas","cardType":"HERO","color":"RED","rarity":"COMUM","cardSet":"BASE","descricao":"Aura: Seus Heróis recebem +1 de Dano Bônus.\nInício do Combate: Invocar: Bola de Fogo.","espirito":3,"ataque":4,"defesa":2,"vidaMaxima":15,"vidaAtual":15,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":["Fogo"],"ordem":[],"classe":["Criador","Emissor"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["AURA","DANO_BONUS","INICIO_DO_COMBATE","INVOCAR"],"imagem":"xama_das_cinzas.png"},{"uuid":"10000000-0000-0000-0000-000000000034","cardName":"Alix, Dragão Flamejante","cardType":"HERO","color":"RED","rarity":"COMUM","cardSet":"BASE","descricao":"Final do Combate: Cause 3 de Dano Mágico ao Herói Inimigo.\nAo jogar Armadura: Invocar: Bola de Fogo.","espirito":2,"ataque":5,"defesa":2,"vidaMaxima":14,"vidaAtual":14,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":["Fogo"],"ordem":[],"classe":["Criador","Emissor"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["FINAL_DO_COMBATE","DANO_MAGICO","INVOCAR"],"imagem":"alix_dragao_flamejante.png"},{"uuid":"10000000-0000-0000-0000-000000000035","cardName":"Constructo das Chamas, Sif","cardType":"HERO","color":"RED","rarity":"COMUM","cardSet":"BASE","descricao":"Início da Partida: Adicione dois Feitiços Fogo de seu baralho para seu descarte.\nInício do Combate: Adicione dois Feitiços Fogo do descarte para sua mão.","espirito":2,"ataque":7,"defesa":2,"vidaMaxima":15,"vidaAtual":15,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":["Constructo"],"afinidades":["Fogo"],"ordem":[],"classe":["Criador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["INICIO_DA_PARTIDA","INICIO_DO_COMBATE"],"imagem":"constructo_das_chamas_sif.png"},{"uuid":"10000000-0000-0000-0000-000000000036","cardName":"Chefe Carrasco","cardType":"HERO","color":"BLACK","rarity":"COMUM","cardSet":"BASE","descricao":"Início do Combate: Se não estiver com Espada do Carrasco Equipada: Invocar: Espada do Carrasco.\nSe o Herói Inimigo tiver vida menor que a sua Vida Máxima, a Defesa do Herói Inimigo se torna 0 até o Final do Combate.","espirito":0,"ataque":6,"defesa":2,"vidaMaxima":15,"vidaAtual":15,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":[],"ordem":[],"classe":["Criador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["INICIO_DO_COMBATE","INVOCAR","FINAL_DO_COMBATE"],"imagem":"chefe_carrasco.png"},{"uuid":"10000000-0000-0000-0000-000000000037","cardName":"Izeref, Executor da Ordem Necrótica","cardType":"HERO","color":"BLACK","rarity":"COMUM","cardSet":"BASE","descricao":"Aura: O Herói Inimigo é considerado morto se sua vida ficar abaixo de 0.","espirito":1,"ataque":7,"defesa":3,"vidaMaxima":14,"vidaAtual":14,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":[],"ordem":[],"classe":["Criador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["AURA","MORTO"],"imagem":"izeref_executor_da_ordem_necrotica.png"},{"uuid":"10000000-0000-0000-0000-000000000038","cardName":"Constructo Carniceiro","cardType":"HERO","color":"BLACK","rarity":"COMUM","cardSet":"BASE","descricao":"Aura: Quando um Herói Inimigo sofrer Dano, ele sofre 1 de Dano a mais.","espirito":2,"ataque":7,"defesa":2,"vidaMaxima":15,"vidaAtual":15,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":["Constructo"],"afinidades":[],"ordem":[],"classe":["Emissor"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["AURA"],"imagem":"constructo_carniceiro.png"},{"uuid":"10000000-0000-0000-0000-000000000039","cardName":"Grande Inquisidor Vael, Voz do Trono","cardType":"HERO","color":"YELLOW","rarity":"COMUM","cardSet":"BASE","descricao":"Aura: Sempre que uma carta for invocada pelo oponente, o Herói Inimigo sofre 1 de Dano Direto.\nInício do Combate: Se o Herói Inimigo possuir 2 ou mais cartas anexadas, o Herói Inimigo recebe +1 de Vulnerabilidade até o Final do Combate.","espirito":3,"ataque":4,"defesa":0,"vidaMaxima":19,"vidaAtual":19,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":[],"ordem":["Guardião do Trono"],"classe":["Transformador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["AURA","INVOCAR","DANO_DIRETO","INICIO_DO_COMBATE","VULNERABILIDADE","FINAL_DO_COMBATE"],"imagem":"grande_inquisidor_vael_voz_do_trono.png"},{"uuid":"10000000-0000-0000-0000-000000000040","cardName":"Justiçar Elyon, Balança Imutável","cardType":"HERO","color":"YELLOW","rarity":"COMUM","cardSet":"BASE","descricao":"Final do Combate: Para cada duas cartas jogadas pelo oponente neste turno: Seu Herói recupera 3 de Vida e o Herói Inimigo sofre 3 de Dano Mágico.","espirito":3,"ataque":5,"defesa":0,"vidaMaxima":20,"vidaAtual":20,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":[],"ordem":["Guardião do Trono"],"classe":["Transformador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["FINAL_DO_COMBATE","DANO_MAGICO"],"imagem":"justicar_elyon_balanca_imutavel.png"},{"uuid":"10000000-0000-0000-0000-000000000041","cardName":"Constructo Arquivista","cardType":"HERO","color":"YELLOW","rarity":"COMUM","cardSet":"BASE","descricao":"Aura: Sempre que uma Tristeza for descartada, Seus Heróis recebem +1 de Ataque.\nInício do Combate: Invocar: Risada Sarcástica.","espirito":3,"ataque":4,"defesa":0,"vidaMaxima":18,"vidaAtual":18,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":["Constructo"],"afinidades":[],"ordem":["Guardião do Trono"],"classe":["Transformador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["AURA","INVOCAR","INICIO_DO_COMBATE"],"imagem":"constructo_arquivista.png"},{"uuid":"10000000-0000-0000-0000-000000000042","cardName":"Alevyr da Forja","cardType":"HERO","color":"RED","rarity":"COMUM","cardSet":"BASE","descricao":"Aura: Seus Escudos recebem Encore.\nFinal do Combate: Impacto.","espirito":1,"ataque":4,"defesa":4,"vidaMaxima":14,"vidaAtual":14,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":[],"ordem":[],"classe":["Criador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["AURA","FINAL_DO_COMBATE"],"imagem":"alevyr_da_forja.png"},{"uuid":"10000000-0000-0000-0000-000000000043","cardName":"Constructo da Caldeira","cardType":"HERO","color":"RED","rarity":"COMUM","cardSet":"BASE","descricao":"Aura: Seus Aliados recebem +1 de Dano em efeitos de Impacto.","espirito":0,"ataque":3,"defesa":4,"vidaMaxima":15,"vidaAtual":15,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":["Constructo"],"afinidades":[],"ordem":[],"classe":["Criador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["AURA"],"imagem":"constructo_da_caldeira.png"},{"uuid":"10000000-0000-0000-0000-000000000044","cardName":"Mestra da Forja","cardType":"HERO","color":"RED","rarity":"COMUM","cardSet":"BASE","descricao":"Início do Combate: Invocar: Martelo & Bigorna.\nFinal do Combate: Impacto.","espirito":1,"ataque":4,"defesa":4,"vidaMaxima":14,"vidaAtual":14,"maxWeaponSlots":2,"reducaoDano":0,"danoBonus":0,"vulnerabilidade":0,"racas":[],"afinidades":[],"ordem":[],"classe":["Criador"],"estaVivo":true,"estaAtivo":true,"unica":false,"keywords":["INICIO_DO_COMBATE","INVOCAR","FINAL_DO_COMBATE"],"imagem":"mestra_da_forja.png"}].map(h => [toSnakeCaseKey(h.cardName), h])
);

export const FALLBACK_CARDS_BY_NAME = new Map(
    [{"uuid":"20000000-0000-0000-0000-000000000001","cardName":"Quimera","cardType":"ITEM","color":"BLUE","rarity":"COMUM","cardSet":"BASE","itemType":"ARMA","empunhadura":1,"descricao":"Arma - Empunhadura Simples - Cavaleiro\nSeu Herói Recebe +2 de Espírito +2 de Ataque +2 de Defesa.\nFinal do Combate: Recupere Vida igual seu Espírito.","racas":[],"ordem":["Cavaleiro"],"classe":[],"afinidades":[],"unica":false,"keywords":["EMPUNHADURA","FINAL_DO_COMBATE"],"imagem":"quimera.png"},{"uuid":"20000000-0000-0000-0000-000000000002","cardName":"Broche de Cristal","cardType":"ITEM","color":"BLUE","rarity":"COMUM","cardSet":"BASE","itemType":"JOIA","empunhadura":0,"descricao":"Jóia - Cristal\nSeu Herói Recebe +2 de Defesa +2 de Espírito.","racas":[],"ordem":["Cristal"],"classe":[],"afinidades":[],"unica":false,"keywords":[],"imagem":"broche_de_cristal.png"},{"uuid":"20000000-0000-0000-0000-000000000003","cardName":"Lamina de Cristal","cardType":"ITEM","color":"BLUE","rarity":"COMUM","cardSet":"BASE","itemType":"ARMA","empunhadura":1,"descricao":"Arma - Empunhadura Simples - Cristal\nSeu Herói Recebe +1 de Ataque.","racas":[],"ordem":["Cristal"],"classe":[],"afinidades":[],"unica":false,"keywords":["EMPUNHADURA"],"imagem":"lamina_de_cristal.png"},{"uuid":"20000000-0000-0000-0000-000000000004","cardName":"Dragão de Cristal","cardType":"ITEM","color":"BLUE","rarity":"COMUM","cardSet":"BASE","itemType":"JOIA","empunhadura":0,"descricao":"Jóia - Cristal - Cavaleiro\nFinal do Combate: O Herói Inimigo Sofre 6 de Dano Mágico.","racas":[],"ordem":["Cristal","Cavaleiro"],"classe":[],"afinidades":[],"unica":false,"keywords":["FINAL_DO_COMBATE","DANO_MAGICO"],"imagem":"dragao_de_cristal.png"},{"uuid":"20000000-0000-0000-0000-000000000005","cardName":"Homunculo Carniceiro","cardType":"ITEM","color":"BLACK","rarity":"COMUM","cardSet":"BASE","itemType":"JOIA","empunhadura":0,"descricao":"Jóia - Zumbi\nFinal do Combate: Recupere 1 de Vida. O Herói Inimigo Sofre 1 de Dano Direto.","racas":[],"ordem":["Zumbi"],"classe":[],"afinidades":[],"unica":false,"keywords":["FINAL_DO_COMBATE","DANO_DIRETO"],"imagem":"homunculo_carniceiro.png"},{"uuid":"20000000-0000-0000-0000-000000000006","cardName":"Quimera Negra","cardType":"ITEM","color":"BLACK","rarity":"COMUM","cardSet":"BASE","itemType":"ARMA","empunhadura":1,"descricao":"Arma - Empunhadura Simples - Zumbi\nInício do Combate: O Herói Inimigo Recebe -1 de Espírito -1 de Ataque -1 de Defesa -1 de Vida Máxima até o Final da Partida.","racas":[],"ordem":["Zumbi"],"classe":[],"afinidades":[],"unica":false,"keywords":["EMPUNHADURA","INICIO_DO_COMBATE"],"imagem":"quimera_negra.png"},{"uuid":"20000000-0000-0000-0000-000000000007","cardName":"Dragast","cardType":"ITEM","color":"BLUE","rarity":"COMUM","cardSet":"BASE","itemType":"ARMA","empunhadura":1,"descricao":"Arma - Empunhadura Simples - Cavaleiro\nSeu Herói Recebe +2 de Espírito e +2 de Ataque.\nFinal do Combate: Recupere 2 pontos de Vida.","racas":[],"ordem":["Cavaleiro"],"classe":[],"afinidades":[],"unica":false,"keywords":["EMPUNHADURA","FINAL_DO_COMBATE"],"imagem":"dragast.png"},{"uuid":"20000000-0000-0000-0000-000000000008","cardName":"Fragmento Afiado","cardType":"ITEM","color":"BLUE","rarity":"COMUM","cardSet":"BASE","itemType":"ARMA","empunhadura":1,"descricao":"Arma - Empunhadura Simples - Cavaleiro\nSeus Heróis Recebem +2 de Ataque. Recupere 2 pontos de Vida.","racas":[],"ordem":["Cavaleiro"],"classe":[],"afinidades":[],"unica":false,"keywords":["EMPUNHADURA"],"imagem":"fragmento_afiado.png"},{"uuid":"20000000-0000-0000-0000-000000000009","cardName":"Rapieira de Cristal","cardType":"ITEM","color":"BLUE","rarity":"COMUM","cardSet":"BASE","itemType":"ARMA","empunhadura":1,"descricao":"Arma - Empunhadura Simples - Cristal\nSeu Herói Recebe +3 de Ataque.\nFinal do Combate: O Herói Inimigo Sofre 5 de Dano Mágico.","racas":[],"ordem":["Cristal"],"classe":[],"afinidades":[],"unica":false,"keywords":["EMPUNHADURA","FINAL_DO_COMBATE","DANO_MAGICO"],"imagem":"rapieira_de_cristal.png"},{"uuid":"20000000-0000-0000-0000-000000000010","cardName":"Garra espectral","cardType":"ITEM","color":"NEUTRAL","rarity":"COMUM","cardSet":"BASE","itemType":"ARMA","empunhadura":1,"descricao":"Arma - Empunhadura Simples\nSeu Herói Recebe +2 de Ataque.\nO Herói Inimigo Recebe -2 de Espírito até o Final do Combate.\nInício do Combate: O Herói Inimigo Recebe -2 de Espírito até o Final do Combate.\nSeus Ataques causam Dano Mágico em vez de Físico.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["EMPUNHADURA","INICIO_DO_COMBATE","FINAL_DO_COMBATE","DANO_MAGICO","DANO_FISICO"],"imagem":"garra_espectral.png"},{"uuid":"20000000-0000-0000-0000-000000000011","cardName":"Machado de Gelo","cardType":"ITEM","color":"GREEN","rarity":"COMUM","cardSet":"BASE","itemType":"ARMA","empunhadura":1,"descricao":"Arma - Empunhadura Simples\nSeu Herói Recebe +2 de Ataque e +2 de Defesa.\nAo Equipar: O Herói Inimigo Recebe +1 de Vulnerabilidade até o Final da Partida.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["EMPUNHADURA","AO_EQUIPAR","VULNERABILIDADE"],"imagem":"machado_de_gelo.png"},{"uuid":"20000000-0000-0000-0000-000000000012","cardName":"Grande Machado Sombrio","cardType":"ITEM","color":"GREEN","rarity":"COMUM","cardSet":"BASE","itemType":"ARMA","empunhadura":2,"descricao":"Arma - Empunhadura Dupla\nSeu Herói Recebe +4 de Ataque e +2 de Defesa.\nAo Equipar: Os Heróis Inimigos Recebem +1 de Vulnerabilidade até o Final da Partida.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["EMPUNHADURA","AO_EQUIPAR","VULNERABILIDADE"],"imagem":"grande_machado_sombrio.png"},{"uuid":"20000000-0000-0000-0000-000000000013","cardName":"Artefato Cortante","cardType":"ITEM","color":"YELLOW","rarity":"COMUM","cardSet":"BASE","itemType":"ARMA","empunhadura":2,"descricao":"Arma - Empunhadura Dupla - Guardião do Trono\nSeu Herói Recebe +3 de Ataque e +1 de Defesa.\nFinal do Combate: Cure seus Heróis em 2.","racas":[],"ordem":["Guardião do Trono"],"classe":[],"afinidades":[],"unica":false,"keywords":["EMPUNHADURA","FINAL_DO_COMBATE"],"imagem":"artefato_cortante.png"},{"uuid":"30000000-0000-0000-0000-000000000001","cardName":"Bola de fogo","cardType":"SPELL","color":"NEUTRAL","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Fogo\nO Herói Inimigo Sofre 4 mais seu Espírito de Dano Mágico.","racas":[],"ordem":[],"classe":[],"afinidades":["Fogo"],"unica":false,"keywords":["DANO_MAGICO"],"imagem":"bola_de_fogo.png"},{"uuid":"30000000-0000-0000-0000-000000000002","cardName":"Maestria com Chamas","cardType":"SPELL","color":"NEUTRAL","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Fogo\nInvocar: Bola de Fogo\nInvocar: Bola de Fogo\nInvocar: Bola de Fogo.\nExilar.","racas":[],"ordem":[],"classe":[],"afinidades":["Fogo"],"unica":false,"keywords":["INVOCAR","EXILAR"],"imagem":"maestria_com_chamas.png"},{"uuid":"30000000-0000-0000-0000-000000000003","cardName":"Estatica","cardType":"SPELL","color":"NEUTRAL","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Ar\nO Herói Inimigo Sofre 3 mais seu Espírito de Dano Mágico. Invocar: Estatica.","racas":[],"ordem":[],"classe":[],"afinidades":["Ar"],"unica":false,"keywords":["DANO_MAGICO","INVOCAR"],"imagem":"estatica.png"},{"uuid":"30000000-0000-0000-0000-000000000004","cardName":"Para-raios","cardType":"SPELL","color":"NEUTRAL","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Ar\nSeu Herói Recebe +1 de Espírito até o Final do Combate.\nInvocar: Estatica.","racas":[],"ordem":[],"classe":[],"afinidades":["Ar"],"unica":false,"keywords":["FINAL_DO_COMBATE","INVOCAR"],"imagem":"para_raios.png"},{"uuid":"30000000-0000-0000-0000-000000000005","cardName":"Atmosfera Pesada","cardType":"SPELL","color":"BLACK","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Ar - Zumbi\nOs Heróis Inimigos Recebem -1 de Espírito até o Final da Partida.","racas":[],"ordem":["Zumbi"],"classe":[],"afinidades":["Ar"],"unica":false,"keywords":[],"imagem":"atmosfera_pesada.png"},{"uuid":"30000000-0000-0000-0000-000000000006","cardName":"Vendaval Arcano","cardType":"SPELL","color":"NEUTRAL","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Ar\nDescarte um Item Aleatório de cada Herói Inimigo.","racas":[],"ordem":[],"classe":[],"afinidades":["Ar"],"unica":false,"keywords":[],"imagem":"vendaval_arcano.png"},{"uuid":"30000000-0000-0000-0000-000000000007","cardName":"Quebra!","cardType":"SPELL","color":"NEUTRAL","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Agua\nEscolha um Item Equipado no Herói Inimigo e o Descarte.","racas":[],"ordem":[],"classe":[],"afinidades":["Agua"],"unica":false,"keywords":[],"imagem":"quebra.png"},{"uuid":"30000000-0000-0000-0000-000000000008","cardName":"Barreira de Gelo","cardType":"SPELL","color":"GREEN","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Agua\nEnquanto estiver Equipada: Seus Heróis Recebem +1 de Espírito e +2 de Defesa.","racas":[],"ordem":[],"classe":[],"afinidades":["Agua"],"unica":false,"keywords":["AO_EQUIPAR"],"imagem":"barreira_de_gelo.png"},{"uuid":"30000000-0000-0000-0000-000000000009","cardName":"Mordida Glacial","cardType":"SPELL","color":"GREEN","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Agua\nSeu Herói Recebe +2 de Ataque e +1 de Espírito.\nAumente em 1 a Vulnerabilidade do Herói Inimigo até o Final da Partida.","racas":[],"ordem":[],"classe":[],"afinidades":["Agua"],"unica":false,"keywords":["VULNERABILIDADE"],"imagem":"mordida_glacial.png"},{"uuid":"30000000-0000-0000-0000-000000000010","cardName":"Contra Ataque","cardType":"SPELL","color":"BLUE","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Cristal - Cavaleiro\nDescarte uma Arma equipada no Herói Inimigo.\nSeu Herói Recebe +3 de Ataque até o Final do Combate.\nCavaleiro: Seu Herói Recebe +1 de Espírito e +1 de Defesa até o Final do Combate.","racas":[],"ordem":["Cavaleiro"],"classe":[],"afinidades":["Cristal"],"unica":false,"keywords":["FINAL_DO_COMBATE"],"imagem":"contra_ataque.png"},{"uuid":"30000000-0000-0000-0000-000000000011","cardName":"Ataque Mágico","cardType":"SPELL","color":"BLUE","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Cristal\nA Defesa do Herói Inimigo se torna 0 até o Final do Combate.","racas":[],"ordem":[],"classe":[],"afinidades":["Cristal"],"unica":false,"keywords":["FINAL_DO_COMBATE"],"imagem":"ataque_magico.png"},{"uuid":"30000000-0000-0000-0000-000000000012","cardName":"Golpes Pesados","cardType":"SPELL","color":"BLUE","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Cristal\nSeu Herói Recebe +X de Ataque até o Final do Combate, onde X é seu Espírito.","racas":[],"ordem":[],"classe":[],"afinidades":["Cristal"],"unica":false,"keywords":["FINAL_DO_COMBATE"],"imagem":"golpes_pesados.png"},{"uuid":"30000000-0000-0000-0000-000000000013","cardName":"Massacre Cristalino","cardType":"SPELL","color":"BLUE","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Cristal - Cristal\nO Herói Inimigo Recebe Espírito = 2.\nAtive Imediatamente todos seus Efeitos de Início de Combate e Final do Combate.","racas":[],"ordem":["Cristal"],"classe":[],"afinidades":["Cristal"],"unica":false,"keywords":["INICIO_DO_COMBATE","FINAL_DO_COMBATE"],"imagem":"massacre_cristalino.png"},{"uuid":"30000000-0000-0000-0000-000000000014","cardName":"Ponto Final","cardType":"SPELL","color":"BLUE","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Cristal\nSeu Herói Recebe +X de Ataque, onde X é seu Espírito vezes 2.","racas":[],"ordem":[],"classe":[],"afinidades":["Cristal"],"unica":false,"keywords":[],"imagem":"ponto_final.png"},{"uuid":"30000000-0000-0000-0000-000000000015","cardName":"Auto Defesa Magica","cardType":"SPELL","color":"BLUE","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Cristal\nSeu Herói Recebe +X de Ataque até o Final do Combate, onde X é seu Espírito.\nO Herói Inimigo Recebe -X de Defesa, onde X é o Espírito do Herói Inimigo.","racas":[],"ordem":[],"classe":[],"afinidades":["Cristal"],"unica":false,"keywords":["FINAL_DO_COMBATE"],"imagem":"auto_defesa_magica.png"},{"uuid":"30000000-0000-0000-0000-000000000016","cardName":"Risada Sarcástica","cardType":"THEURGY","color":"NEUTRAL","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"O Herói Inimigo Sofre X + 2 de Dano Mágico, onde X é seu Espírito.\nFinal do Combate: Adicione uma Tristeza no baralho do Herói Inimigo.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["DANO_MAGICO","FINAL_DO_COMBATE"],"imagem":"risada_sarcastica.png"},{"uuid":"30000000-0000-0000-0000-000000000017","cardName":"Alma em Chamas","cardType":"THEURGY","color":"NEUTRAL","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Fogo\nEnquanto estiver Equipada: Seu Herói Recebe +2 de Espírito e +3 de Ataque.\nFinal do Combate: Recupere 2 de Vida.","racas":[],"ordem":[],"classe":[],"afinidades":["Fogo"],"unica":false,"keywords":["AO_EQUIPAR","FINAL_DO_COMBATE"],"imagem":"alma_em_chamas.png"},{"uuid":"30000000-0000-0000-0000-000000000018","cardName":"Fortaleça sua Fé","cardType":"THEURGY","color":"YELLOW","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Guardião do Trono\nSeu Herói Recebe +2 de Ataque até o Final do Combate.\nSeus Transformadores Recebem +1 de Ataque até o Final da Partida.","racas":[],"ordem":["Guardião do Trono"],"classe":[],"afinidades":[],"unica":false,"keywords":["FINAL_DO_COMBATE"],"imagem":"fortaleca_sua_fe.png"},{"uuid":"30000000-0000-0000-0000-000000000019","cardName":"Saque Rápido","cardType":"THEURGY","color":"YELLOW","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Guardião do Trono\nJogue uma Arma de sua mão, se o fizer Seu Herói Recebe +3 de Ataque até o Final do Combate.","racas":[],"ordem":["Guardião do Trono"],"classe":[],"afinidades":[],"unica":false,"keywords":["FINAL_DO_COMBATE"],"imagem":"saque_rapido.png"},{"uuid":"30000000-0000-0000-0000-000000000020","cardName":"Disciplina","cardType":"ACTION","color":"YELLOW","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Guardião do Trono\nDesequipe até duas cartas do Herói Inimigo.\nSeu Herói Sofre 2 de Dano Direto por Carta Desequipada.","racas":[],"ordem":["Guardião do Trono"],"classe":[],"afinidades":[],"unica":false,"keywords":["DANO_DIRETO"],"imagem":"disciplina.png"},{"uuid":"40000000-0000-0000-0000-000000000001","cardName":"Ração de emergencia","cardType":"ACTION","color":"NEUTRAL","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Compre duas cartas.\nJogue uma Carta de sua mão.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":[],"imagem":"racao_de_emergencia.png"},{"uuid":"40000000-0000-0000-0000-000000000002","cardName":"Determinação Cristalina","cardType":"ACTION","color":"BLUE","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Cristal - Cavaleiro\nSeu Herói Recebe +1 de Ataque até o Final do Combate.\nJogue uma Carta do seu descarte.","racas":[],"ordem":["Cristal","Cavaleiro"],"classe":[],"afinidades":[],"unica":false,"keywords":["FINAL_DO_COMBATE"],"imagem":"determinacao_cristalina.png"},{"uuid":"40000000-0000-0000-0000-000000000003","cardName":"Convocação","cardType":"ACTION","color":"NEUTRAL","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Escolha um Herói: Altere seu Estado Ativo.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["ATIVO"],"imagem":"convocacao.png"},{"uuid":"40000000-0000-0000-0000-000000000004","cardName":"Mirar na cabeça","cardType":"ACTION","color":"NEUTRAL","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Escolha um Herói Inimigo: Ele Sofre 3 de Dano Direto.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["DANO_DIRETO"],"imagem":"mirar_na_cabeca.png"},{"uuid":"40000000-0000-0000-0000-000000000005","cardName":"Ritos Fúnebres","cardType":"ACTION","color":"BLACK","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Zumbi\nEscolha um Herói: Recupere 5 de Vida.\nSe estiver Morto e sua vida ficar acima de 0: O reviva.","racas":[],"ordem":["Zumbi"],"classe":[],"afinidades":[],"unica":false,"keywords":["MORTO"],"imagem":"ritos_funebres.png"},{"uuid":"40000000-0000-0000-0000-000000000006","cardName":"Exumação","cardType":"ACTION","color":"BLACK","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Zumbi\nSeu Herói Recebe -1 de Espírito -1 de Ataque -1 de Defesa até o Final da Partida.\nJogue uma Carta do seu descarte.","racas":[],"ordem":["Zumbi"],"classe":[],"afinidades":[],"unica":false,"keywords":[],"imagem":"exumacao.png"},{"uuid":"40000000-0000-0000-0000-000000000007","cardName":"Banir!","cardType":"ACTION","color":"NEUTRAL","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Prioridade.\nDescarte 1 carta Inimiga em jogo ou na fila de resolução.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["PRIORIDADE"],"imagem":"banir.png"},{"uuid":"40000000-0000-0000-0000-000000000008","cardName":"Impulso de Poder","cardType":"ACTION","color":"BLUE","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Cristal\nAumente seu Dano Bônus em 2 até o Final do Combate.","racas":[],"ordem":["Cristal"],"classe":[],"afinidades":[],"unica":false,"keywords":["DANO_BONUS","FINAL_DO_COMBATE"],"imagem":"impulso_de_poder.png"},{"uuid":"40000000-0000-0000-0000-000000000009","cardName":"Tristeza","cardType":"ACTION","color":"NEUTRAL","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Quem bate esquece, quem apanha não.\nExilar.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["EXILAR"],"imagem":"tristeza.png"},{"uuid":"40000000-0000-0000-0000-000000000010","cardName":"Luz da Lua","cardType":"ACTION","color":"GREEN","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Seu Herói Recebe +2 de Ataque até o Final do Combate.\nCure Seu Herói em X, onde X é seu Ataque.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["FINAL_DO_COMBATE"],"imagem":"luz_da_lua.png"},{"uuid":"40000000-0000-0000-0000-000000000011","cardName":"Ordem Divina","cardType":"THEURGY","color":"YELLOW","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Guardião do Trono\nEscolha um Herói, ele Recebe +2 de Ataque até o Final do Combate e cura 2 pontos.\nSe jogado por um Guardião do Trono: Seu Herói Recebe +2 de Ataque até o Final do Combate e cura 2 pontos.","racas":[],"ordem":["Guardião do Trono"],"classe":[],"afinidades":[],"unica":false,"keywords":["FINAL_DO_COMBATE"],"imagem":"ordem_divina.png"},{"uuid":"40000000-0000-0000-0000-000000000012","cardName":"Sica","cardType":"ITEM","color":"RED","rarity":"COMUM","cardSet":"BASE","itemType":"ARMA","empunhadura":1,"descricao":"Arma - Empunhadura Simples\nSeu Herói Recebe +2 de Ataque e +2 de Espírito.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["EMPUNHADURA"],"imagem":"sica.png"},{"uuid":"40000000-0000-0000-0000-000000000013","cardName":"Sino do Caçador","cardType":"ITEM","color":"RED","rarity":"COMUM","cardSet":"BASE","itemType":"FERRAMENTA","empunhadura":0,"descricao":"Ferramenta\nSeu Herói Recebe +1 de Espírito e +1 de Ataque.\nJogue uma Ferramenta de sua mão.\nJogue a próxima Arma de Empunhadura Simples de seu baralho.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["EMPUNHADURA"],"imagem":"sino_do_cacador.png"},{"uuid":"40000000-0000-0000-0000-000000000014","cardName":"Arpão terrestre","cardType":"ITEM","color":"RED","rarity":"COMUM","cardSet":"BASE","itemType":"ARMA","empunhadura":1,"descricao":"Arma - Empunhadura Simples\nSeu Herói Recebe +3 de Ataque e +2 de Defesa.\nAdicione a próxima Ferramenta de seu baralho a sua mão.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["EMPUNHADURA"],"imagem":"arpao_terrestre.png"},{"uuid":"40000000-0000-0000-0000-000000000015","cardName":"Bomba de fumaça","cardType":"ITEM","color":"RED","rarity":"COMUM","cardSet":"BASE","itemType":"FERRAMENTA","empunhadura":0,"descricao":"Ferramenta\nSeu Herói Recebe +2 de Ataque.\nO Herói Inimigo Recebe -2 de Defesa.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":[],"imagem":"bomba_de_fumaca.png"},{"uuid":"40000000-0000-0000-0000-000000000016","cardName":"Tesouro Cristalino dos Caçadores","cardType":"ITEM","color":"RED","rarity":"COMUM","cardSet":"BASE","itemType":"FERRAMENTA","empunhadura":0,"descricao":"Jóia\nSeu Herói Recebe +X de Ataque, onde X é seu Espírito.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":[],"imagem":"tesouro_cristalino_dos_cacadores.png"},{"uuid":"40000000-0000-0000-0000-000000000017","cardName":"Afie suas Lâminas","cardType":"ACTION","color":"RED","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Seu Herói Recebe +4 de Ataque e +4 de Defesa.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":[],"imagem":"afie_suas_laminas.png"},{"uuid":"40000000-0000-0000-0000-000000000018","cardName":"Estilo Secreto do Caçador","cardType":"ACTION","color":"RED","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Seu Herói Recebe Empunhadura Tripla até o Final da Partida.\nJogue uma Arma de Empunhadura Simples de sua mão.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["EMPUNHADURA"],"imagem":"estilo_secreto_do_cacador.png"},{"uuid":"40000000-0000-0000-0000-000000000019","cardName":"Corte seus Pescoços","cardType":"ACTION","color":"RED","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Para cada Arma equipada no Seu Herói, realize um Ataque Físico aleatoriamente contra um Herói Inimigo.\nDescarte todas as Armas equipadas no Seu Herói.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["DANO_FISICO"],"imagem":"corte_seus_pescocos.png"},{"uuid":"40000000-0000-0000-0000-000000000020","cardName":"Peitoral de brasas","cardType":"ITEM","color":"RED","rarity":"COMUM","cardSet":"BASE","itemType":"ARMADURA","empunhadura":0,"descricao":"Armadura\nSeu Herói Recebe +2 de Espírito e +2 de Defesa.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":[],"imagem":"peitoral_de_brasas.png"},{"uuid":"40000000-0000-0000-0000-000000000021","cardName":"Escudo ardente","cardType":"ITEM","color":"RED","rarity":"COMUM","cardSet":"BASE","itemType":"ESCUDO","empunhadura":0,"descricao":"Escudo\nSeu Herói Recebe +2 de Espírito e +2 de Defesa.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":[],"imagem":"escudo_ardente.png"},{"uuid":"40000000-0000-0000-0000-000000000022","cardName":"Muralha de Fogo","cardType":"SPELL","color":"RED","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Fogo\nO Herói Inimigo Sofre X de Dano Mágico, onde X é seu Espírito somado a sua Defesa +2 .","racas":[],"ordem":[],"classe":[],"afinidades":["Fogo"],"unica":false,"keywords":["DANO_MAGICO"],"imagem":"muralha_de_fogo.png"},{"uuid":"40000000-0000-0000-0000-000000000023","cardName":"Cauterizar","cardType":"SPELL","color":"RED","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Fogo\nCure X de Vida, onde X é seu Espírito +2.","racas":[],"ordem":[],"classe":[],"afinidades":["Fogo"],"unica":false,"keywords":[],"imagem":"cauterizar.png"},{"uuid":"40000000-0000-0000-0000-000000000024","cardName":"Estrela do amanhã","cardType":"ITEM","color":"NEUTRAL","rarity":"COMUM","cardSet":"BASE","itemType":"JOIA","empunhadura":0,"descricao":"Jóia\nFinal do Combate: Se a Vida do Seu Herói for menor que 0, a Vida do Seu Herói se torna 13.\nExilar.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["FINAL_DO_COMBATE","EXILAR"],"imagem":"estrela_do_amanha.png"},{"uuid":"40000000-0000-0000-0000-000000000025","cardName":"Espada do Carrasco","cardType":"ITEM","color":"BLACK","rarity":"COMUM","cardSet":"BASE","itemType":"ARMA","empunhadura":2,"descricao":"Arma - Empunhadura Dupla\nSeu Herói Recebe +3 de Ataque.\nFinal do Combate: Se o Herói Inimigo tiver 0 ou menos de Vida, Seu Herói recupera 5 pontos de Vida.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["EMPUNHADURA","FINAL_DO_COMBATE"],"imagem":"espada_do_carrasco.png"},{"uuid":"40000000-0000-0000-0000-000000000026","cardName":"Seguindo Ordens","cardType":"ACTION","color":"BLACK","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Escolha um Herói Inimigo fora de combate: Ele Sofre 1 de Dano Direto.\nEscolha um Herói Inimigo: Altere seu Estado Ativo.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["DANO_DIRETO","ATIVO"],"imagem":"seguindo_ordens.png"},{"uuid":"40000000-0000-0000-0000-000000000027","cardName":"Grilhões Pesados","cardType":"ACTION","color":"BLACK","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Desequipe um Item do Herói Inimigo. Se o Herói Inimigo estiver Ferido: Desequipe até dois Itens em vez disso.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["FERIDO"],"imagem":"grilhoes_pesados.png"},{"uuid":"40000000-0000-0000-0000-000000000028","cardName":"Manoplas do Necromante","cardType":"ITEM","color":"BLACK","rarity":"COMUM","cardSet":"BASE","itemType":"ARMADURA","empunhadura":0,"descricao":"Armadura\nInício do Combate: Seu Herói recebe +X de Espírito, +X de Ataque e +X de Defesa até o Final do Combate, onde X é a diferença entre a Vida Máxima e a Vida Atual do Herói Inimigo (máximo de 3).","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["INICIO_DO_COMBATE","FINAL_DO_COMBATE"],"imagem":"manoplas_do_necromante.png"},{"uuid":"40000000-0000-0000-0000-000000000029","cardName":"Cheiro de Sangue","cardType":"ACTION","color":"BLACK","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"O Herói Inimigo Sofre 3 de Dano Físico.\nSe o Herói Inimigo estiver Ferido: Ele Sofre 2 de Dano Direto.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["DANO_FISICO","FERIDO","DANO_DIRETO"],"imagem":"cheiro_de_sangue.png"},{"uuid":"40000000-0000-0000-0000-000000000030","cardName":"Arrastar para o Cadafalso","cardType":"ACTION","color":"BLACK","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Escolha um Herói Inimigo Ferido fora de combate: Ele Sofre 2 de Dano Direto.\nO Herói Inimigo Recebe +1 de Vulnerabilidade.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["FERIDO","DANO_DIRETO","VULNERABILIDADE"],"imagem":"arrastar_para_o_cadafalso.png"},{"uuid":"40000000-0000-0000-0000-000000000031","cardName":"Colheita dos Condenados","cardType":"ACTION","color":"BLACK","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Para cada Herói Inimigo Ferido: Seu Herói recupera 2 de Vida e o Herói Inimigo Sofre 1 de Dano Direto.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["FERIDO","DANO_DIRETO"],"imagem":"colheita_dos_condenados.png"},{"uuid":"40000000-0000-0000-0000-000000000032","cardName":"Sentença da Guilhotina","cardType":"ACTION","color":"BLACK","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Se o Herói Inimigo estiver Ferido: O Herói Inimigo Recebe +2 de Vulnerabilidade até o Final do Combate.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["FERIDO","VULNERABILIDADE","FINAL_DO_COMBATE"],"imagem":"sentenca_da_guilhotina.png"},{"uuid":"40000000-0000-0000-0000-000000000033","cardName":"Cutelo do Suplício","cardType":"ITEM","color":"BLACK","rarity":"COMUM","cardSet":"BASE","itemType":"ARMA","empunhadura":1,"descricao":"Arma - Empunhadura Simples\nSeu Herói Recebe +2 de Ataque.\nInício do Combate: Se o Herói Inimigo estiver Ferido: Seu Herói Recebe +2 de Ataque até o Final do Combate.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["EMPUNHADURA","INICIO_DO_COMBATE","FERIDO","FINAL_DO_COMBATE"],"imagem":"cutelo_do_suplicio.png"},{"uuid":"40000000-0000-0000-0000-000000000034","cardName":"Édito contra a Ganância","cardType":"THEURGY","color":"YELLOW","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Guardião do Trono\nO Herói Inimigo sofre X + 2 de Dano Mágico, onde X é o Espírito de Seu Herói.\nSe o oponente comprou cartas fora da Fase de Escolha neste turno, o Herói Inimigo sofre 3 de Dano Direto e recebe -2 de Ataque até o Final do Combate.","racas":[],"ordem":["Guardião do Trono"],"classe":[],"afinidades":[],"unica":false,"keywords":["DANO_MAGICO","FASE","DANO_DIRETO","FINAL_DO_COMBATE"],"imagem":"edito_contra_a_ganancia.png"},{"uuid":"40000000-0000-0000-0000-000000000035","cardName":"Retribuição Dogmática","cardType":"THEURGY","color":"YELLOW","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Guardião do Trono\nSeu Herói recebe +2 de Defesa até o Final do Combate.\nO Herói Inimigo sofre 2 de Dano Mágico para cada carta jogada ou invocada pelo oponente neste combate.","racas":[],"ordem":["Guardião do Trono"],"classe":[],"afinidades":[],"unica":false,"keywords":["FINAL_DO_COMBATE","DANO_MAGICO","INVOCAR"],"imagem":"retribuicao_dogmatica.png"},{"uuid":"40000000-0000-0000-0000-000000000036","cardName":"Interdito Sagrado","cardType":"THEURGY","color":"YELLOW","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Guardião do Trono\nO Dano Bônus do Herói Inimigo torna-se 0 até o Final do Combate.\nSe o Herói Inimigo possuir uma Arma equipada, o Herói Inimigo recebe -2 de Ataque até o Final do Combate.","racas":[],"ordem":["Guardião do Trono"],"classe":[],"afinidades":[],"unica":false,"keywords":["DANO_BONUS","FINAL_DO_COMBATE"],"imagem":"interdito_sagrado.png"},{"uuid":"40000000-0000-0000-0000-000000000037","cardName":"Selo da Confissão","cardType":"ITEM","color":"YELLOW","rarity":"COMUM","cardSet":"BASE","itemType":"JOIA","empunhadura":0,"descricao":"Jóia - Guardião do Trono\nFinal do Combate: Recupere 3 de Vida.\nSe o Herói Inimigo causou Dano Físico ou Dano Mágico neste combate, o Herói Inimigo recebe +1 de Vulnerabilidade até o Final da Partida.","racas":[],"ordem":["Guardião do Trono"],"classe":[],"afinidades":[],"unica":false,"keywords":["FINAL_DO_COMBATE","DANO_FISICO","DANO_MAGICO","VULNERABILIDADE"],"imagem":"selo_da_confissao.png"},{"uuid":"40000000-0000-0000-0000-000000000038","cardName":"Cetro do Julgamento Perpétuo","cardType":"ITEM","color":"YELLOW","rarity":"COMUM","cardSet":"BASE","itemType":"ARMA","empunhadura":2,"descricao":"Arma - Empunhadura Dupla - Guardião do Trono\nSeu Herói recebe +2 de Ataque e +1 de Espírito.\nInício do Combate: Se o Herói Inimigo possuir mais Vida Máxima que Seu Herói, Seu Herói recebe +2 de Ataque e +2 de Defesa até o Final do Combate.","racas":[],"ordem":["Guardião do Trono"],"classe":[],"afinidades":[],"unica":false,"keywords":["EMPUNHADURA","INICIO_DO_COMBATE","FINAL_DO_COMBATE"],"imagem":"cetro_do_julgamento_perpetuo.png"},{"uuid":"40000000-0000-0000-0000-000000000039","cardName":"Bolsa de Armadilhas do Mestre Caçador","cardType":"ITEM","color":"RED","rarity":"LENDARIA","cardSet":"BASE","itemType":"FERRAMENTA","empunhadura":0,"descricao":"Ferramenta\nSeu Herói recebe +2 de Ataque e +2 de Defesa.\nAo jogar e no Início do Combate: Jogue a próxima Ferramenta do seu baralho.\nFinal do Combate: Anexe esta carta a outro Herói Aliado Ativo.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"imagem":"bolsa_de_armadilhas_do_mestre_cacador.png"},{"uuid":"40000000-0000-0000-0000-000000000040","cardName":"Rede de Contenção de Feras","cardType":"ITEM","color":"RED","rarity":"COMUM","cardSet":"BASE","itemType":"FERRAMENTA","empunhadura":0,"descricao":"Ferramenta\nInício do Combate: O Herói Inimigo recebe -2 de Ataque até o Final do Combate.\nAo jogar Ferramenta: Seu Herói recebe +1 de Ataque e +1 de Defesa até o Final do Combate.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"imagem":"rede_de_contencao_de_feras.png"},{"uuid":"40000000-0000-0000-0000-000000000041","cardName":"Escamas do Dragão Eterno","cardType":"ITEM","color":"RED","rarity":"LENDARIA","cardSet":"BASE","itemType":"ARMADURA","empunhadura":0,"descricao":"Armadura\nSeu Herói recebe +3 de Defesa e +2 de Espírito.\nInício do Combate: Invocar: Bola de Fogo.\nFinal do Combate: Se o Herói Inimigo sofreu Dano Mágico neste combate, Seu Herói recupera 3 de Vida.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"imagem":"escamas_do_dragao_eterno.png"},{"uuid":"40000000-0000-0000-0000-000000000042","cardName":"Bastão do Monge Incandescente","cardType":"ITEM","color":"RED","rarity":"COMUM","cardSet":"BASE","itemType":"ARMA","empunhadura":1,"descricao":"Arma - Empunhadura Simples\nSeu Herói recebe +2 de Dano Bônus.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"imagem":"bastao_do_monge_incandescente.png"},{"uuid":"40000000-0000-0000-0000-000000000043","cardName":"Fogo Purificador","cardType":"SPELL","color":"RED","rarity":"INCOMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Fogo\nO Herói Inimigo Sofre 4 mais seu Espírito de Dano Mágico.\nSe o Herói Inimigo for um Constructo: O Herói Inimigo recebe -2 de Defesa até o Final do Combate.\nSe não, o Herói Inimigo recebe -2 de Ataque até o Final do Combate.","racas":[],"ordem":[],"classe":[],"afinidades":["Fogo"],"unica":false,"imagem":"fogo_purificador.png"},{"uuid":"40000000-0000-0000-0000-000000000044","cardName":"Meditação das Cinzas","cardType":"ACTION","color":"RED","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Seu Herói recebe +1 de Defesa e +1 de Espírito até o Final do Combate.\nInvocar: Bola de Fogo.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"imagem":"meditacao_das_cinzas.png"},{"uuid":"40000000-0000-0000-0000-000000000045","cardName":"Incensário Purificador","cardType":"ITEM","color":"RED","rarity":"INCOMUM","cardSet":"BASE","itemType":"JOIA","empunhadura":0,"descricao":"Jóia\nSeu Herói recebe +1 de Espírito e +1 de Defesa.\nAo jogar Feitiço: Seu Herói recupera 2 de Vida.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"imagem":"incensario_purificador.png"},{"uuid":"40000000-0000-0000-0000-000000000046","cardName":"Manto das Brasas Vivas","cardType":"SPELL","color":"RED","rarity":"RARA","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Fogo\nAura: Seus Heróis recebem +1 de Defesa e +1 de Espírito.","racas":[],"ordem":[],"classe":[],"afinidades":["Fogo"],"unica":false,"imagem":"manto_das_brasas_vivas.png"},{"uuid":"40000000-0000-0000-0000-000000000047","cardName":"Baluarte de Espelhos do Dragão","cardType":"ITEM","color":"BLUE","rarity":"INCOMUM","cardSet":"BASE","itemType":"ESCUDO","empunhadura":0,"descricao":"Escudo - Cavaleiro\nSeu Herói recebe +1 de Defesa.\nAo Jogar: Invocar: Escolha um feitiço Cristal do seu descarte.\nFinal do Combate: Retorne 1 Feitiço Cristal do seu descarte para seu baralho.","racas":[],"ordem":["Cavaleiro"],"classe":[],"afinidades":[],"unica":false,"imagem":"baluarte_de_espelhos_do_dragao.png"},{"uuid":"40000000-0000-0000-0000-000000000048","cardName":"Placas de Cristal Draconiano","cardType":"ITEM","color":"BLUE","rarity":"RARA","cardSet":"BASE","itemType":"ARMADURA","empunhadura":0,"descricao":"Armadura - Cavaleiro\nSeu Herói recebe +2 de Defesa.\nInício do Combate: Para cada Herói Aliado Morto, Seu Herói recebe +2 de Ataque e +1 de Espírito até o Final do Combate.","racas":[],"ordem":["Cavaleiro"],"classe":[],"afinidades":[],"unica":false,"imagem":"placas_de_cristal_draconiano.png"},{"uuid":"40000000-0000-0000-0000-000000000049","cardName":"Voto dos Cavaleiros Caídos","cardType":"SPELL","color":"BLUE","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Cristal - Cavaleiro\nSeu Herói recebe +2 de Ataque até o Final do Combate.\nSe houver algum Herói Aliado Morto: Seu Herói recebe +3 de Ataque adicional até o Final do Combate e recupere 3 de Vida.","racas":[],"ordem":["Cavaleiro"],"classe":[],"afinidades":["Cristal"],"unica":false,"imagem":"voto_dos_cavaleiros_caidos.png"},{"uuid":"40000000-0000-0000-0000-000000000050","cardName":"Coração de Cristal Puro","cardType":"ITEM","color":"BLUE","rarity":"LENDARIA","cardSet":"BASE","itemType":"JOIA","empunhadura":0,"descricao":"Jóia - Cristal\nAura: Seus Heróis recebem +2 de Dano Bônus e +2 de Defesa.","racas":[],"ordem":["Cristal"],"classe":[],"afinidades":[],"unica":false,"imagem":"coracao_de_cristal_puro.png"},{"uuid":"40000000-0000-0000-0000-000000000051","cardName":"Dança das Mil Lâminas","cardType":"SPELL","color":"BLUE","rarity":"RARA","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Cristal - Cristal\nO Espírito do Herói Inimigo torna-se 1 até o Final do Combate.\nO Herói Inimigo Sofre 4 de Dano Mágico.\nFinal do Combate: O Herói Inimigo Sofre 3 de Dano Mágico.","racas":[],"ordem":["Cristal"],"classe":[],"afinidades":["Cristal"],"unica":false,"imagem":"danca_das_mil_laminas.png"},{"uuid":"40000000-0000-0000-0000-000000000052","cardName":"Filactéria das Areias Negras","cardType":"ITEM","color":"BLACK","rarity":"LENDARIA","cardSet":"BASE","itemType":"JOIA","empunhadura":0,"descricao":"Jóia - Zumbi\nSeu Herói recebe +2 de Espírito e +2 de Defesa.\nFinal do Combate: Para cada Aliado Zumbi, Seu Herói recupera 1 de Vida e o Herói Inimigo sofre 1 de Dano Direto.","racas":[],"ordem":["Zumbi"],"classe":[],"afinidades":[],"unica":false,"imagem":"filacteria_das_areias_negras.png"},{"uuid":"40000000-0000-0000-0000-000000000053","cardName":"Peste das Areias Negras","cardType":"SPELL","color":"BLACK","rarity":"INCOMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Ar - Zumbi\nO Herói Inimigo Sofre 2 mais seu Espírito de Dano Mágico.\nPara cada Aliado Zumbi Vivo: O Herói Inimigo sofre 2 de Dano Direto.","racas":[],"ordem":["Zumbi"],"classe":[],"afinidades":["Ar"],"unica":false,"imagem":"peste_das_areias_negras.png"},{"uuid":"40000000-0000-0000-0000-000000000054","cardName":"Amuleto de Ossos da Fábrica","cardType":"ITEM","color":"BLACK","rarity":"COMUM","cardSet":"BASE","itemType":"JOIA","empunhadura":0,"descricao":"Jóia - Zumbi\nSeu Herói recebe +1 de Espírito e +1 de Ataque.\nInício do Combate: Para cada Aliado Zumbi Vivo, Seu Herói recebe +1 de Ataque até o Final do Combate.","racas":[],"ordem":["Zumbi"],"classe":[],"afinidades":[],"unica":false,"imagem":"amuleto_de_ossos_da_fabrica.png"},{"uuid":"40000000-0000-0000-0000-000000000055","cardName":"Pilhagem de Cadáveres","cardType":"ACTION","color":"BLACK","rarity":"RARA","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Zumbi\nPara cada Herói Morto na partida: Seu Herói recebe +1 de Ataque e +1 de Defesa até o Final do Combate.\nJogue uma Carta do seu descarte.","racas":[],"ordem":["Zumbi"],"classe":[],"afinidades":[],"unica":false,"imagem":"pilhagem_de_cadaveres.png"},{"uuid":"40000000-0000-0000-0000-000000000056","cardName":"Manto de Ferro do Algoz","cardType":"ITEM","color":"BLACK","rarity":"RARA","cardSet":"BASE","itemType":"ARMADURA","empunhadura":0,"descricao":"Armadura\nSeu Herói recebe +1 de Defesa.\nInício do Combate: Se o Herói Inimigo estiver Ferido, Seu Herói recebe +2 de Defesa até o Final do Combate.\nFinal do Combate: Se o Herói Inimigo estiver com 0 ou menos de Vida, Seu Herói recupera 4 de Vida.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"imagem":"manto_de_ferro_do_algoz.png"},{"uuid":"40000000-0000-0000-0000-000000000057","cardName":"Correntes do Cadafalso","cardType":"ITEM","color":"BLACK","rarity":"COMUM","cardSet":"BASE","itemType":"FERRAMENTA","empunhadura":0,"descricao":"Ferramenta\nInício do Combate: Se o Herói Inimigo estiver Ferido, Seu Herói recebe +2 de Ataque até o Final do Combate.\nFinal do Combate: Se o Herói Inimigo estiver Ferido, o Herói Inimigo sofre 2 de Dano Direto.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"imagem":"correntes_do_cadafalso.png"},{"uuid":"40000000-0000-0000-0000-000000000058","cardName":"Decreto de Excomunhão","cardType":"THEURGY","color":"YELLOW","rarity":"INCOMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Guardião do Trono\nO Herói Inimigo sofre 3 mais seu Espírito de Dano Mágico.\nSe o Herói Inimigo possuir algum Item equipado: Descarte um Item do Herói Inimigo.","racas":[],"ordem":["Guardião do Trono"],"classe":[],"afinidades":[],"unica":false,"imagem":"decreto_de_excomunhao.png"},{"uuid":"40000000-0000-0000-0000-000000000059","cardName":"Liturgia da Balança","cardType":"THEURGY","color":"YELLOW","rarity":"RARA","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Guardião do Trono\nSeu Herói recupera 4 de Vida.\nSe o Herói Inimigo possuir mais Ataque que Seu Herói: O Ataque do Herói Inimigo torna-se igual ao Ataque do Seu Herói até o Final do Combate.","racas":[],"ordem":["Guardião do Trono"],"classe":[],"afinidades":[],"unica":false,"imagem":"liturgia_da_balanca.png"},{"uuid":"40000000-0000-0000-0000-000000000060","cardName":"Tomo das Sentenças Antigas","cardType":"ITEM","color":"YELLOW","rarity":"INCOMUM","cardSet":"BASE","itemType":"JOIA","empunhadura":0,"descricao":"Jóia - Guardião do Trono\nSeu Herói recebe +1 de Espírito e +1 de Defesa.\nInício do Combate: Adicione uma Tristeza no baralho do Herói Inimigo.","racas":[],"ordem":["Guardião do Trono"],"classe":[],"afinidades":[],"unica":false,"imagem":"tomo_das_sentencas_antigas.png"},{"uuid":"40000000-0000-0000-0000-000000000061","cardName":"Bênção do Rei Imóvel","cardType":"THEURGY","color":"YELLOW","rarity":"RARA","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Guardião do Trono\nSeu Herói recupera 3 de Vida.\nAura: Sempre que o oponente jogar uma carta na Fase de Resolução, o Herói Inimigo sofre 1 de Dano Direto.","racas":[],"ordem":["Guardião do Trono"],"classe":[],"afinidades":[],"unica":false,"imagem":"bencao_do_rei_imovel.png"},{"uuid":"40000000-0000-0000-0000-000000000062","cardName":"Veredito Inquisitorial","cardType":"ACTION","color":"YELLOW","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Guardião do Trono\nSe o Herói Inimigo possuir mais Vida Atual que Seu Herói: O Herói Inimigo sofre 3 de Dano Direto e recebe +1 de Vulnerabilidade até o Final do Combate.","racas":[],"ordem":["Guardião do Trono"],"classe":[],"afinidades":[],"unica":false,"imagem":"veredito_inquisitorial.png"},{"uuid":"40000000-0000-0000-0000-000000000063","cardName":"Alabarda Cerimonial do Trono","cardType":"ITEM","color":"YELLOW","rarity":"COMUM","cardSet":"BASE","itemType":"ARMA","empunhadura":1,"descricao":"Arma - Empunhadura Simples - Guardião do Trono\nSeu Herói recebe +1 de Ataque e +1 de Espírito.\nFinal do Combate: Seu Herói recupera 2 de Vida.","racas":[],"ordem":["Guardião do Trono"],"classe":[],"afinidades":[],"unica":false,"imagem":"alabarda_cerimonial_do_trono.png"},{"uuid":"40000000-0000-0000-0000-000000000064","cardName":"Glaive da Guarda Eterna","cardType":"ITEM","color":"YELLOW","rarity":"INCOMUM","cardSet":"BASE","itemType":"ARMA","empunhadura":1,"descricao":"Arma - Empunhadura Simples - Guardião do Trono\nSeu Herói recebe +2 de Ataque.\nAo equipar: Jogue uma Arma de Empunhadura Simples de sua mão.","racas":[],"ordem":["Guardião do Trono"],"classe":[],"afinidades":[],"unica":false,"imagem":"glaive_da_guarda_eterna.png"},{"uuid":"40000000-0000-0000-0000-000000000065","cardName":"Manto Cerimonial do Trono","cardType":"ITEM","color":"YELLOW","rarity":"RARA","cardSet":"BASE","itemType":"ARMADURA","empunhadura":0,"descricao":"Armadura - Guardião do Trono\nSeu Herói recebe +1 de Defesa.\nInício do Combate: Seu Herói recupera 3 de Vida.\nFinal do Combate: Adicione uma Teurgia do seu descarte para a sua mão.","racas":[],"ordem":["Guardião do Trono"],"classe":[],"afinidades":[],"unica":false,"imagem":"manto_cerimonial_do_trono.png"},{"uuid":"40000000-0000-0000-0000-000000000066","cardName":"Fervor Penitencial","cardType":"ACTION","color":"YELLOW","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Guardião do Trono\nSeu Herói sofre 3 de Dano Direto.\nSeu Herói recebe +5 de Ataque até o Final do Combate.","racas":[],"ordem":["Guardião do Trono"],"classe":[],"afinidades":[],"unica":false,"imagem":"fervor_penitencial.png"},{"uuid":"40000000-0000-0000-0000-000000000067","cardName":"Escudo da Devoção Cega","cardType":"ITEM","color":"YELLOW","rarity":"INCOMUM","cardSet":"BASE","itemType":"ESCUDO","empunhadura":0,"descricao":"Escudo - Guardião do Trono\nSeu Herói recebe +1 de Defesa.\nInício do Combate: Para cada Arma equipada no Seu Herói, Seu Herói recupera 2 de Vida.","racas":[],"ordem":["Guardião do Trono"],"classe":[],"afinidades":[],"unica":false,"imagem":"escudo_da_devocao_cega.png"},{"uuid":"40000000-0000-0000-0000-000000000068","cardName":"Baluarte da Tundra Ancestral","cardType":"ITEM","color":"GREEN","rarity":"LENDARIA","cardSet":"BASE","itemType":"ESCUDO","empunhadura":0,"descricao":"Escudo\nSeu Herói recebe +3 de Defesa e +1 de Redução de Dano.\nInício do Combate: O Herói Inimigo recebe +1 de Vulnerabilidade até o Final da Partida.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"imagem":"baluarte_da_tundra_ancestral.png"},{"uuid":"40000000-0000-0000-0000-000000000069","cardName":"Couraça da Tundra Profunda","cardType":"ITEM","color":"GREEN","rarity":"RARA","cardSet":"BASE","itemType":"ARMADURA","empunhadura":0,"descricao":"Armadura\nSeu Herói recebe +2 de Defesa.\nInício do Combate: Para cada 2 pontos de Vulnerabilidade do Herói Inimigo, Seu Herói recebe +1 de Defesa até o Final do Combate.\nFinal do Combate: Seu Herói recupera 2 de Vida.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"imagem":"couraca_da_tundra_profunda.png"},{"uuid":"40000000-0000-0000-0000-000000000070","cardName":"Uivo da Matilha Gélida","cardType":"SPELL","color":"GREEN","rarity":"INCOMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Agua\nO Herói Inimigo Sofre 1 mais seu Espírito de Dano Mágico.\nO Herói Inimigo recebe +1 de Vulnerabilidade até o Final da Partida.","racas":[],"ordem":[],"classe":[],"afinidades":["Agua"],"unica":false,"imagem":"uivo_da_matilha_gelida.png"},{"uuid":"40000000-0000-0000-0000-000000000071","cardName":"Chamado do Domador","cardType":"ACTION","color":"GREEN","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Seus Constructos recebem +2 de Ataque e +1 de Defesa até o Final do Combate.\nCure seus Aliados em 2.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"imagem":"chamado_do_domador.png"},{"uuid":"40000000-0000-0000-0000-000000000072","cardName":"Garras Congeladas","cardType":"ITEM","color":"GREEN","rarity":"COMUM","cardSet":"BASE","itemType":"ARMA","empunhadura":1,"descricao":"Arma - Empunhadura Simples\nSeu Herói recebe +2 de Ataque e +1 de Defesa.\nInício do Combate: Se o Herói Inimigo possuir Vulnerabilidade maior que 0, Seu Herói recebe +2 de Ataque até o Final do Combate.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"imagem":"garras_congeladas.png"},{"uuid":"40000000-0000-0000-0000-000000000073","cardName":"Núcleo de Invenções do Rei Goblin","cardType":"ITEM","color":"GREEN","rarity":"LENDARIA","cardSet":"BASE","itemType":"FERRAMENTA","empunhadura":0,"descricao":"Ferramenta - Goblin\nSeu Herói recebe +1 de Ataque e +1 de Espírito.\nAura: Ao jogar qualquer carta: Seus Heróis recebem +1 de Ataque e +1 de Defesa até o Final do Combate.\nFinal do Combate: Seu Herói recupera 2 de Vida.","racas":["Goblin"],"ordem":[],"classe":[],"afinidades":[],"unica":false,"imagem":"nucleo_de_invencoes_do_rei_goblin.png"},{"uuid":"40000000-0000-0000-0000-000000000074","cardName":"Bateria de Cristal de Poeira","cardType":"ITEM","color":"GREEN","rarity":"COMUM","cardSet":"BASE","itemType":"FERRAMENTA","empunhadura":0,"descricao":"Ferramenta - Goblin\nSeu Herói recebe +1 de Espírito e +1 de Defesa.\nAo jogar Feitiço: Jogue uma Ação de sua mão.","racas":["Goblin"],"ordem":[],"classe":[],"afinidades":[],"unica":false,"imagem":"bateria_de_cristal_de_poeira.png"},{"uuid":"40000000-0000-0000-0000-000000000075","cardName":"Chave Mecânica de Precisão","cardType":"ITEM","color":"GREEN","rarity":"COMUM","cardSet":"BASE","itemType":"ARMA","empunhadura":1,"descricao":"Arma - Empunhadura Simples - Goblin\nSeu Herói recebe +2 de Ataque e +1 de Defesa.\nFinal do Combate: Se você jogou uma Ação neste combate, Seu Herói recupera 2 de Vida.","racas":["Goblin"],"ordem":[],"classe":[],"afinidades":[],"unica":false,"imagem":"chave_mecanica_de_precisao.png"},{"uuid":"40000000-0000-0000-0000-000000000076","cardName":"Engrenagem de Ressonância","cardType":"ITEM","color":"GREEN","rarity":"INCOMUM","cardSet":"BASE","itemType":"JOIA","empunhadura":0,"descricao":"Jóia - Goblin\nSeu Herói recebe +1 de Espírito e +1 de Ataque.\nAo jogar Item: Seu Herói recebe +1 de Ataque até o Final da Partida.","racas":["Goblin"],"ordem":[],"classe":[],"afinidades":[],"unica":false,"imagem":"engrenagem_de_ressonancia.png"},{"uuid":"40000000-0000-0000-0000-000000000077","cardName":"Descarga de Poeira Elétrica","cardType":"SPELL","color":"GREEN","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Ar - Goblin\nO Herói Inimigo Sofre 4 mais seu Espírito de Dano Mágico.\nSe jogado por um Goblin: Jogue um Item de sua mão.","racas":["Goblin"],"ordem":[],"classe":[],"afinidades":["Ar"],"unica":false,"imagem":"descarga_de_poeira_eletrica.png"},{"uuid":"40000000-0000-0000-0000-000000000078","cardName":"Circuito de Sobrecarga","cardType":"SPELL","color":"GREEN","rarity":"INCOMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Ar - Goblin\nO Herói Inimigo Sofre 4 mais seu Espírito de Dano Mágico.\nFinal do Combate: Se você jogou pelo menos 3 cartas neste combate, o Herói Inimigo sofre 2 de Dano Direto.","racas":["Goblin"],"ordem":[],"classe":[],"afinidades":["Ar"],"unica":false,"imagem":"circuito_de_sobrecarga.png"},{"uuid":"40000000-0000-0000-0000-000000000079","cardName":"Campo de Força de Cristal","cardType":"SPELL","color":"GREEN","rarity":"RARA","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Cristal - Goblin\nEnquanto estiver Equipada: Seus Heróis recebem +2 de Defesa e +1 de Espírito.\nInício do Combate: Seus Heróis recebem +1 de Ataque até o Final do Combate.","racas":["Goblin"],"ordem":[],"classe":[],"afinidades":["Cristal"],"unica":false,"imagem":"campo_de_forca_de_cristal.png"},{"uuid":"40000000-0000-0000-0000-000000000080","cardName":"Engenharia Rápida","cardType":"ACTION","color":"GREEN","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Goblin\nSeu Herói recebe +2 de Defesa até o Final do Combate.\nJogue um Item de sua mão.","racas":["Goblin"],"ordem":[],"classe":[],"afinidades":[],"unica":false,"imagem":"engenharia_rapida.png"},{"uuid":"40000000-0000-0000-0000-000000000081","cardName":"Recalibração de Energia","cardType":"ACTION","color":"GREEN","rarity":"INCOMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Goblin\nSeu Herói recebe +1 de Ataque e +1 de Espírito até o Final do Combate.\nJogue um Feitiço de sua mão.","racas":["Goblin"],"ordem":[],"classe":[],"afinidades":[],"unica":false,"imagem":"recalibracao_de_energia.png"},{"uuid":"40000000-0000-0000-0000-000000000082","cardName":"Conferência dos Sábios Goblins","cardType":"ACTION","color":"GREEN","rarity":"RARA","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Goblin\nSeus Heróis recebem +1 de Ataque, +1 de Defesa e +1 de Espírito até o Final do Combate.\nSe o Rei Goblin estiver Ativo: Cure seus Aliados em 3.","racas":["Goblin"],"ordem":[],"classe":[],"afinidades":[],"unica":false,"imagem":"conferencia_dos_sabios_goblins.png"},{"uuid":"40000000-0000-0000-0000-000000000083","cardName":"Martelo & Bigorna","cardType":"ITEM","color":"RED","rarity":"COMUM","cardSet":"BASE","itemType":"ARMA","empunhadura":2,"descricao":"Arma - Empunhadura Dupla\nSeu Herói recebe +4 de Defesa e +2 de Dano Bônus.\nAo Equipar:\nInício do Combate: Desequipe um Item anexado a um Herói Aliado ou ao Herói Inimigo.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["EMPUNHADURA","AO_EQUIPAR","INICIO_DO_COMBATE"],"imagem":"martelo_bigorna.png"},{"uuid":"40000000-0000-0000-0000-000000000084","cardName":"Escudo Improvisado","cardType":"ITEM","color":"RED","rarity":"COMUM","cardSet":"BASE","itemType":"ESCUDO","empunhadura":0,"descricao":"Escudo\nSeu Herói recebe +2 de Defesa.\nAo Equipar: Seu Herói recebe +2 de Defesa até o Final do Combate.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["AO_EQUIPAR","FINAL_DO_COMBATE"],"imagem":"escudo_improvisado.png"},{"uuid":"40000000-0000-0000-0000-000000000085","cardName":"Broquel Explosivo","cardType":"ITEM","color":"RED","rarity":"COMUM","cardSet":"BASE","itemType":"ESCUDO","empunhadura":0,"descricao":"Escudo\nSeu Herói recebe +1 de Defesa e +1 de Espírito.\nAo Equipar: Impacto.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["AO_EQUIPAR"],"imagem":"broquel_explosivo.png"},{"uuid":"40000000-0000-0000-0000-000000000086","cardName":"Armadura Antichamas","cardType":"ITEM","color":"RED","rarity":"COMUM","cardSet":"BASE","itemType":"ARMADURA","empunhadura":0,"descricao":"Armadura\nSeu Herói recebe +2 de Defesa e +2 de Espírito.\nAo Equipar: Escolha um Escudo anexado a um Herói Aliado e jogue-o.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":["AO_EQUIPAR"],"imagem":"armadura_antichamas.png"},{"uuid":"40000000-0000-0000-0000-000000000087","cardName":"Polir o Armamento","cardType":"ACTION","color":"RED","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Criador\nSeu Herói recebe +3 de Defesa até o Final do Combate.\nSeu Herói recupera 3 de Vida.","racas":[],"ordem":[],"classe":["Criador"],"afinidades":[],"unica":false,"keywords":["FINAL_DO_COMBATE"],"imagem":"polir_o_armamento.png"},{"uuid":"40000000-0000-0000-0000-000000000088","cardName":"Não Apresse um Artista","cardType":"ACTION","color":"RED","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Criador\nSeu Herói recebe -3 de Defesa até o Final do Combate.\nInício do Próximo Combate: Seu Herói recebe +4 de Defesa e cause Impacto em todos os Heróis Inimigos.","racas":[],"ordem":[],"classe":["Criador"],"afinidades":[],"unica":false,"keywords":["FINAL_DO_COMBATE","INICIO_DO_COMBATE"],"imagem":"nao_apresse_um_artista.png"},{"uuid":"40000000-0000-0000-0000-000000000089","cardName":"Coração da Forja","cardType":"ITEM","color":"RED","rarity":"COMUM","cardSet":"BASE","itemType":"JOIA","empunhadura":0,"descricao":"Jóia\nSempre que um efeito de Impacto for ativado: Seu Herói recupera 2 de Vida.","racas":[],"ordem":[],"classe":[],"afinidades":[],"unica":false,"keywords":[],"imagem":"coracao_da_forja.png"},{"uuid":"40000000-0000-0000-0000-000000000090","cardName":"Resíduo Incendiário","cardType":"SPELL","color":"BLUE","rarity":"COMUM","cardSet":"BASE","itemType":"NENHUM","empunhadura":0,"descricao":"Afinidade Cristal\nO Herói Inimigo recebe 5 Marcadores de Combustão.","racas":[],"ordem":[],"classe":[],"afinidades":["Cristal"],"unica":false,"keywords":["COMBUSTAO"],"imagem":"residuo_incendiario.png"}].map(c => [toSnakeCaseKey(c.cardName), c])
);

let cachedServerHeroesMap = null;
let cachedServerCardsMap = null;

export async function ensureCatalogLoaded() {
    if (cachedServerHeroesMap && cachedServerCardsMap) {
        return { heroesMap: cachedServerHeroesMap, cardsMap: cachedServerCardsMap };
    }
    try {
        const res = await fetch(`${API_BASE_URL}/api/players/catalog`);
        if (res.ok) {
            const data = await res.json();
            cachedServerHeroesMap = new Map((data.heroes || []).map(h => [toSnakeCaseKey(h.cardName), h]));
            cachedServerCardsMap = new Map((data.cards || []).map(c => [toSnakeCaseKey(c.cardName), c]));
            return { heroesMap: cachedServerHeroesMap, cardsMap: cachedServerCardsMap };
        }
    } catch (e) {
        // Fallback offline
    }
    return { heroesMap: FALLBACK_HEROES_BY_NAME, cardsMap: FALLBACK_CARDS_BY_NAME };
}

export function resolveFullCardOrHeroData(cardNameOrDto, isHeroHint = null) {
    if (!cardNameOrDto) return null;
    const rawName = typeof cardNameOrDto === 'string'
        ? cardNameOrDto
        : (cardNameOrDto.cardName || cardNameOrDto.name || cardNameOrDto.nome || '');
    const snake = toSnakeCaseKey(rawName);

    const hMap = cachedServerHeroesMap || FALLBACK_HEROES_BY_NAME;
    const cMap = cachedServerCardsMap || FALLBACK_CARDS_BY_NAME;

    let baseData = null;
    if (isHeroHint === true) {
        baseData = hMap.get(snake) || FALLBACK_HEROES_BY_NAME.get(snake) || cMap.get(snake) || FALLBACK_CARDS_BY_NAME.get(snake);
    } else if (isHeroHint === false) {
        baseData = cMap.get(snake) || FALLBACK_CARDS_BY_NAME.get(snake) || hMap.get(snake) || FALLBACK_HEROES_BY_NAME.get(snake);
    } else {
        baseData = hMap.get(snake) || FALLBACK_HEROES_BY_NAME.get(snake) || cMap.get(snake) || FALLBACK_CARDS_BY_NAME.get(snake);
    }

    if (typeof cardNameOrDto === 'object') {
        const merged = { ...(baseData || {}), ...cardNameOrDto };
        if (baseData) {
            if (!merged.descricao && baseData.descricao) merged.descricao = baseData.descricao;
            if (!merged.itemType || merged.itemType === 'NENHUM') merged.itemType = baseData.itemType || 'NENHUM';
            if (!merged.empunhadura && baseData.empunhadura) merged.empunhadura = baseData.empunhadura;
            if ((!merged.racas || merged.racas.length === 0) && baseData.racas) merged.racas = baseData.racas;
            if ((!merged.ordem || merged.ordem.length === 0) && baseData.ordem) merged.ordem = baseData.ordem;
            if ((!merged.classe || merged.classe.length === 0) && baseData.classe) merged.classe = baseData.classe;
            if ((!merged.afinidades || merged.afinidades.length === 0) && baseData.afinidades) merged.afinidades = baseData.afinidades;
            if (merged.vidaAtual === undefined && (merged.vidaMaxima !== undefined || baseData.vidaMaxima !== undefined)) {
                merged.vidaAtual = merged.vidaMaxima ?? baseData.vidaMaxima;
            }
        }
        return merged;
    }

    if (baseData) {
        return {
            ...baseData,
            vidaAtual: baseData.vidaAtual ?? baseData.vidaMaxima ?? 0,
            estaVivo: true,
            estaAtivo: true,
            faceUp: true
        };
    }

    return {
        cardName: rawName,
        cardType: isHeroHint ? 'HERO' : 'SPELL',
        color: 'NEUTRAL',
        rarity: 'COMUM',
        descricao: '',
        espirito: 0,
        ataque: 0,
        defesa: 0,
        vidaMaxima: 15,
        vidaAtual: 15,
        estaVivo: true,
        estaAtivo: true,
        faceUp: true
    };
}

export function preloadAllCardArts(scene) {
    if (!scene || !scene.load || !scene.textures) return;

    if (!scene.textures.exists('card_back')) {
        scene.load.svg('card_back', 'images/ui/card_back.svg', { width: 280, height: 380 });
    }
    if (!scene.textures.exists('card_frame_hero')) {
        scene.load.svg('card_frame_hero', 'images/ui/card_frame_hero.svg', { width: 280, height: 380 });
    }
    if (!scene.textures.exists('card_frame_playable')) {
        scene.load.svg('card_frame_playable', 'images/ui/card_frame_playable.svg', { width: 280, height: 380 });
    }
    if (!scene.textures.exists('card_frame_item')) {
        scene.load.svg('card_frame_item', 'images/ui/card_frame_item.svg', { width: 280, height: 380 });
    }
    if (!scene.textures.exists('counter_combustao')) {
        scene.load.svg('counter_combustao', 'images/counters/counter_combustao.svg', { width: 64, height: 64 });
    }
    if (!scene.textures.exists('counter_seda')) {
        scene.load.svg('counter_seda', 'images/counters/counter_seda.svg', { width: 64, height: 64 });
    }
    if (!scene.textures.exists('counter_generic')) {
        scene.load.svg('counter_generic', 'images/counters/counter_generic.svg', { width: 64, height: 64 });
    }

    CARD_ART_KEYS.forEach((snakeKey) => {
        if (!scene.textures.exists(snakeKey)) {
            scene.load.image(snakeKey, `images/cartas/${snakeKey}.png`);
        }
    });
}

export function resolvePetFallbackKey(cardDto) {
    if (!cardDto) return null;
    const isPet = cardDto.cardType === 'PET' || cardDto.itemType === 'PET';
    if (!isPet) return null;

    const text = `${cardDto.cardName || ''} ${cardDto.descricao || ''}`;
    if (/tartaruga|quel[ôo]nio|casco|jabuti|c[áa]gado/i.test(text)) return 'pet_tartaruga';
    if (/felino|gat[oa]|pantera|tigre|le[ãa]o|lince|on[çc]a/i.test(text)) return 'pet_felino';
    return 'pet_canideo';
}

export function resolveCardArtTextureKey(scene, cardOrHeroDto) {
    if (!scene || !scene.textures || !cardOrHeroDto || cardOrHeroDto.faceUp === false) return null;

    if (cardOrHeroDto.imagem) {
        const cleanImg = toSnakeCaseKey(String(cardOrHeroDto.imagem).replace(/\.png$/i, ''));
        if (cleanImg && scene.textures.exists(cleanImg)) return cleanImg;
        if (scene.textures.exists(cardOrHeroDto.imagem)) return cardOrHeroDto.imagem;
    }

    const mappedFile = cardOrHeroDto.cardName ? CARD_ART_FILES[cardOrHeroDto.cardName] : null;
    if (mappedFile) {
        const mappedKey = mappedFile.replace(/\.png$/i, '');
        if (scene.textures.exists(mappedKey)) return mappedKey;
    }

    const snakeKey = toSnakeCaseKey(cardOrHeroDto.cardName || cardOrHeroDto.name || cardOrHeroDto.nome);
    if (snakeKey && scene.textures.exists(snakeKey)) return snakeKey;

    if (cardOrHeroDto.cardName && scene.textures.exists(cardOrHeroDto.cardName)) {
        return cardOrHeroDto.cardName;
    }

    const camelKey = toCamelCaseKey(cardOrHeroDto.cardName);
    if (camelKey && scene.textures.exists(camelKey)) return camelKey;

    const petFallback = resolvePetFallbackKey(cardOrHeroDto);
    if (petFallback && scene.textures.exists(petFallback)) return petFallback;

    if (cardOrHeroDto.uuid && scene.textures.exists(cardOrHeroDto.uuid)) {
        return cardOrHeroDto.uuid;
    }

    return null;
}

/**
 * Registra e aplica a pipeline CRT de forma idempotente na câmera da cena,
 * evitando recriar ou vazar framebuffers WebGL ao trocar de tela.
 */
export function applyCrtPipelineToScene(scene, CRTPostFXPipelineClass) {
    if (!scene || !scene.renderer || !scene.renderer.pipelines || !CRTPostFXPipelineClass) return;
    if (!scene.renderer.pipelines.has('CRTPostFX')) {
        scene.renderer.pipelines.addPostPipeline('CRTPostFX', CRTPostFXPipelineClass);
    }
    if (scene.cameras && scene.cameras.main) {
        scene.cameras.main.setPostPipeline('CRTPostFX');
    }
}

/**
 * Configura em qualquer Scene (CollectionScene, ShopScene, ProfileScene, etc.) os métodos
 * showLargeCardPreview(dto, isHero) e hideLargeCardPreview() idênticos aos da tela de partida!
 */
export function attachStandardCardPreviewToScene(scene, PlayableCardPrefabClass) {
    if (!scene) return;

    if (!scene.selectionManager) {
        scene.selectionManager = {
            selectedOwnHeroId: null,
            selectedEnemyHeroId: null,
            playedCardSlotMap: new Map()
        };
    }
    if (typeof scene.canDragHero !== 'function') {
        scene.canDragHero = () => false;
    }
    if (typeof scene.canDragHandCard !== 'function') {
        scene.canDragHandCard = () => false;
    }

    const { width, height } = BOARD_LAYOUT.LARGE_CARD; // 280 x 380
    scene.INSPECT_PREVIEW_SCALE = 1.22;

    const pos = (svgX, svgY) => ({
        x: ((svgX / 280) - 0.5) * width,
        y: ((svgY / 380) - 0.5) * height
    });

    scene.largePreviewContainer = scene.add.container(230, 560).setDepth(550).setVisible(false);
    scene.lpShadow = scene.add.rectangle(8, 10, width, height, 0x000000, 0.72);

    const artCenter = pos(137, 117);
    const artW = width * (248 / 280);
    const artH = height * (150 / 380);

    scene.lpArtBox = scene.add.rectangle(artCenter.x, artCenter.y, artW, artH, 0xd6cbb5, 1);
    scene.lpArtLabel = scene.add.text(artCenter.x, artCenter.y, 'ILUSTRAÇÃO', {
        fontFamily: 'IoskeleyMono, monospace',
        fontSize: '14px',
        color: '#475569',
        fontStyle: 'bold'
    }).setOrigin(0.5).setResolution(2);

    scene.lpFrame = scene.add.image(0, 0, 'card_frame_hero').setDisplaySize(width, height);

    const headerBarPt = pos(136, 21);
    scene.lpHeaderColorBar = scene.add.rectangle(
        headerBarPt.x,
        headerBarPt.y,
        width * (256 / 280),
        height * (28 / 380),
        0xffffff
    );

    const titlePt = pos(132, 21);
    scene.lpTitle = scene.add.text(titlePt.x, titlePt.y, '', {
        fontFamily: 'IoskeleyMono, monospace',
        fontSize: '16px',
        color: '#090d16',
        fontStyle: 'bold',
        align: 'center',
        wordWrap: { width: width * 0.82 }
    }).setOrigin(0.5).setResolution(2);

    const espPt = pos(264, 224);
    const atqPt = pos(264, 267);
    const defPt = pos(264, 310);
    const hpPt  = pos(264, 353);

    const statStyle = {
        fontFamily: 'IoskeleyMono, monospace',
        fontSize: '19px',
        color: '#090d16',
        fontStyle: 'bold',
        align: 'right'
    };

    scene.lpStatEsp = scene.add.text(espPt.x, espPt.y, '0', statStyle).setOrigin(1, 0.5).setResolution(2);
    scene.lpStatAtq = scene.add.text(atqPt.x, atqPt.y, '0', statStyle).setOrigin(1, 0.5).setResolution(2);
    scene.lpStatDef = scene.add.text(defPt.x, defPt.y, '0', statStyle).setOrigin(1, 0.5).setResolution(2);
    scene.lpStatHp  = scene.add.text(hpPt.x,  hpPt.y,  '0', {
        ...statStyle,
        fontSize: '20px',
        color: '#b91c1c'
    }).setOrigin(1, 0.5).setResolution(2);

    scene.lpTypeLine = scene.add.text(0, 0, '', {
        fontFamily: 'IoskeleyMono, monospace',
        fontSize: '12px',
        color: '#4a3520',
        fontStyle: 'bold',
        align: 'left',
        wordWrap: { width: width * (194 / 280) }
    }).setOrigin(0, 0).setResolution(2);

    scene.lpDesc = scene.add.text(0, 0, '', {
        fontFamily: 'IoskeleyMono, monospace',
        fontSize: '13px',
        color: '#090d16',
        fontStyle: 'bold',
        align: 'left',
        lineSpacing: 3,
        wordWrap: { width: width * (194 / 280) }
    }).setOrigin(0, 0).setResolution(2);

    scene.lpPhaseNodes = [0, 1, 2, 3].map(() => {
        const bg = scene.add.rectangle(0, 0, width * (22 / 280), height * (18 / 380), 0x111827, 1)
            .setStrokeStyle(1.5, 0x475569)
            .setVisible(false);
        const txt = scene.add.text(0, 0, '', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '13px',
            color: '#475569',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2).setVisible(false);
        return { bg, txt };
    });

    const lpPhaseElements = [];
    scene.lpPhaseNodes.forEach(n => lpPhaseElements.push(n.bg, n.txt));

    scene.largePreviewContainer.add([
        scene.lpShadow,
        scene.lpArtBox,
        scene.lpArtLabel,
        scene.lpFrame,
        scene.lpHeaderColorBar,
        scene.lpTitle,
        scene.lpStatEsp,
        scene.lpStatAtq,
        scene.lpStatDef,
        scene.lpStatHp,
        scene.lpTypeLine,
        scene.lpDesc,
        ...lpPhaseElements
    ]);

    const formatHeroTraits = (heroDto) => {
        const cap = (str) => {
            if (!str) return '';
            const clean = String(str).replace(/_/g, ' ').trim();
            return clean.charAt(0).toUpperCase() + clean.slice(1).toLowerCase();
        };
        const fmt = (val, prefix = '') => {
            if (!val) return [];
            const arr = Array.isArray(val) ? val : [val];
            return arr.filter(Boolean).map(v => `${prefix}${cap(v)}`);
        };
        return [
            ...fmt(heroDto.classe),
            ...fmt(heroDto.ordem),
            ...fmt(heroDto.racas || heroDto.raca),
            ...fmt(heroDto.afinidades || heroDto.afinidade, 'Afinidade ')
        ].join(' - ');
    };

    scene.showLargeCardPreview = (rawDto, isHero = false) => {
        if (!rawDto) return;
        const cardOrHeroDto = resolveFullCardOrHeroData(rawDto, isHero);
        const targetScale = scene.INSPECT_PREVIEW_SCALE || 1.22;

        const pointerX = scene.input?.activePointer?.x ?? 960;
        const previewX = pointerX < 960 ? 1660 : 260;
        const previewY = 560;
        scene.largePreviewContainer.setPosition(previewX, previewY);

        const isItem = !isHero && (
            cardOrHeroDto.cardType === 'ITEM' ||
            cardOrHeroDto.cardType === 'PET' ||
            (cardOrHeroDto.itemType && cardOrHeroDto.itemType !== 'NENHUM') ||
            cardOrHeroDto.cardName === 'Alma em Chamas'
        );

        const hasPhaseTrack = isHero || isItem;
        const cArt = pos(hasPhaseTrack ? 123 : 137, 117);
        const cArtW = width * ((hasPhaseTrack ? 218 : 248) / 280);
        const cArtH = height * (150 / 380);
        scene.lpArtBox.setPosition(cArt.x, cArt.y).setSize(cArtW, cArtH);
        scene.lpArtLabel.setPosition(cArt.x, cArt.y);

        if (hasPhaseTrack && scene.lpPhaseNodes && PlayableCardPrefabClass) {
            const phaseStates = PlayableCardPrefabClass.resolveActivePhaseNodes(cardOrHeroDto);
            scene.lpPhaseNodes.forEach((node, idx) => {
                const st = phaseStates[idx];
                const pt = pos(st.svgX, st.svgY);
                node.bg
                    .setPosition(pt.x, pt.y)
                    .setFillStyle(st.active ? st.color : 0x111827, 1)
                    .setStrokeStyle(st.active ? 2 : 1.5, st.active ? 0xffffff : 0x475569)
                    .setVisible(true);
                node.txt
                    .setPosition(pt.x, pt.y)
                    .setText(st.icon)
                    .setColor(st.active ? st.textColor : '#475569')
                    .setVisible(true);
            });
        } else if (scene.lpPhaseNodes) {
            scene.lpPhaseNodes.forEach(node => {
                node.bg.setVisible(false);
                node.txt.setVisible(false);
            });
        }

        const frameKey = isHero
            ? 'card_frame_hero'
            : ((isItem && scene.textures.exists('card_frame_item')) ? 'card_frame_item' : 'card_frame_playable');
        if (scene.textures.exists(frameKey)) {
            scene.lpFrame.setTexture(frameKey).setDisplaySize(width, height);
        }

        scene.lpTitle.setText(cardOrHeroDto.cardName || '');
        const theme = getCardVisualTheme(cardOrHeroDto.color, cardOrHeroDto.rarity);
        scene.lpHeaderColorBar.setFillStyle(theme.headerBg, 1);
        scene.lpTitle.setColor(theme.textColor);

        if (isHero) {
            scene.lpStatEsp.setText(String(cardOrHeroDto.espirito ?? 0)).setStyle({ color: '#090d16' }).setVisible(true);
            scene.lpStatAtq.setText(String(cardOrHeroDto.ataque ?? 0)).setStyle({ color: '#090d16' }).setVisible(true);
            scene.lpStatDef.setText(String(cardOrHeroDto.defesa ?? 0)).setStyle({ color: '#090d16' }).setVisible(true);
            scene.lpStatHp.setText(String(cardOrHeroDto.vidaAtual ?? cardOrHeroDto.vidaMaxima ?? 0)).setStyle({ color: '#b91c1c' }).setVisible(true);

            const traitsPt = pos(14, 201);
            scene.lpTypeLine
                .setOrigin(0, 0)
                .setPosition(traitsPt.x, traitsPt.y)
                .setStyle({
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '11.5px',
                    color: '#4a3520',
                    fontStyle: 'bold',
                    align: 'left',
                    wordWrap: { width: width * (194 / 280) }
                })
                .setText(formatHeroTraits(cardOrHeroDto));

            const descPt = pos(14, 238);
            const fullDesc = formatCardDescriptionWithKeywords(cardOrHeroDto.descricao || '');
            scene.lpDesc
                .setOrigin(0, 0)
                .setPosition(descPt.x, descPt.y)
                .setStyle({
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: fullDesc.length > 130 ? '11.5px' : '12.5px',
                    color: '#090d16',
                    fontStyle: 'bold',
                    align: 'left',
                    lineSpacing: 3,
                    wordWrap: { width: width * (194 / 280) }
                })
                .setText(fullDesc);
        } else if (isItem && PlayableCardPrefabClass) {
            const stats = PlayableCardPrefabClass.extractItemStatBonuses(cardOrHeroDto);
            scene.lpStatEsp.setText(stats.esp > 0 ? ('+' + stats.esp) : '-').setStyle({ color: '#6b21a8' }).setVisible(true);
            scene.lpStatAtq.setText(stats.atq > 0 ? ('+' + stats.atq) : '-').setStyle({ color: '#991b1b' }).setVisible(true);
            scene.lpStatDef.setText(stats.def > 0 ? ('+' + stats.def) : '-').setStyle({ color: '#0369a1' }).setVisible(true);
            scene.lpStatHp.setText(String(stats.emp || 0)).setStyle({ color: '#fde047' }).setVisible(true);

            const subType = (cardOrHeroDto.itemType && cardOrHeroDto.itemType !== 'NENHUM' && cardOrHeroDto.itemType !== cardOrHeroDto.cardType)
                ? (' • ' + cardOrHeroDto.itemType)
                : '';

            const typePt = pos(110, 214);
            scene.lpTypeLine
                .setOrigin(0.5, 0.5)
                .setPosition(typePt.x, typePt.y)
                .setStyle({
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '12px',
                    color: '#4a3520',
                    fontStyle: 'bold',
                    align: 'center',
                    wordWrap: { width: width * (192 / 280) }
                })
                .setText((cardOrHeroDto.cardType || 'ITEM') + subType);

            const descPt = pos(14, 236);
            scene.lpDesc
                .setOrigin(0, 0)
                .setPosition(descPt.x, descPt.y)
                .setStyle({
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '13px',
                    color: '#090d16',
                    fontStyle: 'bold',
                    align: 'left',
                    lineSpacing: 3,
                    wordWrap: { width: width * (192 / 280) }
                })
                .setText(stats.cleanDesc || formatCardDescriptionWithKeywords(cardOrHeroDto.descricao || ''));
        } else {
            scene.lpStatEsp.setVisible(false);
            scene.lpStatAtq.setVisible(false);
            scene.lpStatDef.setVisible(false);
            scene.lpStatHp.setVisible(false);

            const empTxt = (cardOrHeroDto.itemType === 'ARMA' && cardOrHeroDto.empunhadura > 0)
                ? ` • EMP: ${cardOrHeroDto.empunhadura}`
                : '';
            const subType = (cardOrHeroDto.itemType && cardOrHeroDto.itemType !== 'NENHUM')
                ? ` • ${cardOrHeroDto.itemType}`
                : '';

            const typePt = pos(138, 208);
            scene.lpTypeLine
                .setOrigin(0.5, 0.5)
                .setPosition(typePt.x, typePt.y)
                .setStyle({
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '13px',
                    color: '#090d16',
                    fontStyle: 'bold',
                    align: 'center',
                    wordWrap: { width: width * 0.88 }
                })
                .setText(`${cardOrHeroDto.cardType || ''}${subType}${empTxt}`);

            const descPt = pos(16, 230);
            scene.lpDesc
                .setOrigin(0, 0)
                .setPosition(descPt.x, descPt.y)
                .setStyle({
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '14px',
                    color: '#090d16',
                    fontStyle: 'bold',
                    align: 'left',
                    lineSpacing: 4,
                    wordWrap: { width: width * (244 / 280) }
                })
                .setText(formatCardDescriptionWithKeywords(cardOrHeroDto.descricao || ''));
        }

        const artKey = resolveCardArtTextureKey(scene, cardOrHeroDto);
        if (artKey) {
            if (!scene.lpArtSprite) {
                scene.lpArtSprite = scene.add.image(scene.lpArtBox.x, scene.lpArtBox.y, artKey);
                scene.lpArtSprite.texture.setFilter(Phaser.Textures.FilterMode.NEAREST);
                const frameIdx = scene.largePreviewContainer.getIndex(scene.lpFrame);
                scene.largePreviewContainer.addAt(scene.lpArtSprite, frameIdx);
            } else {
                scene.lpArtSprite.setTexture(artKey).setVisible(true);
            }
            scene.lpArtSprite.setPosition(scene.lpArtBox.x, scene.lpArtBox.y).setDisplaySize(scene.lpArtBox.width, scene.lpArtBox.height);
            scene.lpArtLabel.setVisible(false);
        } else {
            if (scene.lpArtSprite) scene.lpArtSprite.setVisible(false);
            scene.lpArtLabel.setVisible(true);
        }

        scene.largePreviewContainer.setVisible(true);
        scene.tweens.killTweensOf(scene.largePreviewContainer);
        scene.largePreviewContainer.setScale(targetScale * 0.92);
        scene.tweens.add({
            targets: scene.largePreviewContainer,
            scaleX: targetScale,
            scaleY: targetScale,
            duration: 160,
            ease: 'Cubic.easeOut'
        });

        if (scene.keywordTooltipsContainer) {
            scene.keywordTooltipsContainer.destroy(true);
        }

        const scaledHalfW = (width * targetScale) / 2;
        const tooltipCenterX = previewX < 960
            ? (previewX + scaledHalfW + 145)
            : (previewX - scaledHalfW - 145);

        scene.keywordTooltipsContainer = PhantomUIKit.createKeywordTooltipsContainer(
            scene,
            cardOrHeroDto,
            tooltipCenterX,
            previewY - ((height * targetScale) / 2)
        );
        if (scene.keywordTooltipsContainer) {
            scene.keywordTooltipsContainer.setDepth(555);
        }
    };

    scene.hideLargeCardPreview = () => {
        if (scene.largePreviewContainer) {
            scene.largePreviewContainer.setVisible(false);
        }
        if (scene.keywordTooltipsContainer) {
            scene.keywordTooltipsContainer.destroy(true);
            scene.keywordTooltipsContainer = null;
        }
    };
}

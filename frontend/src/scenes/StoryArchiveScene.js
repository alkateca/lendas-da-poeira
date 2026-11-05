import Phaser from 'phaser';
import CRTPostFXPipeline from '../fx/CRTPostFXPipeline.js';
import PhantomUIKit from '../ui/PhantomUIKit.js';
import StoryDialogueOverlay, { COLOR_WING_THEMES } from '../ui/StoryDialogueOverlay.js';
import { DEMO_MATCH_UUID, INITIAL_MATCH_SNAPSHOT } from '../config/mockSnapshots.js';
import { API_BASE_URL } from '../config/apiConfig.js';

const FALLBACK_STORY_MISSIONS = [
    {
        id: 'mission_red_01',
        titulo: 'O Primeiro Clarão da Forja',
        subtitulo: 'Arquétipo: Caçadores das Cinzas (Xia & Ferramentas)',
        cor: 'RED',
        capitulo: 1,
        unlocked: true,
        prologo: 'Nos ermos do leste, as fornalhas nunca se apagam. Os Caçadores das Cinzas aprendem a moldar a Poeira em armadilhas e arpões antes mesmo de encarar as abominações ancestrais.\n\nDiante do altar de registro, o Eco de Alka desperta para testar se sua velocidade tática é capaz de superar a guarda dos Heróis do Passado.',
        epilogo: 'Com o constructo e o Eco de Alka silenciados, a Poeira de fogo assentou revelando o primeiro segredo da forja e das ferramentas de caça.',
        scriptedEvents: [
            {
                trigger: 'ON_CARD_PLAYED',
                targetCardName: 'Bola de Fogo',
                speaker: 'Narrador dos Arquivos',
                dialogue: 'A canalização elemental de Fogo exige precisão e pureza de Espírito.'
            },
            {
                trigger: 'ON_HERO_DEATH',
                targetHeroName: 'Alka, Lutador do Grupo dos Heróis',
                speaker: 'Eco de Alka',
                dialogue: 'A chama... ainda arde mais forte que a minha guarda...'
            }
        ]
    },
    {
        id: 'mission_red_02',
        titulo: 'Cauterização nas Cinzas',
        subtitulo: 'Arquétipo: Círculo das Chamas (Alix & Sif)',
        cor: 'RED',
        capitulo: 2,
        unlocked: true,
        prologo: 'Enquanto os caçadores patrulham os ermos, os monges do Círculo das Chamas treinam o próprio corpo com mantos incandescentes para purificar os constructos corrompidos.\n\nNo segundo registro memorial, Leone ergue sua luz ancestral: apenas o fogo contínuo poderá cauterizar suas bênçãos.',
        epilogo: 'As chamas sagradas de Alix e Sif purificaram a miragem dos Arquivos. A Ala da Forja reconhece sua maestria.',
        scriptedEvents: [
            {
                trigger: 'ON_TURN_START',
                turnNumber: 2,
                speaker: 'Eco de Leone',
                dialogue: 'Sem cauterização, o fogo se consome sozinho. Mostre-me a resistência dos monges!'
            }
        ]
    },
    {
        id: 'mission_blue_01',
        titulo: 'O Juramento do Espelho Draconiano',
        subtitulo: 'Arquétipo: Cavaleiros de Cristal (Enamor & Onear)',
        cor: 'BLUE',
        capitulo: 1,
        unlocked: true,
        prologo: 'Os cristais são a própria Poeira condensada em estado sólido. Na ordem militar do Dragão de Cristal, cada cavaleiro porta armaduras espelhadas e sustenta a própria vida através da ressonância espiritual.\n\nTeste a autossustentação de Enamor contra as projeções brancas do Arquivo.',
        epilogo: 'O cristal jamais se estilhaça quando o Espírito é puro. O registro dos Cavaleiros de Cristal foi restaurado.',
        scriptedEvents: [
            {
                trigger: 'ON_CARD_PLAYED',
                targetCardName: 'Determinação Cristalina',
                speaker: 'Enamor (Registro)',
                dialogue: 'Nós não curamos a retaguarda; cada cavaleiro é uma fortaleza autossuficiente.'
            }
        ]
    },
    {
        id: 'mission_black_01',
        titulo: 'As Engrenagens do Cadafalso',
        subtitulo: 'Arquétipo: Legião Necromante & Ordem dos Carrascos',
        cor: 'BLACK',
        capitulo: 1,
        unlocked: true,
        prologo: 'Nas sombras das grandes guerras, conspiradores recolhem cristal quebrado e Poeira negra residual para alimentar a fábrica viva do Rei Demônio.\n\nQuando o inimigo sangra, a Ordem dos Carrascos não permite escapatória.',
        epilogo: 'Os ecos feridos sucumbiram ao cadafalso. Os segredos proibidos da Ruína foram catalogados.',
        scriptedEvents: [
            {
                trigger: 'ON_CARD_PLAYED',
                targetCardName: 'Ritos Fúnebres',
                speaker: 'Necromante das Areias',
                dialogue: 'A morte é apenas matéria-prima para a grande fábrica.'
            }
        ]
    },
    {
        id: 'mission_yellow_01',
        titulo: 'Silêncio Perante o Trono Imóvel',
        subtitulo: 'Arquétipo: Inquisição do Trono & Guarda Real',
        cor: 'YELLOW',
        capitulo: 1,
        unlocked: true,
        prologo: 'No centro da capital repousa o Rei Imóvel. Seus Guardiões dispensam armaduras mundanas: sua convicção se traduz em reservas colossais de vida e Teurgias que punem qualquer transgressão à lei.',
        epilogo: 'O veredito foi proclamado. Nenhuma miragem ousou quebrar a lei sagrada do Trono.',
        scriptedEvents: []
    },
    {
        id: 'mission_green_01',
        titulo: 'O Tratado da Tundra e das Engrenagens',
        subtitulo: 'Arquétipo: Lobos da Tundra & Sociedade Goblin',
        cor: 'GREEN',
        capitulo: 1,
        unlocked: true,
        prologo: 'Nas planícies gélidas do sul, Fenrir, Hati e Skoll caçam em simbiose com os eremitas, enquanto nas metrópoles a nobre e letrada Sociedade Goblin canaliza a Poeira como energia elétrica pura.',
        epilogo: 'A presa marcada pela vulnerabilidade caiu perante Fenrir, e os sábios Goblins registraram a vitória nos Arquivos.',
        scriptedEvents: []
    }
];

export default class StoryArchiveScene extends Phaser.Scene {
    constructor() {
        super({ key: 'StoryArchiveScene' });
        this.missions = [];
        this.selectedColor = 'RED';
        this.storyOverlay = null;
    }

    init(data) {
        this.player = data?.player || JSON.parse(localStorage.getItem('lendas_player')) || {
            uuid: 'local-player',
            name: 'Viajante da Poeira'
        };
        this.selectedColor = data?.selectedColor || 'RED';
    }

    create() {
        if (this.renderer.pipelines) {
            this.renderer.pipelines.addPostPipeline('CRTPostFX', CRTPostFXPipeline);
            this.cameras.main.setPostPipeline('CRTPostFX');
        }

        this.storyOverlay = new StoryDialogueOverlay(this);
        const { width, height } = this.scale;

        // Fundo temático dos Grandes Arquivos
        this.add.rectangle(width / 2, height / 2, width, height, 0x0b101b, 1);

        // Cabeçalho Superior
        this.add.rectangle(width / 2, 62, 1780, 76, 0x07090e, 0.96).setStrokeStyle(2, 0x334155);

        this.add.text(90, 46, '🏛️ GRANDES ARQUIVOS DA POEIRA // MODO HISTÓRIA PvE', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '22px',
            color: '#ffe600',
            fontStyle: 'bold'
        }).setResolution(2);

        this.add.text(90, 74, 'Sincronize com os Ecos da Memória (WHITE) para dominar as 5 cores do continente', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '13px',
            color: '#94a3b8'
        }).setResolution(2);

        PhantomUIKit.createRebelButton(this, 1730, 62, 210, 44, 'VOLTAR AO MENU', 0xe60012, () => {
            this.scene.start('MainMenuScene', { player: this.player });
        });

        this.wingTabsContainer = this.add.container(0, 0);
        this.missionsListContainer = this.add.container(0, 0);

        this.loadMissionsCatalog();
    }

    async loadMissionsCatalog() {
        try {
            const res = await fetch(`${API_BASE_URL}/api/story/missions?playerId=${this.player.uuid}`);
            if (res.ok) {
                const serverMissions = await res.json();
                if (Array.isArray(serverMissions) && serverMissions.length > 0) {
                    this.missions = serverMissions;
                }
            }
        } catch (e) {
            // Usa catálogo local se offline
        }

        if (!this.missions || this.missions.length === 0) {
            this.missions = FALLBACK_STORY_MISSIONS;
        }

        this.renderWingTabs();
        this.renderMissionsForSelectedWing();
    }

    renderWingTabs() {
        this.wingTabsContainer.removeAll(true);
        const wings = ['RED', 'BLUE', 'BLACK', 'YELLOW', 'GREEN'];
        const startX = 250;
        const gapX = 355;
        const y = 148;

        wings.forEach((colorKey, idx) => {
            const theme = COLOR_WING_THEMES[colorKey];
            const isSelected = this.selectedColor === colorKey;
            const x = startX + idx * gapX;

            const shadow = this.add.rectangle(x + 4, y + 4, 330, 56, theme.hex, isSelected ? 1 : 0.35);
            const tabBg = this.add.rectangle(x, y, 330, 56, isSelected ? 0x1e293b : 0x090d16, 0.96)
                .setStrokeStyle(isSelected ? 3 : 1.5, isSelected ? 0xffe600 : theme.hex)
                .setInteractive({ useHandCursor: true });

            const badgeTxt = this.add.text(x, y - 10, theme.badge, {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '14px',
                color: isSelected ? '#ffe600' : theme.textHex,
                fontStyle: 'bold'
            }).setOrigin(0.5).setResolution(2);

            const nameTxt = this.add.text(x, y + 12, theme.name, {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '11px',
                color: '#cbd5e1'
            }).setOrigin(0.5).setResolution(2);

            tabBg.on('pointerdown', () => {
                this.selectedColor = colorKey;
                this.renderWingTabs();
                this.renderMissionsForSelectedWing();
            });

            this.wingTabsContainer.add([shadow, tabBg, badgeTxt, nameTxt]);
        });
    }

    renderMissionsForSelectedWing() {
        this.missionsListContainer.removeAll(true);

        const theme = COLOR_WING_THEMES[this.selectedColor] || COLOR_WING_THEMES.RED;
        const wingMissions = this.missions.filter(m => String(m.cor).toUpperCase() === this.selectedColor);
        const completedIds = new Set(this.player.completedMissions || []);

        // Banner descritivo da Ala selecionada
        const wingBanner = this.add.rectangle(960, 232, 1780, 54, 0x0f172a, 0.95)
            .setStrokeStyle(2, theme.hex);
        const wingTitle = this.add.text(960, 232, `>> ${theme.name.toUpperCase()} — REGISTROS DISPONÍVEIS (${wingMissions.length}) <<`, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '16px',
            color: theme.textHex,
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        this.missionsListContainer.add([wingBanner, wingTitle]);

        wingMissions.forEach((mission, idx) => {
            const cardY = 350 + idx * 165;
            const isCompleted = Boolean(mission.completed || completedIds.has(mission.id));
            const isLocked = mission.unlocked === false;

            const cardW = 1680;
            const cardH = 136;
            const cx = 960;

            const shadow = this.add.rectangle(cx + 6, cardY + 6, cardW, cardH, theme.hex, 0.75);
            const bg = this.add.rectangle(cx, cardY, cardW, cardH, 0x07090e, 0.96)
                .setStrokeStyle(2, isCompleted ? 0x4ade80 : 0xf8fafc);

            const chapterBadge = this.add.rectangle(cx - cardW / 2 + 85, cardY, 130, 96, 0x111827, 1)
                .setStrokeStyle(2, theme.hex);
            const chapterTxt = this.add.text(cx - cardW / 2 + 85, cardY, `CAP.\n0${mission.capitulo || idx + 1}`, {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '20px',
                color: '#ffe600',
                fontStyle: 'bold',
                align: 'center'
            }).setOrigin(0.5).setResolution(2);

            const titleTxt = this.add.text(cx - cardW / 2 + 175, cardY - 34, mission.titulo.toUpperCase(), {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '21px',
                color: '#ffffff',
                fontStyle: 'bold'
            }).setOrigin(0, 0.5).setResolution(2);

            const subTxt = this.add.text(cx - cardW / 2 + 175, cardY - 4, mission.subtitulo || `Missão Guiada • Cor ${mission.cor}`, {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '13.5px',
                color: theme.textHex,
                fontStyle: 'bold'
            }).setOrigin(0, 0.5).setResolution(2);

            const previewPrologo = String(mission.prologo || '').replace(/\n+/g, ' ').slice(0, 135) + '...';
            const descTxt = this.add.text(cx - cardW / 2 + 175, cardY + 30, previewPrologo, {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '12.5px',
                color: '#94a3b8'
            }).setOrigin(0, 0.5).setResolution(2);

            const statusLabel = isCompleted ? '✅ CONCLUÍDA' : (isLocked ? '🔒 BLOQUEADA' : '🟢 DISPONÍVEL');
            const statusColor = isCompleted ? '#4ade80' : (isLocked ? '#64748b' : '#ffe600');

            const statusTxt = this.add.text(cx + cardW / 2 - 340, cardY, statusLabel, {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '14px',
                color: statusColor,
                fontStyle: 'bold'
            }).setOrigin(0.5).setResolution(2);

            this.missionsListContainer.add([
                shadow, bg, chapterBadge, chapterTxt,
                titleTxt, subTxt, descTxt, statusTxt
            ]);

            if (!isLocked) {
                const startBtn = PhantomUIKit.createRebelButton(
                    this,
                    cx + cardW / 2 - 140,
                    cardY,
                    220,
                    50,
                    'LER PRÓLOGO',
                    theme.hex,
                    () => this.openMissionPrologue(mission)
                );
                this.missionsListContainer.add(startBtn.container);
            }
        });
    }

    openMissionPrologue(mission) {
        this.storyOverlay.showFullNarrativeModal({
            headerTag: `CAPÍTULO ${mission.capitulo || 1} // PRÓLOGO DA MISSÃO`,
            title: mission.titulo,
            colorKey: mission.cor,
            narrativeText: mission.prologo,
            confirmLabel: '⚔️ INICIAR COMBATE MEMORIAL',
            onConfirm: () => this.launchStoryMissionMatch(mission),
            onCancel: () => {}
        });
    }

    async launchStoryMissionMatch(mission) {
        try {
            const res = await fetch(`${API_BASE_URL}/api/story/missions/${mission.id}/start`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    playerId: this.player.uuid,
                    playerName: this.player.name,
                    missionId: mission.id
                })
            });

            if (res.ok) {
                const data = await res.json();
                this.scene.start('StoryMatchScene', {
                    matchId: data.matchId,
                    localPlayerId: data.localPlayerId || this.player.uuid,
                    initialSnapshots: data.initialSnapshots || [],
                    missionData: data.mission || mission
                });
                return;
            }
        } catch (err) {
            console.warn('Servidor offline ou endpoint em construção. Iniciando StoryMatchScene com Mock Snapshot...', err);
        }

        // Fallback offline imediato para você já testar toda a UI e os gatilhos
        const offlineSnap = JSON.parse(JSON.stringify(INITIAL_MATCH_SNAPSHOT));
        offlineSnap.activeTurnPlayerId = this.player.uuid;
        offlineSnap.localPlayer.playerId = this.player.uuid;
        offlineSnap.localPlayer.name = this.player.name;
        offlineSnap.opponentPlayer.name = 'Ecos da Memória (WHITE)';

        this.scene.start('StoryMatchScene', {
            matchId: DEMO_MATCH_UUID,
            localPlayerId: this.player.uuid,
            initialSnapshots: [offlineSnap],
            missionData: mission
        });
    }
}

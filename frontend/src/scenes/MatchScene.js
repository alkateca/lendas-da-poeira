import Phaser from 'phaser';
import { BOARD_LAYOUT } from '../config/BoardCoordinates.js';
import HeroCardPrefab from '../prefabs/HeroCardPrefab.js';
import PlayableCardPrefab from '../prefabs/PlayableCardPrefab.js';
import SnapshotPlayerManager from '../managers/SnapshotPlayerManager.js';
import SelectionManager from '../managers/SelectionManager.js';
import CRTPostFXPipeline from '../fx/CRTPostFXPipeline.js';
import PhantomUIKit from '../ui/PhantomUIKit.js';

export default class MatchScene extends Phaser.Scene {
    constructor() {
        super({ key: 'MatchScene' });
        this.matchId = null;
        this.localPlayerId = null;
        this.currentSnapshot = null;

        this.heroPrefabs = new Map();
        this.spawnedCardPrefabs = new Map();
        this.handCardPrefabs = [];
        this.opponentHandDummies = [];
        this.resolveCardPrefabs = [];
        this.resolveSlotVisuals = [];
        this.oppBenchSlotVisuals = [];
        this.localBenchSlotVisuals = [];

        this.isWaitingOpponentConfirmation = false;
        this.isSubmittingAction = false;
        this.isPolling = false;
        this.isChoiceModalActive = false;
        this.isSettingsModalOpen = false;
        this.lastCutInTime = 0;
        this.musicVolume = Number(localStorage.getItem('ldp_music_volume') ?? 0.7);
        this.sfxVolume = Number(localStorage.getItem('ldp_sfx_volume') ?? 0.8);

        this.combatStartSpawnedOrder = [];
    }

    init(data) {
        this.matchId = data.matchId;
        this.localPlayerId = data.localPlayerId;
        this.initialSnapshots = data.initialSnapshots || (data.initialSnapshot ? [data.initialSnapshot] : []);
    }

    preload() {
        this.load.setPath('assets');

        if (!this.textures.exists('card_back')) {
            this.load.svg('card_back', 'images/ui/card_back.svg', { width: 280, height: 380 });
        }

        // Novos SVGs em /assets/images/ui/
        if (!this.textures.exists('card_frame_hero')) {
            this.load.svg('card_frame_hero', 'images/ui/card_frame_hero.svg', { width: 280, height: 380 });
        }
        if (!this.textures.exists('card_frame_playable')) {
            this.load.svg('card_frame_playable', 'images/ui/card_frame_playable.svg', { width: 280, height: 380 });
        }
        if (!this.textures.exists('ui_hud_panel_light')) {
            this.load.svg('ui_hud_panel_light', 'images/ui/ui_hud_panel_light.svg', { width: 360, height: 80 });
        }
        if (!this.textures.exists('ui_slot_frame')) {
            this.load.svg('ui_slot_frame', 'images/ui/ui_slot_frame.svg', { width: 160, height: 210 });
        }
        if (!this.textures.exists('ui_liberation_frame')) {
            this.load.svg('ui_liberation_frame', 'images/ui/ui_liberation_frame.svg', { width: 280, height: 380 });
        }
        if (!this.textures.exists('ui_banner_player')) {
            this.load.svg('ui_banner_player', 'images/ui/ui_banner_player.svg', { width: 360, height: 76 });
        }
        if (!this.textures.exists('ui_p5_crt_button')) {
            this.load.svg('ui_p5_crt_button', 'images/ui/p5_crt_button.svg', { width: 520, height: 84 });
        }
        if (!this.textures.exists('ui_arena_crt_radar')) {
            this.load.svg('ui_arena_crt_radar', 'images/ui/arena_crt_radar.svg', { width: 800, height: 800 });
        }
        if (!this.textures.exists('ui_graveyard_glitch')) {
            this.load.svg('ui_graveyard_glitch', 'images/ui/graveyard_glitch.svg', { width: 82, height: 96 });
        }
        if (!this.textures.exists('card_discard_back')) {
            this.load.image('card_discard_back', 'images/modeloBaralhoDescarte.png');
        }
        if (!this.textures.exists('arena_crt_radar')) {
            this.load.svg('arena_crt_radar', 'images/ui/arena_crt_radar.svg', { width: 800, height: 800 });
        }
        if (!this.textures.exists('vga_chains_overlay')) {
            this.load.svg('vga_chains_overlay', 'images/ui/vga_chains_overlay.svg', { width: 280, height: 380 });
        }
        if (!this.textures.exists('vga_chain_link_shard')) {
            this.load.svg('vga_chain_link_shard', 'images/ui/vga_chain_link_shard.svg', { width: 48, height: 48 });
        }

        // Spritesheets de Efeitos Visuais portadas do partida.lua
        if (!this.textures.exists('vfx_danoMagico')) {
            this.load.spritesheet('vfx_danoMagico', 'images/BlueExplosionA_spritesheet.png', {
                frameWidth: 65,
                frameHeight: 63
            });
        }
        if (!this.textures.exists('vfx_danoFisico')) {
            this.load.spritesheet('vfx_danoFisico', 'images/DustExplosion_spritesheet.png', {
                frameWidth: 65,
                frameHeight: 63
            });
        }
        if (!this.textures.exists('vfx_danoDireto')) {
            this.load.spritesheet('vfx_danoDireto', 'images/SimpleExplosionC_spritesheet.png', {
                frameWidth: 59,
                frameHeight: 63
            });
        }
        if (!this.textures.exists('vfx_buff')) {
            this.load.spritesheet('vfx_buff', 'images/1712.png', {
                frameWidth: 64,
                frameHeight: 72
            });
        }
        if (!this.textures.exists('vfx_debuff')) {
            this.load.spritesheet('vfx_debuff', 'images/debuff.png', {
                frameWidth: 64,
                frameHeight: 72
            });
        }
        if (!this.textures.exists('vfx_cura_raw')) {
            this.load.image('vfx_cura_raw', 'images/HealingEffect_spritesheet.png');
        }

        const cardKeys = [
            'ataqueMagico', 'atmosferaPesada', 'autoDefesaMagica', 'banir', 'barreiraDeGelo',
            'bolaDeFogo', 'brocheCristal', 'convocacao', 'determinacaoCristalina', 'dragaoCristal',
            'dragast', 'estatica', 'exumacao', 'fragmentoAfiado', 'garraEspectral',
            'golpesPesados', 'grandeMachadoSombrio', 'homunculoCarniceiro', 'laminaDeCristal',
            'maestriaComChamas', 'massacreCristalino', 'mirarNaCabeca', 'paraRaios', 'pontoFinal',
            'quebra', 'quimera', 'quimeraNegra', 'racaoDeEmergencia', 'rapieiraDeCristal',
            'risadaSarcastica', 'ritosFunebres', 'tristeza', 'vendavalArcano'
        ];
        cardKeys.forEach(key => {
            if (!this.textures.exists(key)) {
                this.load.image(key, `images/cartas/${key}.png`);
            }
        });

        // Mapeamento das artes de cartas existentes em assets/images/cartas/
        const cardArtFiles = {
            'Ataque Mágico': 'ataqueMagico.png',
            'Atmosfera Pesada': 'atmosferaPesada.png',
            'Auto Defesa Magica': 'autoDefesaMagica.png',
            'Banir!': 'banir.png',
            'Barreira de Gelo': 'barreiraDeGelo.png',
            'Bola de fogo': 'bolaDeFogo.png',
            'Bola de Fogo': 'bolaDeFogo.png',
            'Broche de Cristal': 'brocheCristal.png',
            'Convocação': 'convocacao.png',
            'Determinação Cristalina': 'determinacaoCristalina.png',
            'Dragão de Cristal': 'dragaoCristal.png',
            'Dragast': 'dragast.png',
            'Estatica': 'estatica.png',
            'Exumação': 'exumacao.png',
            'Fragmento Afiado': 'fragmentoAfiado.png',
            'Garra espectral': 'garraEspectral.png',
            'Golpes Pesados': 'golpesPesados.png',
            'Grande Machado Sombrio': 'grandeMachadoSombrio.png',
            'Homunculo Carniceiro': 'homunculoCarniceiro.png',
            'Lamina de Cristal': 'laminaDeCristal.png',
            'Maestria com Chamas': 'maestriaComChamas.png',
            'Massacre Cristalino': 'massacreCristalino.png',
            'Mirar na cabeça': 'mirarNaCabeca.png',
            'Para-raios': 'paraRaios.png',
            'Ponto Final': 'pontoFinal.png',
            'Quebra!': 'quebra.png',
            'Quimera': 'quimera.png',
            'Quimera Negra': 'quimeraNegra.png',
            'Ração de emergencia': 'racaoDeEmergencia.png',
            'Rapieira de Cristal': 'rapieiraDeCristal.png',
            'Risada Sarcástica': 'risadaSarcastica.png',
            'Ritos Fúnebres': 'ritosFunebres.png',
            'Tristeza': 'tristeza.png',
            'Vendaval Arcano': 'vendavalArcano.png'
        };

        Object.entries(cardArtFiles).forEach(([cardName, fileName]) => {
            if (!this.textures.exists(cardName)) {
                this.load.image(cardName, `images/cartas/${fileName}`);
            }
        });
    }

    create() {
        if (this.renderer.pipelines) {
            this.renderer.pipelines.addPostPipeline('CRTPostFX', CRTPostFXPipeline);
            this.cameras.main.setPostPipeline('CRTPostFX');
        }

        this.registerCombatAnimations();
        this.heroPrefabs.clear();
        this.spawnedCardPrefabs = new Map();
        this.handCardPrefabs = [];
        this.opponentHandDummies = [];
        this.resolveCardPrefabs = [];
        this.resolveSlotVisuals = [];
        this.oppBenchSlotVisuals = [];
        this.localBenchSlotVisuals = [];
        this.currentSnapshot = null;
        this.isWaitingOpponentConfirmation = false;
        this.isSubmittingAction = false;
        this.isPolling = false;
        this.isChoiceModalActive = false;
        this.combatStartSpawnedOrder = [];

        this.snapshotPlayer = new SnapshotPlayerManager(this);
        this.selectionManager = new SelectionManager(this);

        this.drawBoardBackgroundAndSlots();
        this.createPilesPlayerBannersAndMainButton();
        this.createLiberationZonePanels();
        this.createLargeHoverPreviewPanel();
        this.createDiscardInspectionModal();
        this.createSettingsGearAndModal();
        this.createGameOverModal();

        if (this.initialSnapshots.length > 0) {
            this.snapshotPlayer.enqueueSnapshots(this.initialSnapshots);
        }

        this.time.addEvent({
            delay: 2000,
            loop: true,
            callback: () => this.pollServerStateIfWaiting()
        });
    }

    registerCombatAnimations() {
        const frameRate = Math.round(1 / 0.06); // 0.06s por frame no Lua (~16.6 FPS)

        // Equivalente ao criarQuadsGrid(imgCurarPersonagem, 5, 3) do partida.lua
        if (this.textures.exists('vfx_cura_raw') && !this.textures.exists('vfx_cura')) {
            const rawImg = this.textures.get('vfx_cura_raw').getSourceImage();
            const frameW = Math.floor(rawImg.width / 5);
            const frameH = Math.floor(rawImg.height / 3);
            this.textures.addSpriteSheet('vfx_cura', rawImg, {
                frameWidth: frameW,
                frameHeight: frameH
            });
        }

        const vfxDefinitions = [
            { key: 'danoMagico', texture: 'vfx_danoMagico', scale: 3.2 },
            { key: 'danoFisico', texture: 'vfx_danoFisico', scale: 4.2 },
            { key: 'danoDireto', texture: 'vfx_danoDireto', scale: 2.6 },
            { key: 'cura',       texture: 'vfx_cura',       scale: 0.75 },
            { key: 'buff',       texture: 'vfx_buff',       scale: 2.2 },
            { key: 'debuff',     texture: 'vfx_debuff',     scale: 2.2 }
        ];

        this.vfxScales = {};

        vfxDefinitions.forEach(def => {
            this.vfxScales[def.key] = def.scale;
            if (this.textures.exists(def.texture)) {
                this.textures.get(def.texture).setFilter(Phaser.Textures.FilterMode.NEAREST);
                if (!this.anims.exists(def.key)) {
                    this.anims.create({
                        key: def.key,
                        frames: this.anims.generateFrameNumbers(def.texture, {}),
                        frameRate: frameRate,
                        repeat: 0
                    });
                }
            }
        });
    }

    /**
     * Equivalente ao Partida.tocarAnimacao(nomeAnimacao, x, y) do partida.lua
     */
    playVisualEffect(animKey, x, y) {
        if (!this.anims.exists(animKey)) return;

        const anim = this.anims.get(animKey);
        const firstFrame = anim.frames[0];
        if (!firstFrame) return;

        const vfxSprite = this.add.sprite(x, y, firstFrame.textureKey)
            .setDepth(175)
            .setOrigin(0.5, 0.5)
            .setScale(this.vfxScales?.[animKey] || 2.0);

        vfxSprite.play(animKey);
        vfxSprite.once('animationcomplete', () => {
            vfxSprite.destroy();
        });
    }

    drawSlotCornerBrackets(graphics, w, h, color, arm = 18, lineWidth = 2.5) {
        graphics.clear();
        const hw = w / 2;
        const hh = h / 2;

        graphics.lineStyle(lineWidth, color, 1);

        // Top-Left (L)
        graphics.beginPath();
        graphics.moveTo(-hw + arm, -hh);
        graphics.lineTo(-hw, -hh);
        graphics.lineTo(-hw, -hh + arm);
        graphics.strokePath();

        // Top-Right (L)
        graphics.beginPath();
        graphics.moveTo(hw - arm, -hh);
        graphics.lineTo(hw, -hh);
        graphics.lineTo(hw, -hh + arm);
        graphics.strokePath();

        // Bottom-Left (L)
        graphics.beginPath();
        graphics.moveTo(-hw + arm, hh);
        graphics.lineTo(-hw, hh);
        graphics.lineTo(-hw, hh - arm);
        graphics.strokePath();

        // Bottom-Right (L)
        graphics.beginPath();
        graphics.moveTo(hw - arm, hh);
        graphics.lineTo(hw, hh);
        graphics.lineTo(hw, hh - arm);
        graphics.strokePath();
    }

    drawBoardBackgroundAndSlots() {
        const { COLORS, MEDIUM_CARD, BENCH_BOXES, ARENA_CIRCLE } = BOARD_LAYOUT;

        // Fundo mais claro que o preto puro, destacando as cartas e os painéis
        this.add.rectangle(960, 540, 1920, 1080, 0x1b2230, 1);

        // Radar Vetorial Central
        const diameter = ARENA_CIRCLE.radius * 2;
        if (this.textures.exists('ui_arena_crt_radar')) {
            this.arenaRadar = this.add.image(ARENA_CIRCLE.x, ARENA_CIRCLE.y, 'ui_arena_crt_radar')
                .setDisplaySize(diameter, diameter)
                .setAlpha(0.95);
            this.tweens.add({ targets: this.arenaRadar, angle: 360, duration: 90000, repeat: -1 });
        } else {
            const graphics = this.add.graphics();
            graphics.fillStyle(0x263042, 1);
            graphics.fillCircle(ARENA_CIRCLE.x, ARENA_CIRCLE.y, ARENA_CIRCLE.radius);
        }

        // Bancadas com borda clara e rótulos em Branco/Amarelo de alta visibilidade
        this.add.rectangle(BENCH_BOXES.OPPONENT.x, BENCH_BOXES.OPPONENT.y, BENCH_BOXES.OPPONENT.width, BENCH_BOXES.OPPONENT.height, 0x263042, 0.9)
            .setStrokeStyle(2, 0x475569);
        this.add.text(BENCH_BOXES.OPPONENT.x, BENCH_BOXES.OPPONENT.y - 128, '▲ BANCADA INIMIGA', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '14px', color: '#e2e8f0', fontStyle: 'bold'
        }).setOrigin(0.5);

        this.add.rectangle(BENCH_BOXES.LOCAL.x, BENCH_BOXES.LOCAL.y, BENCH_BOXES.LOCAL.width, BENCH_BOXES.LOCAL.height, 0x263042, 0.9)
            .setStrokeStyle(2, 0x475569);
        this.add.text(BENCH_BOXES.LOCAL.x, BENCH_BOXES.LOCAL.y - 128, '▼ SUA BANCADA', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '14px', color: '#ffe600', fontStyle: 'bold'
        }).setOrigin(0.5);

        // Slots das Bancadas usando ui_slot_frame.svg
        BOARD_LAYOUT.OPPONENT_BENCH_SLOTS.forEach(pos => {
            this.add.image(pos.x, pos.y, 'ui_slot_frame').setDisplaySize(MEDIUM_CARD.width + 8, MEDIUM_CARD.height + 8);
        });

        BOARD_LAYOUT.LOCAL_BENCH_SLOTS.forEach(pos => {
            this.add.image(pos.x, pos.y, 'ui_slot_frame').setDisplaySize(MEDIUM_CARD.width + 8, MEDIUM_CARD.height + 8);
        });

        // Slots de Batalha Ativa
        this.oppBattleSlotRect = this.add.rectangle(
            BOARD_LAYOUT.BATTLE_SLOTS.OPPONENT_ACTIVE_HERO.x,
            BOARD_LAYOUT.BATTLE_SLOTS.OPPONENT_ACTIVE_HERO.y,
            MEDIUM_CARD.width + 10, MEDIUM_CARD.height + 10, 0x2d3a50, 1
        );
        this.add.image(
            BOARD_LAYOUT.BATTLE_SLOTS.OPPONENT_ACTIVE_HERO.x,
            BOARD_LAYOUT.BATTLE_SLOTS.OPPONENT_ACTIVE_HERO.y,
            'ui_slot_frame'
        ).setDisplaySize(MEDIUM_CARD.width + 12, MEDIUM_CARD.height + 12).setTint(0x38bdf8);

        this.add.text(BOARD_LAYOUT.BATTLE_SLOTS.OPPONENT_ACTIVE_HERO.x, BOARD_LAYOUT.BATTLE_SLOTS.OPPONENT_ACTIVE_HERO.y, 'BATALHA\nINIMIGO', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '13px', color: '#bae6fd', fontStyle: 'bold', align: 'center'
        }).setOrigin(0.5);

        this.localBattleSlotRect = this.add.rectangle(
            BOARD_LAYOUT.BATTLE_SLOTS.LOCAL_ACTIVE_HERO.x,
            BOARD_LAYOUT.BATTLE_SLOTS.LOCAL_ACTIVE_HERO.y,
            MEDIUM_CARD.width + 10, MEDIUM_CARD.height + 10, 0x2d3a50, 1
        );
        this.add.image(
            BOARD_LAYOUT.BATTLE_SLOTS.LOCAL_ACTIVE_HERO.x,
            BOARD_LAYOUT.BATTLE_SLOTS.LOCAL_ACTIVE_HERO.y,
            'ui_slot_frame'
        ).setDisplaySize(MEDIUM_CARD.width + 12, MEDIUM_CARD.height + 12).setTint(0xf87171);

        this.add.text(BOARD_LAYOUT.BATTLE_SLOTS.LOCAL_ACTIVE_HERO.x, BOARD_LAYOUT.BATTLE_SLOTS.LOCAL_ACTIVE_HERO.y, 'BATALHA\nALIADO', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '13px', color: '#fecaca', fontStyle: 'bold', align: 'center'
        }).setOrigin(0.5);

        // 4 Slots de Resolução Central
        this.resolveSlotVisuals = BOARD_LAYOUT.RESOLVE_SLOTS.map((pos, idx) => {
            const rect = this.add.rectangle(pos.x, pos.y, MEDIUM_CARD.width + 4, MEDIUM_CARD.height + 4, COLORS.SLOT_SURFACE, 1);
            const frame = this.add.image(pos.x, pos.y, 'ui_slot_frame').setDisplaySize(MEDIUM_CARD.width + 8, MEDIUM_CARD.height + 8);
            const label = this.add.text(pos.x, pos.y, `${idx + 1}º AÇÃO`, {
                fontFamily: 'IoskeleyMono, monospace', fontSize: '13px', color: '#ffffff', fontStyle: 'bold', align: 'center'
            }).setOrigin(0.5);
            return { rect, frame, label, x: pos.x, y: pos.y, index: idx };
        });
    }

    updateInterleavedResolveSlots(localHasInitiative) {
        const { COLORS } = BOARD_LAYOUT;

        this.resolveSlotVisuals.forEach((slot, idx) => {
            const belongsToLocal = localHasInitiative ? (idx % 2 === 0) : (idx % 2 !== 0);
            const cornerColor = belongsToLocal ? 0xe60012 : 0x00f0ff;
            const fillColor = belongsToLocal ? COLORS.LOCAL_SLOT_TINT : COLORS.OPP_SLOT_TINT;
            const labelColor = belongsToLocal ? '#fca5a5' : '#7dd3fc';
            const ownerTag = belongsToLocal ? 'VOCÊ' : 'OPONENTE';

            slot.belongsToLocal = belongsToLocal;
            slot.cornerColor = cornerColor;
            slot.baseFill = fillColor;
            slot.rect.setFillStyle(fillColor, 1).setStrokeStyle(0);
            if (slot.graphics) {
                this.drawSlotCornerBrackets(slot.graphics, slot.w, slot.h, cornerColor);
            }
            slot.label.setText(`${idx + 1}º AÇÃO\n(${ownerTag})`).setColor(labelColor);
        });
    }

    createPilesPlayerBannersAndMainButton() {
        const { PILES, PLAYER_BANNERS, COLORS } = BOARD_LAYOUT;

        this.oppDeckText = this.createMiniDeckIcon(PILES.OPPONENT_DECK.x, PILES.OPPONENT_DECK.y);
        this.oppDiscardText = this.createMiniTombstoneIcon(PILES.OPPONENT_DISCARD.x, PILES.OPPONENT_DISCARD.y, () => {
            if (this.currentSnapshot) {
                this.openDiscardModal('Cemitério do Oponente', this.currentSnapshot.opponentPlayer.discardPile);
            }
        });

        this.localDeckText = this.createMiniDeckIcon(PILES.LOCAL_DECK.x, PILES.LOCAL_DECK.y);
        this.localDiscardText = this.createMiniTombstoneIcon(PILES.LOCAL_DISCARD.x, PILES.LOCAL_DISCARD.y, () => {
            if (this.currentSnapshot) {
                this.openDiscardModal('Meu Cemitério (Descarte)', this.currentSnapshot.localPlayer.discardPile);
            }
        });

        this.oppBannerContainer = this.createPlayerCornerBanner(
            PLAYER_BANNERS.OPPONENT.x, PLAYER_BANNERS.OPPONENT.y,
            'P2', 'OPONENTE', 'Jogador 2', COLORS.OPP_BLUE
        );

        this.localBannerContainer = this.createPlayerCornerBanner(
            PLAYER_BANNERS.LOCAL.x, PLAYER_BANNERS.LOCAL.y,
            'P1', 'VOCÊ', 'Jogador 1', COLORS.LOCAL_RED
        );

        this.phaseBanner = this.add.text(1035, 88, 'FASE: AGUARDANDO...', {
            fontSize: '18px', color: '#ffd700', fontStyle: 'bold'
        }).setOrigin(0.5);

        this.combatLogText = this.add.text(1035, 114, '', {
            fontSize: '13px', color: '#e2e8f0', align: 'center', wordWrap: { width: 650 }
        }).setOrigin(0.5);

        this.mainButtonBg = this.add.image(PILES.MAIN_BUTTON.x, PILES.MAIN_BUTTON.y, 'ui_p5_crt_button')
            .setDisplaySize(290, 76)
            .setDepth(80)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => this.onMainButtonClicked());

        this.mainButtonBg.setFillStyle = (color) => {
            if (color === 0x475569) {
                this.mainButtonBg.setTint(0x777777).setAlpha(0.55);
            } else if (color === 0xd97706) {
                this.mainButtonBg.setTint(0xffaa00).setAlpha(1.0);
            } else {
                this.mainButtonBg.clearTint().setAlpha(1.0);
            }
            return this.mainButtonBg;
        };

        this.mainButtonLabel = this.add.text(PILES.MAIN_BUTTON.x + 12, PILES.MAIN_BUTTON.y - 2, 'CONFIRMAR', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '15px',
            color: '#ffffff',
            fontStyle: 'bold',
            align: 'center'
        }).setOrigin(0.5).setDepth(81);
    }

    createMiniDeckIcon(x, y) {
        const { width, height } = BOARD_LAYOUT.MEDIUM_CARD; // 140 x 190
        const container = this.add.container(x, y);

        // Sombra da pilha e 3 camadas do verso da carta no tamanho dos Heróis do banco
        const shadow = this.add.rectangle(6, 8, width, height, 0x090d16, 0.7);
        const cardBottom = this.add.image(-6, 6, 'card_back').setDisplaySize(width, height).setTint(0x94a3b8);
        const cardMiddle = this.add.image(-3, 3, 'card_back').setDisplaySize(width, height).setTint(0xcbd5e1);
        const cardTop    = this.add.image(0, 0, 'card_back').setDisplaySize(width, height);

        // Faixa clara de leitura para o contador de cartas (ex: 15/30)
        const badgeShadow = this.add.rectangle(3, 52, width - 16, 44, 0xe60012, 1);
        const badgeBg = this.add.rectangle(0, 49, width - 16, 44, 0xfaf8f5, 0.98)
            .setStrokeStyle(3, 0x090d16);

        const lbl = this.add.text(0, 38, 'BARALHO', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '10px',
            color: '#475569',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        const countTxt = this.add.text(0, 56, '15/30', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '16px',
            color: '#090d16',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        container.add([shadow, cardBottom, cardMiddle, cardTop, badgeShadow, badgeBg, lbl, countTxt]);
        return countTxt;
    }

    createMiniTombstoneIcon(x, y, onClickCallback) {
        const container = this.add.container(x, y);

        const graveSvg = this.add.image(0, 0, 'ui_graveyard_glitch')
            .setDisplaySize(82, 96)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', onClickCallback);

        graveSvg.on('pointerover', () => {
            this.tweens.add({ targets: container, scaleX: 1.08, scaleY: 1.08, angle: -3, duration: 120 });
        });
        graveSvg.on('pointerout', () => {
            this.tweens.add({ targets: container, scaleX: 1.0, scaleY: 1.0, angle: 0, duration: 120 });
        });

        const countTxt = this.add.text(0, 32, '🪦 0', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '12px',
            color: '#00f0ff',
            fontStyle: 'bold',
            stroke: '#050507',
            strokeThickness: 3
        }).setOrigin(0.5);

        container.add([graveSvg, countTxt]);
        return countTxt;
    }

    createPlayerCornerBanner(x, y, badgeCode, roleLabel, defaultName, fillColor) {
        const container = this.add.container(x, y);
        const bg = this.add.image(0, 0, 'ui_banner_player').setDisplaySize(360, 74);
        const avatar = this.add.circle(-144, 0, 20, fillColor).setStrokeStyle(2, 0xffffff);
        const codeTxt = this.add.text(-144, 0, badgeCode, {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '14px', color: '#ffffff', fontStyle: 'bold'
        }).setOrigin(0.5);
        const roleTxt = this.add.text(-102, -18, roleLabel, {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '11.5px', color: '#e60012', fontStyle: 'bold'
        }).setOrigin(0, 0.5);
        const nameTxt = this.add.text(-102, -1, defaultName, {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '15.5px', color: '#090d16', fontStyle: 'bold'
        }).setOrigin(0, 0.5);
        const eloTxt = this.add.text(-102, 18, '🏅 ELO: 1000  |  📅 Semanal: 1000', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '11.5px', color: '#1e3a8a', fontStyle: 'bold'
        }).setOrigin(0, 0.5);

        container.add([bg, avatar, codeTxt, roleTxt, nameTxt, eloTxt]);
        return { container, roleTxt, nameTxt, eloTxt };
    }

    createLargeHoverPreviewPanel() {
        const { x, y } = BOARD_LAYOUT.PILES.LARGE_PREVIEW;
        const { width, height } = BOARD_LAYOUT.LARGE_CARD; // 280 x 380

        const pos = (svgX, svgY) => ({
            x: ((svgX / 280) - 0.5) * width,
            y: ((svgY / 380) - 0.5) * height
        });

        this.largePreviewContainer = this.add.container(x, y).setDepth(190).setVisible(false);

        this.lpShadow = this.add.rectangle(8, 10, width, height, 0x000000, 0.65);

        // Janela de arte (SVG x: 12..261, y: 41..192 -> centro 137, 117)
        const artCenter = pos(137, 117);
        const artW = width * (248 / 280);
        const artH = height * (150 / 380);

        // Fundo creme suave quando não há imagem (em vez de bloco colorido forte)
        this.lpArtBox = this.add.rectangle(artCenter.x, artCenter.y, artW, artH, 0xd6cbb5, 1);
        this.lpArtLabel = this.add.text(artCenter.x, artCenter.y, 'ILUSTRAÇÃO', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '14px',
            color: '#475569',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        this.lpFrame = this.add.image(0, 0, 'card_frame_hero').setDisplaySize(width, height);

        // 1. Título no Topo (SVG X=132, Y=21)
        const titlePt = pos(132, 21);
        this.lpTitle = this.add.text(titlePt.x, titlePt.y, '', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '16px',
            color: '#090d16',
            fontStyle: 'bold',
            align: 'center',
            wordWrap: { width: width * 0.82 }
        }).setOrigin(0.5).setResolution(2);

        // 2. Modificadores de Dano nas 3 Casas (SVG Y=174 | X=186, 220, 254)
        const mod1 = pos(186, 174);
        const mod2 = pos(220, 174);
        const mod3 = pos(254, 174);

        this.lpModBonus = this.add.text(mod1.x, mod1.y, '0', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '18px', color: '#15803d', fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        this.lpModReducao = this.add.text(mod2.x, mod2.y, '0', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '18px', color: '#dc2626', fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        this.lpModVulnerabilidade = this.add.text(mod3.x, mod3.y, '0', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '18px', color: '#7e22ce', fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        // 3. Os 4 Atributos Verticais na Coluna Direita (SVG X=264 | Y=224, 267, 310, 353)
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

        this.lpStatEsp = this.add.text(espPt.x, espPt.y, '0', statStyle).setOrigin(1, 0.5).setResolution(2);
        this.lpStatAtq = this.add.text(atqPt.x, atqPt.y, '0', statStyle).setOrigin(1, 0.5).setResolution(2);
        this.lpStatDef = this.add.text(defPt.x, defPt.y, '0', statStyle).setOrigin(1, 0.5).setResolution(2);
        this.lpStatHp  = this.add.text(hpPt.x,  hpPt.y,  '0', {
            ...statStyle,
            fontSize: '20px',
            color: '#b91c1c'
        }).setOrigin(1, 0.5).setResolution(2);

        // 4. Caixa de Texto Inferior Esquerda
        this.lpTypeLine = this.add.text(0, 0, '', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '12px',
            color: '#4a3520',
            fontStyle: 'bold',
            align: 'left',
            wordWrap: { width: width * (194 / 280) }
        }).setOrigin(0, 0).setResolution(2);

        this.lpDesc = this.add.text(0, 0, '', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '13px',
            color: '#090d16',
            fontStyle: 'bold',
            align: 'left',
            lineSpacing: 3,
            wordWrap: { width: width * (194 / 280) }
        }).setOrigin(0, 0).setResolution(2);

        this.largePreviewContainer.add([
            this.lpShadow,
            this.lpArtBox,
            this.lpArtLabel,
            this.lpFrame,
            this.lpTitle,
            this.lpModBonus,
            this.lpModReducao,
            this.lpModVulnerabilidade,
            this.lpStatEsp,
            this.lpStatAtq,
            this.lpStatDef,
            this.lpStatHp,
            this.lpTypeLine,
            this.lpDesc
        ]);
    }

    formatHeroTraitsLine(heroDto) {
        const capitalize = (str) => {
            if (!str) return '';
            const clean = String(str).replace(/_/g, ' ').trim();
            return clean.charAt(0).toUpperCase() + clean.slice(1).toLowerCase();
        };

        const formatList = (val, prefix = '') => {
            if (!val) return [];
            const arr = Array.isArray(val) ? val : [val];
            return arr.filter(Boolean).map(v => `${prefix}${capitalize(v)}`);
        };

        const traits = [
            ...formatList(heroDto.classe),
            ...formatList(heroDto.ordem),
            ...formatList(heroDto.racas || heroDto.raca),
            ...formatList(heroDto.afinidades || heroDto.afinidade, 'Afinidade ')
        ];

        return traits.join(' - ');
    }

    showLargeCardPreview(cardOrHeroDto, isHero = false) {
        if (!cardOrHeroDto) return;
        const { width, height } = BOARD_LAYOUT.LARGE_CARD;

        const pos = (svgX, svgY) => ({
            x: ((svgX / 280) - 0.5) * width,
            y: ((svgY / 380) - 0.5) * height
        });

        const frameKey = isHero ? 'card_frame_hero' : 'card_frame_playable';
        if (this.textures.exists(frameKey)) {
            this.lpFrame.setTexture(frameKey).setDisplaySize(width, height);
        }

        this.lpTitle.setText(cardOrHeroDto.cardName || cardOrHeroDto.nome || '');

        if (isHero) {
            this.lpArtLabel.setText('⚔ ARTE DO HERÓI');

            const bonus = cardOrHeroDto.danoBonus ?? cardOrHeroDto.DanoBonus ?? 0;
            const reducao = cardOrHeroDto.reducaoDano ?? 0;
            const vulnerabilidade = cardOrHeroDto.vulnerabilidade ?? 0;

            this.lpModBonus.setText(String(bonus)).setVisible(true);
            this.lpModReducao.setText(String(reducao)).setVisible(true);
            this.lpModVulnerabilidade.setText(String(vulnerabilidade)).setVisible(true);

            this.lpStatEsp.setText(String(cardOrHeroDto.espirito ?? 0)).setVisible(true);
            this.lpStatAtq.setText(String(cardOrHeroDto.ataque ?? 0)).setVisible(true);
            this.lpStatDef.setText(String(cardOrHeroDto.defesa ?? 0)).setVisible(true);
            this.lpStatHp.setText(String(cardOrHeroDto.vidaAtual ?? cardOrHeroDto.vidaMaxima ?? 0)).setVisible(true);

            // Faixa de Atributos no topo da caixa esquerda (SVG X=14, Y=201)
            const traitsPt = pos(14, 201);
            const linhaAtributos = this.formatHeroTraitsLine(cardOrHeroDto);
            this.lpTypeLine
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
                .setText(linhaAtributos);

            // Descrição logo abaixo da divisória da caixa (SVG X=14, Y=238)
            const descPt = pos(14, 238);
            let fullDesc = cardOrHeroDto.descricao || '';
            const attached = cardOrHeroDto.attachedCards || [];
            if (attached.length > 0) {
                const itemsList = attached.map(it => `🗡️ ${it.cardName}`).join('\n');
                fullDesc += (fullDesc ? '\n\n' : '') + itemsList;
            }

            this.lpDesc
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

        } else {
            // Carta Jogável (Magia, Item, Ação, Teurgia)
            this.lpModBonus.setVisible(false);
            this.lpModReducao.setVisible(false);
            this.lpModVulnerabilidade.setVisible(false);
            this.lpStatEsp.setVisible(false);
            this.lpStatAtq.setVisible(false);
            this.lpStatDef.setVisible(false);
            this.lpStatHp.setVisible(false);

            const empTxt = (cardOrHeroDto.itemType === 'ARMA' && cardOrHeroDto.empunhadura > 0)
                ? ` • EMP: ${cardOrHeroDto.empunhadura}`
                : '';
            const subType = (cardOrHeroDto.itemType && cardOrHeroDto.itemType !== 'NENHUM')
                ? ` • ${cardOrHeroDto.itemType}`
                : '';

            this.lpArtLabel.setText(`✦ ARTE (${cardOrHeroDto.cardType})`);

            // Faixa amarela centralizada em SVG X=138, Y=208
            const typePt = pos(138, 208);
            this.lpTypeLine
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
                .setText(`${cardOrHeroDto.cardType}${subType}${empTxt}`);

            // Descrição na caixa inferior de largura total (SVG X=16, Y=230)
            const descPt = pos(16, 230);
            this.lpDesc
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
                .setText(cardOrHeroDto.descricao || '');
        }

        const artKey = cardOrHeroDto.imagem && this.textures.exists(cardOrHeroDto.imagem)
            ? cardOrHeroDto.imagem
            : (this.textures.exists(cardOrHeroDto.cardName) ? cardOrHeroDto.cardName : null);

        if (artKey) {
            if (!this.lpArtSprite) {
                this.lpArtSprite = this.add.image(this.lpArtBox.x, this.lpArtBox.y, artKey);
                this.lpArtSprite.texture.setFilter(Phaser.Textures.FilterMode.NEAREST);
                const frameIdx = this.largePreviewContainer.getIndex(this.lpFrame);
                this.largePreviewContainer.addAt(this.lpArtSprite, frameIdx);
            } else {
                this.lpArtSprite.setTexture(artKey).setVisible(true);
            }
            this.lpArtSprite.setDisplaySize(this.lpArtBox.width, this.lpArtBox.height);
            this.lpArtLabel.setVisible(false);
        } else {
            if (this.lpArtSprite) this.lpArtSprite.setVisible(false);
            this.lpArtLabel.setVisible(true);
        }

        this.largePreviewContainer.setVisible(true);
        this.largePreviewContainer.setScale(0.94);
        this.tweens.killTweensOf(this.largePreviewContainer);
        this.tweens.add({
            targets: this.largePreviewContainer,
            scaleX: 1.0,
            scaleY: 1.0,
            duration: 200,
            ease: 'Cubic.easeOut'
        });
    }

    hideLargeCardPreview() {
        if (this.largePreviewContainer) {
            this.largePreviewContainer.setVisible(false);
        }
    }

    canDragHero(heroPrefab) {
        if (this.isSettingsModalOpen || !this.currentSnapshot || this.snapshotPlayer.isPlaying || this.isSubmittingAction) return false;
        const isMyInitiative = String(this.currentSnapshot.activeTurnPlayerId) === String(this.localPlayerId);
        return this.currentSnapshot.turnPhase === 'CHOICE' && isMyInitiative && heroPrefab.heroData.estaVivo;
    }

    canDragHandCard() {
        if (this.isSettingsModalOpen || !this.currentSnapshot || this.snapshotPlayer.isPlaying || this.isSubmittingAction) return false;
        const phase = this.currentSnapshot.turnPhase;
        return phase === 'PREPARATION' || phase === 'DISCARD';
    }

    highlightTargetBattleSlot(isOpponent, active) {
        const targetRect = isOpponent ? this.oppBattleSlotRect : this.localBattleSlotRect;
        targetRect.setFillStyle(active ? 0x1e3a2f : BOARD_LAYOUT.COLORS.SLOT_ELEVATED, 1);
        targetRect.setStrokeStyle(0);
        if (!active) targetRect.setScale(1.0);
    }

    checkMagneticHoverOnBattleSlot(heroPrefab) {
        const targetSlot = heroPrefab.isOpponent
            ? BOARD_LAYOUT.BATTLE_SLOTS.OPPONENT_ACTIVE_HERO
            : BOARD_LAYOUT.BATTLE_SLOTS.LOCAL_ACTIVE_HERO;
        const targetRect = heroPrefab.isOpponent ? this.oppBattleSlotRect : this.localBattleSlotRect;
        const d = Phaser.Math.Distance.Between(heroPrefab.x, heroPrefab.y, targetSlot.x, targetSlot.y);
        targetRect.setScale(d < 240 ? 1.07 : 1.0);
    }

    handleHeroClickSnap(heroPrefab) {
        this.selectionManager.assignHeroSelection(heroPrefab, true);
        this.rearrangeHeroChoicePositions();
        this.updateMainButtonState();
    }

    handleHeroDrop(heroPrefab) {
        const targetSlot = heroPrefab.isOpponent
            ? BOARD_LAYOUT.BATTLE_SLOTS.OPPONENT_ACTIVE_HERO
            : BOARD_LAYOUT.BATTLE_SLOTS.LOCAL_ACTIVE_HERO;

        const dist = Phaser.Math.Distance.Between(heroPrefab.x, heroPrefab.y, targetSlot.x, targetSlot.y);

        if (dist < 280) {
            this.selectionManager.assignHeroSelection(heroPrefab, false);
        } else {
            if (!heroPrefab.isOpponent && this.selectionManager.selectedOwnHeroId === heroPrefab.uuid) {
                this.selectionManager.selectedOwnHeroId = null;
            } else if (heroPrefab.isOpponent && this.selectionManager.selectedEnemyHeroId === heroPrefab.uuid) {
                this.selectionManager.selectedEnemyHeroId = null;
            }
        }

        this.rearrangeHeroChoicePositions();
        this.updateMainButtonState();
    }

    rearrangeHeroChoicePositions() {
        this.heroPrefabs.forEach(prefab => {
            const isSelectedOwn = !prefab.isOpponent && prefab.uuid === this.selectionManager.selectedOwnHeroId;
            const isSelectedEnemy = prefab.isOpponent && prefab.uuid === this.selectionManager.selectedEnemyHeroId;

            if (isSelectedOwn) {
                const slot = BOARD_LAYOUT.BATTLE_SLOTS.LOCAL_ACTIVE_HERO;
                prefab.snapToPosition(slot.x, slot.y, BOARD_LAYOUT.COLORS.SNAP_VALID);
            } else if (isSelectedEnemy) {
                const slot = BOARD_LAYOUT.BATTLE_SLOTS.OPPONENT_ACTIVE_HERO;
                prefab.snapToPosition(slot.x, slot.y, BOARD_LAYOUT.COLORS.SNAP_VALID);
            } else {
                prefab.snapToPosition(prefab.homePos.x, prefab.homePos.y, null);
            }
        });
    }

    highlightValidDropZonesForHandCard(active) {
        const targetScale = active ? 1.15 : 1.0;

        // Slots de Resolução Aliados (#E60012)
        if (this.resolveSlotVisuals) {
            this.resolveSlotVisuals.forEach(slot => {
                if (slot.belongsToLocal) {
                    if (slot.rect) {
                        slot.rect.setFillStyle(active ? 0x1e3a2f : slot.baseFill, 1);
                        slot.rect.setStrokeStyle(0);
                    }
                    if (slot.graphics) {
                        this.tweens.killTweensOf(slot.graphics);
                        this.tweens.add({
                            targets: slot.graphics,
                            scaleX: targetScale,
                            scaleY: targetScale,
                            duration: active ? 160 : 120,
                            ease: active ? 'Back.easeOut' : 'Sine.easeOut'
                        });
                    }
                }
            });
        }

        // Slots de Bancada Aliada (#E60012)
        if (this.localBenchSlotVisuals) {
            this.localBenchSlotVisuals.forEach(slot => {
                if (slot.graphics) {
                    this.tweens.killTweensOf(slot.graphics);
                    this.tweens.add({
                        targets: slot.graphics,
                        scaleX: targetScale,
                        scaleY: targetScale,
                        duration: active ? 160 : 120,
                        ease: active ? 'Back.easeOut' : 'Sine.easeOut'
                    });
                }
            });
        }
    }

    checkMagneticHoverOnSlots(cardPrefab) {
        if (this.currentSnapshot?.turnPhase !== 'PREPARATION') return;

        this.resolveSlotVisuals.forEach(slot => {
            if (!slot.belongsToLocal) {
                slot.rect.setScale(1.0);
                if (slot.graphics) slot.graphics.setScale(1.0);
                return;
            }
            const d = Phaser.Math.Distance.Between(cardPrefab.x, cardPrefab.y, slot.x, slot.y);
            const targetScale = d < 190 ? 1.22 : 1.15;
            slot.rect.setScale(targetScale);
            if (slot.graphics) slot.graphics.setScale(targetScale);
        });
    }

    handleHandCardClickSnap(cardPrefab) {
        const phase = this.currentSnapshot?.turnPhase;
        const hasInitiative = String(this.currentSnapshot?.activeTurnPlayerId) === String(this.localPlayerId);

        if (phase === 'PREPARATION') {
            if (this.selectionManager.playedCardSlotMap.has(cardPrefab.uuid)) {
                this.selectionManager.removeCardFromResolve(cardPrefab.uuid);
            } else {
                this.selectionManager.tryAssignCardToResolveSlot(cardPrefab.uuid, null, hasInitiative);
            }
            this.rearrangePreparationCards();
        } else if (phase === 'DISCARD') {
            this.selectionManager.toggleDiscardCard(cardPrefab.uuid);
            this.rearrangeDiscardCards();
        }

        this.updateMainButtonState();
    }

    handleHandCardDrop(cardPrefab) {
        const phase = this.currentSnapshot?.turnPhase;
        const hasInitiative = String(this.currentSnapshot?.activeTurnPlayerId) === String(this.localPlayerId);

        this.resolveSlotVisuals.forEach(s => {
            s.rect.setScale(1.0);
            if (s.graphics) s.graphics.setScale(1.0);
        });
        if (this.localBenchSlotVisuals) {
            this.localBenchSlotVisuals.forEach(s => {
                if (s.graphics) s.graphics.setScale(1.0);
            });
        }

        if (phase === 'PREPARATION') {
            let bestRedSlot = null;
            let minDistance = Infinity;

            this.resolveSlotVisuals.forEach(slot => {
                if (slot.belongsToLocal) {
                    const dist = Phaser.Math.Distance.Between(cardPrefab.x, cardPrefab.y, slot.x, slot.y);
                    if (dist < minDistance) {
                        minDistance = dist;
                        bestRedSlot = slot;
                    }
                }
            });

            if (bestRedSlot && (minDistance < 320 || cardPrefab.y < 830)) {
                this.selectionManager.tryAssignCardToResolveSlot(cardPrefab.uuid, bestRedSlot.index, hasInitiative);
            } else {
                this.selectionManager.removeCardFromResolve(cardPrefab.uuid);
            }
            this.rearrangePreparationCards();

        } else if (phase === 'DISCARD') {
            if (cardPrefab.y < 915) {
                this.selectionManager.toggleDiscardCard(cardPrefab.uuid, true);
            } else {
                this.selectionManager.toggleDiscardCard(cardPrefab.uuid, false);
            }
            this.rearrangeDiscardCards();
        }

        this.updateMainButtonState();
    }

    rearrangePreparationCards() {
        const slotMap = this.selectionManager.playedCardSlotMap;

        this.handCardPrefabs.forEach(cardPrefab => {
            if (slotMap.has(cardPrefab.uuid)) {
                const slotIndex = slotMap.get(cardPrefab.uuid);
                const targetSlot = BOARD_LAYOUT.RESOLVE_SLOTS[slotIndex];
                cardPrefab.snapToPosition(targetSlot.x, targetSlot.y, BOARD_LAYOUT.COLORS.SNAP_VALID, 0, 'MEDIUM');
            } else {
                cardPrefab.snapToPosition(
                    cardPrefab.homePos.x,
                    cardPrefab.homePos.y,
                    BOARD_LAYOUT.COLORS.GOLD_DUST,
                    cardPrefab.homeAngle,
                    'COMPACT'
                );
            }
        });
    }

    rearrangeDiscardCards() {
        const selectedForDiscard = this.selectionManager.discardCardUuids;

        this.handCardPrefabs.forEach(cardPrefab => {
            const isSelected = selectedForDiscard.includes(cardPrefab.uuid);
            cardPrefab.setDiscardHighlightInHand(isSelected);
        });
    }

    renderSnapshot(snapshot, isLastInQueue = true, options = {}) {
        if (!snapshot) return;

        const previousPhase = options.previousPhase !== undefined
            ? options.previousPhase
            : (this.currentSnapshot ? this.currentSnapshot.turnPhase : null);
        const deferPhaseAnnouncement = Boolean(options.deferPhaseAnnouncement);

        this.currentSnapshot = snapshot;
        this.hideLargeCardPreview();

        if (previousPhase !== snapshot.turnPhase) {
            this.selectionManager.resetSelections();
            this.isWaitingOpponentConfirmation = false;
        }

        const localHasInitiative = String(snapshot.activeTurnPlayerId) === String(this.localPlayerId);

        // Se o anúncio da nova fase/turno NÃO foi adiado para depois dos VFX, atualiza o texto imediatamente
        if (!deferPhaseAnnouncement) {
            this.phaseBanner.setText(`FASE: ${snapshot.turnPhase}`);
        }

        this.updateInterleavedResolveSlots(localHasInitiative);

        if (snapshot.triggerContext?.logMessage) {
            this.combatLogText.setText(snapshot.triggerContext.logMessage);
        }

        const localPlayer = snapshot.localPlayer || {};
        const opponentPlayer = snapshot.opponentPlayer || {};

        const localPermElo = localPlayer.permanentElo ?? 1000;
        const localWeekElo = localPlayer.weeklyElo ?? 1000;
        this.localBannerContainer.nameTxt.setText(localPlayer.name || 'Jogador 1');
        this.localBannerContainer.roleTxt.setText(localHasInitiative ? '★ VOCÊ (COM INICIATIVA)' : 'VOCÊ');
        this.localBannerContainer.eloTxt.setText(`🏅 ELO: ${localPermElo}  |  📅 Semanal: ${localWeekElo}`);

        const oppPermElo = opponentPlayer.permanentElo ?? 1000;
        const oppWeekElo = opponentPlayer.weeklyElo ?? 1000;
        this.oppBannerContainer.nameTxt.setText(opponentPlayer.name || 'Jogador 2');
        this.oppBannerContainer.roleTxt.setText(!localHasInitiative ? '★ OPONENTE (COM INICIATIVA)' : 'OPONENTE');
        this.oppBannerContainer.eloTxt.setText(`🏅 ELO: ${oppPermElo}  |  📅 Semanal: ${oppWeekElo}`);

        this.localDeckText.setText(`${localPlayer.deckCount ?? 0}/30`);
        this.localDiscardText.setText(`🪦 ${(localPlayer.discardPile || []).length}`);
        this.oppDeckText.setText(`${opponentPlayer.deckCount ?? 0}/30`);
        this.oppDiscardText.setText(`🪦 ${(opponentPlayer.discardPile || []).length}`);

        this.syncHeroes(localPlayer.heroes || [], false, BOARD_LAYOUT.LOCAL_BENCH_SLOTS, BOARD_LAYOUT.BATTLE_SLOTS.LOCAL_ACTIVE_HERO);
        this.syncHeroes(opponentPlayer.heroes || [], true, BOARD_LAYOUT.OPPONENT_BENCH_SLOTS, BOARD_LAYOUT.BATTLE_SLOTS.OPPONENT_ACTIVE_HERO);

        this.syncLocalHand(localPlayer.hand || []);
        this.syncOpponentHand(opponentPlayer.handCount ?? 0);
        this.syncLiberationZones(
            localPlayer.liberationZone || [],
            opponentPlayer.liberationZone || []
        );
        if (['CHOICE', 'PREPARATION', 'DISCARD'].includes(snapshot.turnPhase)) {
            this.combatStartSpawnedOrder = [];
        }

        const trigger = snapshot.triggerContext;
        if (
            (snapshot.turnPhase === 'COMBAT_START' || snapshot.turnPhase === 'GAME_START') &&
            (trigger?.eventType === 'CARD_SPAWNED' || trigger?.eventType === 'TOKEN_SUMMONED') &&
            trigger?.spawnedCardUuid &&
            !this.combatStartSpawnedOrder.includes(trigger.spawnedCardUuid)
        ) {
            this.combatStartSpawnedOrder.push(trigger.spawnedCardUuid);
        }

        const eventType = trigger?.eventType;

        this.syncInterleavedResolveZone(
            localPlayer.playedCardsInResolve || [],
            opponentPlayer.playedCardsInResolve || [],
            localHasInitiative
        );
        if (eventType === 'CARD_SPAWNED' || eventType === 'TOKEN_SUMMONED') {
            this.handleCardSpawnEvent(snapshot.triggerContext, localHasInitiative);
        } else if (eventType === 'SPAWN_FAILED_ZONE_FULL') {
            this.handleSpawnFailedZoneFull(snapshot.triggerContext);
        } else if (eventType === 'MISSION_PROGRESS') {
            this.handleMissionProgressEvent(snapshot.triggerContext);
        } else if (eventType === 'CARD_LIBERATED') {
            this.handleCardLiberatedEvent(snapshot.triggerContext);
        } else if (eventType === 'HERO_EVOLVED') {
            this.handleHeroEvolvedEvent(snapshot.triggerContext);
        } else if (eventType === 'EFFECT_RESOLVED' && snapshot.triggerContext?.sourceCardUuid) {
            this.highlightSourceCard(snapshot.triggerContext.sourceCardUuid);
        }

        if (isLastInQueue) {
            this.checkAndShowPendingChoiceModal(snapshot);
        } else if (this.isChoiceModalActive) {
            this.isChoiceModalActive = false;
            this.modalContainer.setVisible(false);
        }

        this.updateMainButtonState();

        if (snapshot.turnPhase === 'GAME_OVER') {
            this.showGameOverModal(snapshot);
        }
    }

    /**
     * Chamado pelo SnapshotPlayerManager DEPOIS que os efeitos visuais (dano, cura, mortes, fim de combate)
     * do turno anterior já terminaram na tela.
     */
    triggerDeferredPhaseAnnouncement(snapshot, previousPhase) {
        if (!snapshot) return;

        const phase = snapshot.turnPhase;
        this.phaseBanner.setText(`FASE: ${phase}`);

        // Proteção contra faixas repetidas em intervalos muito curtos (< 900ms)
        const now = this.time.now;
        if (this.lastCutInTime && (now - this.lastCutInTime) < 900) {
            return;
        }
        this.lastCutInTime = now;

        const localHasInitiative = String(snapshot.activeTurnPlayerId) === String(this.localPlayerId);
        const initiativeOwner = localHasInitiative ? 'INICIATIVA: VOCÊ' : 'INICIATIVA: OPONENTE';

        // Mapeamento amigável e temático das fases principais
        const phaseDisplayConfig = {
            CHOICE:      { title: 'NOVO TURNO // ESCOLHA DE HERÓIS', sub: initiativeOwner, color: 0xe60012 },
            PREPARATION: { title: 'FASE DE PREPARAÇÃO',              sub: 'PROGRAME ATÉ 2 CARTAS NA PILHA', color: 0x2563eb },
            RESOLUTION:  { title: 'RESOLUÇÃO DE CARTAS',             sub: 'EXECUTANDO PILHA DE EFEITOS', color: 0xca8a04 },
            COMBAT:      { title: 'EMBATE FÍSICO',                   sub: 'COLISÃO DE HERÓIS ATIVOS', color: 0xb91c1c },
            DISCARD:     { title: 'FASE DE DESCARTE',                sub: 'RECICLAGEM DE MÃO E COMPRA', color: 0x475569 }
        };

        const cfg = phaseDisplayConfig[phase];
        if (!cfg) return;

        // Se você já importou o PhantomUIKit, use-o; caso contrário, este bloco já desenha a faixa P5+CRT nativa:
        this.playPhaseTransitionBanner(cfg.title, cfg.sub, cfg.color);
    }

    playPhaseTransitionBanner(titleText, subText, accentColor = 0xe60012) {
        const cutIn = this.add.container(960, 540).setDepth(280).setAngle(-6);

        const bgSlash = this.add.rectangle(0, 0, 2400, 130, 0x050507, 0.94)
            .setStrokeStyle(4, 0xf8fafc);
        const topAccent = this.add.rectangle(0, -46, 2400, 18, accentColor, 1);
        const botAccent = this.add.rectangle(0, 50, 2400, 8, 0xffe600, 1);

        const titleObj = this.add.text(-800, -6, `>> ${titleText} <<`, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '36px',
            color: '#ffffff',
            fontStyle: 'italic bold',
            stroke: '#000000',
            strokeThickness: 5
        }).setOrigin(0.5);

        const subObj = this.add.text(800, 30, `[SYS.PHASE_SYNC // ${subText}]`, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '14px',
            color: '#00f0ff',
            fontStyle: 'bold'
        }).setOrigin(0.5);

        cutIn.add([bgSlash, topAccent, botAccent, titleObj, subObj]);
        cutIn.setScale(1, 0);

        // 1. Abre a faixa (150ms)
        this.tweens.add({
            targets: cutIn,
            scaleY: 1,
            duration: 150,
            ease: 'Cubic.easeOut'
        });

        // 2. Desliza os textos com desaceleração suave no centro (700ms de leitura confortável)
        this.tweens.add({
            targets: titleObj,
            x: 0,
            duration: 220,
            ease: 'Expo.easeOut',
            onComplete: () => {
                this.tweens.add({
                    targets: titleObj,
                    x: 45,
                    duration: 550,
                    ease: 'Linear'
                });
            }
        });

        this.tweens.add({
            targets: subObj,
            x: 0,
            duration: 250,
            ease: 'Expo.easeOut'
        });

        // 3. Fecha a faixa (180ms)
        this.tweens.add({
            targets: cutIn,
            scaleY: 0,
            alpha: 0,
            delay: 820,
            duration: 180,
            ease: 'Cubic.easeIn',
            onComplete: () => cutIn.destroy()
        });
    }

    checkAndShowPendingChoiceModal(snapshot) {
        const pending = snapshot?.pendingChoice;
        if (!pending) {
            if (this.isChoiceModalActive) {
                this.isChoiceModalActive = false;
                this.modalContainer.setVisible(false);
            }
            return;
        }

        const isMyChoice = String(pending.choosingPlayerId) === String(this.localPlayerId);
        if (!isMyChoice) {
            if (this.isChoiceModalActive) {
                this.isChoiceModalActive = false;
                this.modalContainer.setVisible(false);
            }
            this.combatLogText.setText('⏳ Aguardando o oponente escolher um alvo de efeito...');
            return;
        }

        const validIds = pending.validTargetUuids || [];
        this.combatLogText.setText(`🎯 ${pending.promptMessage || 'Escolha um alvo para o efeito!'}`);

        const localPlayer = snapshot.localPlayer || {};
        const opponentPlayer = snapshot.opponentPlayer || {};

        const allCandidates = [
            ...(pending.candidateCards || []),
            ...(pending.validTargets || []),
            ...(pending.targetCards || []),
            ...(pending.cards || []),
            ...(pending.options || []),
            ...(localPlayer.hand || []),
            ...(localPlayer.playedCardsInResolve || []),
            ...(localPlayer.discardPile || []),
            ...(localPlayer.exilePile || []),
            ...(localPlayer.deck || []),
            ...(localPlayer.deckCards || []),
            ...(localPlayer.extraDeck || []),
            ...(opponentPlayer.playedCardsInResolve || []),
            ...(opponentPlayer.discardPile || []),
            ...(opponentPlayer.exilePile || []),
            ...(opponentPlayer.deck || []),
            ...(opponentPlayer.deckCards || []),
            ...(opponentPlayer.extraDeck || []),
            ...(localPlayer.heroes || []),
            ...(opponentPlayer.heroes || [])
        ];

        [...(localPlayer.heroes || []), ...(opponentPlayer.heroes || [])].forEach(h => {
            if (h && h.attachedCards && h.attachedCards.length > 0) {
                allCandidates.push(...h.attachedCards);
            }
        });

        const candidateByUuid = new Map();
        allCandidates.forEach(c => {
            if (c && c.uuid && !candidateByUuid.has(c.uuid)) {
                candidateByUuid.set(c.uuid, c);
            }
        });

        let matchingTargets = [];
        if (validIds.length > 0) {
            validIds.forEach((uuid, idx) => {
                if (candidateByUuid.has(uuid)) {
                    matchingTargets.push(candidateByUuid.get(uuid));
                } else {
                    matchingTargets.push({
                        uuid: uuid,
                        cardName: `Alvo #${idx + 1}`,
                        cardType: pending.targetType || 'ALVO',
                        color: 'NEUTRAL',
                        descricao: 'Clique para selecionar',
                        faceUp: true
                    });
                }
            });
        } else if (allCandidates.length > 0 && (pending.candidateCards || pending.validTargets || pending.cards || pending.options)) {
            matchingTargets = Array.from(candidateByUuid.values());
        }

        this.modalTitle.setText(`🎯 ${pending.promptMessage || 'ESCOLHA UM ALVO'}`);
        this.modalCardsGroup.removeAll(true);

        if (matchingTargets.length > 0) {
            const total = matchingTargets.length;
            const useCompactGrid = total > 12;
            const cols = useCompactGrid ? 8 : 6;
            const colSpacing = useCompactGrid ? 168 : 185;
            const rowSpacing = useCompactGrid ? 175 : 225;
            const startX = -((Math.min(total, cols) - 1) * colSpacing) / 2;
            const startY = useCompactGrid ? -185 : -110;
            const cardMode = useCompactGrid ? 'COMPACT' : 'MEDIUM';
            const cardDim = useCompactGrid ? BOARD_LAYOUT.COMPACT_CARD : BOARD_LAYOUT.MEDIUM_CARD;

            matchingTargets.forEach((itemDto, index) => {
                const col = index % cols;
                const row = Math.floor(index / cols);
                const x = startX + col * colSpacing;
                const y = startY + row * rowSpacing;

                const isHero = itemDto.vidaMaxima !== undefined;
                const cardView = isHero
                    ? new HeroCardPrefab(this, x, y, itemDto, false)
                    : new PlayableCardPrefab(this, x, y, { ...itemDto, faceUp: true }, false, cardMode, 0);

                if (isHero && useCompactGrid) {
                    cardView.setScale(0.78);
                }

                const clickZone = this.add.rectangle(x, y, cardDim.width, cardDim.height, 0x22c55e, 0.15)
                    .setStrokeStyle(3, 0x22c55e)
                    .setInteractive({ useHandCursor: true })
                    .on('pointerdown', () => this.submitEffectTargetChoice(itemDto.uuid));

                this.modalCardsGroup.add([cardView, clickZone]);
            });

            if (pending.canSkip) {
                const skipBtn = this.add.text(0, 295, '[ PULAR EFEITO ]', {
                    fontSize: '18px', color: '#fbbf24', backgroundColor: '#1e293b', padding: { x: 16, y: 8 }, fontStyle: 'bold'
                })
                    .setOrigin(0.5)
                    .setInteractive({ useHandCursor: true })
                    .on('pointerdown', () => this.submitEffectTargetChoice(null));
                this.modalCardsGroup.add(skipBtn);
            }

            this.isChoiceModalActive = true;
            this.modalContainer.setVisible(true);
        } else if (pending.canSkip) {
            this.submitEffectTargetChoice(null);
        } else {
            const emptyMsg = this.add.text(0, -20, 'Nenhum alvo válido disponível para este efeito.', {
                fontSize: '18px', color: '#e2e8f0', fontStyle: 'bold', align: 'center'
            }).setOrigin(0.5);

            const continueBtn = this.add.text(0, 60, '[ CONTINUAR ]', {
                fontSize: '18px', color: '#fbbf24', backgroundColor: '#1e293b', padding: { x: 18, y: 10 }, fontStyle: 'bold'
            })
                .setOrigin(0.5)
                .setInteractive({ useHandCursor: true })
                .on('pointerdown', () => this.submitEffectTargetChoice(null));

            this.modalCardsGroup.add([emptyMsg, continueBtn]);
            this.isChoiceModalActive = true;
            this.modalContainer.setVisible(true);
        }
    }

    syncHeroes(heroesDtoList, isOpponent, benchSlots, battleSlot) {
        heroesDtoList.forEach((heroDto, index) => {
            if (!heroDto || !heroDto.uuid) return;
            let prefab = this.heroPrefabs.get(heroDto.uuid);
            const homeSlot = benchSlots[index] || benchSlots[0];
            const targetPos = (heroDto.zoneId === 'BATTLE') ? battleSlot : homeSlot;

            if (!prefab) {
                prefab = new HeroCardPrefab(this, targetPos.x, targetPos.y, heroDto, isOpponent);
                prefab.homePos = { x: homeSlot.x, y: homeSlot.y };
                this.heroPrefabs.set(heroDto.uuid, prefab);
            } else {
                prefab.homePos = { x: homeSlot.x, y: homeSlot.y };
                prefab.updateFromSnapshot(heroDto);

                const moved = Phaser.Math.Distance.Between(prefab.x, prefab.y, targetPos.x, targetPos.y) > 2;
                if (moved) {
                    prefab.snapToPosition(targetPos.x, targetPos.y, null);
                }
            }
        });
    }

    syncLocalHand(handCardsDto = []) {
        const existingByUuid = new Map();
        this.handCardPrefabs.forEach(c => {
            if (c && c.uuid) existingByUuid.set(c.uuid, c);
        });

        const nextHandPrefabs = [];
        const total = handCardsDto.length;

        handCardsDto.forEach((cardDto, index) => {
            let slot;
            if (total <= BOARD_LAYOUT.LOCAL_HAND_SLOTS.length) {
                slot = BOARD_LAYOUT.LOCAL_HAND_SLOTS[index];
            } else {
                const centerX = 1035;
                const spacing = Math.max(128, Math.min(136, 780 / (total - 1)));
                const startX = centerX - ((total - 1) * spacing) / 2;
                const normOffset = (index - (total - 1) / 2) / Math.max(1, (total - 1) / 2);
                slot = {
                    x: startX + index * spacing,
                    y: 964 + Math.abs(normOffset) * 20,
                    angle: normOffset * 8
                };
            }

            const existing = cardDto.uuid ? existingByUuid.get(cardDto.uuid) : null;
            if (existing) {
                existingByUuid.delete(cardDto.uuid);
                existing.homePos = { x: slot.x, y: slot.y };
                existing.homeAngle = slot.angle;
                existing.updateCardVisuals({ ...cardDto, faceUp: true });
                existing.snapToPosition(slot.x, slot.y, null, slot.angle, 'COMPACT');
                nextHandPrefabs.push(existing);
            } else {
                const deckPos = BOARD_LAYOUT.PILES.LOCAL_DECK;
                const cardPrefab = new PlayableCardPrefab(
                    this,
                    deckPos.x,
                    deckPos.y,
                    { ...cardDto, faceUp: true },
                    true,
                    'COMPACT',
                    slot.angle
                );
                cardPrefab.homePos = { x: slot.x, y: slot.y };
                cardPrefab.homeAngle = slot.angle;
                cardPrefab.snapToPosition(slot.x, slot.y, null, slot.angle, 'COMPACT');
                nextHandPrefabs.push(cardPrefab);
            }
        });

        existingByUuid.forEach(leftover => leftover.destroy());
        this.handCardPrefabs = nextHandPrefabs;
    }

    syncOpponentHand(handCount = 0) {
        this.opponentHandDummies.forEach(c => c.destroy());
        this.opponentHandDummies = [];

        const count = Math.max(0, handCount);
        for (let i = 0; i < count; i++) {
            let slot;
            if (count <= BOARD_LAYOUT.OPPONENT_HAND_SLOTS.length) {
                slot = BOARD_LAYOUT.OPPONENT_HAND_SLOTS[i];
            } else {
                const centerX = 1035;
                const spacing = Math.max(126, Math.min(132, 740 / (count - 1)));
                const startX = centerX - ((count - 1) * spacing) / 2;
                const normOffset = (i - (count - 1) / 2) / Math.max(1, (count - 1) / 2);
                slot = {
                    x: startX + i * spacing,
                    y: -6 - Math.abs(normOffset) * 10,
                    angle: -normOffset * 8
                };
            }

            const hiddenCard = new PlayableCardPrefab(
                this,
                slot.x,
                slot.y,
                { faceUp: false },
                false,
                'COMPACT',
                slot.angle
            );
            this.opponentHandDummies.push(hiddenCard);
        }
    }

    syncInterleavedResolveZone(localPlayedDto = [], oppPlayedDto = [], localHasInitiative = true) {
        const existingByUuid = new Map();
        this.resolveCardPrefabs.forEach(c => {
            if (c && c.uuid) existingByUuid.set(c.uuid, c);
        });

        const nextResolvePrefabs = [];
        const firstPlayerCards = localHasInitiative ? localPlayedDto : oppPlayedDto;
        const secondPlayerCards = localHasInitiative ? oppPlayedDto : localPlayedDto;

        const trigger = this.currentSnapshot?.triggerContext;
        const isSpawnSnapshot = trigger && (
            trigger.eventType === 'CARD_SPAWNED' ||
            trigger.eventType === 'TOKEN_SUMMONED'
        );
        const currentSpawnedUuid = isSpawnSnapshot ? trigger.spawnedCardUuid : null;

        const placeOrReuseResolveCard = (cardDto, targetX, targetY) => {
            const existing = cardDto.uuid ? existingByUuid.get(cardDto.uuid) : null;

            if (existing) {
                existingByUuid.delete(cardDto.uuid);
                existing.targetPos = { x: targetX, y: targetY };
                existing.updateCardVisuals(cardDto);
                if (!this.tweens.isTweening(existing)) {
                    existing.snapToPosition(targetX, targetY, null, 0, 'MEDIUM');
                }
                nextResolvePrefabs.push(existing);
                return;
            }

            const isJustSpawned = Boolean(currentSpawnedUuid && cardDto.uuid === currentSpawnedUuid);
            const initialY = isJustSpawned ? targetY : (targetY - 24);

            const prefab = new PlayableCardPrefab(this, targetX, initialY, cardDto, false, 'MEDIUM', 0);
            prefab.targetPos = { x: targetX, y: targetY };

            if (this.spawnedCardPrefabs && cardDto.uuid) {
                this.spawnedCardPrefabs.set(cardDto.uuid, prefab);
            }

            prefab.setAlpha(0);
            if (!isJustSpawned) {
                this.tweens.add({
                    targets: prefab,
                    y: targetY,
                    alpha: 1,
                    duration: 240,
                    ease: 'Power2'
                });
            }

            nextResolvePrefabs.push(prefab);
        };

        const combatStartSet = new Set(this.combatStartSpawnedOrder || []);
        const allCardsMap = new Map();
        [...firstPlayerCards, ...secondPlayerCards].forEach(c => {
            if (c && c.uuid) allCardsMap.set(c.uuid, c);
        });

        // 1. Cartas invocadas em COMBAT_START ficam fixas no início da fila
        const combatStartCards = (this.combatStartSpawnedOrder || [])
            .map(uuid => allCardsMap.get(uuid))
            .filter(Boolean);

        // 2. Cartas de PREPARATION mantêm sua ordem original por jogador sem deslocar índices
        const firstPrep = firstPlayerCards.filter(c => c && !combatStartSet.has(c.uuid));
        const secondPrep = secondPlayerCards.filter(c => c && !combatStartSet.has(c.uuid));

        const hasExtraCards = combatStartCards.length > 0 || firstPrep.length > 2 || secondPrep.length > 2;

        if (!hasExtraCards) {
            const baseSlots = [
                firstPrep[0] || null,
                secondPrep[0] || null,
                firstPrep[1] || null,
                secondPrep[1] || null
            ];
            baseSlots.forEach((cardDto, slotIndex) => {
                if (cardDto) {
                    const pos = BOARD_LAYOUT.RESOLVE_SLOTS[slotIndex];
                    placeOrReuseResolveCard(cardDto, pos.x, pos.y);
                }
            });
        } else {
            const maxPrep = Math.max(firstPrep.length, secondPrep.length);
            const interleavedPrep = [];
            for (let i = 0; i < maxPrep; i++) {
                if (i < firstPrep.length && firstPrep[i]) interleavedPrep.push(firstPrep[i]);
                if (i < secondPrep.length && secondPrep[i]) interleavedPrep.push(secondPrep[i]);
            }

            const visualQueue = [...combatStartCards, ...interleavedPrep];

            if (visualQueue.length <= BOARD_LAYOUT.RESOLVE_SLOTS.length) {
                visualQueue.forEach((cardDto, idx) => {
                    const pos = BOARD_LAYOUT.RESOLVE_SLOTS[idx];
                    placeOrReuseResolveCard(cardDto, pos.x, pos.y);
                });
            } else {
                const centerX = BOARD_LAYOUT.ARENA_CIRCLE.x;
                const centerY = BOARD_LAYOUT.RESOLVE_SLOTS[0].y;
                const spacing = Math.max(150, Math.min(172, 840 / Math.max(1, visualQueue.length)));
                const startX = centerX - ((visualQueue.length - 1) * spacing) / 2;

                visualQueue.forEach((cardDto, idx) => {
                    const x = startX + idx * spacing;
                    placeOrReuseResolveCard(cardDto, x, centerY);
                });
            }
        }

        existingByUuid.forEach(leftover => {
            if (this.spawnedCardPrefabs && leftover.uuid) {
                this.spawnedCardPrefabs.delete(leftover.uuid);
            }
            leftover.destroy();
        });
        this.resolveCardPrefabs = nextResolvePrefabs;
    }

    handleCardSpawnEvent(triggerContext, localHasInitiative) {
        if (!triggerContext) return;

        const {
            sourceCardUuid,
            spawnedCardUuid,
            spawnedCardData,
            targetHeroId
        } = triggerContext;

        // 1. Determina a posição de origem (Herói invocador, Carta invocadora na pilha ou Centro da Arena)
        let originX = BOARD_LAYOUT.ARENA_CIRCLE.x;
        let originY = BOARD_LAYOUT.ARENA_CIRCLE.y;

        if (sourceCardUuid) {
            this.highlightSourceCard(sourceCardUuid);
            const sourceHero = this.heroPrefabs.get(sourceCardUuid);
            const sourceResolveCard = this.resolveCardPrefabs.find(c => c.uuid === sourceCardUuid);
            const sourceObj = sourceHero || sourceResolveCard;
            if (sourceObj) {
                originX = sourceObj.x;
                originY = sourceObj.y;
            }
        } else if (targetHeroId) {
            const targetHero = this.heroPrefabs.get(targetHeroId);
            if (targetHero) {
                originX = targetHero.x;
                originY = targetHero.y;
            }
        }

        // 2. Localiza a carta invocada que foi alocada na pilha de resolução (resolveCardPrefabs)
        let spawnedPrefab = this.resolveCardPrefabs.find(c => c.uuid === spawnedCardUuid);

        // 3. Fallback caso tenha sido invocada fora da pilha
        if (!spawnedPrefab && spawnedCardData) {
            const targetHeroPrefab = targetHeroId ? this.heroPrefabs.get(targetHeroId) : null;
            const destX = targetHeroPrefab ? targetHeroPrefab.x : BOARD_LAYOUT.ARENA_CIRCLE.x;
            const destY = targetHeroPrefab ? targetHeroPrefab.y : BOARD_LAYOUT.ARENA_CIRCLE.y;

            spawnedPrefab = new PlayableCardPrefab(
                this,
                originX,
                originY,
                { ...spawnedCardData, faceUp: true },
                false,
                'MEDIUM',
                0
            );
            spawnedPrefab.targetPos = { x: destX, y: destY };
            this.spawnedCardPrefabs.set(spawnedPrefab.uuid, spawnedPrefab);
            this.resolveCardPrefabs.push(spawnedPrefab);
        }

        // 4. Anima a entrada da carta invocada para o seu slot na pilha de resolução e a mantém visível!
        if (spawnedPrefab) {
            const finalX = spawnedPrefab.targetPos ? spawnedPrefab.targetPos.x : spawnedPrefab.x;
            const finalY = spawnedPrefab.targetPos ? spawnedPrefab.targetPos.y : spawnedPrefab.y;

            this.spawnedCardPrefabs.set(spawnedPrefab.uuid, spawnedPrefab);
            this.tweens.killTweensOf(spawnedPrefab);

            spawnedPrefab.setPosition(originX, originY);
            spawnedPrefab.setScale(0.25);
            spawnedPrefab.setAlpha(0);
            spawnedPrefab.setDepth(145);
            spawnedPrefab.bg.setStrokeStyle(4, 0x38bdf8);

            this.tweens.add({
                targets: spawnedPrefab,
                x: finalX,
                y: finalY,
                scaleX: 1.0,
                scaleY: 1.0,
                alpha: 1,
                duration: 420,
                ease: 'Back.easeOut',
                onComplete: () => {
                    spawnedPrefab.setDepth(25);
                }
            });
        }
    }

    handleSpawnFailedZoneFull(triggerContext) {
        const msg = triggerContext?.logMessage || 'Zona cheia! A carta invocada foi enviada ao Descarte.';
        this.showToast(msg);
        if (triggerContext?.targetHeroId) {
            const hero = this.heroPrefabs.get(triggerContext.targetHeroId);
            if (hero) {
                hero.showFloatingNumber('SLOTS CHEIOS!', '#ef4444');
            }
        }
    }

    highlightSourceCard(sourceUuid) {
        const hero = this.heroPrefabs.get(sourceUuid);
        if (hero) {
            this.tweens.killTweensOf(hero);
            this.tweens.add({ targets: hero, scaleX: 1.16, scaleY: 1.16, yoyo: true, duration: 240 });
            return;
        }
        const resolveCard = this.resolveCardPrefabs.find(c => c.uuid === sourceUuid);
        if (resolveCard) {
            resolveCard.pulseSourceEffect();
        }
    }

    updateMainButtonState() {
        if (!this.currentSnapshot) return;

        const phase = this.currentSnapshot.turnPhase;
        const isMyInitiative = String(this.currentSnapshot.activeTurnPlayerId) === String(this.localPlayerId);

        if (this.snapshotPlayer.isPlaying || this.isSubmittingAction) {
            this.mainButtonLabel.setText('RESOLVENDO...');
            this.mainButtonBg.setTint(0x64748b);
            this.mainButtonBg.input.enabled = false;
            return;
        }

        if (this.currentSnapshot.pendingChoice) {
            const isMyChoice = String(this.currentSnapshot.pendingChoice.choosingPlayerId) === String(this.localPlayerId);
            this.mainButtonLabel.setText(isMyChoice ? 'ESCOLHA UM ALVO NO MODAL' : 'OPONENTE ESCOLHENDO ALVO...');
            this.mainButtonBg.setTint(0xfbbf24);
            this.mainButtonBg.input.enabled = isMyChoice;
            return;
        }

        if (this.isWaitingOpponentConfirmation) {
            this.mainButtonLabel.setText('AGUARDANDO OPONENTE...');
            this.mainButtonBg.setTint(0x64748b);
            this.mainButtonBg.input.enabled = false;
            return;
        }

        const discardCount = this.selectionManager.discardCardUuids.length;
        const prepCount = this.selectionManager.playedCardSlotMap.size;

        const labels = {
            CHOICE: isMyInitiative ? 'CONFIRMAR HERÓIS' : 'OPONENTE ESCOLHENDO...',
            PREPARATION: prepCount === 0
                ? 'PASSAR PREPARAÇÃO (0/2)'
                : `CONFIRMAR PREPARAÇÃO (${prepCount}/2)`,
            COMBAT_START: 'INÍCIO DO COMBATE...',
            RESOLUTION: 'RESOLVENDO EFEITOS...',
            COMBAT: 'COMBATE FÍSICO...',
            COMBAT_END: 'FIM DO COMBATE...',
            DISCARD: discardCount === 0
                ? 'PASSAR SEM DESCARTAR (0)'
                : `DESCARTAR ${discardCount} CARTA(S)`,
            GAME_OVER: 'FIM DE JOGO!'
        };

        const isAutomaticPhase = ['COMBAT_START', 'RESOLUTION', 'COMBAT', 'COMBAT_END', 'GAME_OVER'].includes(phase);
        const canClick = !isAutomaticPhase && this.selectionManager.canConfirmAction(phase, isMyInitiative);

        this.mainButtonLabel.setText(labels[phase] || 'EM COMBATE');
        if (canClick) {
            this.mainButtonBg.clearTint();
        } else {
            this.mainButtonBg.setTint(0x64748b);
        }
        this.mainButtonBg.input.enabled = canClick;
    }

    async onMainButtonClicked() {
        if (!this.currentSnapshot || this.isSubmittingAction) return;

        // Se o jogador clicou no botão principal enquanto há uma escolha pendente sua, reabre o modal de escolha
        if (this.currentSnapshot.pendingChoice) {
            const isMyChoice = String(this.currentSnapshot.pendingChoice.choosingPlayerId) === String(this.localPlayerId);
            if (isMyChoice) {
                this.checkAndShowPendingChoiceModal(this.currentSnapshot);
            }
            return;
        }

        const phase = this.currentSnapshot.turnPhase;
        const payload = this.selectionManager.buildActionRequestDTO(this.matchId, this.localPlayerId, phase);

        this.isSubmittingAction = true;
        this.mainButtonBg.setTint(0x64748b);
        this.mainButtonBg.input.enabled = false;
        this.mainButtonLabel.setText('ENVIANDO...');

        try {
            const response = await fetch(`http://localhost:8080/api/matches/${this.matchId}/confirm-action`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            this.isSubmittingAction = false;

            if (response.ok) {
                const timelineDto = await response.json();
                this.isWaitingOpponentConfirmation = Boolean(timelineDto.waitingForOpponent);
                const snaps = timelineDto.snapshots || [];
                if (snaps.length > 0) {
                    this.snapshotPlayer.enqueueSnapshots(snaps);
                } else {
                    this.updateMainButtonState();
                    this.pollServerStateIfWaiting(true);
                }
            } else {
                this.showToast('Jogada recusada pelo servidor!');
                this.updateMainButtonState();
            }
        } catch (err) {
            this.isSubmittingAction = false;
            this.isWaitingOpponentConfirmation = false;
            console.warn('Servidor offline. Executando simulação local de Snapshots...');
            const { generateOfflineMockTimeline } = await import('../config/mockSnapshots.js');
            const simulatedSnapshots = generateOfflineMockTimeline(this.currentSnapshot, payload);
            this.snapshotPlayer.enqueueSnapshots(simulatedSnapshots);
        }
    }

    createDiscardInspectionModal() {
        this.modalContainer = this.add.container(960, 540).setDepth(220).setVisible(false);
        const overlay = this.add.rectangle(0, 0, 1520, 740, 0x090d16, 0.96).setStrokeStyle(3, BOARD_LAYOUT.COLORS.GOLD_DUST);
        this.modalTitle = this.add.text(0, -330, 'CEMITÉRIO (DESCARTE)', { fontSize: '22px', color: '#ffd700', fontStyle: 'bold' }).setOrigin(0.5);

        const closeBtn = this.add.text(680, -330, '[FECHAR X]', { fontSize: '18px', color: '#ff4444', fontStyle: 'bold' })
            .setOrigin(0.5)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => {
                this.isChoiceModalActive = false;
                this.modalContainer.setVisible(false);
            });

        this.modalCardsGroup = this.add.container(0, 0);
        this.modalContainer.add([overlay, this.modalTitle, closeBtn, this.modalCardsGroup]);
    }

    openDiscardModal(title, discardCardsList = []) {
        if (this.isChoiceModalActive && this.currentSnapshot?.pendingChoice) {
            return;
        }

        this.isChoiceModalActive = false;
        this.modalTitle.setText(`${title} (${discardCardsList.length} cartas)`);
        this.modalCardsGroup.removeAll(true);

        const total = discardCardsList.length;
        const useCompact = total > 14;
        const cols = useCompact ? 9 : 7;
        const colSpacing = useCompact ? 156 : 180;
        const rowSpacing = useCompact ? 175 : 225;
        const startX = -((Math.min(total, cols) - 1) * colSpacing) / 2;
        const startY = useCompact ? -185 : -130;
        const cardMode = useCompact ? 'COMPACT' : 'MEDIUM';

        discardCardsList.forEach((cardDto, index) => {
            const col = index % cols;
            const row = Math.floor(index / cols);
            const x = startX + col * colSpacing;
            const y = startY + row * rowSpacing;
            const card = new PlayableCardPrefab(this, x, y, { ...cardDto, faceUp: true }, false, cardMode, 0);
            this.modalCardsGroup.add(card);
        });

        this.modalContainer.setVisible(true);
    }

    showToast(message) {
        const toast = this.add.text(1035, 450, message, {
            fontSize: '16px', color: '#ffffff', backgroundColor: '#b91c1c', padding: { x: 14, y: 8 }, fontStyle: 'bold'
        }).setOrigin(0.5).setDepth(250);

        this.tweens.add({
            targets: toast,
            y: 390,
            alpha: 0,
            delay: 1800,
            duration: 650,
            ease: 'Cubic.easeOut',
            onComplete: () => toast.destroy()
        });
    }

    async pollServerStateIfWaiting(force = false) {
        if (!this.currentSnapshot || this.snapshotPlayer.isPlaying || this.isSubmittingAction || this.isPolling) return;

        const phase = this.currentSnapshot.turnPhase;
        const isMyInitiative = String(this.currentSnapshot.activeTurnPlayerId) === String(this.localPlayerId);
        const waitingMyChoice = Boolean(
            this.currentSnapshot.pendingChoice &&
            String(this.currentSnapshot.pendingChoice.choosingPlayerId) === String(this.localPlayerId)
        );
        const waitingOtherPlayerChoice = Boolean(
            this.currentSnapshot.pendingChoice &&
            String(this.currentSnapshot.pendingChoice.choosingPlayerId) !== String(this.localPlayerId)
        );
        const isAutomaticPhase = ['COMBAT_START', 'RESOLUTION', 'COMBAT', 'COMBAT_END'].includes(phase);

        const shouldPoll =
            force ||
            (phase === 'CHOICE' && !isMyInitiative) ||
            this.isWaitingOpponentConfirmation ||
            waitingOtherPlayerChoice ||
            (isAutomaticPhase && !waitingMyChoice);

        if (!shouldPoll) return;

        this.isPolling = true;
        try {
            const sinceSeq = this.snapshotPlayer.lastPlayedSequence || this.currentSnapshot.snapshotSequence || 0;
            const res = await fetch(
                `http://localhost:8080/api/matches/${this.matchId}/state?viewerId=${this.localPlayerId}&sinceSequence=${sinceSeq}`
            );
            this.isPolling = false;
            if (!res.ok) return;

            const timelineDto = await res.json();
            const newSnaps = timelineDto.snapshots || [];

            if (newSnaps.length > 0 && !this.isSubmittingAction) {
                this.isWaitingOpponentConfirmation = Boolean(timelineDto.waitingForOpponent);
                this.snapshotPlayer.enqueueSnapshots(newSnaps);
            }
        } catch (e) {
            this.isPolling = false;
            // Silencioso no modo offline
        }
    }

    async submitEffectTargetChoice(selectedTargetUuid) {
        if (!this.currentSnapshot?.pendingChoice || this.isSubmittingAction) return;

        const previousPendingChoice = this.currentSnapshot.pendingChoice;
        this.currentSnapshot.pendingChoice = null;
        this.isSubmittingAction = true;
        this.isChoiceModalActive = false;
        this.modalContainer.setVisible(false);
        this.updateMainButtonState();

        try {
            const response = await fetch(`http://localhost:8080/api/matches/${this.matchId}/confirm-action`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    matchId: this.matchId,
                    playerId: this.localPlayerId,
                    currentPhase: this.currentSnapshot.turnPhase,
                    phase: this.currentSnapshot.turnPhase,
                    selectedTargetUuid: selectedTargetUuid
                })
            });

            this.isSubmittingAction = false;
            if (response.ok) {
                const timelineDto = await response.json();
                this.isWaitingOpponentConfirmation = Boolean(timelineDto.waitingForOpponent);
                const snaps = timelineDto.snapshots || [];
                if (snaps.length > 0) {
                    this.snapshotPlayer.enqueueSnapshots(snaps);
                } else {
                    this.updateMainButtonState();
                    this.pollServerStateIfWaiting(true);
                }
            } else {
                this.currentSnapshot.pendingChoice = previousPendingChoice;
                this.showToast('Alvo recusado pelo servidor!');
                this.checkAndShowPendingChoiceModal(this.currentSnapshot);
                this.updateMainButtonState();
            }
        } catch (e) {
            this.isSubmittingAction = false;
            this.currentSnapshot.pendingChoice = previousPendingChoice;
            console.error('Erro ao enviar escolha de alvo:', e);
            this.checkAndShowPendingChoiceModal(this.currentSnapshot);
            this.updateMainButtonState();
        }
    }

    createSettingsGearAndModal() {
        const { COLORS } = BOARD_LAYOUT;

        // 1. Botão de Engrenagem no canto superior direito
        this.gearBtnContainer = this.add.container(425, 72).setDepth(210);
        const gearBg = this.add.rectangle(0, 0, 54, 54, COLORS.ZONE_SURFACE, 0.95)
            .setStrokeStyle(2, COLORS.GOLD_DUST)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => this.toggleSettingsModal());

        const gearIcon = this.add.text(0, 0, '⚙️', { fontSize: '26px' }).setOrigin(0.5);
        this.gearBtnContainer.add([gearBg, gearIcon]);

        // 2. Overlay e Container Principal do Modal de Configurações (Pausa Visual)
        this.settingsModalContainer = this.add.container(960, 540).setDepth(260).setVisible(false);

        const backdrop = this.add.rectangle(0, 0, 1920, 1080, 0x000000, 0.65)
            .setInteractive(); // Bloqueia cliques nos elementos de baixo sem pausar a rede

        const panel = this.add.rectangle(0, 0, 560, 470, 0x0f172a, 0.98)
            .setStrokeStyle(3, COLORS.GOLD_DUST);

        const title = this.add.text(0, -185, '⚙️ CONFIGURAÇÕES DA PARTIDA', {
            fontSize: '22px', color: '#ffd700', fontStyle: 'bold', fontFamily: 'monospace'
        }).setOrigin(0.5);

        const closeXBtn = this.add.text(245, -185, '✖', {
            fontSize: '20px', color: '#94a3b8', fontStyle: 'bold'
        })
            .setOrigin(0.5)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => this.toggleSettingsModal(false));

        // 3. Controles de Áudio (Música e Efeitos)
        const musicControl = this.createVolumeControlRow(-95, '🎵 Volume da Música', this.musicVolume, (newVal) => {
            this.musicVolume = newVal;
            localStorage.setItem('ldp_music_volume', String(newVal));
            this.applyAudioSettings();
        });

        const sfxControl = this.createVolumeControlRow(-15, '🔊 Efeitos Sonoros (SFX)', this.sfxVolume, (newVal) => {
            this.sfxVolume = newVal;
            localStorage.setItem('ldp_sfx_volume', String(newVal));
            this.applyAudioSettings();
        });

        // 4. Visão Padrão dos Botões (Continuar / Desistir)
        this.settingsMainButtonsGroup = this.add.container(0, 105);

        const resumeBtnBg = this.add.rectangle(0, -20, 340, 50, 0x16a34a)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => this.toggleSettingsModal(false));
        const resumeBtnTxt = this.add.text(0, -20, 'VOLTAR À PARTIDA', {
            fontSize: '16px', color: '#ffffff', fontStyle: 'bold'
        }).setOrigin(0.5);

        const forfeitBtnBg = this.add.rectangle(0, 50, 340, 50, 0xb91c1c)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => this.showForfeitConfirmation(true));
        const forfeitBtnTxt = this.add.text(0, 50, '🏳️ SAIR DA PARTIDA / DESISTIR', {
            fontSize: '16px', color: '#ffffff', fontStyle: 'bold'
        }).setOrigin(0.5);

        this.settingsMainButtonsGroup.add([resumeBtnBg, resumeBtnTxt, forfeitBtnBg, forfeitBtnTxt]);

        // 5. Sub-painel de Confirmação ("Tem certeza que deseja sair?")
        this.forfeitConfirmGroup = this.add.container(0, 105).setVisible(false);

        const confirmPrompt = this.add.text(0, -32, 'Tem certeza que deseja sair?\nIsso encerrará a partida (derrota por desistência).', {
            fontSize: '14.5px', color: '#fca5a5', fontStyle: 'bold', align: 'center'
        }).setOrigin(0.5);

        const confirmYesBg = this.add.rectangle(-95, 42, 165, 48, 0xdc2626)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => this.confirmForfeitAndLeave());
        const confirmYesTxt = this.add.text(-95, 42, 'SIM, SAIR', {
            fontSize: '15px', color: '#ffffff', fontStyle: 'bold'
        }).setOrigin(0.5);

        const confirmNoBg = this.add.rectangle(95, 42, 165, 48, 0x475569)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => this.showForfeitConfirmation(false));
        const confirmNoTxt = this.add.text(95, 42, 'CANCELAR', {
            fontSize: '15px', color: '#ffffff', fontStyle: 'bold'
        }).setOrigin(0.5);

        this.forfeitConfirmGroup.add([confirmPrompt, confirmYesBg, confirmYesTxt, confirmNoBg, confirmNoTxt]);

        this.settingsModalContainer.add([
            backdrop, panel, title, closeXBtn,
            ...musicControl, ...sfxControl,
            this.settingsMainButtonsGroup,
            this.forfeitConfirmGroup
        ]);

        this.applyAudioSettings();
    }

    createVolumeControlRow(yPos, labelText, initialValue, onChange) {
        const label = this.add.text(-220, yPos - 18, labelText, {
            fontSize: '15px', color: '#e2e8f0', fontStyle: 'bold'
        }).setOrigin(0, 0.5);

        const pctText = this.add.text(220, yPos - 18, `${Math.round(initialValue * 100)}%`, {
            fontSize: '15px', color: '#ffd700', fontStyle: 'bold'
        }).setOrigin(1, 0.5);

        const barWidth = 340;
        const trackBg = this.add.rectangle(0, yPos + 14, barWidth, 14, 0x334155)
            .setInteractive({ useHandCursor: true });

        const fillRect = this.add.rectangle(-barWidth / 2, yPos + 14, barWidth * initialValue, 14, 0x38bdf8)
            .setOrigin(0, 0.5);

        let currentValue = initialValue;
        const updateVisual = (val) => {
            currentValue = Phaser.Math.Clamp(Math.round(val * 10) / 10, 0, 1);
            fillRect.width = barWidth * currentValue;
            pctText.setText(`${Math.round(currentValue * 100)}%`);
            onChange(currentValue);
        };

        trackBg.on('pointerdown', (pointer) => {
            const localX = pointer.x - (960 - barWidth / 2);
            updateVisual(localX / barWidth);
        });

        const minusBtn = this.add.text(-205, yPos + 14, '[-]', {
            fontSize: '16px', color: '#94a3b8', fontStyle: 'bold'
        }).setOrigin(0.5).setInteractive({ useHandCursor: true })
            .on('pointerdown', () => updateVisual(currentValue - 0.1));

        const plusBtn = this.add.text(205, yPos + 14, '[+]', {
            fontSize: '16px', color: '#94a3b8', fontStyle: 'bold'
        }).setOrigin(0.5).setInteractive({ useHandCursor: true })
            .on('pointerdown', () => updateVisual(currentValue + 0.1));

        return [label, pctText, trackBg, fillRect, minusBtn, plusBtn];
    }

    applyAudioSettings() {
        if (!this.sound) return;
        this.sound.volume = this.sfxVolume;
        if (this.sound.sounds) {
            this.sound.sounds.forEach(snd => {
                const isMusic = snd.key && snd.key.toLowerCase().includes('music');
                snd.setVolume(isMusic ? this.musicVolume : this.sfxVolume);
            });
        }
    }

    toggleSettingsModal(forceState) {
        const nextState = typeof forceState === 'boolean' ? forceState : !this.isSettingsModalOpen;
        this.isSettingsModalOpen = nextState;
        this.showForfeitConfirmation(false);
        this.settingsModalContainer.setVisible(nextState);
        this.hideLargeCardPreview();
    }

    showForfeitConfirmation(showConfirm) {
        if (this.settingsMainButtonsGroup) this.settingsMainButtonsGroup.setVisible(!showConfirm);
        if (this.forfeitConfirmGroup) this.forfeitConfirmGroup.setVisible(showConfirm);
    }

    async confirmForfeitAndLeave() {
        if (this.isSubmittingAction) return;
        this.isSubmittingAction = true;

        try {
            if (this.matchId) {
                await fetch(`http://localhost:8080/api/matches/${this.matchId}/forfeit`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        matchId: this.matchId,
                        playerId: this.localPlayerId
                    })
                });
            }
        } catch (err) {
            console.warn('Aviso ao notificar saída da partida:', err);
        } finally {
            this.isSubmittingAction = false;
            this.cleanupAndReturnToMenu();
        }
    }

    createGameOverModal() {
        const { COLORS } = BOARD_LAYOUT;

        this.gameOverModalContainer = this.add.container(960, 540).setDepth(270).setVisible(false);

        const overlay = this.add.rectangle(0, 0, 1920, 1080, 0x030712, 0.88).setInteractive();

        const mainPanel = this.add.rectangle(0, 0, 920, 620, 0x0f172a, 0.98)
            .setStrokeStyle(4, COLORS.GOLD_DUST);

        this.goHeaderBanner = this.add.rectangle(0, -245, 912, 86, 0x14532d, 0.45);

        this.gameOverTitleText = this.add.text(0, -258, '🏆 VITÓRIA NAS AREIAS!', {
            fontSize: '34px', color: '#4ade80', fontStyle: 'bold', fontFamily: 'monospace'
        }).setOrigin(0.5);

        this.gameOverSubtitleText = this.add.text(0, -220, '', {
            fontSize: '15px', color: '#e2e8f0', align: 'center', fontFamily: 'monospace'
        }).setOrigin(0.5);

        this.goTurnsBadge = this.add.text(0, -172, '⏳ Turnos Decorridos: 1', {
            fontSize: '15px', color: '#ffd700', fontStyle: 'bold', fontFamily: 'monospace'
        }).setOrigin(0.5);

        // Container onde os 2 cards comparativos (Você vs Oponente) serão desenhados dinamicamente
        this.goStatsCardsGroup = this.add.container(0, -10);

        // Botão "JOGAR NOVAMENTE"
        this.playAgainBtnBg = this.add.rectangle(-165, 238, 290, 58, 0x16a34a)
            .setStrokeStyle(2, 0x86efac)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => this.onPlayAgainClicked());

        this.playAgainBtnTxt = this.add.text(-165, 238, '⚔️ JOGAR NOVAMENTE', {
            fontSize: '17px', color: '#ffffff', fontStyle: 'bold', fontFamily: 'monospace'
        }).setOrigin(0.5);

        // Botão "MENU PRINCIPAL"
        const menuBtnBg = this.add.rectangle(165, 238, 290, 58, 0x334155)
            .setStrokeStyle(2, 0x94a3b8)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => this.cleanupAndReturnToMenu());

        const menuBtnTxt = this.add.text(165, 238, '🏠 MENU PRINCIPAL', {
            fontSize: '17px', color: '#ffffff', fontStyle: 'bold', fontFamily: 'monospace'
        }).setOrigin(0.5);

        this.gameOverModalContainer.add([
            overlay, mainPanel, this.goHeaderBanner,
            this.gameOverTitleText, this.gameOverSubtitleText, this.goTurnsBadge,
            this.goStatsCardsGroup,
            this.playAgainBtnBg, this.playAgainBtnTxt,
            menuBtnBg, menuBtnTxt
        ]);
    }

    showGameOverModal(snapshot) {
        if (!this.gameOverModalContainer || this.gameOverModalContainer.visible) return;

        this.isSettingsModalOpen = false;
        if (this.settingsModalContainer) this.settingsModalContainer.setVisible(false);
        if (this.modalContainer) this.modalContainer.setVisible(false);

        const result = snapshot.matchResult || {};
        const winnerId = result.winnerPlayerId || snapshot.winnerPlayerId;
        const isLocalWinner = winnerId && String(winnerId) === String(this.localPlayerId);
        const winReason = result.winReason || snapshot.winReason || 'ELIMINATION';
        const isForfeit = winReason === 'FORFEIT' || snapshot.triggerContext?.eventType === 'MATCH_FORFEITED';

        if (isLocalWinner) {
            this.goHeaderBanner.setFillStyle(0x14532d, 0.55);
            this.gameOverTitleText.setText('🏆 VITÓRIA GLORIOSA!').setColor('#4ade80');
            this.gameOverSubtitleText.setText(
                isForfeit
                    ? 'Seu oponente bateu em retirada! Vitória por W.O.'
                    : 'Todos os Heróis inimigos tombaram perante sua estratégia!'
            );
        } else {
            this.goHeaderBanner.setFillStyle(0x7f1d1d, 0.55);
            this.gameOverTitleText.setText('💀 DERROTA NAS POEIRAS').setColor('#f87171');
            this.gameOverSubtitleText.setText(
                isForfeit
                    ? 'Partida encerrada por desistência.'
                    : 'Seus Heróis foram derrotados em combate.'
            );
        }

        const turns = result.turnsElapsed || 1;
        this.goTurnsBadge.setText(`⏳ TURNOS DECORRIDOS: ${turns}   •   MOTIVO: ${isForfeit ? 'DESISTÊNCIA (W.O.)' : 'ELIMINAÇÃO'}`);

        const myStats = result.player1Stats || {
            playerName: snapshot.localPlayer?.name || 'Você',
            winner: isLocalWinner,
            totalDamageDealt: 0,
            totalDamageTaken: 0,
            totalHealingDone: 0,
            cardsPlayed: 0,
            heroesDefeated: 0,
            heroesAlive: (snapshot.localPlayer?.heroes || []).filter(h => h.estaVivo).length,
            matchScore: 0
        };

        const oppStats = result.player2Stats || {
            playerName: snapshot.opponentPlayer?.name || 'Oponente',
            winner: !isLocalWinner,
            totalDamageDealt: 0,
            totalDamageTaken: 0,
            totalHealingDone: 0,
            cardsPlayed: 0,
            heroesDefeated: 0,
            heroesAlive: (snapshot.opponentPlayer?.heroes || []).filter(h => h.estaVivo).length,
            matchScore: 0
        };

        if (myStats.permanentElo && myStats.weeklyElo) {
            try {
                const stored = JSON.parse(localStorage.getItem('lendas_player')) || {};
                stored.permanentElo = myStats.permanentElo;
                stored.weeklyElo = myStats.weeklyElo;
                localStorage.setItem('lendas_player', JSON.stringify(stored));
            } catch (e) {
                console.warn('Não foi possível atualizar ELO local:', e);
            }
        }

        this.goStatsCardsGroup.removeAll(true);
        const localCard = this.createPlayerStatsSummaryCard(-215, 0, 'SUAS MÉTRICAS', myStats, true);
        const oppCard = this.createPlayerStatsSummaryCard(215, 0, 'MÉTRICAS DO OPONENTE', oppStats, false);
        this.goStatsCardsGroup.add([localCard, oppCard]);

        // Animação fluida de entrada da tela de resultado
        this.gameOverModalContainer.setVisible(true);
        this.gameOverModalContainer.setAlpha(0);
        this.gameOverModalContainer.setScale(0.9);
        this.tweens.add({
            targets: this.gameOverModalContainer,
            alpha: 1,
            scaleX: 1,
            scaleY: 1,
            duration: 380,
            ease: 'Back.easeOut'
        });
    }

    createPlayerStatsSummaryCard(x, y, headerLabel, stats, isLocal) {
        const container = this.add.container(x, y);
        const borderColor = isLocal ? BOARD_LAYOUT.COLORS.LOCAL_RED : BOARD_LAYOUT.COLORS.OPP_BLUE;

        const cardBg = this.add.rectangle(0, 0, 395, 340, 0x1e293b, 0.95)
            .setStrokeStyle(2, borderColor);

        const tagTxt = this.add.text(0, -142, headerLabel, {
            fontSize: '12px', color: '#94a3b8', fontStyle: 'bold', fontFamily: 'monospace'
        }).setOrigin(0.5);

        const nameTxt = this.add.text(0, -116, `${stats.winner ? '👑 ' : ''}${stats.playerName || 'Jogador'}`, {
            fontSize: '19px', color: '#ffffff', fontStyle: 'bold', fontFamily: 'monospace'
        }).setOrigin(0.5);

        const divider = this.add.rectangle(0, -92, 355, 2, 0x334155);

        const formatDelta = (delta) => {
            if (!delta || delta === 0) return '';
            return delta > 0 ? ` (+${delta})` : ` (${delta})`;
        };

        const permEloVal = `${stats.permanentElo || 1000}${formatDelta(stats.permanentEloDelta)}`;
        const weekEloVal = `${stats.weeklyElo || 1000}${formatDelta(stats.weeklyEloDelta)}`;

        const rows = [
            { label: '🏅 ELO Permanente:', val: permEloVal, color: (stats.permanentEloDelta >= 0) ? '#4ade80' : '#f87171' },
            { label: '📅 ELO Semanal:', val: weekEloVal, color: (stats.weeklyEloDelta >= 0) ? '#38bdf8' : '#f87171' },
            { label: '⚔️ Dano Total Causado:', val: `${stats.totalDamageDealt ?? 0}`, color: '#fbbf24' },
            { label: '🛡️ Dano Total Recebido:', val: `${stats.totalDamageTaken ?? 0}`, color: '#f87171' },
            { label: '💚 Cura Realizada:', val: `${stats.totalHealingDone ?? 0}`, color: '#4ade80' },
            { label: '💀 Heróis Abatidos:', val: `${stats.heroesDefeated ?? 0}`, color: '#c084fc' }
        ];

        const rowElements = [];
        rows.forEach((r, idx) => {
            const ry = -62 + idx * 30;
            const lbl = this.add.text(-170, ry, r.label, {
                fontSize: '13.5px', color: '#cbd5e1', fontFamily: 'monospace'
            }).setOrigin(0, 0.5);
            const val = this.add.text(170, ry, r.val, {
                fontSize: '14.5px', color: r.color, fontStyle: 'bold', fontFamily: 'monospace'
            }).setOrigin(1, 0.5);
            rowElements.push(lbl, val);
        });

        const scoreBg = this.add.rectangle(0, 134, 355, 42, 0x0f172a)
            .setStrokeStyle(1, BOARD_LAYOUT.COLORS.GOLD_DUST);
        const scoreLbl = this.add.text(-155, 134, '⭐ PONTUAÇÃO FINAL:', {
            fontSize: '14px', color: '#ffd700', fontStyle: 'bold', fontFamily: 'monospace'
        }).setOrigin(0, 0.5);
        const scoreVal = this.add.text(155, 134, `${stats.matchScore ?? 0} PTS`, {
            fontSize: '16px', color: '#ffd700', fontStyle: 'bold', fontFamily: 'monospace'
        }).setOrigin(1, 0.5);

        container.add([cardBg, tagTxt, nameTxt, divider, ...rowElements, scoreBg, scoreLbl, scoreVal]);
        return container;
    }

    async onPlayAgainClicked() {
        if (this.isSubmittingAction) return;
        this.isSubmittingAction = true;
        this.playAgainBtnTxt.setText('INICIANDO...');
        this.playAgainBtnBg.setFillStyle(0x475569);

        const oppName = this.currentSnapshot?.opponentPlayer?.name || '';
        const isBotMatch = oppName.toLowerCase().includes('bot') || oppName.toLowerCase().includes('máquina');

        try {
            if (isBotMatch) {
                const res = await fetch(`http://localhost:8080/api/matches/start-pve?playerId=${this.localPlayerId}`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        playerId: this.localPlayerId,
                        playerName: this.currentSnapshot?.localPlayer?.name || 'Viajante da Poeira'
                    })
                });
                if (res.ok) {
                    const data = await res.json();
                    this.isSubmittingAction = false;
                    sessionStorage.setItem('ldp_matchId', data.matchId);
                    this.scene.restart({
                        matchId: data.matchId,
                        localPlayerId: data.playerId || this.localPlayerId,
                        initialSnapshots: data.snapshots || []
                    });
                    return;
                }
            } else {
                // Reingressa na fila de matchmaking PvP
                const res = await fetch(`http://localhost:8080/api/matchmaking/join`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        playerId: this.localPlayerId,
                        playerName: this.currentSnapshot?.localPlayer?.name || 'Viajante da Poeira'
                    })
                });
                if (res.ok) {
                    const data = await res.json();
                    this.isSubmittingAction = false;
                    if (data.status === 'MATCH_FOUND' && data.matchId) {
                        sessionStorage.setItem('ldp_matchId', data.matchId);
                        this.scene.restart({
                            matchId: data.matchId,
                            localPlayerId: data.playerId || this.localPlayerId,
                            initialSnapshots: data.snapshots || []
                        });
                        return;
                    }
                }
            }
        } catch (err) {
            console.warn('Falha ao reiniciar partida automaticamente, voltando ao menu:', err);
        }

        this.isSubmittingAction = false;
        this.cleanupAndReturnToMenu();
    }

    cleanupAndReturnToMenu() {
        if (this.snapshotPlayer) {
            this.snapshotPlayer.clearQueue();
        }
        this.isSettingsModalOpen = false;
        this.isChoiceModalActive = false;
        sessionStorage.removeItem('ldp_matchId');

        const storedPlayer = JSON.parse(localStorage.getItem('lendas_player')) || {
            uuid: this.localPlayerId,
            name: 'Viajante da Poeira'
        };

        // Procura pela cena de Menu/Lobby registrada no Phaser; se não houver, reinicia para a tela inicial
        const menuKeys = ['MainMenuScene', 'MainMenu', 'LobbyScene', 'HomeScene', 'Preloader'];
        const registeredScenes = this.scene.manager.keys;
        const targetMenuKey = menuKeys.find(k => registeredScenes[k]);

        if (targetMenuKey) {
            this.scene.start(targetMenuKey, { player: storedPlayer });
        } else {
            window.location.href = '/';
        }
    }

    // =========================================================================
    // PAINEL E ANIMAÇÕES DO SISTEMA DE LIBERAÇÃO (MISSÃO / EVOLUÇÃO)
    // =========================================================================

    createLiberationZonePanels() {
        const localPos = BOARD_LAYOUT.LIBERATION_PANEL?.LOCAL || { x: 1525, y: 945 };
        const oppPos = BOARD_LAYOUT.LIBERATION_PANEL?.OPPONENT || { x: 1525, y: 135 };

        this.localLiberationContainer = this.add.container(localPos.x, localPos.y).setDepth(50);
        this.oppLiberationContainer = this.add.container(oppPos.x, oppPos.y).setDepth(50);
    }

    syncLiberationZones(localLibCards = [], oppLibCards = []) {
        this.renderSingleLiberationPanel(this.localLiberationContainer, localLibCards, true);
        this.renderSingleLiberationPanel(this.oppLiberationContainer, oppLibCards, false);
    }

    renderSingleLiberationPanel(container, liberationCards, isLocal) {
        if (!container) return;
        container.removeAll(true);

        const { width, height } = BOARD_LAYOUT.MEDIUM_CARD; // 140 x 190 (Mesmo tamanho dos Heróis no banco)

        const shadow = this.add.rectangle(5, 7, width, height, 0x090d16, 0.7);
        const backFill = this.add.rectangle(0, 0, width - 4, height - 4, 0xe6dec8, 1);

        container.add([shadow, backFill]);

        if (liberationCards.length === 0) {
            // Estado Vazio / Já Liberada
            const emptyFrame = this.add.image(0, 0, 'ui_liberation_frame')
                .setDisplaySize(width, height)
                .setTint(0x64748b);

            const headerTxt = this.add.text(0, -height / 2 + 13, isLocal ? 'LIBERAÇÃO' : 'MISSÃO INIMIGA', {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '10px',
                color: '#090d16',
                fontStyle: 'bold'
            }).setOrigin(0.5).setResolution(2);

            const emptyTxt = this.add.text(0, 0, '✨ SELO\nLIBERADO\nOU AUSENTE', {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '11px',
                color: '#334155',
                fontStyle: 'bold',
                align: 'center'
            }).setOrigin(0.5).setResolution(2);

            container.add([emptyFrame, headerTxt, emptyTxt]);
            return;
        }

        const activeMission = liberationCards[0];
        const current = activeMission.missionCurrentProgress ?? 0;
        const target = Math.max(1, activeMission.missionTargetValue ?? 1);
        const ratio = Phaser.Math.Clamp(current / target, 0, 1);

        // 1. Arte da Carta de Liberação na janela superior (se existir textura)
        const artKey = activeMission.imagem && this.textures.exists(activeMission.imagem)
            ? activeMission.imagem
            : (this.textures.exists(activeMission.cardName) ? activeMission.cardName : null);

        if (artKey) {
            const artSprite = this.add.image(0, -height * 0.14, artKey)
                .setDisplaySize(width * 0.86, height * 0.44);
            artSprite.texture.setFilter(Phaser.Textures.FilterMode.NEAREST);
            container.add(artSprite);
        } else {
            const placeholderTxt = this.add.text(0, -height * 0.14, '★ MISSÃO ★', {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '12px',
                color: '#b45309',
                fontStyle: 'bold'
            }).setOrigin(0.5).setResolution(2);
            container.add(placeholderTxt);
        }

        // 2. Moldura SVG de Liberação do tamanho do Herói
        const frame = this.add.image(0, 0, 'ui_liberation_frame').setDisplaySize(width, height);

        // Correntes VGA trancadas sobre a carta enquanto a missão não está completa
        let chainsOverlay = null;
        if (current < target && this.textures.exists('vga_chains_overlay')) {
            chainsOverlay = this.add.image(0, 0, 'vga_chains_overlay').setDisplaySize(width, height);
            container.chainsOverlay = chainsOverlay;
            container.add(chainsOverlay);
        }

        // 3. Título no Topo Claro
        const headerTxt = this.add.text(0, -height / 2 + 12, isLocal ? '★ LIBERAÇÃO' : '★ MISSÃO INIMIGA', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '9.5px',
            color: '#090d16',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        // 4. Faixa Central Preta: Nome da Carta de Liberação
        const cardTitle = this.add.text(0, height * 0.10, activeMission.cardName || 'Carta de Liberação', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '8.5px',
            color: '#ffe600',
            fontStyle: 'bold',
            align: 'center',
            wordWrap: { width: width - 16 }
        }).setOrigin(0.5).setResolution(2);

        // 5. Caixa Clara Inferior: Nome da Evolução + Barra de Progresso Grande
        const targetHeroHint = activeMission.evolvesIntoHeroName
            ? `➜ ${activeMission.evolvesIntoHeroName}`
            : (activeMission.missionType || 'Missão');

        const subTxt = this.add.text(0, height * 0.25, targetHeroHint, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '8.5px',
            color: '#090d16',
            fontStyle: 'bold',
            align: 'center',
            wordWrap: { width: width - 20 }
        }).setOrigin(0.5).setResolution(2);

        const barWidth = width - 26;
        const barY = height * 0.40;
        const barBg = this.add.rectangle(0, barY, barWidth, 16, 0x090d16)
            .setStrokeStyle(1.5, 0x475569);
        const barFill = this.add.rectangle(-barWidth / 2, barY, barWidth * ratio, 16, 0x16a34a)
            .setOrigin(0, 0.5);
        const progLabel = this.add.text(0, barY, `${current} / ${target}`, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '10.5px',
            color: '#ffffff',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        frame.setInteractive({ useHandCursor: true })
            .on('pointerover', () => {
                this.tweens.add({ targets: container, scaleX: 1.05, scaleY: 1.05, duration: 120 });
                this.showLargeCardPreview(activeMission, false);
            })
            .on('pointerout', () => {
                this.tweens.add({ targets: container, scaleX: 1.0, scaleY: 1.0, duration: 120 });
                this.hideLargeCardPreview();
            });

        container.add([frame, headerTxt, cardTitle, subTxt, barBg, barFill, progLabel]);
    }

    handleMissionProgressEvent(triggerContext) {
        if (!triggerContext) return;
        const isLocalOwner = String(triggerContext.ownerPlayerId) === String(this.localPlayerId);
        const targetPanel = isLocalOwner ? this.localLiberationContainer : this.oppLiberationContainer;
        if (!targetPanel) return;

        // Ciclagem de paleta VGA (Branco -> Amarelo #FFFF55 -> Ciano #55FFFF -> Vermelho #FF5555 -> Normal)
        const vgaPaletteCycle = [0xffffff, 0xffff55, 0x55ffff, 0xff5555, null];
        const baseX = targetPanel.x;
        const baseY = targetPanel.y;

        vgaPaletteCycle.forEach((tintHex, stepIdx) => {
            this.time.delayedCall(stepIdx * 65, () => {
                if (!targetPanel || !targetPanel.scene) return;
                // Shake pixelado em saltos inteiros de 4px (estilo DOS/VGA)
                const offsetX = stepIdx < 4 ? (stepIdx % 2 === 0 ? -4 : 4) : 0;
                const offsetY = stepIdx < 4 ? (stepIdx % 2 === 0 ? 2 : -2) : 0;
                targetPanel.setPosition(baseX + offsetX, baseY + offsetY);

                if (targetPanel.chainsOverlay) {
                    if (tintHex) {
                        targetPanel.chainsOverlay.setTintFill(tintHex);
                    } else {
                        targetPanel.chainsOverlay.clearTint();
                    }
                }
            });
        });
    }

    handleCardLiberatedEvent(triggerContext) {
        if (!triggerContext) return;
        const isLocalOwner = String(triggerContext.ownerPlayerId) === String(this.localPlayerId);
        const startPos = isLocalOwner
            ? (BOARD_LAYOUT.LIBERATION_PANEL?.LOCAL || { x: 1525, y: 945 })
            : (BOARD_LAYOUT.LIBERATION_PANEL?.OPPONENT || { x: 1525, y: 135 });

        // 1. Dispara a quebra de correntes em estética VGA (16 cores, 16.6 FPS stepped)
        this.playVgaChainShatterEffect(startPos.x, startPos.y);

        const cardData = triggerContext.spawnedCardData || {
            uuid: triggerContext.spawnedCardUuid,
            cardName: triggerContext.spawnedCardName || 'Carta Liberada',
            cardType: 'ACTION',
            color: 'YELLOW',
            faceUp: isLocalOwner
        };

        // 2. Caixa de Alerta Estilo BIOS / VGA Mode 13h com ciclagem de borda
        const vgaBanner = this.add.container(960, 420).setDepth(215);
        const bannerShadow = this.add.rectangle(6, 6, 620, 64, 0x000000, 1);
        const bannerBg = this.add.rectangle(0, 0, 620, 64, 0x0000aa, 1) // Azul BIOS VGA (#0000AA)
            .setStrokeStyle(4, 0xffff55); // Amarelo VGA (#FFFF55)
        const bannerInnerBorder = this.add.rectangle(0, 0, 608, 52, 0x0000aa, 0)
            .setStrokeStyle(2, 0x55ffff); // Ciano VGA (#55FFFF)

        const bannerTxt = this.add.text(0, 0, `╔══ [SELO ROMPIDO // 100%] ══╗\n${cardData.cardName.toUpperCase()} LIBERADA!`, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '16px',
            color: '#ffffff',
            fontStyle: 'bold',
            align: 'center',
            lineSpacing: 4
        }).setOrigin(0.5).setResolution(2);

        vgaBanner.add([bannerShadow, bannerBg, bannerInnerBorder, bannerTxt]);

        // Faz a borda e o texto piscarem na paleta VGA a cada 90ms
        const vgaColors = ['#ffff55', '#55ffff', '#ff5555', '#55ff55', '#ffffff'];
        this.time.addEvent({
            delay: 90,
            repeat: 18,
            callback: ( () => {
                let tick = 0;
                return () => {
                    if (!vgaBanner.scene) return;
                    bannerTxt.setColor(vgaColors[tick % vgaColors.length]);
                    bannerBg.setStrokeStyle(4, tick % 2 === 0 ? 0xffff55 : 0xff5555);
                    tick++;
                };
            })()
        });

        this.time.delayedCall(1750, () => {
            if (vgaBanner && vgaBanner.scene) vgaBanner.destroy();
        });

        // 3. Carta Liberada voa em passos discretos (Stepped Ease) com rastro fantasma VGA
        const flyingCard = new PlayableCardPrefab(
            this,
            startPos.x,
            startPos.y,
            { ...cardData, faceUp: isLocalOwner },
            false,
            'MEDIUM',
            0
        );
        flyingCard.setDepth(205);
        flyingCard.bg.setStrokeStyle(4, 0xffff55);

        // Gerador de rastro fantasma VGA (Ciano #55FFFF e Magenta #FF55FF) a cada 60ms
        const trailTimer = this.time.addEvent({
            delay: 60,
            repeat: 14,
            callback: () => {
                if (!flyingCard.scene) return;
                const ghost = this.add.rectangle(
                    Math.round(flyingCard.x / 4) * 4,
                    Math.round(flyingCard.y / 4) * 4,
                    BOARD_LAYOUT.MEDIUM_CARD.width * flyingCard.scaleX,
                    BOARD_LAYOUT.MEDIUM_CARD.height * flyingCard.scaleY,
                    0x55ffff,
                    0.45
                ).setStrokeStyle(3, 0xff55ff).setDepth(200);

                this.time.delayedCall(120, () => ghost.destroy());
            }
        });

        const destX = 1035;
        const destY = isLocalOwner ? 964 : 10;

        this.tweens.add({
            targets: flyingCard,
            x: 960,
            y: 540,
            scaleX: 1.25,
            scaleY: 1.25,
            duration: 420,
            ease: 'Stepped',
            easeParams: [7], // 7 quadros discretos estilo VGA
            onComplete: () => {
                this.tweens.add({
                    targets: flyingCard,
                    x: destX,
                    y: destY,
                    scaleX: 0.75,
                    scaleY: 0.75,
                    delay: 420,
                    duration: 420,
                    ease: 'Stepped',
                    easeParams: [7],
                    onComplete: () => {
                        trailTimer.remove(false);
                        flyingCard.destroy();
                    }
                });
            }
        });
    }

    /**
     * Efeito de Explosão de Correntes em VGA (Mode 13h):
     * - Flash de inversão de paleta em blocos (Branco -> Amarelo -> Vermelho -> Ciano)
     * - 14 Elos de corrente pixelados + blocos de pixel 6x6 voando a ~16.6 FPS (intervalos de 60ms)
     */
    playVgaChainShatterEffect(centerX, centerY) {
        const { width, height } = BOARD_LAYOUT.MEDIUM_CARD;
        const vgaPalette = [0xffffff, 0xffff55, 0xff5555, 0x55ffff, 0xff55ff, 0xaaaaaa, 0x555555];

        // 1. Barras de Scan / Ruptura Horizontal VGA sobre o slot
        const scanGroup = this.add.container(centerX, centerY).setDepth(202);
        const stripeCount = 8;
        const stripeH = Math.floor(height / stripeCount);

        for (let i = 0; i < stripeCount; i++) {
            const sy = -height / 2 + i * stripeH + stripeH / 2;
            const color = vgaPalette[i % vgaPalette.length];
            const bar = this.add.rectangle(0, sy, width + 12, stripeH - 2, color, 1);
            scanGroup.add(bar);
        }

        // Alterna as cores das barras e remove em 4 passos de 60ms
        this.time.addEvent({
            delay: 60,
            repeat: 4,
            callback: (() => {
                let step = 0;
                return () => {
                    if (!scanGroup.scene) return;
                    step++;
                    scanGroup.list.forEach((bar, idx) => {
                        bar.x = (idx % 2 === 0 ? 1 : -1) * step * 8;
                        bar.scaleY = Math.max(0, 1 - step * 0.22);
                        bar.setFillStyle(vgaPalette[(idx + step) % vgaPalette.length], 1);
                    });
                    if (step >= 4) scanGroup.destroy();
                };
            })()
        });

        // 2. Gera os elos quebrados (vga_chain_link_shard) e pixels quadrados VGA
        const particles = [];
        const totalShards = 16;

        for (let i = 0; i < totalShards; i++) {
            const angle = (Math.PI * 2 * i) / totalShards + (i % 2) * 0.2;
            const speed = 10 + (i % 3) * 5;
            const isChainLink = (i % 2 === 0) && this.textures.exists('vga_chain_link_shard');

            let sprite;
            if (isChainLink) {
                sprite = this.add.image(centerX, centerY, 'vga_chain_link_shard')
                    .setDisplaySize(28, 28)
                    .setDepth(206);
                sprite.setAngle((i * 45) % 360);
            } else {
                // Pixel quadrado grosso (8x8) nas cores da paleta VGA com borda preta
                const pxColor = vgaPalette[i % vgaPalette.length];
                sprite = this.add.rectangle(centerX, centerY, 10, 10, pxColor, 1)
                    .setStrokeStyle(2, 0x000000)
                    .setDepth(206);
            }

            particles.push({
                sprite,
                vx: Math.cos(angle) * speed,
                vy: Math.sin(angle) * speed - 6,
                gravity: 2.2,
                rotStep: (i % 2 === 0 ? 45 : -45),
                colorIdx: i
            });
        }

        // Atualiza a física dos elos em passos discretos de 60ms (autêntico 16.6 FPS e snap para múltiplos de 2px)
        let frameTick = 0;
        const maxTicks = 12;

        this.time.addEvent({
            delay: 60,
            repeat: maxTicks,
            callback: () => {
                frameTick++;
                particles.forEach(p => {
                    if (!p.sprite || !p.sprite.scene) return;
                    p.x = (p.x ?? p.sprite.x) + p.vx;
                    p.y = (p.y ?? p.sprite.y) + p.vy;
                    p.vy += p.gravity;

                    // Snap nas coordenadas para manter o movimento pixelado (múltiplos de 3px)
                    p.sprite.x = Math.round(p.x / 3) * 3;
                    p.sprite.y = Math.round(p.y / 3) * 3;
                    p.sprite.angle += p.rotStep;

                    // Ciclagem de paleta nos últimos frames antes de sumir
                    if (frameTick > 7) {
                        p.sprite.setVisible(frameTick % 2 === 0); // Blinking estilo DOS/VGA em vez de alpha fade
                    }

                    if (frameTick >= maxTicks) {
                        p.sprite.destroy();
                    }
                });
            }
        });
    }

    handleHeroEvolvedEvent(triggerContext) {
        if (!triggerContext) return;
        const heroId = triggerContext.targetHeroId || triggerContext.sourceCardUuid;
        const heroPrefab = heroId ? this.heroPrefabs.get(heroId) : null;

        const effectX = heroPrefab ? heroPrefab.x : BOARD_LAYOUT.ARENA_CIRCLE.x;
        const effectY = heroPrefab ? heroPrefab.y : BOARD_LAYOUT.ARENA_CIRCLE.y;

        this.playVisualEffect('buff', effectX, effectY);
        this.playVisualEffect('cura', effectX, effectY);

        // Anel de impacto dourado de evolução
        const evoRing = this.add.circle(effectX, effectY, 30, 0xfacc15, 0.45)
            .setStrokeStyle(4, 0xffffff)
            .setDepth(185);

        this.tweens.add({
            targets: evoRing,
            scaleX: 4.5,
            scaleY: 4.5,
            alpha: 0,
            duration: 650,
            ease: 'Cubic.easeOut',
            onComplete: () => evoRing.destroy()
        });

        if (heroPrefab) {
            heroPrefab.showFloatingNumber('🔥 EVOLUIU!', '#facc15');
            this.tweens.killTweensOf(heroPrefab);
            this.tweens.add({
                targets: heroPrefab,
                scaleX: 1.28,
                scaleY: 1.28,
                yoyo: true,
                duration: 340,
                ease: 'Back.easeOut'
            });
        }
    }
}

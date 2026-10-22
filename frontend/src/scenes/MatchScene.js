import Phaser from 'phaser';
import { BOARD_LAYOUT } from '../config/BoardCoordinates.js';
import HeroCardPrefab from '../prefabs/HeroCardPrefab.js';
import PlayableCardPrefab from '../prefabs/PlayableCardPrefab.js';
import SnapshotPlayerManager from '../managers/SnapshotPlayerManager.js';
import SelectionManager from '../managers/SelectionManager.js';

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

        this.isWaitingOpponentConfirmation = false;
        this.isSubmittingAction = false;
        this.isPolling = false;
        this.isChoiceModalActive = false;
        this.isSettingsModalOpen = false;
        this.musicVolume = Number(localStorage.getItem('ldp_music_volume') ?? 0.7);
        this.sfxVolume = Number(localStorage.getItem('ldp_sfx_volume') ?? 0.8);
    }

    init(data) {
        this.matchId = data.matchId;
        this.localPlayerId = data.localPlayerId;
        this.initialSnapshots = data.initialSnapshots || (data.initialSnapshot ? [data.initialSnapshot] : []);
    }

    preload() {
        this.load.setPath('assets');
        if (!this.textures.exists('card_back')) {
            this.load.image('card_back', 'modeloBaralho.png');
        }
        if (!this.textures.exists('card_frame_playable')) {
            this.load.image('card_frame_playable', 'modeloAlfaCartas.jpeg');
        }
        if (!this.textures.exists('card_frame_hero')) {
            this.load.image('card_frame_hero', 'modeloAlfa.jpeg');
        }
    }

    create() {
        this.heroPrefabs.clear();
        this.spawnedCardPrefabs = new Map();
        this.handCardPrefabs = [];
        this.opponentHandDummies = [];
        this.resolveCardPrefabs = [];
        this.resolveSlotVisuals = [];
        this.currentSnapshot = null;
        this.isWaitingOpponentConfirmation = false;
        this.isSubmittingAction = false;
        this.isPolling = false;
        this.isChoiceModalActive = false;

        this.snapshotPlayer = new SnapshotPlayerManager(this);
        this.selectionManager = new SelectionManager(this);

        this.drawBoardBackgroundAndSlots();
        this.createPilesPlayerBannersAndMainButton();
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

    drawBoardBackgroundAndSlots() {
        const { COLORS, MEDIUM_CARD, BENCH_BOXES, ARENA_CIRCLE } = BOARD_LAYOUT;

        // Fundo preto puro sem linhas de grade
        this.add.rectangle(960, 540, 1920, 1080, COLORS.BG_DARK, 1);

        // Círculo central da Arena definido apenas por tom de preto mais claro (sem linha de contorno)
        const graphics = this.add.graphics();
        graphics.fillStyle(COLORS.ZONE_SURFACE, 1);
        graphics.fillCircle(ARENA_CIRCLE.x, ARENA_CIRCLE.y, ARENA_CIRCLE.radius);

        // Áreas das Bancadas (BENCH) em tom de preto mais claro, sem bordas
        this.add.rectangle(BENCH_BOXES.OPPONENT.x, BENCH_BOXES.OPPONENT.y, BENCH_BOXES.OPPONENT.width, BENCH_BOXES.OPPONENT.height, COLORS.ZONE_SURFACE, 1);
        this.add.text(BENCH_BOXES.OPPONENT.x, BENCH_BOXES.OPPONENT.y - 128, 'BANCADA INIMIGA', {
            fontSize: '12px', color: '#64748b', fontStyle: 'bold'
        }).setOrigin(0.5);

        this.add.rectangle(BENCH_BOXES.LOCAL.x, BENCH_BOXES.LOCAL.y, BENCH_BOXES.LOCAL.width, BENCH_BOXES.LOCAL.height, COLORS.ZONE_SURFACE, 1);
        this.add.text(BENCH_BOXES.LOCAL.x, BENCH_BOXES.LOCAL.y - 128, 'SUA BANCADA', {
            fontSize: '12px', color: '#64748b', fontStyle: 'bold'
        }).setOrigin(0.5);

        // Slots individuais das bancadas em tom grafite escuro sem contorno
        BOARD_LAYOUT.OPPONENT_BENCH_SLOTS.forEach(pos => {
            this.add.rectangle(pos.x, pos.y, MEDIUM_CARD.width + 6, MEDIUM_CARD.height + 6, COLORS.SLOT_SURFACE, 1);
        });

        BOARD_LAYOUT.LOCAL_BENCH_SLOTS.forEach(pos => {
            this.add.rectangle(pos.x, pos.y, MEDIUM_CARD.width + 6, MEDIUM_CARD.height + 6, COLORS.SLOT_SURFACE, 1);
        });

        // Slots de Batalha Ativa (sem contorno fixo)
        this.oppBattleSlotRect = this.add.rectangle(
            BOARD_LAYOUT.BATTLE_SLOTS.OPPONENT_ACTIVE_HERO.x,
            BOARD_LAYOUT.BATTLE_SLOTS.OPPONENT_ACTIVE_HERO.y,
            MEDIUM_CARD.width + 10, MEDIUM_CARD.height + 10, COLORS.SLOT_ELEVATED, 1
        );
        this.add.text(BOARD_LAYOUT.BATTLE_SLOTS.OPPONENT_ACTIVE_HERO.x, BOARD_LAYOUT.BATTLE_SLOTS.OPPONENT_ACTIVE_HERO.y, 'BATALHA\nINIMIGO', {
            fontSize: '11px', color: '#64748b', fontStyle: 'bold', align: 'center'
        }).setOrigin(0.5);

        this.localBattleSlotRect = this.add.rectangle(
            BOARD_LAYOUT.BATTLE_SLOTS.LOCAL_ACTIVE_HERO.x,
            BOARD_LAYOUT.BATTLE_SLOTS.LOCAL_ACTIVE_HERO.y,
            MEDIUM_CARD.width + 10, MEDIUM_CARD.height + 10, COLORS.SLOT_ELEVATED, 1
        );
        this.add.text(BOARD_LAYOUT.BATTLE_SLOTS.LOCAL_ACTIVE_HERO.x, BOARD_LAYOUT.BATTLE_SLOTS.LOCAL_ACTIVE_HERO.y, 'BATALHA\nALIADO', {
            fontSize: '11px', color: '#64748b', fontStyle: 'bold', align: 'center'
        }).setOrigin(0.5);

        // 4 Slots de Resolução Central (sem contorno fixo)
        this.resolveSlotVisuals = BOARD_LAYOUT.RESOLVE_SLOTS.map((pos, idx) => {
            const rect = this.add.rectangle(pos.x, pos.y, MEDIUM_CARD.width + 4, MEDIUM_CARD.height + 4, COLORS.SLOT_SURFACE, 1);
            const label = this.add.text(pos.x, pos.y, `${idx + 1}º AÇÃO`, {
                fontSize: '11px', color: '#94a3b8', fontStyle: 'bold', align: 'center'
            }).setOrigin(0.5);
            return { rect, label, x: pos.x, y: pos.y, index: idx };
        });
    }

    updateInterleavedResolveSlots(localHasInitiative) {
        const { COLORS } = BOARD_LAYOUT;

        this.resolveSlotVisuals.forEach((slot, idx) => {
            const belongsToLocal = localHasInitiative ? (idx % 2 === 0) : (idx % 2 !== 0);
            const fillColor = belongsToLocal ? COLORS.LOCAL_SLOT_TINT : COLORS.OPP_SLOT_TINT;
            const labelColor = belongsToLocal ? '#fca5a5' : '#7dd3fc';
            const ownerTag = belongsToLocal ? 'VOCÊ' : 'OPONENTE';

            slot.belongsToLocal = belongsToLocal;
            slot.baseFill = fillColor;
            slot.rect.setFillStyle(fillColor, 1).setStrokeStyle(0);
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

        this.mainButtonBg = this.add.rectangle(PILES.MAIN_BUTTON.x, PILES.MAIN_BUTTON.y, 280, 74, 0x16a34a)
            .setDepth(80)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => this.onMainButtonClicked());

        this.mainButtonLabel = this.add.text(PILES.MAIN_BUTTON.x, PILES.MAIN_BUTTON.y, 'CONFIRMAR', {
            fontSize: '15px', color: '#ffffff', fontStyle: 'bold', align: 'center'
        }).setOrigin(0.5).setDepth(81);
    }

    createMiniDeckIcon(x, y) {
        const { COLORS } = BOARD_LAYOUT;
        const container = this.add.container(x, y);
        const outer = this.add.rectangle(0, 0, 82, 96, COLORS.ZONE_SURFACE);
        const inner = this.add.rectangle(0, 0, 70, 84, COLORS.SLOT_SURFACE);
        const lbl = this.add.text(0, -16, 'BARALHO', { fontSize: '9.5px', color: '#94a3b8', fontStyle: 'bold' }).setOrigin(0.5);
        const countTxt = this.add.text(0, 12, '15/30', { fontSize: '14px', color: '#ffffff', fontStyle: 'bold' }).setOrigin(0.5);
        container.add([outer, inner, lbl, countTxt]);
        return countTxt;
    }

    createMiniTombstoneIcon(x, y, onClickCallback) {
        const { COLORS } = BOARD_LAYOUT;
        const container = this.add.container(x, y);
        const outer = this.add.rectangle(0, 0, 82, 96, COLORS.SLOT_SURFACE);

        const g = this.add.graphics();
        g.fillStyle(0x475569, 1);
        g.fillRoundedRect(-16, -28, 32, 34, { tl: 14, tr: 14, bl: 0, br: 0 });
        g.fillStyle(0x334155, 1);
        g.fillRect(-20, 6, 40, 5);

        const crossV = this.add.rectangle(0, -14, 3, 14, COLORS.ZONE_SURFACE);
        const crossH = this.add.rectangle(0, -17, 11, 3, COLORS.ZONE_SURFACE);

        const countTxt = this.add.text(0, 26, '🪦 0', { fontSize: '12px', color: '#e2e8f0', fontStyle: 'bold' }).setOrigin(0.5);
        container.add([outer, g, crossV, crossH, countTxt]);

        outer.setInteractive({ useHandCursor: true }).on('pointerdown', onClickCallback);
        return countTxt;
    }

    createPlayerCornerBanner(x, y, badgeCode, roleLabel, defaultName, fillColor) {
        const { COLORS } = BOARD_LAYOUT;
        const container = this.add.container(x, y);
        const bg = this.add.rectangle(0, 0, 340, 64, COLORS.ZONE_SURFACE, 1);
        const avatar = this.add.circle(-132, 0, 20, fillColor);
        const codeTxt = this.add.text(-132, 0, badgeCode, { fontSize: '13px', color: '#ffffff', fontStyle: 'bold' }).setOrigin(0.5);
        const roleTxt = this.add.text(-98, -14, roleLabel, { fontSize: '10px', color: '#ffd700', fontStyle: 'bold' }).setOrigin(0, 0.5);
        const nameTxt = this.add.text(-98, 10, defaultName, { fontSize: '15px', color: '#ffffff', fontStyle: 'bold' }).setOrigin(0, 0.5);

        container.add([bg, avatar, codeTxt, roleTxt, nameTxt]);
        return { container, roleTxt, nameTxt };
    }

    createLargeHoverPreviewPanel() {
        const { x, y } = BOARD_LAYOUT.PILES.LARGE_PREVIEW;
        const { width, height } = BOARD_LAYOUT.LARGE_CARD;

        this.largePreviewContainer = this.add.container(x, y).setDepth(190).setVisible(false);

        this.lpShadow = this.add.rectangle(8, 10, width, height, 0x000000, 0.75);
        this.lpBg = this.add.rectangle(0, 0, width + 4, height + 4, 0x000000, 0)
            .setStrokeStyle(4, BOARD_LAYOUT.COLORS.GOLD_DUST);

        const artW = width * 0.905;
        const artH = height * 0.50;
        const artX = -width / 2 + (width * 0.046) + (artW / 2);
        const artY = -height / 2 + (height * 0.11) + (artH / 2);

        this.lpArtBox = this.add.rectangle(artX, artY, artW, artH, 0x1e2430, 0.3).setStrokeStyle(1, 0x475569);
        this.lpArtLabel = this.add.text(artX, artY, 'ILUSTRAÇÃO AMPLIADA', {
            fontSize: '13px', color: '#475569', fontStyle: 'bold', fontFamily: 'monospace'
        }).setOrigin(0.5);

        this.lpFrame = this.add.image(0, 0, 'card_frame_playable').setDisplaySize(width, height);

        const headerY = -height / 2 + (height * 0.055);
        this.lpHeader = this.add.rectangle(0, headerY, width * 0.80, height * 0.065, 0x1d4ed8, 0.22);
        this.lpTitle = this.add.text(0, headerY, '', {
            fontSize: '16px', color: '#000000', fontStyle: 'bold', fontFamily: 'monospace', align: 'center', wordWrap: { width: width * 0.82 }
        }).setOrigin(0.5);

        this.lpTypeLine = this.add.text(0, height * 0.20, '', {
            fontSize: '11.5px', color: '#4a3520', fontStyle: 'bold', fontFamily: 'monospace', align: 'center'
        }).setOrigin(0.5);

        this.lpDesc = this.add.text(0, height * 0.32, '', {
            fontSize: '12px', color: '#000000', fontFamily: 'monospace', align: 'center', wordWrap: { width: width * 0.84 }
        }).setOrigin(0.5);

        this.lpFooterStats = this.add.text(0, height / 2 - 22, '', {
            fontSize: '11.5px', color: '#14532d', fontStyle: 'bold', fontFamily: 'monospace', align: 'center'
        }).setOrigin(0.5);

        this.largePreviewContainer.add([
            this.lpShadow, this.lpBg, this.lpFrame, this.lpArtBox, this.lpArtLabel,
            this.lpHeader, this.lpTitle, this.lpTypeLine, this.lpDesc, this.lpFooterStats
        ]);
    }

    showLargeCardPreview(cardOrHeroDto, isHero = false) {
        if (!cardOrHeroDto) return;
        const { width, height } = BOARD_LAYOUT.LARGE_CARD;
        const headerColor = BOARD_LAYOUT.COLORS.CARD_COLORS[cardOrHeroDto.color] || 0x475569;

        const frameKey = isHero ? 'card_frame_hero' : 'card_frame_playable';
        if (this.textures.exists(frameKey)) {
            this.lpFrame.setTexture(frameKey).setDisplaySize(width, height).setVisible(true);
        }

        this.lpHeader.setFillStyle(headerColor, 0.22);
        this.lpTitle.setText(cardOrHeroDto.cardName);

        if (isHero) {
            this.lpArtLabel.setText('⚔ ARTE DO HERÓI');
            this.lpTypeLine.setPosition(-width * 0.12, height * 0.19).setText(`HERÓI • ${cardOrHeroDto.color}`);
            this.lpDesc.setPosition(-width * 0.12, height * 0.31)
                .setStyle({ fontSize: '11px', color: '#000000', fontFamily: 'monospace', align: 'left', wordWrap: { width: width * 0.64 } })
                .setText(cardOrHeroDto.descricao || '');
            this.lpFooterStats.setText(
                `ESP:${cardOrHeroDto.espirito} | ATQ:${cardOrHeroDto.ataque} | DEF:${cardOrHeroDto.defesa} | HP:${cardOrHeroDto.vidaAtual}/${cardOrHeroDto.vidaMaxima}`
            );
        } else {
            const empTxt = (cardOrHeroDto.itemType === 'ARMA' && cardOrHeroDto.empunhadura > 0)
                ? ` • Empunhadura: ${cardOrHeroDto.empunhadura}`
                : '';
            this.lpArtLabel.setText(`✦ ARTE (${cardOrHeroDto.cardType})`);
            this.lpTypeLine.setPosition(0, height * 0.20).setText(`${cardOrHeroDto.cardType}${empTxt}`);
            this.lpDesc.setPosition(0, height * 0.33)
                .setStyle({ fontSize: '12px', color: '#000000', fontFamily: 'monospace', align: 'center', wordWrap: { width: width * 0.84 } })
                .setText(cardOrHeroDto.descricao || '');
            this.lpFooterStats.setText(cardOrHeroDto.itemType && cardOrHeroDto.itemType !== 'NENHUM' ? `Item: ${cardOrHeroDto.itemType}` : '');
        }

        this.largePreviewContainer.setVisible(true);
        this.largePreviewContainer.setScale(0.92);
        this.tweens.killTweensOf(this.largePreviewContainer);
        this.tweens.add({
            targets: this.largePreviewContainer,
            scaleX: 1.0,
            scaleY: 1.0,
            duration: 260,
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
        const phase = this.currentSnapshot?.turnPhase;
        if (phase === 'PREPARATION') {
            this.resolveSlotVisuals.forEach(slot => {
                if (slot.belongsToLocal) {
                    slot.rect.setFillStyle(active ? 0x1e3a2f : slot.baseFill, 1);
                    slot.rect.setStrokeStyle(0);
                }
            });
        }
    }

    checkMagneticHoverOnSlots(cardPrefab) {
        if (this.currentSnapshot?.turnPhase !== 'PREPARATION') return;

        this.resolveSlotVisuals.forEach(slot => {
            if (!slot.belongsToLocal) {
                slot.rect.setScale(1.0);
                return;
            }
            const d = Phaser.Math.Distance.Between(cardPrefab.x, cardPrefab.y, slot.x, slot.y);
            slot.rect.setScale(d < 190 ? 1.07 : 1.0);
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

        this.resolveSlotVisuals.forEach(s => s.rect.setScale(1.0));

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

    renderSnapshot(snapshot, isLastInQueue = true) {
        if (!snapshot) return;

        const previousPhase = this.currentSnapshot ? this.currentSnapshot.turnPhase : null;
        this.currentSnapshot = snapshot;

        this.hideLargeCardPreview();

        if (previousPhase !== snapshot.turnPhase) {
            this.selectionManager.resetSelections();
            this.isWaitingOpponentConfirmation = false;
        }

        const localHasInitiative = String(snapshot.activeTurnPlayerId) === String(this.localPlayerId);

        this.phaseBanner.setText(`FASE: ${snapshot.turnPhase}`);
        this.updateInterleavedResolveSlots(localHasInitiative);

        if (snapshot.triggerContext?.logMessage) {
            this.combatLogText.setText(snapshot.triggerContext.logMessage);
        }

        const localPlayer = snapshot.localPlayer || {};
        const opponentPlayer = snapshot.opponentPlayer || {};

        this.localBannerContainer.nameTxt.setText(localPlayer.name || 'Jogador 1');
        this.localBannerContainer.roleTxt.setText(localHasInitiative ? '★ VOCÊ (COM INICIATIVA)' : 'VOCÊ');

        this.oppBannerContainer.nameTxt.setText(opponentPlayer.name || 'Jogador 2');
        this.oppBannerContainer.roleTxt.setText(!localHasInitiative ? '★ OPONENTE (COM INICIATIVA)' : 'OPONENTE');

        this.localDeckText.setText(`${localPlayer.deckCount ?? 0}/30`);
        this.localDiscardText.setText(`🪦 ${(localPlayer.discardPile || []).length}`);
        this.oppDeckText.setText(`${opponentPlayer.deckCount ?? 0}/30`);
        this.oppDiscardText.setText(`🪦 ${(opponentPlayer.discardPile || []).length}`);

        this.syncHeroes(localPlayer.heroes || [], false, BOARD_LAYOUT.LOCAL_BENCH_SLOTS, BOARD_LAYOUT.BATTLE_SLOTS.LOCAL_ACTIVE_HERO);
        this.syncHeroes(opponentPlayer.heroes || [], true, BOARD_LAYOUT.OPPONENT_BENCH_SLOTS, BOARD_LAYOUT.BATTLE_SLOTS.OPPONENT_ACTIVE_HERO);

        this.syncLocalHand(localPlayer.hand || []);
        this.syncOpponentHand(opponentPlayer.handCount ?? 0);
        this.syncInterleavedResolveZone(
            localPlayer.playedCardsInResolve || [],
            opponentPlayer.playedCardsInResolve || [],
            localHasInitiative
        );

        const eventType = snapshot.triggerContext?.eventType;
        if (eventType === 'CARD_SPAWNED' || eventType === 'TOKEN_SUMMONED') {
            this.handleCardSpawnEvent(snapshot.triggerContext, localHasInitiative);
        } else if (eventType === 'SPAWN_FAILED_ZONE_FULL') {
            this.handleSpawnFailedZoneFull(snapshot.triggerContext);
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

        const hasExtraTutoredCards = firstPlayerCards.length > 2 || secondPlayerCards.length > 2;

        // Identifica se este snapshot está invocando dinamicamente uma carta específica
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
                existing.snapToPosition(targetX, targetY, null, 0, 'MEDIUM');
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

            // Se for a carta recém-invocada, handleCardSpawnEvent assumirá a animação partindo do invocador;
            // caso contrário, executa o fade-in padrão do slot de resolução.
            if (!isJustSpawned) {
                prefab.setAlpha(0);
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

        if (!hasExtraTutoredCards) {
            const baseSlots = [
                firstPlayerCards[0] || null,
                secondPlayerCards[0] || null,
                firstPlayerCards[1] || null,
                secondPlayerCards[1] || null
            ];
            baseSlots.forEach((cardDto, slotIndex) => {
                if (cardDto) {
                    const pos = BOARD_LAYOUT.RESOLVE_SLOTS[slotIndex];
                    placeOrReuseResolveCard(cardDto, pos.x, pos.y);
                }
            });
        } else {
            const maxLen = Math.max(firstPlayerCards.length, secondPlayerCards.length);
            const activeCards = [];
            for (let i = 0; i < maxLen; i++) {
                if (i < firstPlayerCards.length && firstPlayerCards[i]) activeCards.push(firstPlayerCards[i]);
                if (i < secondPlayerCards.length && secondPlayerCards[i]) activeCards.push(secondPlayerCards[i]);
            }

            const centerX = BOARD_LAYOUT.ARENA_CIRCLE.x;
            const centerY = BOARD_LAYOUT.RESOLVE_SLOTS[0].y;
            const spacing = Math.max(158, Math.min(176, 840 / Math.max(1, activeCards.length)));
            const startX = centerX - ((activeCards.length - 1) * spacing) / 2;

            activeCards.forEach((cardDto, idx) => {
                const x = startX + idx * spacing;
                placeOrReuseResolveCard(cardDto, x, centerY);
            });
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
            targetHeroId,
            targetZone
        } = triggerContext;

        // 1. Destaca a carta ou Herói invocador (se existir)
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
        }

        // 2. Verifica se a carta invocada já está nos slots de resolução (resolveCardPrefabs)
        let spawnedPrefab = this.resolveCardPrefabs.find(c => c.uuid === spawnedCardUuid);

        // 3. Caso a carta tenha ido direto para ATTACHED ou não esteja nos 4 slots, instancia dinamicamente e registra!
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
            spawnedPrefab.setDepth(145);
            this.spawnedCardPrefabs.set(spawnedPrefab.uuid, spawnedPrefab);
            this.resolveCardPrefabs.push(spawnedPrefab);

            spawnedPrefab.setScale(0.25).setAlpha(0);
            this.tweens.add({
                targets: spawnedPrefab,
                x: destX,
                y: destY,
                scaleX: 1.05,
                scaleY: 1.05,
                alpha: 1,
                duration: 380,
                ease: 'Back.easeOut',
                onComplete: () => {
                    if (targetZone === 'ATTACHED' && targetHeroPrefab) {
                        this.tweens.add({
                            targets: spawnedPrefab,
                            scaleX: 0.35,
                            scaleY: 0.35,
                            alpha: 0,
                            delay: 320,
                            duration: 240,
                            ease: 'Cubic.easeIn'
                        });
                    }
                }
            });
            return;
        }

        // 4. Se a carta está no slot de resolução, anima seu surgimento a partir do invocador (ou do nada) até o slot!
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
            this.mainButtonBg.setFillStyle(0x475569);
            this.mainButtonBg.input.enabled = false;
            return;
        }

        if (this.currentSnapshot.pendingChoice) {
            const isMyChoice = String(this.currentSnapshot.pendingChoice.choosingPlayerId) === String(this.localPlayerId);
            this.mainButtonLabel.setText(isMyChoice ? 'ESCOLHA UM ALVO NO MODAL' : 'OPONENTE ESCOLHENDO ALVO...');
            this.mainButtonBg.setFillStyle(0xd97706);
            this.mainButtonBg.input.enabled = isMyChoice;
            return;
        }

        if (this.isWaitingOpponentConfirmation) {
            this.mainButtonLabel.setText('AGUARDANDO OPONENTE...');
            this.mainButtonBg.setFillStyle(0x475569);
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
        this.mainButtonBg.setFillStyle(canClick ? 0x16a34a : 0x475569);
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
        this.gearBtnContainer = this.add.container(1860, 48).setDepth(210);
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
        this.gameOverModalContainer = this.add.container(960, 540).setDepth(270).setVisible(false);

        const overlay = this.add.rectangle(0, 0, 1920, 1080, 0x000000, 0.78).setInteractive();
        const box = this.add.rectangle(0, 0, 620, 360, 0x0f172a, 0.98)
            .setStrokeStyle(4, BOARD_LAYOUT.COLORS.GOLD_DUST);

        this.gameOverTitleText = this.add.text(0, -90, 'FIM DE PARTIDA', {
            fontSize: '32px', color: '#ffd700', fontStyle: 'bold', fontFamily: 'monospace'
        }).setOrigin(0.5);

        this.gameOverSubtitleText = this.add.text(0, -15, '', {
            fontSize: '18px', color: '#e2e8f0', align: 'center', wordWrap: { width: 540 }
        }).setOrigin(0.5);

        const returnBtnBg = this.add.rectangle(0, 95, 280, 56, 0x16a34a)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => this.cleanupAndReturnToMenu());

        const returnBtnTxt = this.add.text(0, 95, 'VOLTAR AO MENU', {
            fontSize: '18px', color: '#ffffff', fontStyle: 'bold'
        }).setOrigin(0.5);

        this.gameOverModalContainer.add([
            overlay, box, this.gameOverTitleText, this.gameOverSubtitleText, returnBtnBg, returnBtnTxt
        ]);
    }

    showGameOverModal(snapshot) {
        if (!this.gameOverModalContainer) return;

        this.isSettingsModalOpen = false;
        if (this.settingsModalContainer) this.settingsModalContainer.setVisible(false);

        const winnerId = snapshot.winnerPlayerId;
        const isLocalWinner = winnerId && String(winnerId) === String(this.localPlayerId);
        const isForfeit = snapshot.winReason === 'FORFEIT' || snapshot.triggerContext?.eventType === 'MATCH_FORFEITED';

        if (isLocalWinner) {
            this.gameOverTitleText.setText('🏆 VITÓRIA!').setColor('#4ade80');
            this.gameOverSubtitleText.setText(
                isForfeit
                    ? 'Seu oponente desistiu da partida (Vitória por W.O.)!'
                    : 'Você derrotou todos os Heróis inimigos!'
            );
        } else {
            this.gameOverTitleText.setText('💀 DERROTA').setColor('#f87171');
            this.gameOverSubtitleText.setText(
                isForfeit
                    ? 'A partida foi encerrada por desistência.'
                    : 'Todos os seus Heróis foram derrotados.'
            );
        }

        this.gameOverModalContainer.setVisible(true);
    }

    cleanupAndReturnToMenu() {
        if (this.snapshotPlayer) {
            this.snapshotPlayer.clearQueue();
        }
        this.isSettingsModalOpen = false;
        this.isChoiceModalActive = false;
        sessionStorage.removeItem('ldp_matchId');

        // Procura pela cena de Menu/Lobby registrada no Phaser; se não houver, reinicia para a tela inicial
        const menuKeys = ['MenuScene', 'MainMenu', 'LobbyScene', 'HomeScene', 'Preloader'];
        const registeredScenes = this.scene.manager.keys;
        const targetMenuKey = menuKeys.find(k => registeredScenes[k]);

        if (targetMenuKey) {
            this.scene.start(targetMenuKey);
        } else {
            window.location.href = '/';
        }
    }
}
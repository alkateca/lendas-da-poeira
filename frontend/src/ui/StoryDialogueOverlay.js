import Phaser from 'phaser';
import PhantomUIKit from './PhantomUIKit.js';

export const COLOR_WING_THEMES = {
    RED:    { name: 'Ala da Forja e Cinzas',     hex: 0xe60012, textHex: '#f87171', badge: '🔥 FORJA & CINZAS' },
    BLUE:   { name: 'Ala dos Prismas e Altares', hex: 0x0284c7, textHex: '#38bdf8', badge: '💎 PRISMAS & ALTARES' },
    BLACK:  { name: 'Ala dos Ecos da Ruína',     hex: 0x7e22ce, textHex: '#c084fc', badge: '💀 ECOS DA RUÍNA' },
    YELLOW: { name: 'Ala da Imutabilidade',      hex: 0xca8a04, textHex: '#facc15', badge: '⚖️ IMUTABILIDADE' },
    GREEN:  { name: 'Ala da Simbiose Ancestral', hex: 0x16a34a, textHex: '#4ade80', badge: '🐺 SIMBIOSE ANCESTRAL' },
    WHITE:  { name: 'Registros de Poeira Pura',  hex: 0xe2e8f0, textHex: '#f8fafc', badge: '✨ ECO MEMORIAL' }
};

export default class StoryDialogueOverlay {
    constructor(scene) {
        this.scene = scene;
        this.dialogueQueue = [];
        this.isShowingBanner = false;
        this.activeModal = null;
        this.bannerContainer = null;
    }

    /**
     * Enfileira múltiplos diálogos in-game vindos de snapshot.pendingDialogues
     * ou de snapshot.triggerContext.scriptedDialogues
     */
    enqueueInCombatDialogues(dialogues = [], colorKey = 'WHITE') {
        if (!Array.isArray(dialogues) || dialogues.length === 0) return;

        dialogues.forEach(d => {
            this.dialogueQueue.push({
                speaker: d.speaker || 'Narrador dos Arquivos',
                dialogue: d.dialogue || d.text || '',
                trigger: d.trigger || 'EVENTO MEMORIAL',
                colorKey: d.color || colorKey
            });
        });

        if (!this.isShowingBanner) {
            this.processNextBannerInQueue();
        }
    }

    processNextBannerInQueue() {
        if (this.dialogueQueue.length === 0) {
            this.isShowingBanner = false;
            return;
        }

        this.isShowingBanner = true;
        const item = this.dialogueQueue.shift();
        const theme = COLOR_WING_THEMES[item.colorKey] || COLOR_WING_THEMES.WHITE;

        if (this.bannerContainer) {
            this.bannerContainer.destroy(true);
        }

        // Posicionado em Y: 185 para não cobrir a mão do oponente nem os slots de resolução (Y: 540)
        const cx = 960;
        const cy = 185;
        const boxW = 920;
        const boxH = 112;

        const container = this.scene.add.container(cx, cy - 40).setDepth(265).setAlpha(0);
        this.bannerContainer = container;

        // Sombra deslocada e placa principal P5
        const shadow = this.scene.add.rectangle(6, 6, boxW, boxH, theme.hex, 0.9);
        const bg = this.scene.add.rectangle(0, 0, boxW, boxH, 0x07090e, 0.96)
            .setStrokeStyle(2.5, 0xf8fafc);

        // Retrato / Emblema do Interlocutor à esquerda
        const portraitBg = this.scene.add.rectangle(-boxW / 2 + 58, 0, 88, 88, 0x111827, 1)
            .setStrokeStyle(2, theme.hex);
        const portraitIcon = this.scene.add.text(-boxW / 2 + 58, 0, '📜', {
            fontSize: '34px'
        }).setOrigin(0.5);

        // Cabeçalho do Interlocutor + Tag do Gatilho
        const speakerTxt = this.scene.add.text(-boxW / 2 + 118, -34, `// ${item.speaker.toUpperCase()}`, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '15px',
            color: theme.textHex,
            fontStyle: 'bold'
        }).setOrigin(0, 0.5).setResolution(2);

        const triggerBadge = this.scene.add.text(boxW / 2 - 18, -34, `[${item.trigger}]`, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '11px',
            color: '#94a3b8',
            fontStyle: 'bold'
        }).setOrigin(1, 0.5).setResolution(2);

        // Texto da fala
        const bodyTxt = this.scene.add.text(-boxW / 2 + 118, 8, `"${item.dialogue}"`, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '14px',
            color: '#f8fafc',
            fontStyle: 'italic',
            wordWrap: { width: boxW - 145 },
            lineSpacing: 3
        }).setOrigin(0, 0.5).setResolution(2);

        // Botão rápido de dispensar clicando no próprio banner
        bg.setInteractive({ useHandCursor: true }).on('pointerdown', () => {
            this.dismissCurrentBanner();
        });

        container.add([shadow, bg, portraitBg, portraitIcon, speakerTxt, triggerBadge, bodyTxt]);

        this.scene.tweens.add({
            targets: container,
            y: cy,
            alpha: 1,
            duration: 240,
            ease: 'Back.easeOut'
        });

        this.bannerTimer = this.scene.time.delayedCall(4600, () => {
            this.dismissCurrentBanner();
        });
    }

    dismissCurrentBanner() {
        if (this.bannerTimer) {
            this.bannerTimer.remove(false);
            this.bannerTimer = null;
        }
        if (!this.bannerContainer || !this.bannerContainer.scene) {
            this.processNextBannerInQueue();
            return;
        }

        const target = this.bannerContainer;
        this.bannerContainer = null;

        this.scene.tweens.add({
            targets: target,
            y: 145,
            alpha: 0,
            duration: 180,
            ease: 'Cubic.easeIn',
            onComplete: () => {
                target.destroy(true);
                this.processNextBannerInQueue();
            }
        });
    }

    /**
     * Abre um Modal Narrativo de Tela Cheia (Prólogo ou Epílogo) com paginação de parágrafos.
     */
    showFullNarrativeModal({
        headerTag = 'REGISTRO MEMORIAL // PRÓLOGO',
        title = '',
        colorKey = 'RED',
        narrativeText = '',
        confirmLabel = 'ENTRAR NA MEMÓRIA',
        onConfirm = null,
        onCancel = null
    }) {
        if (this.activeModal) {
            this.activeModal.destroy(true);
            this.activeModal = null;
        }

        const theme = COLOR_WING_THEMES[colorKey] || COLOR_WING_THEMES.WHITE;
        const { width, height } = this.scene.scale;
        const modal = this.scene.add.container(0, 0).setDepth(310);
        this.activeModal = modal;

        const paragraphs = String(narrativeText || '')
            .split(/\n\s*\n/)
            .map(p => p.trim())
            .filter(Boolean);
        if (paragraphs.length === 0) paragraphs.push('Registro ancestral pronto para sincronização.');

        let pageIndex = 0;

        const backdrop = this.scene.add.rectangle(width / 2, height / 2, width, height, 0x05070b, 0.88)
            .setInteractive();

        const panelW = 1040;
        const panelH = 620;
        const cx = width / 2;
        const cy = height / 2;

        const shadow = this.scene.add.rectangle(cx + 10, cy + 10, panelW, panelH, theme.hex, 1);
        const bg = this.scene.add.rectangle(cx, cy, panelW, panelH, 0x07090e, 0.98)
            .setStrokeStyle(3, 0xf8fafc);

        const topStripe = this.scene.add.rectangle(cx, cy - panelH / 2 + 36, panelW - 6, 66, 0x0f172a, 1);

        const tagTxt = this.scene.add.text(cx - panelW / 2 + 32, cy - panelH / 2 + 22, `// ${headerTag} • ${theme.badge}`, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '13px',
            color: theme.textHex,
            fontStyle: 'bold'
        }).setResolution(2);

        const titleTxt = this.scene.add.text(cx - panelW / 2 + 32, cy - panelH / 2 + 46, title.toUpperCase(), {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '24px',
            color: '#ffe600',
            fontStyle: 'bold'
        }).setResolution(2);

        // Moldura de Ilustração / Retrato do Arquivo
        const artBox = this.scene.add.rectangle(cx, cy - 85, panelW - 96, 170, 0x111827, 1)
            .setStrokeStyle(2, theme.hex);
        const artLabel = this.scene.add.text(cx, cy - 85, `🏛️ GRANDES ARQUIVOS DA POEIRA\n[ ${theme.name.toUpperCase()} ]`, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '16px',
            color: '#94a3b8',
            fontStyle: 'bold',
            align: 'center',
            lineSpacing: 6
        }).setOrigin(0.5).setResolution(2);

        // Caixa de Texto Narrativo
        const textBoxBg = this.scene.add.rectangle(cx, cy + 105, panelW - 96, 165, 0x0b111e, 0.95)
            .setStrokeStyle(1.5, 0x334155);

        const bodyTxt = this.scene.add.text(cx - (panelW - 140) / 2, cy + 40, '', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '16px',
            color: '#f1f5f9',
            wordWrap: { width: panelW - 140 },
            lineSpacing: 6
        }).setOrigin(0, 0).setResolution(2);

        const pageIndicator = this.scene.add.text(cx + (panelW - 120) / 2, cy + 168, '', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '12px',
            color: '#ffe600',
            fontStyle: 'bold'
        }).setOrigin(1, 0.5).setResolution(2);

        const updatePageContent = () => {
            bodyTxt.setText(paragraphs[pageIndex]);
            pageIndicator.setText(`PÁGINA ${pageIndex + 1} / ${paragraphs.length}`);
            if (actionBtn?.txt) {
                const isLast = pageIndex >= paragraphs.length - 1;
                actionBtn.txt.setText(isLast ? confirmLabel.toUpperCase() : 'PRÓXIMA PÁGINA >>');
            }
        };

        const actionBtn = PhantomUIKit.createRebelButton(
            this.scene,
            cx + (onCancel ? 150 : 0),
            cy + panelH / 2 - 54,
            340,
            52,
            paragraphs.length > 1 ? 'PRÓXIMA PÁGINA >>' : confirmLabel,
            theme.hex,
            () => {
                if (pageIndex < paragraphs.length - 1) {
                    pageIndex++;
                    updatePageContent();
                } else {
                    modal.destroy(true);
                    this.activeModal = null;
                    if (onConfirm) onConfirm();
                }
            }
        );

        const elements = [
            backdrop, shadow, bg, topStripe, tagTxt, titleTxt,
            artBox, artLabel, textBoxBg, bodyTxt, pageIndicator,
            actionBtn.container
        ];

        if (onCancel) {
            const cancelBtn = PhantomUIKit.createRebelButton(
                this.scene,
                cx - 180,
                cy + panelH / 2 - 54,
                250,
                52,
                'VOLTAR AOS ARQUIVOS',
                0x475569,
                () => {
                    modal.destroy(true);
                    this.activeModal = null;
                    onCancel();
                }
            );
            elements.push(cancelBtn.container);
        }

        modal.add(elements);
        updatePageContent();
        return modal;
    }

    destroy() {
        if (this.bannerTimer) this.bannerTimer.remove(false);
        if (this.bannerContainer) this.bannerContainer.destroy(true);
        if (this.activeModal) this.activeModal.destroy(true);
    }
}

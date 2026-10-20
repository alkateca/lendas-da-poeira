import Phaser from 'phaser';
import { BOARD_LAYOUT } from '../config/BoardCoordinates.js';

export default class HeroCardPrefab extends Phaser.GameObjects.Container {
    constructor(scene, x, y, heroDto, isOpponent) {
        super(scene, x, y);
        this.scene = scene;
        this.uuid = heroDto.uuid;
        this.isOpponent = isOpponent;
        this.heroData = heroDto;
        this.homePos = { x, y };
        this.dragStartPos = { x, y };
        this.isDragging = false;
        this.longHoverTimer = null;

        const { width, height } = BOARD_LAYOUT.MEDIUM_CARD;
        const teamColor = isOpponent ? BOARD_LAYOUT.COLORS.OPP_BLUE : BOARD_LAYOUT.COLORS.LOCAL_RED;

        // Sombra e Borda externa discreta (para indicar seleção/snap sem tapar o pergaminho)
        this.shadow = scene.add.rectangle(5, 7, width, height, 0x000000, 0.55);
        this.bg = scene.add.rectangle(0, 0, width + 4, height + 4, 0x000000, 0).setStrokeStyle(2, teamColor);

        // 1. Moldura Base de Pergaminho do Herói (modeloAlfa.jpeg - 747x1024)
        this.frameImage = scene.add.image(0, 0, 'card_frame_hero').setDisplaySize(width, height);

        // 2. Arte Customizada do Herói (Só aparece se o Herói tiver textura de arte própria carregada!)
        const artW = width * 0.905;
        const artH = height * 0.45;
        const artX = -width / 2 + (width * 0.046) + (artW / 2);
        const artY = -height / 2 + (height * 0.11) + (artH / 2);

        this.artSprite = null;
        const artKey = this.resolveHeroArtKey(heroDto);
        if (artKey) {
            this.artSprite = scene.add.image(artX, artY, artKey).setDisplaySize(artW, artH);
            this.artSprite.texture.setFilter(Phaser.Textures.FilterMode.NEAREST);
        }

        // 3. Nome do Herói em PRETO na barra metálica superior (igual ao Lua: finalY + 10/190)
        const nameY = -height / 2 + (height * (14 / 190));
        this.nameText = scene.add.text(0, nameY, heroDto.cardName, {
            fontSize: '9.5px',
            color: '#000000',
            fontStyle: 'bold',
            fontFamily: 'monospace',
            align: 'center',
            wordWrap: { width: width * 0.84 }
        }).setOrigin(0.5);

        // 4. Modificadores de Combate (DanoBonus verde, Reducao vermelho, Vulnerabilidade roxo)
        const modY = -height / 2 + (height * (100 / 190));
        this.modBonusText = scene.add.text(width * 0.18, modY, '', {
            fontSize: '8.5px', color: '#16a34a', fontStyle: 'bold', fontFamily: 'monospace'
        }).setOrigin(0.5);
        this.modReducaoText = scene.add.text(width * 0.29, modY, '', {
            fontSize: '8.5px', color: '#dc2626', fontStyle: 'bold', fontFamily: 'monospace'
        }).setOrigin(0.5);
        this.modVulnText = scene.add.text(width * 0.40, modY, '', {
            fontSize: '8.5px', color: '#9333ea', fontStyle: 'bold', fontFamily: 'monospace'
        }).setOrigin(0.5);

        // 5. Texto na Aba Rasgada Inferior Esquerda (largura máxima: 68% para não invadir a coluna direita!)
        const leftTextX = -width / 2 + (width * 0.06);
        const leftTextY = -height / 2 + (height * (132 / 190));
        this.descText = scene.add.text(leftTextX, leftTextY, heroDto.descricao || '', {
            fontSize: '7.5px',
            color: '#000000',
            fontFamily: 'monospace',
            align: 'left',
            wordWrap: { width: width * 0.65 }
        }).setOrigin(0, 0);

        // 6. Miniatura do Item Equipado no Canto Inferior Esquerdo (igual ao desenharMiniaturaHeroi do Lua)
        this.equippedMiniFrame = scene.add.image(
            -width / 2 + (width * (28 / 140)),
            height / 2 - (height * (18 / 190)),
            'card_frame_playable'
        ).setDisplaySize(width * (34 / 140), height * (42 / 190)).setVisible(false);

        this.metaText = scene.add.text(leftTextX, height / 2 - (height * (12 / 190)), '', {
            fontSize: '7.5px', color: '#3b2816', fontStyle: 'bold', fontFamily: 'monospace'
        }).setOrigin(0, 0.5);

        // 7. Os 4 Atributos em PRETO na Coluna Direita (Coordenadas exatas do Lua: 110/190, 130/190, 150/190, 170/190)
        const statRightX = -width / 2 + (width * (130 / 140));
        const espY = -height / 2 + (height * (115 / 190));
        const atqY = -height / 2 + (height * (135 / 190));
        const defY = -height / 2 + (height * (155 / 190));
        const hpY  = -height / 2 + (height * (175 / 190));

        this.espBadge = this.createStatNumber(statRightX, espY);
        this.atqBadge = this.createStatNumber(statRightX, atqY);
        this.defBadge = this.createStatNumber(statRightX, defY);
        this.hpBadge  = this.createStatNumber(statRightX, hpY);

        // 8. Ícones de Status do Lua (💤 e 💀)
        this.sleepStatusIcon = scene.add.text(0, -height * 0.05, '💤', { fontSize: '24px' }).setOrigin(0.5).setVisible(false);
        this.deadOverlayBg = scene.add.rectangle(0, 0, width, height, 0x000000, 0.35).setVisible(false);
        this.deadOverlayText = scene.add.text(0, -height * 0.05, '💀', { fontSize: '28px' }).setOrigin(0.5).setVisible(false);

        this.add([
            this.shadow, this.bg, this.frameImage,
            ...(this.artSprite ? [this.artSprite] : []),
            this.nameText,
            this.modBonusText, this.modReducaoText, this.modVulnText,
            this.descText, this.equippedMiniFrame, this.metaText,
            ...this.espBadge.elements, ...this.atqBadge.elements,
            ...this.defBadge.elements, ...this.hpBadge.elements,
            this.sleepStatusIcon, this.deadOverlayBg, this.deadOverlayText
        ]);

        this.setSize(width, height);
        this.setInteractive({ draggable: true, useHandCursor: true });

        this.setupDragAndHoverEvents();
        this.updateFromSnapshot(heroDto);
        scene.add.existing(this);
    }

    resolveHeroArtKey(heroDto) {
        if (!heroDto) return null;
        if (heroDto.imagem && this.scene.textures.exists(heroDto.imagem)) return heroDto.imagem;
        if (heroDto.cardName && this.scene.textures.exists(heroDto.cardName)) return heroDto.cardName;
        if (heroDto.uuid && this.scene.textures.exists(heroDto.uuid)) return heroDto.uuid;
        return null;
    }

    createStatNumber(rightEdgeX, y) {
        const val = this.scene.add.text(rightEdgeX, y, '0', {
            fontSize: '12px',
            color: '#000000',
            fontStyle: 'bold',
            fontFamily: 'monospace',
            align: 'right'
        }).setOrigin(1, 0.5);
        return { elements: [val], valueText: val };
    }

    getTargetPosture() {
        const inBattle = this.heroData.zoneId === 'BATTLE' ||
                         this.scene.selectionManager.selectedOwnHeroId === this.uuid ||
                         this.scene.selectionManager.selectedEnemyHeroId === this.uuid;

        if (this.heroData.estaVivo === false) {
            return { angle: 0, scaleX: 1.0, scaleY: 1.0 };
        }
        if (!inBattle && this.heroData.estaAtivo === false) {
            return { angle: 12, scaleX: 1.0, scaleY: 1.0 };
        }
        return { angle: 0, scaleX: 1.0, scaleY: 1.0 };
    }

    setupDragAndHoverEvents() {
        this.on('pointerover', () => {
            if (this.isDragging) return;
            this.setDepth(45);

            if (this.heroData.estaVivo !== false) {
                this.scene.tweens.killTweensOf(this);
                this.scene.tweens.add({
                    targets: this,
                    scaleX: 1.06,
                    scaleY: 1.06,
                    duration: 120
                });
            }

            this.clearLongHoverTimer();
            this.longHoverTimer = this.scene.time.delayedCall(600, () => {
                if (!this.isDragging) {
                    this.scene.showLargeCardPreview(this.heroData, true);
                }
            });
        });

        this.on('pointerout', () => {
            this.clearLongHoverTimer();
            this.scene.hideLargeCardPreview();
            if (!this.isDragging) {
                this.setDepth(10);
                const posture = this.getTargetPosture();
                this.scene.tweens.killTweensOf(this);
                this.scene.tweens.add({
                    targets: this,
                    angle: posture.angle,
                    scaleX: posture.scaleX,
                    scaleY: posture.scaleY,
                    duration: 160
                });
            }
        });

        this.on('dragstart', () => {
            this.clearLongHoverTimer();
            this.scene.hideLargeCardPreview();
            if (!this.scene.canDragHero(this)) return;
            this.isDragging = true;
            this.dragStartPos = { x: this.x, y: this.y };
            this.scene.tweens.killTweensOf(this);
            this.setAngle(0);
            this.setScale(1.08);
            this.setDepth(100);
            this.scene.highlightTargetBattleSlot(this.isOpponent, true);
        });

        this.on('drag', (pointer, dragX, dragY) => {
            if (!this.scene.canDragHero(this)) return;
            this.x = dragX;
            this.y = dragY;
            this.scene.checkMagneticHoverOnBattleSlot(this);
        });

        this.on('dragend', () => {
            if (!this.scene.canDragHero(this)) return;
            this.isDragging = false;
            this.scene.highlightTargetBattleSlot(this.isOpponent, false);

            const dragDist = Phaser.Math.Distance.Between(this.dragStartPos.x, this.dragStartPos.y, this.x, this.y);
            if (dragDist < 12) {
                this.scene.handleHeroClickSnap(this);
            } else {
                this.scene.handleHeroDrop(this);
            }
        });
    }

    clearLongHoverTimer() {
        if (this.longHoverTimer) {
            this.longHoverTimer.remove(false);
            this.longHoverTimer = null;
        }
    }

    snapToPosition(targetX, targetY, borderColor = null) {
        this.scene.tweens.killTweensOf(this);
        if (borderColor !== null) {
            this.bg.setStrokeStyle(4, borderColor);
        } else {
            const teamColor = this.isOpponent ? BOARD_LAYOUT.COLORS.OPP_BLUE : BOARD_LAYOUT.COLORS.LOCAL_RED;
            this.bg.setStrokeStyle(2, teamColor);
        }

        const posture = this.getTargetPosture();
        this.scene.tweens.add({
            targets: this,
            x: targetX,
            y: targetY,
            angle: posture.angle,
            scaleX: posture.scaleX,
            scaleY: posture.scaleY,
            duration: 280,
            ease: 'Back.easeOut'
        });
    }

    updateFromSnapshot(newHeroDto) {
        const prev = this.heroData || newHeroDto;
        this.heroData = newHeroDto;

        this.espBadge.valueText.setText(`${newHeroDto.espirito}`);
        this.atqBadge.valueText.setText(`${newHeroDto.ataque}`);
        this.defBadge.valueText.setText(`${newHeroDto.defesa}`);
        this.hpBadge.valueText.setText(`${newHeroDto.vidaAtual}`);

        const bonus = newHeroDto.danoBonus || 0;
        const reducao = newHeroDto.reducaoDano || 0;
        const vuln = newHeroDto.vulnerabilidade || 0;
        const showMods = (bonus !== 0 || reducao !== 0 || vuln !== 0);

        this.modBonusText.setText(showMods ? `${bonus}` : '');
        this.modReducaoText.setText(showMods ? `${reducao}` : '');
        this.modVulnText.setText(showMods ? `${vuln}` : '');

        const attachedCount = newHeroDto.attachedCards ? newHeroDto.attachedCards.length : 0;
        this.equippedMiniFrame.setVisible(attachedCount > 0);
        this.metaText.setText(attachedCount > 0 ? `x${attachedCount}` : '');

        const diffs = [];
        if (newHeroDto.espirito !== prev.espirito) {
            const d = newHeroDto.espirito - prev.espirito;
            diffs.push({ text: `${d > 0 ? '+' : ''}${d} ESP`, color: d > 0 ? '#c084fc' : '#f87171' });
        }
        if (newHeroDto.ataque !== prev.ataque) {
            const d = newHeroDto.ataque - prev.ataque;
            diffs.push({ text: `${d > 0 ? '+' : ''}${d} ATQ`, color: d > 0 ? '#fbbf24' : '#f87171' });
        }
        if (newHeroDto.defesa !== prev.defesa) {
            const d = newHeroDto.defesa - prev.defesa;
            diffs.push({ text: `${d > 0 ? '+' : ''}${d} DEF`, color: d > 0 ? '#60a5fa' : '#f87171' });
        }
        if (newHeroDto.vidaAtual !== prev.vidaAtual) {
            const d = newHeroDto.vidaAtual - prev.vidaAtual;
            diffs.push({ text: `${d > 0 ? '+' : ''}${d} HP`, color: d > 0 ? '#4ade80' : '#ff3355' });
        }

        diffs.forEach((item, idx) => {
            this.scene.time.delayedCall(idx * 220, () => {
                this.showFloatingNumber(item.text, item.color);
            });
        });

        const posture = this.getTargetPosture();
        this.scene.tweens.add({
            targets: this,
            angle: posture.angle,
            scaleX: posture.scaleX,
            scaleY: posture.scaleY,
            duration: 320,
            ease: 'Power2'
        });

        if (newHeroDto.estaVivo === false) {
            this.deadOverlayBg.setVisible(true);
            this.deadOverlayText.setVisible(true);
            this.sleepStatusIcon.setVisible(false);
            this.setAlpha(0.75);
            this.frameImage.setTint(0x555555);
            if (this.artSprite) this.artSprite.setTint(0x555555);
        } else {
            this.deadOverlayBg.setVisible(false);
            this.deadOverlayText.setVisible(false);
            this.frameImage.clearTint();
            if (this.artSprite) this.artSprite.clearTint();
            const inBattle = newHeroDto.zoneId === 'BATTLE';
            this.sleepStatusIcon.setVisible(!inBattle && newHeroDto.estaAtivo === false);
            this.setAlpha(newHeroDto.estaAtivo ? 1.0 : 0.88);
        }
    }

    showFloatingNumber(text, color) {
        const floatTxt = this.scene.add.text(this.x, this.y - 25, text, {
            fontSize: '22px', color: color, fontStyle: 'bold', stroke: '#000000', strokeThickness: 4
        }).setOrigin(0.5).setDepth(160);

        this.scene.tweens.add({
            targets: floatTxt,
            y: this.y - 85,
            alpha: 0,
            duration: 950,
            onComplete: () => floatTxt.destroy()
        });
    }

    destroy(fromScene) {
        this.clearLongHoverTimer();
        if (this.scene && this.scene.tweens) {
            this.scene.tweens.killTweensOf(this);
        }
        super.destroy(fromScene);
    }
}
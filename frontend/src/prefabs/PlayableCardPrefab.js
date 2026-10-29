import Phaser from 'phaser';
import { BOARD_LAYOUT, getCardVisualTheme } from '../config/BoardCoordinates.js';

export default class PlayableCardPrefab extends Phaser.GameObjects.Container {
    constructor(scene, x, y, cardDto, isDraggable = false, initialMode = 'COMPACT', homeAngle = 0) {
        super(scene, x, y);
        this.scene = scene;
        this.uuid = cardDto?.uuid || null;
        this.cardData = cardDto;
        this.homePos = { x, y };
        this.homeAngle = homeAngle;
        this.dragStartPos = { x, y };
        this.prevDragX = x;
        this.isDraggableCard = isDraggable;
        this.isDragging = false;
        this.isMarkedForDiscard = false;
        this.currentMode = initialMode;
        this.longHoverTimer = null;

        this.setAngle(homeAngle);

        const { width, height } = BOARD_LAYOUT.COMPACT_CARD;

        // Sombra dura e fundo claro cor de papel (#f5efe2) para preencher a janela caso a carta não tenha arte
        this.shadow = scene.add.rectangle(5, 7, width, height, 0x090d16, 0.75);
        this.bg = scene.add.rectangle(0, 0, width, height, 0xf5efe2, 1)
            .setStrokeStyle(0, BOARD_LAYOUT.COLORS.GOLD_DUST);

        // 1. Arte da Carta (Posiciona-se ATRÁS da moldura SVG vazada)
        this.artSprite = null;
        const artKey = this.resolveArtTextureKey(cardDto);
        if (artKey) {
            this.artSprite = scene.add.image(0, 0, artKey);
            this.artSprite.texture.setFilter(Phaser.Textures.FilterMode.NEAREST);
        }

        // 2. Moldura Clara SVG (card_frame_playable.svg)
        this.frameImage = scene.add.image(0, 0, 'card_frame_playable').setDisplaySize(width, height);

        this.headerColorBar = scene.add.rectangle(0, -height * 0.43, width * 0.91, height * 0.075, 0xffffff);

        // 3. Título em PRETO forte na barra branca superior
        this.titleText = scene.add.text(0, -height * 0.43, '', {
            fontSize: '10.5px',
            color: '#090d16',
            fontStyle: 'bold',
            fontFamily: 'IoskeleyMono, monospace',
            align: 'center',
            wordWrap: { width: width * 0.84 }
        }).setOrigin(0.5).setResolution(2);

        // 4. Faixa Central Amarela: Tipo e Empunhadura em PRETO
        this.typeBadge = scene.add.text(0, height * 0.06, '', {
            fontSize: '9px',
            color: '#090d16',
            fontStyle: 'bold',
            fontFamily: 'IoskeleyMono, monospace',
            align: 'center'
        }).setOrigin(0.5).setResolution(2);

        // 5. Caixa Clara Inferior: Descrição do Efeito em PRETO
        this.descText = scene.add.text(0, height * 0.25, '', {
            fontSize: '9.5px',
            color: '#090d16',
            fontStyle: 'bold',
            fontFamily: 'IoskeleyMono, monospace',
            align: 'center',
            lineSpacing: 1,
            wordWrap: { width: width * 0.82 }
        }).setOrigin(0.5).setResolution(2);

        // 6. Selo de Descarte
        this.discardBadge = scene.add.text(0, -height / 2 - 14, '✖ DESCARTAR', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '11px',
            color: '#ffffff',
            backgroundColor: '#e60012',
            padding: { x: 8, y: 4 },
            fontStyle: 'bold'
        }).setOrigin(0.5).setVisible(false).setResolution(2);

        // 7. Verso da Carta
        this.cardBackPattern = scene.add.image(0, 0, 'card_back').setDisplaySize(width, height);

        const children = [
            this.shadow,
            this.bg,
            ...(this.artSprite ? [this.artSprite] : []),
            this.frameImage,
            this.headerColorBar,
            this.titleText,
            this.typeBadge,
            this.descText,
            this.discardBadge,
            this.cardBackPattern
        ];
        this.add(children);

        this.setSize(width, height);
        this.setInteractive({ draggable: isDraggable, useHandCursor: true });

        this.setupHoverAndDragEvents();
        this.updateCardVisuals(cardDto);
        this.applySizeLayout(initialMode);
        scene.add.existing(this);
    }

    toCamelCaseKey(str) {
        if (!str || str === 'Carta Oculta') return null;
        return str
            .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
            .replace(/[^a-zA-Z0-9 ]/g, '')
            .split(' ')
            .filter(Boolean)
            .map((w, i) => i === 0 ? w.toLowerCase() : w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
            .join('');
    }

    resolveArtTextureKey(cardDto) {
        if (!cardDto || cardDto.faceUp === false) return null;
        if (cardDto.imagem && this.scene.textures.exists(cardDto.imagem)) return cardDto.imagem;
        if (cardDto.cardName && this.scene.textures.exists(cardDto.cardName)) return cardDto.cardName;
        const camelKey = this.toCamelCaseKey(cardDto.cardName);
        if (camelKey && this.scene.textures.exists(camelKey)) return camelKey;
        if (cardDto.uuid && this.scene.textures.exists(cardDto.uuid)) return cardDto.uuid;
        return null;
    }

    applySizeLayout(mode) {
        this.currentMode = mode;
        const isFaceUp = this.cardData && this.cardData.faceUp !== false;
        const dim = (mode === 'MEDIUM') ? BOARD_LAYOUT.MEDIUM_CARD : BOARD_LAYOUT.COMPACT_CARD;

        const pos = (svgX, svgY) => ({
            x: ((svgX / 280) - 0.5) * dim.width,
            y: ((svgY / 380) - 0.5) * dim.height
        });

        this.shadow.setSize(dim.width, dim.height);
        this.bg.setSize(dim.width, dim.height);
        this.frameImage.setDisplaySize(dim.width, dim.height);
        this.cardBackPattern.setDisplaySize(dim.width, dim.height);
        this.setSize(dim.width, dim.height);

        if (!isFaceUp) return;

        if (this.artSprite) {
            const artCenter = pos(137, 117);
            const artW = dim.width * (247 / 280);
            const artH = dim.height * (150 / 380);
            this.artSprite.setDisplaySize(artW, artH).setPosition(artCenter.x, artCenter.y);
        }

        const headerPt = pos(138, 22);
        this.headerColorBar
            .setPosition(headerPt.x, headerPt.y)
            .setSize(dim.width * (256 / 280), dim.height * (28 / 380));

        const typePt   = pos(138, 208);
        const descPt   = pos(138, 294);

        if (mode === 'MEDIUM') {
            this.titleText.setPosition(headerPt.x, headerPt.y).setStyle({
                fontSize: '10.5px',
                wordWrap: { width: dim.width * 0.86 }
            });
            this.typeBadge.setPosition(typePt.x, typePt.y).setStyle({
                fontSize: '8.5px'
            }).setVisible(true);
            this.descText.setPosition(descPt.x, descPt.y).setStyle({
                fontSize: '8.8px',
                wordWrap: { width: dim.width * 0.84 }
            }).setVisible(true);
            this.discardBadge.setPosition(0, -dim.height / 2 - 14);
        } else {
            this.titleText.setPosition(headerPt.x, headerPt.y).setStyle({
                fontSize: '9.5px',
                wordWrap: { width: dim.width * 0.86 }
            });
            this.typeBadge.setPosition(typePt.x, typePt.y).setStyle({
                fontSize: '7.5px'
            }).setVisible(true);
            this.descText.setPosition(descPt.x, descPt.y).setStyle({
                fontSize: '7.5px',
                wordWrap: { width: dim.width * 0.84 }
            }).setVisible(true);
            this.discardBadge.setPosition(0, -dim.height / 2 - 14);
        }
    }

    setupHoverAndDragEvents() {
        this.on('pointerover', () => {
            if (this.isDragging) return;
            const isFaceUp = this.cardData && this.cardData.faceUp !== false;
            if (!isFaceUp) return;

            this.setDepth(90);
            this.shadow.setPosition(10, 12).setFillStyle(0xe60012, 0.9);

            if (this.isDraggableCard) {
                this.applySizeLayout('MEDIUM');
                const isAllocatedInResolve = this.scene.selectionManager.playedCardSlotMap.has(this.uuid);
                const hoverY = isAllocatedInResolve ? this.y : (this.homePos.y - 54);

                this.scene.tweens.killTweensOf(this);
                this.scene.tweens.add({
                    targets: this,
                    y: hoverY,
                    angle: 0,
                    scaleX: 1.05,
                    scaleY: 1.05,
                    duration: 140,
                    ease: 'Back.easeOut'
                });
            }

            this.clearLongHoverTimer();
            this.longHoverTimer = this.scene.time.delayedCall(450, () => {
                if (!this.isDragging) {
                    this.scene.showLargeCardPreview(this.cardData, false);
                }
            });
        });

        this.on('pointerout', () => {
            this.clearLongHoverTimer();
            this.scene.hideLargeCardPreview();
            if (this.isDragging) return;

            this.setDepth(15);
            this.shadow.setPosition(5, 7).setFillStyle(0x090d16, 0.75);

            if (this.isDraggableCard) {
                const isAllocatedInResolve = this.scene.selectionManager.playedCardSlotMap.has(this.uuid);
                if (isAllocatedInResolve) {
                    this.applySizeLayout('MEDIUM');
                } else {
                    this.applySizeLayout('COMPACT');
                    const restY = this.isMarkedForDiscard ? (this.homePos.y - 30) : this.homePos.y;
                    this.scene.tweens.killTweensOf(this);
                    this.scene.tweens.add({
                        targets: this,
                        x: this.homePos.x,
                        y: restY,
                        angle: this.homeAngle,
                        scaleX: 1.0,
                        scaleY: 1.0,
                        duration: 160,
                        ease: 'Power2'
                    });
                }
            }
        });

        if (!this.isDraggableCard) return;

        this.on('dragstart', () => {
            this.clearLongHoverTimer();
            this.scene.hideLargeCardPreview();
            if (!this.scene.canDragHandCard()) return;
            this.isDragging = true;
            this.dragStartPos = { x: this.x, y: this.y };
            this.prevDragX = this.x;
            this.scene.tweens.killTweensOf(this);
            this.applySizeLayout('MEDIUM');
            this.setAngle(0);
            this.setDepth(130);
            this.scene.highlightValidDropZonesForHandCard(true);
        });

        this.on('drag', (pointer, dragX, dragY) => {
            if (!this.scene.canDragHandCard()) return;
            const vx = dragX - this.prevDragX;
            this.prevDragX = dragX;
            this.setAngle(Phaser.Math.Clamp(vx * 0.75, -16, 16));
            this.x = dragX;
            this.y = dragY;
            this.scene.checkMagneticHoverOnSlots(this);
        });

        this.on('dragend', () => {
            if (!this.scene.canDragHandCard()) return;
            this.isDragging = false;
            this.shadow.setPosition(5, 7).setFillStyle(0x090d16, 0.75);
            this.scene.highlightValidDropZonesForHandCard(false);

            const dragDist = Phaser.Math.Distance.Between(this.dragStartPos.x, this.dragStartPos.y, this.x, this.y);
            if (dragDist < 12) {
                this.scene.handleHandCardClickSnap(this);
            } else {
                this.scene.handleHandCardDrop(this);
            }
        });
    }

    clearLongHoverTimer() {
        if (this.longHoverTimer) {
            this.longHoverTimer.remove(false);
            this.longHoverTimer = null;
        }
    }

    snapToPosition(targetX, targetY, highlightColor = null, targetAngle = 0, sizeMode = 'COMPACT') {
        this.scene.tweens.killTweensOf(this);
        this.applySizeLayout(sizeMode);
        if (highlightColor && highlightColor !== BOARD_LAYOUT.COLORS.GOLD_DUST) {
            this.bg.setStrokeStyle(4, highlightColor);
        } else if (this.cardData && this.cardData.faceUp !== false) {
            const theme = getCardVisualTheme(this.cardData.color, this.cardData.rarity);
            this.bg.setStrokeStyle(2.5, theme.border);
        } else {
            this.bg.setStrokeStyle(0, 0x000000);
        }

        this.scene.tweens.add({
            targets: this,
            x: targetX,
            y: targetY,
            angle: targetAngle,
            scaleX: 1.0,
            scaleY: 1.0,
            duration: 260,
            ease: 'Back.easeOut'
        });
    }

    setDiscardHighlightInHand(isSelected) {
        this.isMarkedForDiscard = isSelected;
        this.discardBadge.setVisible(isSelected);

        const targetY = isSelected ? this.homePos.y - 30 : this.homePos.y;
        const strokeColor = isSelected ? 0xe60012 : null;
        this.snapToPosition(this.homePos.x, targetY, strokeColor, this.homeAngle, 'COMPACT');
    }

    updateCardVisuals(cardDto) {
        this.cardData = cardDto;
        const isFaceUp = cardDto && cardDto.faceUp !== false;

        if (!isFaceUp) {
            this.bg.setVisible(false);
            this.frameImage.setVisible(false);
            if (this.headerColorBar) this.headerColorBar.setVisible(false);
            if (this.artSprite) this.artSprite.setVisible(false);
            this.titleText.setVisible(false);
            this.typeBadge.setVisible(false);
            this.descText.setVisible(false);
            this.cardBackPattern.setVisible(true);
        } else {
            this.bg.setVisible(true);
            this.frameImage.setVisible(true);

            const theme = getCardVisualTheme(cardDto.color, cardDto.rarity);
            this.headerColorBar.setFillStyle(theme.headerBg, 1).setVisible(true);
            this.titleText.setColor(theme.textColor);
            this.bg.setStrokeStyle(2.5, theme.border);

            const artKey = this.resolveArtTextureKey(cardDto);
            if (artKey) {
                if (!this.artSprite) {
                    this.artSprite = this.scene.add.image(0, 0, artKey);
                    this.artSprite.texture.setFilter(Phaser.Textures.FilterMode.NEAREST);
                    // Insere ANTES de this.frameImage para ficar dentro da janela vazada!
                    const frameIdx = this.getIndex(this.frameImage);
                    this.addAt(this.artSprite, frameIdx);
                } else {
                    this.artSprite.setTexture(artKey);
                }
                this.artSprite.setVisible(true);
            } else if (this.artSprite) {
                this.artSprite.setVisible(false);
            }

            this.titleText.setText(cardDto.cardName || '').setVisible(true);

            const empInfo = (cardDto.itemType === 'ARMA' && cardDto.empunhadura > 0)
                ? ` • EMP:${cardDto.empunhadura}`
                : '';
            const subInfo = (cardDto.itemType && cardDto.itemType !== 'NENHUM')
                ? ` • ${cardDto.itemType}`
                : '';
            this.typeBadge.setText(`${cardDto.cardType || ''}${subInfo}${empInfo}`);
            this.descText.setText(cardDto.descricao || '');
            this.cardBackPattern.setVisible(false);

            this.applySizeLayout(this.currentMode);
        }
    }

    pulseSourceEffect() {
        if (!this.scene || !this.scene.tweens) return;
        this.scene.tweens.killTweensOf(this);
        this.bg.setStrokeStyle(4, 0x00ff88);
        this.scene.tweens.add({
            targets: this,
            scaleX: 1.2,
            scaleY: 1.2,
            yoyo: true,
            duration: 200,
            repeat: 1
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

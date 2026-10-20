import Phaser from 'phaser';
import { BOARD_LAYOUT } from '../config/BoardCoordinates.js';

export default class PlayableCardPrefab extends Phaser.GameObjects.Container {
    constructor(scene, x, y, cardDto, isDraggable = false, initialMode = 'COMPACT', homeAngle = 0) {
        super(scene, x, y);
        this.scene = scene;
        this.uuid = cardDto?.uuid || null;
        this.cardData = cardDto;
        this.homePos = { x, y };
        this.homeAngle = homeAngle;
        this.dragStartPos = { x, y };
        this.isDraggableCard = isDraggable;
        this.isDragging = false;
        this.isMarkedForDiscard = false;
        this.currentMode = initialMode;
        this.longHoverTimer = null;

        this.setAngle(homeAngle);

        const { width, height } = BOARD_LAYOUT.COMPACT_CARD;

        // Sombra e Borda externa transparente (usada apenas quando precisa brilhar em snap/pulse)
        this.shadow = scene.add.rectangle(4, 6, width, height, 0x000000, 0.55);
        this.bg = scene.add.rectangle(0, 0, width + 4, height + 4, 0x000000, 0)
            .setStrokeStyle(0, BOARD_LAYOUT.COLORS.GOLD_DUST);

        // 1. Moldura de Pergaminho (modeloAlfaCartas.jpeg) desenhada SEMPRE na base (igual ao partida.lua)
        this.frameImage = scene.add.image(0, 0, 'card_frame_playable').setDisplaySize(width, height);

        // 2. Arte da Carta (Sobrepõe apenas a janela superior do pergaminho se existir textura!)
        this.artSprite = null;
        const artKey = this.resolveArtTextureKey(cardDto);
        if (artKey) {
            this.artSprite = scene.add.image(0, 0, artKey);
            this.artSprite.texture.setFilter(Phaser.Textures.FilterMode.NEAREST);
        }

        // 3. Título na Barra Metálica Superior em PRETO (#000000)
        this.titleText = scene.add.text(0, -height * 0.43, '', {
            fontSize: '9px',
            color: '#000000',
            fontStyle: 'bold',
            fontFamily: 'monospace',
            align: 'center',
            wordWrap: { width: width * 0.82 }
        }).setOrigin(0.5);

        // 4. Textos na Aba Inferior Rasgada do Pergaminho (y = 275/380 no Lua -> ~72% da altura)
        this.typeBadge = scene.add.text(0, height * 0.21, '', {
            fontSize: '8px',
            color: '#3b2816',
            fontStyle: 'bold',
            fontFamily: 'monospace',
            align: 'center'
        }).setOrigin(0.5).setVisible(false);

        this.descText = scene.add.text(0, height * 0.32, '', {
            fontSize: '8.5px',
            color: '#000000',
            fontFamily: 'monospace',
            align: 'center',
            wordWrap: { width: width * 0.84 }
        }).setOrigin(0.5).setVisible(false);

        // 5. Selo de Descarte
        this.discardBadge = scene.add.text(0, -height / 2 - 14, '✖ DESCARTAR', {
            fontSize: '10px', color: '#ffffff', backgroundColor: '#dc2626', padding: { x: 6, y: 3 }, fontStyle: 'bold'
        }).setOrigin(0.5).setVisible(false);

        // 6. Verso da Carta Puro (modeloBaralho.jpg)
        this.cardBackPattern = scene.add.image(0, 0, 'card_back').setDisplaySize(width, height);

        const children = [
            this.shadow, this.bg, this.frameImage,
            ...(this.artSprite ? [this.artSprite] : []),
            this.titleText, this.typeBadge, this.descText,
            this.discardBadge, this.cardBackPattern
        ];
        this.add(children);

        this.setSize(width, height);
        this.setInteractive({ draggable: isDraggable, useHandCursor: true });

        this.setupHoverAndDragEvents();
        this.updateCardVisuals(cardDto);
        this.applySizeLayout(initialMode);
        scene.add.existing(this);
    }

    resolveArtTextureKey(cardDto) {
        if (!cardDto) return null;
        if (cardDto.imagem && this.scene.textures.exists(cardDto.imagem)) return cardDto.imagem;
        if (cardDto.cardName && this.scene.textures.exists(cardDto.cardName)) return cardDto.cardName;
        if (cardDto.uuid && this.scene.textures.exists(cardDto.uuid)) return cardDto.uuid;
        return null;
    }

    applySizeLayout(mode) {
        this.currentMode = mode;
        const isFaceUp = this.cardData && this.cardData.faceUp !== false;
        const dim = (mode === 'MEDIUM') ? BOARD_LAYOUT.MEDIUM_CARD : BOARD_LAYOUT.COMPACT_CARD;

        this.shadow.setSize(dim.width, dim.height);
        this.bg.setSize(dim.width + 4, dim.height + 4);
        this.frameImage.setDisplaySize(dim.width, dim.height);
        this.cardBackPattern.setDisplaySize(dim.width, dim.height);
        this.setSize(dim.width, dim.height);

        if (!isFaceUp) return;

        // Janela de arte idêntica ao desenharImagemCarta do partida.lua:
        // margemEsq = 4.6%, margemTopo = 11%, larguraArte = 90.5%, alturaArte = 56%
        if (this.artSprite) {
            const artW = dim.width * 0.905;
            const artH = dim.height * 0.56;
            const artCenterX = -dim.width / 2 + (dim.width * 0.046) + (artW / 2);
            const artCenterY = -dim.height / 2 + (dim.height * 0.11) + (artH / 2);
            this.artSprite.setDisplaySize(artW, artH).setPosition(artCenterX, artCenterY);
        }

        const headerY = -dim.height / 2 + (dim.height * (22 / 380));

        if (mode === 'MEDIUM') {
            this.titleText.setPosition(0, headerY).setStyle({
                fontSize: '9.5px',
                wordWrap: { width: dim.width * 0.82 }
            });
            this.typeBadge.setPosition(0, dim.height * 0.21).setVisible(true);
            this.descText.setPosition(0, dim.height * 0.33).setStyle({
                fontSize: '8px',
                wordWrap: { width: dim.width * 0.84 }
            }).setVisible(true);
            this.discardBadge.setPosition(0, -dim.height / 2 - 14);
        } else {
            this.titleText.setPosition(0, headerY).setStyle({
                fontSize: '8.5px',
                wordWrap: { width: dim.width * 0.82 }
            });
            this.typeBadge.setVisible(false);
            this.descText.setVisible(false);
            this.discardBadge.setPosition(0, -dim.height / 2 - 14);
        }
    }

    setupHoverAndDragEvents() {
        this.on('pointerover', () => {
            if (this.isDragging) return;
            const isFaceUp = this.cardData && this.cardData.faceUp !== false;
            if (!isFaceUp) return;

            this.setDepth(90);

            if (this.isDraggableCard) {
                this.applySizeLayout('MEDIUM');
                const isAllocatedInResolve = this.scene.selectionManager.playedCardSlotMap.has(this.uuid);
                const hoverY = isAllocatedInResolve ? this.y : (this.homePos.y - 48);

                this.scene.tweens.killTweensOf(this);
                this.scene.tweens.add({
                    targets: this,
                    y: hoverY,
                    angle: 0,
                    scaleX: 1.0,
                    scaleY: 1.0,
                    duration: 140,
                    ease: 'Power2'
                });
            }

            this.clearLongHoverTimer();
            this.longHoverTimer = this.scene.time.delayedCall(600, () => {
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
            this.scene.tweens.killTweensOf(this);
            this.applySizeLayout('MEDIUM');
            this.setAngle(0);
            this.setDepth(130);
            this.scene.highlightValidDropZonesForHandCard(true);
        });

        this.on('drag', (pointer, dragX, dragY) => {
            if (!this.scene.canDragHandCard()) return;
            this.x = dragX;
            this.y = dragY;
            this.scene.checkMagneticHoverOnSlots(this);
        });

        this.on('dragend', () => {
            if (!this.scene.canDragHandCard()) return;
            this.isDragging = false;
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
            this.bg.setStrokeStyle(3, highlightColor);
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
        const strokeColor = isSelected ? 0xff3333 : null;
        this.snapToPosition(this.homePos.x, targetY, strokeColor, this.homeAngle, 'COMPACT');
    }

    updateCardVisuals(cardDto) {
        this.cardData = cardDto;
        const isFaceUp = cardDto && cardDto.faceUp !== false;

        if (!isFaceUp) {
            this.frameImage.setVisible(false);
            if (this.artSprite) this.artSprite.setVisible(false);
            this.titleText.setVisible(false);
            this.typeBadge.setVisible(false);
            this.descText.setVisible(false);
            this.cardBackPattern.setVisible(true);
        } else {
            this.frameImage.setVisible(true);
            if (this.artSprite) this.artSprite.setVisible(true);
            this.titleText.setText(cardDto.cardName).setVisible(true);

            const empInfo = (cardDto.itemType === 'ARMA' && cardDto.empunhadura > 0)
                ? ` • Emp:${cardDto.empunhadura}`
                : '';
            this.typeBadge.setText(`${cardDto.cardType}${empInfo}`);
            this.descText.setText(cardDto.descricao || '');
            this.cardBackPattern.setVisible(false);
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
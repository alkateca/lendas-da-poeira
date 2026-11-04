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
        this.isHovered = false;
        this.isMarkedForDiscard = false;
        this.currentMode = initialMode;
        this.longHoverTimer = null;

        // Por padrão, NUNCA mostra descrição/subtipo na mão, banco ou fila de resolução (apenas no Hover lateral)
        this.showFullText = false;

        this.setAngle(homeAngle);

        const { width, height } = BOARD_LAYOUT.COMPACT_CARD;

        this.shadow = scene.add.rectangle(5, 7, width, height, 0x090d16, 0.75);
        this.bg = scene.add.rectangle(0, 0, width, height, 0xf5efe2, 1)
            .setStrokeStyle(0, BOARD_LAYOUT.COLORS.GOLD_DUST);

        // 1. Arte da Carta
        this.artSprite = null;
        const artKey = this.resolveArtTextureKey(cardDto);
        if (artKey) {
            this.artSprite = scene.add.image(0, 0, artKey);
            this.artSprite.texture.setFilter(Phaser.Textures.FilterMode.NEAREST);
        }

        // 2. Moldura SVG (Alterna entre card_frame_playable e card_frame_item)
        this.frameImage = scene.add.image(0, 0, 'card_frame_playable').setDisplaySize(width, height);

        // Barra de cor temática no topo
        this.headerColorBar = scene.add.rectangle(0, -height * 0.43, width * 0.91, height * 0.075, 0xffffff);

        // 3. Título no Topo (Sempre visível em cartas abertas)
        this.titleText = scene.add.text(0, -height * 0.43, '', {
            fontSize: '10px',
            color: '#090d16',
            fontStyle: 'bold',
            fontFamily: 'IoskeleyMono, monospace',
            align: 'center',
            wordWrap: { width: width * 0.84 }
        }).setOrigin(0.5).setResolution(2);

        // 4. Faixa de Tipo (Oculta na mão e na fila de resolução)
        this.typeBadge = scene.add.text(0, height * 0.06, '', {
            fontSize: '9px',
            color: '#4a3520',
            fontStyle: 'bold',
            fontFamily: 'IoskeleyMono, monospace',
            align: 'center'
        }).setOrigin(0.5).setResolution(2).setVisible(false);

        // 5. Caixa de Descrição (Oculta na mão e na fila de resolução)
        this.descText = scene.add.text(0, height * 0.25, '', {
            fontSize: '9.5px',
            color: '#090d16',
            fontStyle: 'bold',
            fontFamily: 'IoskeleyMono, monospace',
            align: 'left',
            lineSpacing: 1,
            wordWrap: { width: width * 0.82 }
        }).setOrigin(0, 0).setResolution(2).setVisible(false);

        // 6. Coluna Direita de Atributos para Itens/Equipamentos (+ESP, +ATQ, +DEF, EMP)
        const statBaseStyle = {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '12px',
            fontStyle: 'bold',
            align: 'right'
        };

        this.itemEspText = scene.add.text(0, 0, '-', { ...statBaseStyle, color: '#6b21a8' }).setOrigin(1, 0.5).setResolution(2).setVisible(false);
        this.itemAtqText = scene.add.text(0, 0, '-', { ...statBaseStyle, color: '#991b1b' }).setOrigin(1, 0.5).setResolution(2).setVisible(false);
        this.itemDefText = scene.add.text(0, 0, '-', { ...statBaseStyle, color: '#0369a1' }).setOrigin(1, 0.5).setResolution(2).setVisible(false);
        this.itemEmpText = scene.add.text(0, 0, '0', { ...statBaseStyle, color: '#fde047' }).setOrigin(0.5, 0.5).setResolution(2).setVisible(false);

        // 7. Trilha Vertical de 4 Etapas de Ativação (X = 253 | Y = 62, 98, 134, 170)
        this.phaseNodes = [0, 1, 2, 3].map(() => {
            const nodeBg = scene.add.rectangle(0, 0, 12, 10, 0x111827, 1)
                .setStrokeStyle(1.2, 0x475569)
                .setVisible(false);
            const nodeTxt = scene.add.text(0, 0, '', {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '7.5px',
                color: '#475569',
                fontStyle: 'bold'
            }).setOrigin(0.5).setResolution(2).setVisible(false);
            return { bg: nodeBg, txt: nodeTxt };
        });

        // 8. Selo de Descarte e Verso da Carta
        this.discardBadge = scene.add.text(0, -height / 2 - 14, '✖ DESCARTAR', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '11px',
            color: '#ffffff',
            backgroundColor: '#e60012',
            padding: { x: 8, y: 4 },
            fontStyle: 'bold'
        }).setOrigin(0.5).setVisible(false).setResolution(2);

        this.cardBackPattern = scene.add.image(0, 0, 'card_back').setDisplaySize(width, height);

        const phaseElements = [];
        this.phaseNodes.forEach(n => phaseElements.push(n.bg, n.txt));

        this.add([
            this.shadow,
            this.bg,
            ...(this.artSprite ? [this.artSprite] : []),
            this.frameImage,
            this.headerColorBar,
            this.titleText,
            this.typeBadge,
            this.descText,
            this.itemEspText,
            this.itemAtqText,
            this.itemDefText,
            this.itemEmpText,
            ...phaseElements,
            this.discardBadge,
            this.cardBackPattern
        ]);

        this.setSize(width, height);
        this.setInteractive({ draggable: isDraggable, useHandCursor: true });

        this.setupHoverAndDragEvents();
        this.on('destroy', () => this.clearLongHoverTimer());
        this.updateCardVisuals(cardDto);
        this.applySizeLayout(initialMode);
        scene.add.existing(this);
    }

    /**
     * Ativado apenas nas cartas abertas dentro do painel de Hover ao lado do Herói.
     */
    setShowFullText(show) {
        this.showFullText = Boolean(show);
        this.applySizeLayout(this.currentMode);
    }

    isEquipmentLayout(cardDto) {
        if (!cardDto) return false;
        if (cardDto.equipable) return true;
        if (cardDto.cardType === 'ITEM') return true;
        if (cardDto.itemType && cardDto.itemType !== 'NENHUM') return true;
        if (cardDto.zoneId === 'ATTACHED' || cardDto.attachedToCardId) return true;
        return false;
    }

    static resolveActivePhaseNodes(cardOrHeroDto) {
        if (Array.isArray(cardOrHeroDto?.activePhaseKeys) && cardOrHeroDto.activePhaseKeys.length > 0) {
            const activeSet = new Set(cardOrHeroDto.activePhaseKeys);
            return [
                { key: 'ON_PLAY',      svgX: 253, svgY: 62,  icon: '⚡', color: 0x22c55e, textColor: '#07090e', active: activeSet.has('ON_PLAY') },
                { key: 'COMBAT_START', svgX: 253, svgY: 98,  icon: '☀', color: 0x00f0ff, textColor: '#07090e', active: activeSet.has('COMBAT_START') },
                { key: 'AURA_COMBAT',  svgX: 253, svgY: 134, icon: '♾', color: 0xffe600, textColor: '#07090e', active: activeSet.has('AURA_COMBAT') },
                { key: 'COMBAT_END',   svgX: 253, svgY: 170, icon: '⏳', color: 0xe60012, textColor: '#ffffff', active: activeSet.has('COMBAT_END') }
            ];
        }

        const desc = String(cardOrHeroDto?.descricao || '');
        return [
            {
                key: 'ON_PLAY',
                svgX: 253, svgY: 62,
                icon: '⚡', color: 0x22c55e, textColor: '#07090e',
                active: /Ao jogar|Ao equipar|Quando jogad[ao]/i.test(desc)
            },
            {
                key: 'COMBAT_START',
                svgX: 253, svgY: 98,
                icon: '☀', color: 0x00f0ff, textColor: '#07090e',
                active: /In[íi]cio do Combate|In[íi]cio da Partida/i.test(desc)
            },
            {
                key: 'AURA_COMBAT',
                svgX: 253, svgY: 134,
                icon: '♾', color: 0xffe600, textColor: '#07090e',
                active: /Aura|Passiv[ao]|Durante o Combate|Ataque B[áa]sico/i.test(desc)
            },
            {
                key: 'COMBAT_END',
                svgX: 253, svgY: 170,
                icon: '⏳', color: 0xe60012, textColor: '#ffffff',
                active: /Final do Combate|Fim do Combate/i.test(desc)
            }
        ];
    }

    static extractItemStatBonuses(cardDto) {
        if (!cardDto) return { esp: 0, atq: 0, def: 0, emp: 0, cleanDesc: '' };

        const rawDesc = cardDto.descricao || '';
        const passiveLines = rawDesc
            .split('\n')
            .filter(line => !/^(In[íi]cio d[eo]|Final do|Fim do)|at[ée] o Final do Combate/i.test(line.trim()))
            .join(' ');

        const matchStat = (regex) => {
            const m = passiveLines.match(regex);
            return m ? parseInt(m[1], 10) : 0;
        };

        const esp = cardDto.bonusEspirito ?? cardDto.espirito ?? matchStat(/\+(\d+)\s*(?:de\s+)?Esp[íi]rito/i);
        const atq = cardDto.bonusAtaque   ?? cardDto.ataque   ?? matchStat(/\+(\d+)\s*(?:de\s+)?Ataque/i);
        const def = cardDto.bonusDefesa   ?? cardDto.defesa   ?? matchStat(/\+(\d+)\s*(?:de\s+)?Defesa/i);
        const emp = cardDto.empunhadura ?? 0;

        return { esp, atq, def, emp, cleanDesc: rawDesc };
    }

    toCamelCaseKey(str) {
        if (!str || str === 'Carta Oculta') return null;
        const camel = str
            .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
            .replace(/[^a-zA-Z0-9 ]/g, '')
            .split(' ')
            .filter(Boolean)
            .map((w, i) => i === 0 ? w.toLowerCase() : w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
            .join('');
        const aliases = {
            brocheDeCristal: 'brocheCristal',
            dragaoDeCristal: 'dragaoCristal'
        };
        return aliases[camel] || camel;
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
        const isItem = this.isEquipmentLayout(this.cardData);
        const shouldShowText = Boolean(this.showFullText);

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
            if (isItem) {
                const artCenter = pos(123, 117);
                const artW = dim.width * (218 / 280);
                const artH = dim.height * (150 / 380);
                this.artSprite.setDisplaySize(artW, artH).setPosition(artCenter.x, artCenter.y);
            } else {
                const artCenter = pos(137, 117);
                const artW = dim.width * (248 / 280);
                const artH = dim.height * (150 / 380);
                this.artSprite.setDisplaySize(artW, artH).setPosition(artCenter.x, artCenter.y);
            }
        }

        const headerPt = pos(138, 21);
        this.headerColorBar
            .setPosition(headerPt.x, headerPt.y)
            .setSize(dim.width * (252 / 280), dim.height * (28 / 380));

        this.titleText.setPosition(headerPt.x, headerPt.y).setStyle({
            fontSize: mode === 'MEDIUM' ? '10px' : '9px',
            wordWrap: { width: dim.width * 0.84 }
        });

        if (isItem) {
            const typePt = pos(110, 214);
            const descPt = pos(14, 236);
            const textMaxW = dim.width * (192 / 280);

            // Textos de subtipo e descrição ficam OCULTOS na mão, banco e fila de resolução
            this.typeBadge.setOrigin(0.5, 0.5).setPosition(typePt.x, typePt.y).setStyle({
                fontSize: mode === 'MEDIUM' ? '8px' : '7px'
            }).setVisible(shouldShowText);

            this.descText.setOrigin(0, 0).setPosition(descPt.x, descPt.y).setStyle({
                fontSize: mode === 'MEDIUM' ? '8px' : '7px',
                align: 'left',
                wordWrap: { width: textMaxW }
            }).setVisible(shouldShowText);

            // Status laterais (+ESP, +ATQ, +DEF, EMP) e Trilha de Fases continuam visíveis nos Itens
            const espPt = pos(264, 224);
            const atqPt = pos(264, 267);
            const defPt = pos(264, 310);
            const empPt = pos(243, 353);
            const statFont = mode === 'MEDIUM' ? '11.5px' : '10px';

            this.itemEspText.setPosition(espPt.x, espPt.y).setFontSize(statFont).setVisible(true);
            this.itemAtqText.setPosition(atqPt.x, atqPt.y).setFontSize(statFont).setVisible(true);
            this.itemDefText.setPosition(defPt.x, defPt.y).setFontSize(statFont).setVisible(true);
            this.itemEmpText.setPosition(empPt.x, empPt.y).setFontSize(mode === 'MEDIUM' ? '10.5px' : '9px').setVisible(true);

            const phaseStates = PlayableCardPrefab.resolveActivePhaseNodes(this.cardData);
            const boxW = dim.width * (22 / 280);
            const boxH = dim.height * (18 / 380);
            const nodeFont = mode === 'MEDIUM' ? '7.5px' : '6.5px';

            this.phaseNodes.forEach((node, idx) => {
                const st = phaseStates[idx];
                const pt = pos(st.svgX, st.svgY);
                node.bg
                    .setPosition(pt.x, pt.y)
                    .setSize(boxW, boxH)
                    .setFillStyle(st.active ? st.color : 0x111827, 1)
                    .setStrokeStyle(st.active ? 1.5 : 1, st.active ? 0xffffff : 0x475569)
                    .setVisible(true);
                node.txt
                    .setPosition(pt.x, pt.y)
                    .setText(st.icon)
                    .setFontSize(nodeFont)
                    .setColor(st.active ? st.textColor : '#475569')
                    .setVisible(true);
            });
        } else {
            const typePt = pos(138, 208);
            const descPt = pos(18, 232);

            // Textos de subtipo e descrição ficam OCULTOS na mão e na fila de resolução
            this.typeBadge.setOrigin(0.5, 0.5).setPosition(typePt.x, typePt.y).setStyle({
                fontSize: mode === 'MEDIUM' ? '8.5px' : '7.5px'
            }).setVisible(shouldShowText);

            this.descText.setOrigin(0, 0).setPosition(descPt.x, descPt.y).setStyle({
                fontSize: mode === 'MEDIUM' ? '8.5px' : '7.5px',
                align: 'left',
                wordWrap: { width: dim.width * (244 / 280) }
            }).setVisible(shouldShowText);

            this.itemEspText.setVisible(false);
            this.itemAtqText.setVisible(false);
            this.itemDefText.setVisible(false);
            this.itemEmpText.setVisible(false);
            this.phaseNodes.forEach(node => {
                node.bg.setVisible(false);
                node.txt.setVisible(false);
            });
        }

        this.discardBadge.setPosition(0, -dim.height / 2 - 14);
    }

    setupHoverAndDragEvents() {
        this.on('pointerover', () => {
            if (this.isDragging) return;
            const isFaceUp = this.cardData && this.cardData.faceUp !== false;
            if (!isFaceUp) return;

            this.isHovered = true;
            this.setDepth(90);
            this.shadow.setPosition(10, 12).setFillStyle(0xc59b27, 0.85);

            if (this.isDraggableCard) {
                this.applySizeLayout('MEDIUM');
                const isAllocatedInResolve = this.scene.selectionManager.playedCardSlotMap.has(this.uuid);
                const hoverY = isAllocatedInResolve ? this.y : (this.homePos.y - 54);

                this.scene.tweens.killTweensOf(this);
                this.scene.tweens.add({
                    targets: this,
                    y: hoverY,
                    angle: 0,
                    scaleX: 1.08,
                    scaleY: 1.08,
                    duration: 140,
                    ease: 'Back.easeOut'
                });
            } else {
                this.scene.tweens.killTweensOf(this);
                this.scene.tweens.add({
                    targets: this,
                    scaleX: 1.08,
                    scaleY: 1.08,
                    duration: 140,
                    ease: 'Back.easeOut'
                });
            }

            this.clearLongHoverTimer();
            this.longHoverTimer = this.scene.time.delayedCall(160, () => {
                if (!this.isDragging && this.isHovered) {
                    this.scene.showLargeCardPreview(this.cardData, false);
                }
            });
        });

        this.on('pointerout', () => {
            this.isHovered = false;
            this.clearLongHoverTimer();
            this.scene.hideLargeCardPreview();
            if (this.isDragging) return;

            this.setDepth(15);
            this.shadow.setPosition(5, 7).setFillStyle(0x090d16, 0.75);

            if (this.isDraggableCard) {
                const isAllocatedInResolve = this.scene.selectionManager.playedCardSlotMap.has(this.uuid);
                if (isAllocatedInResolve) {
                    this.applySizeLayout('MEDIUM');
                    this.scene.tweens.killTweensOf(this);
                    this.scene.tweens.add({
                        targets: this,
                        scaleX: 1.0,
                        scaleY: 1.0,
                        duration: 150,
                        ease: 'Power2'
                    });
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
            } else {
                this.scene.tweens.killTweensOf(this);
                this.scene.tweens.add({
                    targets: this,
                    scaleX: 1.0,
                    scaleY: 1.0,
                    duration: 150,
                    ease: 'Power2'
                });
            }
        });

        if (!this.isDraggableCard) return;

        this.on('dragstart', () => {
            this.isHovered = false;
            this.clearLongHoverTimer();
            this.scene.hideLargeCardPreview();
            if (!this.scene.canDragHandCard()) return;
            this.isDragging = true;
            this.dragStartPos = { x: this.x, y: this.y };
            this.prevDragX = this.x;
            this.scene.tweens.killTweensOf(this);
            this.applySizeLayout('MEDIUM');
            this.setAngle(0);
            this.setScale(1.08);
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

        const isAllocatedInResolve = this.scene?.selectionManager?.playedCardSlotMap?.has(this.uuid);
        const keepHandHover = this.isHovered && !this.isDragging && this.isDraggableCard && !isAllocatedInResolve;

        this.applySizeLayout(keepHandHover ? 'MEDIUM' : sizeMode);

        if (highlightColor && highlightColor !== BOARD_LAYOUT.COLORS.GOLD_DUST) {
            this.bg.setStrokeStyle(4, highlightColor);
        } else {
            const theme = getCardVisualTheme(this.cardData?.color, this.cardData?.rarity);
            this.bg.setStrokeStyle(2.5, theme.border);
        }

        const finalY = keepHandHover ? (targetY - 54) : targetY;
        const finalAngle = keepHandHover ? 0 : targetAngle;
        const finalScale = (this.isHovered && !this.isDragging) ? 1.08 : 1.0;

        this.scene.tweens.add({
            targets: this,
            x: targetX,
            y: finalY,
            angle: finalAngle,
            scaleX: finalScale,
            scaleY: finalScale,
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
            this.headerColorBar.setVisible(false);
            if (this.artSprite) this.artSprite.setVisible(false);
            this.titleText.setVisible(false);
            this.typeBadge.setVisible(false);
            this.descText.setVisible(false);
            this.itemEspText.setVisible(false);
            this.itemAtqText.setVisible(false);
            this.itemDefText.setVisible(false);
            this.itemEmpText.setVisible(false);
            this.phaseNodes.forEach(node => {
                node.bg.setVisible(false);
                node.txt.setVisible(false);
            });
            this.cardBackPattern.setVisible(true);
            return;
        }

        this.bg.setVisible(true);
        this.frameImage.setVisible(true);

        const isItem = this.isEquipmentLayout(cardDto);
        const targetFrameKey = (isItem && this.scene.textures.exists('card_frame_item'))
            ? 'card_frame_item'
            : 'card_frame_playable';
        this.frameImage.setTexture(targetFrameKey);

        const theme = getCardVisualTheme(cardDto.color, cardDto.rarity);
        this.headerColorBar.setFillStyle(theme.headerBg, 1).setVisible(true);
        this.titleText.setColor(theme.textColor).setText(cardDto.cardName || '').setVisible(true);
        this.bg.setStrokeStyle(2.5, theme.border);

        const artKey = this.resolveArtTextureKey(cardDto);
        if (artKey) {
            if (!this.artSprite) {
                this.artSprite = this.scene.add.image(0, 0, artKey);
                this.artSprite.texture.setFilter(Phaser.Textures.FilterMode.NEAREST);
                const frameIdx = this.getIndex(this.frameImage);
                this.addAt(this.artSprite, frameIdx);
            } else {
                this.artSprite.setTexture(artKey);
            }
            this.artSprite.setVisible(true);
        } else if (this.artSprite) {
            this.artSprite.setVisible(false);
        }

        const subInfo = (cardDto.itemType && cardDto.itemType !== 'NENHUM')
            ? ` • ${cardDto.itemType}` : '';

        if (isItem) {
            const stats = PlayableCardPrefab.extractItemStatBonuses(cardDto);
            this.typeBadge.setText(`${cardDto.cardType || 'ITEM'}${subInfo}`);
            this.descText.setText(stats.cleanDesc);
            this.itemEspText.setText(stats.esp > 0 ? `+${stats.esp}` : '-');
            this.itemAtqText.setText(stats.atq > 0 ? `+${stats.atq}` : '-');
            this.itemDefText.setText(stats.def > 0 ? `+${stats.def}` : '-');
            this.itemEmpText.setText(String(stats.emp || 0));
        } else {
            this.typeBadge.setText(`${cardDto.cardType || ''}${subInfo}`);
            this.descText.setText(cardDto.descricao || '');
        }

        this.cardBackPattern.setVisible(false);
        this.applySizeLayout(this.currentMode);
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

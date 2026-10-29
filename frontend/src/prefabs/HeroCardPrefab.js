import Phaser from 'phaser';
import { BOARD_LAYOUT, getCardVisualTheme } from '../config/BoardCoordinates.js';
import PlayableCardPrefab from './PlayableCardPrefab.js';

export default class HeroCardPrefab extends Phaser.GameObjects.Container {
    constructor(scene, x, y, heroDto, isOpponent) {
        super(scene, x, y);
        this.scene = scene;
        this.uuid = heroDto.uuid;
        this.isOpponent = isOpponent;
        this.heroData = heroDto;
        this.homePos = { x, y };
        this.dragStartPos = { x, y };
        this.prevDragX = x;
        this.isDragging = false;
        this.longHoverTimer = null;

        const { width, height } = BOARD_LAYOUT.MEDIUM_CARD; // 140 x 190
        this.cardW = width;
        this.cardH = height;

        // Converte coordenadas do SVG (280x380) para coordenadas locais do Container (-width/2 .. +width/2)
        const pos = (svgX, svgY) => ({
            x: ((svgX / 280) - 0.5) * width,
            y: ((svgY / 380) - 0.5) * height
        });

        const teamColor = isOpponent ? BOARD_LAYOUT.COLORS.OPP_BLUE : BOARD_LAYOUT.COLORS.LOCAL_RED;

        this.shadow = scene.add.rectangle(4, 6, width, height, 0x090d16, 0.65);

        // Fundo suave cor de pergaminho (evita aquele bloco vermelho berrante quando não há arte)
        this.bg = scene.add.rectangle(0, 0, width, height, 0xd6cbb5, 1)
            .setStrokeStyle(3, teamColor);

        // 1. Arte do Herói na janela (x: 12..261, y: 41..192 -> centro 136.5, 116.5)
        const artCenter = pos(137, 117);
        const artW = width * (248 / 280);
        const artH = height * (150 / 380);

        this.artSprite = null;
        const artKey = this.resolveHeroArtKey(heroDto);
        if (artKey) {
            this.artSprite = scene.add.image(artCenter.x, artCenter.y, artKey).setDisplaySize(artW, artH);
            this.artSprite.texture.setFilter(Phaser.Textures.FilterMode.NEAREST);
        }

        // 2. Moldura SVG (280x380 escalada para width x height)
        this.frameImage = scene.add.image(0, 0, 'card_frame_hero').setDisplaySize(width, height);

        const headerBarPt = pos(136, 21);
        this.headerColorBar = scene.add.rectangle(
            headerBarPt.x,
            headerBarPt.y,
            width * (256 / 280),
            height * (28 / 380),
            0xf8fafc
        );

        // 3. Nome do Herói no Topo (Centro SVG: X=132, Y=21)
        const namePt = pos(132, 21);
        this.nameText = scene.add.text(namePt.x, namePt.y, heroDto.cardName, {
            fontSize: '10px',
            color: '#090d16',
            fontStyle: 'bold',
            fontFamily: 'IoskeleyMono, monospace',
            align: 'center',
            wordWrap: { width: width * 0.82 }
        }).setOrigin(0.5).setResolution(2);

        // 4. Modificadores de Dano Centralizados nas 3 Casas (SVG Y=174 | X=186, 220, 254)
        const mod1 = pos(186, 174);
        const mod2 = pos(220, 174);
        const mod3 = pos(254, 174);

        this.modBonusText = scene.add.text(mod1.x, mod1.y, '0', {
            fontSize: '11px', color: '#15803d', fontStyle: 'bold', fontFamily: 'IoskeleyMono, monospace'
        }).setOrigin(0.5).setResolution(2);

        this.modReducaoText = scene.add.text(mod2.x, mod2.y, '0', {
            fontSize: '11px', color: '#dc2626', fontStyle: 'bold', fontFamily: 'IoskeleyMono, monospace'
        }).setOrigin(0.5).setResolution(2);

        this.modVulnText = scene.add.text(mod3.x, mod3.y, '0', {
            fontSize: '11px', color: '#7e22ce', fontStyle: 'bold', fontFamily: 'IoskeleyMono, monospace'
        }).setOrigin(0.5).setResolution(2);

        // 5. Caixa de Texto Inferior Esquerda (Começa no topo da caixa em SVG X=14, Y=201)
        const traitsPt = pos(14, 201);
        const descPt = pos(14, 238);
        this.descBasePos = descPt;
        const textMaxW = width * (194 / 280);

        this.traitsText = scene.add.text(traitsPt.x, traitsPt.y, '', {
            fontSize: '7.5px',
            color: '#4a3520',
            fontStyle: 'bold',
            fontFamily: 'IoskeleyMono, monospace',
            align: 'left',
            wordWrap: { width: textMaxW }
        }).setOrigin(0, 0).setResolution(2);

        this.descText = scene.add.text(descPt.x, descPt.y, heroDto.descricao || '', {
            fontSize: '8px',
            color: '#090d16',
            fontStyle: 'bold',
            fontFamily: 'IoskeleyMono, monospace',
            align: 'left',
            lineSpacing: 1,
            wordWrap: { width: textMaxW }
        }).setOrigin(0, 0).setResolution(2);

        // 6. Camadas de Cartas Anexadas (Traseira em repouso / Frontal em Hover)
        this.attachedBackLayer = scene.add.container(0, 0);
        this.attachedFrontLayer = scene.add.container(0, 0);
        this.attachedCardNodes = [];
        this.knownAttachedUuids = new Set();

        // 7. Os 4 Atributos na Coluna Direita (SVG X=264, Centros Y = 224, 267, 310, 353)
        const espPt = pos(264, 224);
        const atqPt = pos(264, 267);
        const defPt = pos(264, 310);
        const hpPt  = pos(264, 353);

        this.espBadge = this.createStatNumber(espPt.x, espPt.y, '12px', '#090d16');
        this.atqBadge = this.createStatNumber(atqPt.x, atqPt.y, '12px', '#090d16');
        this.defBadge = this.createStatNumber(defPt.x, defPt.y, '12px', '#090d16');
        this.hpBadge  = this.createStatNumber(hpPt.x, hpPt.y, '13px', '#b91c1c');

        // 8. Ícones de Status (💤 e 💀)
        this.sleepStatusIcon = scene.add.text(0, -height * 0.12, '💤', { fontSize: '24px' }).setOrigin(0.5).setVisible(false);
        this.deadOverlayBg = scene.add.rectangle(0, 0, width, height, 0x090d16, 0.5).setVisible(false);
        this.deadOverlayText = scene.add.text(0, -height * 0.12, '💀', { fontSize: '30px' }).setOrigin(0.5).setVisible(false);

        this.add([
            this.attachedBackLayer, // Fica ATRÁS do Herói em repouso
            this.shadow,
            this.bg,
            ...(this.artSprite ? [this.artSprite] : []),
            this.frameImage,
            this.headerColorBar,
            this.nameText,
            this.modBonusText, this.modReducaoText, this.modVulnText,
            this.traitsText, this.descText,
            ...this.espBadge.elements, ...this.atqBadge.elements,
            ...this.defBadge.elements, ...this.hpBadge.elements,
            this.sleepStatusIcon, this.deadOverlayBg, this.deadOverlayText,
            this.attachedFrontLayer // Fica NA FRENTE do Herói quando um anexo recebe hover
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

    formatHeroTraits(heroDto) {
        const cap = (s) => {
            if (!s) return '';
            const clean = String(s).replace(/_/g, ' ').trim();
            return clean.charAt(0).toUpperCase() + clean.slice(1).toLowerCase();
        };
        const list = (val, prefix = '') => {
            if (!val) return [];
            const arr = Array.isArray(val) ? val : [val];
            return arr.filter(Boolean).map(v => `${prefix}${cap(v)}`);
        };
        return [
            ...list(heroDto.classe),
            ...list(heroDto.ordem),
            ...list(heroDto.racas || heroDto.raca),
            ...list(heroDto.afinidades || heroDto.afinidade, 'Afinidade ')
        ].join(' - ');
    }

    createStatNumber(rightEdgeX, y, fontSize = '12px', color = '#090d16') {
        const val = this.scene.add.text(rightEdgeX, y, '0', {
            fontSize: fontSize,
            color: color,
            fontStyle: 'bold',
            fontFamily: 'IoskeleyMono, monospace',
            align: 'right'
        }).setOrigin(1, 0.5).setResolution(2);
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
            this.longHoverTimer = this.scene.time.delayedCall(350, () => {
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

        this.on('pointerup', () => {
            if (this.isDragging) return;
            if (!this.scene.canDragHero(this) && this.heroData?.attachedCards?.length > 0) {
                this.scene.openAttachedCardsModal(this.heroData);
            }
        });

        this.on('dragstart', () => {
            this.clearLongHoverTimer();
            this.scene.hideLargeCardPreview();
            if (!this.scene.canDragHero(this)) return;
            this.isDragging = true;
            this.dragStartPos = { x: this.x, y: this.y };
            this.prevDragX = this.x;
            this.scene.tweens.killTweensOf(this);
            this.setAngle(0);
            this.setScale(1.08);
            this.setDepth(100);
            this.scene.highlightTargetBattleSlot(this.isOpponent, true);
        });

        this.on('drag', (pointer, dragX, dragY) => {
            if (!this.scene.canDragHero(this)) return;
            const vx = dragX - this.prevDragX;
            this.prevDragX = dragX;
            this.setAngle(Phaser.Math.Clamp(vx * 0.5, -10, 10));
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
            this.bg.setStrokeStyle(3, teamColor);
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
        const theme = getCardVisualTheme(newHeroDto.color, newHeroDto.rarity);
        this.headerColorBar.setFillStyle(theme.headerBg, 1);
        this.nameText.setColor(theme.textColor);
        if (String(newHeroDto.rarity || '').toUpperCase() === 'LENDARIA') {
            this.bg.setStrokeStyle(3.5, theme.border);
        }

        const prev = this.heroData || newHeroDto;
        this.heroData = newHeroDto;

        this.nameText.setText(newHeroDto.cardName);
        this.traitsText.setText(this.formatHeroTraits(newHeroDto));

        const rawDesc = newHeroDto.descricao || '';
        this.descText.setFontSize(rawDesc.length > 95 ? '7.2px' : '8px');
        this.descText.setText(rawDesc);

        this.espBadge.valueText.setText(`${newHeroDto.espirito}`);
        this.atqBadge.valueText.setText(`${newHeroDto.ataque}`);
        this.defBadge.valueText.setText(`${newHeroDto.defesa}`);
        this.hpBadge.valueText.setText(`${newHeroDto.vidaAtual}`);

        const bonus = newHeroDto.danoBonus ?? newHeroDto.DanoBonus ?? 0;
        const reducao = newHeroDto.reducaoDano ?? 0;
        const vuln = newHeroDto.vulnerabilidade ?? 0;

        this.modBonusText.setText(`${bonus}`);
        this.modReducaoText.setText(`${reducao}`);
        this.modVulnText.setText(`${vuln}`);

        this.syncAttachedCardsVisuals(newHeroDto.attachedCards || []);

        const diffs = [];
        const vfxToTrigger = new Set();

        const triggerCtx = this.scene.currentSnapshot?.triggerContext || {};
        const eventType = triggerCtx.eventType || '';
        const effectClass = triggerCtx.effectClassName || '';

        if (newHeroDto.espirito !== prev.espirito) {
            const d = newHeroDto.espirito - prev.espirito;
            diffs.push({ text: `${d > 0 ? '+' : ''}${d} ESP`, color: d > 0 ? '#c084fc' : '#f87171' });
            vfxToTrigger.add(d > 0 ? 'buff' : 'debuff');
        }
        if (newHeroDto.ataque !== prev.ataque) {
            const d = newHeroDto.ataque - prev.ataque;
            diffs.push({ text: `${d > 0 ? '+' : ''}${d} ATQ`, color: d > 0 ? '#fbbf24' : '#f87171' });
            vfxToTrigger.add(d > 0 ? 'buff' : 'debuff');
        }
        if (newHeroDto.defesa !== prev.defesa) {
            const d = newHeroDto.defesa - prev.defesa;
            diffs.push({ text: `${d > 0 ? '+' : ''}${d} DEF`, color: d > 0 ? '#60a5fa' : '#f87171' });
            vfxToTrigger.add(d > 0 ? 'buff' : 'debuff');
        }
        if ((newHeroDto.danoBonus || 0) > (prev.danoBonus || 0) || (newHeroDto.reducaoDano || 0) > (prev.reducaoDano || 0)) {
            vfxToTrigger.add('buff');
        }
        if ((newHeroDto.vulnerabilidade || 0) > (prev.vulnerabilidade || 0)) {
            vfxToTrigger.add('debuff');
        }

        if (newHeroDto.vidaAtual !== prev.vidaAtual) {
            const d = newHeroDto.vidaAtual - prev.vidaAtual;
            diffs.push({ text: `${d > 0 ? '+' : ''}${d} HP`, color: d > 0 ? '#4ade80' : '#ff3355' });

            if (d > 0) {
                vfxToTrigger.add('cura');
            } else {
                if (eventType === 'PHYSICAL_COMBAT_EXCHANGE' || effectClass.includes('PhysicalAttack')) {
                    vfxToTrigger.add(newHeroDto.hasMagicBasicAttacks ? 'danoMagico' : 'danoFisico');
                } else if (
                    effectClass.includes('DirectDamage') ||
                    effectClass.includes('TrueDamage') ||
                    effectClass.includes('SelfDamage')
                ) {
                    vfxToTrigger.add('danoDireto');
                } else {
                    vfxToTrigger.add('danoMagico');
                }
            }
        }

        if (this.scene && typeof this.scene.playVisualEffect === 'function') {
            Array.from(vfxToTrigger).forEach((vfxKey, idx) => {
                this.scene.time.delayedCall(idx * 180, () => {
                    this.scene.playVisualEffect(vfxKey, this.x, this.y);
                });
            });
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
            this.frameImage.setTint(0x777777);
            if (this.artSprite) this.artSprite.setTint(0x666666);
        } else {
            this.deadOverlayBg.setVisible(false);
            this.deadOverlayText.setVisible(false);
            this.frameImage.clearTint();
            if (this.artSprite) this.artSprite.clearTint();
            const inBattle = newHeroDto.zoneId === 'BATTLE';
            this.sleepStatusIcon.setVisible(!inBattle && newHeroDto.estaAtivo === false);
            this.setAlpha(newHeroDto.estaAtivo ? 1.0 : 0.9);
        }
    }

    syncAttachedCardsVisuals(attachedCards = []) {
        this.attachedBackLayer.removeAll(true);
        this.attachedFrontLayer.removeAll(true);
        this.attachedCardNodes = [];

        // Filtra itens que ainda estão visualmente na fila de resolução (evita anexar antes do COMBAT_END)
        const snap = this.scene?.currentSnapshot;
        const inResolveUuids = new Set([
            ...(snap?.localPlayer?.playedCardsInResolve || []).map(c => c.uuid),
            ...(snap?.opponentPlayer?.playedCardsInResolve || []).map(c => c.uuid)
        ]);

        const visibleAttached = (attachedCards || []).filter(c => c && !inResolveUuids.has(c.uuid));

        if (visibleAttached.length === 0) {
            this.knownAttachedUuids.clear();
            return;
        }

        const inBattle = this.heroData?.zoneId === 'BATTLE';
        const count = visibleAttached.length;

        const maxSpreadX = inBattle ? 138 : 42;
        const stepX = inBattle
            ? Math.min(46, Math.max(24, Math.floor(maxSpreadX / count)))
            : Math.min(20, Math.max(12, Math.floor(maxSpreadX / count)));
        const baseY = inBattle ? 38 : 28;
        const restScale = inBattle ? 0.92 : 0.84;

        const currentUuids = new Set();

        for (let i = visibleAttached.length - 1; i >= 0; i--) {
            const attDto = visibleAttached[i];
            if (attDto.uuid) currentUuids.add(attDto.uuid);

            const baseX = (i + 1) * stepX;
            const isNewlyEquipped = Boolean(attDto.uuid && !this.knownAttachedUuids.has(attDto.uuid));

            const attCard = new PlayableCardPrefab(
                this.scene,
                baseX,
                baseY,
                { ...attDto, faceUp: true },
                false,
                'MEDIUM',
                0
            );

            attCard.removeAllListeners('pointerover');
            attCard.removeAllListeners('pointerout');
            attCard.removeAllListeners('pointerdown');

            attCard.on('pointerover', () => {
                if (this.isDragging) return;
                this.setDepth(95);
                this.attachedFrontLayer.add(attCard);

                this.scene.tweens.killTweensOf(attCard);
                this.scene.tweens.add({
                    targets: attCard,
                    x: baseX,
                    y: baseY - 58,
                    scaleX: 1.08,
                    scaleY: 1.08,
                    duration: 140,
                    ease: 'Back.easeOut'
                });
                attCard.bg.setStrokeStyle(3, 0xffe600);
                this.scene.showLargeCardPreview(attDto, false);
            });

            attCard.on('pointerout', () => {
                this.scene.hideLargeCardPreview();
                if (!attCard.scene) return;

                this.attachedBackLayer.add(attCard);
                this.attachedCardNodes
                    .slice()
                    .reverse()
                    .forEach(node => {
                        if (node && node.parentContainer === this.attachedBackLayer) {
                            this.attachedBackLayer.bringToTop(node);
                        }
                    });

                this.scene.tweens.killTweensOf(attCard);
                this.scene.tweens.add({
                    targets: attCard,
                    x: baseX,
                    y: baseY,
                    scaleX: restScale,
                    scaleY: restScale,
                    duration: 150,
                    ease: 'Power2'
                });
                attCard.bg.setStrokeStyle(0, 0x000000);
                if (!this.isDragging) this.setDepth(10);
            });

            attCard.on('pointerdown', () => {
                if (this.scene && typeof this.scene.openAttachedCardsModal === 'function') {
                    this.scene.openAttachedCardsModal(this.heroData);
                }
            });

            if (isNewlyEquipped && this.scene) {
                const resolveSource = (this.scene.resolveCardPrefabs || []).find(c => c.uuid === attDto.uuid);
                const worldOriginX = resolveSource ? resolveSource.x : BOARD_LAYOUT.ARENA_CIRCLE.x;
                const worldOriginY = resolveSource ? resolveSource.y : BOARD_LAYOUT.ARENA_CIRCLE.y;

                if (resolveSource) resolveSource.setAlpha(0);

                attCard.setPosition(worldOriginX - this.x, worldOriginY - this.y);
                attCard.setScale(1.0);
                attCard.bg.setStrokeStyle(4, 0x55ffff);
                this.attachedFrontLayer.add(attCard);

                this.scene.tweens.add({
                    targets: attCard,
                    x: baseX,
                    y: baseY,
                    scaleX: restScale,
                    scaleY: restScale,
                    duration: 360,
                    ease: 'Back.easeInOut',
                    onComplete: () => {
                        if (!attCard.scene) return;
                        attCard.bg.setStrokeStyle(0, 0x000000);
                        this.attachedBackLayer.add(attCard);
                        this.attachedCardNodes
                            .slice()
                            .reverse()
                            .forEach(node => {
                                if (node && node.parentContainer === this.attachedBackLayer) {
                                    this.attachedBackLayer.bringToTop(node);
                                }
                            });
                    }
                });
            } else {
                attCard.setScale(restScale);
                this.attachedBackLayer.add(attCard);
            }

            this.attachedCardNodes[i] = attCard;
        }

        this.knownAttachedUuids = currentUuids;
    }

    showFloatingNumber(text, color) {
        const floatTxt = this.scene.add.text(this.x, this.y - 25, text, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '22px',
            color: color,
            fontStyle: 'bold',
            stroke: '#090d16',
            strokeThickness: 4
        }).setOrigin(0.5).setDepth(160).setResolution(2);

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

import Phaser from 'phaser';
import {
    KEYWORD_CATEGORIES,
    resolveCardKeywords,
    searchGlossaryKeywords
} from '../config/keywordsConfig.js';

export default class PhantomUIKit {
    /**
     * Cria um botão poligonal assimétrico estilo Persona 5 com telemetria CRT.
     */
    static createRebelButton(scene, x, y, width, height, label, accentColor = 0xe60012, onClick) {
        const container = scene.add.container(x, y);
        const hw = width / 2;
        const hh = height / 2;

        // 1. Sombra dura deslocada (muda de posição no hover)
        const shadowPoly = scene.add.polygon(8, 8, [
            -hw + 12, -hh + 4,
             hw,      -hh,
             hw - 14,  hh,
            -hw,       hh - 4
        ], accentColor, 1).setOrigin(0);

        // 2. Placa principal chanfrada preta com contorno branco cortante
        const bgPoly = scene.add.polygon(0, 0, [
            -hw + 10, -hh,
             hw,      -hh + 4,
             hw - 12,  hh,
            -hw,       hh - 6
        ], 0x07090e, 0.96).setOrigin(0).setStrokeStyle(2.5, 0xf8fafc);

        // 3. Tag lateral estilo terminal ">_"
        const tagPoly = scene.add.polygon(0, 0, [
            -hw + 14, -hh + 4,
            -hw + 52, -hh + 4,
            -hw + 42,  hh - 8,
            -hw + 6,   hh - 8
        ], accentColor, 1).setOrigin(0);

        const promptTxt = scene.add.text(-hw + 28, 0, '>_', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '15px',
            color: '#ffffff',
            fontStyle: 'bold'
        }).setOrigin(0.5);

        // 4. Texto principal com leve inclinação rebelde
        const txt = scene.add.text(14, 0, label.toUpperCase(), {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '16px',
            color: '#f8fafc',
            fontStyle: 'bold'
        }).setOrigin(0.5);

        container.add([shadowPoly, bgPoly, tagPoly, promptTxt, txt]);

        // Zona de clique
        const hitZone = scene.add.rectangle(0, 0, width, height, 0xffffff, 0)
            .setInteractive({ useHandCursor: true });
        container.add(hitZone);

        hitZone.on('pointerover', () => {
            scene.tweens.killTweensOf(container);
            scene.tweens.add({
                targets: container,
                scaleX: 1.05,
                scaleY: 1.05,
                angle: -1.8,
                duration: 110,
                ease: 'Back.easeOut'
            });
            shadowPoly.setFillStyle(0xffe600, 1);
            shadowPoly.setPosition(13, 11);
            bgPoly.setStrokeStyle(3, 0x00f0ff);
            txt.setColor('#ffe600');
        });

        hitZone.on('pointerout', () => {
            scene.tweens.killTweensOf(container);
            scene.tweens.add({
                targets: container,
                scaleX: 1.0,
                scaleY: 1.0,
                angle: 0,
                duration: 140,
                ease: 'Sine.easeOut'
            });
            shadowPoly.setFillStyle(accentColor, 1);
            shadowPoly.setPosition(8, 8);
            bgPoly.setStrokeStyle(2.5, 0xf8fafc);
            txt.setColor('#f8fafc');
        });

        hitZone.on('pointerdown', () => {
            scene.tweens.add({
                targets: container,
                scaleX: 0.95,
                scaleY: 0.95,
                duration: 60,
                yoyo: true,
                onComplete: () => {
                    if (onClick) onClick();
                }
            });
        });

        return { container, bg: bgPoly, txt, shadow: shadowPoly };
    }

    /**
     * Dispara uma faixa diagonal cinética ("Cut-In" estilo Persona 5)
     * Ideal para mudanças de fase em MatchScene.js (CHOICE, PREPARATION, RESOLUTION, COMBAT)
     */
    static playPhaseCutIn(scene, phaseTitle, subText = 'SYS.EXEC // PROTOCOLO DE ARENA', colorHex = 0xe60012) {
        const cutInContainer = scene.add.container(960, 540).setDepth(280).setAngle(-7);

        const backSlash = scene.add.rectangle(0, 0, 2400, 150, 0x050507, 0.95)
            .setStrokeStyle(5, 0xf8fafc);
        const accentStripe = scene.add.rectangle(0, -52, 2400, 24, colorHex, 1);
        const bottomStripe = scene.add.rectangle(0, 58, 2400, 12, 0xffe600, 1);

        const mainTxt = scene.add.text(-900, -4, `// ${phaseTitle.toUpperCase()} //`, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '46px',
            color: '#ffffff',
            fontStyle: 'italic bold',
            stroke: '#e60012',
            strokeThickness: 6
        }).setOrigin(0.5);

        const telemetryTxt = scene.add.text(900, 38, subText, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '14px',
            color: '#00f0ff',
            fontStyle: 'bold'
        }).setOrigin(0.5);

        cutInContainer.add([backSlash, accentStripe, bottomStripe, mainTxt, telemetryTxt]);
        cutInContainer.setScale(1, 0);

        // Abre a fenda diagonal
        scene.tweens.add({
            targets: cutInContainer,
            scaleY: 1,
            duration: 140,
            ease: 'Cubic.easeOut'
        });

        // Desliza o texto em alta velocidade para o centro e depois rasga para fora
        scene.tweens.add({
            targets: mainTxt,
            x: 0,
            duration: 200,
            ease: 'Expo.easeOut',
            onComplete: () => {
                scene.tweens.add({
                    targets: mainTxt,
                    x: 80,
                    duration: 450
                });
            }
        });

        scene.tweens.add({
            targets: telemetryTxt,
            x: 120,
            duration: 240,
            ease: 'Expo.easeOut'
        });

        // Fecha a fenda e destrói
        scene.tweens.add({
            targets: cutInContainer,
            scaleY: 0,
            alpha: 0,
            delay: 720,
            duration: 160,
            ease: 'Cubic.easeIn',
            onComplete: () => cutInContainer.destroy()
        });
    }

    /**
     * Renderiza uma coluna de Popovers explicativos para cada Keyword detectada na carta inspecionada.
     * Chame dentro de scene.showLargeCardPreview(cardDto, isHero) e destrua em hideLargeCardPreview().
     */
    static createKeywordTooltipsContainer(scene, cardDto, x, y) {
        const keywords = resolveCardKeywords(cardDto);
        const root = scene.add.container(x, y).setDepth(260);
        if (keywords.length === 0) return root;

        const boxW = 250;
        const gapY = 8;
        let currentY = 0;

        keywords.slice(0, 4).forEach((kw) => {
            const accentInt = Phaser.Display.Color.HexStringToColor(kw.badgeColor).color;

            const descObj = scene.add.text(-boxW / 2 + 12, 0, kw.description, {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '11px',
                color: '#e2e8f0',
                wordWrap: { width: boxW - 24 },
                lineSpacing: 2
            }).setResolution(2);

            const boxH = Math.max(66, descObj.height + 36);
            const itemContainer = scene.add.container(0, currentY + boxH / 2);

            const shadow = scene.add.rectangle(5, 5, boxW, boxH, accentInt, 0.85);
            const bg = scene.add.rectangle(0, 0, boxW, boxH, 0x07090e, 0.96)
                .setStrokeStyle(2, 0xf8fafc);
            const sideBar = scene.add.rectangle(-boxW / 2 + 3, 0, 6, boxH, accentInt, 1);

            const title = scene.add.text(-boxW / 2 + 14, -boxH / 2 + 8, kw.label.toUpperCase(), {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '12.5px',
                color: kw.badgeColor,
                fontStyle: 'bold'
            }).setResolution(2);

            const catTag = scene.add.text(boxW / 2 - 10, -boxH / 2 + 9, kw.category, {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '9px',
                color: '#94a3b8',
                fontStyle: 'bold'
            }).setOrigin(1, 0).setResolution(2);

            descObj.setPosition(-boxW / 2 + 14, -boxH / 2 + 28);

            itemContainer.add([shadow, bg, sideBar, title, catTag, descObj]);
            root.add(itemContainer);

            currentY += boxH + gapY;
        });

        return root;
    }

    /**
     * Abre o Modal Central de Glossário (acessível no Menu Principal, Deck Builder e Pausa da Partida).
     */
    static openGlossaryModal(scene) {
        if (scene.activeGlossaryModal) {
            scene.activeGlossaryModal.destroy(true);
            scene.activeGlossaryModal = null;
        }

        const { width, height } = scene.scale;
        const modal = scene.add.container(0, 0).setDepth(320);
        scene.activeGlossaryModal = modal;

        let activeCategory = KEYWORD_CATEGORIES.TODOS;
        let searchQuery = '';

        // Backdrop escuro bloqueador de clique
        const backdrop = scene.add.rectangle(width / 2, height / 2, width, height, 0x05070b, 0.86)
            .setInteractive();

        const panelW = 1120;
        const panelH = 720;
        const cx = width / 2;
        const cy = height / 2;

        const panelShadow = scene.add.rectangle(cx + 10, cy + 10, panelW, panelH, 0xe60012, 1);
        const panelBg = scene.add.rectangle(cx, cy, panelW, panelH, 0x07090e, 0.98)
            .setStrokeStyle(3, 0xf8fafc);

        const headerStripe = scene.add.rectangle(cx, cy - panelH / 2 + 34, panelW - 6, 62, 0x0f172a, 1);
        const headerTitle = scene.add.text(cx - panelW / 2 + 28, cy - panelH / 2 + 20, '// CODEX DE REGRAS & KEYWORDS //', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '22px',
            color: '#ffe600',
            fontStyle: 'bold'
        }).setResolution(2);

        // Barra de Busca Rápida (Teclado na Cena)
        const searchBoxW = 340;
        const searchBoxX = cx + panelW / 2 - searchBoxW / 2 - 140;
        const searchBoxY = cy - panelH / 2 + 34;

        const searchBg = scene.add.rectangle(searchBoxX, searchBoxY, searchBoxW, 38, 0x090d16, 1)
            .setStrokeStyle(2, 0x00f0ff);
        const searchText = scene.add.text(searchBoxX - searchBoxW / 2 + 12, searchBoxY, '🔍 Buscar termo... (Digite)', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '13px',
            color: '#94a3b8'
        }).setOrigin(0, 0.5).setResolution(2);

        // Botão Fechar
        const closeBtn = PhantomUIKit.createRebelButton(
            scene,
            cx + panelW / 2 - 64,
            cy - panelH / 2 + 34,
            96,
            38,
            'FECHAR',
            0xe60012,
            () => closeModal()
        );

        // Abas de Categoria
        const categories = Object.values(KEYWORD_CATEGORIES);
        const tabsContainer = scene.add.container(0, 0);
        const cardsGridContainer = scene.add.container(0, 0);

        const renderTabs = () => {
            tabsContainer.removeAll(true);
            const startX = cx - panelW / 2 + 115;
            const tabY = cy - panelH / 2 + 92;
            const tabW = 205;
            const tabGap = 12;

            categories.forEach((cat, idx) => {
                const isSelected = cat === activeCategory;
                const tx = startX + idx * (tabW + tabGap);
                const tabBg = scene.add.rectangle(tx, tabY, tabW, 34, isSelected ? 0xe60012 : 0x111827, 1)
                    .setStrokeStyle(2, isSelected ? 0xffe600 : 0x475569)
                    .setInteractive({ useHandCursor: true });

                const tabLbl = scene.add.text(tx, tabY, cat, {
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '12px',
                    color: isSelected ? '#ffffff' : '#cbd5e1',
                    fontStyle: 'bold'
                }).setOrigin(0.5).setResolution(2);

                tabBg.on('pointerdown', () => {
                    activeCategory = cat;
                    renderTabs();
                    renderGrid();
                });

                tabsContainer.add([tabBg, tabLbl]);
            });
        };

        const renderGrid = () => {
            cardsGridContainer.removeAll(true);
            const results = searchGlossaryKeywords(searchQuery, activeCategory);

            const cols = 2;
            const cardW = 515;
            const cardH = 88;
            const gapX = 24;
            const gapY = 14;
            const startX = cx - (cardW + gapX) / 2;
            const startY = cy - panelH / 2 + 168;

            if (results.length === 0) {
                const emptyTxt = scene.add.text(cx, cy, 'Nenhuma keyword encontrada para o filtro atual.', {
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '16px',
                    color: '#64748b'
                }).setOrigin(0.5).setResolution(2);
                cardsGridContainer.add(emptyTxt);
                return;
            }

            results.slice(0, 10).forEach((kw, idx) => {
                const col = idx % cols;
                const row = Math.floor(idx / cols);
                const kx = startX + col * (cardW + gapX);
                const ky = startY + row * (cardH + gapY);

                const accentInt = Phaser.Display.Color.HexStringToColor(kw.badgeColor).color;
                const cardBg = scene.add.rectangle(kx, ky, cardW, cardH, 0x0f172a, 0.95)
                    .setStrokeStyle(1.5, 0x334155);
                const bar = scene.add.rectangle(kx - cardW / 2 + 4, ky, 8, cardH, accentInt, 1);

                const title = scene.add.text(kx - cardW / 2 + 18, ky - cardH / 2 + 10, kw.label.toUpperCase(), {
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '14px',
                    color: kw.badgeColor,
                    fontStyle: 'bold'
                }).setResolution(2);

                const catBadge = scene.add.text(kx + cardW / 2 - 12, ky - cardH / 2 + 11, kw.category, {
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '10px',
                    color: '#94a3b8'
                }).setOrigin(1, 0).setResolution(2);

                const desc = scene.add.text(kx - cardW / 2 + 18, ky - cardH / 2 + 32, kw.description, {
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '11.5px',
                    color: '#f1f5f9',
                    wordWrap: { width: cardW - 32 },
                    lineSpacing: 2
                }).setResolution(2);

                cardsGridContainer.add([cardBg, bar, title, catBadge, desc]);
            });
        };

        const keyHandler = (event) => {
            if (event.key === 'Escape') {
                closeModal();
                return;
            }
            if (event.key === 'Backspace') {
                searchQuery = searchQuery.slice(0, -1);
            } else if (event.key.length === 1 && searchQuery.length < 24) {
                searchQuery += event.key;
            } else {
                return;
            }
            searchText.setText(searchQuery.length > 0 ? `🔍 ${searchQuery}_` : '🔍 Buscar termo... (Digite)');
            searchText.setColor(searchQuery.length > 0 ? '#ffe600' : '#94a3b8');
            renderGrid();
        };

        const closeModal = () => {
            scene.input.keyboard.off('keydown', keyHandler);
            modal.destroy(true);
            scene.activeGlossaryModal = null;
        };

        scene.input.keyboard.on('keydown', keyHandler);

        modal.add([
            backdrop,
            panelShadow,
            panelBg,
            headerStripe,
            headerTitle,
            searchBg,
            searchText,
            closeBtn.container,
            tabsContainer,
            cardsGridContainer
        ]);

        renderTabs();
        renderGrid();
        return modal;
    }
}

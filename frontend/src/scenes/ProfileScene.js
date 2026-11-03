import Phaser from 'phaser';
import CRTPostFXPipeline from '../fx/CRTPostFXPipeline.js';

export default class ProfileScene extends Phaser.Scene {
    constructor() {
        super({ key: 'ProfileScene' });
        this.player = null;
        this.expandedPurchases = new Set();
        this.expandedMatches = new Set();

        this.purchaseScrollY = 0;
        this.maxPurchaseScrollY = 0;
        this.matchScrollY = 0;
        this.maxMatchScrollY = 0;

        this.listTopY = 212;
        this.listViewportH = 796;
    }

    init(data) {
        this.player = data?.player || JSON.parse(localStorage.getItem('lendas_player')) || {};
        if (!Array.isArray(this.player.purchaseHistory)) this.player.purchaseHistory = [];
        if (!Array.isArray(this.player.matchHistory)) this.player.matchHistory = [];
        this.expandedPurchases.clear();
        this.expandedMatches.clear();
        this.purchaseScrollY = 0;
        this.matchScrollY = 0;
    }

    async create() {
        if (this.renderer.pipelines) {
            this.renderer.pipelines.addPostPipeline('CRTPostFX', CRTPostFXPipeline);
            this.cameras.main.setPostPipeline('CRTPostFX');
        }

        this.drawBackgroundAndLayout();
        this.createTopHeader();
        this.createHistoryPanels();
        this.setupScrollingEvents();

        this.renderBothHistories();
        await this.syncPlayerFromServer();
    }

    async syncPlayerFromServer() {
        if (!this.player?.uuid) return;
        try {
            const res = await fetch(`http://localhost:8080/api/players/${this.player.uuid}`);
            if (res.ok) {
                const updated = await res.json();
                const localPurchases = Array.isArray(this.player?.purchaseHistory) ? this.player.purchaseHistory : [];
                const localMatches = Array.isArray(this.player?.matchHistory) ? this.player.matchHistory : [];

                const serverPurchases = Array.isArray(updated.purchaseHistory) ? updated.purchaseHistory : [];
                const serverMatches = Array.isArray(updated.matchHistory) ? updated.matchHistory : [];

                const mergedPurchases = [...serverPurchases];
                for (const lp of localPurchases) {
                    if (lp && !mergedPurchases.some(sp => (sp.id && lp.id && String(sp.id) === String(lp.id)) || (Math.abs((sp.timestamp || 0) - (lp.timestamp || 0)) < 2000 && sp.productName === lp.productName))) {
                        mergedPurchases.push(lp);
                    }
                }
                mergedPurchases.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));

                const mergedMatches = [...serverMatches];
                for (const lm of localMatches) {
                    if (lm && !mergedMatches.some(sm => sm.matchId && lm.matchId && String(sm.matchId) === String(lm.matchId))) {
                        mergedMatches.push(lm);
                    }
                }
                mergedMatches.sort((a, b) => (b.timestamp || 0) - (a.timestamp || 0));

                this.player = {
                    ...updated,
                    purchaseHistory: mergedPurchases,
                    matchHistory: mergedMatches
                };
                localStorage.setItem('lendas_player', JSON.stringify(this.player));
                this.refreshHeaderStats();
                this.renderBothHistories();
            }
        } catch (e) {
            // Mantém dados locais se offline
        }
    }

    drawBackgroundAndLayout() {
        this.add.rectangle(960, 540, 1920, 1080, 0x0b0f19, 1);
        const g = this.add.graphics();
        g.lineStyle(1, 0x1e293b, 0.55);
        for (let x = 0; x < 1920; x += 80) g.lineBetween(x, 0, x, 1080);
        for (let y = 0; y < 1080; y += 80) g.lineBetween(0, y, 1920, y);

        // Cabeçalho Superior
        this.add.rectangle(960, 65, 1820, 92, 0x0f172a, 0.96).setStrokeStyle(3, 0xffe600);

        // Painel Esquerdo: Histórico de Compras
        this.add.rectangle(500, 585, 890, 890, 0x111827, 0.95).setStrokeStyle(2.5, 0x334155);
        this.add.rectangle(500, 170, 886, 56, 0x1e293b, 1).setStrokeStyle(1.5, 0x475569);
        this.purchaseHeaderText = this.add.text(80, 170, '🛒 HISTÓRICO DE COMPRAS', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '19px',
            color: '#ffe600',
            fontStyle: 'bold'
        }).setOrigin(0, 0.5).setResolution(2);
        this.add.text(920, 170, 'Clique em um Booster p/ ver as cartas', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '11.5px',
            color: '#94a3b8'
        }).setOrigin(1, 0.5).setResolution(2);

        // Painel Direito: Histórico de Partidas
        this.add.rectangle(1420, 585, 890, 890, 0x111827, 0.95).setStrokeStyle(2.5, 0x334155);
        this.add.rectangle(1420, 170, 886, 56, 0x1e293b, 1).setStrokeStyle(1.5, 0x475569);
        this.matchHeaderText = this.add.text(1000, 170, '⚔️ HISTÓRICO DE PARTIDAS', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '19px',
            color: '#38bdf8',
            fontStyle: 'bold'
        }).setOrigin(0, 0.5).setResolution(2);
        this.add.text(1840, 170, 'Clique na partida p/ ver pontuação e heróis', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '11.5px',
            color: '#94a3b8'
        }).setOrigin(1, 0.5).setResolution(2);
    }

    createTopHeader() {
        this.profileTitleText = this.add.text(80, 44, '', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '22px',
            color: '#ffffff',
            fontStyle: 'bold'
        }).setOrigin(0, 0.5).setResolution(2);

        this.profileSubText = this.add.text(80, 82, '', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '13.5px',
            color: '#38bdf8',
            fontStyle: 'bold'
        }).setOrigin(0, 0.5).setResolution(2);

        this.add.rectangle(1280, 65, 290, 48, 0xfaf8f5).setStrokeStyle(3, 0x090d16);
        this.goldText = this.add.text(1280, 65, '', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '18px',
            color: '#090d16',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        const backBtn = this.add.rectangle(1690, 65, 280, 50, 0xe60012)
            .setStrokeStyle(2.5, 0xffffff)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => {
                this.scene.start('MainMenuScene', { player: this.player });
            });
        backBtn.on('pointerover', () => backBtn.setFillStyle(0xb91c1c));
        backBtn.on('pointerout', () => backBtn.setFillStyle(0xe60012));

        this.add.text(1690, 65, '⬅ VOLTAR AO MENU', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '15px',
            color: '#ffffff',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        this.refreshHeaderStats();
    }

    refreshHeaderStats() {
        const name = this.player?.name || 'Jogador';
        const permElo = this.player?.permanentElo ?? 1000;
        const weekElo = this.player?.weeklyElo ?? 1000;
        const gold = this.player?.balance?.gold ?? this.player?.poeira ?? 0;

        this.profileTitleText.setText(`👤 PERFIL DO JOGADOR: ${name}`);
        this.profileSubText.setText(
            `🏅 ELO Permanente: ${permElo}   |   📅 ELO Semanal: ${weekElo}   |   UUID: ${this.player?.uuid || '-'}`
        );
        this.goldText.setText(`🪙 GOLD: ${gold}`);
    }

    createHistoryPanels() {
        // Container + Máscara para Histórico de Compras (Esquerda)
        this.purchaseListContainer = this.add.container(72, this.listTopY);
        const purchaseMaskShape = this.make.graphics({ x: 0, y: 0, add: false });
        purchaseMaskShape.fillStyle(0xffffff);
        purchaseMaskShape.fillRect(62, this.listTopY - 8, 876, this.listViewportH);
        this.purchaseListContainer.setMask(purchaseMaskShape.createGeometryMask());

        this.purchaseScrollTrack = this.add.rectangle(934, this.listTopY + this.listViewportH / 2, 8, this.listViewportH, 0x0f172a)
            .setStrokeStyle(1, 0x334155)
            .setVisible(false);
        this.purchaseScrollThumb = this.add.rectangle(934, this.listTopY + 30, 8, 60, 0xffe600)
            .setVisible(false);

        // Container + Máscara para Histórico de Partidas (Direita)
        this.matchListContainer = this.add.container(992, this.listTopY);
        const matchMaskShape = this.make.graphics({ x: 0, y: 0, add: false });
        matchMaskShape.fillStyle(0xffffff);
        matchMaskShape.fillRect(982, this.listTopY - 8, 876, this.listViewportH);
        this.matchListContainer.setMask(matchMaskShape.createGeometryMask());

        this.matchScrollTrack = this.add.rectangle(1854, this.listTopY + this.listViewportH / 2, 8, this.listViewportH, 0x0f172a)
            .setStrokeStyle(1, 0x334155)
            .setVisible(false);
        this.matchScrollThumb = this.add.rectangle(1854, this.listTopY + 30, 8, 60, 0x38bdf8)
            .setVisible(false);
    }

    setupScrollingEvents() {
        this.input.on('wheel', (pointer, gameObjects, deltaX, deltaY) => {
            const step = deltaY > 0 ? 48 : -48;
            if (pointer.x >= 55 && pointer.x <= 945 && pointer.y >= this.listTopY && pointer.y <= this.listTopY + this.listViewportH) {
                if (this.maxPurchaseScrollY > 0) {
                    this.purchaseScrollY = Phaser.Math.Clamp(this.purchaseScrollY + step, 0, this.maxPurchaseScrollY);
                    this.purchaseListContainer.y = this.listTopY - this.purchaseScrollY;
                    this.updateScrollbars();
                }
            } else if (pointer.x >= 975 && pointer.x <= 1865 && pointer.y >= this.listTopY && pointer.y <= this.listTopY + this.listViewportH) {
                if (this.maxMatchScrollY > 0) {
                    this.matchScrollY = Phaser.Math.Clamp(this.matchScrollY + step, 0, this.maxMatchScrollY);
                    this.matchListContainer.y = this.listTopY - this.matchScrollY;
                    this.updateScrollbars();
                }
            }
        });
    }

    updateScrollbars() {
        if (this.maxPurchaseScrollY > 0) {
            this.purchaseScrollTrack.setVisible(true);
            this.purchaseScrollThumb.setVisible(true);
            const contentH = this.maxPurchaseScrollY + this.listViewportH;
            const thumbH = Math.max(40, (this.listViewportH / contentH) * this.listViewportH);
            const ratio = this.purchaseScrollY / this.maxPurchaseScrollY;
            const avail = this.listViewportH - thumbH;
            this.purchaseScrollThumb.setSize(8, thumbH);
            this.purchaseScrollThumb.y = (this.listTopY - 8) + thumbH / 2 + ratio * avail;
        } else {
            this.purchaseScrollTrack.setVisible(false);
            this.purchaseScrollThumb.setVisible(false);
        }

        if (this.maxMatchScrollY > 0) {
            this.matchScrollTrack.setVisible(true);
            this.matchScrollThumb.setVisible(true);
            const contentH = this.maxMatchScrollY + this.listViewportH;
            const thumbH = Math.max(40, (this.listViewportH / contentH) * this.listViewportH);
            const ratio = this.matchScrollY / this.maxMatchScrollY;
            const avail = this.listViewportH - thumbH;
            this.matchScrollThumb.setSize(8, thumbH);
            this.matchScrollThumb.y = (this.listTopY - 8) + thumbH / 2 + ratio * avail;
        } else {
            this.matchScrollTrack.setVisible(false);
            this.matchScrollThumb.setVisible(false);
        }
    }

    renderBothHistories() {
        this.renderPurchaseHistory();
        this.renderMatchHistory();
        this.updateScrollbars();
    }

    renderPurchaseHistory() {
        this.purchaseListContainer.removeAll(true);
        const purchases = this.player?.purchaseHistory || [];
        this.purchaseHeaderText.setText(`🛒 HISTÓRICO DE COMPRAS (${purchases.length})`);

        if (purchases.length === 0) {
            const emptyTxt = this.add.text(428, 280, 'Nenhuma compra registrada até o momento.', {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '15px',
                color: '#64748b',
                fontStyle: 'bold'
            }).setOrigin(0.5).setResolution(2);
            this.purchaseListContainer.add(emptyTxt);
            this.maxPurchaseScrollY = 0;
            this.purchaseScrollY = 0;
            this.purchaseListContainer.y = this.listTopY;
            return;
        }

        let cursorY = 0;
        const rowW = 848;

        purchases.forEach((entry, idx) => {
            const entryKey = entry.id || `purchase-${idx}`;
            const type = String(entry.purchaseType || '').toUpperCase();
            const isBooster = type === 'BOOSTER';
            const isDeck = type === 'DECK';
            const isSingleCard = type === 'SINGLE_CARD';
            const isExpanded = isBooster && this.expandedPurchases.has(entryKey);

            // Formatação conforme regras da Issue #40:
            // - Carta avulsa: nome e valor da compra
            // - Booster: "comprou X booster" e ao clicar expande quais cartas vieram
            // - Baralho: apenas o nome do baralho
            let mainLabel;
            let badgeColor;
            if (isSingleCard) {
                mainLabel = `🃏 Comprou Carta Avulsa: ${entry.productName}   —   🪙 ${entry.price ?? 0} Gold`;
                badgeColor = 0x22c55e;
            } else if (isDeck) {
                mainLabel = `📦 Comprou Baralho: ${entry.productName}`;
                badgeColor = 0x3b82f6;
            } else if (isBooster) {
                const arrow = isExpanded ? '▼' : '▶';
                mainLabel = `${arrow} Comprou Booster: ${entry.productName}`;
                badgeColor = 0xfacc15;
            } else {
                mainLabel = `🛍️ Comprou: ${entry.productName}`;
                badgeColor = 0x94a3b8;
            }

            const rowH = 50;
            const rowBg = this.add.rectangle(rowW / 2, cursorY + rowH / 2, rowW, rowH, 0x1e293b, 0.96)
                .setStrokeStyle(1.5, isExpanded ? 0xfacc15 : 0x334155);
            const leftBar = this.add.rectangle(4, cursorY + rowH / 2, 6, rowH - 4, badgeColor);

            const titleTxt = this.add.text(20, cursorY + rowH / 2, mainLabel, {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '14px',
                color: '#f8fafc',
                fontStyle: 'bold',
                wordWrap: { width: isBooster ? 670 : 810 }
            }).setOrigin(0, 0.5).setResolution(2);

            this.purchaseListContainer.add([rowBg, leftBar, titleTxt]);

            if (isBooster) {
                const expandHint = this.add.text(rowW - 14, cursorY + rowH / 2, isExpanded ? 'Ocultar cartas ▲' : 'Ver cartas ▼', {
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '11.5px',
                    color: '#fde047',
                    fontStyle: 'bold'
                }).setOrigin(1, 0.5).setResolution(2);
                this.purchaseListContainer.add(expandHint);

                rowBg.setInteractive({ useHandCursor: true })
                    .on('pointerover', () => rowBg.setFillStyle(0x283548, 1))
                    .on('pointerout', () => rowBg.setFillStyle(0x1e293b, 0.96))
                    .on('pointerdown', () => {
                        if (this.expandedPurchases.has(entryKey)) {
                            this.expandedPurchases.delete(entryKey);
                        } else {
                            this.expandedPurchases.add(entryKey);
                        }
                        this.renderPurchaseHistory();
                        this.updateScrollbars();
                    });
            }

            cursorY += rowH + 6;

            // Expansão das cartas sorteadas no Booster
            if (isExpanded) {
                const cards = Array.isArray(entry.boosterCards) ? entry.boosterCards : [];
                const lines = cards.length > 0
                    ? cards.map((cName, cIdx) => `  ${cIdx + 1}. ✦ ${cName}`).join('\n')
                    : '  (Nenhuma carta detalhada neste pacote)';
                const detailH = Math.max(54, cards.length * 22 + 28);

                const detailBg = this.add.rectangle(rowW / 2, cursorY + detailH / 2 - 4, rowW - 16, detailH, 0x0f172a, 0.98)
                    .setStrokeStyle(1.5, 0xca8a04);
                const detailHeader = this.add.text(26, cursorY + 6, 'CARTAS QUE VIERAM NO BOOSTER:', {
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '11px',
                    color: '#facc15',
                    fontStyle: 'bold'
                }).setOrigin(0, 0).setResolution(2);
                const detailBody = this.add.text(26, cursorY + 24, lines, {
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '12.5px',
                    color: '#e2e8f0',
                    lineSpacing: 3
                }).setOrigin(0, 0).setResolution(2);

                this.purchaseListContainer.add([detailBg, detailHeader, detailBody]);
                cursorY += detailH + 8;
            }
        });

        this.maxPurchaseScrollY = Math.max(0, cursorY - (this.listViewportH - 20));
        this.purchaseScrollY = Phaser.Math.Clamp(this.purchaseScrollY, 0, this.maxPurchaseScrollY);
        this.purchaseListContainer.y = this.listTopY - this.purchaseScrollY;
    }

    renderMatchHistory() {
        this.matchListContainer.removeAll(true);
        const matches = this.player?.matchHistory || [];
        this.matchHeaderText.setText(`⚔️ HISTÓRICO DE PARTIDAS (${matches.length})`);

        if (matches.length === 0) {
            const emptyTxt = this.add.text(428, 280, 'Nenhuma partida concluída registrada até o momento.', {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '15px',
                color: '#64748b',
                fontStyle: 'bold'
            }).setOrigin(0.5).setResolution(2);
            this.matchListContainer.add(emptyTxt);
            this.maxMatchScrollY = 0;
            this.matchScrollY = 0;
            this.matchListContainer.y = this.listTopY;
            return;
        }

        let cursorY = 0;
        const rowW = 848;

        matches.forEach((entry, idx) => {
            const entryKey = entry.matchId || `match-${idx}`;
            const isExpanded = this.expandedMatches.has(entryKey);
            const isWin = Boolean(entry.victory);
            const pName = entry.playerName || this.player?.name || 'Você';
            const oppName = entry.opponentName || 'Oponente';
            const statusText = isWin ? '🏆 VITÓRIA' : '💀 DERROTA';
            const accentColor = isWin ? 0x22c55e : 0xef4444;
            const arrow = isExpanded ? '▼' : '▶';

            const rowH = 52;
            const rowBg = this.add.rectangle(rowW / 2, cursorY + rowH / 2, rowW, rowH, 0x1e293b, 0.96)
                .setStrokeStyle(1.5, isExpanded ? accentColor : 0x334155);
            const leftBar = this.add.rectangle(4, cursorY + rowH / 2, 6, rowH - 4, accentColor);

            const matchupTxt = this.add.text(20, cursorY + rowH / 2, `${arrow} ${pName}  vs  ${oppName}`, {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '14.5px',
                color: '#ffffff',
                fontStyle: 'bold'
            }).setOrigin(0, 0.5).setResolution(2);

            const resultBadge = this.add.text(rowW - 16, cursorY + rowH / 2, statusText, {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '13.5px',
                color: isWin ? '#4ade80' : '#f87171',
                fontStyle: 'bold'
            }).setOrigin(1, 0.5).setResolution(2);

            rowBg.setInteractive({ useHandCursor: true })
                .on('pointerover', () => rowBg.setFillStyle(0x283548, 1))
                .on('pointerout', () => rowBg.setFillStyle(0x1e293b, 0.96))
                .on('pointerdown', () => {
                    if (this.expandedMatches.has(entryKey)) {
                        this.expandedMatches.delete(entryKey);
                    } else {
                        this.expandedMatches.add(entryKey);
                    }
                    this.renderMatchHistory();
                    this.updateScrollbars();
                });

            this.matchListContainer.add([rowBg, leftBar, matchupTxt, resultBadge]);
            cursorY += rowH + 6;

            // Expansão ao clicar: mostra quem ganhou, pontuação e os heróis usados por cada jogador
            if (isExpanded) {
                const detailH = 104;
                const detailBg = this.add.rectangle(rowW / 2, cursorY + detailH / 2 - 4, rowW - 16, detailH, 0x0f172a, 0.98)
                    .setStrokeStyle(1.5, accentColor);

                const winnerStr = entry.winnerName || (isWin ? pName : oppName);
                const pScore = entry.playerScore ?? 0;
                const oScore = entry.opponentScore ?? 0;
                const pHeroes = (entry.playerHeroes && entry.playerHeroes.length > 0)
                    ? entry.playerHeroes.join('  •  ')
                    : 'Não informado';
                const oHeroes = (entry.opponentHeroes && entry.opponentHeroes.length > 0)
                    ? entry.opponentHeroes.join('  •  ')
                    : 'Não informado';

                const line1 = `🏅 Vencedor: ${winnerStr}     |     📊 Pontuação: ${pName} (${pScore} pts)  ×  ${oppName} (${oScore} pts)`;
                const line2 = `🛡️ Heróis de ${pName}: ${pHeroes}`;
                const line3 = `⚔️ Heróis de ${oppName}: ${oHeroes}`;

                const txt1 = this.add.text(26, cursorY + 12, line1, {
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '12.5px',
                    color: '#fde047',
                    fontStyle: 'bold'
                }).setOrigin(0, 0).setResolution(2);

                const txt2 = this.add.text(26, cursorY + 40, line2, {
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '12px',
                    color: '#86efac',
                    fontStyle: 'bold',
                    wordWrap: { width: rowW - 48 }
                }).setOrigin(0, 0).setResolution(2);

                const txt3 = this.add.text(26, cursorY + 68, line3, {
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '12px',
                    color: '#fca5a5',
                    fontStyle: 'bold',
                    wordWrap: { width: rowW - 48 }
                }).setOrigin(0, 0).setResolution(2);

                this.matchListContainer.add([detailBg, txt1, txt2, txt3]);
                cursorY += detailH + 8;
            }
        });

        this.maxMatchScrollY = Math.max(0, cursorY - (this.listViewportH - 20));
        this.matchScrollY = Phaser.Math.Clamp(this.matchScrollY, 0, this.maxMatchScrollY);
        this.matchListContainer.y = this.listTopY - this.matchScrollY;
    }
}

import Phaser from 'phaser';
import CRTPostFXPipeline from '../fx/CRTPostFXPipeline.js';

export default class CollectionScene extends Phaser.Scene {
    constructor() {
        super({ key: 'CollectionScene' });
        this.player = null;
        this.activeFilter = 'CARDS'; // 'CARDS' ou 'HEROES'
        this.currentPage = 0;
        this.itemsPerPage = 8;
        this.selectedItemForSale = null;
        this.salePrice = 100;
        this.isSubmittingSale = false;
    }

    init(data) {
        this.player = data?.player || JSON.parse(localStorage.getItem('lendas_player')) || {};
        const currentGold = this.player.balance?.gold ?? this.player.poeira ?? 500;
        this.player.balance = { gold: currentGold };
        this.player.poeira = currentGold;
        if (!this.player.ownedCards) this.player.ownedCards = {};
        if (!this.player.ownedHeroes) this.player.ownedHeroes = {};
    }

    preload() {
        this.load.setPath('assets');
        if (!this.textures.exists('card_frame_hero')) {
            this.load.svg('card_frame_hero', 'images/ui/card_frame_hero.svg', { width: 280, height: 380 });
        }
        if (!this.textures.exists('card_frame_playable')) {
            this.load.svg('card_frame_playable', 'images/ui/card_frame_playable.svg', { width: 280, height: 380 });
        }
    }

    create() {
        if (this.renderer.pipelines) {
            this.renderer.pipelines.addPostPipeline('CRTPostFX', CRTPostFXPipeline);
            this.cameras.main.setPostPipeline('CRTPostFX');
        }

        this.drawBackground();
        this.createTopBar();
        this.createFilterTabs();

        this.gridContainer = this.add.container(0, 0);
        this.createPaginationControls();
        this.createSellConfirmationModal();

        this.renderCollectionGrid();
    }

    getGoldBalance() {
        return this.player.balance?.gold ?? this.player.poeira ?? 0;
    }

    drawBackground() {
        this.add.rectangle(960, 540, 1920, 1080, 0x181c24, 1);
        const g = this.add.graphics();
        g.lineStyle(1, 0x263042, 0.5);
        for (let x = 0; x < 1920; x += 80) g.lineBetween(x, 0, x, 1080);
        for (let y = 0; y < 1080; y += 80) g.lineBetween(0, y, 1920, y);

        this.add.rectangle(960, 62, 1800, 82, 0x0f172a, 0.96).setStrokeStyle(3, 0xffe600);
        this.add.rectangle(960, 595, 1740, 810, 0x263042, 0.92).setStrokeStyle(3, 0x090d16);
    }

    createTopBar() {
        this.add.text(90, 62, '📚 TERMINAL DE ACERVO // MINHA COLEÇÃO', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '24px',
            color: '#ffe600',
            fontStyle: 'bold'
        }).setOrigin(0, 0.5).setResolution(2);

        this.add.rectangle(1320, 62, 310, 48, 0xfaf8f5).setStrokeStyle(3, 0x090d16);
        this.balanceText = this.add.text(1320, 62, `🪙 GOLD: ${this.getGoldBalance()}`, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '18px',
            color: '#090d16',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        this.add.rectangle(1685, 62, 250, 48, 0xe60012)
            .setStrokeStyle(2.5, 0xffffff)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => {
                if (!this.sellModal.visible) {
                    this.scene.start('MainMenuScene', { player: this.player });
                }
            });

        this.add.text(1685, 62, '⬅ VOLTAR AO MENU', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '15px',
            color: '#ffffff',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);
    }

    createFilterTabs() {
        this.cardsTabBtn = this.createTabButton(420, 145, '🃏 CARTAS DE COMBATE', 'CARDS');
        this.heroesTabBtn = this.createTabButton(900, 145, '🛡️ HERÓIS DA COLEÇÃO', 'HEROES');

        const goMarketBtn = this.add.rectangle(1480, 145, 380, 52, 0x16a34a)
            .setStrokeStyle(3, 0x090d16)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => {
                if (!this.sellModal.visible) {
                    this.scene.start('ShopScene', { player: this.player, initialTab: 'MARKET' });
                }
            });

        this.add.text(1480, 145, '⚖️ VER MERCADO DE JOGADORES', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '15px',
            color: '#ffffff',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        this.updateTabVisuals();
    }

    createTabButton(x, y, label, tabKey) {
        const bg = this.add.rectangle(x, y, 420, 52, 0x1e293b)
            .setStrokeStyle(3, 0x090d16)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => {
                if (this.sellModal.visible) return;
                this.activeFilter = tabKey;
                this.currentPage = 0;
                this.updateTabVisuals();
                this.renderCollectionGrid();
            });

        const txt = this.add.text(x, y, label, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '16px',
            color: '#ffffff',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        return { bg, txt };
    }

    updateTabVisuals() {
        const isCards = this.activeFilter === 'CARDS';
        this.cardsTabBtn.bg.setFillStyle(isCards ? 0xe60012 : 0x1e293b);
        this.cardsTabBtn.txt.setColor(isCards ? '#ffe600' : '#94a3b8');

        this.heroesTabBtn.bg.setFillStyle(!isCards ? 0xe60012 : 0x1e293b);
        this.heroesTabBtn.txt.setColor(!isCards ? '#ffe600' : '#94a3b8');
    }

    getOwnedEntriesList() {
        const sourceMap = this.activeFilter === 'HEROES'
            ? (this.player.ownedHeroes || {})
            : (this.player.ownedCards || {});

        return Object.entries(sourceMap)
            .filter(([, count]) => count > 0)
            .map(([cardName, count]) => ({
                cardName,
                count,
                isHero: this.activeFilter === 'HEROES'
            }))
            .sort((a, b) => a.cardName.localeCompare(b.cardName));
    }

    renderCollectionGrid() {
        this.gridContainer.removeAll(true);
        const allItems = this.getOwnedEntriesList();
        const totalPages = Math.max(1, Math.ceil(allItems.length / this.itemsPerPage));
        if (this.currentPage >= totalPages) this.currentPage = totalPages - 1;

        const startIdx = this.currentPage * this.itemsPerPage;
        const pageItems = allItems.slice(startIdx, startIdx + this.itemsPerPage);

        this.pageIndicatorTxt.setText(`PÁGINA ${this.currentPage + 1} / ${totalPages} (${allItems.length} ITENS)`);

        if (pageItems.length === 0) {
            const emptyTxt = this.add.text(960, 560, 'Nenhum item disponível nesta categoria da sua coleção.', {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '20px',
                color: '#94a3b8',
                fontStyle: 'bold'
            }).setOrigin(0.5).setResolution(2);
            this.gridContainer.add(emptyTxt);
            return;
        }

        pageItems.forEach((item, index) => {
            const col = index % 4;
            const row = Math.floor(index / 4);
            const x = 345 + col * 410;
            const y = 395 + row * 340;
            this.createCollectionCardTile(x, y, item);
        });
    }

    createCollectionCardTile(x, y, item) {
        const tile = this.add.container(x, y);
        const w = 360;
        const h = 300;

        const bg = this.add.rectangle(0, 0, w, h, 0xfaf8f5, 1).setStrokeStyle(3, 0x090d16);
        const header = this.add.rectangle(0, -h / 2 + 34, w - 6, 62, 0x090d16);

        const title = this.add.text(0, -h / 2 + 34, item.cardName, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '15px',
            color: '#ffe600',
            fontStyle: 'bold',
            align: 'center',
            wordWrap: { width: w - 24 }
        }).setOrigin(0.5).setResolution(2);

        const typeBadge = this.add.text(0, -25, item.isHero ? '🛡️ HERÓI' : '🃏 CARTA DE COMBATE', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '14px',
            color: '#334155',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        const qtyBadge = this.add.rectangle(0, 25, 220, 40, 0xe6dec8).setStrokeStyle(2, 0x090d16);
        const qtyTxt = this.add.text(0, 25, `NA COLEÇÃO: ${item.count}x`, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '16px',
            color: '#090d16',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        const sellBtnBg = this.add.rectangle(0, 100, 300, 50, 0x16a34a)
            .setStrokeStyle(3, 0x090d16)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => this.openSellModal(item));

        const sellBtnTxt = this.add.text(0, 100, '💰 VENDER CARTA', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '16px',
            color: '#ffffff',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        tile.add([bg, header, title, typeBadge, qtyBadge, qtyTxt, sellBtnBg, sellBtnTxt]);
        this.gridContainer.add(tile);
    }

    createPaginationControls() {
        const prevBtn = this.add.rectangle(680, 955, 180, 44, 0x1e293b)
            .setStrokeStyle(2, 0xffe600)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => {
                if (this.currentPage > 0) {
                    this.currentPage--;
                    this.renderCollectionGrid();
                }
            });
        this.add.text(680, 955, '◀ ANTERIOR', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '14px', color: '#ffffff', fontStyle: 'bold'
        }).setOrigin(0.5);

        this.pageIndicatorTxt = this.add.text(960, 955, 'PÁGINA 1 / 1', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '16px', color: '#ffe600', fontStyle: 'bold'
        }).setOrigin(0.5);

        const nextBtn = this.add.rectangle(1240, 955, 180, 44, 0x1e293b)
            .setStrokeStyle(2, 0xffe600)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => {
                const total = this.getOwnedEntriesList().length;
                if ((this.currentPage + 1) * this.itemsPerPage < total) {
                    this.currentPage++;
                    this.renderCollectionGrid();
                }
            });
        this.add.text(1240, 955, 'PRÓXIMA ▶', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '14px', color: '#ffffff', fontStyle: 'bold'
        }).setOrigin(0.5);
    }

    createSellConfirmationModal() {
        this.sellModal = this.add.container(960, 540).setDepth(300).setVisible(false);

        const overlay = this.add.rectangle(0, 0, 1920, 1080, 0x050508, 0.88).setInteractive();
        const box = this.add.rectangle(0, 0, 760, 480, 0x181c24, 0.98).setStrokeStyle(4, 0xffe600);

        const header = this.add.text(0, -190, '╔══ CONFIRMAR ANÚNCIO DE VENDA NO MERCADO ══╗', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '20px', color: '#ffe600', fontStyle: 'bold'
        }).setOrigin(0.5);

        this.modalCardNameTxt = this.add.text(0, -125, '', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '22px', color: '#ffffff', fontStyle: 'bold'
        }).setOrigin(0.5);

        const subInfo = this.add.text(0, -80, 'A unidade será retirada da sua coleção e listada para os jogadores:', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '14px', color: '#94a3b8'
        }).setOrigin(0.5);

        this.modalPriceDisplay = this.add.text(0, -10, '🪙 100 GOLD', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '32px', color: '#4ade80', fontStyle: 'bold'
        }).setOrigin(0.5);

        const adjustments = [
            { label: '-50', delta: -50, x: -240 },
            { label: '-10', delta: -10, x: -120 },
            { label: '+10', delta: 10, x: 120 },
            { label: '+50', delta: 50, x: 240 }
        ];

        const adjElements = [];
        adjustments.forEach(adj => {
            const btn = this.add.rectangle(adj.x, 65, 95, 44, 0x334155)
                .setStrokeStyle(2, 0xffffff)
                .setInteractive({ useHandCursor: true })
                .on('pointerdown', () => {
                    this.salePrice = Math.max(10, this.salePrice + adj.delta);
                    this.modalPriceDisplay.setText(`🪙 ${this.salePrice} GOLD`);
                });
            const txt = this.add.text(adj.x, 65, adj.label, {
                fontFamily: 'IoskeleyMono, monospace', fontSize: '16px', color: '#ffffff', fontStyle: 'bold'
            }).setOrigin(0.5);
            adjElements.push(btn, txt);
        });

        const confirmBtn = this.add.rectangle(-155, 165, 270, 54, 0x16a34a)
            .setStrokeStyle(3, 0xffffff)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => this.confirmListingCreation());

        const confirmTxt = this.add.text(-155, 165, '✅ CONFIRMAR VENDA', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '16px', color: '#ffffff', fontStyle: 'bold'
        }).setOrigin(0.5);

        const cancelBtn = this.add.rectangle(155, 165, 270, 54, 0xe60012)
            .setStrokeStyle(3, 0xffffff)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => {
                if (!this.isSubmittingSale) this.sellModal.setVisible(false);
            });

        const cancelTxt = this.add.text(155, 165, '✖ CANCELAR', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '16px', color: '#ffffff', fontStyle: 'bold'
        }).setOrigin(0.5);

        this.sellModal.add([
            overlay, box, header, this.modalCardNameTxt, subInfo,
            this.modalPriceDisplay, ...adjElements, confirmBtn, confirmTxt, cancelBtn, cancelTxt
        ]);
    }

    openSellModal(item) {
        this.selectedItemForSale = item;
        this.salePrice = 100;
        this.modalCardNameTxt.setText(`${item.cardName} (Possui: ${item.count}x)`);
        this.modalPriceDisplay.setText(`🪙 ${this.salePrice} GOLD`);
        this.sellModal.setVisible(true);
    }

    async confirmListingCreation() {
        if (this.isSubmittingSale || !this.selectedItemForSale) return;
        this.isSubmittingSale = true;

        const idempotencyKey = `LIST_${this.player.uuid}_${this.selectedItemForSale.cardName}_${Date.now()}`;

        try {
            const res = await fetch('http://localhost:8080/api/shop/listings', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Idempotency-Key': idempotencyKey
                },
                body: JSON.stringify({
                    sellerId: this.player.uuid,
                    cardName: this.selectedItemForSale.cardName,
                    heroCard: this.selectedItemForSale.isHero,
                    price: this.salePrice
                })
            });

            const data = await res.json();
            if (!res.ok) {
                this.showToast(data.message || 'Não foi possível anunciar a carta.');
                this.isSubmittingSale = false;
                return;
            }

            if (data.player) {
                this.player = data.player;
                localStorage.setItem('lendas_player', JSON.stringify(this.player));
            }
            this.balanceText.setText(`🪙 GOLD: ${this.getGoldBalance()}`);
            this.sellModal.setVisible(false);
            this.renderCollectionGrid();
            this.showToast(data.message || 'Carta anunciada com sucesso!', '#16a34a');
        } catch (e) {
            this.showToast('Erro de comunicação com o servidor.');
        } finally {
            this.isSubmittingSale = false;
        }
    }

    showToast(msg, bgColor = '#b91c1c') {
        const toast = this.add.text(960, 110, msg, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '15px',
            color: '#ffffff',
            backgroundColor: bgColor,
            padding: { x: 16, y: 8 },
            fontStyle: 'bold'
        }).setOrigin(0.5).setDepth(400).setResolution(2);

        this.tweens.add({
            targets: toast,
            alpha: 0,
            delay: 2400,
            duration: 400,
            onComplete: () => toast.destroy()
        });
    }
}

import Phaser from 'phaser';
import { BOARD_LAYOUT } from '../config/BoardCoordinates.js';
import CRTPostFXPipeline from '../fx/CRTPostFXPipeline.js';

export default class ShopScene extends Phaser.Scene {
    constructor() {
        super({ key: 'ShopScene' });
        this.player = null;
        this.products = [];
        this.activeTab = 'BOOSTER'; // 'BOOSTER', 'DECK' ou 'MARKET'
        this.isOpeningBooster = false;
        this.marketListings = [];
        this.isProcessingTransaction = false;
    }

    init(data) {
        this.player = data?.player || JSON.parse(localStorage.getItem('lendas_player')) || {};
        const currentGold = this.player.balance?.gold ?? this.player.poeira ?? 500;
        this.player.balance = { gold: currentGold };
        this.player.poeira = currentGold;
        if (!this.player.ownedCards) this.player.ownedCards = {};
        if (!this.player.ownedHeroes) this.player.ownedHeroes = {};
        if (!this.player.decks) this.player.decks = [];
        this.activeTab = data?.initialTab || 'BOOSTER'; // 'BOOSTER', 'DECK' ou 'MARKET'
        this.marketListings = [];
        this.isProcessingTransaction = false;
    }

    getGoldBalance() {
        return this.player.balance?.gold ?? this.player.poeira ?? 0;
    }

    preload() {
        this.load.setPath('assets');
        if (!this.textures.exists('card_back')) {
            this.load.svg('card_back', 'images/ui/card_back.svg', { width: 280, height: 380 });
        }
        if (!this.textures.exists('card_frame_hero')) {
            this.load.svg('card_frame_hero', 'images/ui/card_frame_hero.svg', { width: 280, height: 380 });
        }
        if (!this.textures.exists('card_frame_playable')) {
            this.load.svg('card_frame_playable', 'images/ui/card_frame_playable.svg', { width: 280, height: 380 });
        }
        if (!this.textures.exists('ui_p5_crt_button')) {
            this.load.svg('ui_p5_crt_button', 'images/ui/p5_crt_button.svg', { width: 520, height: 84 });
        }
    }

    async create() {
        if (this.renderer.pipelines) {
            this.renderer.pipelines.addPostPipeline('CRTPostFX', CRTPostFXPipeline);
            this.cameras.main.setPostPipeline('CRTPostFX');
        }

        this.drawShopBackground();
        this.createTopBar();
        this.createTabs();

        this.productsContainer = this.add.container(0, 0);
        this.createBoosterRevealModal();

        await this.fetchProductsFromServer();
        await this.fetchMarketListingsFromServer();
        this.renderProductsGrid();
    }

    drawShopBackground() {
        this.add.rectangle(960, 540, 1920, 1080, 0x181c24, 1);

        const g = this.add.graphics();
        g.lineStyle(1, 0x263042, 0.5);
        for (let x = 0; x < 1920; x += 80) g.lineBetween(x, 0, x, 1080);
        for (let y = 0; y < 1080; y += 80) g.lineBetween(0, y, 1920, y);

        this.add.rectangle(960, 62, 1800, 82, 0x0f172a, 0.96).setStrokeStyle(3, 0xffe600);
        this.add.rectangle(960, 595, 1740, 810, 0x263042, 0.92).setStrokeStyle(3, 0x090d16);
    }

    createTopBar() {
        this.add.text(90, 62, '🛒 TERMINAL DE SUPRIMENTOS // LOJA DA POEIRA', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '24px',
            color: '#ffe600',
            fontStyle: 'bold'
        }).setOrigin(0, 0.5).setResolution(2);

        // Visor Claro de Saldo de Gold
        this.add.rectangle(1320, 62, 310, 48, 0xfaf8f5).setStrokeStyle(3, 0x090d16);
        this.balanceText = this.add.text(1320, 62, `🪙 GOLD: ${this.getGoldBalance()}`, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '18px',
            color: '#090d16',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        const backBtn = this.add.rectangle(1685, 62, 250, 48, 0xe60012)
            .setStrokeStyle(2.5, 0xffffff)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => {
                if (!this.isOpeningBooster && !this.isProcessingTransaction) {
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

    createTabs() {
        this.boosterTabBtn = this.createTabButton(380, 145, '📦 BOOSTERS', 'BOOSTER');
        this.deckTabBtn = this.createTabButton(960, 145, '🃏 BARALHOS', 'DECK');
        this.marketTabBtn = this.createTabButton(1540, 145, '⚖️ CARTAS DE PLAYERS', 'MARKET');
        this.updateTabVisuals();
    }

    createTabButton(x, y, label, tabKey) {
        const bg = this.add.rectangle(x, y, 440, 52, 0x1e293b)
            .setStrokeStyle(3, 0x090d16)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => {
                if (this.isOpeningBooster || this.isProcessingTransaction) return;
                this.activeTab = tabKey;
                this.updateTabVisuals();
                this.renderProductsGrid();
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
        const tabs = [
            { key: 'BOOSTER', btn: this.boosterTabBtn },
            { key: 'DECK', btn: this.deckTabBtn },
            { key: 'MARKET', btn: this.marketTabBtn }
        ];
        tabs.forEach(t => {
            const active = this.activeTab === t.key;
            t.btn.bg.setFillStyle(active ? 0xe60012 : 0x1e293b);
            t.btn.txt.setColor(active ? '#ffe600' : '#94a3b8');
        });
    }

    async fetchProductsFromServer() {
        try {
            const res = await fetch('http://localhost:8080/api/shop/products');
            if (!res.ok) throw new Error('Falha ao buscar produtos');
            this.products = await res.json();
        } catch (e) {
            // Catálogo de fallback caso servidor esteja offline
            this.products = [
                {
                    id: 'booster-base-5',
                    name: 'Pacote das Areias (5 Cartas)',
                    description: 'Contém 5 cartas sorteadas da Coleção Base (60% Comum, 25% Incomum, 12% Rara, 3% Lendária).',
                    type: 'BOOSTER',
                    price: 100,
                    cardsPerBooster: 5
                },
                {
                    id: 'booster-elite-5',
                    name: 'Cofre de Cristal (5 Cartas + Herói)',
                    description: 'Pacote especial com 5 cartas e 100% de garantia de conter 1 Herói no último slot!',
                    type: 'BOOSTER',
                    price: 220,
                    cardsPerBooster: 5
                },
                {
                    id: 'deck-preset-1',
                    name: 'Deck Estruturado: Ordem do Cristal',
                    description: 'Desbloqueia 3 Heróis e 30 Cartas estruturadas prontas para combate.',
                    type: 'DECK',
                    price: 350
                },
                {
                    id: 'deck-preset-2',
                    name: 'Deck Estruturado: Fúria das Cinzas',
                    description: 'Baralho ofensivo completo adicionado diretamente à sua coleção e lista de decks.',
                    type: 'DECK',
                    price: 350
                }
            ];
        }
    }

    async fetchMarketListingsFromServer() {
        try {
            const res = await fetch('http://localhost:8080/api/shop/listings');
            if (res.ok) {
                this.marketListings = await res.json();
            }
        } catch (e) {
            this.marketListings = [];
        }
    }

    renderProductsGrid() {
        this.productsContainer.removeAll(true);

        if (this.activeTab === 'MARKET') {
            this.renderPlayerListingsGrid();
            return;
        }

        const filtered = this.products.filter(p => p.type === this.activeTab);
        filtered.forEach((product, idx) => {
            const x = 520 + (idx % 2) * 880;
            const y = 560;
            this.createProductCard(x, y, product);
        });
    }

    renderPlayerListingsGrid() {
        if (!this.marketListings || this.marketListings.length === 0) {
            const emptyMsg = this.add.text(960, 540, 'Nenhuma carta anunciada pelos jogadores no momento.\nVisite a tela MINHA COLEÇÃO para anunciar cartas!', {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '20px',
                color: '#94a3b8',
                align: 'center',
                fontStyle: 'bold'
            }).setOrigin(0.5).setResolution(2);
            this.productsContainer.add(emptyMsg);
            return;
        }

        this.marketListings.slice(0, 8).forEach((listing, idx) => {
            const col = idx % 4;
            const row = Math.floor(idx / 4);
            const x = 345 + col * 410;
            const y = 395 + row * 340;
            this.createListingCard(x, y, listing);
        });
    }

    createListingCard(x, y, listing) {
        const card = this.add.container(x, y);
        const w = 360;
        const h = 300;
        const isOwnListing = listing.sellerId === this.player.uuid;
        const canAfford = this.getGoldBalance() >= listing.price;

        const bg = this.add.rectangle(0, 0, w, h, 0xfaf8f5, 1).setStrokeStyle(3, 0x090d16);
        const header = this.add.rectangle(0, -h / 2 + 34, w - 6, 62, 0x090d16);

        const title = this.add.text(0, -h / 2 + 34, listing.cardName, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '15px',
            color: '#ffe600',
            fontStyle: 'bold',
            align: 'center',
            wordWrap: { width: w - 24 }
        }).setOrigin(0.5).setResolution(2);

        const sellerTxt = this.add.text(0, -35, `Vendedor: ${listing.sellerName || 'Jogador'}`, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '13px',
            color: '#334155',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        const priceBadge = this.add.rectangle(0, 18, 240, 42, 0xe6dec8).setStrokeStyle(2, 0x090d16);
        const priceTxt = this.add.text(0, 18, `🪙 ${listing.price} GOLD`, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '17px',
            color: isOwnListing ? '#090d16' : (canAfford ? '#15803d' : '#dc2626'),
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        const btnColor = isOwnListing ? 0xd97706 : (canAfford ? 0x16a34a : 0x64748b);
        const btnLabel = isOwnListing ? '↩ REMOVER ANÚNCIO' : (canAfford ? '⚡ COMPRAR CARTA' : '🔒 SALDO INSUFICIENTE');

        const actionBtnBg = this.add.rectangle(0, 95, 300, 50, btnColor)
            .setStrokeStyle(3, 0x090d16)
            .setInteractive({ useHandCursor: isOwnListing || canAfford })
            .on('pointerdown', () => {
                if (isOwnListing) {
                    this.handleCancelListing(listing);
                } else if (canAfford) {
                    this.handleBuyListing(listing);
                } else {
                    this.showShopToast('Saldo de Gold insuficiente para comprar esta carta!');
                }
            });

        const actionBtnTxt = this.add.text(0, 95, btnLabel, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '15px',
            color: '#ffffff',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        card.add([bg, header, title, sellerTxt, priceBadge, priceTxt, actionBtnBg, actionBtnTxt]);
        this.productsContainer.add(card);
    }

    createProductCard(x, y, product) {
        const card = this.add.container(x, y);
        const w = 740;
        const h = 640;

        const canAfford = this.getGoldBalance() >= product.price;

        const shadow = this.add.rectangle(10, 12, w, h, 0x090d16, 0.8);
        const bg = this.add.rectangle(0, 0, w, h, 0xfaf8f5, 1).setStrokeStyle(4, 0x090d16);
        const header = this.add.rectangle(0, -h / 2 + 42, w - 8, 76, 0x090d16);
        const stripe = this.add.rectangle(0, -h / 2 + 80, w - 8, 8, 0xe60012);

        const title = this.add.text(0, -h / 2 + 42, product.name.toUpperCase(), {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '22px',
            color: '#ffe600',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        // Ilustração Central do Pacote / Deck usando o card_back SVG
        const packVisual = this.add.image(0, -45, 'card_back').setDisplaySize(175, 238);
        if (product.type === 'DECK') {
            const backStack = this.add.image(-14, -38, 'card_frame_hero').setDisplaySize(175, 238).setAngle(-6);
            card.add([shadow, bg, header, stripe, title, backStack, packVisual]);
        } else {
            card.add([shadow, bg, header, stripe, title, packVisual]);
        }

        const desc = this.add.text(0, 125, product.description || '', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '16px',
            color: '#090d16',
            fontStyle: 'bold',
            align: 'center',
            wordWrap: { width: w - 80 }
        }).setOrigin(0.5).setResolution(2);

        const priceBadge = this.add.rectangle(0, 200, 260, 42, 0xe6dec8).setStrokeStyle(2, 0x090d16);
        const priceTxt = this.add.text(0, 200, `CUSTO: 🪙 ${product.price} GOLD`, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '16px',
            color: canAfford ? '#15803d' : '#dc2626',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        const buyBtnBg = this.add.rectangle(0, 265, 380, 58, canAfford ? 0x16a34a : 0x64748b)
            .setStrokeStyle(3, 0x090d16)
            .setInteractive({ useHandCursor: canAfford })
            .on('pointerdown', () => {
                if (!canAfford) {
                    this.showShopToast('Saldo de Gold insuficiente para esta compra!');
                    return;
                }
                this.handlePurchaseProduct(product);
            });

        const buyBtnTxt = this.add.text(0, 265, canAfford ? '⚡ ADQUIRIR AGORA' : '🔒 SALDO INSUFICIENTE', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '17px',
            color: '#ffffff',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        card.add([desc, priceBadge, priceTxt, buyBtnBg, buyBtnTxt]);
        this.productsContainer.add(card);
    }

    async handlePurchaseProduct(product) {
        if (this.isOpeningBooster || this.isProcessingTransaction) return;
        this.isProcessingTransaction = true;

        try {
            const idempotencyKey = `BUY_PROD_${this.player.uuid}_${product.id}_${Date.now()}`;
            const res = await fetch('http://localhost:8080/api/shop/buy', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Idempotency-Key': idempotencyKey
                },
                body: JSON.stringify({
                    playerId: this.player.uuid,
                    productId: product.id
                })
            });

            const data = await res.json();
            if (!res.ok) {
                this.showShopToast(data.message || 'Erro ao processar compra.');
                return;
            }

            this.player = data.updatedPlayer;
            localStorage.setItem('lendas_player', JSON.stringify(this.player));
            this.balanceText.setText(`🪙 GOLD: ${this.getGoldBalance()}`);
            this.renderProductsGrid();

            if (data.productType === 'BOOSTER' && data.openedBoosterCards?.length > 0) {
                this.playBoosterOpeningSequence(product.name, data.openedBoosterCards);
            } else {
                this.showShopToast(data.message || 'Compra concluída com sucesso!', '#16a34a');
            }
        } catch (err) {
            this.showShopToast('Erro de conexão com o servidor da Loja.');
        } finally {
            this.isProcessingTransaction = false;
        }
    }

    async handleBuyListing(listing) {
        if (this.isProcessingTransaction) return;
        this.isProcessingTransaction = true;

        try {
            const res = await fetch(`http://localhost:8080/api/shop/listings/${listing.id}/buy`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Idempotency-Key': `BUY_LISTING_${listing.id}_${this.player.uuid}`
                },
                body: JSON.stringify({ playerId: this.player.uuid })
            });

            const data = await res.json();
            if (!res.ok) {
                this.showShopToast(data.message || 'Erro ao comprar carta.');
                return;
            }

            this.player = data.player;
            localStorage.setItem('lendas_player', JSON.stringify(this.player));
            this.balanceText.setText(`🪙 GOLD: ${this.getGoldBalance()}`);

            await this.fetchMarketListingsFromServer();
            this.renderProductsGrid();
            this.showShopToast(data.message || 'Carta adquirida com sucesso!', '#16a34a');
        } catch (e) {
            this.showShopToast('Erro de conexão com o servidor.');
        } finally {
            this.isProcessingTransaction = false;
        }
    }

    async handleCancelListing(listing) {
        if (this.isProcessingTransaction) return;
        this.isProcessingTransaction = true;

        try {
            const res = await fetch(`http://localhost:8080/api/shop/listings/${listing.id}/cancel`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Idempotency-Key': `CANCEL_LISTING_${listing.id}_${this.player.uuid}`
                },
                body: JSON.stringify({ playerId: this.player.uuid })
            });

            const data = await res.json();
            if (!res.ok) {
                this.showShopToast(data.message || 'Erro ao cancelar anúncio.');
                return;
            }

            this.player = data.player;
            localStorage.setItem('lendas_player', JSON.stringify(this.player));
            await this.fetchMarketListingsFromServer();
            this.renderProductsGrid();
            this.showShopToast(data.message || 'Anúncio removido!', '#ca8a04');
        } catch (e) {
            this.showShopToast('Erro de conexão com o servidor.');
        } finally {
            this.isProcessingTransaction = false;
        }
    }

    createBoosterRevealModal() {
        this.revealModal = this.add.container(960, 540).setDepth(300).setVisible(false);

        const overlay = this.add.rectangle(0, 0, 1920, 1080, 0x050508, 0.92).setInteractive();
        const frame = this.add.rectangle(0, 0, 1680, 820, 0x181c24, 0.98).setStrokeStyle(4, 0xffe600);

        this.revealTitle = this.add.text(0, -350, '╔══ ABERTURA DE PACOTE // CLIQUE NAS CARTAS PARA REVELAR ══╗', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '22px',
            color: '#ffe600',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        this.revealCardsGroup = this.add.container(0, -15);

        this.closeRevealBtn = this.add.rectangle(0, 340, 360, 56, 0xe60012)
            .setStrokeStyle(3, 0xffffff)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => {
                this.isOpeningBooster = false;
                this.revealModal.setVisible(false);
            });

        this.closeRevealTxt = this.add.text(0, 340, 'COLECIONAR E FECHAR [OK]', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '16px',
            color: '#ffffff',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        this.revealModal.add([overlay, frame, this.revealTitle, this.revealCardsGroup, this.closeRevealBtn, this.closeRevealTxt]);
    }

    playBoosterOpeningSequence(packName, pulledCards) {
        this.isOpeningBooster = true;
        this.revealModal.setVisible(true);
        this.revealCardsGroup.removeAll(true);
        this.revealTitle.setText(`╔══ ${packName.toUpperCase()} // CLIQUE NAS CARTAS PARA REVELAR ══╗`);

        const total = pulledCards.length;
        const spacing = 310;
        const startX = -((total - 1) * spacing) / 2;

        pulledCards.forEach((cardDto, index) => {
            const targetX = startX + index * spacing;
            const slotContainer = this.add.container(0, 0);

            // Começa virada para baixo (card_back)
            const cardBack = this.add.image(0, 0, 'card_back').setDisplaySize(260, 352);
            slotContainer.add(cardBack);
            this.revealCardsGroup.add(slotContainer);

            // Animação Stepped VGA distribuindo do centro para os 5 slots
            this.tweens.add({
                targets: slotContainer,
                x: targetX,
                duration: 360,
                delay: index * 90,
                ease: 'Stepped',
                easeParams: [6]
            });

            let isRevealed = false;
            cardBack.setInteractive({ useHandCursor: true }).on('pointerdown', () => {
                if (isRevealed) return;
                isRevealed = true;
                this.flipAndRevealBoosterCard(slotContainer, cardBack, cardDto);
            });
        });
    }

    flipAndRevealBoosterCard(slotContainer, cardBack, cardDto) {
        const rarityColors = {
            COMUM: 0x94a3b8,
            INCOMUM: 0x55ffff,
            RARA: 0xffff55,
            LENDARIA: 0xff55ff
        };
        const glowColor = rarityColors[cardDto.rarity] || 0xffffff;

        // Fecha horizontalmente em 3 passos discretos VGA, troca para a frente clara e abre!
        this.tweens.add({
            targets: slotContainer,
            scaleX: 0,
            duration: 150,
            ease: 'Stepped',
            easeParams: [3],
            onComplete: () => {
                cardBack.destroy();

                const w = 260;
                const h = 352;
                const glowRect = this.add.rectangle(0, 0, w + 12, h + 12, glowColor, 1)
                    .setStrokeStyle(3, 0x090d16);
                const bgPaper = this.add.rectangle(0, 0, w, h, 0xfaf8f5, 1);
                const frameKey = cardDto.isHero ? 'card_frame_hero' : 'card_frame_playable';
                const frameImg = this.add.image(0, 0, frameKey).setDisplaySize(w, h);

                const nameTxt = this.add.text(0, -h / 2 + 20, cardDto.cardName, {
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '13.5px',
                    color: '#090d16',
                    fontStyle: 'bold',
                    align: 'center',
                    wordWrap: { width: w * 0.82 }
                }).setOrigin(0.5).setResolution(2);

                const rarityBadge = this.add.rectangle(0, -28, w - 24, 26, 0x090d16);
                const rarityTxt = this.add.text(0, -28, `★ ${cardDto.rarity} • ${cardDto.cardType}`, {
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '11.5px',
                    color: '#ffe600',
                    fontStyle: 'bold'
                }).setOrigin(0.5).setResolution(2);

                const descTxt = this.add.text(-w / 2 + 16, -4, cardDto.descricao || '', {
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '11.5px',
                    color: '#090d16',
                    fontStyle: 'bold',
                    align: 'left',
                    wordWrap: { width: w - 36 }
                }).setOrigin(0, 0).setResolution(2);

                const ownedTag = this.add.text(0, h / 2 + 22, `Na Coleção: ${cardDto.ownedCopiesAfterPull}x`, {
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '13px',
                    color: '#55ffff',
                    fontStyle: 'bold'
                }).setOrigin(0.5).setResolution(2);

                slotContainer.add([glowRect, bgPaper, frameImg, nameTxt, rarityBadge, rarityTxt, descTxt, ownedTag]);

                this.tweens.add({
                    targets: slotContainer,
                    scaleX: 1,
                    duration: 180,
                    ease: 'Stepped',
                    easeParams: [3]
                });
            }
        });
    }

    showShopToast(msg, bgColor = '#b91c1c') {
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
            delay: 2200,
            duration: 400,
            onComplete: () => toast.destroy()
        });
    }
}

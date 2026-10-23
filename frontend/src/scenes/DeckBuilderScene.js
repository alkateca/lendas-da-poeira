import Phaser from 'phaser';
import { BOARD_LAYOUT } from '../config/BoardCoordinates.js';
import DeckRestrictionValidator from '../managers/DeckRestrictionValidator.js';

export default class DeckBuilderScene extends Phaser.Scene {
    constructor() {
        super({ key: 'DeckBuilderScene' });
        this.player = null;
        this.catalogHeroes = [];
        this.catalogCards = [];

        // Estado do baralho em edição
        this.currentDeck = null;
        this.activeTab = 'HEROES'; // 'HEROES', 'LIBERATION', 'ALL_CARDS', 'ITEM', 'SPELL', 'ACTION', 'THEURGY'
        this.hideLockedCards = false;
        this.currentPage = 0;
        this.itemsPerPage = 10;
        this.longHoverTimer = null;
    }

    init(data) {
        this.player = data.player || JSON.parse(localStorage.getItem('lendas_player'));
        if (!this.player.decks) this.player.decks = [];
    }

    async create() {
        this.drawBackgroundAndFrames();
        this.createTopBar();
        this.createHeroTeamSection();
        this.createCatalogSection();
        this.createRightDeckListSection();
        this.createLargePreviewOverlay();

        await this.loadCatalogFromServer();

        // Carrega o primeiro baralho salvo do jogador ou cria um novo rascunho
        if (this.player.decks.length > 0) {
            this.loadDeckIntoEditor(this.player.decks[0]);
        } else {
            this.createNewBlankDeck();
        }
    }

    drawBackgroundAndFrames() {
        const g = this.add.graphics();
        g.lineStyle(1, 0x1e293b, 0.35);
        for (let x = 0; x < 1920; x += 80) g.lineBetween(x, 0, x, 1080);
        for (let y = 0; y < 1080; y += 80) g.lineBetween(0, y, 1920, y);

        // Moldura Barra Superior
        this.add.rectangle(960, 48, 1860, 68, 0x0f172a, 0.95).setStrokeStyle(2, 0x334155);

        // Moldura Passo 1: Equipe de 3 Heróis + Slot de Carta de Liberação
        this.add.rectangle(690, 215, 1320, 230, 0x111827, 0.9).setStrokeStyle(2, 0x1d4ed8);

        // Moldura Passo 2: Catálogo de Cartas
        this.add.rectangle(690, 695, 1320, 690, 0x111827, 0.9).setStrokeStyle(2, 0x334155);

        // Moldura Direita: Lista do Baralho Atual
        this.add.rectangle(1635, 570, 510, 940, 0x0f172a, 0.95).setStrokeStyle(2, BOARD_LAYOUT.COLORS.GOLD_DUST);
    }

    createTopBar() {
        this.add.text(45, 48, '🃏 MONTADOR DE BARALHOS', {
            fontSize: '22px', color: '#ffd700', fontStyle: 'bold'
        }).setOrigin(0, 0.5);

        // Input HTML para o Nome do Baralho (deckName)
        const inputHtml = `
            <input type="text" id="deck-name-input" placeholder="Nome do Baralho..."
                style="width: 280px; height: 38px; font-size: 15px; padding: 0 12px;
                background: #1e293b; color: #ffffff; border: 2px solid #38bdf8;
                border-radius: 6px; font-weight: bold; outline: none;" />
        `;
        this.deckNameDom = this.add.dom(530, 48).createFromHTML(inputHtml);

        // Botão Alternar entre Baralhos Salvos
        this.deckCycleBtn = this.createUIButton(830, 48, 250, 42, 'Meus Decks (0)', 0x1e293b, () => {
            if (this.player.decks.length === 0) return;
            const currentIdx = this.player.decks.findIndex(d => d.uuid === this.currentDeck?.uuid);
            const nextIdx = (currentIdx + 1) % this.player.decks.length;
            this.loadDeckIntoEditor(this.player.decks[nextIdx]);
        });

        // Botão + Novo Deck
        this.createUIButton(1055, 48, 160, 42, '+ NOVO DECK', 0x2563eb, () => {
            this.createNewBlankDeck();
        });

        // Botão Salvar Deck
        this.createUIButton(1245, 48, 180, 42, '💾 SALVAR DECK', 0x16a34a, () => {
            this.saveCurrentDeck();
        });

        // Botão Definir como Principal (Para Jogar)
        this.createUIButton(1465, 48, 220, 42, '★ USAR EM PARTIDA', 0xca8a04, () => {
            this.setAsCurrentPlayableDeck();
        });

        // Botão Voltar ao Menu
        this.createUIButton(1745, 48, 260, 42, '⬅ VOLTAR AO MENU', 0x475569, () => {
            this.scene.start('MainMenuScene', { player: this.player });
        });
    }

    createHeroTeamSection() {
        this.add.text(50, 122, 'PASSO 1: ESCOLHA OS 3 HERÓIS E SUA CARTA DE LIBERAÇÃO (FORA DO BARALHO)', {
            fontSize: '14px', color: '#60a5fa', fontStyle: 'bold'
        });

        this.selectedHeroesContainer = this.add.container(0, 0);

        // Painel de Permissões Desbloqueadas pelos 3 Heróis (ajustado à direita do slot de Liberação)
        this.add.rectangle(1065, 225, 510, 175, 0x0f172a).setStrokeStyle(1, 0x334155);
        this.permissionsTitle = this.add.text(825, 152, '🔓 PERMISSÕES LIBERADAS PELA EQUIPE:', {
            fontSize: '13.5px', color: '#ffd700', fontStyle: 'bold'
        });
        this.permissionsBody = this.add.text(825, 178, '', {
            fontSize: '12px', color: '#e2e8f0', lineSpacing: 5, wordWrap: { width: 480 }
        });
    }

    createCatalogSection() {
        const tabs = [
            { key: 'HEROES',     label: '1. HERÓIS',       x: 105, w: 125 },
            { key: 'LIBERATION', label: '★ LIBERAÇÃO',     x: 240, w: 130 },
            { key: 'ALL_CARDS',  label: '2. TODAS CARTAS', x: 382, w: 140 },
            { key: 'ITEM',       label: 'ITENS / ARMAS',   x: 530, w: 136 },
            { key: 'SPELL',      label: 'MAGIAS',          x: 668, w: 120 },
            { key: 'ACTION',     label: 'AÇÕES',           x: 790, w: 110 },
            { key: 'THEURGY',    label: 'TEURGIAS',        x: 905, w: 115 }
        ];

        this.tabButtons = [];
        tabs.forEach(t => {
            const btn = this.createUIButton(t.x, 382, t.w, 36, t.label, 0x1e293b, () => {
                this.activeTab = t.key;
                this.currentPage = 0;
                this.refreshCatalogGrid();
            });
            this.tabButtons.push({ key: t.key, ...btn });
        });

        // Toggle para Ocultar Cartas Bloqueadas
        this.filterLockedBtn = this.createUIButton(1080, 382, 200, 36, '☐ Ocultar Bloqueadas', 0x334155, () => {
            this.hideLockedCards = !this.hideLockedCards;
            this.filterLockedBtn.txt.setText(this.hideLockedCards ? '☑ Ocultar Bloqueadas' : '☐ Ocultar Bloqueadas');
            this.currentPage = 0;
            this.refreshCatalogGrid();
        });

        // Paginação
        this.createUIButton(1215, 382, 44, 36, '◀', 0x334155, () => {
            if (this.currentPage > 0) {
                this.currentPage--;
                this.refreshCatalogGrid();
            }
        });
        this.pageLabel = this.add.text(1265, 382, '1/1', { fontSize: '13px', color: '#ffffff', fontStyle: 'bold' }).setOrigin(0.5);
        this.createUIButton(1315, 382, 44, 36, '▶', 0x334155, () => {
            this.currentPage++;
            this.refreshCatalogGrid();
        });

        this.catalogGridContainer = this.add.container(0, 0);
    }

    createRightDeckListSection() {
        this.add.text(1635, 130, 'LISTA DO BARALHO ATUAL', {
            fontSize: '18px', color: '#ffd700', fontStyle: 'bold'
        }).setOrigin(0.5);

        this.deckStatusBadge = this.add.text(1635, 165, 'Heróis: 0/3 | Cartas: 0/30 | Liberação: 0/1', {
            fontSize: '13.5px', color: '#f87171', fontStyle: 'bold'
        }).setOrigin(0.5);

        this.deckPlayableWarning = this.add.text(1635, 192, '⚠️ INCOMPLETO (Indisponível para Jogar)', {
            fontSize: '12px', color: '#fbbf24', fontStyle: 'bold'
        }).setOrigin(0.5);

        this.deckListContainer = this.add.container(1400, 225);
    }

    async loadCatalogFromServer() {
        try {
            const res = await fetch('http://localhost:8080/api/players/catalog');
            if (!res.ok) throw new Error('Falha ao buscar catálogo');
            const data = await res.json();
            this.catalogHeroes = data.heroes || [];
            this.catalogCards = data.cards || [];
        } catch (e) {
            console.warn('Servidor offline ao carregar catálogo. Usando dados de fallback...');
            const { INITIAL_MATCH_SNAPSHOT } = await import('../config/mockSnapshots.js');
            this.catalogHeroes = [
                ...INITIAL_MATCH_SNAPSHOT.localPlayer.heroes,
                ...INITIAL_MATCH_SNAPSHOT.opponentPlayer.heroes
            ];
            this.catalogCards = INITIAL_MATCH_SNAPSHOT.localPlayer.hand;
        }

        // Garante que as Cartas de Liberação padrão estejam disponíveis no catálogo mesmo se ainda não cadastradas no cards.json
        this.ensureDefaultLiberationCardsInCatalog();
    }

    ensureDefaultLiberationCardsInCatalog() {
        const defaultLiberations = [
            {
                cardName: 'Sagração da Santa das Lâminas',
                cardType: 'THEURGY',
                color: 'WHITE',
                unica: true,
                liberationCard: true,
                missionType: 'PLAY_SPELL',
                missionTargetValue: 2,
                targetBaseHeroName: 'Moyra, Aprendiz da Santa',
                evolvesIntoHeroName: 'Moyra, Santa das Laminas',
                descricao: 'Missão (Jogar 2 Feitiços): Entra na mão.\nQuando jogada por Moyra, Aprendiz da Santa, evolui para Moyra, Santa das Lâminas e joga 1 carta da sua mão à sua escolha. Exilar.'
            },
            {
                cardName: 'Ascensão da Horda Lendária',
                cardType: 'ACTION',
                color: 'GREEN',
                unica: true,
                liberationCard: true,
                missionType: 'ALLY_HEROES_DEAD',
                missionTargetValue: 2,
                targetBaseHeroName: 'Esquadrão Goblin',
                evolvesIntoHeroName: 'Heróis Lendários dos Goblin',
                descricao: 'Missão (2 Aliados Mortos): Entra na mão.\nRevive 1 aliado morto como Heróis Lendários dos Goblin, somando +2 ESP, +3 ATQ, +2 DEF, +8 HP e herdando seus efeitos. Exilar.'
            },
            {
                cardName: 'Despertar do Cavaleiro Bestial',
                cardType: 'ACTION',
                color: 'RED',
                unica: true,
                liberationCard: true,
                missionType: 'DEAL_DAMAGE',
                missionTargetValue: 10,
                targetBaseHeroName: 'Kael, Domador de Feras',
                evolvesIntoHeroName: 'Kael, Cavaleiro Bestial',
                descricao: 'Missão (Causar 10 de Dano): Entra na mão.\nQuando jogada por Kael, evolui para Kael, Cavaleiro Bestial e joga 1 carta da sua mão. Exilar.'
            }
        ];

        defaultLiberations.forEach(defLib => {
            const existing = this.catalogCards.find(c => c.cardName === defLib.cardName);
            if (existing) {
                Object.assign(existing, defLib);
            } else {
                this.catalogCards.push(defLib);
            }
        });
    }

    createNewBlankDeck() {
        this.currentDeck = {
            uuid: null,
            deckName: `Novo Baralho ${this.player.decks.length + 1}`,
            heroList: [],
            cardList: [],
            liberationList: []
        };
        this.activeTab = 'HEROES';
        this.currentPage = 0;
        this.syncInputDeckName(this.currentDeck.deckName);
        this.refreshAllViews();
    }

    loadDeckIntoEditor(deck) {
        this.currentDeck = JSON.parse(JSON.stringify(deck));
        if (!this.currentDeck.heroList) this.currentDeck.heroList = [];
        if (!this.currentDeck.cardList) this.currentDeck.cardList = [];
        if (!this.currentDeck.liberationList) this.currentDeck.liberationList = [];
        this.syncInputDeckName(this.currentDeck.deckName || 'Baralho Sem Nome');
        this.refreshAllViews();
    }

    syncInputDeckName(name) {
        const input = document.getElementById('deck-name-input');
        if (input) input.value = name;
    }

    getInputDeckName() {
        const input = document.getElementById('deck-name-input');
        return input && input.value.trim() ? input.value.trim() : 'Baralho Sem Nome';
    }

    refreshAllViews() {
        this.deckCycleBtn.txt.setText(`Alternar Deck (${this.player.decks.length})`);
        this.renderSelectedHeroesAndPermissions();
        this.refreshCatalogGrid();
        this.renderRightDeckCardList();
    }

    renderSelectedHeroesAndPermissions() {
        this.selectedHeroesContainer.removeAll(true);
        const heroes = this.currentDeck.heroList;

        // 1. Renderiza os 3 Slots de Heróis
        for (let i = 0; i < 3; i++) {
            const slotX = 125 + i * 170;
            const slotY = 232;
            const hero = heroes[i];

            const slotRect = this.add.rectangle(slotX, slotY, 152, 148, 0x1e293b, 0.8)
                .setStrokeStyle(2, hero ? 0x00ff88 : 0x475569);
            this.selectedHeroesContainer.add(slotRect);

            if (hero) {
                const colorHex = BOARD_LAYOUT.COLORS.CARD_COLORS[hero.color] || 0x475569;
                const bar = this.add.rectangle(slotX, slotY - 56, 144, 26, colorHex);
                const name = this.add.text(slotX, slotY - 56, hero.cardName, {
                    fontSize: '10px', color: '#ffffff', fontStyle: 'bold', align: 'center', wordWrap: { width: 138 }
                }).setOrigin(0.5);

                const stats = this.add.text(slotX, slotY - 12, `ESP:${hero.espirito} ATQ:${hero.ataque}\nDEF:${hero.defesa} HP:${hero.vidaMaxima}`, {
                    fontSize: '10.5px', color: '#ffd700', fontStyle: 'bold', align: 'center'
                }).setOrigin(0.5);

                const removeHint = this.add.text(slotX, slotY + 52, '[REMOVER ✖]', {
                    fontSize: '10px', color: '#f87171', fontStyle: 'bold'
                }).setOrigin(0.5);

                slotRect.setInteractive({ useHandCursor: true })
                    .on('pointerdown', () => {
                        this.currentDeck.heroList.splice(i, 1);
                        this.pruneInvalidCardsAfterHeroChange();
                        this.refreshAllViews();
                    })
                    .on('pointerover', () => this.startLongHover(hero, true))
                    .on('pointerout', () => this.cancelLongHover());

                this.selectedHeroesContainer.add([bar, name, stats, removeHint]);
            } else {
                const emptyTxt = this.add.text(slotX, slotY, `HERÓI ${i + 1}/3\n(Vazio)`, {
                    fontSize: '12px', color: '#64748b', align: 'center', fontStyle: 'bold'
                }).setOrigin(0.5);
                this.selectedHeroesContainer.add(emptyTxt);
            }
        }

        // 2. Renderiza o Slot Especial da Carta de Liberação (Fora do Baralho)
        const libSlotX = 685;
        const libSlotY = 232;
        const activeLibCard = this.currentDeck.liberationList?.[0] || null;

        const libRect = this.add.rectangle(libSlotX, libSlotY, 175, 148, 0x271a0c, 0.9)
            .setStrokeStyle(2, activeLibCard ? 0xfacc15 : 0xca8a04);
        this.selectedHeroesContainer.add(libRect);

        if (activeLibCard) {
            const bar = this.add.rectangle(libSlotX, libSlotY - 56, 167, 26, 0xca8a04);
            const name = this.add.text(libSlotX, libSlotY - 56, activeLibCard.cardName, {
                fontSize: '9.5px', color: '#000000', fontStyle: 'bold', align: 'center', wordWrap: { width: 160 }
            }).setOrigin(0.5);

            const targetInfo = activeLibCard.evolvesIntoHeroName
                ? `Evolui:\n${activeLibCard.evolvesIntoHeroName}`
                : `Missão: ${activeLibCard.missionType || 'Especial'}`;

            const infoTxt = this.add.text(libSlotX, libSlotY - 8, targetInfo, {
                fontSize: '10px', color: '#fef08a', fontStyle: 'bold', align: 'center', wordWrap: { width: 158 }
            }).setOrigin(0.5);

            const removeHint = this.add.text(libSlotX, libSlotY + 52, '[REMOVER ✖]', {
                fontSize: '10px', color: '#f87171', fontStyle: 'bold'
            }).setOrigin(0.5);

            libRect.setInteractive({ useHandCursor: true })
                .on('pointerdown', () => {
                    this.currentDeck.liberationList = [];
                    this.refreshAllViews();
                })
                .on('pointerover', () => this.startLongHover(activeLibCard, false))
                .on('pointerout', () => this.cancelLongHover());

            this.selectedHeroesContainer.add([bar, name, infoTxt, removeHint]);
        } else {
            const emptyLibTxt = this.add.text(libSlotX, libSlotY, `★ CARTA DE\nLIBERAÇÃO (0/1)\n(Clique p/ escolher)`, {
                fontSize: '11px', color: '#fbbf24', align: 'center', fontStyle: 'bold'
            }).setOrigin(0.5);

            libRect.setInteractive({ useHandCursor: true })
                .on('pointerdown', () => {
                    this.activeTab = 'LIBERATION';
                    this.currentPage = 0;
                    this.refreshCatalogGrid();
                });

            this.selectedHeroesContainer.add(emptyLibTxt);
        }

        // 3. Atualiza texto de Permissões da Equipe
        const team = DeckRestrictionValidator.analyzeHeroTeam(heroes);
        const colorDesc = team.hasWhiteHero
            ? 'CORINGA BRANCO ATIVO (Apenas WHITE e NEUTRAL)'
            : (team.baseColor || 'Nenhuma (Escolha o 1º Herói)');

        this.permissionsBody.setText(
            `• Cor Permitida: ${colorDesc}\n` +
            `• Afinidades: ${team.affinities.size > 0 ? Array.from(team.affinities).join(', ') : 'Nenhuma'}\n` +
            `• Ordens / Raças: ${team.ordersAndRaces.size > 0 ? Array.from(team.ordersAndRaces).join(', ') : 'Nenhuma'}\n` +
            `• Classes: ${team.classes.size > 0 ? Array.from(team.classes).join(', ') : 'Nenhuma'}`
        );
    }

    /**
     * Valida se uma Carta de Liberação pode ser equipada pela equipe de Heróis atual
     */
    canAddLiberationCard(card) {
        if (!this.currentDeck.liberationList) this.currentDeck.liberationList = [];
        if (this.currentDeck.liberationList.length >= 1) {
            return { allowed: false, reason: 'Já possui 1 Carta de Liberação equipada' };
        }
        if (this.currentDeck.heroList.length === 0) {
            return { allowed: false, reason: 'Escolha seus Heróis primeiro' };
        }
        if (card.targetBaseHeroName) {
            const hasRequiredHero = this.currentDeck.heroList.some(
                h => h.cardName && h.cardName.toLowerCase() === card.targetBaseHeroName.toLowerCase()
            );
            if (!hasRequiredHero) {
                return { allowed: false, reason: `Requer Herói: ${card.targetBaseHeroName}` };
            }
        }
        return { allowed: true, reason: 'Equipar na Zona de Liberação' };
    }

    pruneInvalidCardsAfterHeroChange() {
        // Revalida a Carta de Liberação caso o Herói exigido tenha sido removido
        if (this.currentDeck.liberationList?.length > 0) {
            const libCard = this.currentDeck.liberationList[0];
            if (libCard.targetBaseHeroName) {
                const stillHasHero = this.currentDeck.heroList.some(
                    h => h.cardName && h.cardName.toLowerCase() === libCard.targetBaseHeroName.toLowerCase()
                );
                if (!stillHasHero) {
                    this.currentDeck.liberationList = [];
                }
            }
        }

        if (this.currentDeck.heroList.length < 3) return;
        const validCards = [];
        this.currentDeck.cardList.forEach(card => {
            const check = DeckRestrictionValidator.canAddCardToDeck(card, this.currentDeck.heroList, validCards);
            if (check.allowed) {
                validCards.push(card);
            }
        });
        this.currentDeck.cardList = validCards;
    }

    refreshCatalogGrid() {
        this.catalogGridContainer.removeAll(true);

        // Destaca a aba ativa
        this.tabButtons.forEach(tb => {
            const isTabActive = tb.key === this.activeTab;
            const defaultColor = tb.key === 'LIBERATION' ? 0x422006 : 0x1e293b;
            tb.bg.setFillStyle(isTabActive ? 0x2563eb : defaultColor);
        });

        let items = [];
        const isHeroTab = this.activeTab === 'HEROES';
        const isLiberationTab = this.activeTab === 'LIBERATION';

        if (isHeroTab) {
            items = this.catalogHeroes.map(h => ({
                item: h,
                isHero: true,
                isLiberation: false,
                validation: DeckRestrictionValidator.canSelectHero(h, this.currentDeck.heroList)
            }));
        } else if (isLiberationTab) {
            const liberationCards = this.catalogCards.filter(c => c.liberationCard);
            items = liberationCards.map(c => ({
                item: c,
                isHero: false,
                isLiberation: true,
                validation: this.canAddLiberationCard(c)
            }));
        } else {
            // Abas normais do baralho de 30 cartas (exclui cartas de liberação para não ocuparem vaga nas 30)
            let filteredCards = this.catalogCards.filter(c => !c.liberationCard);
            if (this.activeTab !== 'ALL_CARDS') {
                filteredCards = filteredCards.filter(c => c.cardType === this.activeTab);
            }
            items = filteredCards.map(c => ({
                item: c,
                isHero: false,
                isLiberation: false,
                validation: DeckRestrictionValidator.canAddCardToDeck(c, this.currentDeck.heroList, this.currentDeck.cardList)
            }));
        }

        if (this.hideLockedCards) {
            items = items.filter(entry => entry.validation.allowed);
        }

        const totalPages = Math.max(1, Math.ceil(items.length / this.itemsPerPage));
        if (this.currentPage >= totalPages) this.currentPage = totalPages - 1;
        this.pageLabel.setText(`${this.currentPage + 1}/${totalPages}`);

        const pageSlice = items.slice(this.currentPage * this.itemsPerPage, (this.currentPage + 1) * this.itemsPerPage);

        pageSlice.forEach((entry, idx) => {
            const col = idx % 5;
            const row = Math.floor(idx / 5);
            const x = 165 + col * 255;
            const y = 535 + row * 285;

            this.createCatalogCardTile(x, y, entry.item, entry.isHero, entry.isLiberation, entry.validation);
        });
    }

    createCatalogCardTile(x, y, data, isHero, isLiberation, validation) {
        const tile = this.add.container(x, y);
        const w = 225;
        const h = 255;

        const accentColor = isLiberation
            ? 0xca8a04
            : (BOARD_LAYOUT.COLORS.CARD_COLORS[data.color] || 0x475569);
        const borderColor = validation.allowed ? (isLiberation ? 0xfacc15 : 0x00ff88) : 0xff3333;

        const bg = this.add.rectangle(0, 0, w, h, isLiberation ? 0x271a0c : 0x1e2430)
            .setStrokeStyle(2, borderColor)
            .setAlpha(validation.allowed ? 1.0 : 0.48);

        const header = this.add.rectangle(0, -h / 2 + 18, w - 6, 30, accentColor);
        const title = this.add.text(0, -h / 2 + 18, data.cardName, {
            fontSize: '11.5px', color: '#ffffff', fontStyle: 'bold', align: 'center', wordWrap: { width: w - 14 }
        }).setOrigin(0.5);

        const subType = isHero
            ? `HERÓI • ${data.color}${data.afinidades?.length ? ' • ' + data.afinidades.join('/') : ''}`
            : (isLiberation
                ? `★ LIBERAÇÃO (${data.cardType})`
                : `${data.cardType}${data.unica ? ' ★ ÚNICA' : ''}${data.itemType && data.itemType !== 'NENHUM' ? ' • ' + data.itemType : ''}`);

        const badge = this.add.text(0, -h / 2 + 46, subType, {
            fontSize: '10px', color: '#ffd700', fontStyle: 'bold', align: 'center', wordWrap: { width: w - 12 }
        }).setOrigin(0.5);

        const desc = this.add.text(0, 8, data.descricao || '', {
            fontSize: '10px', color: '#e2e8f0', align: 'center', wordWrap: { width: w - 20 }
        }).setOrigin(0.5);

        const statusFooter = this.add.rectangle(0, h / 2 - 20, w - 6, 32, validation.allowed ? 0x064e3b : 0x450a0a);
        const statusTxt = this.add.text(0, h / 2 - 20, validation.allowed ? `➕ ${validation.reason}` : `🔒 ${validation.reason}`, {
            fontSize: '10px', color: validation.allowed ? '#4ade80' : '#fca5a5', fontStyle: 'bold', align: 'center', wordWrap: { width: w - 10 }
        }).setOrigin(0.5);

        tile.add([bg, header, title, badge, desc, statusFooter, statusTxt]);

        bg.setInteractive({ useHandCursor: validation.allowed })
            .on('pointerdown', () => {
                if (!validation.allowed) {
                    this.showToast(validation.reason);
                    return;
                }
                if (isHero) {
                    this.currentDeck.heroList.push(data);
                    if (this.currentDeck.heroList.length === 3) {
                        this.activeTab = 'LIBERATION';
                        this.currentPage = 0;
                    }
                } else if (isLiberation) {
                    this.currentDeck.liberationList = [data];
                } else {
                    this.currentDeck.cardList.push(data);
                }
                this.refreshAllViews();
            })
            .on('pointerover', () => this.startLongHover(data, isHero))
            .on('pointerout', () => this.cancelLongHover());

        this.catalogGridContainer.add(tile);
    }

    renderRightDeckCardList() {
        this.deckListContainer.removeAll(true);

        const hCount = this.currentDeck.heroList.length;
        const cCount = this.currentDeck.cardList.length;
        const libCount = this.currentDeck.liberationList?.length || 0;
        const isPlayable = DeckRestrictionValidator.isDeckReadyForMatch(this.currentDeck);

        this.deckStatusBadge.setText(`Heróis: ${hCount}/3  |  Cartas: ${cCount}/30  |  Liberação: ${libCount}/1`);
        this.deckStatusBadge.setColor(isPlayable ? '#4ade80' : '#f87171');

        this.deckPlayableWarning.setText(
            isPlayable
                ? '✅ BARALHO VÁLIDO E DISPONÍVEL PARA JOGAR!'
                : '⚠️ INCOMPLETO (Precisa de 3 Heróis e 30 Cartas no Baralho)'
        );
        this.deckPlayableWarning.setColor(isPlayable ? '#4ade80' : '#fbbf24');

        let rowIdx = 0;

        // Exibe a Carta de Liberação equipada em destaque dourado no topo da lista direita (fora das 30 cartas)
        if (libCount > 0) {
            const libCard = this.currentDeck.liberationList[0];
            const y = rowIdx * 38;
            const rowBg = this.add.rectangle(235, y, 460, 34, 0x422006).setStrokeStyle(1.5, 0xfacc15);
            const txt = this.add.text(16, y, `★ [LIBERAÇÃO] ${libCard.cardName}`, {
                fontSize: '12px', color: '#fef08a', fontStyle: 'bold'
            }).setOrigin(0, 0.5);

            const removeBtn = this.add.text(445, y, '[ - ]', {
                fontSize: '13px', color: '#f87171', fontStyle: 'bold'
            }).setOrigin(1, 0.5).setInteractive({ useHandCursor: true });

            removeBtn.on('pointerdown', () => {
                this.currentDeck.liberationList = [];
                this.refreshAllViews();
            });

            this.deckListContainer.add([rowBg, txt, removeBtn]);
            rowIdx++;
        }

        // Agrupa as 30 cartas do baralho principal por nome
        const grouped = new Map();
        this.currentDeck.cardList.forEach(card => {
            if (!grouped.has(card.cardName)) {
                grouped.set(card.cardName, { card, count: 0 });
            }
            grouped.get(card.cardName).count++;
        });

        grouped.forEach(({ card, count }) => {
            const y = rowIdx * 38;
            const rowBg = this.add.rectangle(235, y, 460, 34, 0x1e293b).setStrokeStyle(1, 0x3b82f6);
            const maxLabel = card.unica ? '1' : '3';
            const txt = this.add.text(16, y, `${count}/${maxLabel}x  ${card.cardName} (${card.cardType})`, {
                fontSize: '12.5px', color: '#ffffff', fontStyle: 'bold'
            }).setOrigin(0, 0.5);

            const removeBtn = this.add.text(445, y, '[ - ]', {
                fontSize: '13px', color: '#f87171', fontStyle: 'bold'
            }).setOrigin(1, 0.5).setInteractive({ useHandCursor: true });

            removeBtn.on('pointerdown', () => {
                const idxToRemove = this.currentDeck.cardList.findIndex(c => c.cardName === card.cardName);
                if (idxToRemove > -1) {
                    this.currentDeck.cardList.splice(idxToRemove, 1);
                    this.refreshAllViews();
                }
            });

            this.deckListContainer.add([rowBg, txt, removeBtn]);
            rowIdx++;
        });
    }

    async saveCurrentDeck() {
        this.currentDeck.deckName = this.getInputDeckName();
        const isPlayable = DeckRestrictionValidator.isDeckReadyForMatch(this.currentDeck);

        try {
            const res = await fetch(`http://localhost:8080/api/players/${this.player.uuid}/decks`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(this.currentDeck)
            });

            if (res.ok) {
                this.player = await res.json();
                localStorage.setItem('lendas_player', JSON.stringify(this.player));
                const saved = this.player.decks[this.player.decks.length - 1];
                if (!this.currentDeck.uuid && saved) this.currentDeck.uuid = saved.uuid;

                this.showToast(
                    isPlayable
                        ? `Baralho "${this.currentDeck.deckName}" salvo e PRONTO para combate!`
                        : `Baralho "${this.currentDeck.deckName}" salvo (Indisponível para jogar: ${this.currentDeck.cardList.length}/30 cartas).`,
                    isPlayable ? '#16a34a' : '#ca8a04'
                );
                this.refreshAllViews();
            }
        } catch (e) {
            if (!this.currentDeck.uuid) {
                this.currentDeck.uuid = 'deck-' + Date.now();
                this.player.decks.push(this.currentDeck);
            } else {
                const idx = this.player.decks.findIndex(d => d.uuid === this.currentDeck.uuid);
                if (idx > -1) this.player.decks[idx] = this.currentDeck;
            }
            localStorage.setItem('lendas_player', JSON.stringify(this.player));
            this.showToast('Baralho salvo localmente!', '#16a34a');
            this.refreshAllViews();
        }
    }

    async setAsCurrentPlayableDeck() {
        if (!DeckRestrictionValidator.isDeckReadyForMatch(this.currentDeck)) {
            this.showToast('Este baralho está incompleto! Precisa de 3 Heróis e exatamente 30 Cartas para jogar.');
            return;
        }
        if (!this.currentDeck.uuid) {
            await this.saveCurrentDeck();
        }

        this.player.currentDeck = this.currentDeck;
        localStorage.setItem('lendas_player', JSON.stringify(this.player));

        try {
            await fetch(`http://localhost:8080/api/players/${this.player.uuid}/active-deck/${this.currentDeck.uuid}`, {
                method: 'PUT'
            });
        } catch (e) {
            // Silencioso se offline
        }

        this.showToast(`"${this.currentDeck.deckName}" definido como seu baralho ativo de batalha!`, '#16a34a');
    }

    createLargePreviewOverlay() {
        this.largePreview = this.add.container(1635, 560).setDepth(300).setVisible(false);
        const bg = this.add.rectangle(0, 0, 320, 420, 0x161b26, 0.98).setStrokeStyle(4, BOARD_LAYOUT.COLORS.GOLD_DUST);
        this.lpTitle = this.add.text(0, -175, '', { fontSize: '18px', color: '#ffd700', fontStyle: 'bold', align: 'center', wordWrap: { width: 290 } }).setOrigin(0.5);
        this.lpMeta = this.add.text(0, -130, '', { fontSize: '12px', color: '#38bdf8', fontStyle: 'bold', align: 'center', wordWrap: { width: 290 } }).setOrigin(0.5);
        this.lpDesc = this.add.text(0, 10, '', { fontSize: '14px', color: '#ffffff', align: 'center', wordWrap: { width: 280 } }).setOrigin(0.5);
        this.lpStats = this.add.text(0, 170, '', { fontSize: '13px', color: '#4ade80', fontStyle: 'bold', align: 'center' }).setOrigin(0.5);

        this.largePreview.add([bg, this.lpTitle, this.lpMeta, this.lpDesc, this.lpStats]);
    }

    startLongHover(data, isHero) {
        this.cancelLongHover();
        this.longHoverTimer = this.time.delayedCall(600, () => {
            this.lpTitle.setText(data.cardName);
            this.lpMeta.setText(
                isHero
                    ? `HERÓI • Cor: ${data.color}`
                    : `${data.liberationCard ? '★ CARTA DE LIBERAÇÃO • ' : ''}${data.cardType} • Cor: ${data.color}${data.unica ? ' • ÚNICA' : ''}`
            );
            this.lpDesc.setText(data.descricao || '');
            this.lpStats.setText(
                isHero
                    ? `ESP: ${data.espirito} | ATQ: ${data.ataque} | DEF: ${data.defesa} | HP: ${data.vidaMaxima}`
                    : (data.liberationCard
                        ? `Meta da Missão: ${data.missionTargetValue || 1} (${data.missionType || 'Especial'})`
                        : (data.empunhadura > 0 ? `Empunhadura: ${data.empunhadura}` : ''))
            );
            this.largePreview.setVisible(true);
        });
    }

    cancelLongHover() {
        if (this.longHoverTimer) {
            this.longHoverTimer.remove(false);
            this.longHoverTimer = null;
        }
        if (this.largePreview) this.largePreview.setVisible(false);
    }

    createUIButton(x, y, w, h, label, color, onClick) {
        const bg = this.add.rectangle(x, y, w, h, color)
            .setStrokeStyle(1.5, 0x94a3b8)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', onClick);
        const txt = this.add.text(x, y, label, { fontSize: '12px', color: '#ffffff', fontStyle: 'bold' }).setOrigin(0.5);
        return { bg, txt };
    }

    showToast(message, bgColor = '#b91c1c') {
        const toast = this.add.text(960, 105, message, {
            fontSize: '15px', color: '#ffffff', backgroundColor: bgColor, padding: { x: 16, y: 8 }, fontStyle: 'bold'
        }).setOrigin(0.5).setDepth(400);

        this.tweens.add({
            targets: toast,
            alpha: 0,
            delay: 2000,
            duration: 400,
            onComplete: () => toast.destroy()
        });
    }
}

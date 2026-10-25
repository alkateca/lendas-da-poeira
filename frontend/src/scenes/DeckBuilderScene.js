import Phaser from 'phaser';
import { BOARD_LAYOUT } from '../config/BoardCoordinates.js';
import DeckRestrictionValidator from '../managers/DeckRestrictionValidator.js';
import CRTPostFXPipeline from '../fx/CRTPostFXPipeline.js';

export default class DeckBuilderScene extends Phaser.Scene {
    constructor() {
        super({ key: 'DeckBuilderScene' });
        this.player = null;
        this.catalogHeroes = [];
        this.catalogCards = [];

        // Estado do baralho em edição
        this.currentDeck = null;
        this.activeTab = 'HEROES'; // 'HEROES', 'LIBERATION', 'ALL_CARDS', 'ITEM', 'SPELL', 'ACTION', 'THEURGY'
        this.hideIncompatibleCards = true; // Filtra dinamicamente cartas incompatíveis com os heróis selecionados
        this.currentPage = 0;
        this.itemsPerPage = 10;
        this.longHoverTimer = null;

        // Estado dos Filtros Cumulativos
        this.filters = {
            search: '',
            color: 'ALL',
            typeOrSubtype: 'ALL',
            raca: 'ALL',
            afinidade: 'ALL',
            classe: 'ALL',
            ordem: 'ALL',
            empunhadura: 'ALL'
        };
    }

    init(data) {
        this.player = data?.player || JSON.parse(localStorage.getItem('lendas_player'));
        if (!this.player.decks) this.player.decks = [];
    }

    preload() {
        this.load.setPath('assets');
        if (!this.textures.exists('ui_p5_crt_button')) {
            this.load.svg('ui_p5_crt_button', 'images/ui/p5_crt_button.svg', { width: 520, height: 84 });
        }
        if (!this.textures.exists('ui_card_frame_phantom')) {
            this.load.svg('ui_card_frame_phantom', 'images/ui/card_frame_phantom.svg', { width: 320, height: 440 });
        }
        if (!this.textures.exists('ui_arena_crt_radar')) {
            this.load.svg('ui_arena_crt_radar', 'images/ui/arena_crt_radar.svg', { width: 800, height: 800 });
        }
    }

    async create() {
        if (this.renderer.pipelines) {
            this.renderer.pipelines.addPostPipeline('CRTPostFX', CRTPostFXPipeline);
            this.cameras.main.setPostPipeline('CRTPostFX');
        }

        this.drawBackgroundAndFrames();
        this.createTopBar();
        this.createHeroTeamSection();
        this.createCatalogSection();
        this.createRightDeckListSection();
        this.createLargePreviewOverlay();

        await this.loadCatalogFromServer();

        if (this.player.decks.length > 0) {
            this.loadDeckIntoEditor(this.player.decks[0]);
        } else {
            this.createNewBlankDeck();
        }
    }

    // Normaliza strings removendo acentos e underscores para comparação consistente entre JSON e Enums
    normalizeKey(val) {
        if (!val) return '';
        return String(val)
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .replace(/_/g, ' ')
            .trim()
            .toUpperCase();
    }

    drawBackgroundAndFrames() {
        const g = this.add.graphics();
        g.lineStyle(1, 0x1e293b, 0.35);
        for (let x = 0; x < 1920; x += 80) g.lineBetween(x, 0, x, 1080);
        for (let y = 0; y < 1080; y += 80) g.lineBetween(0, y, 1920, y);

        // Moldura Barra Superior
        this.add.rectangle(960, 48, 1860, 68, 0x0f172a, 0.95).setStrokeStyle(2, 0x334155);

        // Moldura Passo 1: Equipe de 3 Heróis + Slot de Carta de Liberação
        this.add.rectangle(690, 205, 1320, 210, 0x111827, 0.9).setStrokeStyle(2, 0x1d4ed8);

        // Moldura Passo 2: Catálogo de Cartas e Barra de Filtros
        this.add.rectangle(690, 680, 1320, 720, 0x111827, 0.9).setStrokeStyle(2, 0x334155);

        // Moldura Direita: Lista do Baralho Atual
        this.add.rectangle(1635, 570, 510, 940, 0x0f172a, 0.95).setStrokeStyle(2, BOARD_LAYOUT.COLORS.GOLD_DUST);
    }

    createTopBar() {
        this.add.text(45, 48, '🃏 MONTADOR DE BARALHOS', {
            fontSize: '22px', color: '#ffd700', fontStyle: 'bold'
        }).setOrigin(0, 0.5);

        const inputHtml = `
            <input type="text" id="deck-name-input" class="phantom-crt-input" placeholder="Nome do Baralho..."
                style="width: 260px; height: 36px; font-size: 15px; padding: 0 12px;
                font-weight: bold; outline: none;" />
        `;
        this.deckNameDom = this.add.dom(520, 48).createFromHTML(inputHtml);

        this.deckCycleBtn = this.createUIButton(805, 48, 220, 40, 'Alternar Deck (0)', 0x1e293b, () => {
            if (this.player.decks.length === 0) return;
            const currentIdx = this.player.decks.findIndex(d => d.uuid === this.currentDeck?.uuid);
            const nextIdx = (currentIdx + 1) % this.player.decks.length;
            this.loadDeckIntoEditor(this.player.decks[nextIdx]);
        });

        this.createUIButton(1005, 48, 150, 40, '+ NOVO DECK', 0x2563eb, () => {
            this.createNewBlankDeck();
        });

        this.createUIButton(1175, 48, 165, 40, '💾 SALVAR DECK', 0x16a34a, () => {
            this.saveCurrentDeck();
        });

        this.createUIButton(1375, 48, 205, 40, '★ USAR EM PARTIDA', 0xca8a04, () => {
            this.setAsCurrentPlayableDeck();
        });

        this.createUIButton(1735, 48, 250, 40, '⬅ VOLTAR AO MENU', 0x475569, () => {
            this.scene.start('MainMenuScene', { player: this.player });
        });
    }

    createHeroTeamSection() {
        this.add.text(50, 114, 'PASSO 1: ESCOLHA OS 3 HERÓIS E SUA CARTA DE LIBERAÇÃO (Clique para remover)', {
            fontSize: '13.5px', color: '#60a5fa', fontStyle: 'bold'
        });

        this.selectedHeroesContainer = this.add.container(0, 0);

        this.add.rectangle(1065, 218, 510, 162, 0x0f172a).setStrokeStyle(1, 0x334155);
        this.permissionsTitle = this.add.text(825, 148, '🔓 PERMISSÕES ATIVAS DA EQUIPE:', {
            fontSize: '13px', color: '#ffd700', fontStyle: 'bold'
        });
        this.permissionsBody = this.add.text(825, 172, '', {
            fontSize: '11.5px', color: '#e2e8f0', lineSpacing: 4, wordWrap: { width: 480 }
        });
    }

    createCatalogSection() {
        const tabs = [
            { key: 'HEROES',     label: '1. HERÓIS',       x: 98,  w: 112 },
            { key: 'LIBERATION', label: '★ LIBERAÇÃO',     x: 220, w: 120 },
            { key: 'ALL_CARDS',  label: '2. TODAS CARTAS', x: 352, w: 132 },
            { key: 'ITEM',       label: 'ITENS / ARMAS',   x: 490, w: 124 },
            { key: 'SPELL',      label: 'MAGIAS',          x: 610, w: 96  },
            { key: 'ACTION',     label: 'AÇÕES',           x: 708, w: 90  },
            { key: 'THEURGY',    label: 'TEURGIAS',        x: 806, w: 100 }
        ];

        this.tabButtons = [];
        tabs.forEach(t => {
            const btn = this.createUIButton(t.x, 346, t.w, 34, t.label, 0x1e293b, () => {
                this.activeTab = t.key;
                this.currentPage = 0;
                this.refreshCatalogGrid();
            });
            this.tabButtons.push({ key: t.key, ...btn });
        });

        // Toggle para Ocultar/Exibir incompatíveis com a equipe atual
        this.filterLockedBtn = this.createUIButton(965, 346, 195, 34, '☑ Só Compatíveis', 0x065f46, () => {
            this.hideIncompatibleCards = !this.hideIncompatibleCards;
            this.filterLockedBtn.txt.setText(this.hideIncompatibleCards ? '☑ Só Compatíveis' : '☐ Mostrar Bloq.');
            this.filterLockedBtn.bg.setFillStyle(this.hideIncompatibleCards ? 0x065f46 : 0x334155);
            this.currentPage = 0;
            this.refreshCatalogGrid();
        });

        // Contador de Cartas Encontradas
        this.catalogCountLabel = this.add.text(1145, 346, '0 encontradas', {
            fontSize: '12px', color: '#38bdf8', fontStyle: 'bold'
        }).setOrigin(0.5);

        // Paginação
        this.createUIButton(1232, 346, 38, 34, '◀', 0x334155, () => {
            if (this.currentPage > 0) {
                this.currentPage--;
                this.refreshCatalogGrid();
            }
        });
        this.pageLabel = this.add.text(1278, 346, '1/1', {
            fontSize: '12.5px', color: '#ffffff', fontStyle: 'bold'
        }).setOrigin(0.5);
        this.createUIButton(1324, 346, 38, 34, '▶', 0x334155, () => {
            this.currentPage++;
            this.refreshCatalogGrid();
        });

        // Barra de Busca por Escrita + Seletores de Todos os Atributos + Botão Limpar Filtros
        const filterBarHtml = `
            <div id="deckbuilder-filter-bar" style="
                width: 1300px;
                display: flex;
                flex-wrap: nowrap;
                gap: 6px;
                align-items: center;
                background: #0f172a;
                padding: 5px 8px;
                border: 1px solid #334155;
                border-radius: 6px;
                box-sizing: border-box;
                font-family: sans-serif;
            ">
                <input type="text" id="db-search-input" class="phantom-crt-input" placeholder="🔍 Buscar nome ou efeito..."
                    style="width: 198px; height: 28px; padding: 0 8px; font-size: 12px; font-weight: bold; outline: none;" />

                <select id="db-filter-color" class="phantom-crt-select" style="width: 122px; height: 28px; font-size: 11.5px; font-weight: bold; outline: none;">
                    <option value="ALL">🎨 Cor: Todas</option>
                    <option value="WHITE">Branca / Incolor</option>
                    <option value="BLUE">Azul</option>
                    <option value="RED">Vermelha</option>
                    <option value="GREEN">Verde</option>
                    <option value="BLACK">Preta</option>
                    <option value="YELLOW">Amarela</option>
                    <option value="NEUTRAL">Neutra</option>
                </select>

                <select id="db-filter-type" class="phantom-crt-select" style="width: 142px; height: 28px; font-size: 11.5px; font-weight: bold; outline: none;">
                    <option value="ALL">🃏 Tipo/Sub: Todos</option>
                    <optgroup label="Tipos de Carta">
                        <option value="TYPE:SPELL">Magia / Feitiço</option>
                        <option value="TYPE:ITEM">Item (Todos)</option>
                        <option value="TYPE:ACTION">Ação</option>
                        <option value="TYPE:THEURGY">Teurgia</option>
                    </optgroup>
                    <optgroup label="Subtipos de Equipamento">
                        <option value="SUB:ARMA">Arma</option>
                        <option value="SUB:ARMADURA">Armadura</option>
                        <option value="SUB:ESCUDO">Escudo</option>
                        <option value="SUB:JOIA">Joia / Acessório</option>
                        <option value="SUB:FERRAMENTA">Ferramenta</option>
                    </optgroup>
                </select>

                <select id="db-filter-raca" class="phantom-crt-select" style="width: 134px; height: 28px; font-size: 11.5px; font-weight: bold; outline: none;">
                    <option value="ALL">🧬 Raça: Todas</option>
                </select>

                <select id="db-filter-afinidade" class="phantom-crt-select" style="width: 136px; height: 28px; font-size: 11.5px; font-weight: bold; outline: none;">
                    <option value="ALL">🔥 Afinidade: Todas</option>
                </select>

                <select id="db-filter-classe" class="phantom-crt-select" style="width: 134px; height: 28px; font-size: 11.5px; font-weight: bold; outline: none;">
                    <option value="ALL">⚔️ Classe: Todas</option>
                </select>

                <select id="db-filter-ordem" class="phantom-crt-select" style="width: 134px; height: 28px; font-size: 11.5px; font-weight: bold; outline: none;">
                    <option value="ALL">🛡️ Ordem: Todas</option>
                </select>

                <select id="db-filter-empunhadura" class="phantom-crt-select" style="width: 110px; height: 28px; font-size: 11.5px; font-weight: bold; outline: none;">
                    <option value="ALL">✋ Emp.: Todas</option>
                    <option value="1">1 Mão (Emp. 1)</option>
                    <option value="2">2 Mãos (Emp. 2)</option>
                    <option value="0">Sem Emp. (0)</option>
                </select>

                <button type="button" id="db-clear-filters-btn" style="
                    height: 28px; padding: 0 10px; background: #b91c1c; color: #ffffff;
                    border: 1px solid #f87171; border-radius: 4px; font-size: 11.5px;
                    font-weight: bold; cursor: pointer; white-space: nowrap;
                ">✖ Limpar</button>
            </div>
        `;

        this.filterDom = this.add.dom(690, 394).createFromHTML(filterBarHtml);
        this.bindFilterDomEvents();

        this.catalogGridContainer = this.add.container(0, 0);
    }

    bindFilterDomEvents() {
        if (!this.filterDom || !this.filterDom.node) return;
        const root = this.filterDom.node;

        const searchEl = root.querySelector('#db-search-input');
        const colorEl = root.querySelector('#db-filter-color');
        const typeEl = root.querySelector('#db-filter-type');
        const racaEl = root.querySelector('#db-filter-raca');
        const afinidadeEl = root.querySelector('#db-filter-afinidade');
        const classeEl = root.querySelector('#db-filter-classe');
        const ordemEl = root.querySelector('#db-filter-ordem');
        const empEl = root.querySelector('#db-filter-empunhadura');
        const clearBtn = root.querySelector('#db-clear-filters-btn');

        if (searchEl) {
            searchEl.addEventListener('input', (e) => {
                this.filters.search = (e.target.value || '').trim().toLowerCase();
                this.currentPage = 0;
                this.refreshCatalogGrid();
            });
        }

        const bindSelect = (el, filterKey) => {
            if (!el) return;
            el.addEventListener('change', (e) => {
                this.filters[filterKey] = e.target.value;
                this.currentPage = 0;
                this.refreshCatalogGrid();
            });
        };

        bindSelect(colorEl, 'color');
        bindSelect(typeEl, 'typeOrSubtype');
        bindSelect(racaEl, 'raca');
        bindSelect(afinidadeEl, 'afinidade');
        bindSelect(classeEl, 'classe');
        bindSelect(ordemEl, 'ordem');
        bindSelect(empEl, 'empunhadura');

        if (clearBtn) {
            clearBtn.addEventListener('click', () => this.resetAllFilters());
        }
    }

    populateDynamicFilterDropdowns() {
        if (!this.filterDom || !this.filterDom.node) return;
        const root = this.filterDom.node;

        const racasMap = new Map([
            ['HUMANO', 'Humano'],
            ['GOBLIN', 'Goblin'],
            ['CONSTRUCTO', 'Constructo'],
            ['ZUMBI', 'Zumbi'],
            ['BESTA', 'Besta'],
            ['DRAGAO', 'Dragão'],
            ['GUARDIOES DO TRONO', 'Guardião do Trono']
        ]);
        const afinidadesMap = new Map([
            ['FOGO', 'Fogo'],
            ['AGUA', 'Água'],
            ['TERRA', 'Terra'],
            ['AR', 'Ar'],
            ['GELO', 'Gelo'],
            ['RAIO', 'Raio'],
            ['LUZ', 'Luz'],
            ['TREVAS', 'Trevas'],
            ['CRISTAL', 'Cristal']
        ]);
        const classesMap = new Map([
            ['GUERREIRO', 'Guerreiro'],
            ['MAGO', 'Mago'],
            ['CLERIGO', 'Clérigo'],
            ['CACADOR', 'Caçador'],
            ['LADINO', 'Ladino'],
            ['EMISSOR', 'Emissor'],
            ['TRANSFORMADOR', 'Transformador'],
            ['DOMADOR', 'Domador']
        ]);
        const ordensMap = new Map([
            ['CAVALEIRO', 'Cavaleiro'],
            ['GUARDAO DO TRONO', 'Guardião do Trono'],
            ['GRUPO DOS HEROIS', 'Grupo dos Heróis'],
            ['SANTA DAS LAMINAS', 'Santa das Lâminas'],
            ['NECROMANTE', 'Necromante']
        ]);

        [...this.catalogHeroes, ...this.catalogCards].forEach(item => {
            (item.racas || []).forEach(v => {
                const norm = this.normalizeKey(v);
                if (norm && !racasMap.has(norm)) racasMap.set(norm, String(v).replace(/_/g, ' '));
            });
            (item.afinidades || []).forEach(v => {
                const norm = this.normalizeKey(v);
                if (norm && !afinidadesMap.has(norm)) afinidadesMap.set(norm, String(v).replace(/_/g, ' '));
            });
            (item.classe || []).forEach(v => {
                const norm = this.normalizeKey(v);
                if (norm && !classesMap.has(norm)) classesMap.set(norm, String(v).replace(/_/g, ' '));
            });
            (item.ordem || []).forEach(v => {
                const norm = this.normalizeKey(v);
                if (norm && !ordensMap.has(norm)) ordensMap.set(norm, String(v).replace(/_/g, ' '));
            });
        });

        const fillOptions = (selector, defaultLabel, mapEntries) => {
            const selectEl = root.querySelector(selector);
            if (!selectEl) return;
            const currentVal = selectEl.value || 'ALL';
            selectEl.innerHTML = `<option value="ALL">${defaultLabel}</option>`;
            Array.from(mapEntries.entries())
                .sort((a, b) => a[1].localeCompare(b[1]))
                .forEach(([key, label]) => {
                    const opt = document.createElement('option');
                    opt.value = key;
                    opt.textContent = label;
                    selectEl.appendChild(opt);
                });
            selectEl.value = currentVal;
        };

        fillOptions('#db-filter-raca', '🧬 Raça: Todas', racasMap);
        fillOptions('#db-filter-afinidade', '🔥 Afinidade: Todas', afinidadesMap);
        fillOptions('#db-filter-classe', '⚔️ Classe: Todas', classesMap);
        fillOptions('#db-filter-ordem', '🛡️ Ordem: Todas', ordensMap);
    }

    resetAllFilters() {
        this.filters = {
            search: '',
            color: 'ALL',
            typeOrSubtype: 'ALL',
            raca: 'ALL',
            afinidade: 'ALL',
            classe: 'ALL',
            ordem: 'ALL',
            empunhadura: 'ALL'
        };

        if (this.filterDom && this.filterDom.node) {
            const root = this.filterDom.node;
            const searchEl = root.querySelector('#db-search-input');
            if (searchEl) searchEl.value = '';
            [
                '#db-filter-color',
                '#db-filter-type',
                '#db-filter-raca',
                '#db-filter-afinidade',
                '#db-filter-classe',
                '#db-filter-ordem',
                '#db-filter-empunhadura'
            ].forEach(sel => {
                const el = root.querySelector(sel);
                if (el) el.value = 'ALL';
            });
        }

        this.currentPage = 0;
        this.refreshCatalogGrid();
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

        this.ensureDefaultLiberationCardsInCatalog();
        this.populateDynamicFilterDropdowns();
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
            const slotY = 218;
            const hero = heroes[i];

            const slotRect = this.add.rectangle(slotX, slotY, 152, 142, 0x1e293b, 0.8)
                .setStrokeStyle(2, hero ? 0x00ff88 : 0x475569);
            this.selectedHeroesContainer.add(slotRect);

            if (hero) {
                const colorHex = BOARD_LAYOUT.COLORS.CARD_COLORS[hero.color] || 0x475569;
                const bar = this.add.rectangle(slotX, slotY - 54, 144, 24, colorHex);
                const name = this.add.text(slotX, slotY - 54, hero.cardName, {
                    fontSize: '10px', color: '#ffffff', fontStyle: 'bold', align: 'center', wordWrap: { width: 138 }
                }).setOrigin(0.5);

                const stats = this.add.text(slotX, slotY - 10, `ESP:${hero.espirito} ATQ:${hero.ataque}\nDEF:${hero.defesa} HP:${hero.vidaMaxima}`, {
                    fontSize: '10.5px', color: '#ffd700', fontStyle: 'bold', align: 'center'
                }).setOrigin(0.5);

                const removeHint = this.add.text(slotX, slotY + 50, '[REMOVER ✖]', {
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
        const libSlotY = 218;
        const activeLibCard = this.currentDeck.liberationList?.[0] || null;

        const libRect = this.add.rectangle(libSlotX, libSlotY, 175, 142, 0x271a0c, 0.9)
            .setStrokeStyle(2, activeLibCard ? 0xfacc15 : 0xca8a04);
        this.selectedHeroesContainer.add(libRect);

        if (activeLibCard) {
            const bar = this.add.rectangle(libSlotX, libSlotY - 54, 167, 24, 0xca8a04);
            const name = this.add.text(libSlotX, libSlotY - 54, activeLibCard.cardName, {
                fontSize: '9.5px', color: '#000000', fontStyle: 'bold', align: 'center', wordWrap: { width: 160 }
            }).setOrigin(0.5);

            const targetInfo = activeLibCard.evolvesIntoHeroName
                ? `Evolui:\n${activeLibCard.evolvesIntoHeroName}`
                : `Missão: ${activeLibCard.missionType || 'Especial'}`;

            const infoTxt = this.add.text(libSlotX, libSlotY - 8, targetInfo, {
                fontSize: '10px', color: '#fef08a', fontStyle: 'bold', align: 'center', wordWrap: { width: 158 }
            }).setOrigin(0.5);

            const removeHint = this.add.text(libSlotX, libSlotY + 50, '[REMOVER ✖]', {
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
            : (team.baseColor ? `${team.baseColor} e NEUTRAL` : 'Nenhuma (Escolha o 1º Herói)');

        this.permissionsBody.setText(
            `• Cor Permitida: ${colorDesc}\n` +
            `• Afinidades: ${team.affinities.size > 0 ? Array.from(team.affinities).join(', ') : 'Nenhuma'}\n` +
            `• Ordens / Raças: ${team.ordersAndRaces.size > 0 ? Array.from(team.ordersAndRaces).join(', ') : 'Nenhuma'}\n` +
            `• Classes: ${team.classes.size > 0 ? Array.from(team.classes).join(', ') : 'Nenhuma'}` +
            `${team.hasEmissor ? '  ★ [Emissor Ativo]' : ''}` +
            `${team.hasGoblin ? '  ★ [Goblin Ativo]' : ''}`
        );
    }

    /**
     * Verifica se um item (Carta ou Herói) passa por todos os seletores de atributos e busca textual.
     */
    matchesActiveAttributeFilters(item) {
        const f = this.filters;

        // 1. Busca Textual (Nome ou Texto de Descrição do Efeito)
        if (f.search) {
            const nameStr = (item.cardName || '').toLowerCase();
            const descStr = (item.descricao || '').toLowerCase();
            if (!nameStr.includes(f.search) && !descStr.includes(f.search)) {
                return false;
            }
        }

        // 2. Cor / Elemento
        if (f.color !== 'ALL') {
            if ((item.color || 'NEUTRAL').toUpperCase() !== f.color) return false;
        }

        // 3. Tipo de Carta ou Subtipo de Equipamento
        if (f.typeOrSubtype !== 'ALL') {
            if (f.typeOrSubtype.startsWith('TYPE:')) {
                const reqType = f.typeOrSubtype.replace('TYPE:', '');
                if ((item.cardType || '').toUpperCase() !== reqType) return false;
            } else if (f.typeOrSubtype.startsWith('SUB:')) {
                const reqSub = f.typeOrSubtype.replace('SUB:', '');
                if ((item.itemType || 'NENHUM').toUpperCase() !== reqSub) return false;
            }
        }

        // 4. Raça (normalizada contra acentos/underscores)
        if (f.raca !== 'ALL') {
            const racas = (item.racas || []).map(r => this.normalizeKey(r));
            const matchesRaca = racas.some(r => r === f.raca || (f.raca.includes('GUARDI') && r.includes('GUARDI')));
            if (!matchesRaca) return false;
        }

        // 5. Afinidade
        if (f.afinidade !== 'ALL') {
            const afinidades = (item.afinidades || []).map(a => this.normalizeKey(a));
            if (!afinidades.includes(f.afinidade)) return false;
        }

        // 6. Classe
        if (f.classe !== 'ALL') {
            const classes = (item.classe || []).map(c => this.normalizeKey(c));
            if (!classes.includes(f.classe)) return false;
        }

        // 7. Ordem
        if (f.ordem !== 'ALL') {
            const ordens = (item.ordem || []).map(o => this.normalizeKey(o));
            const matchesOrdem = ordens.some(o => o === f.ordem || (f.ordem.includes('GUARDI') && o.includes('GUARDI')));
            if (!matchesOrdem) return false;
        }

        // 8. Empunhadura
        if (f.empunhadura !== 'ALL') {
            const emp = item.empunhadura ?? 0;
            if (Number(emp) !== Number(f.empunhadura)) return false;
        }

        return true;
    }

    /**
     * Ao remover ou trocar um Herói (de 0 a 3 heróis), revalida o baralho e a Carta de Liberação
     */
    pruneInvalidCardsAfterHeroChange() {
        const heroes = this.currentDeck.heroList || [];

        if (this.currentDeck.liberationList?.length > 0) {
            const libCard = this.currentDeck.liberationList[0];
            if (heroes.length === 0) {
                this.currentDeck.liberationList = [];
            } else {
                const checkLib = DeckRestrictionValidator.canAddLiberationCard(libCard, heroes, []);
                if (!checkLib.compatibleWithTeam) {
                    this.currentDeck.liberationList = [];
                }
            }
        }

        if (heroes.length === 0) return;

        const validCards = [];
        this.currentDeck.cardList.forEach(card => {
            const reqCheck = DeckRestrictionValidator.checkCardHeroRequirements(card, heroes);
            if (reqCheck.compatible) {
                validCards.push(card);
            }
        });
        this.currentDeck.cardList = validCards;
    }

    refreshCatalogGrid() {
        this.catalogGridContainer.removeAll(true);

        this.tabButtons.forEach(tb => {
            const isTabActive = tb.key === this.activeTab;
            const defaultColor = tb.key === 'LIBERATION' ? 0x422006 : 0x1e293b;
            tb.bg.setFillStyle(isTabActive ? 0x2563eb : defaultColor);
        });

        let items = [];
        const isHeroTab = this.activeTab === 'HEROES';
        const isLiberationTab = this.activeTab === 'LIBERATION';
        const heroes = this.currentDeck.heroList || [];

        if (isHeroTab) {
            items = this.catalogHeroes.map(h => ({
                item: h,
                isHero: true,
                isLiberation: false,
                validation: DeckRestrictionValidator.canSelectHero(h, heroes, this.player)
            }));
        } else if (isLiberationTab) {
            const liberationCards = this.catalogCards.filter(c => c.liberationCard);
            items = liberationCards.map(c => ({
                item: c,
                isHero: false,
                isLiberation: true,
                validation: DeckRestrictionValidator.canAddLiberationCard(c, heroes, this.currentDeck.liberationList, this.player)
            }));
        } else {
            let filteredCards = this.catalogCards.filter(c => !c.liberationCard);
            if (this.activeTab !== 'ALL_CARDS') {
                filteredCards = filteredCards.filter(c => c.cardType === this.activeTab);
            }
            items = filteredCards.map(c => ({
                item: c,
                isHero: false,
                isLiberation: false,
                validation: DeckRestrictionValidator.canAddCardToDeck(c, heroes, this.currentDeck.cardList, this.player)
            }));
        }

        // 1. Aplica os Filtros Cumulativos de Atributos e Busca por Escrita
        items = items.filter(entry => this.matchesActiveAttributeFilters(entry.item));

        // Filtra apenas cartas/heróis que pertencem à coleção quando "☑ Só Compatíveis" está ativo
        if (this.hideIncompatibleCards) {
            items = items.filter(entry => !entry.validation.notOwned && entry.validation.compatibleWithTeam !== false);
        }

        if (this.catalogCountLabel) {
            this.catalogCountLabel.setText(`${items.length} encontrada(s)`);
        }

        const totalPages = Math.max(1, Math.ceil(items.length / this.itemsPerPage));
        if (this.currentPage >= totalPages) this.currentPage = totalPages - 1;
        this.pageLabel.setText(`${this.currentPage + 1}/${totalPages}`);

        const pageSlice = items.slice(this.currentPage * this.itemsPerPage, (this.currentPage + 1) * this.itemsPerPage);

        if (pageSlice.length === 0) {
            const emptyMsg = this.add.text(690, 680, 'Nenhuma carta encontrada com os filtros atuais.\nClique em "✖ Limpar" para resetar a busca.', {
                fontSize: '16px', color: '#94a3b8', align: 'center', fontStyle: 'bold'
            }).setOrigin(0.5);
            this.catalogGridContainer.add(emptyMsg);
            return;
        }

        pageSlice.forEach((entry, idx) => {
            const col = idx % 5;
            const row = Math.floor(idx / 5);
            const x = 165 + col * 255;
            const y = 555 + row * 270;

            this.createCatalogCardTile(x, y, entry.item, entry.isHero, entry.isLiberation, entry.validation);
        });
    }

    createCatalogCardTile(x, y, data, isHero, isLiberation, validation) {
        const tile = this.add.container(x, y);
        const w = 225;
        const h = 248;

        const accentColor = isLiberation
            ? 0xca8a04
            : (BOARD_LAYOUT.COLORS.CARD_COLORS[data.color] || 0x475569);
        const borderColor = validation.allowed ? (isLiberation ? 0xfacc15 : 0x16a34a) : 0xdc2626;

        // Sombra dura e fundo Papel Creme Claro (#faf8f5) para leitura fácil em preto
        const shadow = this.add.rectangle(5, 6, w, h, 0x090d16, 0.65);
        const bg = this.add.rectangle(0, 0, w, h, isLiberation ? 0xfef9c3 : 0xfaf8f5)
            .setStrokeStyle(3, borderColor)
            .setAlpha(validation.allowed ? 1.0 : 0.55);

        const header = this.add.rectangle(0, -h / 2 + 18, w - 6, 30, accentColor)
            .setStrokeStyle(1.5, 0x090d16);
        const isLightHeader = ['WHITE', 'YELLOW'].includes(data.color) || isLiberation;
        const title = this.add.text(0, -h / 2 + 18, data.cardName, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '12px',
            color: isLightHeader ? '#090d16' : '#ffffff',
            fontStyle: 'bold',
            align: 'center',
            wordWrap: { width: w - 14 }
        }).setOrigin(0.5).setResolution(2);

        const subType = isHero
            ? `HERÓI • ${data.color}${data.afinidades?.length ? ' • ' + data.afinidades.join('/') : ''}`
            : (isLiberation
                ? `★ LIBERAÇÃO (${data.cardType})`
                : `${data.cardType}${data.unica ? ' ★ ÚNICA' : ''}${data.itemType && data.itemType !== 'NENHUM' ? ' • ' + data.itemType : ''}`);

        const subBar = this.add.rectangle(0, -h / 2 + 46, w - 6, 22, 0xe6dec8)
            .setStrokeStyle(1, 0x94a3b8);
        const badge = this.add.text(0, -h / 2 + 46, subType, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '9.5px',
            color: '#4a3520',
            fontStyle: 'bold',
            align: 'center',
            wordWrap: { width: w - 12 }
        }).setOrigin(0.5).setResolution(2);

        // Se for Herói, mostra os 4 status em destaque antes da descrição
        const statsStr = isHero
            ? `ESP:${data.espirito}  ATQ:${data.ataque}  DEF:${data.defesa}  HP:${data.vidaMaxima}\n`
            : '';

        const desc = this.add.text(-w / 2 + 12, -h / 2 + 64, statsStr + (data.descricao || ''), {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '10.5px',
            color: '#090d16',
            fontStyle: 'bold',
            align: 'left',
            lineSpacing: 2,
            wordWrap: { width: w - 24 }
        }).setOrigin(0, 0).setResolution(2);

        const statusFooter = this.add.rectangle(0, h / 2 - 17, w - 6, 28, validation.allowed ? 0x14532d : 0x7f1d1d);
        const statusTxt = this.add.text(0, h / 2 - 17, validation.allowed ? `➕ ${validation.reason}` : `🔒 ${validation.reason}`, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '10px',
            color: validation.allowed ? '#86efac' : '#fecaca',
            fontStyle: 'bold',
            align: 'center',
            wordWrap: { width: w - 10 }
        }).setOrigin(0.5).setResolution(2);

        tile.add([shadow, bg, header, title, subBar, badge, desc, statusFooter, statusTxt]);

        bg.setInteractive({ useHandCursor: validation.allowed })
            .on('pointerdown', () => {
                if (!validation.allowed) {
                    this.showToast(validation.reason);
                    return;
                }
                if (isHero) {
                    this.currentDeck.heroList.push(data);
                    this.pruneInvalidCardsAfterHeroChange();
                    if (this.currentDeck.heroList.length === 3) {
                        this.activeTab = 'ALL_CARDS';
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
            const ownedMax = this.player?.ownedCards?.[card.cardName] ?? (card.unica ? 1 : 3);
            const effectiveMax = Math.min(card.unica ? 1 : 3, ownedMax);
            const txt = this.add.text(16, y, `${count}/${effectiveMax} possuídas  •  ${card.cardName}`, {
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
        const shadow = this.add.rectangle(8, 10, 320, 420, 0x000000, 0.7);
        const bg = this.add.rectangle(0, 0, 320, 420, 0xfaf8f5, 1).setStrokeStyle(4, 0x090d16);
        const topBar = this.add.rectangle(0, -172, 312, 56, 0xe60012);

        this.lpTitle = this.add.text(0, -172, '', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '18px', color: '#ffffff', fontStyle: 'bold', align: 'center', wordWrap: { width: 290 }
        }).setOrigin(0.5).setResolution(2);

        this.lpMeta = this.add.text(0, -124, '', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '12.5px', color: '#4a3520', fontStyle: 'bold', align: 'center', wordWrap: { width: 290 }
        }).setOrigin(0.5).setResolution(2);

        this.lpDesc = this.add.text(-140, -90, '', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '14px', color: '#090d16', fontStyle: 'bold', align: 'left', lineSpacing: 4, wordWrap: { width: 280 }
        }).setOrigin(0, 0).setResolution(2);

        const botBar = this.add.rectangle(0, 175, 312, 46, 0x090d16);
        this.lpStats = this.add.text(0, 175, '', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '13.5px', color: '#ffe600', fontStyle: 'bold', align: 'center'
        }).setOrigin(0.5).setResolution(2);

        this.largePreview.add([shadow, bg, topBar, this.lpTitle, this.lpMeta, this.lpDesc, botBar, this.lpStats]);
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
        const txt = this.add.text(x, y, label, { fontSize: '11.5px', color: '#ffffff', fontStyle: 'bold' }).setOrigin(0.5);
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

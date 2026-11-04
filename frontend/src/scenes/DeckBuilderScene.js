import Phaser from 'phaser';
import { BOARD_LAYOUT, getCardVisualTheme } from '../config/BoardCoordinates.js';
import DeckRestrictionValidator from '../managers/DeckRestrictionValidator.js';
import CRTPostFXPipeline from '../fx/CRTPostFXPipeline.js';
import PhantomUIKit from '../ui/PhantomUIKit.js';
import HeroCardPrefab from '../prefabs/HeroCardPrefab.js';
import PlayableCardPrefab from '../prefabs/PlayableCardPrefab.js';
import { formatCardDescriptionWithKeywords } from '../config/keywordsConfig.js';
import { API_BASE_URL } from '../config/apiConfig.js';

export default class DeckBuilderScene extends Phaser.Scene {
    constructor() {
        super({ key: 'DeckBuilderScene' });
        this.player = null;
        this.catalogHeroes = [];
        this.catalogCards = [];

        // Estado do baralho em edição
        this.currentDeck = null;
        this.activeTab = 'HEROES'; // 'HEROES' | 'ALL_CARDS'
        this.hideIncompatibleCards = true; // Filtra dinamicamente cartas incompatíveis com os heróis selecionados
        this.currentPage = 0;
        this.itemsPerPage = 10;
        this.longHoverTimer = null;

        // Estado de scroll da lista de cartas do baralho
        this.deckListScrollY = 0;
        this.maxDeckListScrollY = 0;
        this.deckListBaseY = 416;
        this.deckListViewportHeight = 550;
        this.isDraggingScrollThumb = false;

        // Compatibilidade com prefabs de partida
        this.selectionManager = {
            selectedOwnHeroId: null,
            selectedEnemyHeroId: null,
            playedCardSlotMap: new Map()
        };

        // Estado dos Filtros Cumulativos (Apenas Nome, Tipo, Classe e Afinidade)
        this.filters = {
            search: '',
            typeOrSubtype: 'ALL',
            classe: 'ALL',
            afinidade: 'ALL'
        };
    }

    init(data) {
        this.player = data?.player || JSON.parse(localStorage.getItem('lendas_player'));
        if (!this.player.decks) this.player.decks = [];
    }

    canDragHero() {
        return false;
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
        if (!this.textures.exists('card_frame_item')) {
            this.load.svg('card_frame_item', 'images/ui/card_frame_item.svg', { width: 280, height: 380 });
        }
        if (!this.textures.exists('ui_slot_frame')) {
            this.load.svg('ui_slot_frame', 'images/ui/ui_slot_frame.svg', { width: 160, height: 210 });
        }
        if (!this.textures.exists('ui_liberation_frame')) {
            this.load.svg('ui_liberation_frame', 'images/ui/ui_liberation_frame.svg', { width: 280, height: 380 });
        }
        if (!this.textures.exists('ui_p5_crt_button')) {
            this.load.svg('ui_p5_crt_button', 'images/ui/p5_crt_button.svg', { width: 520, height: 84 });
        }
        if (!this.textures.exists('ui_card_frame_phantom')) {
            this.load.svg('ui_card_frame_phantom', 'images/ui/card_frame_phantom.svg', { width: 320, height: 440 });
        }
        if (!this.textures.exists('ui_arena_crt_radar')) {
            this.load.svg('ui_arena_crt_radar', 'images/ui/arena_crt_radar.svg', { width: 800, height: 800 });
        }

        const cardKeys = [
            'ataqueMagico', 'atmosferaPesada', 'autoDefesaMagica', 'banir', 'barreiraDeGelo',
            'bolaDeFogo', 'brocheCristal', 'convocacao', 'determinacaoCristalina', 'dragaoCristal',
            'dragast', 'estatica', 'exumacao', 'fragmentoAfiado', 'garraEspectral',
            'golpesPesados', 'grandeMachadoSombrio', 'homunculoCarniceiro', 'laminaDeCristal',
            'maestriaComChamas', 'massacreCristalino', 'mirarNaCabeca', 'paraRaios', 'pontoFinal',
            'quebra', 'quimera', 'quimeraNegra', 'racaoDeEmergencia', 'rapieiraDeCristal',
            'risadaSarcastica', 'ritosFunebres', 'tristeza', 'vendavalArcano'
        ];
        cardKeys.forEach(key => {
            if (!this.textures.exists(key)) {
                this.load.image(key, `images/cartas/${key}.png`);
            }
        });

        const cardArtFiles = {
            'Ataque Mágico': 'ataqueMagico.png',
            'Atmosfera Pesada': 'atmosferaPesada.png',
            'Auto Defesa Magica': 'autoDefesaMagica.png',
            'Banir!': 'banir.png',
            'Barreira de Gelo': 'barreiraDeGelo.png',
            'Bola de fogo': 'bolaDeFogo.png',
            'Bola de Fogo': 'bolaDeFogo.png',
            'Broche de Cristal': 'brocheCristal.png',
            'Convocação': 'convocacao.png',
            'Determinação Cristalina': 'determinacaoCristalina.png',
            'Dragão de Cristal': 'dragaoCristal.png',
            'Dragast': 'dragast.png',
            'Estatica': 'estatica.png',
            'Exumação': 'exumacao.png',
            'Fragmento Afiado': 'fragmentoAfiado.png',
            'Garra espectral': 'garraEspectral.png',
            'Golpes Pesados': 'golpesPesados.png',
            'Grande Machado Sombrio': 'grandeMachadoSombrio.png',
            'Homunculo Carniceiro': 'homunculoCarniceiro.png',
            'Lamina de Cristal': 'laminaDeCristal.png',
            'Maestria com Chamas': 'maestriaComChamas.png',
            'Massacre Cristalino': 'massacreCristalino.png',
            'Mirar na cabeça': 'mirarNaCabeca.png',
            'Para-raios': 'paraRaios.png',
            'Ponto Final': 'pontoFinal.png',
            'Quebra!': 'quebra.png',
            'Quimera': 'quimera.png',
            'Quimera Negra': 'quimeraNegra.png',
            'Ração de emergencia': 'racaoDeEmergencia.png',
            'Rapieira de Cristal': 'rapieiraDeCristal.png',
            'Risada Sarcástica': 'risadaSarcastica.png',
            'Ritos Fúnebres': 'ritosFunebres.png',
            'Tristeza': 'tristeza.png',
            'Vendaval Arcano': 'vendavalArcano.png'
        };

        Object.entries(cardArtFiles).forEach(([cardName, fileName]) => {
            if (!this.textures.exists(cardName)) {
                this.load.image(cardName, `images/cartas/${fileName}`);
            }
        });
    }

    async create() {
        if (this.renderer.pipelines) {
            this.renderer.pipelines.addPostPipeline('CRTPostFX', CRTPostFXPipeline);
            this.cameras.main.setPostPipeline('CRTPostFX');
        }

        this.drawBackgroundAndFrames();
        this.createTopBar();
        this.createCatalogSection();
        this.createRightDeckListSection();
        this.createLargeHoverPreviewPanel();
        this.setupDeckListScrollingEvents();

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

        // Moldura Esquerda: Coleção de Cartas e Barra de Filtros
        this.add.rectangle(690, 570, 1320, 940, 0x111827, 0.9).setStrokeStyle(2, 0x334155);

        // Moldura Direita: Lista do Baralho Atual + Heróis + Aviso de Permissões + Botão Salvar
        this.add.rectangle(1635, 570, 510, 940, 0x0f172a, 0.95).setStrokeStyle(2, BOARD_LAYOUT.COLORS.GOLD_DUST);
    }

    createTopBar() {
        this.add.text(45, 48, '🃏 MONTADOR DE BARALHOS', {
            fontSize: '22px', color: '#ffd700', fontStyle: 'bold'
        }).setOrigin(0, 0.5);

        const inputHtml = `
            <input type="text" id="deck-name-input" class="phantom-crt-input" placeholder="Nome do Baralho..."
                style="width: 280px; height: 36px; font-size: 15px; padding: 0 12px;
                font-weight: bold; outline: none;" />
        `;
        this.deckNameDom = this.add.dom(530, 48).createFromHTML(inputHtml);

        this.deckCycleBtn = this.createUIButton(835, 48, 220, 40, 'Alternar Deck (0)', 0x1e293b, () => {
            if (this.player.decks.length === 0) return;
            const currentIdx = this.player.decks.findIndex(d => d.uuid === this.currentDeck?.uuid);
            const nextIdx = (currentIdx + 1) % this.player.decks.length;
            this.loadDeckIntoEditor(this.player.decks[nextIdx]);
        });

        this.createUIButton(1045, 48, 160, 40, '+ NOVO DECK', 0x2563eb, () => {
            this.createNewBlankDeck();
        });

        this.createUIButton(1245, 48, 195, 40, '★ USAR EM PARTIDA', 0xca8a04, () => {
            this.setAsCurrentPlayableDeck();
        });

        this.createUIButton(1450, 48, 165, 40, '📖 GLOSSÁRIO', 0x0284c7, () => {
            this.openDeckBuilderGlossary();
        });

        this.createUIButton(1755, 48, 220, 40, '⬅ VOLTAR AO MENU', 0x475569, () => {
            this.scene.start('MainMenuScene', { player: this.player });
        });
    }

    openDeckBuilderGlossary() {
        if (this.deckNameDom) this.deckNameDom.setVisible(false);
        if (this.filterDom) this.filterDom.setVisible(false);

        const modal = PhantomUIKit.openGlossaryModal(this);
        modal.on('destroy', () => {
            if (this.deckNameDom) this.deckNameDom.setVisible(true);
            if (this.filterDom) this.filterDom.setVisible(true);
        });
    }

    createCatalogSection() {
        // Campos: Cartas de Heróis, Todas as Cartas e Compatíveis
        const tabs = [
            { key: 'HEROES',    label: '🛡️ CARTAS DE HERÓIS', x: 155, w: 210 },
            { key: 'ALL_CARDS', label: '🃏 TODAS AS CARTAS',  x: 380, w: 210 }
        ];

        this.tabButtons = [];
        tabs.forEach(t => {
            const btn = this.createUIButton(t.x, 128, t.w, 36, t.label, 0x1e293b, () => {
                this.activeTab = t.key;
                this.currentPage = 0;
                this.refreshCatalogGrid();
            });
            this.tabButtons.push({ key: t.key, ...btn });
        });

        // Toggle para Compatíveis
        this.filterLockedBtn = this.createUIButton(605, 128, 205, 36, '☑ Compatíveis', 0x065f46, () => {
            this.hideIncompatibleCards = !this.hideIncompatibleCards;
            this.filterLockedBtn.txt.setText(this.hideIncompatibleCards ? '☑ Compatíveis' : '☐ Compatíveis');
            this.filterLockedBtn.bg.setFillStyle(this.hideIncompatibleCards ? 0x065f46 : 0x334155);
            this.currentPage = 0;
            this.refreshCatalogGrid();
        });

        // Contador de Cartas Encontradas
        this.catalogCountLabel = this.add.text(1235, 128, '0 encontrada(s)', {
            fontSize: '13px', color: '#38bdf8', fontStyle: 'bold'
        }).setOrigin(0.5);

        // Barra de Busca: Apenas Nome, Tipo, Classe e Afinidade
        const filterBarHtml = `
            <div id="deckbuilder-filter-bar" style="
                width: 1290px;
                display: flex;
                flex-wrap: nowrap;
                gap: 10px;
                align-items: center;
                background: #0f172a;
                padding: 6px 12px;
                border: 1px solid #334155;
                border-radius: 6px;
                box-sizing: border-box;
                font-family: sans-serif;
            ">
                <input type="text" id="db-search-input" class="phantom-crt-input" placeholder="🔍 Buscar por nome..."
                    style="flex: 1.4; height: 30px; padding: 0 10px; font-size: 13px; font-weight: bold; outline: none;" />

                <select id="db-filter-type" class="phantom-crt-select" style="flex: 1; height: 30px; font-size: 12.5px; font-weight: bold; outline: none;">
                    <option value="ALL">🃏 Tipo: Todos</option>
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

                <select id="db-filter-classe" class="phantom-crt-select" style="flex: 1; height: 30px; font-size: 12.5px; font-weight: bold; outline: none;">
                    <option value="ALL">⚔️ Classe: Todas</option>
                </select>

                <select id="db-filter-afinidade" class="phantom-crt-select" style="flex: 1; height: 30px; font-size: 12.5px; font-weight: bold; outline: none;">
                    <option value="ALL">🔥 Afinidade: Todas</option>
                </select>

                <button type="button" id="db-clear-filters-btn" style="
                    height: 30px; padding: 0 14px; background: #b91c1c; color: #ffffff;
                    border: 1px solid #f87171; border-radius: 4px; font-size: 12px;
                    font-weight: bold; cursor: pointer; white-space: nowrap;
                ">✖ Limpar</button>
            </div>
        `;

        this.filterDom = this.add.dom(690, 178).createFromHTML(filterBarHtml);
        this.bindFilterDomEvents();

        this.catalogGridContainer = this.add.container(0, 0);

        // Paginação abaixo da coleção de cartas, centralizada na área da coleção (x = 690)
        this.prevPageBtn = this.createUIButton(550, 1005, 110, 36, '◀ ANTERIOR', 0x334155, () => {
            if (this.currentPage > 0) {
                this.currentPage--;
                this.refreshCatalogGrid();
            }
        });
        this.pageLabel = this.add.text(690, 1005, 'PÁGINA 1 / 1', {
            fontSize: '13.5px', color: '#ffffff', fontStyle: 'bold'
        }).setOrigin(0.5);
        this.nextPageBtn = this.createUIButton(830, 1005, 110, 36, 'PRÓXIMA ▶', 0x334155, () => {
            this.currentPage++;
            this.refreshCatalogGrid();
        });
    }

    bindFilterDomEvents() {
        if (!this.filterDom || !this.filterDom.node) return;
        const root = this.filterDom.node;

        const searchEl = root.querySelector('#db-search-input');
        const typeEl = root.querySelector('#db-filter-type');
        const classeEl = root.querySelector('#db-filter-classe');
        const afinidadeEl = root.querySelector('#db-filter-afinidade');
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

        bindSelect(typeEl, 'typeOrSubtype');
        bindSelect(classeEl, 'classe');
        bindSelect(afinidadeEl, 'afinidade');

        if (clearBtn) {
            clearBtn.addEventListener('click', () => this.resetAllFilters());
        }
    }

    populateDynamicFilterDropdowns() {
        if (!this.filterDom || !this.filterDom.node) return;
        const root = this.filterDom.node;

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

        [...this.catalogHeroes, ...this.catalogCards].forEach(item => {
            (item.afinidades || []).forEach(v => {
                const norm = this.normalizeKey(v);
                if (norm && !afinidadesMap.has(norm)) afinidadesMap.set(norm, String(v).replace(/_/g, ' '));
            });
            (item.classe || []).forEach(v => {
                const norm = this.normalizeKey(v);
                if (norm && !classesMap.has(norm)) classesMap.set(norm, String(v).replace(/_/g, ' '));
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

        fillOptions('#db-filter-afinidade', '🔥 Afinidade: Todas', afinidadesMap);
        fillOptions('#db-filter-classe', '⚔️ Classe: Todas', classesMap);
    }

    resetAllFilters() {
        this.filters = {
            search: '',
            typeOrSubtype: 'ALL',
            classe: 'ALL',
            afinidade: 'ALL'
        };

        if (this.filterDom && this.filterDom.node) {
            const root = this.filterDom.node;
            const searchEl = root.querySelector('#db-search-input');
            if (searchEl) searchEl.value = '';

            [
                '#db-filter-type',
                '#db-filter-classe',
                '#db-filter-afinidade'
            ].forEach(sel => {
                const el = root.querySelector(sel);
                if (el) el.value = 'ALL';
            });
        }

        this.currentPage = 0;
        this.refreshCatalogGrid();
    }

    createRightDeckListSection() {
        // 1. Título, Números do Baralho e Status de Validade no topo da coluna direita
        this.add.text(1635, 118, 'LISTA DO BARALHO ATUAL', {
            fontSize: '18px', color: '#ffd700', fontStyle: 'bold'
        }).setOrigin(0.5);

        this.deckStatusBadge = this.add.text(1635, 145, 'Heróis: 0/3  |  Cartas: 0/30', {
            fontSize: '14px', color: '#f87171', fontStyle: 'bold'
        }).setOrigin(0.5);

        this.deckPlayableWarning = this.add.text(1635, 168, '⚠️ BARALHO INVÁLIDO / INCOMPLETO', {
            fontSize: '12px', color: '#fbbf24', fontStyle: 'bold'
        }).setOrigin(0.5);

        // 2. Campo dos Heróis abaixo dos números/validade e acima da lista de cartas do baralho
        this.add.rectangle(1635, 286, 484, 192, 0x111827, 0.95).setStrokeStyle(1.5, 0x1d4ed8);
        this.add.text(1635, 200, 'HERÓIS DO BARALHO (Clique para remover)', {
            fontSize: '11px', color: '#60a5fa', fontStyle: 'bold'
        }).setOrigin(0.5);

        this.selectedHeroesContainer = this.add.container(0, 0);

        // 3. Cabeçalho e Lista de Cartas com Scroll abaixo dos Heróis
        this.add.text(1400, 396, 'CARTAS DO BARALHO', {
            fontSize: '11.5px', color: '#94a3b8', fontStyle: 'bold'
        }).setOrigin(0, 0.5);

        this.deckListBaseY = 428;
        this.deckListContainer = this.add.container(1400, this.deckListBaseY);

        this.deckListMaskGraphics = this.make.graphics({ x: 0, y: 0, add: false });
        this.deckListMask = new Phaser.Display.Masks.GeometryMask(this, this.deckListMaskGraphics);
        this.deckListContainer.setMask(this.deckListMask);

        // Barra de Scroll lateral direita
        this.deckListScrollTrack = this.add.rectangle(1876, 635, 8, 430, 0x1e293b, 0.9)
            .setStrokeStyle(1, 0x334155)
            .setVisible(false);
        this.deckListScrollThumb = this.add.rectangle(1876, 440, 8, 60, 0x60a5fa, 1)
            .setInteractive({ useHandCursor: true, draggable: true })
            .setVisible(false);

        // 4. Aviso de Permissões / Invalidez (só aparece caso o deck esteja inválido, sobre o botão de salvar)
        this.invalidDeckWarningContainer = this.add.container(1635, 912).setDepth(30).setVisible(false);
        const warnBg = this.add.rectangle(0, 0, 480, 118, 0x2a1215, 0.98)
            .setStrokeStyle(2, 0xf87171);
        const warnTitle = this.add.text(-228, -48, '⚠️ BARALHO INVÁLIDO — O QUE ESTÁ ERRADO & PERMISSÕES:', {
            fontSize: '11px', color: '#fbbf24', fontStyle: 'bold'
        }).setOrigin(0, 0.5);
        this.permissionsBody = this.add.text(-228, -32, '', {
            fontSize: '10.5px', color: '#fecaca', lineSpacing: 3, wordWrap: { width: 456 }
        }).setOrigin(0, 0);
        this.invalidDeckWarningContainer.add([warnBg, warnTitle, this.permissionsBody]);

        // 5. Botão de Salvar Baralho na base do painel direito
        this.saveDeckBtn = this.createUIButton(1635, 1004, 480, 44, '💾 SALVAR BARALHO', 0x16a34a, () => {
            this.saveCurrentDeck();
        });
        this.saveDeckBtn.txt.setFontSize('14px');
    }

    setupDeckListScrollingEvents() {
        this.input.on('wheel', (pointer, gameObjects, deltaX, deltaY) => {
            if (pointer.x >= 1380 && pointer.x <= 1890 && pointer.y >= 405 && pointer.y <= 975) {
                if (this.maxDeckListScrollY <= 0) return;
                const step = deltaY > 0 ? 38 : -38;
                this.setDeckListScrollOffset(this.deckListScrollY + step);
            }
        });

        this.deckListScrollThumb.on('drag', (pointer, dragX, dragY) => {
            if (this.maxDeckListScrollY <= 0) return;
            const trackTop = this.deckListBaseY - 16;
            const trackHeight = this.deckListViewportHeight;
            const thumbHeight = this.deckListScrollThumb.height;
            const availableTrack = Math.max(1, trackHeight - thumbHeight);
            const clampedCenterY = Phaser.Math.Clamp(
                dragY,
                trackTop + thumbHeight / 2,
                trackTop + trackHeight - thumbHeight / 2
            );
            const ratio = (clampedCenterY - (trackTop + thumbHeight / 2)) / availableTrack;
            this.setDeckListScrollOffset(ratio * this.maxDeckListScrollY);
        });
    }

    setDeckListScrollOffset(newScrollY) {
        this.deckListScrollY = Phaser.Math.Clamp(newScrollY, 0, this.maxDeckListScrollY);
        this.deckListContainer.y = this.deckListBaseY - this.deckListScrollY;
        this.updateDeckListScrollbarThumb();
    }

    updateDeckListViewportAndMask(isDeckValid, totalRows) {
        const listTop = 410;
        const listBottom = isDeckValid ? 972 : 846;
        this.deckListViewportHeight = listBottom - listTop;

        this.deckListMaskGraphics.clear();
        this.deckListMaskGraphics.fillStyle(0xffffff);
        this.deckListMaskGraphics.fillRect(1390, listTop, 484, this.deckListViewportHeight);

        const contentHeight = totalRows * 38;
        this.maxDeckListScrollY = Math.max(0, contentHeight - (this.deckListViewportHeight - 16));
        this.deckListScrollY = Phaser.Math.Clamp(this.deckListScrollY, 0, this.maxDeckListScrollY);
        this.deckListContainer.y = this.deckListBaseY - this.deckListScrollY;

        if (this.maxDeckListScrollY > 0) {
            this.deckListScrollTrack
                .setPosition(1876, listTop + this.deckListViewportHeight / 2)
                .setSize(8, this.deckListViewportHeight)
                .setVisible(true);
            this.deckListScrollThumb.setVisible(true);
            this.updateDeckListScrollbarThumb();
        } else {
            this.deckListScrollTrack.setVisible(false);
            this.deckListScrollThumb.setVisible(false);
        }
    }

    updateDeckListScrollbarThumb() {
        if (this.maxDeckListScrollY <= 0) return;
        const listTop = 410;
        const trackHeight = this.deckListViewportHeight;
        const contentHeight = this.maxDeckListScrollY + trackHeight;
        const thumbHeight = Math.max(36, Math.min(trackHeight, (trackHeight / contentHeight) * trackHeight));
        const availableTrack = Math.max(1, trackHeight - thumbHeight);
        const ratio = this.deckListScrollY / this.maxDeckListScrollY;
        const thumbCenterY = listTop + thumbHeight / 2 + ratio * availableTrack;

        this.deckListScrollThumb.setSize(8, thumbHeight);
        this.deckListScrollThumb.setPosition(1876, thumbCenterY);
    }

    async loadCatalogFromServer() {
        try {
            const res = await fetch(`${API_BASE_URL}/api/players/catalog`);
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

        this.populateDynamicFilterDropdowns();
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
        this.deckListScrollY = 0;
        this.syncInputDeckName(this.currentDeck.deckName);
        this.refreshAllViews();
    }

    loadDeckIntoEditor(deck) {
        this.currentDeck = JSON.parse(JSON.stringify(deck));
        if (!this.currentDeck.heroList) this.currentDeck.heroList = [];
        if (!this.currentDeck.cardList) this.currentDeck.cardList = [];
        if (!this.currentDeck.liberationList) this.currentDeck.liberationList = [];
        this.deckListScrollY = 0;
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
        this.hideLargeCardPreview();
        this.deckCycleBtn.txt.setText(`Alternar Deck (${this.player.decks.length})`);
        this.renderSelectedHeroesAndPermissions();
        this.refreshCatalogGrid();
        this.renderRightDeckCardList();
    }

    evaluateDeckIssuesAndPermissions() {
        const heroes = this.currentDeck.heroList || [];
        const cards = this.currentDeck.cardList || [];
        const issues = [];

        if (heroes.length < 3) {
            issues.push(`• Faltam ${3 - heroes.length} Herói(s) (${heroes.length}/3 selecionados).`);
        }

        if (cards.length < 30) {
            issues.push(`• Faltam ${30 - cards.length} Carta(s) no baralho (${cards.length}/30).`);
        } else if (cards.length > 30) {
            issues.push(`• Excesso de cartas (${cards.length}/30). Remova ${cards.length - 30}.`);
        }

        // Verifica se alguma carta no baralho viola os requisitos dos heróis atuais
        const invalidCards = [];
        if (heroes.length > 0) {
            cards.forEach(c => {
                const check = DeckRestrictionValidator.checkCardHeroRequirements(c, heroes);
                if (!check.compatible && !invalidCards.some(ic => ic.name === c.cardName)) {
                    invalidCards.push({ name: c.cardName, reason: check.reason });
                }
            });
        } else if (cards.length > 0) {
            issues.push('• Selecione 3 Heróis para validar as permissões das cartas.');
        }

        invalidCards.slice(0, 2).forEach(ic => {
            issues.push(`• ${ic.name}: ${ic.reason}`);
        });

        // Verifica limite de cópias por carta (1 cópia para cartas únicas, 3 cópias para normais)
        const copyCheck = DeckRestrictionValidator.validateDeckCopyLimits(cards);
        copyCheck.violations.slice(0, 2).forEach(v => {
            issues.push(`• ${v.name}: ${v.reason}`);
        });

        const team = DeckRestrictionValidator.analyzeHeroTeam(heroes);
        const colorDesc = team.hasWhiteHero
            ? 'WHITE e NEUTRAL (Coringa Branco)'
            : (team.baseColor ? `${team.baseColor} e NEUTRAL` : 'Nenhuma (Escolha o 1º Herói)');

        const affinitiesStr = team.affinities.size > 0 ? Array.from(team.affinities).join(', ') : 'Nenhuma';
        const classesStr = team.classes.size > 0 ? Array.from(team.classes).join(', ') : 'Nenhuma';
        const ordersStr = team.ordersAndRaces.size > 0 ? Array.from(team.ordersAndRaces).slice(0, 4).join(', ') : 'Nenhuma';

        const permissionsSummary =
            `Permissões da Equipe: Cor [${colorDesc}] | Classes [${classesStr}] | Afinidades [${affinitiesStr}] | Ordens/Raças [${ordersStr}]`;

        const isValid = heroes.length === 3 && cards.length === 30 && invalidCards.length === 0 && copyCheck.valid;

        return {
            isValid,
            text: [...issues, permissionsSummary].join('\n')
        };
    }

    renderSelectedHeroesAndPermissions() {
        this.selectedHeroesContainer.removeAll(true);
        const heroes = this.currentDeck.heroList || [];

        // Renderiza os 3 Slots de Heróis sobre a lista de cartas do baralho usando o modelo HeroCardPrefab
        const heroScale = 0.76;
        const scaledW = BOARD_LAYOUT.MEDIUM_CARD.width * heroScale;
        const scaledH = BOARD_LAYOUT.MEDIUM_CARD.height * heroScale;

        for (let i = 0; i < 3; i++) {
            const slotX = 1480 + i * 155;
            const slotY = 286;
            const hero = heroes[i];

            if (hero) {
                const slotNode = this.add.container(slotX, slotY - 6);
                const heroDto = {
                    ...hero,
                    vidaAtual: hero.vidaMaxima ?? hero.vidaAtual ?? 0,
                    estaVivo: true,
                    estaAtivo: true
                };

                const heroPrefab = new HeroCardPrefab(this, 0, 0, heroDto, false);
                this.input.setDraggable(heroPrefab, false);

                heroPrefab.on('pointerover', () => {
                    this.selectedHeroesContainer.bringToTop(slotNode);
                });
                heroPrefab.on('pointerdown', () => {
                    this.currentDeck.heroList.splice(i, 1);
                    this.pruneInvalidCardsAfterHeroChange();
                    this.refreshAllViews();
                });

                const scaleWrapper = this.add.container(0, 0, [heroPrefab]).setScale(heroScale);

                const removeBadgeBg = this.add.rectangle(0, scaledH / 2 + 12, scaledW, 18, 0x7f1d1d, 0.95)
                    .setStrokeStyle(1, 0xf87171)
                    .setInteractive({ useHandCursor: true })
                    .on('pointerdown', () => {
                        this.currentDeck.heroList.splice(i, 1);
                        this.pruneInvalidCardsAfterHeroChange();
                        this.refreshAllViews();
                    });
                const removeBadgeTxt = this.add.text(0, scaledH / 2 + 12, '✖ REMOVER', {
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '9.5px',
                    color: '#fecaca',
                    fontStyle: 'bold'
                }).setOrigin(0.5).setResolution(2);

                slotNode.add([scaleWrapper, removeBadgeBg, removeBadgeTxt]);
                this.selectedHeroesContainer.add(slotNode);
            } else {
                const slotNode = this.add.container(slotX, slotY);
                let slotFrame;
                if (this.textures.exists('ui_slot_frame')) {
                    slotFrame = this.add.image(0, 0, 'ui_slot_frame')
                        .setDisplaySize(scaledW, scaledH)
                        .setAlpha(0.75);
                } else {
                    slotFrame = this.add.rectangle(0, 0, scaledW, scaledH, 0x1e293b, 0.8)
                        .setStrokeStyle(2, 0x475569);
                }

                const hitArea = this.add.rectangle(0, 0, scaledW, scaledH, 0x000000, 0.01)
                    .setInteractive({ useHandCursor: true })
                    .on('pointerdown', () => {
                        this.activeTab = 'HEROES';
                        this.currentPage = 0;
                        this.refreshCatalogGrid();
                    });

                const emptyTxt = this.add.text(0, 0, `HERÓI ${i + 1}/3\n(Vazio)`, {
                    fontSize: '11.5px', color: '#64748b', align: 'center', fontStyle: 'bold'
                }).setOrigin(0.5);

                slotNode.add([slotFrame, hitArea, emptyTxt]);
                this.selectedHeroesContainer.add(slotNode);
            }
        }

        // Atualiza o aviso de permissões sobre o botão de salvar (visível apenas quando o baralho é inválido)
        const evalResult = this.evaluateDeckIssuesAndPermissions();
        this.invalidDeckWarningContainer.setVisible(!evalResult.isValid);
        if (!evalResult.isValid) {
            this.permissionsBody.setText(evalResult.text);
        }
    }

    /**
     * Verifica se um item (Carta ou Herói) passa pelos filtros ativos (Nome, Tipo, Classe e Afinidade).
     */
    matchesActiveAttributeFilters(item) {
        const f = this.filters;

        // 1. Busca por Nome
        if (f.search) {
            const nameStr = (item.cardName || '').toLowerCase();
            if (!nameStr.includes(f.search)) {
                return false;
            }
        }

        // 2. Tipo de Carta ou Subtipo de Equipamento
        if (f.typeOrSubtype !== 'ALL') {
            if (f.typeOrSubtype.startsWith('TYPE:')) {
                const reqType = f.typeOrSubtype.replace('TYPE:', '');
                if ((item.cardType || '').toUpperCase() !== reqType) return false;
            } else if (f.typeOrSubtype.startsWith('SUB:')) {
                const reqSub = f.typeOrSubtype.replace('SUB:', '');
                if ((item.itemType || 'NENHUM').toUpperCase() !== reqSub) return false;
            }
        }

        // 3. Classe
        if (f.classe !== 'ALL') {
            const classes = (item.classe || []).map(c => this.normalizeKey(c));
            if (!classes.includes(f.classe)) return false;
        }

        // 4. Afinidade
        if (f.afinidade !== 'ALL') {
            const afinidades = (item.afinidades || []).map(a => this.normalizeKey(a));
            if (!afinidades.includes(f.afinidade)) return false;
        }

        return true;
    }

    /**
     * Ao remover ou trocar um Herói (de 0 a 3 heróis), revalida o baralho
     */
    pruneInvalidCardsAfterHeroChange() {
        const heroes = this.currentDeck.heroList || [];
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
        this.hideLargeCardPreview();
        this.catalogGridContainer.removeAll(true);

        this.tabButtons.forEach(tb => {
            const isTabActive = tb.key === this.activeTab;
            tb.bg.setFillStyle(isTabActive ? 0x2563eb : 0x1e293b);
        });

        let items = [];
        const isHeroTab = this.activeTab === 'HEROES';
        const heroes = this.currentDeck.heroList || [];

        if (isHeroTab) {
            items = this.catalogHeroes.map(h => ({
                item: h,
                isHero: true,
                validation: DeckRestrictionValidator.canSelectHero(h, heroes, this.player)
            }));
        } else {
            const filteredCards = this.catalogCards.filter(c => !c.liberationCard);
            items = filteredCards.map(c => ({
                item: c,
                isHero: false,
                validation: DeckRestrictionValidator.canAddCardToDeck(c, heroes, this.currentDeck.cardList, this.player)
            }));
        }

        // 1. Aplica os Filtros de Nome, Tipo, Classe e Afinidade
        items = items.filter(entry => this.matchesActiveAttributeFilters(entry.item));

        // 2. Filtra apenas cartas/heróis compatíveis e possuídos quando "☑ Compatíveis" está ativo
        if (this.hideIncompatibleCards) {
            items = items.filter(entry => !entry.validation.notOwned && entry.validation.compatibleWithTeam !== false);
        }

        if (this.catalogCountLabel) {
            this.catalogCountLabel.setText(`${items.length} encontrada(s)`);
        }

        const totalPages = Math.max(1, Math.ceil(items.length / this.itemsPerPage));
        if (this.currentPage >= totalPages) this.currentPage = totalPages - 1;
        this.pageLabel.setText(`PÁGINA ${this.currentPage + 1} / ${totalPages}`);

        const pageSlice = items.slice(this.currentPage * this.itemsPerPage, (this.currentPage + 1) * this.itemsPerPage);

        if (pageSlice.length === 0) {
            const emptyMsg = this.add.text(690, 580, 'Nenhuma carta encontrada com os filtros atuais.\nClique em "✖ Limpar" para resetar a busca.', {
                fontSize: '16px', color: '#94a3b8', align: 'center', fontStyle: 'bold'
            }).setOrigin(0.5);
            this.catalogGridContainer.add(emptyMsg);
            return;
        }

        pageSlice.forEach((entry, idx) => {
            const col = idx % 5;
            const row = Math.floor(idx / 5);
            const x = 180 + col * 255;
            const y = 370 + row * 360;

            this.createCatalogCardTile(x, y, entry.item, entry.isHero, entry.validation);
        });
    }

    createCatalogCardTile(x, y, data, isHero, validation) {
        const tile = this.add.container(x, y);
        const cardScale = 1.24;
        const scaledW = BOARD_LAYOUT.MEDIUM_CARD.width * cardScale;
        const scaledH = BOARD_LAYOUT.MEDIUM_CARD.height * cardScale;

        const scaleWrapper = this.add.container(0, 0).setScale(cardScale);
        let cardModel;

        if (isHero) {
            const heroDto = {
                ...data,
                vidaAtual: data.vidaMaxima ?? data.vidaAtual ?? 0,
                estaVivo: true,
                estaAtivo: true
            };
            cardModel = new HeroCardPrefab(this, 0, 0, heroDto, false);
            this.input.setDraggable(cardModel, false);
        } else {
            cardModel = new PlayableCardPrefab(
                this,
                0,
                0,
                { ...data, faceUp: true },
                false,
                'MEDIUM',
                0
            );
            cardModel.setShowFullText(true);
        }

        scaleWrapper.add(cardModel);

        const statusFooterY = scaledH / 2 + 18;
        const statusFooter = this.add.rectangle(
            0,
            statusFooterY,
            scaledW + 16,
            26,
            validation.allowed ? 0x14532d : 0x7f1d1d
        ).setStrokeStyle(1.5, validation.allowed ? 0x22c55e : 0xef4444);

        const statusTxt = this.add.text(
            0,
            statusFooterY,
            validation.allowed ? `➕ ${validation.reason}` : `🔒 ${validation.reason}`,
            {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '9.5px',
                color: validation.allowed ? '#86efac' : '#fecaca',
                fontStyle: 'bold',
                align: 'center',
                wordWrap: { width: scaledW + 10 }
            }
        ).setOrigin(0.5).setResolution(2);

        const onSelectCard = () => {
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
            } else {
                this.currentDeck.cardList.push(data);
            }
            this.refreshAllViews();
        };

        cardModel.on('pointerover', () => {
            this.catalogGridContainer.bringToTop(tile);
        });
        cardModel.on('pointerdown', onSelectCard);

        statusFooter.setInteractive({ useHandCursor: validation.allowed })
            .on('pointerdown', onSelectCard);

        tile.add([scaleWrapper, statusFooter, statusTxt]);
        this.catalogGridContainer.add(tile);
    }

    renderRightDeckCardList() {
        this.deckListContainer.removeAll(true);

        const hCount = this.currentDeck.heroList.length;
        const cCount = this.currentDeck.cardList.length;
        const evalResult = this.evaluateDeckIssuesAndPermissions();
        const isPlayable = evalResult.isValid;

        this.deckStatusBadge.setText(`Heróis: ${hCount}/3  |  Cartas: ${cCount}/30`);
        this.deckStatusBadge.setColor(isPlayable ? '#4ade80' : '#f87171');

        this.deckPlayableWarning.setText(
            isPlayable
                ? '✅ BARALHO VÁLIDO E DISPONÍVEL PARA JOGAR!'
                : '⚠️ BARALHO INVÁLIDO / INCOMPLETO'
        );
        this.deckPlayableWarning.setColor(isPlayable ? '#4ade80' : '#fbbf24');

        const grouped = new Map();
        this.currentDeck.cardList.forEach(card => {
            if (!grouped.has(card.cardName)) {
                grouped.set(card.cardName, { card, count: 0 });
            }
            grouped.get(card.cardName).count++;
        });

        let rowIdx = 0;
        grouped.forEach(({ card, count }) => {
            const y = rowIdx * 38;
            const colorHex = BOARD_LAYOUT.COLORS.CARD_COLORS[card.color] || 0x3b82f6;
            const rowBg = this.add.rectangle(232, y, 464, 34, 0x1e293b)
                .setStrokeStyle(1, colorHex)
                .setInteractive({ useHandCursor: true })
                .on('pointerover', () => this.startLongHover(card, false))
                .on('pointerout', () => this.cancelLongHover());

            const colorStripe = this.add.rectangle(4, y, 6, 32, colorHex);
            const ownedMax = this.player?.ownedCards?.[card.cardName] ?? (card.unica ? 1 : 3);
            const effectiveMax = Math.min(card.unica ? 1 : 3, ownedMax);
            const uniqueSuffix = card.unica ? ' [ÚNICA]' : '';
            const txt = this.add.text(16, y, `${count}/${effectiveMax}x  •  ${card.cardName}${uniqueSuffix}`, {
                fontSize: '12.5px', color: card.unica ? '#fde047' : '#ffffff', fontStyle: 'bold'
            }).setOrigin(0, 0.5);

            const removeBtn = this.add.text(452, y, '[ - ]', {
                fontSize: '13px', color: '#f87171', fontStyle: 'bold'
            }).setOrigin(1, 0.5).setInteractive({ useHandCursor: true });

            removeBtn.on('pointerdown', () => {
                const idxToRemove = this.currentDeck.cardList.findIndex(c => c.cardName === card.cardName);
                if (idxToRemove > -1) {
                    this.currentDeck.cardList.splice(idxToRemove, 1);
                    this.refreshAllViews();
                }
            });

            this.deckListContainer.add([rowBg, colorStripe, txt, removeBtn]);
            rowIdx++;
        });

        this.updateDeckListViewportAndMask(isPlayable, rowIdx);
    }

    async saveCurrentDeck() {
        this.currentDeck.deckName = this.getInputDeckName();
        const isPlayable = DeckRestrictionValidator.isDeckReadyForMatch(this.currentDeck);

        try {
            const res = await fetch(`${API_BASE_URL}/api/players/${this.player.uuid}/decks`, {
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
            await fetch(`${API_BASE_URL}/api/players/${this.player.uuid}/active-deck/${this.currentDeck.uuid}`, {
                method: 'PUT'
            });
        } catch (e) {
            // Silencioso se offline
        }

        this.showToast(`"${this.currentDeck.deckName}" definido como seu baralho ativo de batalha!`, '#16a34a');
    }

    // =========================================================================
    // HOVER PREVIEW DAS PARTIDAS (Idêntico ao MatchScene)
    // =========================================================================
    createLargeHoverPreviewPanel() {
        const rawPos = BOARD_LAYOUT.PILES.LARGE_PREVIEW;
        const x = Math.max(205, rawPos.x);
        const y = rawPos.y;
        const { width, height } = BOARD_LAYOUT.LARGE_CARD; // 280 x 380

        this.INSPECT_PREVIEW_SCALE = 1.22;
        this.hoveredPreviewState = null;

        const pos = (svgX, svgY) => ({
            x: ((svgX / 280) - 0.5) * width,
            y: ((svgY / 380) - 0.5) * height
        });

        this.largePreviewContainer = this.add.container(x, y).setDepth(500).setVisible(false);

        this.lpShadow = this.add.rectangle(8, 10, width, height, 0x000000, 0.65);

        const artCenter = pos(137, 117);
        const artW = width * (248 / 280);
        const artH = height * (150 / 380);

        this.lpArtBox = this.add.rectangle(artCenter.x, artCenter.y, artW, artH, 0xd6cbb5, 1);
        this.lpArtLabel = this.add.text(artCenter.x, artCenter.y, 'ILUSTRAÇÃO', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '14px',
            color: '#475569',
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        this.lpFrame = this.add.image(0, 0, 'card_frame_hero').setDisplaySize(width, height);

        const headerBarPt = pos(136, 21);
        this.lpHeaderColorBar = this.add.rectangle(
            headerBarPt.x,
            headerBarPt.y,
            width * (256 / 280),
            height * (28 / 380),
            0xffffff
        );

        const titlePt = pos(132, 21);
        this.lpTitle = this.add.text(titlePt.x, titlePt.y, '', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '16px',
            color: '#090d16',
            fontStyle: 'bold',
            align: 'center',
            wordWrap: { width: width * 0.82 }
        }).setOrigin(0.5).setResolution(2);

        const mod1 = pos(186, 174);
        const mod2 = pos(220, 174);
        const mod3 = pos(254, 174);

        this.lpModBonus = this.add.text(mod1.x, mod1.y, '0', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '18px', color: '#15803d', fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        this.lpModReducao = this.add.text(mod2.x, mod2.y, '0', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '18px', color: '#dc2626', fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        this.lpModVulnerabilidade = this.add.text(mod3.x, mod3.y, '0', {
            fontFamily: 'IoskeleyMono, monospace', fontSize: '18px', color: '#7e22ce', fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        const espPt = pos(264, 224);
        const atqPt = pos(264, 267);
        const defPt = pos(264, 310);
        const hpPt  = pos(264, 353);

        const statStyle = {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '19px',
            color: '#090d16',
            fontStyle: 'bold',
            align: 'right'
        };

        this.lpStatEsp = this.add.text(espPt.x, espPt.y, '0', statStyle).setOrigin(1, 0.5).setResolution(2);
        this.lpStatAtq = this.add.text(atqPt.x, atqPt.y, '0', statStyle).setOrigin(1, 0.5).setResolution(2);
        this.lpStatDef = this.add.text(defPt.x, defPt.y, '0', statStyle).setOrigin(1, 0.5).setResolution(2);
        this.lpStatHp  = this.add.text(hpPt.x,  hpPt.y,  '0', {
            ...statStyle,
            fontSize: '20px',
            color: '#b91c1c'
        }).setOrigin(1, 0.5).setResolution(2);

        this.lpTypeLine = this.add.text(0, 0, '', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '12px',
            color: '#4a3520',
            fontStyle: 'bold',
            align: 'left',
            wordWrap: { width: width * (194 / 280) }
        }).setOrigin(0, 0).setResolution(2);

        this.lpDesc = this.add.text(0, 0, '', {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '13px',
            color: '#090d16',
            fontStyle: 'bold',
            align: 'left',
            lineSpacing: 3,
            wordWrap: { width: width * (194 / 280) }
        }).setOrigin(0, 0).setResolution(2);

        this.lpPhaseNodes = [0, 1, 2, 3].map(() => {
            const bg = this.add.rectangle(0, 0, width * (22 / 280), height * (18 / 380), 0x111827, 1)
                .setStrokeStyle(1.5, 0x475569)
                .setVisible(false);
            const txt = this.add.text(0, 0, '', {
                fontFamily: 'IoskeleyMono, monospace',
                fontSize: '13px',
                color: '#475569',
                fontStyle: 'bold'
            }).setOrigin(0.5).setResolution(2).setVisible(false);
            return { bg, txt };
        });

        const lpPhaseElements = [];
        this.lpPhaseNodes.forEach(n => lpPhaseElements.push(n.bg, n.txt));

        this.largePreviewContainer.add([
            this.lpShadow,
            this.lpArtBox,
            this.lpArtLabel,
            this.lpFrame,
            this.lpHeaderColorBar,
            this.lpTitle,
            this.lpModBonus,
            this.lpModReducao,
            this.lpModVulnerabilidade,
            this.lpStatEsp,
            this.lpStatAtq,
            this.lpStatDef,
            this.lpStatHp,
            this.lpTypeLine,
            this.lpDesc,
            ...lpPhaseElements
        ]);
    }

    formatHeroTraitsLine(heroDto) {
        const capitalize = (str) => {
            if (!str) return '';
            const clean = String(str).replace(/_/g, ' ').trim();
            return clean.charAt(0).toUpperCase() + clean.slice(1).toLowerCase();
        };

        const formatList = (val, prefix = '') => {
            if (!val) return [];
            const arr = Array.isArray(val) ? val : [val];
            return arr.filter(Boolean).map(v => `${prefix}${capitalize(v)}`);
        };

        const traits = [
            ...formatList(heroDto.classe),
            ...formatList(heroDto.ordem),
            ...formatList(heroDto.racas || heroDto.raca),
            ...formatList(heroDto.afinidades || heroDto.afinidade, 'Afinidade ')
        ];

        return traits.join(' - ');
    }

    showLargeCardPreview(cardOrHeroDto, isHero = false, isRefresh = false) {
        if (!cardOrHeroDto) return;
        const { width, height } = BOARD_LAYOUT.LARGE_CARD;
        const targetScale = this.INSPECT_PREVIEW_SCALE || 1.22;

        // Posiciona o painel de hover no lado oposto ao cursor para nunca cobrir a carta sob o mouse
        const pointerX = this.input?.activePointer?.x ?? 960;
        const previewX = pointerX < 750 ? 1140 : 230;
        const previewY = 580;
        this.largePreviewContainer.setPosition(previewX, previewY);

        this.hoveredPreviewState = {
            uuid: cardOrHeroDto.uuid || null,
            cardName: cardOrHeroDto.cardName || cardOrHeroDto.nome || '',
            isHero: Boolean(isHero),
            lastDto: cardOrHeroDto
        };

        const pos = (svgX, svgY) => ({
            x: ((svgX / 280) - 0.5) * width,
            y: ((svgY / 380) - 0.5) * height
        });

        const isItem = !isHero && (
            cardOrHeroDto.cardType === 'ITEM' ||
            (cardOrHeroDto.itemType && cardOrHeroDto.itemType !== 'NENHUM') ||
            cardOrHeroDto.cardName === 'Alma em Chamas'
        );

        const hasPhaseTrack = isHero || isItem;
        const artCenter = pos(hasPhaseTrack ? 123 : 137, 117);
        const artW = width * ((hasPhaseTrack ? 218 : 248) / 280);
        const artH = height * (150 / 380);
        this.lpArtBox.setPosition(artCenter.x, artCenter.y).setSize(artW, artH);
        this.lpArtLabel.setPosition(artCenter.x, artCenter.y);

        if (hasPhaseTrack && this.lpPhaseNodes) {
            const phaseStates = PlayableCardPrefab.resolveActivePhaseNodes(cardOrHeroDto);
            this.lpPhaseNodes.forEach((node, idx) => {
                const st = phaseStates[idx];
                const pt = pos(st.svgX, st.svgY);
                node.bg
                    .setPosition(pt.x, pt.y)
                    .setFillStyle(st.active ? st.color : 0x111827, 1)
                    .setStrokeStyle(st.active ? 2 : 1.5, st.active ? 0xffffff : 0x475569)
                    .setVisible(true);
                node.txt
                    .setPosition(pt.x, pt.y)
                    .setText(st.icon)
                    .setColor(st.active ? st.textColor : '#475569')
                    .setVisible(true);
            });
        } else if (this.lpPhaseNodes) {
            this.lpPhaseNodes.forEach(node => {
                node.bg.setVisible(false);
                node.txt.setVisible(false);
            });
        }

        const frameKey = isHero
            ? 'card_frame_hero'
            : ((isItem && this.textures.exists('card_frame_item')) ? 'card_frame_item' : 'card_frame_playable');
        if (this.textures.exists(frameKey)) {
            this.lpFrame.setTexture(frameKey).setDisplaySize(width, height);
        }

        this.lpTitle.setText(cardOrHeroDto.cardName || cardOrHeroDto.nome || '');

        const theme = getCardVisualTheme(cardOrHeroDto.color, cardOrHeroDto.rarity);
        this.lpHeaderColorBar.setFillStyle(theme.headerBg, 1);
        this.lpTitle.setColor(theme.textColor);

        if (isHero) {
            this.lpArtLabel.setText('⚔ ARTE DO HERÓI');

            this.lpModBonus.setVisible(false);
            this.lpModReducao.setVisible(false);
            this.lpModVulnerabilidade.setVisible(false);

            this.lpStatEsp.setText(String(cardOrHeroDto.espirito ?? 0)).setStyle({ color: '#090d16' }).setVisible(true);
            this.lpStatAtq.setText(String(cardOrHeroDto.ataque ?? 0)).setStyle({ color: '#090d16' }).setVisible(true);
            this.lpStatDef.setText(String(cardOrHeroDto.defesa ?? 0)).setStyle({ color: '#090d16' }).setVisible(true);
            this.lpStatHp.setText(String(cardOrHeroDto.vidaAtual ?? cardOrHeroDto.vidaMaxima ?? 0)).setStyle({ color: '#b91c1c' }).setVisible(true);

            const traitsPt = pos(14, 201);
            const linhaAtributos = this.formatHeroTraitsLine(cardOrHeroDto);
            this.lpTypeLine
                .setOrigin(0, 0)
                .setPosition(traitsPt.x, traitsPt.y)
                .setStyle({
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '11.5px',
                    color: '#4a3520',
                    fontStyle: 'bold',
                    align: 'left',
                    wordWrap: { width: width * (194 / 280) }
                })
                .setText(linhaAtributos);

            const descPt = pos(14, 238);
            const fullDesc = formatCardDescriptionWithKeywords(cardOrHeroDto.descricao || '');

            this.lpDesc
                .setOrigin(0, 0)
                .setPosition(descPt.x, descPt.y)
                .setStyle({
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: fullDesc.length > 130 ? '11.5px' : '12.5px',
                    color: '#090d16',
                    fontStyle: 'bold',
                    align: 'left',
                    lineSpacing: 3,
                    wordWrap: { width: width * (194 / 280) }
                })
                .setText(fullDesc);

        } else if (isItem) {
            this.lpModBonus.setVisible(false);
            this.lpModReducao.setVisible(false);
            this.lpModVulnerabilidade.setVisible(false);

            const stats = PlayableCardPrefab.extractItemStatBonuses(cardOrHeroDto);
            this.lpStatEsp.setText(stats.esp > 0 ? ('+' + stats.esp) : '-').setStyle({ color: '#6b21a8' }).setVisible(true);
            this.lpStatAtq.setText(stats.atq > 0 ? ('+' + stats.atq) : '-').setStyle({ color: '#991b1b' }).setVisible(true);
            this.lpStatDef.setText(stats.def > 0 ? ('+' + stats.def) : '-').setStyle({ color: '#0369a1' }).setVisible(true);
            this.lpStatHp.setText(String(stats.emp || 0)).setStyle({ color: '#fde047' }).setVisible(true);

            const subType = (cardOrHeroDto.itemType && cardOrHeroDto.itemType !== 'NENHUM')
                ? (' • ' + cardOrHeroDto.itemType)
                : '';
            this.lpArtLabel.setText('✦ ARTE (' + (cardOrHeroDto.cardType || 'ITEM') + ')');

            const typePt = pos(110, 214);
            this.lpTypeLine
                .setOrigin(0.5, 0.5)
                .setPosition(typePt.x, typePt.y)
                .setStyle({
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '12px',
                    color: '#4a3520',
                    fontStyle: 'bold',
                    align: 'center',
                    wordWrap: { width: width * (192 / 280) }
                })
                .setText((cardOrHeroDto.cardType || 'ITEM') + subType);

            const descPt = pos(14, 236);
            this.lpDesc
                .setOrigin(0, 0)
                .setPosition(descPt.x, descPt.y)
                .setStyle({
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '13px',
                    color: '#090d16',
                    fontStyle: 'bold',
                    align: 'left',
                    lineSpacing: 3,
                    wordWrap: { width: width * (192 / 280) }
                })
                .setText(stats.cleanDesc || formatCardDescriptionWithKeywords(cardOrHeroDto.descricao || ''));
        } else {
            this.lpModBonus.setVisible(false);
            this.lpModReducao.setVisible(false);
            this.lpModVulnerabilidade.setVisible(false);
            this.lpStatEsp.setVisible(false);
            this.lpStatAtq.setVisible(false);
            this.lpStatDef.setVisible(false);
            this.lpStatHp.setVisible(false);

            const empTxt = (cardOrHeroDto.itemType === 'ARMA' && cardOrHeroDto.empunhadura > 0)
                ? ` • EMP: ${cardOrHeroDto.empunhadura}`
                : '';
            const subType = (cardOrHeroDto.itemType && cardOrHeroDto.itemType !== 'NENHUM')
                ? ` • ${cardOrHeroDto.itemType}`
                : '';

            this.lpArtLabel.setText(`✦ ARTE (${cardOrHeroDto.cardType})`);

            const typePt = pos(138, 208);
            this.lpTypeLine
                .setOrigin(0.5, 0.5)
                .setPosition(typePt.x, typePt.y)
                .setStyle({
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '13px',
                    color: '#090d16',
                    fontStyle: 'bold',
                    align: 'center',
                    wordWrap: { width: width * 0.88 }
                })
                .setText(`${cardOrHeroDto.cardType}${subType}${empTxt}`);

            const descPt = pos(16, 230);
            this.lpDesc
                .setOrigin(0, 0)
                .setPosition(descPt.x, descPt.y)
                .setStyle({
                    fontFamily: 'IoskeleyMono, monospace',
                    fontSize: '14px',
                    color: '#090d16',
                    fontStyle: 'bold',
                    align: 'left',
                    lineSpacing: 4,
                    wordWrap: { width: width * (244 / 280) }
                })
                .setText(formatCardDescriptionWithKeywords(cardOrHeroDto.descricao || ''));
        }

        const camelKey = PlayableCardPrefab.prototype.toCamelCaseKey(cardOrHeroDto.cardName);
        const artKey = (cardOrHeroDto.imagem && this.textures.exists(cardOrHeroDto.imagem))
            ? cardOrHeroDto.imagem
            : (this.textures.exists(cardOrHeroDto.cardName)
                ? cardOrHeroDto.cardName
                : (camelKey && this.textures.exists(camelKey) ? camelKey : null));

        if (artKey) {
            if (!this.lpArtSprite) {
                this.lpArtSprite = this.add.image(this.lpArtBox.x, this.lpArtBox.y, artKey);
                this.lpArtSprite.texture.setFilter(Phaser.Textures.FilterMode.NEAREST);
                const frameIdx = this.largePreviewContainer.getIndex(this.lpFrame);
                this.largePreviewContainer.addAt(this.lpArtSprite, frameIdx);
            } else {
                this.lpArtSprite.setTexture(artKey).setVisible(true);
            }
            this.lpArtSprite.setPosition(this.lpArtBox.x, this.lpArtBox.y).setDisplaySize(this.lpArtBox.width, this.lpArtBox.height);
            this.lpArtLabel.setVisible(false);
        } else {
            if (this.lpArtSprite) this.lpArtSprite.setVisible(false);
            this.lpArtLabel.setVisible(true);
        }

        this.largePreviewContainer.setVisible(true);
        this.tweens.killTweensOf(this.largePreviewContainer);

        if (isRefresh) {
            this.largePreviewContainer.setScale(targetScale);
        } else {
            this.largePreviewContainer.setScale(targetScale * 0.92);
            this.tweens.add({
                targets: this.largePreviewContainer,
                scaleX: targetScale,
                scaleY: targetScale,
                duration: 180,
                ease: 'Cubic.easeOut'
            });
        }

        if (this.keywordTooltipsContainer) {
            this.keywordTooltipsContainer.destroy(true);
        }

        const scaledHalfW = (width * targetScale) / 2;
        const tooltipCenterX = previewX < 700
            ? (previewX + scaledHalfW + 145)
            : (previewX - scaledHalfW - 145);

        this.keywordTooltipsContainer = PhantomUIKit.createKeywordTooltipsContainer(
            this,
            cardOrHeroDto,
            tooltipCenterX,
            previewY - ((height * targetScale) / 2)
        );
        if (this.keywordTooltipsContainer) {
            this.keywordTooltipsContainer.setDepth(505);
        }
    }

    hideLargeCardPreview() {
        this.hoveredPreviewState = null;
        if (this.largePreviewContainer) {
            this.largePreviewContainer.setVisible(false);
        }
        if (this.keywordTooltipsContainer) {
            this.keywordTooltipsContainer.destroy(true);
            this.keywordTooltipsContainer = null;
        }
    }

    startLongHover(data, isHero) {
        this.cancelLongHover();
        this.longHoverTimer = this.time.delayedCall(180, () => {
            this.showLargeCardPreview(data, isHero);
        });
    }

    cancelLongHover() {
        if (this.longHoverTimer) {
            this.longHoverTimer.remove(false);
            this.longHoverTimer = null;
        }
        this.hideLargeCardPreview();
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
        }).setOrigin(0.5).setDepth(600);

        this.tweens.add({
            targets: toast,
            alpha: 0,
            delay: 2000,
            duration: 400,
            onComplete: () => toast.destroy()
        });
    }
}

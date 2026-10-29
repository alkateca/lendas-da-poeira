export default class MatchRecapSidebar {
    constructor(scene) {
        this.scene = scene;
        this.lastPhase = null;
        this.turnCounter = 0;
        this.lastBattleHeroesKey = '';
        this.lastPrepCountKey = '';
        this.previousHeroHp = new Map();
        this.entryIndex = 0;

        this.mountSidebarDOM();
    }

    mountSidebarDOM() {
        const existing = document.getElementById('ldp-recap-wrapper');
        if (existing) existing.remove();

        this.isOpen = false;

        this.wrapperEl = document.createElement('div');
        this.wrapperEl.id = 'ldp-recap-wrapper';
        this.wrapperEl.style.cssText = `
            position: fixed;
            left: 0;
            top: 0;
            height: 100vh;
            z-index: 1200;
            pointer-events: none;
            font-family: 'IoskeleyMono', monospace;
            user-select: none;
        `;

        this.wrapperEl.innerHTML = `
            <!-- Gaveta Retrátil (Oculta por padrão em translateX(-100%)) -->
            <div id="ldp-recap-drawer" style="
                position: absolute;
                left: 0;
                top: 0;
                width: 330px;
                height: 100vh;
                background: rgba(9, 13, 22, 0.96);
                border-right: 4px solid #e60012;
                box-shadow: inset -3px 0 0 #ffe600, 10px 0 28px rgba(0,0,0,0.85);
                display: flex;
                flex-direction: column;
                transform: translateX(-100%);
                transition: transform 0.22s cubic-bezier(0.22, 1, 0.36, 1);
                pointer-events: auto;
            ">
                <!-- Botão Aba Lateral acoplado na borda direita da gaveta -->
                <button id="ldp-recap-toggle-btn" type="button" style="
                    position: absolute;
                    right: -46px;
                    top: 50%;
                    transform: translateY(-50%);
                    width: 46px;
                    height: 136px;
                    background: #090d16;
                    color: #ffe600;
                    border: 3px solid #ffe600;
                    border-left: none;
                    box-shadow: 4px 4px 0px #e60012;
                    cursor: pointer;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    justify-content: center;
                    gap: 6px;
                    padding: 6px 2px;
                    font-family: 'IoskeleyMono', monospace;
                    font-weight: bold;
                    font-size: 12px;
                    pointer-events: auto;
                ">
                    <span id="ldp-recap-arrow" style="font-size: 16px; color: #00f0ff;">▶</span>
                    <span style="writing-mode: vertical-rl; text-orientation: mixed; letter-spacing: 2px;">LOG PARTIDA</span>
                    <span id="ldp-recap-unread-badge" style="
                        background: #e60012;
                        color: #ffffff;
                        font-size: 10px;
                        padding: 2px 5px;
                        border-radius: 3px;
                        display: none;
                    ">0</span>
                </button>

                <!-- Cabeçalho SVG da Gaveta -->
                <svg viewBox="0 0 330 74" width="100%" height="74" style="flex-shrink: 0; background: #090d16;">
                    <polygon points="10,12 318,6 310,62 6,58" fill="#e60012" stroke="#ffffff" stroke-width="2" />
                    <polygon points="16,18 314,12 308,54 12,50" fill="#090d16" />
                    <text x="165" y="34" font-family="IoskeleyMono, monospace" font-size="14" font-weight="bold" fill="#ffe600" text-anchor="middle">
                        // LOG DA PARTIDA //
                    </text>
                    <text id="ldp-recap-turn-label" x="165" y="49" font-family="IoskeleyMono, monospace" font-size="9.5" font-weight="bold" fill="#00f0ff" text-anchor="middle">
                        SYS.TIMELINE_RECAP [INÍCIO]
                    </text>
                </svg>

                <!-- Lista de Eventos com Scroll -->
                <div id="ldp-recap-feed" style="
                    flex: 1;
                    overflow-y: auto;
                    padding: 10px 12px 18px 26px;
                    position: relative;
                    background: repeating-linear-gradient(0deg, rgba(0,240,255,0.03) 0px, rgba(0,240,255,0.03) 1px, transparent 1px, transparent 4px);
                ">
                    <div style="position: absolute; left: 15px; top: 0; bottom: 0; width: 3px; background: #334155;"></div>
                </div>

                <!-- Rodapé -->
                <div style="
                    padding: 8px;
                    background: #1e293b;
                    border-top: 2px solid #ffe600;
                    color: #ffe600;
                    font-size: 10px;
                    font-weight: bold;
                    text-align: center;
                    flex-shrink: 0;
                ">
                    🖱️ HOVER NO EVENTO = PREVIEW DA CARTA
                </div>
            </div>
        `;

        document.body.appendChild(this.wrapperEl);

        this.drawerEl = this.wrapperEl.querySelector('#ldp-recap-drawer');
        this.toggleBtn = this.wrapperEl.querySelector('#ldp-recap-toggle-btn');
        this.arrowEl = this.wrapperEl.querySelector('#ldp-recap-arrow');
        this.feedEl = this.wrapperEl.querySelector('#ldp-recap-feed');
        this.turnLabelEl = this.wrapperEl.querySelector('#ldp-recap-turn-label');

        this.toggleBtn.addEventListener('click', () => this.toggleDrawer());

        this.scene.events.once('shutdown', () => this.destroy());
    }

    toggleDrawer(forceState) {
        this.isOpen = typeof forceState === 'boolean' ? forceState : !this.isOpen;
        if (this.drawerEl) {
            this.drawerEl.style.transform = this.isOpen ? 'translateX(0%)' : 'translateX(-100%)';
        }
        if (this.arrowEl) {
            this.arrowEl.textContent = this.isOpen ? '◀' : '▶';
        }
        if (this.isOpen) {
            this.scrollToBottom();
        }
    }

    recordSnapshot(snapshot) {
        if (!snapshot || !this.feedEl) return;

        const local = snapshot.localPlayer || {};
        const opp = snapshot.opponentPlayer || {};
        const localName = local.name || 'Você';
        const oppName = opp.name || 'Inimigo';
        const localHasInit = String(snapshot.activeTurnPlayerId) === String(this.scene.localPlayerId);
        const activePlayerName = localHasInit ? localName : oppName;

        const phase = snapshot.turnPhase;
        const trigger = snapshot.triggerContext || {};
        const eventType = trigger.eventType || '';

        const allHeroes = [...(local.heroes || []), ...(opp.heroes || [])];

        // 1. Início da Partida / Mudança de Fase
        if (this.lastPhase !== phase) {
            if (this.lastPhase === null) {
                this.turnCounter = 1;
                this.appendNode({
                    type: 'START',
                    title: '► INÍCIO DA PARTIDA',
                    lines: [`Iniciativa: ${activePlayerName}`],
                    accent: '#00f0ff',
                    dark: true
                });
                allHeroes.forEach(h => {
                    if (h?.uuid) this.previousHeroHp.set(h.uuid, h.vidaAtual);
                });
            } else if (phase === 'CHOICE') {
                this.turnCounter++;
                this.lastBattleHeroesKey = '';
                this.lastPrepCountKey = '';
            }

            if (this.turnLabelEl) {
                this.turnLabelEl.textContent = `SYS.TIMELINE_RECAP [TURNO 0${this.turnCounter}]`;
            }

            const phaseLabels = {
                CHOICE:      { label: `TURNO ${this.turnCounter} > ESCOLHA DE HERÓIS`, bg: '#ffe600', fg: '#090d16' },
                PREPARATION: { label: 'FASE > PREPARAÇÃO',                             bg: '#38bdf8', fg: '#090d16' },
                COMBAT_START:{ label: 'FASE > INÍCIO DO COMBATE',                      bg: '#f59e0b', fg: '#090d16' },
                RESOLUTION:  { label: 'FASE > RESOLUÇÃO DA PILHA',                     bg: '#e60012', fg: '#ffffff' },
                COMBAT:      { label: 'FASE > EMBATE FÍSICO',                          bg: '#b91c1c', fg: '#ffffff' },
                COMBAT_END:  { label: 'FASE > FIM DO COMBATE',                         bg: '#475569', fg: '#ffffff' },
                DISCARD:     { label: 'FASE > DESCARTE E COMPRA',                      bg: '#334155', fg: '#e2e8f0' },
                GAME_OVER:   { label: '🏆 FIM DE PARTIDA',                             bg: '#22c55e', fg: '#090d16' }
            };

            if (phaseLabels[phase]) {
                this.appendPhaseBadge(phaseLabels[phase].label, phaseLabels[phase].bg, phaseLabels[phase].fg);
            }
            this.lastPhase = phase;
        }

        // 2. Seleção de Heróis (Quando entram em BATTLE na transição CHOICE -> PREPARATION)
        const localBattleHero = (local.heroes || []).find(h => h.zoneId === 'BATTLE');
        const oppBattleHero = (opp.heroes || []).find(h => h.zoneId === 'BATTLE');
        if (localBattleHero && oppBattleHero) {
            const battleKey = `${this.turnCounter}:${localBattleHero.uuid}:${oppBattleHero.uuid}`;
            if (this.lastBattleHeroesKey !== battleKey) {
                this.lastBattleHeroesKey = battleKey;
                const chosenOwn = localHasInit ? localBattleHero : oppBattleHero;
                const chosenTarget = localHasInit ? oppBattleHero : localBattleHero;
                this.appendNode({
                    title: `🎯 ${activePlayerName} selecionou:`,
                    lines: [
                        `• Aliado: ${chosenOwn.cardName}`,
                        `• Alvo: ${chosenTarget.cardName}`
                    ],
                    accent: '#e60012',
                    previewDto: chosenOwn,
                    isHero: true
                });
            }
        }

        // 3. Cartas Programadas na Preparação (Ao iniciar COMBAT_START)
        if (phase === 'COMBAT_START' && eventType === 'PHASE_CHANGE') {
            const localPlayed = local.playedCardsInResolve || [];
            const oppPlayed = opp.playedCardsInResolve || [];
            const prepKey = `${this.turnCounter}:${localPlayed.length}:${oppPlayed.length}`;
            if (this.lastPrepCountKey !== prepKey && (localPlayed.length > 0 || oppPlayed.length > 0)) {
                this.lastPrepCountKey = prepKey;
                const localNames = localPlayed.map(c => `[${c.cardName}]`).join(', ') || 'Nenhuma';
                this.appendNode({
                    title: '🃏 Cartas Preparadas:',
                    lines: [
                        `• ${localName}: ${localNames}`,
                        `• ${oppName}: ${oppPlayed.length} carta(s) oculta(s)`
                    ],
                    accent: '#38bdf8'
                });
            }
        }

        // 4. Calcula variações de HP em relação ao snapshot anterior
        const hpChanges = [];
        allHeroes.forEach(h => {
            if (!h || !h.uuid) return;
            const prevHp = this.previousHeroHp.has(h.uuid) ? this.previousHeroHp.get(h.uuid) : h.vidaMaxima;
            const diff = h.vidaAtual - prevHp;
            if (diff !== 0) {
                hpChanges.push(`${h.cardName}: ${diff > 0 ? '+' : ''}${diff} HP (${h.vidaAtual}/${h.vidaMaxima})`);
            }
            this.previousHeroHp.set(h.uuid, h.vidaAtual);
        });

        // 5. Invocações, Resoluções de Cartas e Combate Físico
        if (eventType === 'CARD_SPAWNED' || eventType === 'TOKEN_SUMMONED') {
            const ownerIsLocal = String(trigger.ownerPlayerId) === String(this.scene.localPlayerId);
            const ownerTag = ownerIsLocal ? localName : oppName;
            this.appendNode({
                title: `✨ ${ownerTag} invocou:`,
                lines: [
                    `➔ [${trigger.spawnedCardName || 'Carta'}]`,
                    trigger.sourceCardName ? `Fonte: ${trigger.sourceCardName}` : ''
                ].filter(Boolean),
                accent: '#00f0ff',
                previewDto: trigger.spawnedCardData,
                isHero: false
            });
        } else if (eventType === 'EFFECT_RESOLVED') {
            const allCards = [
                ...(local.playedCardsInResolve || []),
                ...(opp.playedCardsInResolve || []),
                ...allHeroes
            ];
            const srcDto = allCards.find(c => c.uuid === trigger.sourceCardUuid);
            const isLocalCard = (local.playedCardsInResolve || []).some(c => c.uuid === trigger.sourceCardUuid)
                || (local.heroes || []).some(h => h.uuid === trigger.sourceCardUuid);
            const actorName = isLocalCard ? localName : oppName;

            this.appendNode({
                title: `⚡ ${actorName} > ${trigger.sourceCardName || 'Efeito'}`,
                lines: [
                    trigger.logMessage || 'Efeito resolvido.',
                    ...hpChanges
                ],
                accent: isLocalCard ? '#e60012' : '#2563eb',
                previewDto: srcDto,
                isHero: Boolean(srcDto && srcDto.vidaMaxima !== undefined)
            });
        } else if (eventType === 'PHYSICAL_COMBAT_EXCHANGE') {
            this.appendNode({
                title: '⚔️ Troca de Dano Físico:',
                lines: hpChanges.length > 0 ? hpChanges : ['Nenhum dano sofrido (Bloqueado).'],
                accent: '#b91c1c'
            });
        } else if (eventType === 'CARD_LIBERATED') {
            this.appendNode({
                title: '🔓 Selo de Liberação Rompido!',
                lines: [`[${trigger.spawnedCardName || trigger.sourceCardName || 'Carta Liberada'}] foi para a mão!`],
                accent: '#ffe600',
                previewDto: trigger.spawnedCardData,
                isHero: false
            });
        } else if (eventType === 'HERO_EVOLVED') {
            this.appendNode({
                title: '🔥 Evolução de Herói!',
                lines: [`${trigger.previousHeroName || 'Herói'} ➔ ${trigger.evolvedHeroName || ''}`],
                accent: '#ffe600'
            });
        } else if (eventType === 'COMBAT_END_ITEMS_ATTACHED') {
            const attachedSummary = allHeroes
                .filter(h => h.attachedCards && h.attachedCards.length > 0)
                .map(h => `🗡️ ${h.cardName}: ${h.attachedCards.map(a => a.cardName).join(', ')}`);
            if (attachedSummary.length > 0) {
                this.appendNode({
                    title: '🛡️ Equipamentos Acoplados:',
                    lines: attachedSummary,
                    accent: '#16a34a'
                });
            }
        }
    }

    appendPhaseBadge(text, bgColor, textColor) {
        const badge = document.createElement('div');
        badge.style.cssText = `
            position: relative;
            margin: 10px 0 6px 0;
            padding: 4px 8px;
            background: ${bgColor};
            color: ${textColor};
            border: 2px solid #090d16;
            font-size: 10.5px;
            font-weight: bold;
            clip-path: polygon(0 0, 100% 0, calc(100% - 8px) 100%, 0 100%);
        `;
        badge.textContent = text;
        this.feedEl.appendChild(badge);
        this.scrollToBottom();
    }

    appendNode({ title, lines = [], accent = '#e60012', dark = false, previewDto = null, isHero = false }) {
        const item = document.createElement('div');
        item.style.cssText = `
            position: relative;
            margin: 6px 0;
            padding: 6px 8px 6px 10px;
            background: ${dark ? '#1e293b' : '#faf8f5'};
            color: ${dark ? '#e2e8f0' : '#090d16'};
            border: 2px solid #090d16;
            border-left: 5px solid ${accent};
            box-shadow: 3px 3px 0px rgba(0,0,0,0.45);
            font-size: 10px;
            line-height: 1.35;
            cursor: ${previewDto ? 'pointer' : 'default'};
            transition: transform 0.1s ease;
        `;

        const dot = document.createElement('div');
        dot.style.cssText = `
            position: absolute;
            left: -20px;
            top: 8px;
            width: 9px;
            height: 9px;
            border-radius: 50%;
            background: ${accent};
            border: 2px solid #090d16;
        `;
        item.appendChild(dot);

        const titleEl = document.createElement('div');
        titleEl.style.cssText = `font-weight: bold; color: ${dark ? accent : '#090d16'}; margin-bottom: 2px;`;
        titleEl.textContent = title;
        item.appendChild(titleEl);

        lines.forEach(line => {
            const p = document.createElement('div');
            p.style.cssText = `color: ${dark ? '#cbd5e1' : '#1e293b'}; font-weight: bold;`;
            p.textContent = line;
            item.appendChild(p);
        });

        if (previewDto && this.scene) {
            item.addEventListener('mouseenter', () => {
                item.style.transform = 'translateX(3px)';
                this.scene.showLargeCardPreview(previewDto, isHero);
            });
            item.addEventListener('mouseleave', () => {
                item.style.transform = 'translateX(0)';
                this.scene.hideLargeCardPreview();
            });
        }

        this.feedEl.appendChild(item);
        this.scrollToBottom();
    }

    scrollToBottom() {
        if (this.feedEl) {
            this.feedEl.scrollTop = this.feedEl.scrollHeight;
        }
    }

    destroy() {
        if (this.wrapperEl) {
            this.wrapperEl.remove();
            this.wrapperEl = null;
        }
    }
}

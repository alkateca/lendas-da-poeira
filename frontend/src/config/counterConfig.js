import { BOARD_LAYOUT } from './BoardCoordinates.js';

export const COUNTER_VISUAL_REGISTRY = {
    POEIRA: {
        counterType: 'POEIRA',
        displayName: 'Poeira',
        iconKey: 'counter_poeira',
        accentColor: '#facc15',
        description: 'Marcador de Poeira acumulado sobre a carta.'
    },
    VENENO: {
        counterType: 'VENENO',
        displayName: 'Veneno',
        iconKey: 'counter_veneno',
        accentColor: '#22c55e',
        description: 'Marcador de Veneno acumulado sobre a carta.'
    },
    POLVORA: {
        counterType: 'POLVORA',
        displayName: 'Pólvora',
        iconKey: 'counter_polvora',
        accentColor: '#ef4444',
        description: 'Marcador de Pólvora acumulado sobre a carta.'
    },
    COMBUSTAO: {
        counterType: 'COMBUSTAO',
        displayName: 'Combustão',
        iconKey: 'counter_combustao',
        accentColor: '#f97316',
        description: 'Adiciona o valor total de Combustão ao dano causado apenas por Magias de Fogo contra o portador e zera esses contadores.'
    }
};

export const GENERIC_COUNTER_FALLBACK = {
    counterType: 'GENERIC',
    displayName: 'Contador',
    iconKey: 'counter_generic',
    accentColor: '#38bdf8',
    description: 'Marcador numérico acumulado sobre a carta.'
};

/**
 * Resolve metadados visuais de um contador combinando os campos vindos do snapshot
 * com o registro local de fallback.
 */
export function resolveCounterVisualMeta(counterEntry) {
    if (!counterEntry) return { ...GENERIC_COUNTER_FALLBACK };
    const rawKey = String(counterEntry.counterType || counterEntry.type || '')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .trim()
        .toUpperCase();

    const preset = COUNTER_VISUAL_REGISTRY[rawKey] || GENERIC_COUNTER_FALLBACK;
    return {
        counterType: rawKey || preset.counterType,
        displayName: counterEntry.displayName || preset.displayName,
        iconKey: counterEntry.iconKey || preset.iconKey,
        accentColor: counterEntry.accentColor || preset.accentColor,
        description: counterEntry.description || preset.description
    };
}

/**
 * Normaliza a coleção de contadores de um Herói ou Carta (array de DTOs ou mapa tipo->quantidade),
 * mantendo apenas marcadores com quantidade positiva (> 0).
 */
export function normalizeCountersList(cardOrHeroDto) {
    if (!cardOrHeroDto || !cardOrHeroDto.counters) return [];
    const raw = cardOrHeroDto.counters;

    const entries = Array.isArray(raw)
        ? raw
        : Object.entries(raw).map(([counterType, quantity]) => ({ counterType, quantity }));

    const normalized = [];
    entries.forEach((entry) => {
        if (!entry) return;
        const qty = Number(entry.quantity ?? entry.count ?? 0);
        if (!Number.isFinite(qty) || qty <= 0) return;

        const meta = resolveCounterVisualMeta(entry);
        normalized.push({
            ...meta,
            quantity: Math.floor(qty)
        });
    });
    return normalized;
}

/**
 * Formata o valor do contador no padrão visual "x(número)".
 */
export function formatCounterQuantity(quantity) {
    const safeQty = Math.max(0, Math.floor(Number(quantity) || 0));
    return `x${safeQty}`;
}

/**
 * Calcula posições locais (x, y) para alinhar as mini-cartas de contadores
 * sob a parte inferior da carta do Herói, na região da caixa de regras.
 */
export function computeCounterLayoutPositions(
    count,
    heroWidth = BOARD_LAYOUT.MEDIUM_CARD.width,
    heroHeight = BOARD_LAYOUT.MEDIUM_CARD.height
) {
    if (!count || count <= 0) return [];

    const baseHeroW = BOARD_LAYOUT.MEDIUM_CARD.width;
    const scale = heroWidth / baseHeroW;
    const miniW = BOARD_LAYOUT.COUNTER_CARD.width * scale;
    const miniH = BOARD_LAYOUT.COUNTER_CARD.height * scale;

    // Centro da caixa de regras em card_frame_hero.svg (x: 8..210 de 280)
    const rulesCenterX = ((109 / 280) - 0.5) * heroWidth;
    const maxRulesSpan = heroWidth * (204 / 280);
    const nominalStepX = miniW + (4 * scale);

    const stepX = count === 1
        ? 0
        : Math.min(nominalStepX, (maxRulesSpan - miniW) / (count - 1));

    const startX = rulesCenterX - ((count - 1) * stepX) / 2;

    // A metade superior fica sob a carta do Herói; a metade inferior fica visível logo abaixo da caixa de regras
    const y = (heroHeight / 2) + (1 * scale);

    return Array.from({ length: count }, (_, idx) => ({
        x: Math.round((startX + idx * stepX) * 100) / 100,
        y: Math.round(y * 100) / 100,
        scale,
        width: miniW,
        height: miniH
    }));
}

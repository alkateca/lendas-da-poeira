export const BOARD_LAYOUT = {
    WIDTH: 1920,
    HEIGHT: 1080,

    // 3 Níveis de Tamanho (LOD - Level of Detail) (+10% maiores)
    COMPACT_CARD: { width: 121, height: 156 }, // Carta parada na mão (só imagem + título)
    MEDIUM_CARD:  { width: 150, height: 200 }, // Heróis no banco + Carta da mão em Hover Rápido (com texto)
    LARGE_CARD:   { width: 308, height: 418 }, // Carta em Hover Longo (surge sobre o Botão Principal)

    COLORS: {
        BG_DARK: 0x181c24,          // Cinza-azulado médio-escuro (em vez de preto absoluto)
        ZONE_SURFACE: 0x262d3d,     // Superfície das bancadas e da arena bem visível e clara
        SLOT_SURFACE: 0x333d52,     // Slots das cartas mais claros para fácil identificação
        SLOT_ELEVATED: 0x3f4b66,    // Slots de Batalha Ativa destacados
        LOCAL_SLOT_TINT: 0x4c1d24,  // Tom avermelhado mais vivo nos slots aliados da pilha
        OPP_SLOT_TINT: 0x1e3a5f,    // Tom azul mais vivo nos slots do oponente da pilha
        GOLD_DUST: 0xffe600,        // Amarelo Ouro vibrante
        SNAP_VALID: 0x22c55e,
        LOCAL_RED: 0xe60012,
        OPP_BLUE: 0x00f0ff,
        CARD_COLORS: {
            WHITE: 0xf8fafc,
            BLUE: 0x38bdf8,
            RED: 0xef4444,
            GREEN: 0x22c55e,
            BLACK: 0x64748b,
            YELLOW: 0xfacc15,
            NEUTRAL: 0x94a3b8
        },
        CARD_THEMES: {
            LENDARIA: { headerBg: 0xffd700, border: 0xff55ff, stripe: 0xf59e0b, textColor: '#090d16' }, // Cor exclusiva de Raridade Lendária
            RED:      { headerBg: 0xdc2626, border: 0xef4444, stripe: 0x991b1b, textColor: '#ffffff' },
            BLUE:     { headerBg: 0x2563eb, border: 0x38bdf8, stripe: 0x1e40af, textColor: '#ffffff' },
            BLACK:    { headerBg: 0x1e293b, border: 0x9333ea, stripe: 0x090d16, textColor: '#f8fafc' },
            GREEN:    { headerBg: 0x16a34a, border: 0x4ade80, stripe: 0x14532d, textColor: '#ffffff' },
            YELLOW:   { headerBg: 0xfacc15, border: 0xffe600, stripe: 0xca8a04, textColor: '#090d16' },
            WHITE:    { headerBg: 0xf8fafc, border: 0xe2e8f0, stripe: 0x94a3b8, textColor: '#090d16' },
            NEUTRAL:  { headerBg: 0xCBD5E1, border: 0x94a3b8, stripe: 0x475569, textColor: '#090d16' }
        }
    },

    OPPONENT_HAND_SLOTS: [
        { x: 779,  y: -16, angle: 8 },
        { x: 907,  y: -10, angle: 4 },
        { x: 1035, y: -6,  angle: 0 },
        { x: 1163, y: -10, angle: -4 },
        { x: 1291, y: -16, angle: -8 }
    ],

    PLAYER_BANNERS: {
        OPPONENT: { x: 215, y: 72 },
        LOCAL:    { x: 215, y: 1008 }
    },

    LIBERATION_PANEL: {
        OPPONENT: { x: 1525, y: 135 },
        LOCAL:    { x: 1525, y: 945 }
    },

    PILES: {
        OPPONENT_DECK:    { x: 1695, y: 135 },
        OPPONENT_DISCARD: { x: 1835, y: 135 },

        LOCAL_DECK:       { x: 1695, y: 945 },
        LOCAL_DISCARD:    { x: 1835, y: 945 },

        MAIN_BUTTON:      { x: 1695, y: 540 },
        LARGE_PREVIEW:    { x: 215,  y: 540 }
    },

    BENCH_BOXES: {
        OPPONENT: { x: 310, y: 275, width: 530, height: 300 },
        LOCAL:    { x: 310, y: 805, width: 530, height: 300 }
    },

    OPPONENT_BENCH_SLOTS: [
        { x: 138, y: 285 },
        { x: 310, y: 285 },
        { x: 482, y: 285 }
    ],

    LOCAL_BENCH_SLOTS: [
        { x: 138, y: 815 },
        { x: 310, y: 815 },
        { x: 482, y: 815 }
    ],

    ARENA_CIRCLE: { x: 1035, y: 500, radius: 385 },

    BATTLE_SLOTS: {
        OPPONENT_ACTIVE_HERO: { x: 1035, y: 235 },
        LOCAL_ACTIVE_HERO:    { x: 1035, y: 765 }
    },

    RESOLVE_SLOTS: [
        { x: 756,  y: 500, order: 1 },
        { x: 942,  y: 500, order: 2 },
        { x: 1128, y: 500, order: 3 },
        { x: 1314, y: 500, order: 4 }
    ],

    LOCAL_HAND_SLOTS: [
        { x: 771,  y: 985, angle: -8 },
        { x: 903,  y: 970, angle: -4 },
        { x: 1035, y: 964, angle: 0 },
        { x: 1167, y: 970, angle: 4 },
        { x: 1299, y: 985, angle: 8 }
    ]
};

export function getCardVisualTheme(color, rarity) {
    const themes = BOARD_LAYOUT.COLORS.CARD_THEMES;
    if (String(rarity || '').toUpperCase() === 'LENDARIA') {
        return themes.LENDARIA;
    }
    const key = String(color || 'NEUTRAL').toUpperCase();
    return themes[key] || themes.NEUTRAL;
}

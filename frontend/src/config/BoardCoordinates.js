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
        }
    },

    // 1. TOPO CENTRAL: Mão do Oponente espaçada (sem sobreposição)
    OPPONENT_HAND_SLOTS: [
        { x: 779,  y: -16, angle: 8 },
        { x: 907,  y: -10, angle: 4 },
        { x: 1035, y: -6,  angle: 0 },
        { x: 1163, y: -10, angle: -4 },
        { x: 1291, y: -16, angle: -8 }
    ],

    // BANNERS DOS JOGADORES -> Agora no lado ESQUERDO da tela
    PLAYER_BANNERS: {
        OPPONENT: { x: 215, y: 72 },
        LOCAL:    { x: 215, y: 1008 }
    },

    // ZONA DE LIBERAÇÃO -> Agora no lado DIREITO (Tamanho MEDIUM_CARD: 140x190)
    LIBERATION_PANEL: {
        OPPONENT: { x: 1525, y: 135 },
        LOCAL:    { x: 1525, y: 945 }
    },

    // BARALHO E DESCARTE (LÁPIDE) -> Agora no lado DIREITO, ao lado da Zona de Liberação
    PILES: {
        OPPONENT_DECK:    { x: 1695, y: 135 }, // Tamanho MEDIUM_CARD (140x190)
        OPPONENT_DISCARD: { x: 1835, y: 135 }, // Ícone de Lápide (82x96)

        LOCAL_DECK:       { x: 1695, y: 945 }, // Tamanho MEDIUM_CARD (140x190)
        LOCAL_DISCARD:    { x: 1835, y: 945 }, // Ícone de Lápide (82x96)

        // Botão Confirmar centralizado na direita (entre os recursos do oponente e os seus)
        MAIN_BUTTON:      { x: 1695, y: 540 },

        // Preview Ampliado no centro-esquerda (entre os dois banners de nome)
        LARGE_PREVIEW:    { x: 215,  y: 540 }
    },

    // 3. ESQUERDA: Molduras e Slots das Bancadas de Heróis (ZoneId.BENCH - Tamanho Médio com espaçamento ampliado)
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

    // 4. CENTRO: Círculo da Arena + Batalha + 4 Slots Intercalados espaçados
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

    // 5. BASE CENTRAL: Mão do Jogador Local com espaçamento amplo (sem sobreposição)
    LOCAL_HAND_SLOTS: [
        { x: 771,  y: 985, angle: -8 },
        { x: 903,  y: 970, angle: -4 },
        { x: 1035, y: 964, angle: 0 },
        { x: 1167, y: 970, angle: 4 },
        { x: 1299, y: 985, angle: 8 }
    ]
};

export const KEYWORD_CATEGORIES = {
    TODOS: 'TODOS',
    ACAO_GATILHO: 'GATILHOS & AÇÕES',
    COMBATE: 'DANO & COMBATE',
    ESTADO: 'ESTADOS & ZONAS',
    TURNO: 'CICLO DO TURNO'
};

export const KEYWORDS_REGISTRY = {
    // ==========================================
    // 1. AÇÕES E GATILHOS
    // ==========================================
    INVOCAR: {
        id: 'INVOCAR',
        label: 'Invocar',
        category: KEYWORD_CATEGORIES.ACAO_GATILHO,
        badgeColor: '#38bdf8',
        pattern: /\b(invocar|invoque)\b/i,
        summary: 'Tutora ou gera a carta diretamente em jogo.',
        description: 'A carta citada é colocada diretamente na fila de Resolução ou anexada imediatamente ao Herói correspondente sem passar pela mão.'
    },
    AURA: {
        id: 'AURA',
        label: 'Aura',
        category: KEYWORD_CATEGORIES.ACAO_GATILHO,
        badgeColor: '#facc15',
        pattern: /\baura:/i,
        summary: 'Efeito passivo contínuo.',
        description: 'Habilidade passiva que permanece ativa continuamente enquanto o Herói portador estiver Vivo em campo.'
    },
    INICIO_DA_PARTIDA: {
        id: 'INICIO_DA_PARTIDA',
        label: 'Início da Partida',
        category: KEYWORD_CATEGORIES.ACAO_GATILHO,
        badgeColor: '#fb923c',
        pattern: /\bin[íi]cio da partida:/i,
        summary: 'Dispara uma única vez ao iniciar o jogo.',
        description: 'Gatilho resolvido automaticamente na abertura da partida, antes da primeira Fase de Escolha (CHOICE).'
    },
    INICIO_DO_COMBATE: {
        id: 'INICIO_DO_COMBATE',
        label: 'Início do Combate',
        category: KEYWORD_CATEGORIES.ACAO_GATILHO,
        badgeColor: '#f97316',
        pattern: /\bin[íi]cio d[eo] combate/i,
        summary: 'Ativa na fase COMBAT_START.',
        description: 'Gatilho temporal ativado na fase Início do Combate (TurnPhase.COMBAT_START), antes da revelação e resolução das cartas baixadas.'
    },
    FINAL_DO_COMBATE: {
        id: 'FINAL_DO_COMBATE',
        label: 'Final do Combate',
        category: KEYWORD_CATEGORIES.ACAO_GATILHO,
        badgeColor: '#818cf8',
        pattern: /\bfinal do combate/i,
        summary: 'Ativa na fase COMBAT_END.',
        description: 'Gatilho ativado no encerramento do combate (TurnPhase.COMBAT_END), após a troca de ataques físicos e antes da checagem de morte e limpeza de buffs temporários.'
    },
    UMA_VEZ_POR_COMBATE: {
        id: 'UMA_VEZ_POR_COMBATE',
        label: 'Uma vez por Combate',
        category: KEYWORD_CATEGORIES.ACAO_GATILHO,
        badgeColor: '#2dd4bf',
        pattern: /\buma vez por combate:/i,
        summary: 'Limite de 1 ativação por turno de combate.',
        description: 'Restringe o gatilho da habilidade para disparar apenas na primeira vez em que a condição for cumprida durante o combate atual.'
    },
    AO_EQUIPAR: {
        id: 'AO_EQUIPAR',
        label: 'Ao Equipar',
        category: KEYWORD_CATEGORIES.ACAO_GATILHO,
        badgeColor: '#60a5fa',
        pattern: /\b(ao equipar|enquanto estiver equipada):/i,
        summary: 'Efeito ativado ou mantido pelo item anexado.',
        description: 'Dispara quando o Item é anexado ao Herói na Fase de Resolução ou mantém o bônus ativo enquanto permanecer anexado.'
    },
    PRIORIDADE: {
        id: 'PRIORIDADE',
        label: 'Prioridade',
        category: KEYWORD_CATEGORIES.ACAO_GATILHO,
        badgeColor: '#10b981',
        pattern: /\bprioridade\b/i,
        summary: 'Resolve antes das cartas normais na fila de Resolução.',
        description: 'Refaz a fila na Fase de Resolução (TurnPhase.RESOLUTION), colocando esta carta no início da pilha antes de qualquer carta sem Prioridade. Se ambos os jogadores jogarem cartas com Prioridade, o jogador ativo que escolheu os Heróis do turno resolve primeiro.'
    },

    // ==========================================
    // 2. TIPOS DE DANO E COMBATE
    // ==========================================
    DANO_DIRETO: {
        id: 'DANO_DIRETO',
        label: 'Dano Direto',
        category: KEYWORD_CATEGORIES.COMBATE,
        badgeColor: '#ef4444',
        pattern: /\bdano direto\b/i,
        summary: 'Ignora Defesa, Espírito e reduções.',
        description: 'Dano puro que ignora Defesa, Espírito e quaisquer mitigações, subtraindo diretamente os pontos de Vida do alvo.'
    },
    DANO_MAGICO: {
        id: 'DANO_MAGICO',
        label: 'Dano Mágico',
        category: KEYWORD_CATEGORIES.COMBATE,
        badgeColor: '#c084fc',
        pattern: /\bdano m[áa]gico\b/i,
        summary: 'Mitigado pelo Espírito do alvo.',
        description: 'Dano elemental ou arcano. Fórmula base: Dano Sofrido = Dano Mágico - Espírito do Alvo.'
    },
    DANO_FISICO: {
        id: 'DANO_FISICO',
        label: 'Dano Físico',
        category: KEYWORD_CATEGORIES.COMBATE,
        badgeColor: '#f59e0b',
        pattern: /\b(dano f[íi]sico|ataque f[íi]sico)\b/i,
        summary: 'Mitigado pela Defesa do alvo.',
        description: 'Dano de impacto corporal ou de armas. Fórmula base: Dano Sofrido = Dano Físico - Defesa do Alvo.'
    },
    DANO_BONUS: {
        id: 'DANO_BONUS',
        label: 'Dano Bônus',
        category: KEYWORD_CATEGORIES.COMBATE,
        badgeColor: '#22c55e',
        pattern: /\bdano b[ôo]nus\b/i,
        summary: 'Aumenta o dano causado pelo Herói.',
        description: 'Modificador ofensivo somado ao dano causado pelas ações e ataques do Herói.'
    },
    REDUCAO_DE_DANO: {
        id: 'REDUCAO_DE_DANO',
        label: 'Redução de Dano',
        category: KEYWORD_CATEGORIES.COMBATE,
        badgeColor: '#dc2626',
        pattern: /\bredu[çc][ãa]o de dano\b/i,
        summary: 'Mitiga o dano físico ou mágico recebido.',
        description: 'Modificador defensivo que reduz o dano sofrido pelo Herói (não mitiga Dano Direto).'
    },
    VULNERABILIDADE: {
        id: 'VULNERABILIDADE',
        label: 'Vulnerabilidade',
        category: KEYWORD_CATEGORIES.COMBATE,
        badgeColor: '#a855f7',
        pattern: /\bvulnerabilidade\b/i,
        summary: 'Amplia o dano recebido pelo Herói.',
        description: 'Acúmulo negativo que aumenta o dano sofrido pelo Herói portador.'
    },
    EMPUNHADURA: {
        id: 'EMPUNHADURA',
        label: 'Empunhadura',
        category: KEYWORD_CATEGORIES.COMBATE,
        badgeColor: '#94a3b8',
        pattern: /\bempunhadura\b/i,
        summary: 'Custo de slots de arma (Padrão: 2 slots).',
        description: 'Armas ocupam slots de Empunhadura (Simples = 1, Dupla = 2). Um Herói possui 2 slots por padrão e não pode exceder seu limite máximo.'
    },

    // ==========================================
    // 3. ESTADOS DE JOGO
    // ==========================================
    VIVO: {
        id: 'VIVO',
        label: 'Vivo',
        category: KEYWORD_CATEGORIES.ESTADO,
        badgeColor: '#4ade80',
        pattern: /\bvivo[s]?\b/i,
        summary: 'Herói apto a atuar na partida.',
        description: 'O Herói permanece Vivo enquanto não for declarado derrotado na checagem final da fase COMBAT_END.'
    },
    MORTO: {
        id: 'MORTO',
        label: 'Morto',
        category: KEYWORD_CATEGORIES.ESTADO,
        badgeColor: '#64748b',
        pattern: /\b(morto|morre|reviva)\b/i,
        summary: 'Vida <= 0 ao final de COMBAT_END.',
        description: 'A Vida pode cair abaixo de 0 durante o turno, mas a morte só é contabilizada ao término da fase Final do Combate (TurnPhase.COMBAT_END) se a Vida for igual ou menor que 0.'
    },
    ATIVO: {
        id: 'ATIVO',
        label: 'Ativo / Inativo',
        category: KEYWORD_CATEGORIES.ESTADO,
        badgeColor: '#38bdf8',
        pattern: /\bestado ativo\b/i,
        summary: 'Disponibilidade do Herói para ser escolhido.',
        description: 'Heróis Ativos podem ser selecionados na Fase de Escolha para entrar em combate. Após combater, o Herói fica Inativo (💤) na bancada até recuperar seu estado Ativo.'
    },
    FERIDO: {
        id: 'FERIDO',
        label: 'Ferido',
        category: KEYWORD_CATEGORIES.ESTADO,
        badgeColor: '#f43f5e',
        pattern: /\bferido[s]?\b/i,
        summary: 'Vida Atual menor que a Vida Máxima.',
        description: 'Um Herói é considerado Ferido sempre que sua Vida Atual estiver abaixo da sua Vida Máxima.'
    },
    EXILAR: {
        id: 'EXILAR',
        label: 'Exilar',
        category: KEYWORD_CATEGORIES.ESTADO,
        badgeColor: '#e11d48',
        pattern: /\b(exilar|exile)\b/i,
        summary: 'Remove a carta da partida permanentemente.',
        description: 'Move a carta para a Zona de Exílio (ZoneId.EXILE), impedindo que ela retorne ao baralho ou seja reutilizada do descarte.'
    },

    // ==========================================
    // 4. ESTRUTURAS DE TURNO
    // ==========================================
    FASE: {
        id: 'FASE',
        label: 'Fases do Turno',
        category: KEYWORD_CATEGORIES.TURNO,
        badgeColor: '#00f0ff',
        pattern: /\bfase de\b/i,
        summary: 'Máquina de estados linear de 7 fases.',
        description: 'Todo turno segue estritamente 7 fases: 1. Escolha (CHOICE) → 2. Preparação Oculta (PREPARATION) → 3. Início do Combate (COMBAT_START) → 4. Resolução (RESOLUTION) → 5. Combate Físico (COMBAT) → 6. Final do Combate (COMBAT_END) → 7. Descarte (DISCARD).'
    },
    RESOLUCAO: {
        id: 'RESOLUCAO',
        label: 'Resolução',
        category: KEYWORD_CATEGORIES.TURNO,
        badgeColor: '#ffe600',
        pattern: /\bresolu[çc][ãa]o\b/i,
        summary: 'Etapa onde as cartas baixadas são reveladas e ativadas.',
        description: 'Na Fase de Resolução (TurnPhase.RESOLUTION), Magias, Ações e Teurgias alocadas são reveladas e resolvidas na fila, e Equipamentos são anexados aos Heróis.'
    }
};

/**
 * Retorna a lista de objetos de Keyword presentes em uma carta/herói.
 * Usa os IDs vindos do backend (cardDto.keywords) ou infere via regex como fallback.
 */
export function resolveCardKeywords(cardDto) {
    if (!cardDto || cardDto.faceUp === false) return [];

    const matchedIds = new Set();

    if (Array.isArray(cardDto.keywords) && cardDto.keywords.length > 0) {
        cardDto.keywords.forEach(id => {
            if (KEYWORDS_REGISTRY[id]) matchedIds.add(id);
        });
    } else {
        const desc = cardDto.descricao || cardDto.description || '';
        Object.values(KEYWORDS_REGISTRY).forEach(entry => {
            if (entry.pattern && entry.pattern.test(desc)) {
                matchedIds.add(entry.id);
            }
        });
        if (cardDto.itemType === 'ARMA' && cardDto.empunhadura > 0) {
            matchedIds.add('EMPUNHADURA');
        }
    }

    return Array.from(matchedIds).map(id => KEYWORDS_REGISTRY[id]).filter(Boolean);
}

/**
 * Estiliza gatilhos e keywords no texto da carta sem quebrar o wordWrap nativo do Phaser.
 */
export function formatCardDescriptionWithKeywords(rawText) {
    if (!rawText) return '';
    let formatted = rawText;

    const triggerHeaders = [
        'Início da Partida:',
        'Início do Combate:',
        'Final do Combate:',
        'Uma vez por Combate:',
        'Ao Equipar:',
        'Enquanto estiver Equipada:',
        'Aura:'
    ];

    triggerHeaders.forEach(header => {
        const regex = new RegExp(header, 'gi');
        formatted = formatted.replace(regex, match => `▸ ${match.toUpperCase()}`);
    });

    formatted = formatted.replace(/\bPrioridade\./gi, '[PRIORIDADE]');
    formatted = formatted.replace(/\bInvocar:\s*/gi, '⚡INVOCAR: ');
    formatted = formatted.replace(/\bExilar\./gi, '[EXILAR]');

    return formatted;
}

/**
 * Filtra o dicionário de keywords para o Modal de Glossário.
 */
export function searchGlossaryKeywords(query = '', category = KEYWORD_CATEGORIES.TODOS) {
    const cleanQuery = query.trim().toLowerCase();
    return Object.values(KEYWORDS_REGISTRY).filter(entry => {
        const matchesCategory = category === KEYWORD_CATEGORIES.TODOS || entry.category === category;
        if (!matchesCategory) return false;
        if (!cleanQuery) return true;
        return (
            entry.label.toLowerCase().includes(cleanQuery) ||
            entry.summary.toLowerCase().includes(cleanQuery) ||
            entry.description.toLowerCase().includes(cleanQuery)
        );
    });
}

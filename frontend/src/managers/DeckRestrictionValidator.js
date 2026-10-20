export default class DeckRestrictionValidator {

    /**
     * Extrai todas as permissões desbloqueadas pelos Heróis atualmente selecionados (0 a 3 Heróis)
     */
    static analyzeHeroTeam(selectedHeroes = []) {
        const hasWhiteHero = selectedHeroes.some(h => h.color === 'WHITE');
        const nonWhiteHero = selectedHeroes.find(h => h.color && h.color !== 'WHITE');
        const baseColor = nonWhiteHero ? nonWhiteHero.color : (hasWhiteHero ? 'WHITE' : null);

        const affinities = new Set();
        const ordersAndRaces = new Set();
        const classes = new Set();

        let hasEmissor = false;
        let hasGoblin = false;

        selectedHeroes.forEach(h => {
            (h.afinidades || []).forEach(a => affinities.add(String(a).toUpperCase()));
            (h.ordem || []).forEach(o => ordersAndRaces.add(String(o).toUpperCase()));
            (h.racas || []).forEach(r => {
                const upper = String(r).toUpperCase();
                ordersAndRaces.add(upper);
                // Normaliza "Guardiões do Trono" (plural em heros.json) com "Guardião do Trono" (singular em cards.json)
                if (upper.includes('GUARDI')) ordersAndRaces.add('GUARDIÃO DO TRONO');
                if (upper === 'GOBLIN') hasGoblin = true;
            });
            (h.classe || []).forEach(c => {
                const upper = String(c).toUpperCase();
                classes.add(upper);
                if (upper === 'EMISSOR') hasEmissor = true;
            });
        });

        return {
            heroCount: selectedHeroes.length,
            hasWhiteHero,
            baseColor,
            // Regra 3 definida pelo usuário: se houver Herói WHITE (coringa), só pode usar cartas WHITE e NEUTRAL
            allowedCardColors: hasWhiteHero
                ? ['WHITE', 'NEUTRAL']
                : (baseColor ? [baseColor, 'NEUTRAL'] : []),
            affinities,
            ordersAndRaces,
            classes,
            hasEmissor,
            hasGoblin
        };
    }

    /**
     * Verifica se um Herói do catálogo pode ser adicionado ao trio atual
     */
    static canSelectHero(candidateHero, selectedHeroes = []) {
        if (selectedHeroes.some(h => h.cardName === candidateHero.cardName)) {
            return { allowed: false, reason: 'Já está na equipe' };
        }
        if (selectedHeroes.length >= 3) {
            return { allowed: false, reason: 'Equipe completa (3/3)' };
        }

        const candidateColor = candidateHero.color || 'NEUTRAL';
        if (candidateColor === 'WHITE') {
            return { allowed: true, reason: 'Coringa (Restringe a cartas Brancas/Neutras)' };
        }

        const existingNonWhite = selectedHeroes.find(h => h.color && h.color !== 'WHITE');
        if (existingNonWhite && existingNonWhite.color !== candidateColor) {
            return { allowed: false, reason: `Requer cor ${existingNonWhite.color} (ou WHITE)` };
        }

        return { allowed: true, reason: 'Disponível' };
    }

    /**
     * Verifica se uma Carta do catálogo é liberada pelos 3 Heróis e se ainda cabe no baralho
     */
    static canAddCardToDeck(card, selectedHeroes = [], currentDeckCards = []) {
        if (selectedHeroes.length < 3) {
            return { allowed: false, reason: 'Escolha os 3 Heróis primeiro' };
        }

        const team = this.analyzeHeroTeam(selectedHeroes);
        const cardColor = (card.color || 'NEUTRAL').toUpperCase();

        // 1. Validação de Cor da Carta (Regra do Coringa WHITE ou Mesma Cor dos Heróis)
        if (!team.hasGoblin && !team.allowedCardColors.includes(cardColor)) {
            return {
                allowed: false,
                reason: team.hasWhiteHero
                    ? 'Herói Branco só permite cartas Brancas/Neutras'
                    : `Requer cor ${team.baseColor} ou Neutra`
            };
        }

        // 2. Validação de Afinidade Elemental e Magias (SPELL)
        const reqAffinities = (card.afinidades || []).map(a => String(a).toUpperCase());
        if (reqAffinities.length > 0) {
            const hasMatchingAffinity = reqAffinities.some(a => team.affinities.has(a));
            if (!hasMatchingAffinity && !team.hasEmissor && !team.hasGoblin) {
                return { allowed: false, reason: `Requer Afinidade [${card.afinidades.join('/')}] ou Emissor` };
            }
        } else if (card.cardType === 'SPELL' && team.affinities.size === 0 && !team.hasEmissor && !team.hasGoblin) {
            return { allowed: false, reason: 'Requer Herói com Afinidade ou Emissor' };
        }

        // 3. Validação de Ordem / Raça / Classe requerida pela carta
        const reqOrders = [...(card.ordem || []), ...(card.racas || [])].map(o => String(o).toUpperCase());
        if (reqOrders.length > 0 && !team.hasGoblin) {
            const hasMatchingOrder = reqOrders.some(o => team.ordersAndRaces.has(o));
            if (!hasMatchingOrder) {
                return { allowed: false, reason: `Requer [${[...(card.ordem || []), ...(card.racas || [])].join('/')}]` };
            }
        }

        const reqClasses = (card.classe || []).map(c => String(c).toUpperCase());
        if (reqClasses.length > 0 && !team.hasGoblin) {
            const hasMatchingClass = reqClasses.some(c => team.classes.has(c));
            if (!hasMatchingClass) {
                return { allowed: false, reason: `Requer Classe [${card.classe.join('/')}]` };
            }
        }

        // 4. Validação de Limite de Cópias (unica = máx 1, normal = máx 3) e Total de 30 Cartas
        if (currentDeckCards.length >= 30) {
            return { allowed: false, reason: 'Baralho cheio (30/30)' };
        }

        const copiesInDeck = currentDeckCards.filter(c => c.cardName === card.cardName).length;
        const maxCopies = card.unica === true ? 1 : 3;

        if (copiesInDeck >= maxCopies) {
            return {
                allowed: false,
                reason: card.unica ? 'Carta Única (Máx 1 cópia)' : 'Limite atingido (3/3 cópias)'
            };
        }

        return { allowed: true, reason: `Liberada (${copiesInDeck}/${maxCopies})` };
    }

    /**
     * Verifica se o baralho está pronto para iniciar uma partida (3 Heróis + 30 Cartas válidas)
     */
    static isDeckReadyForMatch(deck) {
        if (!deck || !deck.heroList || !deck.cardList) return false;
        return deck.heroList.length === 3 && deck.cardList.length === 30;
    }
}
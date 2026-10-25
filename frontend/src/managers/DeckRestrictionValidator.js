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
            (h.ordem || []).forEach(o => {
                const upper = String(o).toUpperCase();
                ordersAndRaces.add(upper);
                if (upper.includes('GUARDI')) {
                    ordersAndRaces.add('GUARDIÃO DO TRONO');
                    ordersAndRaces.add('GUARDIOES DO TRONO');
                    ordersAndRaces.add('GUARDAO_DO_TRONO');
                    ordersAndRaces.add('GUARDIOES_DO_TRONO');
                }
            });
            (h.racas || []).forEach(r => {
                const upper = String(r).toUpperCase();
                ordersAndRaces.add(upper);
                // Normaliza "Guardiões do Trono" (plural em heros.json) com "Guardião do Trono" (singular em cards.json)
                if (upper.includes('GUARDI')) {
                    ordersAndRaces.add('GUARDIÃO DO TRONO');
                    ordersAndRaces.add('GUARDIOES DO TRONO');
                    ordersAndRaces.add('GUARDAO_DO_TRONO');
                    ordersAndRaces.add('GUARDIOES_DO_TRONO');
                }
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
            // Regra 3: se houver Herói WHITE (coringa), só pode usar cartas WHITE e NEUTRAL
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
    static canSelectHero(hero, currentHeroList = [], player = null) {
        const ownedHeroes = player?.ownedHeroes || {};
        const hasCollectionData = Object.keys(ownedHeroes).length > 0;
        const ownedCount = hasCollectionData ? (ownedHeroes[hero.cardName] || 0) : 1;

        if (ownedCount <= 0) {
            return {
                allowed: false,
                compatibleWithTeam: true,
                notOwned: true,
                reason: 'NÃO ADQUIRIDO (0/1)'
            };
        }

        if (currentHeroList.some(h => h.cardName === hero.cardName)) {
            return { allowed: false, reason: 'Já está na equipe', compatibleWithTeam: false };
        }
        if (currentHeroList.length >= 3) {
            return { allowed: false, reason: 'Equipe completa (3/3)', compatibleWithTeam: false };
        }

        const candidateColor = hero.color || 'NEUTRAL';
        if (candidateColor === 'WHITE') {
            return { allowed: true, reason: 'Coringa (Restringe a cartas Brancas/Neutras)', compatibleWithTeam: true };
        }

        const existingNonWhite = currentHeroList.find(h => h.color && h.color !== 'WHITE');
        if (existingNonWhite && existingNonWhite.color !== candidateColor) {
            return {
                allowed: false,
                reason: `Requer cor ${existingNonWhite.color} (ou WHITE)`,
                compatibleWithTeam: false
            };
        }

        return { allowed: true, reason: 'Disponível', compatibleWithTeam: true };
    }

    /**
     * Avalia se uma Carta atende aos pré-requisitos de Cor, Afinidade, Ordem, Raça e Classe
     * dos Heróis atualmente selecionados (funciona dinamicamente para 1, 2 ou 3 Heróis!).
     */
    static checkCardHeroRequirements(card, selectedHeroes = []) {
        if (!selectedHeroes || selectedHeroes.length === 0) {
            return { compatible: true, reason: 'Selecione ao menos 1 Herói' };
        }

        const team = this.analyzeHeroTeam(selectedHeroes);
        const cardColor = (card.color || 'NEUTRAL').toUpperCase();

        // 1. Validação de Cor da Carta (Regra do Coringa WHITE ou Mesma Cor dos Heróis)
        if (!team.hasGoblin && !team.allowedCardColors.includes(cardColor)) {
            return {
                compatible: false,
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
                return { compatible: false, reason: `Requer Afinidade [${card.afinidades.join('/')}] ou Emissor` };
            }
        } else if (card.cardType === 'SPELL' && team.affinities.size === 0 && !team.hasEmissor && !team.hasGoblin) {
            return { compatible: false, reason: 'Requer Herói com Afinidade ou Emissor' };
        }

        // 3. Validação de Ordem / Raça requerida pela carta
        const reqOrders = [...(card.ordem || []), ...(card.racas || [])].map(o => String(o).toUpperCase());
        if (reqOrders.length > 0 && !team.hasGoblin) {
            const hasMatchingOrder = reqOrders.some(o => team.ordersAndRaces.has(o));
            if (!hasMatchingOrder) {
                return { compatible: false, reason: `Requer [${[...(card.ordem || []), ...(card.racas || [])].join('/')}]` };
            }
        }

        // 4. Validação de Classe requerida pela carta
        const reqClasses = (card.classe || []).map(c => String(c).toUpperCase());
        if (reqClasses.length > 0 && !team.hasGoblin) {
            const hasMatchingClass = reqClasses.some(c => team.classes.has(c));
            if (!hasMatchingClass) {
                return { compatible: false, reason: `Requer Classe [${card.classe.join('/')}]` };
            }
        }

        return { compatible: true, reason: 'Compatível com a equipe' };
    }

    /**
     * Verifica se uma Carta do catálogo pode ser adicionada ao baralho neste momento.
     * Permite adicionar cartas desde que a carta seja compatível com os Heróis já escolhidos (1 a 3 Heróis),
     * e revalida automaticamente caso um Herói seja removido ou trocado!
     */
    static canAddCardToDeck(card, currentHeroList = [], currentCardList = [], player = null) {
        const ownedCards = player?.ownedCards || {};
        const hasCollectionData = Object.keys(ownedCards).length > 0;
        const ownedCount = hasCollectionData ? (ownedCards[card.cardName] || 0) : (card.unica ? 1 : 3);

        const ruleMaxCopies = card.unica ? 1 : 3;
        const usedCopies = currentCardList.filter(c => c.cardName === card.cardName).length;

        if (ownedCount <= 0) {
            const reqCheck = this.checkCardHeroRequirements(card, currentHeroList);
            return {
                allowed: false,
                compatibleWithTeam: reqCheck.compatible,
                notOwned: true,
                usedCopies,
                ownedCount: 0,
                reason: 'NÃO POSSUÍDA (0)'
            };
        }

        // Verifica compatibilidade com os Heróis selecionados primeiro
        const reqCheck = this.checkCardHeroRequirements(card, currentHeroList);
        if (!reqCheck.compatible) {
            return {
                allowed: false,
                compatibleWithTeam: false,
                usedCopies,
                ownedCount,
                reason: reqCheck.reason
            };
        }

        if (currentCardList.length >= 30) {
            return { allowed: false, compatibleWithTeam: true, usedCopies, ownedCount, reason: 'Baralho cheio (30/30)' };
        }

        if (usedCopies >= ruleMaxCopies) {
            return {
                allowed: false,
                compatibleWithTeam: true,
                usedCopies,
                ownedCount,
                reason: `Limite da carta (${usedCopies}/${ruleMaxCopies})`
            };
        }

        if (usedCopies >= ownedCount) {
            return {
                allowed: false,
                compatibleWithTeam: true,
                usedCopies,
                ownedCount,
                reason: `Limite da Coleção (${usedCopies}/${ownedCount} possuídas)`
            };
        }

        return {
            allowed: true,
            compatibleWithTeam: true,
            usedCopies,
            ownedCount,
            reason: `ADICIONAR (${usedCopies}/${ownedCount} possuídas)`
        };
    }

    /**
     * Verifica se uma Carta de Liberação é compatível e pode ser equipada pela equipe atual
     */
    static canAddLiberationCard(card, currentHeroList = [], currentLiberationList = [], player = null) {
        const ownedCards = player?.ownedCards || {};
        const hasCollectionData = Object.keys(ownedCards).length > 0;
        const ownedCount = hasCollectionData ? (ownedCards[card.cardName] || 0) : 1;

        if (ownedCount <= 0) {
            return {
                allowed: false,
                compatibleWithTeam: true,
                notOwned: true,
                usedCopies: 0,
                ownedCount: 0,
                reason: 'NÃO POSSUÍDA (0/1)'
            };
        }

        if (!currentHeroList || currentHeroList.length === 0) {
            return { allowed: false, reason: 'Escolha seus Heróis primeiro', compatibleWithTeam: true };
        }

        if (card.targetBaseHeroName) {
            const hasRequiredHero = currentHeroList.some(
                h => h.cardName && h.cardName.toLowerCase() === card.targetBaseHeroName.toLowerCase()
            );
            if (!hasRequiredHero) {
                return {
                    allowed: false,
                    reason: `Requer Herói: ${card.targetBaseHeroName}`,
                    compatibleWithTeam: false
                };
            }
        } else {
            const reqCheck = this.checkCardHeroRequirements(card, currentHeroList);
            if (!reqCheck.compatible) {
                return { allowed: false, reason: reqCheck.reason, compatibleWithTeam: false };
            }
        }

        if (currentLiberationList && currentLiberationList.length >= 1) {
            const isSame = currentLiberationList[0]?.cardName === card.cardName;
            return {
                allowed: false,
                reason: isSame ? 'Já equipada (1/1)' : 'Já possui 1 Liberação equipada',
                compatibleWithTeam: true
            };
        }

        return { allowed: true, reason: 'Equipar na Zona de Liberação', compatibleWithTeam: true };
    }

    /**
     * Verifica se o baralho está pronto para iniciar uma partida (3 Heróis + 30 Cartas válidas)
     */
    static isDeckReadyForMatch(deck) {
        if (!deck || !deck.heroList || !deck.cardList) return false;
        return deck.heroList.length === 3 && deck.cardList.length === 30;
    }
}

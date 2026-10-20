export const LOCAL_PLAYER_UUID = 'a1b2c3d4-0000-0000-0000-000000000001';
export const OPPONENT_PLAYER_UUID = 'a1b2c3d4-0000-0000-0000-000000000002';
export const DEMO_MATCH_UUID = '99999999-0000-0000-0000-000000000001';

/**
 * Foto Inicial na TurnPhase.CHOICE:
 * Clique num Herói seu (vermelho) e num Herói inimigo vivo (azul) e depois no Botão Principal.
 */
export const INITIAL_MATCH_SNAPSHOT = {
    snapshotSequence: 1,
    turnPhase: 'CHOICE',
    activeTurnPlayerId: LOCAL_PLAYER_UUID,
    waitingForMainButton: true,
    triggerContext: {
        eventType: 'PHASE_CHANGE',
        sourceCardUuid: null,
        sourceCardName: null,
        effectClassName: null,
        logMessage: 'Fase de Escolha: Selecione 1 Herói seu e 1 Herói inimigo para o combate.'
    },
    localPlayer: {
        playerId: LOCAL_PLAYER_UUID,
        name: 'Jogador Local',
        deckCount: 15,
        handCount: 5,
        activeHeroId: null,
        heroes: [
            {
                uuid: '10000000-0000-0000-0000-000000000010',
                cardName: 'Isenora, Santa das Laminas',
                color: 'BLUE',
                descricao: 'Aura: Seus Aliados recebem +1 de Espírito, +1 de Ataque e +1 de Defesa.\nFinal do Turno: Cause 5 de Dano Mágico ao Herói Inimigo.',
                zoneId: 'BENCH',
                espirito: 2,
                ataque: 5,
                defesa: 1,
                vidaMaxima: 13,
                vidaAtual: 13,
                maxWeaponSlots: 2,
                usedWeaponSlots: 0,
                reducaoDano: 0,
                danoBonus: 0,
                vulnerabilidade: 0,
                estaVivo: true,
                estaAtivo: true,
                attachedCards: []
            },
            {
                uuid: '10000000-0000-0000-0000-000000000017',
                cardName: 'Cavaleiro Onear',
                color: 'BLUE',
                descricao: 'Uma vez por Combate: Ao jogar Magia, recebe +2 de Ataque.',
                zoneId: 'BENCH',
                espirito: 2,
                ataque: 7,
                defesa: 3,
                vidaMaxima: 15,
                vidaAtual: 15,
                maxWeaponSlots: 2,
                usedWeaponSlots: 0,
                reducaoDano: 0,
                danoBonus: 0,
                vulnerabilidade: 0,
                estaVivo: true,
                estaAtivo: true,
                attachedCards: []
            },
            {
                uuid: '10000000-0000-0000-0000-000000000016',
                cardName: 'Ageus, Constructo Cristalino',
                color: 'BLUE',
                descricao: 'Aura: Quando um Cavaleiro Aliado morre, seus outros Cavaleiros Aliados vivos recebem +2 de Espírito e +2 de Defesa.',
                zoneId: 'BENCH',
                espirito: 2,
                ataque: 7,
                defesa: 3,
                vidaMaxima: 15,
                vidaAtual: 15,
                maxWeaponSlots: 2,
                usedWeaponSlots: 0,
                reducaoDano: 0,
                danoBonus: 0,
                vulnerabilidade: 0,
                estaVivo: true,
                estaAtivo: true,
                attachedCards: []
            }
        ],
        hand: [
            {
                uuid: '20000000-0000-0000-0000-000000000001',
                cardName: 'Quimera',
                cardType: 'ITEM',
                color: 'BLUE',
                itemType: 'ARMA',
                empunhadura: 1,
                descricao: 'Arma - Empunhadura Simples - Cavaleiro\nSeu Herói Recebe +2 de Espírito +2 de Ataque +2 de Defesa.',
                zoneId: 'HAND',
                faceUp: true
            },
            {
                uuid: '30000000-0000-0000-0000-000000000010',
                cardName: 'Contra Ataque',
                cardType: 'SPELL',
                color: 'BLUE',
                itemType: 'NENHUM',
                empunhadura: 0,
                descricao: 'Afinidade Cristal - Cavaleiro\nDescarte uma Arma equipada no Herói Inimigo.\nSeu Herói Recebe +3 de Ataque até o Final do Combate.',
                zoneId: 'HAND',
                faceUp: true
            },
            {
                uuid: '20000000-0000-0000-0000-000000000009',
                cardName: 'Rapieira de Cristal',
                cardType: 'ITEM',
                color: 'BLUE',
                itemType: 'ARMA',
                empunhadura: 1,
                descricao: 'Arma - Empunhadura Simples - Cristal\nSeu Herói Recebe +3 de Ataque.\nFinal do Combate: O Herói Inimigo Sofre 5 de Dano Mágico.',
                zoneId: 'HAND',
                faceUp: true
            },
            {
                uuid: '40000000-0000-0000-0000-000000000002',
                cardName: 'Determinação Cristalina',
                cardType: 'ACTION',
                color: 'BLUE',
                itemType: 'NENHUM',
                empunhadura: 0,
                descricao: 'Cristal - Cavaleiro\nSeu Herói Recebe +1 de Ataque até o Final do Combate.\nJogue uma Carta do seu descarte.',
                zoneId: 'HAND',
                faceUp: true
            },
            {
                uuid: '40000000-0000-0000-0000-000000000008',
                cardName: 'Impulso de Poder',
                cardType: 'ACTION',
                color: 'BLUE',
                itemType: 'NENHUM',
                empunhadura: 0,
                descricao: 'Cristal\nAumente seu Dano Bônus em 2 até o Final do Combate.',
                zoneId: 'HAND',
                faceUp: true
            }
        ],
        playedCardsInResolve: [],
        discardPile: [
            {
                uuid: '30000000-0000-0000-0000-000000000012',
                cardName: 'Golpes Pesados',
                cardType: 'SPELL',
                color: 'BLUE',
                itemType: 'NENHUM',
                empunhadura: 0,
                descricao: 'Afinidade Cristal\nSeu Herói Recebe +X de Ataque até o Final do Combate, onde X é seu Espírito.',
                zoneId: 'DISCARD',
                faceUp: true
            }
        ],
        exilePile: []
    },
    opponentPlayer: {
        playerId: OPPONENT_PLAYER_UUID,
        name: 'Oponente',
        deckCount: 16,
        handCount: 5,
        activeHeroId: null,
        heroes: [
            {
                uuid: '10000000-0000-0000-0000-000000000008',
                cardName: 'Quimera Carniceira',
                color: 'BLACK',
                descricao: 'Início do Combate: Para cada Aliado Zumbi: Recebe +1 de Espírito e +1 de Ataque e recupere 2 de Vida.',
                zoneId: 'BENCH',
                espirito: 0,
                ataque: 5,
                defesa: 2,
                vidaMaxima: 14,
                vidaAtual: 6,
                maxWeaponSlots: 2,
                usedWeaponSlots: 0,
                reducaoDano: 0,
                danoBonus: 0,
                vulnerabilidade: 0,
                estaVivo: true,
                estaAtivo: true,
                attachedCards: []
            },
            {
                uuid: '10000000-0000-0000-0000-000000000007',
                cardName: 'Rainha Goblin',
                color: 'BLACK',
                descricao: 'Final do Combate: Cure seus Aliados em 2.',
                zoneId: 'BENCH',
                espirito: 3,
                ataque: 3,
                defesa: 2,
                vidaMaxima: 12,
                vidaAtual: 12,
                maxWeaponSlots: 2,
                usedWeaponSlots: 0,
                reducaoDano: 0,
                danoBonus: 0,
                vulnerabilidade: 0,
                estaVivo: true,
                estaAtivo: true,
                attachedCards: []
            },
            {
                // Exemplo de Herói já derrotado (estaVivo = false) que segue na mesa!
                uuid: '10000000-0000-0000-0000-000000000009',
                cardName: 'Necromante das Areias',
                color: 'BLACK',
                descricao: 'Início do Combate: Invocar: Ritos Fúnebres.',
                zoneId: 'BENCH',
                espirito: 3,
                ataque: 3,
                defesa: 2,
                vidaMaxima: 15,
                vidaAtual: 0,
                maxWeaponSlots: 2,
                usedWeaponSlots: 0,
                reducaoDano: 0,
                danoBonus: 0,
                vulnerabilidade: 0,
                estaVivo: true,
                estaAtivo: true,
                attachedCards: []
            }
        ],
        hand: [], // Oculto para o jogador local
        playedCardsInResolve: [],
        discardPile: [
            {
                uuid: '30000000-0000-0000-0000-000000000005',
                cardName: 'Atmosfera Pesada',
                cardType: 'SPELL',
                color: 'BLACK',
                itemType: 'NENHUM',
                empunhadura: 0,
                descricao: 'Afinidade Ar - Zumbi\nOs Heróis Inimigos Recebem -1 de Espírito.',
                zoneId: 'DISCARD',
                faceUp: true
            }
        ],
        exilePile: []
    }
};

/**
 * Gera fotos simuladas caso o servidor Java (localhost:8080) esteja desligado,
 * permitindo testar todo o ciclo visual no navegador.
 */
export function generateOfflineMockTimeline(currentSnapshot, payload) {
    const clone = JSON.parse(JSON.stringify(currentSnapshot));

    if (payload.currentPhase === 'CHOICE') {
        clone.turnPhase = 'PREPARATION';
        clone.localPlayer.activeHeroId = payload.selectedOwnHeroId;
        clone.opponentPlayer.activeHeroId = payload.selectedEnemyHeroId;

        clone.localPlayer.heroes.forEach(h => {
            if (h.uuid === payload.selectedOwnHeroId) h.zoneId = 'BATTLE';
        });
        clone.opponentPlayer.heroes.forEach(h => {
            if (h.uuid === payload.selectedEnemyHeroId) h.zoneId = 'BATTLE';
        });
        clone.triggerContext = {
            eventType: 'PHASE_CHANGE',
            logMessage: 'Fase de Preparação: Clique nas cartas da sua mão para alocá-las nos espaços cinzas centrais.'
        };
        return [clone];
    }

    if (payload.currentPhase === 'PREPARATION') {
        // 1. Foto de Revelação e Resolução (TurnPhase.RESOLUTION)
        const snapResolution = JSON.parse(JSON.stringify(clone));
        snapResolution.turnPhase = 'RESOLUTION';
        const playedCards = snapResolution.localPlayer.hand.filter(c => payload.playedCardUuids.includes(c.uuid));
        snapResolution.localPlayer.hand = snapResolution.localPlayer.hand.filter(c => !payload.playedCardUuids.includes(c.uuid));
        snapResolution.localPlayer.playedCardsInResolve = playedCards;

        const ownActiveHero = snapResolution.localPlayer.heroes.find(h => h.zoneId === 'BATTLE');
        if (ownActiveHero && playedCards.length > 0) {
            ownActiveHero.ataque += 3;
            snapResolution.triggerContext = {
                eventType: 'EFFECT_RESOLVED',
                sourceCardUuid: playedCards[0].uuid,
                logMessage: `Efeito de ${playedCards[0].cardName} ativado! ${ownActiveHero.cardName} recebeu +3 de Ataque!`
            };
        }

        // 2. Foto do Combate Físico (TurnPhase.COMBAT - vida pode ficar negativa)
        const snapCombat = JSON.parse(JSON.stringify(snapResolution));
        snapCombat.turnPhase = 'COMBAT';
        snapCombat.localPlayer.playedCardsInResolve = [];
        snapCombat.localPlayer.discardPile.push(...playedCards);

        const p1Hero = snapCombat.localPlayer.heroes.find(h => h.zoneId === 'BATTLE');
        const p2Hero = snapCombat.opponentPlayer.heroes.find(h => h.zoneId === 'BATTLE');
        if (p1Hero && p2Hero) {
            const dmgToP2 = Math.max(0, p1Hero.ataque - p2Hero.defesa);
            const dmgToP1 = Math.max(0, p2Hero.ataque - p1Hero.defesa);
            p2Hero.vidaAtual -= dmgToP2;
            p1Hero.vidaAtual -= dmgToP1;
            snapCombat.triggerContext = {
                eventType: 'PHYSICAL_COMBAT_EXCHANGE',
                sourceCardUuid: p1Hero.uuid,
                logMessage: `Combate Físico! ${p2Hero.cardName} sofreu ${dmgToP2} de dano (Vida: ${p2Hero.vidaAtual}).`
            };
        }

        // 3. Foto do Final do Combate e Checagem de Morte (TurnPhase.COMBAT_END -> DISCARD)
        const snapEnd = JSON.parse(JSON.stringify(snapCombat));
        snapEnd.turnPhase = 'DISCARD';
        snapEnd.localPlayer.heroes.forEach(h => {
            if (h.zoneId === 'BATTLE') {
                h.zoneId = 'BENCH';
                h.estaAtivo = false;
            }
            if (h.vidaAtual <= 0) h.estaVivo = false;
        });
        snapEnd.opponentPlayer.heroes.forEach(h => {
            if (h.zoneId === 'BATTLE') {
                h.zoneId = 'BENCH';
                h.estaAtivo = false;
            }
            if (h.vidaAtual <= 0) h.estaVivo = false;
        });
        snapEnd.triggerContext = {
            eventType: 'COMBAT_END_CLEANUP',
            logMessage: 'Final do Combate concluído! Fase de Descarte: selecione cartas da mão para descartar ou confirme.'
        };

        return [snapResolution, snapCombat, snapEnd];
    }

    if (payload.currentPhase === 'DISCARD') {
        const discarded = clone.localPlayer.hand.filter(c => payload.discardCardUuids.includes(c.uuid));
        clone.localPlayer.hand = clone.localPlayer.hand.filter(c => !payload.discardCardUuids.includes(c.uuid));
        clone.localPlayer.discardPile.push(...discarded);

        // Alterna a iniciativa entre Jogador Local e Oponente para demonstrar a troca de cores nos 4 slots!
        clone.activeTurnPlayerId = (clone.activeTurnPlayerId === LOCAL_PLAYER_UUID)
            ? OPPONENT_PLAYER_UUID
            : LOCAL_PLAYER_UUID;

        clone.turnPhase = 'CHOICE';
        clone.triggerContext = {
            eventType: 'PHASE_CHANGE',
            logMessage: 'Novo Turno! A iniciativa mudou e as cores dos 4 slots centrais foram intercaladas.'
        };
        return [clone];
    }

    return [clone];
}
import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import {
    COUNTER_VISUAL_REGISTRY,
    resolveCounterVisualMeta,
    normalizeCountersList,
    formatCounterQuantity,
    computeCounterLayoutPositions
} from '../../config/counterConfig.js';
import {
    KEYWORDS_REGISTRY,
    resolveCardKeywords,
    formatCardDescriptionWithKeywords,
    searchGlossaryKeywords
} from '../../config/keywordsConfig.js';
import { BOARD_LAYOUT } from '../../config/BoardCoordinates.js';

describe('counterConfig — Sistema Visual de Contadores em Cartas', () => {
    it('deve manter apenas Combustão, Seda e fallback genérico (sem Pólvora, Veneno ou Poeira)', () => {
        const combustao = resolveCounterVisualMeta({ counterType: 'combustao' });
        assert.equal(combustao.displayName, 'Combustão');
        assert.equal(combustao.iconKey, 'counter_combustao');

        const seda = resolveCounterVisualMeta({ counterType: 'seda' });
        assert.equal(seda.displayName, 'Seda');
        assert.equal(seda.iconKey, 'counter_seda');

        assert.equal(COUNTER_VISUAL_REGISTRY.POEIRA, undefined);
        assert.equal(COUNTER_VISUAL_REGISTRY.VENENO, undefined);
        assert.equal(COUNTER_VISUAL_REGISTRY.POLVORA, undefined);
        assert.ok(COUNTER_VISUAL_REGISTRY.COMBUSTAO);
        assert.ok(COUNTER_VISUAL_REGISTRY.SEDA);

        const unknown = resolveCounterVisualMeta({ counterType: 'CARGA_ARCANICA' });
        assert.equal(unknown.counterType, 'CARGA_ARCANICA');
        assert.equal(unknown.iconKey, 'counter_generic');
    });

    it('deve formatar quantidades no padrão x(número)', () => {
        assert.equal(formatCounterQuantity(1), 'x1');
        assert.equal(formatCounterQuantity(5), 'x5');
        assert.equal(formatCounterQuantity(12), 'x12');
        assert.equal(formatCounterQuantity(-3), 'x0');
    });

    it('deve normalizar contadores ignorando quantidades <= 0 (array ou mapa)', () => {
        const fromArray = normalizeCountersList({
            counters: [
                { counterType: 'SEDA', quantity: 3 },
                { counterType: 'COMBUSTAO', quantity: 0 },
                { counterType: 'SEDA', quantity: -1 },
                null
            ]
        });
        assert.equal(fromArray.length, 1);
        assert.equal(fromArray[0].counterType, 'SEDA');
        assert.equal(fromArray[0].quantity, 3);

        const fromMap = normalizeCountersList({
            counters: { SEDA: 2, COMBUSTAO: 4 }
        });
        assert.equal(fromMap.length, 2);
        assert.equal(fromMap[0].counterType, 'SEDA');
        assert.equal(fromMap[1].counterType, 'COMBUSTAO');
        assert.equal(fromMap[1].quantity, 4);
    });

    it('deve alocar os contadores sob a base do Herói próximos à caixa de regras sem extrapolar a largura', () => {
        const { width, height } = BOARD_LAYOUT.MEDIUM_CARD; // 150 x 200
        const one = computeCounterLayoutPositions(1, width, height);
        assert.equal(one.length, 1);
        assert.ok(one[0].y > height / 2, 'O centro da mini-carta deve ficar abaixo da borda inferior do Herói');
        assert.ok(one[0].x < 0, 'Deve ficar centralizado sob a caixa de regras inferior esquerda');

        const three = computeCounterLayoutPositions(3, width, height);
        assert.equal(three.length, 3);
        const halfMiniW = three[0].width / 2;
        assert.ok(three[0].x - halfMiniW >= -width / 2, '1º contador não deve vazar à esquerda do Herói');
        assert.ok(three[2].x + halfMiniW <= width / 2, '3º contador não deve colidir com a borda direita do Herói');
        assert.ok(three[0].x < three[1].x && three[1].x < three[2].x);

        const five = computeCounterLayoutPositions(5, width, height);
        assert.equal(five.length, 5);
        assert.ok(five[0].x - halfMiniW >= -width / 2);
        assert.ok(five[4].x + halfMiniW <= 45, 'Múltiplos contadores devem permanecer na faixa da caixa de regras');
    });

    it('deve registrar Combustão e Seda como contadores visuais e keywords destacáveis', () => {
        const combustaoMeta = resolveCounterVisualMeta({ counterType: 'COMBUSTAO' });
        assert.equal(combustaoMeta.displayName, 'Combustão');
        assert.equal(combustaoMeta.iconKey, 'counter_combustao');

        const sedaMeta = resolveCounterVisualMeta({ counterType: 'SEDA' });
        assert.equal(sedaMeta.displayName, 'Seda');
        assert.equal(sedaMeta.iconKey, 'counter_seda');

        assert.ok(KEYWORDS_REGISTRY.COMBUSTAO);
        assert.ok(KEYWORDS_REGISTRY.SEDA);

        const resolvedCombustao = resolveCardKeywords({
            descricao: 'Afinidade Cristal\nO Herói Inimigo recebe 5 Marcadores de Combustão.',
            faceUp: true
        });
        assert.ok(resolvedCombustao.some(k => k.id === 'COMBUSTAO'));

        const resolvedSeda = resolveCardKeywords({
            descricao: 'Consuma todos os Marcadores de Seda: Seu Herói recebe +2 de Defesa.',
            faceUp: true
        });
        assert.ok(resolvedSeda.some(k => k.id === 'SEDA'));

        assert.ok(formatCardDescriptionWithKeywords('Recebe 5 Marcadores de Combustão.').includes('[COMBUSTÃO]'));
        assert.ok(formatCardDescriptionWithKeywords('Consuma 2 Marcadores de Seda.').includes('[SEDA]'));

        assert.ok(searchGlossaryKeywords('combustão').some(k => k.id === 'COMBUSTAO'));
        assert.ok(searchGlossaryKeywords('seda').some(k => k.id === 'SEDA'));
    });
});

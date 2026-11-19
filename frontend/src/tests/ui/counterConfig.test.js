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
    it('deve resolver metadados visuais de Poeira, Veneno, Pólvora e fallback genérico', () => {
        const poeira = resolveCounterVisualMeta({ counterType: 'POEIRA' });
        assert.equal(poeira.displayName, 'Poeira');
        assert.equal(poeira.iconKey, 'counter_poeira');

        const veneno = resolveCounterVisualMeta({ counterType: 'veneno' });
        assert.equal(veneno.displayName, 'Veneno');
        assert.equal(veneno.iconKey, 'counter_veneno');

        const polvora = resolveCounterVisualMeta({ counterType: 'Pólvora' });
        assert.equal(polvora.displayName, 'Pólvora');
        assert.equal(polvora.iconKey, 'counter_polvora');

        const unknown = resolveCounterVisualMeta({ counterType: 'CARGA_ARCANICA' });
        assert.equal(unknown.counterType, 'CARGA_ARCANICA');
        assert.equal(unknown.iconKey, 'counter_generic');
        assert.ok(COUNTER_VISUAL_REGISTRY.POEIRA);
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
                { counterType: 'POEIRA', quantity: 3 },
                { counterType: 'VENENO', quantity: 0 },
                { counterType: 'POLVORA', quantity: -1 },
                null
            ]
        });
        assert.equal(fromArray.length, 1);
        assert.equal(fromArray[0].counterType, 'POEIRA');
        assert.equal(fromArray[0].quantity, 3);

        const fromMap = normalizeCountersList({
            counters: { POEIRA: 2, VENENO: 4, POLVORA: 0 }
        });
        assert.equal(fromMap.length, 2);
        assert.equal(fromMap[0].counterType, 'POEIRA');
        assert.equal(fromMap[1].counterType, 'VENENO');
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

    it('deve registrar Combustão como contador visual e keyword destacável com explicação', () => {
        const combustaoMeta = resolveCounterVisualMeta({ counterType: 'COMBUSTAO' });
        assert.equal(combustaoMeta.displayName, 'Combustão');
        assert.equal(combustaoMeta.iconKey, 'counter_combustao');

        assert.ok(KEYWORDS_REGISTRY.COMBUSTAO);
        const resolved = resolveCardKeywords({
            descricao: 'Afinidade Cristal\nO Herói Inimigo recebe 5 Marcadores de Combustão.',
            faceUp: true
        });
        assert.ok(resolved.some(k => k.id === 'COMBUSTAO'));

        const formatted = formatCardDescriptionWithKeywords('O Herói Inimigo recebe 5 Marcadores de Combustão.');
        assert.ok(formatted.includes('[COMBUSTÃO]'));

        const glossaryHits = searchGlossaryKeywords('combustão');
        assert.ok(glossaryHits.some(k => k.id === 'COMBUSTAO'));
    });
});

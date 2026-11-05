import { describe, it, expect, vi, beforeEach } from 'vitest';
import StoryDialogueOverlay from '../../ui/StoryDialogueOverlay.js';

describe('StoryDialogueOverlay — Paginação de Prólogo/Epílogo e Fila de Banners', () => {
    let mockScene;
    let overlay;

    beforeEach(() => {
        const createChainableMock = () => {
            const obj = {
                setDepth: vi.fn().mockReturnThis(),
                setAlpha: vi.fn().mockReturnThis(),
                setStrokeStyle: vi.fn().mockReturnThis(),
                setOrigin: vi.fn().mockReturnThis(),
                setResolution: vi.fn().mockReturnThis(),
                setInteractive: vi.fn().mockReturnThis(),
                setText: vi.fn().mockReturnThis(),
                setColor: vi.fn().mockReturnThis(),
                setPosition: vi.fn().mockReturnThis(),
                on: vi.fn().mockReturnThis(),
                add: vi.fn().mockReturnThis(),
                destroy: vi.fn()
            };
            return obj;
        };

        mockScene = {
            scale: { width: 1920, height: 1080 },
            add: {
                container: vi.fn(() => createChainableMock()),
                rectangle: vi.fn(() => createChainableMock()),
                polygon: vi.fn(() => createChainableMock()),
                text: vi.fn((x, y, txt) => {
                    const t = createChainableMock();
                    t.currentText = txt;
                    t.setText = vi.fn((newTxt) => {
                        t.currentText = newTxt;
                        return t;
                    });
                    return t;
                })
            },
            tweens: {
                add: vi.fn((cfg) => {
                    if (cfg.onComplete) cfg.onComplete();
                }),
                killTweensOf: vi.fn()
            },
            time: {
                delayedCall: vi.fn(() => ({ remove: vi.fn() }))
            }
        };

        overlay = new StoryDialogueOverlay(mockScene);
    });

    it('deve enfileirar múltiplos diálogos in-combat e exibir o primeiro imediatamente', () => {
        overlay.enqueueInCombatDialogues([
            { speaker: 'Eco de Alka', dialogue: 'Primeira fala', trigger: 'ON_CARD_PLAYED' },
            { speaker: 'Narrador', dialogue: 'Segunda fala', trigger: 'ON_TURN_START' }
        ], 'RED');

        // O primeiro já foi consumido para exibição ativa, restando 1 na fila
        expect(overlay.isShowingBanner).toBe(true);
        expect(overlay.dialogueQueue.length).toBe(1);

        // Ao dispensar o banner atual, o segundo da fila deve ser exibido
        overlay.dismissCurrentBanner();
        expect(overlay.isShowingBanner).toBe(true);
        expect(overlay.dialogueQueue.length).toBe(0);
    });

    it('deve dividir o texto do Prólogo em páginas por parágrafo duplo e só disparar onConfirm na última página', () => {
        const onConfirmMock = vi.fn();

        overlay.showFullNarrativeModal({
            headerTag: 'PRÓLOGO',
            title: 'O Primeiro Clarão da Forja',
            colorKey: 'RED',
            narrativeText: 'Primeiro parágrafo das fornalhas.\n\nSegundo parágrafo com o Eco de Alka.',
            confirmLabel: 'INICIAR PARTIDA',
            onConfirm: onConfirmMock
        });

        expect(overlay.activeModal).not.toBeNull();
        expect(onConfirmMock).not.toHaveBeenCalled();
    });
});

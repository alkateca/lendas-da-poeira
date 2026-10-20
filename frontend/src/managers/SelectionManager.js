export default class SelectionManager {
    constructor(matchScene) {
        this.scene = matchScene;
        this.resetSelections();
    }

    resetSelections() {
        this.selectedOwnHeroId = null;
        this.selectedEnemyHeroId = null;
        this.playedCardSlotMap = new Map(); // cardUuid -> slotIndex (0 a 3)
        this.discardCardUuids = [];         // Cartas destacadas na mão para descarte na fase DISCARD
    }

    /**
     * Índices dos 4 slots de RESOLVE pertencentes ao Jogador Local (Vermelho):
     * - Com iniciativa: [0, 2] (1º e 3º)
     * - Sem iniciativa: [1, 3] (2º e 4º)
     */
    getLocalPlayerAllowedResolveIndices(hasInitiative) {
        return hasInitiative ? [0, 2] : [1, 3];
    }

    /**
     * Valida e seleciona (ou desmarca) um Herói na fase CHOICE
     */
    assignHeroSelection(heroPrefab, allowToggleOff = false) {
        const heroData = heroPrefab.heroData;
        if (!heroData.estaVivo) {
            this.scene.showToast('Este Herói está derrotado (estaVivo = false)!');
            return false;
        }

        if (!heroPrefab.isOpponent) {
            if (!heroData.estaAtivo) {
                this.scene.showToast('Este Herói já atuou neste ciclo!');
                return false;
            }
            if (allowToggleOff && this.selectedOwnHeroId === heroData.uuid) {
                this.selectedOwnHeroId = null;
                return false;
            }
            this.selectedOwnHeroId = heroData.uuid;
        } else {
            if (allowToggleOff && this.selectedEnemyHeroId === heroData.uuid) {
                this.selectedEnemyHeroId = null;
                return false;
            }
            this.selectedEnemyHeroId = heroData.uuid;
        }
        return true;
    }

    /**
     * Encaixa uma carta da mão num slot vermelho específico (ou no próximo livre se targetSlotIndex for null)
     */
    tryAssignCardToResolveSlot(cardUuid, targetSlotIndex, hasInitiative) {
        const allowedIndices = this.getLocalPlayerAllowedResolveIndices(hasInitiative); // [0, 2] ou [1, 3]
        let finalSlotIndex = targetSlotIndex;

        // Se veio de um clique simples (targetSlotIndex == null) ou soltou sobre slot azul, acha o primeiro slot vermelho livre
        if (finalSlotIndex === null || finalSlotIndex === undefined || !allowedIndices.includes(finalSlotIndex)) {
            const occupiedSlots = Array.from(this.playedCardSlotMap.entries())
                .filter(([uuid]) => uuid !== cardUuid)
                .map(([, idx]) => idx);

            const freeRedSlot = allowedIndices.find(idx => !occupiedSlots.includes(idx));
            if (freeRedSlot === undefined) {
                // Se os 2 slots vermelhos já estão cheios, substitui o segundo slot vermelho
                finalSlotIndex = allowedIndices[1];
            } else {
                finalSlotIndex = freeRedSlot;
            }
        }

        // Se já havia outra carta neste slot vermelho, libera a anterior de volta para a mão
        for (const [existingUuid, slotIdx] of this.playedCardSlotMap.entries()) {
            if (slotIdx === finalSlotIndex && existingUuid !== cardUuid) {
                this.playedCardSlotMap.delete(existingUuid);
            }
        }

        this.playedCardSlotMap.set(cardUuid, finalSlotIndex);
        return true;
    }

    removeCardFromResolve(cardUuid) {
        this.playedCardSlotMap.delete(cardUuid);
    }

    /**
     * Alterna o destaque de descarte da carta na própria mão (0 a X cartas)
     */
    toggleDiscardCard(cardUuid, forceState = null) {
        const idx = this.discardCardUuids.indexOf(cardUuid);
        const shouldSelect = forceState !== null ? forceState : (idx === -1);

        if (shouldSelect && idx === -1) {
            this.discardCardUuids.push(cardUuid);
        } else if (!shouldSelect && idx > -1) {
            this.discardCardUuids.splice(idx, 1);
        }
    }

    getOrderedPlayedCardUuids() {
        return Array.from(this.playedCardSlotMap.entries())
            .sort((a, b) => a[1] - b[1])
            .map(entry => entry[0]);
    }

    canConfirmAction(currentPhase, isMyInitiative) {
        if (this.scene.snapshotPlayer.isPlaying) return false;

        switch (currentPhase) {
            case 'CHOICE':
                return isMyInitiative && this.selectedOwnHeroId !== null && this.selectedEnemyHeroId !== null;
            case 'PREPARATION':
                return true; // Pode confirmar com 0, 1 ou 2 cartas
            case 'DISCARD':
                return true; // Pode confirmar com 0 a X cartas
            default:
                return false;
        }
    }

    buildActionRequestDTO(matchId, localPlayerId, currentPhase) {
        return {
            matchId: matchId,
            playerId: localPlayerId,
            currentPhase: currentPhase,
            selectedOwnHeroId: this.selectedOwnHeroId,
            selectedEnemyHeroId: this.selectedEnemyHeroId,
            playedCardUuids: this.getOrderedPlayedCardUuids(),
            discardCardUuids: [...this.discardCardUuids]
        };
    }
}
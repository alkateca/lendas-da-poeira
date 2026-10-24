export default class SnapshotPlayerManager {
    constructor(scene) {
        this.scene = scene;
        this.queue = [];
        this.isPlaying = false;
        this.lastPlayedSequence = 0;
        this.activeTimer = null;

        // Tabela de tempos (em ms) para dar peso e leitura a cada momento da partida
        this.PACING = {
            BASE_STEP: 1150,           // Tempo base padrão entre resoluções normais
            COMBAT_ATTACK: 1550,       // Ataques físicos e mágicos (tempo para ver o sprite + número flutuante)
            EFFECT_RESOLVED: 1450,     // Resolução de feitiços, ações, itens e teurgias
            CARD_SPAWNED: 1600,        // Carta invocada voando para a pilha
            CARD_LIBERATED: 2300,      // Animação da Carta de Liberação indo para a mão
            HERO_EVOLVED: 2200,        // Anel dourado + explosão de evolução
            PHASE_BREATH_BUFFER: 650,  // Respiro extra ANTES de virar a fase/turno
            CUTIN_DURATION: 1150       // Tempo reservado para a faixa de Novo Turno/Fase após os VFX
        };
    }

    enqueueSnapshots(snapshots = []) {
        if (!Array.isArray(snapshots) || snapshots.length === 0) return;

        snapshots.forEach(snap => {
            const seq = snap.snapshotSequence || 0;
            if (seq === 0 || seq > this.lastPlayedSequence) {
                this.queue.push(snap);
                if (seq > this.lastPlayedSequence) {
                    this.lastPlayedSequence = seq;
                }
            }
        });

        if (!this.isPlaying && this.queue.length > 0) {
            this.isPlaying = true;
            this.scene.updateMainButtonState();
            this.playNext();
        }
    }

    playNext() {
        if (this.queue.length === 0) {
            this.isPlaying = false;
            this.scene.updateMainButtonState();
            return;
        }

        const nextSnapshot = this.queue.shift();
        const isLastInQueue = this.queue.length === 0;

        const prevPhase = this.scene.currentSnapshot ? this.scene.currentSnapshot.turnPhase : null;
        const nextPhase = nextSnapshot.turnPhase;
        const phaseChanged = Boolean(prevPhase && prevPhase !== nextPhase);

        // Verifica se este snapshot traz uma faixa de anúncio de Fase/Turno Principal
        const shouldShowPhaseBanner = this.shouldAnnouncePhaseChange(prevPhase, nextPhase, nextSnapshot);

        // 1. Calcula quanto tempo os efeitos visuais (VFX, dano, spawns) deste snapshot precisam para terminar
        const vfxDuration = this.calculateVfxDuration(nextSnapshot, phaseChanged);

        // 2. Renderiza o estado da mesa e dispara os VFX primeiro (adiando o banner de fase se houver)
        this.scene.renderSnapshot(nextSnapshot, isLastInQueue, {
            deferPhaseAnnouncement: shouldShowPhaseBanner,
            previousPhase: prevPhase
        });

        // Se o snapshot abriu um modal de escolha de alvo para o jogador local, pausa a fila até ele escolher
        if (isLastInQueue && nextSnapshot.pendingChoice) {
            const isMyChoice = String(nextSnapshot.pendingChoice.choosingPlayerId) === String(this.scene.localPlayerId);
            if (isMyChoice) {
                this.isPlaying = false;
                this.scene.updateMainButtonState();
                return;
            }
        }

        // 3. Se houve mudança para um Novo Turno ou Fase Chave, espera os VFX terminarem ANTES de tocar a faixa!
        if (shouldShowPhaseBanner) {
            this.activeTimer = this.scene.time.delayedCall(vfxDuration, () => {
                this.scene.triggerDeferredPhaseAnnouncement(nextSnapshot, prevPhase);

                // Aguarda a faixa terminar de passar na tela antes de puxar o próximo snapshot
                this.activeTimer = this.scene.time.delayedCall(this.PACING.CUTIN_DURATION, () => {
                    this.playNext();
                });
            });
        } else {
            // Sem faixa de turno: apenas aguarda o tempo natural da animação + respiro
            this.activeTimer = this.scene.time.delayedCall(vfxDuration, () => {
                this.playNext();
            });
        }
    }

    /**
     * Evita spam de faixas na tela em turnos curtos!
     * Só exibe a faixa cinética nas fases de decisão ou no início real dos combates/resoluções.
     */
    shouldAnnouncePhaseChange(prevPhase, nextPhase, snapshot) {
        if (!prevPhase || prevPhase === nextPhase) return false;

        // Sempre anuncia quando um Novo Turno começa (volta para CHOICE) ou em fases onde o jogador age
        if (['CHOICE', 'PREPARATION', 'DISCARD', 'GAME_OVER'].includes(nextPhase)) {
            return true;
        }

        // Nas fases automáticas (COMBAT_START, RESOLUTION, COMBAT, COMBAT_END),
        // consolida em um único anúncio no início da Resolução/Combate para não piscar 4 faixas seguidas
        if (nextPhase === 'RESOLUTION') {
            const localCards = snapshot.localPlayer?.playedCardsInResolve?.length || 0;
            const oppCards = snapshot.opponentPlayer?.playedCardsInResolve?.length || 0;
            return (localCards + oppCards) > 0; // Só anuncia resolução se houver cartas na pilha!
        }

        if (nextPhase === 'COMBAT') {
            return true; // Anuncia o embate físico entre os heróis ativos
        }

        // Ignora faixas para micro-fases de transição (COMBAT_START e COMBAT_END)
        return false;
    }

    /**
     * Define a duração de cada passo de acordo com a complexidade visual do que aconteceu.
     */
    calculateVfxDuration(snapshot, phaseChanged) {
        const eventType = snapshot.triggerContext?.eventType || '';
        let duration = this.PACING.BASE_STEP;

        switch (eventType) {
            case 'CARD_LIBERATED':
                duration = this.PACING.CARD_LIBERATED;
                break;
            case 'HERO_EVOLVED':
                duration = this.PACING.HERO_EVOLVED;
                break;
            case 'CARD_SPAWNED':
            case 'TOKEN_SUMMONED':
                duration = this.PACING.CARD_SPAWNED;
                break;
            case 'EFFECT_RESOLVED':
            case 'MISSION_PROGRESS':
                duration = this.PACING.EFFECT_RESOLVED;
                break;
            case 'PHYSICAL_ATTACK':
            case 'MAGIC_DAMAGE':
            case 'DIRECT_DAMAGE':
            case 'HEAL':
            case 'HERO_DIED':
                duration = this.PACING.COMBAT_ATTACK;
                break;
            default:
                // Se for um passo de COMBAT ou RESOLUTION mesmo sem tag específica, dá mais tempo
                if (['RESOLUTION', 'COMBAT', 'COMBAT_START', 'COMBAT_END'].includes(snapshot.turnPhase)) {
                    duration = 1350;
                } else {
                    duration = 850;
                }
                break;
        }

        // Adiciona um respiro extra quando a fase está prestes a mudar,
        // para o jogador absorver o último dano/efeito antes da faixa de novo turno entrar
        if (phaseChanged) {
            duration += this.PACING.PHASE_BREATH_BUFFER;
        }

        return duration;
    }

    clearQueue() {
        this.queue = [];
        this.isPlaying = false;
        if (this.activeTimer) {
            this.activeTimer.remove(false);
            this.activeTimer = null;
        }
    }
}

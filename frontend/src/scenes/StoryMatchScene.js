import MatchScene from './MatchScene.js';
import StoryDialogueOverlay, { COLOR_WING_THEMES } from '../ui/StoryDialogueOverlay.js';
import { BOARD_LAYOUT } from '../config/BoardCoordinates.js';
import { API_BASE_URL } from '../config/apiConfig.js';

export default class StoryMatchScene extends MatchScene {
    constructor() {
        super();
        // Sobrescreve a key da cena no Phaser mantendo toda a herança de MatchScene
        this.sys.settings.key = 'StoryMatchScene';
        this.missionData = null;
        this.storyOverlay = null;
        this.triggeredScriptKeys = new Set();
        this.epilogueShown = false;
    }

    init(data) {
        super.init(data);
        this.missionData = data.missionData || {
            id: data.missionId || 'mission_red_01',
            titulo: 'O Primeiro Clarão da Forja',
            cor: 'RED',
            capitulo: 1,
            epilogo: 'Com o constructo silenciado, a Poeira de fogo assentou revelando o segredo da forja.',
            scriptedEvents: []
        };
        this.triggeredScriptKeys = new Set();
        this.epilogueShown = false;
    }

    create() {
        super.create();

        this.storyOverlay = new StoryDialogueOverlay(this);
        this.createStoryHeaderBadge();

        // Renomeia o botão de "Menu Principal" do GameOverModal para "Voltar aos Arquivos"
        if (this.playAgainBtnTxt) {
            this.playAgainBtnTxt.setText('🔄 REPETIR MISSÃO');
        }
    }

    createStoryHeaderBadge() {
        const theme = COLOR_WING_THEMES[this.missionData?.cor] || COLOR_WING_THEMES.WHITE;
        const centerX = BOARD_LAYOUT.ARENA_CIRCLE.x;

        this.storyMissionBadge = this.add.container(centerX, 42).setDepth(205);
        const bg = this.add.rectangle(0, 0, 560, 32, 0x07090e, 0.92)
            .setStrokeStyle(1.5, theme.hex);

        const txt = this.add.text(0, 0, `📜 CAPÍTULO ${this.missionData.capitulo || 1}: ${this.missionData.titulo.toUpperCase()}`, {
            fontFamily: 'IoskeleyMono, monospace',
            fontSize: '13px',
            color: theme.textHex,
            fontStyle: 'bold'
        }).setOrigin(0.5).setResolution(2);

        this.storyMissionBadge.add([bg, txt]);
    }

    /**
     * Sobrescreve renderSnapshot de MatchScene para interceptar os diálogos scriptados
     * enviados pelo backend (ou avaliados localmente em modo offline).
     */
    renderSnapshot(snapshot, isLastInQueue = true, options = {}) {
        super.renderSnapshot(snapshot, isLastInQueue, options);

        if (!snapshot || !this.storyOverlay) return;

        // 1. Consome pendingDialogues gerados diretamente pelo GameEngine (Backend)
        const backendDialogues = snapshot.pendingDialogues || snapshot.triggerContext?.pendingDialogues || [];
        if (backendDialogues.length > 0) {
            this.storyOverlay.enqueueInCombatDialogues(backendDialogues, this.missionData?.cor);
        } else {
            // 2. Fallback para avaliar scriptedEvents localmente caso esteja em simulação offline
            this.evaluateClientSideFallbackTriggers(snapshot);
        }
    }

    evaluateClientSideFallbackTriggers(snapshot) {
        const events = this.missionData?.scriptedEvents || [];
        if (events.length === 0) return;

        const allHeroes = [
            ...(snapshot.localPlayer?.heroes || []),
            ...(snapshot.opponentPlayer?.heroes || [])
        ];
        const allResolveCards = [
            ...(snapshot.localPlayer?.playedCardsInResolve || []),
            ...(snapshot.opponentPlayer?.playedCardsInResolve || [])
        ];

        events.forEach((ev, idx) => {
            const eventKey = `${ev.trigger}_${ev.targetHeroName || ''}_${ev.targetCardName || ''}_${ev.turnNumber || idx}`;
            if (this.triggeredScriptKeys.has(eventKey)) return;

            if (ev.trigger === 'ON_CARD_PLAYED' && ev.targetCardName) {
                const playedMatch = allResolveCards.some(
                    c => c && c.faceUp !== false &&
                    String(c.cardName).toLowerCase() === String(ev.targetCardName).toLowerCase()
                );
                if (playedMatch) {
                    this.triggeredScriptKeys.add(eventKey);
                    this.storyOverlay.enqueueInCombatDialogues([ev], this.missionData?.cor);
                }
            } else if (ev.trigger === 'ON_HERO_DEATH' && ev.targetHeroName) {
                const deadMatch = allHeroes.some(
                    h => h && h.estaVivo === false &&
                    String(h.cardName).toLowerCase() === String(ev.targetHeroName).toLowerCase()
                );
                if (deadMatch) {
                    this.triggeredScriptKeys.add(eventKey);
                    this.storyOverlay.enqueueInCombatDialogues([ev], this.missionData?.cor);
                }
            } else if (ev.trigger === 'ON_LOW_HP' && ev.targetHeroName) {
                const lowHpMatch = allHeroes.some(
                    h => h && h.estaVivo &&
                    String(h.cardName).toLowerCase() === String(ev.targetHeroName).toLowerCase() &&
                    (h.vidaAtual <= (h.vidaMaxima / 2))
                );
                if (lowHpMatch) {
                    this.triggeredScriptKeys.add(eventKey);
                    this.storyOverlay.enqueueInCombatDialogues([ev], this.missionData?.cor);
                }
            } else if (ev.trigger === 'ON_TURN_START' && ev.turnNumber) {
                const currentTurn = snapshot.turnNumber || snapshot.currentTurn || 1;
                if (snapshot.turnPhase === 'CHOICE' && currentTurn === Number(ev.turnNumber)) {
                    this.triggeredScriptKeys.add(eventKey);
                    this.storyOverlay.enqueueInCombatDialogues([ev], this.missionData?.cor);
                }
            }
        });
    }

    /**
     * Intercepta o fim de jogo: se o jogador venceu a missão, exibe primeiro o Epílogo Narrativo
     * e registra a conclusão da missão nos Arquivos.
     */
    showGameOverModal(snapshot) {
        const result = snapshot?.matchResult || {};
        const winnerId = result.winnerPlayerId || snapshot?.winnerPlayerId;
        const isLocalWinner = winnerId && String(winnerId) === String(this.localPlayerId);

        if (isLocalWinner && this.missionData?.epilogo && !this.epilogueShown) {
            this.epilogueShown = true;
            this.markMissionCompleted(this.missionData.id);

            this.storyOverlay.showFullNarrativeModal({
                headerTag: 'MEMÓRIA SINCRONIZADA // EPÍLOGO DESBLOQUEADO',
                title: this.missionData.titulo,
                colorKey: this.missionData.cor,
                narrativeText: this.missionData.epilogo,
                confirmLabel: 'VER RELATÓRIO DE BATALHA',
                onConfirm: () => {
                    super.showGameOverModal(snapshot);
                }
            });
            return;
        }

        super.showGameOverModal(snapshot);
    }

    async markMissionCompleted(missionId) {
        try {
            const stored = JSON.parse(localStorage.getItem('lendas_player')) || {};
            if (!Array.isArray(stored.completedMissions)) stored.completedMissions = [];
            if (!stored.completedMissions.includes(missionId)) {
                stored.completedMissions.push(missionId);
                localStorage.setItem('lendas_player', JSON.stringify(stored));
            }
            if (this.localPlayerId) {
                await fetch(`${API_BASE_URL}/api/story/missions/${missionId}/complete?playerId=${this.localPlayerId}`, {
                    method: 'POST'
                });
            }
        } catch (e) {
            // Silencioso se offline
        }
    }

    async onPlayAgainClicked() {
        if (this.isSubmittingAction) return;
        this.isSubmittingAction = true;
        this.playAgainBtnTxt.setText('REINICIANDO MEMÓRIA...');

        try {
            const res = await fetch(`${API_BASE_URL}/api/story/missions/${this.missionData.id}/start`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    playerId: this.localPlayerId,
                    playerName: this.currentSnapshot?.localPlayer?.name || 'Viajante da Poeira'
                })
            });

            if (res.ok) {
                const data = await res.json();
                this.isSubmittingAction = false;
                this.scene.restart({
                    matchId: data.matchId,
                    localPlayerId: data.localPlayerId || this.localPlayerId,
                    initialSnapshots: data.initialSnapshots || [],
                    missionData: data.mission || this.missionData
                });
                return;
            }
        } catch (e) {
            console.warn('Reiniciando missão em modo offline:', e);
        }

        this.isSubmittingAction = false;
        this.cleanupAndReturnToMenu();
    }

    cleanupAndReturnToMenu() {
        if (this.storyOverlay) {
            this.storyOverlay.destroy();
        }
        if (this.snapshotPlayer) {
            this.snapshotPlayer.clearQueue();
        }
        if (this.recapSidebar) {
            this.recapSidebar.destroy();
        }

        const storedPlayer = JSON.parse(localStorage.getItem('lendas_player')) || {
            uuid: this.localPlayerId,
            name: 'Viajante da Poeira'
        };

        // No Modo História, retorna diretamente para a sala "Grandes Arquivos da Poeira"
        this.scene.start('StoryArchiveScene', {
            player: storedPlayer,
            selectedColor: this.missionData?.cor || 'RED'
        });
    }
}

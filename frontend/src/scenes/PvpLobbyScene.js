import Phaser from 'phaser';
import DeckRestrictionValidator from '../managers/DeckRestrictionValidator.js';

export default class PvpLobbyScene extends Phaser.Scene {
    constructor() {
        super({ key: 'PvpLobbyScene' });
        this.pollLobbyTimer = null;
        this.isInQueue = false;
        this.lobbyPlayers = [];
        this.incomingChallenges = [];
        this.rowObjects = [];
        this.isTransitioning = false;
    }

    init(data) {
        this.player = data?.player || JSON.parse(localStorage.getItem('lendas_player'));
        if (!this.player.decks) this.player.decks = [];
        this.isInQueue = false;
        this.isTransitioning = false;
    }

    create() {
        const { width, height } = this.scale;

        // Fundo e Cabeçalho do Salão
        this.add.rectangle(width / 2, 65, 1760, 80, 0x0f172a, 0.95).setStrokeStyle(2, 0x334155);

        const permElo = this.player.permanentElo ?? 1000;
        const activeDeckName = this.player.currentDeck?.deckName || 'Deck Inicial Padrão';

        this.add.text(90, 44, `🏛️ SALÃO DE PARTIDAS (LOBBY PVP)`, {
            fontSize: '24px', color: '#ffd700', fontStyle: 'bold'
        });

        this.add.text(90, 74, `👤 ${this.player.name}   |   🏅 ELO Permanente: ${permElo}   |   🃏 Baralho: ${activeDeckName}`, {
            fontSize: '14px', color: '#38bdf8', fontStyle: 'bold', fontFamily: 'monospace'
        });

        // Botão: Voltar ao Menu Principal
        const backBtn = this.add.rectangle(1680, 65, 220, 48, 0x475569)
            .setStrokeStyle(2, 0x94a3b8)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => this.onLeaveLobbyClicked());
        this.add.text(1680, 65, '⬅ VOLTAR AO MENU', {
            fontSize: '14px', color: '#ffffff', fontStyle: 'bold'
        }).setOrigin(0.5);
        backBtn.on('pointerover', () => backBtn.setFillStyle(0x64748b));
        backBtn.on('pointerout', () => backBtn.setFillStyle(0x475569));

        // Banner de Notificação de Desafios Recebidos
        this.notificationBannerBg = this.add.rectangle(width / 2, 135, 1760, 44, 0x7f1d1d, 0.95)
            .setStrokeStyle(2, 0xf87171)
            .setVisible(false);
        this.notificationBannerTxt = this.add.text(width / 2, 135, '', {
            fontSize: '16px', color: '#fef08a', fontStyle: 'bold'
        }).setOrigin(0.5).setVisible(false);

        // Painel Principal da Lista de Jogadores no Salão
        this.add.rectangle(width / 2, 520, 1460, 660, 0x0f172a, 0.90)
            .setStrokeStyle(2, 0x334155);

        // Cabeçalho da Tabela
        this.add.rectangle(width / 2, 225, 1420, 46, 0x1e293b)
            .setStrokeStyle(1, 0x475569);

        this.add.text(280, 225, 'JOGADOR NO SALÃO', {
            fontSize: '15px', color: '#94a3b8', fontStyle: 'bold'
        }).setOrigin(0, 0.5);

        this.add.text(850, 225, 'ELO PERMANENTE', {
            fontSize: '15px', color: '#94a3b8', fontStyle: 'bold'
        }).setOrigin(0.5, 0.5);

        this.add.text(1150, 225, 'STATUS', {
            fontSize: '15px', color: '#94a3b8', fontStyle: 'bold'
        }).setOrigin(0.5, 0.5);

        this.add.text(1480, 225, 'AÇÃO DIRETA', {
            fontSize: '15px', color: '#94a3b8', fontStyle: 'bold'
        }).setOrigin(0.5, 0.5);

        this.rowsContainer = this.add.container(0, 0);

        // Barra Inferior de Controles e Matchmaking Automático
        this.statusText = this.add.text(width / 2, 885, 'Conectando ao Salão Comunitário...', {
            fontSize: '16px', color: '#38bdf8', fontStyle: 'bold'
        }).setOrigin(0.5);

        this.findMatchBtnBg = this.add.rectangle(width / 2, 945, 460, 64, 0xb91c1c)
            .setStrokeStyle(3, 0xd4af37)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => this.onFindMatchToggleClicked());

        this.findMatchBtnTxt = this.add.text(width / 2, 945, '⚔️ ACHAR PARTIDA (FILA AUTOMÁTICA)', {
            fontSize: '17px', color: '#ffffff', fontStyle: 'bold'
        }).setOrigin(0.5);

        this.findMatchBtnBg.on('pointerover', () => this.findMatchBtnBg.setScale(1.02));
        this.findMatchBtnBg.on('pointerout', () => this.findMatchBtnBg.setScale(1.0));

        // Registra presença inicial no salão e inicia o polling em tempo real
        this.enterLobbyOnServer();

        this.pollLobbyTimer = this.time.addEvent({
            delay: 1500,
            loop: true,
            callback: () => this.pollLobbyState()
        });

        this.events.once('shutdown', () => this.cleanupTimers());
    }

    cleanupTimers() {
        if (this.pollLobbyTimer) {
            this.pollLobbyTimer.remove(false);
            this.pollLobbyTimer = null;
        }
    }

    async enterLobbyOnServer() {
        try {
            const res = await fetch('http://localhost:8080/api/lobby/join', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    playerId: this.player.uuid,
                    playerName: this.player.name,
                    currentDeck: this.player.currentDeck
                })
            });

            if (!res.ok) throw new Error('Falha ao entrar no salão');
            const state = await res.json();
            this.applyLobbyState(state);
        } catch (err) {
            console.error('Erro ao entrar no salão PvP:', err);
            this.statusText.setText('❌ Erro ao conectar ao Salão PvP (Servidor offline)').setColor('#f87171');
        }
    }

    async pollLobbyState() {
        if (this.isTransitioning) return;
        try {
            const res = await fetch(`http://localhost:8080/api/lobby/state?playerId=${this.player.uuid}`);
            if (!res.ok) return;
            const state = await res.json();
            this.applyLobbyState(state);
        } catch (err) {
            console.error('Erro ao atualizar estado do salão:', err);
        }
    }

    applyLobbyState(state) {
        if (!state || this.isTransitioning) return;

        // Se uma partida foi iniciada (por desafio aceito ou matchmaking automático), redireciona!
        if (state.status === 'MATCH_FOUND' && state.activeMatch) {
            this.startPvpMatch(state.activeMatch);
            return;
        }

        this.isInQueue = (state.status === 'IN_QUEUE');
        this.updateQueueButtonVisuals();

        this.lobbyPlayers = state.players || [];
        this.incomingChallenges = state.incomingChallenges || [];

        // Atualiza banner de notificação de convites de duelo
        if (this.incomingChallenges.length > 0) {
            const latest = this.incomingChallenges[0];
            this.notificationBannerBg.setVisible(true);
            this.notificationBannerTxt
                .setText(`🔥 DESAFIO RECEBIDO! ${latest.challengerName} (ELO ${latest.challengerElo}) convidou você para um combate! Clique em "ACEITAR COMBATE" na lista.`)
                .setVisible(true);
        } else {
            this.notificationBannerBg.setVisible(false);
            this.notificationBannerTxt.setVisible(false);
        }

        if (!this.isInQueue) {
            const count = this.lobbyPlayers.length;
            this.statusText.setText(`🟢 Conectado ao Salão — ${count} jogador(es) online no salão.`).setColor('#4ade80');
        }

        this.renderPlayerRows();
    }

    renderPlayerRows() {
        this.rowsContainer.removeAll(true);

        const startY = 280;
        const rowHeight = 58;
        const maxRows = 9;

        const visiblePlayers = this.lobbyPlayers.slice(0, maxRows);

        if (visiblePlayers.length === 0) {
            const emptyTxt = this.add.text(this.scale.width / 2, 500, 'Nenhum jogador no salão no momento...', {
                fontSize: '18px', color: '#64748b'
            }).setOrigin(0.5);
            this.rowsContainer.add(emptyTxt);
            return;
        }

        visiblePlayers.forEach((p, index) => {
            const y = startY + index * rowHeight;
            const isSelf = p.playerId === this.player.uuid;
            const receivedChallenge = p.challengeStatus === 'CHALLENGE_RECEIVED';
            const sentChallenge = p.challengeStatus === 'CHALLENGE_SENT';

            const rowColor = receivedChallenge
                ? 0x3f1d2e
                : (isSelf ? 0x1e3a5f : (index % 2 === 0 ? 0x162032 : 0x1b283d));

            const strokeColor = receivedChallenge ? 0xfacc15 : (isSelf ? 0x38bdf8 : 0x334155);

            const bg = this.add.rectangle(this.scale.width / 2, y, 1420, 50, rowColor)
                .setStrokeStyle(receivedChallenge ? 2 : 1, strokeColor);
            this.rowsContainer.add(bg);

            // Nome do jogador + indicação se é você ou se desafiou você
            let nameLabel = isSelf ? `⭐ ${p.playerName} (Você)` : `👤 ${p.playerName}`;
            if (receivedChallenge) {
                nameLabel = `⚔️ ${p.playerName}  [DESAFIOU VOCÊ!]`;
            }

            const nameTxt = this.add.text(280, y, nameLabel, {
                fontSize: '16px',
                color: receivedChallenge ? '#fef08a' : (isSelf ? '#38bdf8' : '#ffffff'),
                fontStyle: 'bold'
            }).setOrigin(0, 0.5);
            this.rowsContainer.add(nameTxt);

            // ELO Permanente
            const eloTxt = this.add.text(850, y, `🏅 ${p.permanentElo ?? 1000}`, {
                fontSize: '16px',
                color: '#ffd700',
                fontStyle: 'bold',
                fontFamily: 'monospace'
            }).setOrigin(0.5, 0.5);
            this.rowsContainer.add(eloTxt);

            // Status (No Salão / Buscando Fila)
            const statusLabel = p.inQueue ? '🔍 Na Fila' : '🟢 No Salão';
            const statusColor = p.inQueue ? '#fbbf24' : '#4ade80';
            const stTxt = this.add.text(1150, y, statusLabel, {
                fontSize: '14px',
                color: statusColor,
                fontStyle: 'bold'
            }).setOrigin(0.5, 0.5);
            this.rowsContainer.add(stTxt);

            // Coluna de Ações Contextuais
            if (isSelf) {
                const selfTag = this.add.text(1480, y, '—', {
                    fontSize: '15px', color: '#64748b'
                }).setOrigin(0.5, 0.5);
                this.rowsContainer.add(selfTag);
            } else if (receivedChallenge && p.challengeId) {
                // Botão "Aceitar Combate" em destaque
                const acceptBg = this.add.rectangle(1480, y, 210, 38, 0x16a34a)
                    .setStrokeStyle(2, 0xfef08a)
                    .setInteractive({ useHandCursor: true })
                    .on('pointerdown', () => this.onAcceptChallengeClicked(p.challengeId));

                const acceptTxt = this.add.text(1480, y, '⚡ ACEITAR COMBATE', {
                    fontSize: '13px', color: '#ffffff', fontStyle: 'bold'
                }).setOrigin(0.5, 0.5);

                acceptBg.on('pointerover', () => acceptBg.setFillStyle(0x22c55e));
                acceptBg.on('pointerout', () => acceptBg.setFillStyle(0x16a34a));

                this.rowsContainer.add([acceptBg, acceptTxt]);
            } else if (sentChallenge) {
                const pendingBg = this.add.rectangle(1480, y, 210, 38, 0x334155)
                    .setStrokeStyle(1, 0x94a3b8)
                    .setInteractive({ useHandCursor: true })
                    .on('pointerdown', () => this.onCancelChallengeClicked(p.challengeId));

                const pendingTxt = this.add.text(1480, y, '⏳ DESAFIO ENVIADO (X)', {
                    fontSize: '12px', color: '#fbbf24', fontStyle: 'bold'
                }).setOrigin(0.5, 0.5);

                this.rowsContainer.add([pendingBg, pendingTxt]);
            } else {
                // Botão "Desafiar"
                const challengeBg = this.add.rectangle(1480, y, 180, 38, 0x1d4ed8)
                    .setStrokeStyle(2, 0x60a5fa)
                    .setInteractive({ useHandCursor: true })
                    .on('pointerdown', () => this.onSendChallengeClicked(p.playerId, p.playerName));

                const challengeTxt = this.add.text(1480, y, '🧤 DESAFIAR', {
                    fontSize: '13px', color: '#ffffff', fontStyle: 'bold'
                }).setOrigin(0.5, 0.5);

                challengeBg.on('pointerover', () => challengeBg.setFillStyle(0x2563eb));
                challengeBg.on('pointerout', () => challengeBg.setFillStyle(0x1d4ed8));

                this.rowsContainer.add([challengeBg, challengeTxt]);
            }
        });
    }

    async onSendChallengeClicked(targetPlayerId, targetPlayerName) {
        try {
            this.statusText.setText(`📨 Enviando convite de duelo para ${targetPlayerName}...`).setColor('#38bdf8');
            const res = await fetch('http://localhost:8080/api/lobby/challenge', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    challengerId: this.player.uuid,
                    challengerName: this.player.name,
                    challengerDeck: this.player.currentDeck,
                    targetPlayerId: targetPlayerId
                })
            });

            if (!res.ok) throw new Error('Não foi possível desafiar o jogador');
            await this.pollLobbyState();
        } catch (err) {
            console.error('Erro ao enviar desafio:', err);
            this.statusText.setText('⚠️ Não foi possível enviar o desafio para este jogador.').setColor('#f87171');
        }
    }

    async onAcceptChallengeClicked(challengeId) {
        if (this.isTransitioning) return;
        try {
            this.statusText.setText('⚔️ Aceitando desafio e preparando a arena...').setColor('#ffd700');
            const res = await fetch(`http://localhost:8080/api/lobby/challenge/${challengeId}/accept`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    challengeId: challengeId,
                    targetPlayerId: this.player.uuid,
                    targetPlayerName: this.player.name,
                    targetDeck: this.player.currentDeck
                })
            });

            if (!res.ok) throw new Error('Desafio expirado ou indisponível');
            const matchData = await res.json();
            if (matchData.status === 'MATCH_FOUND') {
                this.startPvpMatch(matchData);
            }
        } catch (err) {
            console.error('Erro ao aceitar desafio:', err);
            this.statusText.setText('⚠️ Este desafio expirou ou foi cancelado pelo desafiante.').setColor('#f87171');
            await this.pollLobbyState();
        }
    }

    async onCancelChallengeClicked(challengeId) {
        if (!challengeId) return;
        try {
            await fetch(`http://localhost:8080/api/lobby/challenge/${challengeId}/cancel?playerId=${this.player.uuid}`, {
                method: 'POST'
            });
            await this.pollLobbyState();
        } catch (err) {
            console.error('Erro ao cancelar desafio:', err);
        }
    }

    async onFindMatchToggleClicked() {
        if (this.isTransitioning) return;

        if (!this.isInQueue) {
            this.statusText.setText('🔍 Buscando oponente na fila de pareamento automático...').setColor('#fbbf24');
            try {
                const res = await fetch('http://localhost:8080/api/matchmaking/join', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        playerId: this.player.uuid,
                        playerName: this.player.name,
                        currentDeck: this.player.currentDeck
                    })
                });

                if (!res.ok) throw new Error('Erro ao entrar na fila');
                const data = await res.json();
                if (data.status === 'MATCH_FOUND') {
                    this.startPvpMatch(data);
                } else {
                    this.isInQueue = true;
                    this.updateQueueButtonVisuals();
                    await this.pollLobbyState();
                }
            } catch (err) {
                console.error('Erro ao entrar na fila:', err);
                this.statusText.setText('❌ Falha ao entrar na fila de pareamento.').setColor('#f87171');
            }
        } else {
            try {
                await fetch(`http://localhost:8080/api/matchmaking/leave?playerId=${this.player.uuid}`, {
                    method: 'POST'
                });
                this.isInQueue = false;
                this.updateQueueButtonVisuals();
                await this.pollLobbyState();
            } catch (err) {
                console.error('Erro ao sair da fila:', err);
            }
        }
    }

    updateQueueButtonVisuals() {
        if (this.isInQueue) {
            this.findMatchBtnBg.setFillStyle(0xd97706);
            this.findMatchBtnTxt.setText('⏳ BUSCANDO PARTIDA... (CLIQUE P/ CANCELAR)');
            this.statusText.setText('🔍 Você está na fila automática! Você também pode desafiar ou aceitar duelos na lista acima.').setColor('#fbbf24');
        } else {
            this.findMatchBtnBg.setFillStyle(0xb91c1c);
            this.findMatchBtnTxt.setText('⚔️ ACHAR PARTIDA (FILA AUTOMÁTICA)');
        }
    }

    async onLeaveLobbyClicked() {
        this.isTransitioning = true;
        this.cleanupTimers();
        try {
            await fetch(`http://localhost:8080/api/lobby/leave?playerId=${this.player.uuid}`, {
                method: 'POST'
            });
        } catch (e) {
            // Ignora erro se servidor estiver offline
        }
        this.scene.start('MainMenuScene', { player: this.player });
    }

    startPvpMatch(matchData) {
        if (this.isTransitioning) return;
        this.isTransitioning = true;
        this.cleanupTimers();

        this.scene.start('MatchScene', {
            matchId: matchData.matchId,
            localPlayerId: matchData.localPlayerId || this.player.uuid,
            initialSnapshots: matchData.initialSnapshots || matchData.snapshots
        });
    }
}

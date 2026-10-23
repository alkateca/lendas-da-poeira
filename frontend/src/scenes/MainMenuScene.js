import Phaser from 'phaser';
import DeckRestrictionValidator from '../managers/DeckRestrictionValidator.js';
import { DEMO_MATCH_UUID, INITIAL_MATCH_SNAPSHOT } from '../config/mockSnapshots.js';

export default class MainMenuScene extends Phaser.Scene {
    constructor() {
        super({ key: 'MainMenuScene' });
        this.isSearchingMatch = false;
        this.pollQueueTimer = null;
    }

    init(data) {
        this.player = data?.player || JSON.parse(localStorage.getItem('lendas_player'));
        if (!this.player.decks) this.player.decks = [];
        this.autoStartPve = !!data?.autoStartPve;
        this.autoSearchPvp = !!data?.autoSearchPvp;
    }

    create() {
        const { width, height } = this.scale;

        // Painel Superior de Perfil e Seletor de Baralho
        this.add.rectangle(width / 2, 65, 1760, 80, 0x0f172a, 0.95).setStrokeStyle(2, 0x334155);

        const permElo = this.player.permanentElo ?? 1000;
        const weekElo = this.player.weeklyElo ?? 1000;

        this.playerNameText = this.add.text(110, 42, `👤 Jogador: ${this.player.name}`, {
            fontSize: '19px', color: '#ffffff', fontStyle: 'bold'
        });
        this.playerEloText = this.add.text(110, 66, `🏅 ELO: ${permElo}  |  📅 Semanal: ${weekElo}`, {
            fontSize: '13px', color: '#38bdf8', fontStyle: 'bold', fontFamily: 'monospace'
        });
        this.playerUuidText = this.add.text(110, 86, `UUID: ${this.player.uuid}`, {
            fontSize: '11px', color: '#64748b'
        });

        this.syncPlayerProfileFromServer();

        // Botão para alternar o Baralho Escolhido no Menu Principal
        this.deckSelectorBg = this.add.rectangle(1420, 65, 480, 50, 0x1e293b)
            .setStrokeStyle(2, 0xd4af37)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => this.cycleActiveDeck());

        this.deckSelectorTxt = this.add.text(1420, 65, '', {
            fontSize: '14px', color: '#ffd700', fontStyle: 'bold'
        }).setOrigin(0.5);

        this.refreshSelectedDeckLabel();

        // Botão Trocar de Conta / Logout
        this.add.rectangle(1755, 65, 140, 44, 0x475569)
            .setStrokeStyle(1, 0x94a3b8)
            .setInteractive({ useHandCursor: true })
            .on('pointerdown', () => this.scene.start('LoginScene'));
        this.add.text(1755, 65, 'SAIR / LOGIN', { fontSize: '12px', color: '#ffffff', fontStyle: 'bold' }).setOrigin(0.5);

        // Título Central
        this.add.text(width / 2, height / 2 - 170, 'LENDAS DA POEIRA', {
            fontSize: '42px', color: '#ffd700', fontStyle: 'bold'
        }).setOrigin(0.5);

        this.statusText = this.add.text(width / 2, height / 2 - 115, 'Escolha um modo de jogo para iniciar', {
            fontSize: '16px', color: '#94a3b8'
        }).setOrigin(0.5);

        // Botão 1: Partida contra a Máquina (PvE)
        this.createButton(width / 2, height / 2 - 30, '🤖 PARTIDA CONTRA A MÁQUINA (PvE)', 0x1d4ed8, () => {
            this.startMatchAgainstMachine();
        });

        // Botão 2: PvP
        this.createButton(width / 2, height / 2 + 65, '⚔️ PVP (JOGADOR VS JOGADOR)', 0xb91c1c, () => {
            this.onPvpButtonClicked();
        });

        // Botão 3: Montar Baralho
        this.createButton(width / 2, height / 2 + 160, '🃏 MONTAR BARALHO (DECKBUILDER)', 0xca8a04, () => {
            this.scene.start('DeckBuilderScene', { player: this.player });
        });

        if (this.autoStartPve) {
            this.time.delayedCall(200, () => this.startMatchAgainstMachine());
        } else if (this.autoSearchPvp) {
            this.time.delayedCall(200, () => this.onPvpButtonClicked());
        }
    }

    refreshSelectedDeckLabel() {
        const active = this.player.currentDeck;
        if (!active) {
            this.deckSelectorTxt.setText('🃏 Baralho Ativo: Deck Inicial Padrão (Clique p/ trocar)');
            this.deckSelectorTxt.setColor('#38bdf8');
            return;
        }
        const ready = DeckRestrictionValidator.isDeckReadyForMatch(active);
        const statusIcon = ready ? '✅' : '⚠️ Incompleto';
        this.deckSelectorTxt.setText(`🃏 Deck: ${active.deckName} (${active.cardList?.length || 0}/30) ${statusIcon}`);
        this.deckSelectorTxt.setColor(ready ? '#4ade80' : '#fbbf24');
    }

    async cycleActiveDeck() {
        if (!this.player.decks || this.player.decks.length === 0) {
            this.statusText.setText('Ainda não tem baralhos guardados! Clique em MONTAR BARALHO.').setColor('#fbbf24');
            return;
        }

        const currentUuid = this.player.currentDeck?.uuid;
        const idx = this.player.decks.findIndex(d => d.uuid === currentUuid);
        const nextDeck = this.player.decks[(idx + 1) % this.player.decks.length];

        this.player.currentDeck = nextDeck;
        localStorage.setItem('lendas_player', JSON.stringify(this.player));
        this.refreshSelectedDeckLabel();

        if (DeckRestrictionValidator.isDeckReadyForMatch(nextDeck)) {
            try {
                await fetch(`http://localhost:8080/api/players/${this.player.uuid}/active-deck/${nextDeck.uuid}`, {
                    method: 'PUT'
                });
            } catch (e) {
                // Silencioso se offline
            }
        }
    }

    async startMatchAgainstMachine() {
        const activeDeck = this.player.currentDeck;

        if (activeDeck && !DeckRestrictionValidator.isDeckReadyForMatch(activeDeck)) {
            this.statusText.setText(
                `⚠️ O baralho "${activeDeck.deckName}" está incompleto (${activeDeck.heroList?.length || 0}/3 Heróis, ${activeDeck.cardList?.length || 0}/30 Cartas) e está indisponível para jogar!`
            ).setColor('#f87171');
            return;
        }

        this.statusText.setText('A sortear baralho da máquina e a preparar a arena...').setColor('#ffd700');

        try {
            const res = await fetch('http://localhost:8080/api/matches/start-pve', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    playerId: this.player.uuid,
                    playerName: this.player.name,
                    currentDeck: activeDeck
                })
            });

            if (!res.ok) throw new Error('Falha ao iniciar partida PvE');

            const data = await res.json();
            this.scene.start('MatchScene', {
                matchId: data.matchId || DEMO_MATCH_UUID,
                localPlayerId: data.localPlayerId,
                initialSnapshots: data.initialSnapshots
            });
        } catch (e) {
            console.warn('Servidor offline. Iniciando partida com Mock Snapshot...', e);
            const offlineSnap = JSON.parse(JSON.stringify(INITIAL_MATCH_SNAPSHOT));
            offlineSnap.activeTurnPlayerId = this.player.uuid;
            offlineSnap.localPlayer.playerId = this.player.uuid;
            this.scene.start('MatchScene', {
                matchId: DEMO_MATCH_UUID,
                localPlayerId: this.player.uuid,
                initialSnapshots: [offlineSnap]
            });
        }
    }

    createButton(x, y, label, color, callback) {
        const bg = this.add.rectangle(x, y, 500, 72, color)
            .setStrokeStyle(3, 0xd4af37)
            .setInteractive({ useHandCursor: true });

        this.add.text(x, y, label, {
            fontSize: '17px', color: '#ffffff', fontStyle: 'bold'
        }).setOrigin(0.5);

        bg.on('pointerover', () => bg.setScale(1.03));
        bg.on('pointerout', () => bg.setScale(1.0));
        bg.on('pointerdown', callback);
    }

    onPvpButtonClicked() {
        const activeDeck = this.player.currentDeck;

        if (activeDeck && !DeckRestrictionValidator.isDeckReadyForMatch(activeDeck)) {
            this.statusText.setText(
                `⚠️ O baralho "${activeDeck.deckName}" está incompleto (${activeDeck.heroList?.length || 0}/3 Heróis, ${activeDeck.cardList?.length || 0}/30 Cartas) e está indisponível para jogar!`
            ).setColor('#f87171');
            return;
        }

        this.scene.start('PvpLobbyScene', { player: this.player });
    }

    async checkQueueStatus() {
        try {
            const res = await fetch(`http://localhost:8080/api/matchmaking/status?playerId=${this.player.uuid}`);
            if (!res.ok) return;

            const data = await res.json();

            if (data.status === 'MATCH_FOUND') {
                if (this.pollQueueTimer) {
                    this.pollQueueTimer.remove(false);
                    this.pollQueueTimer = null;
                }
                this.startMatchScene(data);
            }
        } catch (err) {
            console.error('Erro ao checar fila:', err);
        }
    }

    startMatchScene(matchData) {
        if (this.pollQueueTimer) {
            this.pollQueueTimer.remove(false);
            this.pollQueueTimer = null;
        }
        this.isSearchingMatch = false;

        this.scene.start('MatchScene', {
            matchId: matchData.matchId,
            localPlayerId: matchData.localPlayerId,
            initialSnapshots: matchData.initialSnapshots
        });
    }

    async syncPlayerProfileFromServer() {
        if (!this.player?.uuid) return;
        try {
            const res = await fetch(`http://localhost:8080/api/players/${this.player.uuid}`);
            if (res.ok) {
                const fresh = await res.json();
                this.player.permanentElo = fresh.permanentElo ?? 1000;
                this.player.weeklyElo = fresh.weeklyElo ?? 1000;
                this.player.lastWeeklyResetWeek = fresh.lastWeeklyResetWeek;
                localStorage.setItem('lendas_player', JSON.stringify(this.player));
                if (this.playerEloText) {
                    this.playerEloText.setText(`🏅 ELO: ${this.player.permanentElo}  |  📅 Semanal: ${this.player.weeklyElo}`);
                }
            }
        } catch (e) {
            // Silencioso se offline
        }
    }
}

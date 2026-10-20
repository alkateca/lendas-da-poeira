export default class SnapshotPlayerManager {
    constructor(matchScene) {
        this.scene = matchScene;
        this.queue = [];
        this.isPlaying = false;
        this.activeTimer = null;
        this.lastPlayedSequence = 0;
    }

    clearQueue() {
        this.queue = [];
        this.isPlaying = false;
        if (this.activeTimer) {
            this.activeTimer.remove(false);
            this.activeTimer = null;
        }
    }

    enqueueSnapshots(snapshotsArray) {
        if (!snapshotsArray || snapshotsArray.length === 0) {
            if (this.queue.length === 0 && !this.activeTimer) {
                this.isPlaying = false;
                if (this.scene && typeof this.scene.updateMainButtonState === 'function') {
                    this.scene.updateMainButtonState();
                }
            }
            return;
        }

        const queuedSequences = new Set(
            this.queue
                .map(s => s?.snapshotSequence)
                .filter(seq => typeof seq === 'number' && seq > 1)
        );

        const validSnapshots = snapshotsArray.filter(snap => {
            if (!snap) return false;
            const seq = snap.snapshotSequence;
            if (typeof seq === 'number' && seq > 1) {
                if (seq <= this.lastPlayedSequence || queuedSequences.has(seq)) {
                    return false;
                }
                queuedSequences.add(seq);
            }
            return true;
        });

        if (validSnapshots.length === 0) {
            if (this.queue.length === 0 && !this.activeTimer) {
                this.isPlaying = false;
                if (this.scene && typeof this.scene.updateMainButtonState === 'function') {
                    this.scene.updateMainButtonState();
                }
            }
            return;
        }

        this.queue.push(...validSnapshots);

        if (!this.isPlaying) {
            this.playNext();
        }
    }

    playNext() {
        if (this.activeTimer) {
            this.activeTimer.remove(false);
            this.activeTimer = null;
        }

        if (this.queue.length === 0) {
            this.isPlaying = false;
            if (this.scene && typeof this.scene.updateMainButtonState === 'function') {
                this.scene.updateMainButtonState();
            }
            return;
        }

        const currentSnapshot = this.queue.shift();
        const isLastInQueue = this.queue.length === 0;
        const seq = currentSnapshot?.snapshotSequence;
        if (typeof seq === 'number' && seq > 1) {
            this.lastPlayedSequence = Math.max(this.lastPlayedSequence, seq);
        }

        const pausesForChoice = isLastInQueue && Boolean(currentSnapshot?.pendingChoice);
        this.isPlaying = !pausesForChoice;

        try {
            if (this.scene && typeof this.scene.renderSnapshot === 'function') {
                this.scene.renderSnapshot(currentSnapshot, isLastInQueue);
            }
        } catch (err) {
            console.error('Erro ao renderizar Snapshot:', err);
        }

        if (pausesForChoice) {
            if (this.scene && typeof this.scene.updateMainButtonState === 'function') {
                this.scene.updateMainButtonState();
            }
            return;
        }

        const eventType = currentSnapshot?.triggerContext?.eventType;
        const delay = (eventType === 'EFFECT_RESOLVED' || eventType === 'PHYSICAL_COMBAT_EXCHANGE')
            ? 900
            : 450;

        this.activeTimer = this.scene.time.delayedCall(delay, () => {
            this.activeTimer = null;
            this.playNext();
        });
    }
}
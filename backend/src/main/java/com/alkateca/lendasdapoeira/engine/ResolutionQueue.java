package com.alkateca.lendasdapoeira.engine;

import com.alkateca.lendasdapoeira.effects.Effect;
import com.alkateca.lendasdapoeira.entity.Card;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.LinkedList;

public class ResolutionQueue {

    private LinkedList<PendingEffect> queue = new LinkedList<>();

    @Data
    @AllArgsConstructor
    private static class PendingEffect {
        private Effect effect;
        private Card sourceCard;
    }

    public void enqueue(Effect effect, Card sourceCard) {
        queue.addLast(new PendingEffect(effect, sourceCard));
    }

    /**
     * Enfileira no início da fila (usado quando uma carta é jogada/invocada por outro efeito durante a resolução).
     */
    public void enqueueFirst(Effect effect, Card sourceCard) {
        queue.addFirst(new PendingEffect(effect, sourceCard));
    }

    public void resolveAll(GameEngine engine) {
        while (!queue.isEmpty()) {
            // Se o efeito anterior abriu uma escolha de alvo para o jogador, PAUSA a fila imediatamente!
            if (engine.isWaitingForPlayerChoice()) {
                return;
            }

            PendingEffect pending = queue.pollFirst();
            int historySizeBefore = engine.getSnapshotHistory().size();
            pending.getEffect().resolve(engine, pending.getSourceCard());
            engine.evaluateInstantDeathByAura();

            // Captura foto do efeito resolvido (ou do pedido de escolha aberto)
            if (engine.isWaitingForPlayerChoice()) {
                engine.captureSnapshot(pending.getSourceCard(), pending.getEffect(), "CHOICE_REQUESTED");
            } else if (engine.getSnapshotHistory().size() == historySizeBefore
                    && pending.getEffect().shouldCaptureSnapshot()) {
                engine.captureSnapshot(pending.getSourceCard(), pending.getEffect(), "EFFECT_RESOLVED");
            }
        }
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}

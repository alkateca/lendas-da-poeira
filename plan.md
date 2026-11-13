# Plan — Issue #49 (Front-end): Resolução Alternada Imediata + Cartas de Reação

> **Repositório:** `C:\Users\BARCA02\Desktop\lendas-da-poeira`
> **Branch:** `feat/issue-49-resolucao-imediata-reacao` (já existe, já com o back-end concluído)
> **Issue:** https://github.com/alkateca/lendas-da-poeira/issues/49
> **Escopo deste plano:** SOMENTE `frontend/`. O back-end está pronto (140/140 testes passando) — **não alterar o back-end**.

> [!IMPORTANT]
> Leia antes de começar: `GEMINI.md` (raiz), `backend/game_architecture.md`, `backend/cardRules.md`, `backend/effectRules.md` e, se existir, `frontendRules.md`.
> O GameEngine (back-end) é a única fonte da verdade. O front-end **não calcula** regra de jogo; apenas reflete o snapshot e bloqueia interações óbvias para UX.

---

## 1. Regras de negócio (já implementadas no back-end)

1. **CHOICE** — inalterada (jogador com iniciativa escolhe herói aliado + herói inimigo).
2. **PREPARATION** — existe apenas **antes da primeira ação** do combate. Ao entrar nela, `resolutionActivePlayerId = activeTurnPlayerId`.
3. Na **primeira ação** (jogar carta ou passar) em PREPARATION, o servidor executa `COMBAT_START` uma vez e muda para **RESOLUTION**.
4. **RESOLUTION alternada:** quem tem a vez (`resolutionActivePlayerId`) joga **1 carta** (revelada e resolvida **imediatamente**) **ou passa a janela**. Depois a vez vai para o oponente. Alterna até **2 janelas por jogador** (`maxCardsThisTurn`, padrão 2).
5. **Passar** consome só a janela atual (`resolutionWindowsUsed += 1`). Quem passou na 1ª janela ainda pode jogar na 2ª.
6. Quando os dois jogadores esgotam as janelas, o servidor segue sozinho: `COMBAT → COMBAT_END → DISCARD` (ou `GAME_OVER`).
7. **Cartas de Reação (`cardType: "REACTION"`)**:
   - **Não** podem ser jogadas numa janela normal.
   - São oferecidas fora do turno, quando o oponente joga uma carta compatível, via `pendingChoice` com `targetType: "REACTION_WINDOW"` e `canSkip: true`.
   - Resolvem antes da carta que as disparou (LIFO) e **não consomem** as 2 janelas/cartas do turno.
   - **Prompt de ativação:** o jogador só recebe o prompt quando tem na mão pelo menos 1 Reação que **pode** ser ativada contra aquela carta específica (o filtro é feito no servidor). Sem Reação válida, não há prompt e o jogo segue direto.
   - **Resposta a certas cartas:** cada Reação declara a quais cartas/efeitos responde (`ReactionEffect.canReact(...)` no back-end). O front **não** decide elegibilidade; apenas exibe os UUIDs de `validTargetUuids`.
   - **Ativação antes da carta e Negação de Ativação:** a Reação ativa **antes** que os efeitos da carta inimiga resolvam. Isso permite que efeitos de reação (ex.: `ReactionCancelTriggeringCardEffect`) **neguem e cancelem a ativação da carta**, descartando-a sem que nenhum efeito dela seja disparado.
   - **Encadeamento imediato:** a ativação da carta inimiga abre o prompt. Se o jogador reagir, os efeitos da Reação resolvem **logo em sequência**, antes dos efeitos da carta que a disparou. Caso a carta inimiga seja negada/anulada pela Reação, sua execução é interrompida imediatamente. Se não for negada, os efeitos dela continuam após a resolução da Reação.
   - Cada carta inimiga abre **no máximo 1** prompt de Reação por combate (`reactionPromptedCardIdsInCurrentCombat`).
8. **Timeout entre cada jogada de carta (PvP):**
   - O timer de 30 segundos (com contagem regressiva nos 10s finais) corre individualmente para **cada janela de ação** (`resolutionActivePlayerId`).
   - O timeout pode ocorrer entre a jogada de cada carta: se o jogador da vez não jogar 1 carta nem passar a janela dentro do limite, o timeout é disparado contra ele.

   Sequência completa de snapshots que o front recebe:

   ```mermaid
   sequenceDiagram
       participant A as Jogador da vez
       participant S as Servidor (GameEngine)
       participant D as Defensor
       A->>S: confirm-action [cardX]
       S-->>A: CARD_PLAYED_IMMEDIATE (cardX revelada)
       S->>S: Defensor tem Reação válida contra cardX?
       alt Sim
           S-->>D: pendingChoice REACTION_WINDOW (sourceCard = cardX)
           S-->>A: aguardando (Oponente pode reagir...)
           alt Defensor reage
               D->>S: confirm-action selectedTargetUuid = reaçãoY
               S-->>A: REACTION_PLAYED (reaçãoY entra na pilha)
               S-->>A: EFFECT_RESOLVED (efeitos de reaçãoY, em sequência)
               S-->>A: EFFECT_RESOLVED (efeitos de cardX, se não cancelada)
           else Defensor não reage
               D->>S: confirm-action selectedTargetUuid = null
               S-->>A: EFFECT_RESOLVED (efeitos de cardX)
           end
       else Não
           S-->>A: EFFECT_RESOLVED (efeitos de cardX)
       end
       S-->>A: vez passa (resolutionActivePlayerId muda)
   ```

---

## 2. Contrato do back-end que o front-end deve consumir

### 2.1 Novos campos do snapshot (`MatchSnapshotDTO`)

| Campo | Onde | Significado |
|---|---|---|
| `resolutionActivePlayerId` | raiz do snapshot | UUID de quem tem a vez de jogar/passar em PREPARATION/RESOLUTION. Fallback: `activeTurnPlayerId`. |
| `cardsPlayedThisTurn` | `localPlayer` / `opponentPlayer` | Cartas **normais** jogadas no turno (reações NÃO contam). Zera no fim do COMBAT_END. |
| `resolutionWindowsUsed` | `localPlayer` / `opponentPlayer` | Janelas consumidas (jogando ou passando). |
| `maxCardsThisTurn` | `localPlayer` / `opponentPlayer` | Limite de janelas/cartas no turno (padrão 2). |
| `reactionCard` | cada carta (`CardSnapshotDTO`) | `true` se a carta for de Reação. |
| `cardType` | cada carta | Agora pode ser `"REACTION"`. |

### 2.2 Novos `triggerContext.eventType`

| eventType | Quando | Campos úteis |
|---|---|---|
| `CARD_PLAYED_IMMEDIATE` | Um jogador jogou 1 carta na janela dele (antes de ela resolver) | `sourceCardUuid`, `sourceCardName`. **`logMessage` vem genérico (`"Fase atual: RESOLUTION"`)** → o front deve montar um texto próprio. |
| `RESOLUTION_WINDOW_PASSED` | Um jogador passou a janela | Sem carta. `logMessage` genérico → o front monta o texto. Para saber quem passou: é o `resolutionActivePlayerId` do snapshot **anterior**. |
| `REACTION_PLAYED` | Uma Carta de Reação foi ativada | `spawnedCardUuid`, `spawnedCardName`, `spawnedCardData`, `ownerPlayerId`, `targetSlotIndex`, `logMessage` (já vem pronto: `"⚡ Reação ativada: X (em resposta a Y)"`). |

### 2.3 Janela de Reação (`pendingChoice`)

```json
{
  "choosingPlayerId": "<uuid do defensor>",
  "sourceCardUuid": "<uuid da carta inimiga que disparou a reação>",
  "sourceCardName": "<nome da carta inimiga>",
  "targetType": "REACTION_WINDOW",
  "promptMessage": "Reação disponível contra 'X'! Escolha uma Carta de Reação da sua mão para ativar ou pule.",
  "validTargetUuids": ["<uuid de carta REACTION na mão do localPlayer>", "..."],
  "canSkip": true
}
```

- Os UUIDs válidos estão em `snapshot.localPlayer.hand`, então o modal atual (`checkAndShowPendingChoiceModal`) já consegue montar as cartas.
- Resposta: o mesmo endpoint de escolha de alvo (`submitEffectTargetChoice`) → `selectedTargetUuid = <uuid>` para reagir ou `null` para não reagir.

### 2.4 Endpoint de ação (sem mudança de rota)

`POST /api/matches/{matchId}/confirm-action` com o payload que já existe (`SelectionManager.buildActionRequestDTO`):

- Em `PREPARATION` ou `RESOLUTION`, o servidor chama `engine.submitPreparationCards(playerId, playedCardUuids)`:
  - `playedCardUuids = []` → **passa a janela**.
  - `playedCardUuids = [uuid]` → **joga e ativa** essa carta imediatamente.
  - **Envie no máximo 1 UUID.** Mais de 1 entra num caminho legado; não use.
- Erros possíveis (HTTP != 2xx, `errBody.error`): não é sua vez, limite atingido, carta inválida/restrita. O `onMainButtonClicked` já mostra o toast.
- `waitingForOpponent` na resposta = `true` quando a vez passou para o oponente.

---

## 3. Mudanças por arquivo

> Convenção usada abaixo (repetida em vários pontos). Crie um helper na `MatchScene` e reutilize:
>
> ```js
> isLocalResolutionTurn(snapshot = this.currentSnapshot) {
>     if (!snapshot) return false;
>     const actor = snapshot.resolutionActivePlayerId || snapshot.activeTurnPlayerId;
>     return String(actor) === String(this.localPlayerId);
> }
>
> isCardPlayPhase(phase = this.currentSnapshot?.turnPhase) {
>     return phase === 'PREPARATION' || phase === 'RESOLUTION';
> }
> ```

---

### 3.1 `frontend/src/managers/SelectionManager.js`

**a) `tryAssignCardToResolveSlot(cardUuid, targetSlotIndex, hasInitiative)`** — reescrever o início do método:

1. Se `!this.scene.isLocalResolutionTurn()` → `this.scene.showToast('⚠️ Aguarde sua janela de resolução para jogar uma carta!')` e `return false`.
2. Buscar `cardData` em `localPlayer.hand` (como já faz). Se `String(cardData?.cardType || '').toUpperCase() === 'REACTION' || cardData?.reactionCard === true` → toast `'⚠️ Cartas de Reação só podem ser ativadas em resposta a um efeito inimigo!'` e `return false`.
3. Se `(localPlayer.resolutionWindowsUsed ?? 0) >= (localPlayer.maxCardsThisTurn ?? 2)` → toast `'⚠️ Você já usou todas as janelas deste turno!'` e `return false`.
4. **Slot fixo da janela** (ignora `targetSlotIndex`, que serve só para o drop ter funcionado):
   ```js
   const allowedIndices = this.getLocalPlayerAllowedResolveIndices(hasInitiative); // [0,2] ou [1,3]
   const playedCount = Math.min(1, localPlayer.cardsPlayedThisTurn ?? 0);
   const finalSlotIndex = allowedIndices[playedCount];
   ```
   > Use `cardsPlayedThisTurn` (e não `resolutionWindowsUsed`) para o slot bater com o que `syncInterleavedResolveZone` desenha depois (ele posiciona pela ordem das cartas jogadas, não pela janela).
5. Manter as validações de `DeckRestrictionValidator.canHeroPlayCard` e de empunhadura. No cálculo de empunhadura, `pendingWeaponSlots` passa a ser sempre 0 (só existe 1 carta preparada por vez); pode simplificar.
6. **Uma carta por janela:** antes de `set`, `this.playedCardSlotMap.clear();` e depois `this.playedCardSlotMap.set(cardUuid, finalSlotIndex); return true;`.
7. Remover a lógica antiga de "achar o primeiro slot vermelho livre / substituir o 2º slot".

**b) `canConfirmAction(currentPhase, isMyInitiative)`**:

```js
case 'PREPARATION':
case 'RESOLUTION':
    return this.scene.isLocalResolutionTurn();   // 0 cartas = passar janela, 1 carta = ativar
```

**c) `buildActionRequestDTO`** — garantir no máximo 1 UUID:

```js
playedCardUuids: this.getOrderedPlayedCardUuids().slice(0, 1),
```

**d) Atualizar o JSDoc** de `getLocalPlayerAllowedResolveIndices`: o índice 0 do array é o slot da 1ª carta do jogador e o índice 1 é o da 2ª.

---

### 3.2 `frontend/src/scenes/MatchScene.js`

Adicionar os helpers `isLocalResolutionTurn()` e `isCardPlayPhase()` (seção 3) perto de `isCurrentMatchPvp()` (linha ~595).

**a) `doesLocalPlayerNeedToAct()`** (linha ~604):

```js
if (phase === 'PREPARATION' || phase === 'RESOLUTION') {
    return this.isLocalResolutionTurn();
}
if (phase === 'DISCARD') {
    return !this.isWaitingOpponentConfirmation;
}
```

**b) `updateTurnTimerTick()`** (linha ~620) — **Timeout entre cada jogada de carta**:
- `isInteractivePhase = Boolean(pendingChoice) || ['CHOICE', 'PREPARATION', 'RESOLUTION', 'DISCARD'].includes(phase);`
- Em `PREPARATION` e `RESOLUTION`, a cada janela de jogada de carta o timer de 30s é reiniciado para o jogador ativo (`resolutionActivePlayerId`).
- A chave do timer reinicia a cada troca de vez/jogador:
  ```js
  const targetTimerKey = pendingChoice
      ? `PENDING:${this.currentSnapshot.snapshotSequence || 0}:${pendingChoice.choosingPlayerId}`
      : `${phase}:${this.currentSnapshot.resolutionActivePlayerId || ''}:${this.phaseTimerInstanceId}`;
  ```
- O timeout de 30s pode ocorrer entre a jogada de cada carta: se o jogador da vez não agir dentro dos 30s, o timeout é disparado (`triggerPhaseTimeout()`).

**c) `canDragHandCard()`** (linha ~1406):

```js
const phase = this.currentSnapshot.turnPhase;
if (this.isCardPlayPhase(phase)) return this.isLocalResolutionTurn() && !this.currentSnapshot.pendingChoice;
return phase === 'DISCARD';
```

**d) `checkMagneticHoverOnSlots(cardPrefab)`** (linha ~1514): trocar a guarda `turnPhase !== 'PREPARATION'` por `if (!this.isCardPlayPhase() || !this.isLocalResolutionTurn()) return;`.

**e) `handleHandCardClickSnap(cardPrefab)`** (linha ~1530) e **`handleHandCardDrop(cardPrefab)`** (linha ~1549): trocar `if (phase === 'PREPARATION')` por `if (this.isCardPlayPhase(phase))`. No drop, o slot escolhido pelo jogador pode ser repassado, mas o `SelectionManager` decide o slot final (3.1-a-4).

**f) `rearrangePreparationCards()`** — sem mudança lógica (pode renomear para `rearrangeStagedResolutionCard`; se renomear, atualize as 2 chamadas).

**g) `renderSnapshot(snapshot, ...)`** (linha ~1625) — resetar seleção quando a **vez** muda, não só quando a fase muda:

```js
const previousActor = this.currentSnapshot?.resolutionActivePlayerId ?? null;
// ... depois de this.currentSnapshot = snapshot;
const actorChanged = previousActor !== (snapshot.resolutionActivePlayerId ?? null);
if (previousPhase !== snapshot.turnPhase || actorChanged) {
    this.selectionManager.resetSelections();
    this.isWaitingOpponentConfirmation = false;
    this.phaseTimerInstanceId = (this.phaseTimerInstanceId || 0) + 1;
}
```

Também na mesma função:
- Tratar `eventType === 'CARD_PLAYED_IMMEDIATE'`: `this.highlightSourceCard(trigger.sourceCardUuid)` e `combatLogText`. Se o dono for local → `🃏 Você jogou ${sourceCardName}`; senão → `🃏 Oponente jogou ${sourceCardName}`. Dono local = `(localPlayer.playedCardsInResolve || []).some(c => c.uuid === trigger.sourceCardUuid)`.
- Tratar `eventType === 'RESOLUTION_WINDOW_PASSED'`: `combatLogText` = `⏭️ ${quem} passou a janela`. Use o `resolutionActivePlayerId` **anterior** (`previousActor`) para saber quem passou.
- Tratar `eventType === 'REACTION_PLAYED'`: reutilizar `this.handleCardSpawnEvent(snapshot.triggerContext, localHasInitiative)`, porque o payload tem `spawnedCardUuid`/`spawnedCardData` e a animação de entrada serve. Depois aplique um destaque extra (ex.: `spawnedPrefab.bg.setStrokeStyle(4, 0xffe600)`) e `this.showToast('⚡ REAÇÃO!')`. **Basta adicionar `'REACTION_PLAYED'` na condição do `if (eventType === 'CARD_SPAWNED' || eventType === 'TOKEN_SUMMONED')`.**
- Depois do log do trigger, se `this.isCardPlayPhase(snapshot.turnPhase) && !snapshot.pendingChoice`, mostrar uma linha de status da janela no `combatLogText`, a menos que o trigger tenha mensagem específica:
  - Sua vez: `🟥 SUA JANELA (${used + 1}/${max}) — jogue 1 carta ou passe.`
  - Vez do oponente: `🟦 Janela do oponente (${oppUsed + 1}/${oppMax})...`

**h) `triggerDeferredPhaseAnnouncement`** (linha ~1758) — atualizar os textos:

```js
PREPARATION: { title: 'FASE DE PREPARAÇÃO', sub: localHasInitiative ? 'VOCÊ ABRE A RESOLUÇÃO' : 'OPONENTE ABRE A RESOLUÇÃO', color: 0x2563eb },
RESOLUTION:  { title: 'RESOLUÇÃO ALTERNADA', sub: 'CARTAS ATIVAM IMEDIATAMENTE • REAÇÕES LIBERADAS', color: 0xca8a04 },
```

**i) `checkAndShowPendingChoiceModal(snapshot)`** (linha ~1843). Para `REACTION_WINDOW`, use o prompt dedicado da **seção 3.9**. O restante deste item é o comportamento mínimo/fallback:
- `const isReactionWindow = pending.targetType === 'REACTION_WINDOW';`
- Título: `isReactionWindow ? '⚡ JANELA DE REAÇÃO — ' + promptMessage : '🎯 ' + promptMessage`.
- Botão de pular: texto `isReactionWindow ? '[ NÃO REAGIR / PASSAR ]' : '[ PULAR EFEITO ]'`.
- Quando **não** é a escolha do jogador local e é janela de reação: `combatLogText` = `'⏳ Oponente decidindo se reage...'` (no lugar de "escolher um alvo").
- Fora isso, nada muda: o clique em uma carta chama `submitEffectTargetChoice(uuid)` e o pular chama `submitEffectTargetChoice(null)`.

**j) `updateMainButtonState()`** (linha ~2375):

```js
const isMyResolutionTurn = this.isLocalResolutionTurn();
const lp = this.currentSnapshot.localPlayer || {};
const maxWindows = lp.maxCardsThisTurn ?? 2;
const usedWindows = Math.min(maxWindows, lp.resolutionWindowsUsed ?? 0);
const windowLabel = `${Math.min(usedWindows + 1, maxWindows)}/${maxWindows}`;
const cardPlayLabel = !isMyResolutionTurn
    ? 'OPONENTE JOGANDO...'
    : (prepCount === 0 ? `PULAR JANELA (${windowLabel})` : `ATIVAR CARTA (${windowLabel})`);

const labels = {
    CHOICE: ...,                     // inalterado
    PREPARATION: cardPlayLabel,
    RESOLUTION: cardPlayLabel,
    COMBAT_START: 'INÍCIO DO COMBATE...',
    COMBAT: 'COMBATE FÍSICO...',
    COMBAT_END: 'FIM DO COMBATE...',
    DISCARD: ...,                    // inalterado
    GAME_OVER: 'FIM DE JOGO!'
};

const isAutomaticPhase = ['COMBAT_START', 'COMBAT', 'COMBAT_END', 'GAME_OVER'].includes(phase); // REMOVER 'RESOLUTION'
```

Botão de pending choice: se `pendingChoice.targetType === 'REACTION_WINDOW'`, use os textos `'REAGIR? (VER MODAL)'` / `'OPONENTE PODE REAGIR...'`.

**k) `pollServerStateIfWaiting(force)`** (linha ~2593):

```js
const isAutomaticPhase = ['COMBAT_START', 'COMBAT', 'COMBAT_END'].includes(phase); // REMOVER 'RESOLUTION'
const waitingOpponentWindow = this.isCardPlayPhase(phase) && !this.isLocalResolutionTurn() && !this.currentSnapshot.pendingChoice;

const shouldPoll =
    force ||
    (phase === 'CHOICE' && !isMyInitiative) ||
    this.isWaitingOpponentConfirmation ||
    waitingOtherPlayerChoice ||
    waitingOpponentWindow ||
    (isAutomaticPhase && !waitingMyChoice);
```

**l) `syncInterleavedResolveZone`** (linha ~2106) — sem mudança obrigatória. O layout base `[first0, second0, first1, second1]` continua valendo, e o fallback `hasExtraCards` já cobre o caso de uma Reação deixar um jogador com mais de 2 cartas na pilha. Só atualize o comentário da linha ~2172 (`Cartas de PREPARATION` → `Cartas jogadas nas janelas de RESOLUTION`).

**m) Linha ~1688** — `if (['CHOICE', 'PREPARATION', 'DISCARD'].includes(snapshot.turnPhase)) this.combatStartSpawnedOrder = [];` — **manter**. Não incluir RESOLUTION, senão as cartas invocadas no COMBAT_START perdem a ordem fixa.

---

### 3.3 `frontend/src/managers/SnapshotPlayerManager.js`

**a) `playNext()`** — **sem mudança**. A pausa para `pendingChoice` local já cobre a janela de reação, e as janelas normais são liberadas quando a fila termina (`isPlaying = false`).

**b) `shouldAnnouncePhaseChange(prevPhase, nextPhase, snapshot)`**:
- RESOLUTION: anunciar **sempre** que a fase mudar para RESOLUTION (sai de COMBAT_START uma vez por turno), sem depender de haver cartas na pilha:
  ```js
  if (nextPhase === 'RESOLUTION') return true;
  ```

**c) `calculateVfxDuration(snapshot, phaseChanged)`** — adicionar casos:

```js
case 'CARD_PLAYED_IMMEDIATE':
    duration = 900;  // tempo de revelar a carta antes do efeito
    break;
case 'RESOLUTION_WINDOW_PASSED':
    duration = 600;
    break;
case 'REACTION_PLAYED':
    duration = this.PACING.CARD_SPAWNED;
    break;
```

---

### 3.4 `frontend/src/ui/MatchRecapSidebar.js`

**a) `phaseLabels`** (linha ~192):
- `PREPARATION: { label: 'FASE > PREPARAÇÃO (ABERTURA)', ... }`
- `RESOLUTION:  { label: 'FASE > RESOLUÇÃO ALTERNADA', ... }`

**b) Bloco "3. Cartas Programadas na Preparação"** (linha ~231-248) — **remover**. Ele assume cartas ocultas em lote, o que não existe mais.

**c) Novos eventos** na cadeia `if/else if` do item 5 (linha ~263):

```js
} else if (eventType === 'CARD_PLAYED_IMMEDIATE') {
    const isLocalCard = (local.playedCardsInResolve || []).some(c => c.uuid === trigger.sourceCardUuid);
    const srcDto = [...(local.playedCardsInResolve || []), ...(opp.playedCardsInResolve || [])]
        .find(c => c.uuid === trigger.sourceCardUuid);
    this.appendNode({
        title: `🃏 ${isLocalCard ? localName : oppName} jogou:`,
        lines: [`➔ [${trigger.sourceCardName || 'Carta'}]`],
        accent: isLocalCard ? '#e60012' : '#2563eb',
        previewDto: srcDto,
        isHero: false
    });
} else if (eventType === 'RESOLUTION_WINDOW_PASSED') {
    // quem passou = resolutionActivePlayerId do snapshot anterior -> guarde this.lastResolutionActor no fim de recordSnapshot
    const passedIsLocal = String(this.lastResolutionActor) === String(this.scene.localPlayerId);
    this.appendNode({
        title: `⏭️ ${passedIsLocal ? localName : oppName} passou a janela`,
        lines: [],
        accent: '#64748b'
    });
} else if (eventType === 'REACTION_PLAYED') {
    const ownerIsLocal = String(trigger.ownerPlayerId) === String(this.scene.localPlayerId);
    this.appendNode({
        title: `⚡ ${ownerIsLocal ? localName : oppName} REAGIU:`,
        lines: [trigger.logMessage || `➔ [${trigger.spawnedCardName || 'Reação'}]`],
        accent: '#ffe600',
        previewDto: trigger.spawnedCardData,
        isHero: false
    });
}
```

- No construtor: `this.lastResolutionActor = null;`.
- No **fim** de `recordSnapshot`: `this.lastResolutionActor = snapshot.resolutionActivePlayerId || snapshot.activeTurnPlayerId;`.

> Confirme se `appendNode` aceita `lines: []` sem quebrar. Se não aceitar, passe `lines: ['—']`.

---

### 3.5 `frontend/src/scenes/DeckBuilderScene.js`

No `<select id="db-filter-type">` (linha ~294-299), dentro de `<optgroup label="Tipos de Carta">`, adicionar:

```html
<option value="TYPE:REACTION">Reação</option>
```

O filtro `matchesActiveAttributeFilters` já compara `cardType` em maiúsculas, então funciona sem mais mudanças.

> Verifique também se `frontend/src/scenes/CollectionScene.js` tem um filtro de tipo equivalente (procure `TYPE:SPELL` ou `THEURGY`). Se tiver, adicione a mesma opção lá.

---

### 3.6 `frontend/src/config/keywordsConfig.js`

**a) `FASE.description`** (linha ~239):

```
'Todo turno segue as fases: 1. Escolha (CHOICE) → 2. Preparação (PREPARATION, só até a 1ª jogada) → 3. Início do Combate (COMBAT_START) → 4. Resolução Alternada (RESOLUTION: cada jogador joga 1 carta por vez, até 2, ativando imediatamente) → 5. Combate Físico (COMBAT) → 6. Final do Combate (COMBAT_END) → 7. Descarte (DISCARD).'
```

**b) `RESOLUCAO`** (linha ~241):
- `summary`: `'Jogadores alternam jogando 1 carta por vez; cada carta é ativada imediatamente.'`
- `description`: `'Na Resolução (TurnPhase.RESOLUTION), o jogador com iniciativa joga 1 carta (ou passa) e ela resolve na hora; em seguida o oponente faz o mesmo. Cada jogador tem até 2 janelas por turno. Equipamentos são anexados ao resolver.'`

**c) `INICIO_DO_COMBATE.description`** (linha ~47): trocar `"antes da revelação e resolução das cartas baixadas"` por `"disparado uma única vez, imediatamente antes da primeira carta (ou passe) da Resolução"`.

**d) `PRIORIDADE`** (linha ~76): a fila em lote não existe mais. Ajuste a descrição para: `'Ao ser jogada, resolve antes de qualquer efeito pendente na fila de Resolução.'` (ou deixe como está e registre na seção 5 como pendente de validação com o usuário).

**e) Nova keyword** em `KEYWORDS_REGISTRY` (categoria `ACAO_GATILHO`):

```js
REACAO: {
    id: 'REACAO',
    label: 'Reação',
    category: KEYWORD_CATEGORIES.ACAO_GATILHO,
    badgeColor: '#fde047',
    pattern: /\brea[çc][ãa]o\b/i,
    summary: 'Ativada fora do turno, em resposta a uma carta inimiga.',
    description: 'Cartas de Reação (REACTION) não podem ser jogadas nas janelas normais. Quando o oponente joga uma carta que satisfaz a condição da Reação, abre-se uma janela para ativá-la; ela resolve antes da carta inimiga e não consome as 2 cartas do turno.'
}
```

> `resolveCardKeywords` usa `cardDto.keywords` vindos do back-end quando existem. Se o back-end não enviar `REACAO`, o regex serve de fallback. Não altere o back-end.

---

### 3.7 `frontend/src/config/mockSnapshots.js` (modo offline)

Reescrever `generateOfflineMockTimeline` para simular o fluxo novo:

1. `INITIAL_MATCH_SNAPSHOT`:
   - Adicionar `resolutionActivePlayerId: LOCAL_PLAYER_UUID`.
   - Adicionar em `localPlayer` e `opponentPlayer`: `cardsPlayedThisTurn: 0, resolutionWindowsUsed: 0, maxCardsThisTurn: 2`.
   - (Opcional) Adicionar 1 carta `cardType: 'REACTION', reactionCard: true` na mão local para testar o bloqueio visual.
2. `payload.currentPhase === 'CHOICE'`: como hoje, mais `clone.resolutionActivePlayerId = clone.activeTurnPlayerId` e zerar os contadores dos dois jogadores.
3. `payload.currentPhase === 'PREPARATION' || 'RESOLUTION'` (jogada local):
   - `snapPlay`: `turnPhase = 'RESOLUTION'`. Se `payload.playedCardUuids[0]` existir, mova a carta da mão para `localPlayer.playedCardsInResolve`, faça `cardsPlayedThisTurn++` e use `triggerContext.eventType = 'CARD_PLAYED_IMMEDIATE'`; senão use `'RESOLUTION_WINDOW_PASSED'`. Em ambos os casos, `localPlayer.resolutionWindowsUsed++`.
   - `snapEffect` (se jogou carta): `+3 ataque` como hoje, `eventType = 'EFFECT_RESOLVED'`.
   - `snapOppPass`: o oponente simulado sempre passa: `opponentPlayer.resolutionWindowsUsed++`, `eventType = 'RESOLUTION_WINDOW_PASSED'`, `resolutionActivePlayerId = LOCAL_PLAYER_UUID`.
   - Se os dois jogadores chegaram a `resolutionWindowsUsed >= 2`: anexar as fotos `COMBAT` e `DISCARD` (reaproveitar o código atual de `snapCombat`/`snapEnd`, zerando `cardsPlayedThisTurn`/`resolutionWindowsUsed` no DISCARD).
   - Se não: retornar `[snapPlay, snapEffect?, snapOppPass]`, com o último em `turnPhase = 'RESOLUTION'` e `resolutionActivePlayerId = LOCAL_PLAYER_UUID`.
4. `payload.currentPhase === 'DISCARD'`: como hoje, mais `clone.resolutionActivePlayerId = clone.activeTurnPlayerId`.

> Ordem real do servidor: quem tem iniciativa joga primeiro. No mock offline, simplifique: o oponente simulado passa logo depois de cada janela local. Basta para testar a UI.

---

### 3.8 Arquivos que NÃO precisam mudar (já verificados)

- `frontend/src/managers/DeckRestrictionValidator.js`: carta `REACTION` passa pelas mesmas regras de cor/afinidade/classe/ordem/raça. Não há regra especial no back-end para Reação no deckbuilding.
- `frontend/src/prefabs/PlayableCardPrefab.js`: a única mudança obrigatória é o método `setReactionReady` (seção 3.9.4). O `typeBadge` já mostra `cardDto.cardType` (vai exibir `REACTION`), e o layout não-item já serve.
  - **Opcional:** se `cardDto.cardType === 'REACTION'`, mostrar `typeBadge` como `'REAÇÃO'` e pintar a borda de amarelo (`0xfde047`) em `updateCardVisuals`.
  - **Opcional:** em `resolveActivePhaseNodes`, `activePhaseKeys` pode conter `'REACTION'` (o back-end adiciona). Hoje é ignorado sem quebrar, então não precisa mexer.
- `frontend/src/ui/PhantomUIKit.js`: apenas comentário citando fases.

---

### 3.9 Prompt de Ativação de Reação (fluxo dedicado)

> **Objetivo:** quando uma Carta de Reação **puder** ser ativada, o jogador recebe um prompt claro e dedicado, que mostra **contra qual carta** está reagindo. Ao confirmar, os efeitos da Reação resolvem **imediatamente em sequência**, como resposta à ativação da carta inimiga.
> **Princípio:** o front só reage a `pendingChoice.targetType === 'REACTION_WINDOW'`. Toda elegibilidade vem do servidor (`validTargetUuids`). O front **nunca** decide se uma Reação pode ser usada.

#### 3.9.1 `MatchScene.js` — despacho por tipo de escolha (Strategy)

No início de `checkAndShowPendingChoiceModal(snapshot)`, depois de validar `pending` e `isMyChoice`:

```js
if (pending.targetType === 'REACTION_WINDOW') {
    this.showReactionPrompt(snapshot, pending);
    return;
}
// ... fluxo genérico de escolha de alvo continua igual
```

> Mantenha o modal genérico intacto para os outros `targetType`. Novos tipos de prompt devem seguir o mesmo padrão de despacho (Open/Closed).

#### 3.9.2 `MatchScene.js` — novo método `showReactionPrompt(snapshot, pending)`

Reutilize `this.modalContainer`, `this.modalTitle` e `this.modalCardsGroup` (mesmo container do modal atual) com outro layout:

1. **Título:** `this.modalTitle.setText('⚡ REAÇÃO DISPONÍVEL!')`, cor `#fde047`.
2. **Carta disparadora (lado esquerdo, x ≈ -420):**
   - Buscar o DTO por `pending.sourceCardUuid` em `snapshot.opponentPlayer.playedCardsInResolve`. Fallback: um DTO mínimo `{ uuid: pending.sourceCardUuid, cardName: pending.sourceCardName, faceUp: true, color: 'NEUTRAL' }`.
   - Renderizar com `new PlayableCardPrefab(this, x, y, { ...dto, faceUp: true }, false, 'MEDIUM', 0)` e `setShowFullText(true)` para o jogador ler o efeito.
   - Rótulo acima: `'CARTA INIMIGA ATIVADA'` (azul `#7dd3fc`).
3. **Conector (centro, x ≈ -230):** texto `'➜ EM RESPOSTA A'` / seta, em amarelo.
4. **Reações elegíveis (lado direito, a partir de x ≈ -60):**
   - Para cada `uuid` em `pending.validTargetUuids`, buscar o DTO em `snapshot.localPlayer.hand`.
   - Renderizar com `PlayableCardPrefab` (`setShowFullText(true)`) mais uma `clickZone` amarela (`0xfde047`, stroke 3) que chama `this.submitEffectTargetChoice(uuid)`.
   - Rótulo acima de cada uma: `'ATIVAR REAÇÃO'`.
5. **Botão de recusa (embaixo, y ≈ 295):** `'[ NÃO REAGIR / PASSAR ]'` → `this.submitEffectTargetChoice(null)`.
6. **Texto auxiliar** (y ≈ 245): `pending.promptMessage`.
7. `this.isChoiceModalActive = true; this.modalContainer.setVisible(true);`
8. **Salvaguarda:** se nenhum UUID de `validTargetUuids` for encontrado na mão (não deve acontecer), chame `this.submitEffectTargetChoice(null)` e retorne. Não mantenha o jogo travado.

> O botão `[FECHAR X]` do modal hoje só esconde o container. Para `REACTION_WINDOW`, o jogador pode fechar e reabrir pelo botão principal (`onMainButtonClicked` já reabre quando há `pendingChoice` local). Isso deve continuar funcionando.

#### 3.9.3 `MatchScene.js` — destaque das Reações na mão

Enquanto houver `REACTION_WINDOW` local:

- Em `renderSnapshot` (depois de `syncLocalHand`), chame `this.refreshReactionHandHighlights(snapshot)`:
  ```js
  refreshReactionHandHighlights(snapshot) {
      const pending = snapshot?.pendingChoice;
      const isMyReactionWindow = pending
          && pending.targetType === 'REACTION_WINDOW'
          && String(pending.choosingPlayerId) === String(this.localPlayerId);
      const eligible = new Set(isMyReactionWindow ? (pending.validTargetUuids || []) : []);

      this.handCardPrefabs.forEach(card => {
          card.setReactionReady(eligible.has(card.uuid));
      });
  }
  ```
- `submitEffectTargetChoice` já zera `pendingChoice` localmente. Depois disso, chame `this.refreshReactionHandHighlights(this.currentSnapshot)` para remover o brilho na hora.
- **Clique na mão:** em `handleHandCardClickSnap`, **antes** do tratamento de fase:
  ```js
  const pending = this.currentSnapshot?.pendingChoice;
  if (pending?.targetType === 'REACTION_WINDOW'
      && String(pending.choosingPlayerId) === String(this.localPlayerId)
      && (pending.validTargetUuids || []).includes(cardPrefab.uuid)) {
      this.submitEffectTargetChoice(cardPrefab.uuid);
      return;
  }
  ```
  > `canDragHandCard()` retorna `false` com `pendingChoice` ativo (3.2-c), mas o evento de clique vem do `dragend` do prefab. Garanta que, com `REACTION_WINDOW` local, o clique simples (sem arrasto) em uma carta elegível chegue a `handleHandCardClickSnap`. Ou libere `canDragHandCard()` apenas para cartas elegíveis, ou adicione um handler `pointerup` dedicado no prefab.

#### 3.9.4 `PlayableCardPrefab.js` — estado visual "Reação pronta"

Adicionar um método isolado (não mexe na lógica existente):

```js
setReactionReady(isReady) {
    if (this.isReactionReady === isReady) return;
    this.isReactionReady = isReady;

    if (this.reactionPulseTween) {
        this.reactionPulseTween.stop();
        this.reactionPulseTween = null;
    }

    if (isReady) {
        this.bg.setStrokeStyle(4, 0xfde047);
        this.reactionPulseTween = this.scene.tweens.add({
            targets: this.shadow,
            alpha: { from: 0.45, to: 1 },
            yoyo: true,
            repeat: -1,
            duration: 420
        });
        this.shadow.setFillStyle(0xfde047, 0.85);
    } else {
        const theme = getCardVisualTheme(this.cardData?.color, this.cardData?.rarity);
        this.bg.setStrokeStyle(2.5, theme.border);
        this.shadow.setFillStyle(0x090d16, 0.75).setAlpha(1);
    }
}
```

- No `destroy()`, parar `this.reactionPulseTween` se existir.
- **Atenção:** `snapToPosition` e os handlers de hover reescrevem `bg.setStrokeStyle` e `shadow`. Em `pointerout` e no fim de `snapToPosition`, se `this.isReactionReady`, reaplique a borda amarela (`this.bg.setStrokeStyle(4, 0xfde047)`).

#### 3.9.5 Lado do jogador que disparou (quem espera)

- `checkAndShowPendingChoiceModal`, quando `!isMyChoice && pending.targetType === 'REACTION_WINDOW'`: `combatLogText` = `'⏳ Oponente pode reagir a ' + (pending.sourceCardName || 'sua carta') + '...'`.
- Pulse a própria carta disparadora na pilha: `this.highlightSourceCard(pending.sourceCardUuid)`.
- `updateMainButtonState`: `'OPONENTE PODE REAGIR...'` (já previsto em 3.2-j).

#### 3.9.6 Encadeamento visual da resolução (Reação → carta disparadora)

O servidor já manda os snapshots na ordem certa: `REACTION_PLAYED` → `EFFECT_RESOLVED` da Reação → `EFFECT_RESOLVED` da carta disparadora (se não cancelada). O front só precisa deixar a sequência legível:

1. **`REACTION_PLAYED`** (já tratado em 3.2-g via `handleCardSpawnEvent`): a Reação entra na pilha com borda amarela.
   - **Extra:** desenhe um "raio" de conexão entre a Reação e a carta disparadora. Use `this.add.line` ou `graphics.lineBetween` do prefab da Reação até o prefab cuja `uuid === this.lastReactionTriggerUuid`, amarelo, espessura 4, `depth 150`, com fade-out em ~700ms.
   - Guarde o UUID disparador quando o prompt abrir: `this.lastReactionTriggerUuid = pending.sourceCardUuid` em `checkAndShowPendingChoiceModal`, para os dois lados.
   - Toast: `'⚡ REAÇÃO!'`.
2. **`EFFECT_RESOLVED` com `sourceCardUuid` = Reação:** o comportamento atual (`highlightSourceCard`) já pulsa a carta. Nada a fazer.
3. **Carta disparadora cancelada:** se o snapshot seguinte não trouxer a carta disparadora em `playedCardsInResolve` (foi para o descarte/exílio) e não houver `EFFECT_RESOLVED` dela, `syncInterleavedResolveZone` a remove naturalmente.
   - **Opcional:** antes de destruí-la, aplique tint vermelho com o texto `'ANULADA'` (detectar em `existingByUuid.forEach(leftover => ...)` quando `leftover.uuid === this.lastReactionTriggerUuid`).
4. **`SnapshotPlayerManager.calculateVfxDuration`:** `REACTION_PLAYED` já usa `PACING.CARD_SPAWNED` (3.3-c), o que dá tempo para ler a entrada da Reação antes dos efeitos.

#### 3.9.7 Timer do prompt de Reação (PvP)

> [!WARNING]
> Hoje o timeout de qualquer `pendingChoice` faz o servidor chamar `engine.timeoutMatch(choosingPlayerId)`, ou seja, **derrota por W.O.** Para um prompt opcional de Reação isso é punitivo demais. **Não alterar o back-end neste escopo**; resolver no front:

- Em `updateTurnTimerTick`, quando `remainingMs <= 0` e `pendingChoice?.targetType === 'REACTION_WINDOW'`:
  - Se for a escolha local: chame `this.submitEffectTargetChoice(null)` (**auto-passar**) em vez de `triggerPhaseTimeout()`.
  - Se **não** for a escolha local: **não** chame `triggerPhaseTimeout()`. Espere o auto-pass do outro cliente; continue o polling normalmente.
- Registre isto como pendência de back-end na seção 5 (timeout de `REACTION_WINDOW` deveria ser um auto-pass no servidor).

#### 3.9.8 Bot (PvE)

Nada a fazer no front. O `OpponentBotService` decide sozinho no servidor se reage. Se o **jogador** tiver Reação válida contra uma carta do bot, o prompt chega pelo polling/resposta normal e segue 3.9.1–3.9.3.

---

## 4. Verificação

1. `cd frontend; npm run build`: zero erros.
2. `cd frontend; npm test` (se houver script de teste): tudo passando. Se algum teste depender do fluxo antigo de PREPARATION em lote (2 cartas), atualize-o para o fluxo de 1 carta por janela.
3. Teste manual com o back-end rodando (`$env:JAVA_HOME = "C:\Users\BARCA02\.jdks\corretto-26.0.2.1"; cd backend; .\mvnw.cmd spring-boot:run`) e partida PvE:
   - [ ] CHOICE → PREPARATION: botão `PULAR JANELA (1/2)` se você tem iniciativa, ou `OPONENTE JOGANDO...` se não tem.
   - [ ] Arrastar ou clicar uma carta põe **só 1** no slot (o slot certo para a iniciativa). Clicar em outra **troca** a carta.
   - [ ] `ATIVAR CARTA (1/2)` → a carta é revelada e resolve na hora. A vez passa ao bot, ele joga ou passa, e a vez volta com `(2/2)`.
   - [ ] Carta `REACTION` na mão: clicar ou arrastar mostra o toast de bloqueio.
   - [ ] Quando o bot joga uma carta que dispara Reação: abre o prompt dedicado `⚡ REAÇÃO DISPONÍVEL!`, que mostra a **carta inimiga à esquerda** e as **Reações elegíveis à direita**, com `[ NÃO REAGIR / PASSAR ]`.
   - [ ] As Reações elegíveis **brilham na mão** enquanto o prompt está aberto. Clicar nelas na mão também ativa. O brilho some ao responder.
   - [ ] Sem Reação válida contra aquela carta: **nenhum** prompt aparece.
   - [ ] Ao reagir: `REACTION_PLAYED` (entrada amarela e conexão com a carta disparadora) → efeitos da Reação → efeitos da carta disparadora (ou carta anulada). O contador de janelas **não** muda.
   - [ ] Fechar o modal com `[FECHAR X]` e reabrir pelo botão principal funciona.
   - [ ] PvP: deixar o timer do prompt de Reação zerar = **auto-passar**, sem derrota por W.O.
   - [ ] Depois das 4 janelas: COMBAT → COMBAT_END → DISCARD automáticos.
   - [ ] PvP: o timer de 30s reinicia a cada troca de `resolutionActivePlayerId`, e o timeout é atribuído ao jogador da vez.
   - [ ] O log lateral (MatchRecapSidebar) mostra "jogou", "passou a janela" e "REAGIU".
   - [ ] DeckBuilder: o filtro "Reação" funciona.
4. Modo offline (back-end desligado): o fluxo simulado funciona sem erros no console.

---

## 5. Pontos em aberto (perguntar ao usuário se surgirem)

- Texto e semântica da keyword `PRIORIDADE` no fluxo novo (seção 3.6-d).
- Visual dedicado para cartas de Reação (moldura ou cor própria). Hoje está como opcional.
- Se uma carta normal jogada depois de um passe deve ocupar o slot da janela (posicional) em vez do slot pela ordem de cartas. O plano usa **ordem de cartas**, igual à renderização atual.
- **Back-end (fora deste escopo):** o timeout de `REACTION_WINDOW` hoje gera derrota por W.O. O ideal é o servidor tratar como auto-pass. O front contorna isso em 3.9.7. Sugerir uma issue separada via `gh issue create` se o usuário pedir.
- **Decisão do usuário já incorporada:**
  - *Timeout entre a jogada de cada carta:* Confirmado. Corre a cada janela de 30s para o jogador da vez em `PREPARATION` e `RESOLUTION`.
  - *Momento da Reação e Negação de Ativação:* Confirmado. A Reação ativa antes dos efeitos da carta disparadora, permitindo anular/negar a ativação dela (ex.: `ReactionCancelTriggeringCardEffect`).

---

## 6. Finalização

- Commits pequenos por arquivo/área, em PT-BR, ex.: `feat(front): janela única de resolução no SelectionManager (#49)`.
- **Não** fazer push nem abrir PR sem o usuário pedir.
- Não criar arquivos `ISSUE_*.md`. Issues só via `gh issue create` (regra do `GEMINI.md`).

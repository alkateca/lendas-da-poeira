# Diretrizes Globais do Agente (Guard Rails) - Front-End

1. **Obediência Cega aos Manuais e Fonte da Verdade:**
   - O Front-end (Phaser.js) é uma camada de visualização e interface: **NUNCA** calcule dano, resolução de combate ou lógica de vitória no cliente. O `GameEngine` (Back-end) é a única fonte da verdade.
   - Antes de implementar ou alterar componentes visuais, cenas ou prefabs, consulte obrigatoriamente [docs/frontendRules.md](file:///home/alkateca/Área de trabalho/lendas-da-poeira/frontend/docs/frontendRules.md).

2. **Respeito Estrito à Máquina de Estados (`TurnPhase`):**
   - Todas as animações, transições e o estado do **Botão Principal de Confirmação** são estritamente guiados pelo `TurnPhase` atual (`CHOICE`, `PREPARATION`, `COMBAT_START`, `RESOLUTION`, `COMBAT`, `COMBAT_END`, `DISCARD`).
   - Respeite a ordem dos gatilhos: revelação de cartas e execução sequencial de efeitos com animações em `TurnPhase.RESOLUTION`, choque físico em `TurnPhase.COMBAT`, checagem de morte e limpeza em `TurnPhase.COMBAT_END`.

3. **Fidelidade ao Layout de Zonas (`ZoneId`) e Visibilidade:**
   - Siga rigorosamente as coordenadas e slots definidos em `MatchScene` e `BoardCoordinates`:
     - Topo/Sub-topo: Oponente (`HAND`, `BENCH`).
     - Centro: `BATTLE` (Oponente), faixa horizontal de 4 slots `RESOLVE`, `BATTLE` (Local).
     - Base/Sub-base: Jogador Local (`BENCH`, `HAND`).
     - Laterais: `DECK`, `DISCARD`, `EXILE`.
   - Cartas do oponente na `HAND` e em `RESOLVE` durante `TurnPhase.PREPARATION` permanecem ocultas (dorso da carta).

4. **Contratos Visuais de Entidades (Prefabs & Containers):**
   - **Heróis:** Containers com HUD dinâmico (`vidaAtual`, `vidaMaxima`, `ataque`, `defesa`, `espirito`, `maxWeaponSlots`). A UI **deve** suportar valores negativos de `vidaAtual` durante o combate antes da checagem final.
   - **Morte do Herói:** Quando `estaVivo == false`, o Herói **NÃO** é removido de cena; permanece no slot da `BENCH` com tint escurecido (`0x555555`) e ícone de derrota.
   - **Itens e Anexações (`ZoneId.ATTACHED`):** Equipamentos resolvidos devem ser visualmente vinculados ao Herói ativo.

5. **Reatividade e Feedback Imediato:**
   - Não acumule atualizações visuais apenas para o fim do turno. Ao final de cada efeito individual na fila de resolução, atualize imediatamente os atributos dos Heróis na tela com feedback visual (números flutuantes / floating numbers).

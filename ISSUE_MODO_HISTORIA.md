# ISSUE: Modo História PvE — "Grandes Arquivos da Poeira"

**Status:** Aberta  
**Módulo:** Backend (Engine / Missões / DTOs) & Frontend (Phaser / UI de Diálogos / Hub)  
**Dependências:** `GameEngine`, `OpponentBotService`, `TurnTimelineResponseDTO`, `cards.json`, `heros.json`

---

## 1. Visão Geral

Implementar o **Modo História PvE** centralizado na sala temática **"Grandes Arquivos da Poeira"**. Nesse modo, o jogador acessa registros memoriais ancestrais para disputar missões guiadas por cor (Vermelho, Azul, Preto, Amarelo e Verde), conhecendo as origens do continente, a civilização goblin, a teocracia do Rei Imóvel, as fábricas do Rei Demônio e o culto ao Dragão de Cristal.

---

## 2. A Cor WHITE e os Inimigos dos Arquivos

A máquina utilizará primordialmente a cor **WHITE** para compor os desafios da IA:
* **Conceito Narrativo:** As cartas e heróis brancos representam **Ecos da Memória**, **Projeções de Poeira Pura** e o lendário **"Grupo dos Heróis do Passado"** arquivado nos registros.
* **Elenco Base Existente:**
  * `Alucinação Cintilante` (Constructo/Miragem dos registros corrompidos).
  * `Alka, Lutador do Grupo dos Heróis` (Combatente corpo-a-corpo ancestral).
  * `Leone, Clériga do Grupo dos Heróis` (Sustentação sagrada e punição por cura acumulada).
* **Função no Design:** A IA não joga com cores normais para não criar conflito temático com a cor ensinada; ela encarna os guardiões e testes memoriais dos Arquivos.

---

## 3. Estrutura Narrativa por Missão

Cada missão é governada por 3 camadas de texto:

1. **Prólogo (Pré-Partida):**
   * Contextualização histórica antes do início do jogo (tela dedicada com texto narrativo, ilustração e objetivos da missão).
2. **Eventos Scriptados In-Game (Durante a Batalha):**
   * Diálogos que disparam em momentos-chave da partida sem interromper as regras da física:
     * **Morte de Herói Específico:** Fala dramática ao abater o herói líder da máquina (ex: morte de Alka ou Leone).
     * **Jogada de Carta Específica:** Quando o jogador ou a IA joga uma carta-chave pela primeira vez (ex: primeira vez jogando *Bola de Fogo* na missão do Vermelho, gerando um comentário do mentor).
     * **Limiar de Vida / Turno Chave:** Mensagens ao atingir 50% de HP ou no início do Turno 3.
3. **Epílogo (Pós-Partida):**
   * Conclusão narrativa após a vitória (`GAME_OVER` com vitória do Player 1), revelando o registro histórico desbloqueado e recompensas.

---

## 4. Trilha de Capítulos ("Grandes Arquivos")

A sala conterá 5 alas memoriais:
1. **Ala da Forja e Cinzas (RED):** A história dos caçadores de bestas colossais e a purificação pelo fogo sagrado.
2. **Ala dos Prismas e Altares (BLUE):** A fundação da Ordem dos Cavaleiros e a nanotecnologia das Santas de Cristal.
3. **Ala dos Ecos da Ruína (BLACK):** Os segredos da fábrica viva do Rei Demônio e a origem dos Carrascos.
4. **Ala da Imutabilidade (YELLOW):** A ascensão do Trono Sagrado, os decretos do Rei Imóvel e os votos dos Guardiões.
5. **Ala da Simbiose Ancestral (GREEN):** A harmonia com as matilhas gélidas e o tratado de sabedoria da Sociedade Goblin.

---

## 5. Especificação Técnica — Backend

### 5.1 Catálogo de Missões (`missions.json`)
Criar `backend/src/main/resources/missions.json` com a estrutura:
```json
[
  {
    "id": "mission_red_01",
    "titulo": "O Primeiro Clarão da Forja",
    "cor": "RED",
    "capitulo": 1,
    "prologo": "Nos ermos do leste, as fornalhas nunca se apagam...",
    "epilogo": "Com o constructo silenciado, a Poeira de fogo assentou...",
    "playerDeckPresetId": "DECK_RED_TUTORIAL",
    "aiDeck": {
      "heros": ["10000000-0000-0000-0000-000000000003", "..."],
      "cards": ["..."]
    },
    "scriptedEvents": [
      {
        "trigger": "ON_HERO_DEATH",
        "targetHeroName": "Alka, Lutador do Grupo dos Heróis",
        "speaker": "Eco de Alka",
        "dialogue": "A chama... ainda arde mais forte que a minha guarda..."
      },
      {
        "trigger": "ON_CARD_PLAYED",
        "targetCardName": "Bola de Fogo",
        "speaker": "Narrador dos Arquivos",
        "dialogue": "A canalização elemental de Fogo exige precisão e pureza de Espírito."
      }
    ]
  }
]
```

### 5.2 Disparo de Eventos no `GameEngine`
* Adicionar campo `List<ScriptedDialogueDTO> pendingDialogues` ao `GameStateDTO` ou dentro de `TurnTimelineResponseDTO`.
* O `GameEngine` ou um `StoryMissionManagerService` monitora a fila de resolução e a morte de heróis. Ao encontrar correspondência com os `scriptedEvents` da missão ativa, empacota o diálogo para o snapshot do cliente.

---

## 6. Especificação Técnica — Frontend (Phaser 3)

### 6.1 Nova Cena: `StoryArchiveScene` ("Grandes Arquivos da Poeira")
* Interface visual de seleção de capítulos agrupados por cor.
* Indicador de progresso (Concluída / Bloqueada).
* Botão para abrir o Modal de Prólogo antes de transicionar para a `BattleScene`.

### 6.2 Componente de Diálogo: `StoryDialogueOverlay`
* **Modo Prólogo/Epílogo:** Painel visual narrativo com retrato do interlocutor e texto paginado com botão "Continuar".
* **Modo In-Combat (Toast/Banner Narrativo):** Balão de diálogo não-bloqueante ou pause momentâneo na batalha ao disparar o trigger scriptado (exibindo retrato do herói que falou e a frase).

---

## 7. Critérios de Aceite (DoD)

- [ ] Arquivo `missions.json` estruturado com ao menos 1 missão piloto para a cor RED contra o time WHITE.
- [ ] Backend capaz de instanciar uma partida PvE associada a uma `missionId`.
- [ ] Backend emite diálogos scriptados no snapshot quando a carta-alvo for jogada ou herói da máquina morrer.
- [ ] Frontend possui tela acessível "Grandes Arquivos da Poeira" com fluxo: Seleção -> Prólogo -> Partida -> Diálogo In-Game -> Vitória -> Epílogo.
- [ ] Testes de integração no backend cobrindo o ciclo de disparo do `scriptedEvent`.

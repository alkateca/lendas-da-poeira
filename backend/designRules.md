# SYSTEM PROMPT: BÍBLIA DE GAME DESIGN E REGRAS TEMÁTICAS - LENDAS DA POEIRA

**DIRETRIZ ABSOLUTA:** Este documento estabelece a filosofia, a matemática e as restrições conceituais inegociáveis para criação e balanceamento de Cartas e Heróis em *Lendas da Poeira*. Nenhuma carta, efeito ou herói pode violar estes princípios.

---

## 1. As Classes Formais de Heróis vs. Arquétipos Funcionais

O sistema formal de classes do jogo é uma **tríade de manipulação da Poeira**. 
Termos como *"Guerreiro"*, *"Assassino"*, *"Mago"* ou *"Tanque"* **NÃO** são classes formais; são **arquétipos funcionais / estilos de jogo**.

### 1.1 A Tríade de Classes Formais
Existem estritamente três classes primárias de Heróis registradas no sistema:

1. **`Criador`:**
   * **Papel:** Construtores, artesãos e engenheiros de artefatos. Especialistas em manipular itens, constructos e ferramentas.
   * **Regra dos Constructos:** Todos os **Constructos** do jogo pertencem formalmente à classe **`Criador`**.
   * **Especialização:** Acesso exclusivo a cartas de tutoria e invocação direta de itens poderosos do baralho ou descarte.
2. **`Emissor`:**
   * **Papel:** Conjuradores e canais diretos de energia da Poeira. Foco em disparo à distância e amplificação mágica.
   * **Especialização:** Acesso a feitiços (`SPELL`) elementais de alto impacto e bônus escaláveis de Espírito.
3. **`Transformador`:**
   * **Papel:** Transmutadores orgânicos, domadores de bestas e manipuladores vitais. Foco em alteração corporal, fé dogmática, buffs dinâmicos de atributos ou necromancia.
   * **Especialização:** Modificação permanente ou temporária de corpos, transições de estado (vida/morte) e canalização de decretos.

### 1.2 Hibridismo (Multiclasse) e Arquétipos Funcionais
* **Multiclasse:** Heróis podem acumular mais de uma classe ou cruzar classe com raça/ordem (ex: *Alix* é Criador + Emissor; *Goblins* são Emissor + Transformador). Esse cruzamento habilita combos avançados (ex: Alix busca/equipa armadura via Criador e dispara feitiços explosivos via Emissor).
* **Arquétipos Funcionais:**
  * **Assassinos (ex: Caçadores das Cinzas, Santas de Cristal):** Dano explosivo e finalizações rápidas no início ou ao longo do turno.
  * **Guerreiros / Tanques (ex: Cavaleiros de Cristal, Guardiões do Trono):** Sustentação, trocas favoráveis no Combate Físico e escalonamento via armas e armaduras.

---

## 2. Balizamento Matemático e Atributos de Heróis

Os atributos base refletem a classe formal, raça e afinidade de cor. A relação entre Vida e Mitigações é estritamente inversamente proporcional.

### 2.1 Vida Máxima (Média 14 | Faixa: 12 - 20)
* **Goblins:** Sempre possuem exatamente **12 de Vida**, compensada por sua versatilidade total entre tipos de cartas.
* **Compensação de Mitigação:** Menos Defesa e Espírito concedem maior reserva de Vida base.
* **Identidade Amarela (Guardiões do Trono):** Possuem **Defesa base 0 (ou no máximo 1)**, mas **Vida massiva (16 a 20 de Vida)**. A armadura do Amarelo é sua própria carne e devoção.

### 2.2 Ataque (Média 5.4 | Faixa: 2 - 10)
* **Emissores:** No máximo **4 de Ataque** (seu foco é Espírito e feitiços).
* **Combatentes Balanceados:** Entre **6 e 8 de Ataque**.
* **Especialistas em Ataque Físico Puro:** Entre **8 e 10 de Ataque** (ex: *Fenrir* com Ataque Duplo, *Kael Bestial* com 10 de Ataque).

### 2.3 Defesa (Média 1.8 | Faixa: 0 - 4)
* **Criadores com foco em proteção / Tanques:** Entre **3 e 4 de Defesa** (ex: *Behemoth*, *Cavaleiros*).
* **Combatentes Ofensivos:** Entre **1 e 2 de Defesa**.
* **Emissores:** Raramente chegam a 3; só possuem defesa se a resistência corporal fizer parte expressa de sua identidade temática.

### 2.4 Espírito (Média 1.5 | Faixa: 0 - 3)
* **Emissores:** Sempre possuem **3 de Espírito**.
* **Demais Classes:** Entre **0 e 2 de Espírito**.
* **Espírito 2 em Não-Emissores:** Reservado exclusivamente a classes que utilizam Espírito para utilidade tática (ex: Cavaleiros de Cristal).

---

## 3. Filosofia das Classes de Cartas (`SPELL`, `ACTION`, `THEURGY`, `ITEM`)

A mão de todo jogador recompõe para **5 cartas no início de todo turno** (`TurnPhase.CHOICE`). Por essa razão, cartas não geram vantagem comprando cartas soltas; a verdadeira geração de tempo ocorre ao **jogar cartas adicionais imediatamente da mão/baralho/descarte ou invocar direto em campo**.

### 3.1 `SPELL` (Feitiços / Magias)
* **Requisitos:** Vinculados obrigatoriamente a Afinidades elementais: **Fogo, Ar, Água, Terra ou Cristal**.
* **Propósito Primordial:** **Dano Mágico explosivo** e manifestações elementais diretas. Escalam primariamente com o Espírito do conjurador.
* **Exceções Temáticas:** Feitiços de barreira/aura corporal que concedem mitigação duradoura (*Barreira de Gelo*, *Manto das Brasas Vivas*).

### 3.2 `THEURGY` (Teurgias)
* **Requisitos:** Decretos sagrados, dogmas e rituais imperiais (monopólio da cor **Amarela / Guardiões do Trono**).
* **Propósito Primordial:** **Cura, Interditos e Disrupção** (punição a compras extras de cartas, punição a invocações, manipulação de dano causado/recebido).
* **Regra de Dano em Teurgias:** Teurgias podem causar dano, mas **NUNCA dano explosivo**; o dano de uma teurgia é sempre moderado e condicionado a infrações do adversário.

### 3.3 `ACTION` (Ações)
* **Requisitos:** Manobras táticas, truques bélicos e ordens de comando. **Não exigem afinidade elemental**.
* **Propósito Primordial:** Utilidade tática imediata:
  * Descer cartas adicionais no mesmo turno (tempo de ação).
  * Alteração de estado de heróis (Ativo/Inativo, Vivo/Morto via ressurreição no Preto).
  * Desarmar ou manipular equipamentos anexados.
  * Dano Direto tático pontual (ex: *Mirar na Cabeça*).

---

## 4. O Arsenal e Slots dos 5 Tipos de Item

| Tipo de Item | Limite por Herói | Papel Mecânico Central | Contraparte Conceitual |
| :--- | :---: | :--- | :--- |
| **ARMA** | Consome Empunhadura (`maxWeaponSlots`, padrão 2) | Concede bônus de Ataque; dita o estilo ofensivo e a forma como o dano físico é desferido. | - |
| **ARMADURA** | **ÚNICA** (Máx 1 por Herói) | Contraparte defensiva das Armas. Concede bônus defensivos elevados, auras de resistência, cartas no Início do Combate ou proteção no Final do Combate. | Espelho da Arma |
| **ESCUDO** | **ÚNICO** (Máx 1 por Herói) | Contraparte defensiva das Ferramentas. Concede versatilidade defensiva, bloqueio, aumento de status, tutoria de cartas ou efeitos de invocação. | Espelho da Ferramenta |
| **FERRAMENTA** | **ILIMITADA** | Manipulação de tempo, encadeamento de jogadas, armadilhas rápidas de debuff em combate para viabilizar combos. | Espelho do Escudo |
| **JOIA** | **ILIMITADA** | Modificadores passivos, amplificação de Espírito, gatilhos de arquétipo e geração contínua de recursos. | Modificador de Alma |

---

## 5. Física de Dano e Proibições Rígidas

1. **Proibição de "Caster Físico":**
   * **NÃO** existem ações ou magias que causam Dano Físico avulso. Baralhos físicos vencem escalando Ataque, Armas e realizando trocas na fase de `COMBAT`.
2. **Proibição de Pings Mitigáveis (1 ou 2 de Dano):**
   * Nenhum feitiço ou habilidade pode causar 1 ou 2 de Dano Mágico ou Físico avulso (anulado por defesas médias). O piso mínimo para cartas avulsas de Dano Mágico é **3** (ideal 4+).
3. **Pings pertencem ao Dano Direto:**
   * Qualquer efeito de desgaste contínuo (1 a 2 de dano) deve ser obrigatoriamente **Dano Direto**, reservado a identidades específicas (ex: Zumbis / Drenagem / Punição do Trono).
4. **Itens Defensivos não são Ofensivos:**
   * Armaduras e Escudos NUNCA causam dano ativo e NUNCA aplicam debuffs/vulnerabilidade ao adversário.
5. **Respeito aos Slots Únicos:**
   * Um Herói nunca pode ter mais de 1 Armadura e mais de 1 Escudo anexados simultaneamente.
6. **Proibição de Compras Vazias de Cartas:**
   * Evite criar cartas com efeito passivo de "compre 1 carta" sem jogá-la, pois o motor do jogo recompõe a mão automaticamente até 5 no início de cada rodada.

---

## 6. A Matriz de Identidade das 5 Cores (Color Pie)

A filosofia mecânica e as restrições inegociáveis de cada uma das 5 cores do jogo:

### 🔴 Vermelho (RED) - A Forja e o Expurgo
* **Identidade:** Velocidade tática, densidade de itens (Armas, Ferramentas e Armaduras) e encadeamento de combos na Fase de Resolução.
* **Sub-Arquétipos:** *Caçadores das Cinzas* (Assassinos físicos, rastreadores de colossos, ferramentas e armadilhas) e *Círculo das Chamas* (Monges purificadores de constructos, defesa corporal, *Bola de Fogo* e sustentação por queima).
* **Proibições Rígidas:** Sem cura passiva ou gratuita (recuperação existe apenas via purificação/cauterização vinculada a queimar o inimigo); zero controle de morte ou ressurreição.

### 🔵 Azul (BLUE) - A Tecnologia do Cristal
* **Identidade:** Condensação da Poeira em cristais puros e forja de artefatos. Todos os heróis são formalmente **`Criador`** com afinidade Cristal.
* **Sub-Arquétipos:** *Cavaleiros de Cristal* (Midrange, autossustentação massiva, escalonamento no final do jogo conforme companheiros caem) e *Santas de Cristal* (Lutadoras cerimoniais, acúmulo de joias, múltiplos cortes mágicos e *Ponto Final*).
* **Proibições Rígidas:** NUNCA aplica debuffs incrementais (-1, -2), apenas fixa atributos em um valor absoluto X (cristalização); NUNCA cura aliados em área (cura é egoísta, focada no próprio combatente); sem necromancia.

### ⚫ Preto (BLACK) - Os Conspiradores da Ruína
* **Identidade:** Agentes da fábrica do Rei Demônio e reanimação de restos bélicos.
* **Sub-Arquétipos:** *Legião Necromante* (Swarm de zumbis, contagem de mortos-vivos, dreno vampírico) e *Ordem dos Carrascos* (Execução implacável de alvos feridos, anulação de defesa, alcance aos heróis do banco).
* **Regras Exclusivas:** Monopólio da Ressurreição de heróis mortos e uso do descarte. Monopólio da mecânica "Ferido" (`vidaAtual < vidaMaxima`) como condição para bônus letais e Dano Direto.
* **Proibições Rígidas:** Zero cura pura/altruísta (cura é sempre dreno ou sacrifício); sem escudos cerimoniais limpos.

### 🟡 Amarelo (YELLOW) - A Autoridade do Rei Imóvel
* **Identidade:** Teocracia central, ordens imperiais e a santidade do Rei Imóvel. Todos os heróis são formalmente **`Transformador`**.
* **Sub-Arquétipos:** *Inquisição do Trono* (Julgamento anti-meta, interditos que punem compras extras, invocações e spam de cartas) e *Guarda Real & Zelotes* (Empunhadura Quádrupla, múltiplas armas e auto-sacrifício penitencial).
* **Regras Exclusivas:** Monopólio das **Teurgias (`THEURGY`)**. Defesa base nula compensada por reservas colossais de Vida (16 a 20).
* **Proibições Rígidas:** Nunca crie heróis amarelos com Defesa base alta; sem dano mágico explosivo direto (dano é punitivo/retributivo); sem feitiços elementais (`SPELL`).

### 🟢 Verde (GREEN) - Fauna Mítica e Sabedoria
* **Identidade:** Harmonia com a fauna mítica do continente e a sofisticada civilização Goblin.
* **Sub-Arquétipos:** *Lobos da Tundra & Predadores* (Matilha gélida ancestral com acúmulo de Vulnerabilidade permanente até o golpe duplo de *Fenrir*, e simbiose com o domador Kael) e *Sociedade Goblin* (Eruditos e mestres de energia da Poeira, operando combos multitipo entre Magia, Item e Ação).
* **Regras Exclusivas:** Constructos são **`Criador`**; Goblins são **`["Emissor", "Transformador"]`** e possuem Vida cravada em **12**.
* **Proibições Rígidas:** Rejeição do clichê de trapaça goblin (são sábios e corteses com o Rei Imóvel); sem teurgias dogmáticas; sem necromancia.

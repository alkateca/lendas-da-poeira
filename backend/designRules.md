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
   * **Especialização:** Acesso exclusivo a cartas de tutoria e invocação direta de itens poderosos do baralho ou descarte.
2. **`Emissor`:**
   * **Papel:** Conjuradores e canais diretos de energia da Poeira. Foco em disparo à distância e amplificação mágica.
   * **Especialização:** Acesso a feitiços (`SPELL`) elementais de alto impacto e bônus escaláveis de Espírito.
3. **`Transformador`:**
   * **Papel:** Transmutadores orgânicos, domadores de bestas e manipuladores vitais. Foco em alteração corporal, feras, buffs dinâmicos de atributos ou necromancia.
   * **Especialização:** Modificação permanente ou temporária de corpos, transições de estado (vida/morte) e sinergias orgânicas.

### 1.2 Hibridismo (Multiclasse) e Arquétipos Funcionais
* **Multiclasse:** Heróis podem acumular mais de uma classe ou cruzar classe com raça/ordem (ex: *Alix* é Criador + Emissor; *Rainha Goblin* é Transformador + Goblin). Esse cruzamento habilita combos de alto impacto (ex: Alix busca/equipa sua armadura lendária via Criador e dispara feitiços explosivos via Emissor).
* **Arquétipos Funcionais:**
  * **Assassinos (ex: Caçadores das Cinzas):** Altíssimo dano explosivo inicial; perdem fôlego e energia se a partida se estender contra baralhos de tempo/controle.
  * **Guerreiros / Tanques (ex: Cavaleiros, Guardiões):** Sustentação, trocas favoráveis no Combate Físico e escalonamento via Armas e Armaduras.

---

## 2. Balizamento Matemático e Atributos de Heróis

Os atributos base refletem a classe formal, raça e afinidade de cor. A relação entre Vida e Mitigações é estritamente inversamente proporcional.

### 2.1 Vida Máxima (Média 14 | Faixa: 12 - 20)
* **Goblins:** Sempre possuem exatamente **12 de Vida**.
* **Compensação de Mitigação:** Menos Defesa e Espírito concedem maior reserva de Vida base.
* **Identidade Amarela (Guardiões do Trono):** Possuem **Defesa base muito baixa ou nula**, mas **Vida massiva**. Um perfil padrão que em outra cor teria 14 de Vida e 3 de Defesa é traduzido em Amarelo como 17 ou 18 de Vida e Defesa 0 ou 1.

### 2.2 Ataque (Média 5.4 | Faixa: 2 - 10)
* **Emissores:** No máximo **4 de Ataque** (seu foco é Espírito e feitiços).
* **Perfis Combatentes / Guerreiros:** Entre **6 e 8 de Ataque**.
* **Especialistas em Ataque Físico Puro (Assassinos/Berserkers):** Entre **8 e 10 de Ataque**.

### 2.3 Defesa (Média 1.8 | Faixa: 0 - 4)
* **Criadores com foco em proteção:** Entre **3 e 4 de Defesa**.
* **Combatentes Ofensivos:** Entre **1 e 2 de Defesa**.
* **Emissores:** Raramente chegam a 3; só possuem defesa elevada se a resistência corporal fizer parte expressa de sua identidade temática.

### 2.4 Espírito (Média 1.5 | Faixa: 0 - 3)
* **Emissores:** Sempre possuem **3 de Espírito**.
* **Demais Classes:** Entre **0 e 2 de Espírito**.
* **Espírito 2 em Não-Emissores:** Reservado exclusivamente a papéis que utilizam Espírito para utilidade tática (ex: Cavaleiros e Paladinos).

---

## 3. Filosofia das Classes de Cartas (`SPELL`, `ACTION`, `THEURGY`, `ITEM`)

As cartas não-herói dividem-se em papéis estratégicos claros:

### 3.1 `SPELL` (Feitiços / Magias)
* **Requisitos:** Vinculados obrigatoriamente a Afinidades elementais: **Fogo, Ar, Água, Terra ou Cristal**.
* **Propósito Primordial:** **Dano Mágico explosivo** e manifestações elementais diretas. Escalam primariamente com o atributo de Espírito do conjurador.
* **Exceções Temáticas:** Existem feitiços que fogem do dano puro para reforçar a temática elemental (ex: *Barreira de Gelo*, feitiço de Água focado em gerar defesa em área).

### 3.2 `THEURGY` (Teurgias)
* **Requisitos:** Rituais, dogmas e decretos sagrados (predominantes na cor Amarela / Guardiões do Trono).
* **Propósito Primordial:** **Cura e Disrupção** (manipulação de dano causado/recebido, punição às ações do adversário, controle das leis da partida).
* **Regra de Dano em Teurgias:** Teurgias podem causar dano, mas **NUNCA dano explosivo**; o dano de uma teurgia é moderado ou punitivo/reativo.
* **Exceções Temáticas:** Teurgias de ordem militar que aceleram jogadas (ex: *Saque Rápido*, permitindo descer armas da mão com bônus de Ataque).

### 3.3 `ACTION` (Ações)
* **Requisitos:** Manobras táticas, truques bélicos e ordens de comando. **Não exigem afinidade elemental**.
* **Propósito Primordial:** Utilidade imediata e controle do tabuleiro:
  * Compra de cartas e aceleração de mão.
  * Alteração de estado de heróis (Ativo/Inativo, Vivo/Morto via ressurreição).
  * Desarmar ou manipular equipamentos anexados.
  * Dano Direto tático pontual (ex: *Mirar na Cabeça*).

---

## 4. O Arsenal e Slots dos 5 Tipos de Item

| Tipo de Item | Limite por Herói | Papel Mecânico Central | Contraparte Conceitual |
| :--- | :---: | :--- | :--- |
| **ARMA** | Consome Empunhadura (`maxWeaponSlots`, padrão 2) | Concede bônus de Ataque; dita o estilo ofensivo e a forma como o dano físico é desferido. | - |
| **ARMADURA** | **ÚNICA** (Máx 1 por Herói) | Contraparte defensiva das Armas. Concede bônus defensivos elevados, auras de resistência, cartas no Início do Combate ou proteção no Final do Combate. | Espelho da Arma |
| **ESCUDO** | **ÚNICO** (Máx 1 por Herói) | Contraparte defensiva das Ferramentas. Concede versatilidade defensiva, bloqueio, aumento de status, tutoria de cartas ou efeitos de invocação. | Espelho da Ferramenta |
| **FERRAMENTA** | **ILIMITADA** | Manipulação de tempo, compra/ciclo de cartas, armadilhas rápidas de debuff em combate para viabilizar combos. | Espelho do Escudo |
| **JOIA** | **ILIMITADA** | Modificadores passivos, amplificação de Espírito, gatilhos de arquétipo e geração de recursos contínuos. | Modificador de Alma |

---

## 5. Física de Dano e Proibições Rígidas

1. **Proibição de "Caster Físico":**
   * **NÃO** existem ações ou magias que causam Dano Físico avulso. Baralhos físicos escalam via Ataque, Armas e trocas na fase de `COMBAT`.
2. **Proibição de Pings Mitigáveis (1 ou 2 de Dano):**
   * Nenhum feitiço ou habilidade pode causar 1 ou 2 de Dano Mágico ou Físico avulso. O piso mínimo para cartas avulsas de Dano Mágico é **3** (ideal 4+).
3. **Pings pertencem ao Dano Direto:**
   * Qualquer efeito de desgaste contínuo (1 a 2 de dano) deve ser obrigatoriamente **Dano Direto**, reservado a identidades específicas (ex: Zumbis / Drenagem).
4. **Itens Defensivos não são Ofensivos:**
   * Armaduras e Escudos NUNCA causam dano ativo e NUNCA aplicam debuffs/vulnerabilidade ao oponente.
5. **Respeito aos Slots Únicos:**
   * Um Herói nunca pode ter mais de 1 Armadura e mais de 1 Escudo anexados simultaneamente.
6. **Compensação Amarela:**
   * Nunca crie Heróis Amarelos com Defesa base elevada; sua sobrevivência provém exclusivamente de sua reserva massiva de Vida.

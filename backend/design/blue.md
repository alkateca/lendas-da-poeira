# BÍBLIA TEMÁTICA - COR AZUL (BLUE)

**DIRETRIZ DA COR:** O Azul em *Lendas da Poeira* é a cor da **condensação da Poeira em Cristais, tecnologia de artefatos, autossustentação (self-sustain) e manipulação precisa de atributos**. Todos os seus personagens são primariamente **Criadores** que dominam a forja e ressonância do cristal, permitindo-lhes conjurar feitiços elementais devastadores de Cristal mesmo sem pertencerem à classe Emissor.

---

## 1. Lore e Filosofia do Cristal

* **A Natureza do Cristal:** Os cristais são a própria Poeira condensada e estabilizada em estado sólido. Representam o mais alto patamar de pureza energética e tecnologia material do continente.
* **O Culto ao Dragão de Cristal:** A cultura azul é profundamente ligada à veneração do **Dragão de Cristal**, um constructo mítico colossal adorado quase como divindade. Toda a ordem militar e sacerdotal do azul deriva dessa devoção.
* **Mestres da Criação:** Seus heróis não são conjuradores etéreos; são artesãos, ferreiros e lutadores que utilizam joias, lâminas e artefatos de cristal para projetar poder espiritual e físico.

---

## 2. Leis de Design e Limites Rígidos

### 2.1 O que o Azul FAZ
* **Afinidade Cristal Universal:** Por manipularem a Poeira condensada, Heróis Azuis acessam feitiços (`SPELL`) de Cristal poderosos sendo puramente da classe **Criador**.
* **Cristalização de Atributos (Fixação em X):** O Azul altera o estado das coisas fixando números (ex: *"O Ataque torna-se X"* ou *"A Defesa torna-se 0"*), refletindo o ato de petrificar/cristalizar uma característica.
* **Auto-Sustentação Pesada (Self-Sustain Egoísta):** Alta capacidade de recuperar pontos de vida para o **próprio Herói** que está em combate, escalando frequentemente com o atributo de Espírito.
* **Midrange & Late-Game Crescente:** Seus guerreiros tornam-se exponencialmente mais letais conforme a partida avança ou conforme baixas ocorrem na equipe.

### 2.2 O que o Azul NUNCA FAZ (Proibições)
1. **Sem Debuffs Incrementais (-1, -2):** O Azul **NUNCA** aplica penalidades graduais de `-1` ou `-2` de status (isso pertence ao Vermelho, Preto ou Amarelo). Ele fixa valores absolutos através da cristalização.
2. **Sem Cura em Aliados (Cura Coletiva Proibida):** O Azul possui cura massiva, mas **exclusivamente no próprio conjurador/combatente**. Ele não cura parceiros no banco ou a equipe inteira.
3. **Sem Manipulação de Morte/Necromancia:** O Azul não mexe com o estado de mortos, não revive heróis e não possui ressurreição.
4. **Sem Truques Sujos ou Armadilhas:** A ordem do Cristal luta de forma cerimonial e honrada; não utiliza venenos ou ferramentas de emboscada.

---

## 3. Sub-Arquétipos

### Arquétipo A: Cavaleiros de Cristal (Midrange / Tempo / Vingança do Dragão)

* **Conceito & Lore:** A nobre ordem militar de guerreiros que servem ao Dragão de Cristal. Portam armaduras espelhadas e espadas forjadas em cristal puro. São guerreiros de elite treinados para resistir e lutar até o último homem.
* **Estilo de Jogo:** **Midrange / Tempo.** Possuem Defesa natural elevada (base 3) e muita sustentação própria. Crescem em poder conforme o combate avança e se tornam extremamente perigosos quando aliados da ordem são derrotados.
* **Heróis Centrais (Todos `Criador`):**
  * [`Enamor, O Grande Cavaleiro`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L370): Guerreiro pilar de autossustentação que recupera vida igual ao seu Espírito.
  * [`Cavaleiro Onear`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L422): Combatente pesado que converte Espírito em dano físico bônus no encerramento da batalha.
  * [`Ageus, Constructo Cristalino`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L396): Constructo colossal que serve de elo entre a mística do Dragão de Cristal e a cavalaria terrestre.
* **Mecânicas Centrais:**
  * **Armas de Cristal & Espírito Híbrido:** Armas de empunhadura simples (*Quimera*, *Dragast*, *Fragmento Afiado*) que concedem tanto Ataque quanto Espírito.
  * **Escala por Baixas de Cavaleiros:** Bônus passivos de fúria e escalonamento no final do jogo caso companheiros caiam.
  * **Feitiços de Auto-Fortalecimento:** Uso de magias como *Contra-Ataque* e *Golpes Pesados*.
* **Equipamento Lendário Único:**
  * `Quimera` (*Arma - Empunhadura Simples - Cavaleiro*): Concede altos bônus de Ataque, Defesa e Espírito, e dobra a recuperação de Vida baseada em Espírito no Final do Combate.

---

### Arquétipo B: Santas de Cristal / Lâminas Cerimoniais (Assassinas Mágicas / Ressonância)

* **Conceito & Lore:** Lutadoras cerimoniais e artesãs sacerdotais que protegem e criam a nanotecnologia do cristal condensado. A Santa das Lâminas é a líder espiritual máxima da religião do Dragão de Cristal. Suas guerreiras dançam no combate desferindo cortes de energia pura.
* **Estilo de Jogo:** **Assassinas de Dano Mágico Contínuo.** Ao invés de um único golpe físico, utilizam ressonância cristalina para causar múltiplas instâncias de dano mágico em diferentes fases do turno, finalizando alvos enfraquecidos.
* **Heróis Centrais (Todos `Criador`):**
  * [`Isenora, Santa das Laminas`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L248): Dispara dano mágico ao final do combate e potencializa todos os feitiços de Cristal da equipe.
  * [`Moyra, Aprendiz da Santa`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L274) / [`Moyra, Santa das Laminas`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L448): Tutora de feitiços de cristal no início da batalha e executora implacável.
  * [`Naelis, Grande Artesã de Cristais`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L344): Artesã que forja e refina joias e broches de cristal para fortalecer as combatentes.
* **Mecânicas Centrais:**
  * **Joias de Cristal Ilimitadas:** Acúmulo de *Broche de Cristal* e *Dragão de Cristal* para elevar o Espírito e infligir dano passivo.
  * **Múltiplos Pings de Dano Mágico:** Ataques que se dividem entre a fase de Resolução e o Final do Combate (*Massacre Cristalino*, *Ataque Mágico*).
  * **O Golpe de Misericórdia (*Ponto Final*):** Feitiço supremo de dano concentrado para abater alvos já desgastados pela ressonância.
* **Equipamento Lendário Único:**
  * `Coração de Cristal Puro` (*Joia - Cristal*): Joia lendária que maximiza o Espírito e projeta aura de amplificação direta para todos os feitiços de cristal.

---

## 4. Matriz de Deckbuilding do Azul

| Componente | Cavaleiros de Cristal | Santas / Lâminas de Cristal |
| :--- | :--- | :--- |
| **Arquétipo Funcional** | Midrange / Tanque de Sustento | Assassinas de Ressonância Mágica |
| **Tipo de Dano Primário** | Físico apoiado por Espírito | Mágico (Múltiplas Fontes + Feitiços) |
| **Itens Primários** | Armas Simples (*Dragast*, *Quimera*) | Joias (*Broches*, *Coração de Cristal*) e Lâminas Rápidas |
| **Estilo de Cura** | Regeneração massiva no próprio Herói | Redução tática e bloqueio com defesas mágicas |
| **Comportamento de Turno** | Escala e resiste para dominar o late game | Executa oponente via múltiplos cortes e *Ponto Final* |
| **Manipulação de Status** | Auto-buffs de combate | Fixação de atributos para anular vantagens inimigas |

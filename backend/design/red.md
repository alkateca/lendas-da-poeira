# BÍBLIA TEMÁTICA - COR VERMELHA (RED)

**DIRETRIZ DA COR:** O Vermelho em *Lendas da Poeira* é a cor da **velocidade tática, densidade de itens e combos com múltiplas cartas**. Seus baralhos não dependem de força bruta isolada de um único golpe lento, mas sim da sinergia explosiva entre equipamentos (Armas, Ferramentas, Armaduras) e sequências de Ações/Feitiços jogados no mesmo turno.

---

## 1. Filosofia de Design e Limites Rígidos

### 1.1 O que o Vermelho FAZ
* **Combos de Múltiplas Cartas:** Encadeamento de Itens + Ações na Fase de Resolução para desencadear gatilhos cumulativos.
* **Arsenal Especializado:** Uso intensivo de **Ferramentas** para desarmar, criar armadilhas e acelerar o tempo de jogo.
* **Purificação pelo Fogo:** Feitiços de dano mágico de alto impacto baseados na afinidade Fogo (*Bola de Fogo*).
* **Defesa Purificadora & Cauterização:** Aumento de Defesa corporal e sustentação reativa condicionada a punir ou queimar o inimigo (nunca cura passiva ou gratuita).
* **Assassinato de Alvos Colossais:** Redução agressiva da Defesa adversária para abrir caminho a finalizações rápidas.

### 1.2 O que o Vermelho NUNCA FAZ (Proibições)
1. **Sem Cura Passiva/Direta:** O Vermelho possui pouca ou nenhuma cura. Qualquer recuperação de vida deve ser temática de purificação/cauterização (ex: recuperar vida somente se causou dano mágico de fogo ou expurgou uma ameaça).
2. **Sem Controle de Morte/Ressurreição:** O ciclo da vida e da morte pertence ao Preto. Heróis vermelhos caídos não retornam.
3. **Sem Caster Físico:** Caçadores atacam com armas físicas no combate; nunca lançam "magias físicas".
4. **Vulnerabilidade a Feitiços no Eixo Físico:** Caçadores possuem baixo Espírito base, tornando-se suscetíveis a dano mágico pesado caso não eliminem a ameaça rapidamente.

---

## 2. O Papel dos Constructos Vermelhos

Os Constructos em baralhos vermelhos atuam como **pilares de suporte poderoso**. Eles não competem pelo papel de herói principal, mas fornecem a retaguarda resistente e os amplificadores mecânicos necessários para que os humanos brilhem:
* **Constructo de Combate, Arsenal Vivo:** Provê suporte de armas, manipulação de itens e consistência física para os Caçadores.
* **Constructo das Chamas, Sif:** Provê suporte defensivo ígneo e amplificação mágica para os Monges do Fogo.

---

## 3. Sub-Arquétipos

### Arquétipo A: Caçadores das Cinzas (Assassinos / Físico / Ferramentas)

* **Conceito & Lore:** Rastreadores de feras e constructos colossais espalhados pelos ermos do continente. Não lidam com invasões urbanas rotineiras; sua missão é caçar e neutralizar abominações ancestrais distantes das cidades antes que se tornem ameaças ativas.
* **Estilo de Jogo:** **Assassino / Tempo.** Dano explosivo massivo no Turno 1 e 2 via armadilhas e armas leves. Perde energia se a partida se arrastar contra decks de controle.
* **Heróis Centrais:**
  * [`Xia, Caçador de Bestas`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L720) (*Criador*): Puxa e joga Ferramentas diretamente do baralho no `COMBAT_START`.
  * [`Asahum, Caçadora de Bestas`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L747) (*Criador*): Especialista em finalização e sinergia de caça.
  * [`Arsenal Vivo`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L773) (*Constructo*): Suporte tático para sustentação e troca de arsenal.
* **Mecânicas Centrais:**
  * **Ferramentas Ilimitadas:** Uso contínuo de itens como *Sino do Caçador*, *Bomba de Fumaça* e *Tesouro Cristalino*.
  * **Debuffs de Defesa:** Cartas de Ação (*Corte seus Pescoços*, *Bomba de Fumaça*) que reduzem a Defesa do inimigo a zero.
  * **Ataques Fora de Fase:** Gatilhos que forçam ataques físicos adicionais antes do `COMBAT`.
* **Equipamento Lendário Único:**
  * `Bolsa de Armadilhas do Mestre Caçador` (*Ferramenta*): Pode ser tutorada no Turno 1 pelo Xia, joga a próxima ferramenta do baralho ao entrar e no Início do Combate, e migra para outro aliado ativo no Final do Combate.
* **As 10 Cartas do Baralho:**
  1. `Sica` (Arma - Simples)
  2. `Arpão terrestre` (Arma - Simples)
  3. `Sino do Caçador` (Ferramenta)
  4. `Bomba de fumaça` (Ferramenta)
  5. `Tesouro Cristalino dos Caçadores` (Ferramenta)
  6. `Rede de Contenção de Feras` (Ferramenta)
  7. `Bolsa de Armadilhas do Mestre Caçador` (Ferramenta Lendária Única)
  8. `Afie suas Lâminas` (Ação)
  9. `Estilo Secreto do Caçador` (Ação)
  10. `Corte seus Pescoços` (Ação)

---

### Arquétipo B: Círculo das Chamas (Monges Purificadores / Mágico / Fogo)

* **Conceito & Lore:** Monges guerreiros e ascetas que utilizam o fogo sagrado da Poeira para purificar constructos corrompidos que atacam ativamente a civilização. Diferente de magos frágeis, treinam o corpo e utilizam mantos e escudos incandescentes para lutar na linha de frente.
* **Estilo de Jogo:** **Caster de Queima Sustentada & Defesa Ígnea.** Combina feitiços explosivos com autossuficiência defensiva e cauterização de ferimentos.
* **Heróis Centrais:**
  * [`Alix, Dragão Flamejante`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L825) (*Criador + Emissor*): Híbrido que conecta armaduras corporais à geração contínua de feitiços de *Bola de Fogo*.
  * [`Xamã das Cinzas`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L799) (*Criador + Emissor*): Suporte ritualístico focado em chamas e Poeira.
  * [`Constructo das Chamas, Sif`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L852) (*Constructo*): Muralha resistente que absorve impacto e amplifica a queima mágica.
* **Mecânicas Centrais:**
  * **Feitiços de Dano Mágico:** Uso massivo de *Bola de Fogo* (4 + Espírito de dano) e *Maestria com Chamas*.
  * **Defesa Corporal & Escudo:** Uso de *Peitoral de Brasas* (Armadura Única) e *Escudo Ardente* (Escudo Único) para resistir a ataques físicos e disparar gatilhos.
  * **Sustain por Cauterização:** Efeitos que recuperam Vida apenas quando Dano Mágico de Fogo é causado com sucesso no combate.
* **Equipamento Lendário Único:**
  * `Escamas do Dragão Eterno` (*Armadura Única*): Oferece alta Defesa e Espírito, invoca *Bola de Fogo* ao entrar no combate e recupera Vida no Final do Combate se o inimigo sofreu dano mágico.
* **As 10 Cartas do Baralho:**
  1. `Escamas do Dragão Eterno` (Armadura Lendária Única)
  2. `Peitoral de brasas` (Armadura)
  3. `Escudo ardente` (Escudo)
  4. `Bastão do Monge Incandescente` (Arma - Simples)
  5. `Incensário Purificador` (Joia)
  6. `Muralha de Fogo` (Feitiço - Fogo)
  7. `Cauterizar` (Feitiço - Fogo)
  8. `Fogo Purificador` (Feitiço - Fogo)
  9. `Manto das Brasas Vivas` (Feitiço - Fogo)
  10. `Meditação das Cinzas` (Ação)
  *(Suporte de Feitiços Neutros de Fogo: Bola de Fogo, Maestria com Chamas, Alma em Chamas)*

---

## 4. Matriz de Deckbuilding do Vermelho

| Componente | Caçadores das Cinzas | Círculo das Chamas |
| :--- | :--- | :--- |
| **Tipo de Dano Primário** | Físico (Ataque de Armas + Combate) | Mágico (Feitiços de Fogo + Espírito) |
| **Itens Primários** | Ferramentas (Sinos/Bombas) e Armas (Sica/Arpão) | Armadura Única (Peitoral) e Escudo Único |
| **Ações / Feitiços** | Ações de Preparo, Debuff de Defesa e Golpes Rápidos | Feitiços Elementais (`SPELL`) de Afinidade Fogo |
| **Tipo de Suporte do Constructo** | Provisão de armas e ataques coordenados | Barreira de contenção e ignição contínua |
| **Comportamento Temporal** | Explosão nos turnos 1 e 2 (All-in tático) | Pressão contínua com queima e regeneração |

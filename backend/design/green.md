# BÍBLIA TEMÁTICA - COR VERDE (GREEN)

**DIRETRIZ DA COR:** O Verde em *Lendas da Poeira* é a cor da **fauna mítica de constructos, da sabedoria tecnológica da sociedade Goblin e da simbiose ecológica com a Poeira**. Longe de ser um arquétipo de "selvagens", o Verde reúne tribos e eremitas que convivem com animais mecânicos majestosos e uma civilização goblin letrada, engenhosa e diplomática.

---

## 1. Lore e Filosofia da Cor Verde

* **A Fauna Mítica de Constructos:** Diferente das abominações do Rei Demônio, os constructos do Verde existem como parte orgânica do ecossistema do continente. São animais e predadores míticos (lobos polares, behemoths, rapinárias) que vivem em harmonia com os biomas naturais e não são inimigos ativos da humanidade.
* **Os Lobos das Planícies do Sul:** Nas tundras gélidas do sul habitam constructos lupinos ancestrais — liderados por **Fenrir, Hati e Skoll** —, guardiões naturais daquelas terras que caçam com precisão predatória.
* **Eremitas e Tribos Simbióticas:** Humanos que rejeitaram a rigidez imperial da capital e escolheram viver nos ermos em contato direto com a natureza e com os constructos (como **Kael, o Domador de Feras**). São primordialmente **Transformadores** ou Criadores integrados à terra.
* **A Nobre Sociedade Goblin:** 
  * Goblins **NÃO são trapaceiros, selvagens ou monstros de emboscada**.
  * São uma civilização de seres inteligentes, sábios, respeitados e cordiais, habitando suas próprias metrópoles ou bairros integrados nas cidades humanas.
  * Mantêm laços diplomáticos antigos e grande familiaridade com o **Rei Imóvel**.
  * Dominam a Poeira como uma fonte de energia motriz e elétrica contínua para alimentar seus aparelhos e criações de cristal. Não usam a Poeira para invocar armas mágicas marciais, mas para manipular qualquer tipo de recurso com flexibilidade total.

---

## 2. Leis de Design e Limites Rígidos

### 2.1 O que o Verde FAZ
* **Classes Especializadas:**
  * **Constructos Ferais:** São formalmente **`Criador`** (suas garras, dentes e carcaças foram forjados com precisão ancestral).
  * **Goblins:** São formalmente híbridos **`["Emissor", "Transformador"]`**, refletindo seu domínio irrestrito de qualquer energia ou magia.
  * **Domadores e Guardiões:** São formalmente **`Transformador`** (ligação empática e corporal com as feras).
* **A Regra dos 12 de Vida dos Goblins:** Todo Herói Goblin possui estritamente **12 de Vida**, compensando sua menor massa física com uma versatilidade de cartas incomparável.
* **Mecânica de Predação (Vulnerabilidade Acumulativa):** Exclusiva do sub-arquétipo dos Lobos Gélidos. Acumula marcadores de Vulnerabilidade permanente no alvo até abrir espaço para o golpe duplo e finalização de *Fenrir*.
* **Afinidade Água / Gelo:** Feitiços de resfriamento, congelamento e resistência corporal (*Barreira de Gelo*, *Mordida Glacial*).
* **Multitipo Goblin:** Habilidade única dos Goblins de se fortalecerem independentemente do tipo de carta jogada (Magias geram Espírito, Itens geram Ataque, Ações geram Defesa).

### 2.2 O que o Verde NUNCA FAZ (Proibições)
1. **Sem Estereótipos de Trapaça Goblin:** Goblins não usam bombas de veneno covardes ou furtividade suja; usam sabedoria, engenharia e energia de cristal.
2. **Sem Teurgias Dogmáticas:** O Verde não promulga interditos religiosos imperiais (`THEURGY` pertence exclusivamente ao Amarelo).
3. **Sem Manipulação de Morte ou Sacrifício:** A fauna verde respeita o ciclo da vida natural; não cria zumbis nem profana túmulos (Preto).

---

## 3. Sub-Arquétipos

### Arquétipo A: Lobos da Tundra & Constructos Míticos (Predadores / Físico / Vulnerabilidade)

* **Conceito & Lore:** A matilha de guardiões gélidos das planícies do sul e os eremitas que os compreendem. Lutam como uma unidade predatória perfeita que cerca a presa, enfraquece suas defesas e abate alvos colossais.
* **Estilo de Jogo:** **Rastreio, Vulnerabilidade e Finalização Devastadora.** Utiliza armas de gelo e feitiços para acumular Vulnerabilidade até que a presa não consiga mais resistir aos golpes de Fenrir.
* **Heróis Centrais:**
  * [`Fenrir, o Grande Devorador`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L630) (*Criador | Constructo*): O predador ápice. Concede Ataque e Defesa a todos os constructos e desfere Ataque Duplo se o alvo tiver 5 ou mais de Vulnerabilidade.
  * [`Skoll, Espreitador Solar`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L604) (*Criador | Constructo*): Aplica Vulnerabilidade no início do combate e converte a fragilidade do alvo em Ataque próprio.
  * [`Hati, Constructo Gélido`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L578) (*Criador | Constructo*): Caçador de conjuradores com Ataque elevado contra Emissores, que transfere a vulnerabilidade do inimigo morto para o próximo herói.
  * [`Kael, Domador de Feras`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L526) (*Transformador*): O elo humano. Cura e fortalece o Ataque dos constructos aliados no combate.
  * [`Behemoth-29A`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L552) (*Criador | Constructo*): Baluarte defensivo pesado com Defesa 4.
* **Mecânicas Centrais:**
  * **Vulnerabilidade Permanente:** Acúmulo de bônus via *Machado de Gelo*, *Grande Machado Sombrio* e *Mordida Glacial*.
  * **Ataque Duplo Condicional:** Execução rápida com Fenrir assim que o threshold de vulnerabilidade for atingido.
* **Equipamento Lendário Único:**
  * `Baluarte da Tundra Ancestral` (*Escudo Único*): Oferece alta Defesa, Redução de Dano e aplica Vulnerabilidade contínua no inimigo ao início do combate.
* **As 10 Cartas do Baralho:**
  1. `Baluarte da Tundra Ancestral` (Escudo Único - Lendário)
  2. `Couraça da Tundra Profunda` (Armadura Única)
  3. `Machado de Gelo` (Arma Simples)
  4. `Grande Machado Sombrio` (Arma Dupla)
  5. `Garras Congeladas` (Arma Simples)
  6. `Barreira de Gelo` (Feitiço - Afinidade Água)
  7. `Mordida Glacial` (Feitiço - Afinidade Água)
  8. `Uivo da Matilha Gélida` (Feitiço - Afinidade Água)
  9. `Luz da Lua` (Ação)
  10. `Chamado do Domador` (Ação)

---

### Arquétipo B: A Sociedade Goblin (Engenharia Sábia / Flexibilidade Energética)

* **Conceito & Lore:** Eruditos e inventores de baixa estatura que dominam o fluxo da Poeira como corrente energética contínua. Lutam em equipes coordenadas, respondendo a qualquer situação através de sua maestria técnica.
* **Estilo de Jogo:** **Adaptação Total e Reação Multitipo.** Beneficiam-se de turnos variados onde diferentes tipos de cartas são acionados em sequência.
* **Heróis Centrais (Todos `Emissor + Transformador`):**
  * [`Rei Goblin`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L121) (Esp 3, Atq 2, Def 1, HP 12): O sábio monarca. Sua aura concede +1 no valor de todos os efeitos ativados pelos seus aliados goblins.
  * [`Esquadrão Goblin`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L95) (Esp 2, Atq 4, Def 2, HP 12): Converte o jogo de Magias em Espírito, Itens em Ataque e Ações em Defesa, recuperando vida ao fim do turno.
  * [`Traidor Goblin`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L147) (Esp 0, Atq 6, Def 1, HP 12): Especialista combatente que ganha Ataque e Defesa por cada goblin no time.
  * [`Heróis Lendários dos Goblin`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L173) (Esp 3, Atq 5, Def 3, HP 12): Triunfo da engenharia goblin, curando aliados e recebendo atributos ao longo da resolução.
* **Mecânicas Centrais:**
  * **Isenção de Restrições:** Liberdade para utilizar cartas de diversas afinidades sem penalidade.
  * **Amplificação pelo Rei:** O Rei Goblin duplica ou incrementa a potência de cada gatilho ativado pela equipe.
* **Equipamento Lendário Único:**
  * `Núcleo de Invenções do Rei Goblin` (*Ferramenta Lendária*): Artefato de energia pura que concede atributos a cada carta jogada e recupera Vida no Final do Combate.
* **As 10 Cartas do Baralho:**
  1. `Núcleo de Invenções do Rei Goblin` (Ferramenta - Lendária)
  2. `Bateria de Cristal de Poeira` (Ferramenta)
  3. `Chave Mecânica de Precisão` (Arma Simples)
  4. `Engrenagem de Ressonância` (Joia)
  5. `Descarga de Poeira Elétrica` (Feitiço - Afinidade Ar)
  6. `Circuito de Sobrecarga` (Feitiço - Afinidade Ar)
  7. `Campo de Força de Cristal` (Feitiço - Afinidade Cristal)
  8. `Engenharia Rápida` (Ação)
  9. `Recalibração de Energia` (Ação)
  10. `Conferência dos Sábios Goblins` (Ação)

---

## 4. Matriz de Deckbuilding do Verde

| Componente | Lobos da Tundra & Predadores | Sociedade Goblin |
| :--- | :--- | :--- |
| **Arquétipo Funcional** | Caçador Feral de Vulnerabilidade | Engenheiro Sábio Adaptativo |
| **Gatilho Central** | Acúmulo de Vulnerabilidade permanente | Jogar cartas de diferentes categorias |
| **Tipo de Dano Primário** | Dano Físico Devastador (Ataque Duplo) | Balanceado (Mágico, Físico e Efeitos) |
| **Fator Defensivo** | Armaduras de gelo (*Barreira de Gelo*) | Defesa via Ações e cura do Esquadrão |
| **Itens Primários** | Machados pesados e Escudos de bloqueio | Ferramentas de energia e núcleos de Poeira |
| **Estilo de Vitória** | Explosão física quando o alvo acumula debuff | Vantagem de valor e sinergia do Rei Goblin |

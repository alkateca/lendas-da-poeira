# BÍBLIA TEMÁTICA - COR PRETA (BLACK)

**DIRETRIZ DA COR:** O Preto em *Lendas da Poeira* é a cor dos **Conspiradores da Ruína, das abominações do Rei Demônio, da manipulação de corpos (vivos e mortos) e da execução impiedosa de alvos feridos**. É a única cor do jogo com domínio sobre o ciclo da morte, ressurreição e a mecânica de Dano Direto contínuo.

---

## 1. Lore e Filosofia da Cor Preta

* **O Rei Demônio:** Uma entidade mítica lendária: um **constructo colossal que atua como uma fábrica viva autônoma**, forjando outros constructos de poder catastrófico (chefes de raid) destinados a expurgar a humanidade.
* **Os Necromantes e Conspiradores:** Facções humanas traidoras e fanáticos que vasculham os campos de batalha após grandes guerras. Eles recolhem fragmentos de cristal quebrado, fluidos vitais e a Poeira negra residual para fundir restos mortais a engrenagens, criando abominações orgânico-mecânicas (Zumbis e Quimeras).
* **Ideologia da Ruína:** Acreditam fervorosamente que a civilização humana é uma praga falha e decadente, e que o mundo deve ser entregue ao domínio eterno dos constructos.
* **A Ordem dos Carrascos:** O braço executor da conspiração. Algozes impiedosos que caçam combatentes já debilitados, arrastando-os ao cadafalso para alimentar a fábrica com novas cobaias e peças.

---

## 2. Leis de Design e Limites Rígidos

### 2.1 O que o Preto FAZ
* **Monopólio da Ressurreição & Descarte:** Única cor autorizada a trazer Heróis de volta da morte (*Ritos Fúnebres*) ou conjurar cartas do descarte através de sacrifício (*Exumação*).
* **A Mecânica "Ferido":** Habilidade-chave de todo o arquétipo. Se o Herói Inimigo estiver com a Vida abaixo do seu valor máximo (`vidaAtual < vidaMaxima`), as cartas pretas disparam efeitos devastadores: anulação de Defesa, concessão de Vulnerabilidade, ataques extras e Dano Direto massivo.
* **Identidade do Dano Direto (Pings Punitivos):** Pings de 1 a 2 de dano no jogo pertencem ao Preto sob a forma de Dano Direto (ignora Defesa, Espírito e Redução de Dano).
* **Dreno e Vampirismo:** Sustentação obtida através da drenagem da vitalidade inimiga (*Homúnculo Carniceiro*, *Colheita dos Condenados*).
* **Corrupção e Debuffs Permanentes:** Redução gradual e definitiva de Vida Máxima, Ataque e Espírito do oponente (*Quimera Negra*, *Atmosfera Pesada*).

### 2.2 O que o Preto NUNCA FAZ (Proibições)
1. **Sem Cura Pura/Altruísta:** O Preto **NUNCA** cura de forma limpa ou sagrada; sua cura provém exclusivamente de drenagem de sangue/vida do inimigo ou colheita de cadáveres.
2. **Sem Escudos de Bloqueio Honrado:** O Preto não usa escudos defensivos convencionais; sua sobrevivência baseia-se em punir os feridos, drenar vida ou retornar da sepultura.
3. **Sem Caster Físico:** Embora possua manobras de cadafalso, o dano físico puro provém de Armas (como a *Espada do Carrasco*); o dano que ignora mitigação deve ser tipado como Dano Direto.

---

## 3. Sub-Arquétipos

### Arquétipo A: Legião Necromante (Zumbis / Construtos de Restos Mortais / Dreno)

* **Conceito & Lore:** Conspiradores e cultistas que fundem restos mortais a carcaças de constructos com Poeira negra residual. Criam um exército de Zumbis e Quimeras carniceiras que se fortalecem com o acúmulo de mortes em campo.
* **Estilo de Jogo:** **Swarm, Dreno e Guerra de Atrito.** Constrói uma presença implacável de aliados Zumbis, drena vida a cada rodada e pune o adversário corrompendo seus atributos base.
* **Heróis Centrais:**
  * [`Rainha Goblin`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L173) (*Transformador | Goblin, Zumbi*): Curandeira necrótica de aliados que faz a ponte entre o enxame goblin e as abominações da fábrica.
  * [`Necromante das Areias`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L223) (*Emissor + Transformador | Zumbi*): Mestre ritualístico que invoca *Ritos Fúnebres* no início do combate para trazer companheiros de volta.
  * [`Quimera Carniceira`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L199) (*Transformador | Zumbi, Constructo*): Abominação de retalhos que escala Ataque, Espírito e cura passiva proporcionalmente ao número de aliados Zumbis em jogo.
* **Mecânicas Centrais:**
  * **Sinergia Zumbi:** Cartas e habilidades que contam a quantidade de Zumbis no time para multiplicar efeitos.
  * **Drenagem de Vida Contínua:** Pings de Dano Direto combinados com ganho de vida no final do turno (*Homúnculo Carniceiro*).
  * **Corrupção de Atributos:** Enfraquecimento definitivo dos recursos do oponente (*Atmosfera Pesada*, *Quimera Negra*).
* **Equipamento Lendário Único:**
  * `Filactéria das Areias Negras` (*Joia - Zumbi*): Relicário que converte a presença e morte de zumbis em drenagem de vida direta no encerramento de cada turno.
* **As 10 Cartas do Baralho:**
  1. `Filactéria das Areias Negras` (Joia - Lendária Única)
  2. `Homunculo Carniceiro` (Joia)
  3. `Amuleto de Ossos da Fábrica` (Joia)
  4. `Quimera Negra` (Arma - Simples)
  5. `Manoplas do Necromante` (Armadura Única)
  6. `Atmosfera Pesada` (Feitiço - Afinidade Ar)
  7. `Peste das Areias Negras` (Feitiço - Afinidade Ar)
  8. `Ritos Fúnebres` (Ação)
  9. `Exumação` (Ação)
  10. `Pilhagem de Cadáveres` (Ação)

---

### Arquétipo B: Ordem dos Carrascos (Algozes do Cadafalso / Executores de Feridos)

* **Conceito & Lore:** Os executores juramentados da conspiração do Rei Demônio. Armados com cutelos pesados e guilhotinas, são enviados para abater qualquer líder ou herói que já tenha sido ferido em combate, ceifando vidas sem permitir escape.
* **Estilo de Jogo:** **Controle Agressivo e Finalização Implacável.** Especializado em aplicar o primeiro ferimento e, em seguida, desencadear uma cascata de punições contra o alvo ferido.
* **Heróis Centrais:**
  * [`Chefe Carrasco`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L876) (*Criador*): O executor supremo. Invoca a *Espada do Carrasco* e reduz a Defesa do oponente ferido a 0.
  * [`Izeref, Executor da Ordem Necrótica`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L903) (*Criador*): Rompe a mecânica padrão do jogo através de sua aura: o inimigo é considerado morto imediatamente se sua vida cair abaixo de zero.
  * [`Constructo Carniceiro`](file:///C:/Users/BARCA02/Desktop/lendas-da-poeira/backend/src/main/resources/heros.json#L958) (*Emissor | Constructo*): Constructo de choque que amplifica qualquer dano sofrido pelos inimigos (+1 de dano adicional).
* **Mecânicas Centrais:**
  * **Gatilho de "Ferido":** Ativação de bônus letais como Vulnerabilidade (*Sentença da Guilhotina*), Ataque bônus (*Cutelo do Suplício*) e Dano Direto (*Cheiro de Sangue*, *Arrastar para o Cadafalso*).
  * **Ataques a Heróis Fora de Combate:** Habilidade de atingir ou arrastar heróis feridos que estão no banco de reservas (*Seguindo Ordens*, *Arrastar para o Cadafalso*).
  * **Armas Pesadas de Empunhadura Dupla:** Uso massivo de armas de 2 slots para maximizar o golpe de misericórdia.
* **Equipamento Lendário Único:**
  * `Espada do Carrasco` (*Arma - Empunhadura Dupla*): Concede alto Ataque e recupera 5 de Vida caso o oponente tenha 0 ou menos de Vida no encerramento da batalha.
* **As 10 Cartas do Baralho:**
  1. `Espada do Carrasco` (Arma Dupla - Lendária do Chefe)
  2. `Cutelo do Suplício` (Arma Simples)
  3. `Manto de Ferro do Algoz` (Armadura Única)
  4. `Correntes do Cadafalso` (Ferramenta)
  5. `Seguindo Ordens` (Ação)
  6. `Grilhões Pesados` (Ação)
  7. `Cheiro de Sangue` (Ação)
  8. `Arrastar para o Cadafalso` (Ação)
  9. `Colheita dos Condenados` (Ação)
  10. `Sentença da Guilhotina` (Ação)

---

## 4. Matriz de Deckbuilding do Preto

| Componente | Legião Necromante | Ordem dos Carrascos |
| :--- | :--- | :--- |
| **Arquétipo Funcional** | Swarm / Dreno / Degeneração | Executor de Alvos Debilitados |
| **Gatilho Central** | Contagem de Zumbis e Morte | Condição "Alvo Ferido" |
| **Tipo de Dano Primário** | Dano Direto & Dano Mágico de Ar | Dano Físico Pesado apoiado por Dano Direto |
| **Mecânica de Sobrevivência** | Ressurreição e Dreno de Vida | Eliminar o oponente antes do contra-ataque |
| **Itens Primários** | Joias necróticas e Manoplas de retalhos | Armas de Empunhadura Dupla e Cutelos |
| **Interação com Descarte** | Recupera cartas e heróis da sepultura | Utiliza o cemitério inimigo como troféu |

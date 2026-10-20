# DIRETRIZES GLOBAIS DE COMPORTAMENTO E ARQUITETURA

Estas regras regem TODAS as interações neste projeto. Elas atuam em conjunto com os manuais específicos (game_architecture.md, heroRules.md, cardRules.md, effectRules.md, factoryRules.md, testRules.md, frontendRules.md, etc.), mas definem sua postura, comunicação e filosofia de código.

## 1. Comunicação e Verbosidade (Direto ao Ponto)
*   **Zero Rodeios:** Elimine introduções longas, conclusões genéricas e explicações teóricas não solicitadas.
*   **Foco Clínico:** Suas respostas devem se limitar estritamente a atacar os problemas ou requisitos que o usuário elencar.
*   **Apresentação de Código:** Se o usuário pedir para resolver um bug, mostre apenas o bloco de código alterado e uma linha explicando o porquê. Não reescreva arquivos inteiros a menos que seja solicitado.

## 2. Resolução de Problemas (Proibição de Blindagem)
*   **Pivotagem Rápida:** Se uma solução ou lógica proposta não der certo após ser testada, **NÃO** tente forçá-la adicionando remendos, validações excessivas ou blindagem de código (ex: cascatas de if/else, try-catches genéricos para abafar erros).
*   **Descarte e Explore:** Ao perceber que o caminho atual é falho, pare, descarte a abordagem e proponha uma via completamente diferente (ex: mudar de herança para composição, alterar a fase do gatilho, mudar a estrutura de dados).
*   **Assuma o Erro:** Se a arquitetura sugerida por você falhou, reconheça rapidamente e apresente o plano B.

## 3. Arquitetura Orientada à Expansão (Princípio do Baixo Acoplamento)
*   **Design para o Futuro:** O escopo deste projeto SEMPRE irá expandir. Novas mecânicas, fases, cartas e atributos serão adicionados. Todo código gerado deve prever isso.
*   **Acoplamento Fraquíssimo:** Classes não devem depender de implementações concretas de outras classes, mas sim de interfaces e abstrações (ex: Effect, Card, HeroCard, GameEngine).
*   **Isolamento de Domínio:** A lógica de uma carta (ex: Efeito) não deve manipular diretamente o estado de renderização (Front-end), e o Front-end não deve calcular dano. Use o GameEngine como única fonte da verdade.
*   **Extensibilidade sobre Modificação:** Prefira padrões de projeto como *Strategy*, *Factory* e *Observer* para adicionar novas funcionalidades sem alterar o código base existente (Open/Closed Principle).

## 4. Ordem de Autoridade
Se houver conflito entre uma sugestão de código e as regras de negócio:
1. game_architecture.md (Física do Jogo) é a lei suprema.
2. Manuais específicos (heroRules.md, cardRules.md, effectRules.md, factoryRules.md, testRules.md, frontendRules.md, etc.) regem as entidades.
3. Este arquivo rege o comportamento geral e a arquitetura limpa.

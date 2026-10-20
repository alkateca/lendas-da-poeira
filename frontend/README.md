# Lendas da Poeira - FrontEnd (Phaser 3 + Vite)

Projeto inicial configurado com **Phaser 3** e empacotador **Vite** para desenvolvimento rápido de jogos com Hot Reload.

---

## 🚀 Comandos Disponíveis

| Comando | Descrição |
|---|---|
| `npm run dev` | Inicia o servidor local de desenvolvimento (com Hot Module Replacement) |
| `npm run build` | Gera o build de produção otimizado na pasta `dist/` |
| `npm run preview` | Pré-visualiza localmente os arquivos gerados em `dist/` |

---

## 📁 Estrutura de Arquivos

```text
├── public/                 # Arquivos estáticos servidos diretamente
│   ├── assets/             # Imagens, sons e spritesheets do jogo
│   └── style.css           # Estilização global da página
├── src/
│   ├── main.js             # Ponto de entrada da aplicação web
│   └── game/
│       ├── main.js         # Configurações do Phaser (resolução, física, cenas)
│       └── scenes/         # Cenas do jogo
│           ├── Boot.js     # Carregamento inicial de assets mínimos
│           ├── Preloader.js# Barra de progresso e carregamento de assets
│           ├── MainMenu.js # Menu principal
│           ├── Game.js     # Cena principal do jogo
│           └── GameOver.js # Tela de fim de jogo
├── vite/                   # Configurações do Vite (desenvolvimento e produção)
├── index.html              # Container HTML do jogo
└── package.json            # Dependências e scripts
```

---

## 🎮 Como Começar

1. Para rodar em desenvolvimento:
   ```bash
   npm run dev
   ```
2. Abra seu navegador em `http://localhost:8080` (ou na porta indicada no terminal).
3. Modifique os arquivos em `src/game/scenes/` para criar a mecânica do jogo.

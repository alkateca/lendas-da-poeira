# Lendas da Poeira

Monorepo contendo o backend (Spring Boot / Java) e o frontend (Phaser 3 / Vite) do jogo **Lendas da Poeira**.

---

## 📁 Estrutura do Repositório

```text
lendas-da-poeira/
├── backend/       # API e regras de negócio em Java (Spring Boot)
├── frontend/      # Interface e jogo em Phaser 3 + Vite
├── .gitignore
└── README.md
```

---

## 🚀 Como Executar

### Forma Rápida (Script Unificado)

Basta rodar o script na raiz do repositório:

```bash
# Inicia ou reinicia ambos os serviços automaticamente
./start.sh
```

Comandos adicionais do script:
```bash
./start.sh restart  # Força reinicialização do backend e frontend
./start.sh status   # Exibe o status e portas dos serviços
./start.sh logs     # Acompanha os logs em tempo real
./start.sh stop     # Para todos os serviços
```

* **Frontend:** http://localhost:5173
* **Backend:**  http://localhost:8080

---

### Execução Individual Manual

### 1. Backend (Java / Spring Boot)
Pré-requisitos: JDK 17+ instalado.

```bash
cd backend
./mvnw spring-boot:run
```

### 2. Frontend (Phaser 3 / Vite)
Pré-requisitos: Node.js (v18+) e npm instalados.

```bash
cd frontend
npm install
npm run dev
```

O jogo estará disponível no navegador através do endereço exibido pelo Vite (normalmente `http://localhost:5173/` ou similar).

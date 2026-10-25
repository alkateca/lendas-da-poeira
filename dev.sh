#!/usr/bin/env bash

# ==============================================================================
# Script de Gerenciamento: Lendas da Poeira (Backend + Frontend)
# ==============================================================================

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"
PID_DIR="$ROOT_DIR/.pids"
LOG_DIR="$ROOT_DIR/logs"

BACKEND_PID_FILE="$PID_DIR/backend.pid"
FRONTEND_PID_FILE="$PID_DIR/frontend.pid"
BACKEND_LOG="$LOG_DIR/backend.log"
FRONTEND_LOG="$LOG_DIR/frontend.log"

BACKEND_PORT=8080
FRONTEND_PORT=5173

# Cores
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

mkdir -p "$PID_DIR" "$LOG_DIR"
chmod +x "$BACKEND_DIR/mvnw" 2>/dev/null || true

# Configura JAVA_HOME automaticamente caso não esteja definido
if [ -z "$JAVA_HOME" ] || [ ! -d "$JAVA_HOME" ]; then
    if [ -d "/c/Program Files/Java/jdk-25" ]; then
        export JAVA_HOME="/c/Program Files/Java/jdk-25"
    elif [ -d "/usr/lib/jvm/java-17-openjdk-amd64" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
    elif [ -d "/usr/lib/jvm/default-java" ]; then
        export JAVA_HOME="/usr/lib/jvm/default-java"
    elif command -v java >/dev/null 2>&1; then
        export JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"
    fi
fi
if [ -n "$JAVA_HOME" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

is_pid_running() {
    local pid="$1"
    [ -z "$pid" ] && return 1
    if kill -0 "$pid" 2>/dev/null; then
        return 0
    fi
    if command -v tasklist >/dev/null 2>&1; then
        tasklist //FI "PID eq $pid" 2>/dev/null | grep -q "$pid" && return 0
    fi
    return 1
}

is_port_in_use() {
    local port="$1"
    if command -v fuser >/dev/null 2>&1 && fuser "${port}/tcp" >/dev/null 2>&1; then
        return 0
    fi
    if command -v netstat >/dev/null 2>&1; then
        netstat -ano 2>/dev/null | grep -E "(:|\.)${port}[[:space:]]" | grep -i "LISTENING" >/dev/null 2>&1 && return 0
    fi
    return 1
}

is_backend_running() {
    if [ -f "$BACKEND_PID_FILE" ] && is_pid_running "$(cat "$BACKEND_PID_FILE" 2>/dev/null)"; then
        return 0
    fi
    is_port_in_use "$BACKEND_PORT"
}

is_frontend_running() {
    if [ -f "$FRONTEND_PID_FILE" ] && is_pid_running "$(cat "$FRONTEND_PID_FILE" 2>/dev/null)"; then
        return 0
    fi
    is_port_in_use "$FRONTEND_PORT"
}

stop_service() {
    local name="$1"
    local pid_file="$2"
    local port="$3"

    echo -e "${YELLOW}Parando $name...${NC}"

    if [ -f "$pid_file" ]; then
        local pid
        pid=$(cat "$pid_file" 2>/dev/null)
        if is_pid_running "$pid"; then
            if command -v pkill >/dev/null 2>&1; then
                pkill -P "$pid" 2>/dev/null || true
            fi
            kill "$pid" 2>/dev/null || true
            sleep 1
            if is_pid_running "$pid"; then
                kill -9 "$pid" 2>/dev/null || true
            fi
        fi
        rm -f "$pid_file"
    fi

    # Garante que a porta associada foi liberada
    if [ -n "$port" ]; then
        if command -v fuser >/dev/null 2>&1; then
            fuser -k "${port}/tcp" >/dev/null 2>&1 || true
        elif command -v netstat >/dev/null 2>&1 && command -v taskkill >/dev/null 2>&1; then
            local pids
            pids=$(netstat -ano 2>/dev/null | grep -E "(:|\.)${port}[[:space:]]" | grep -i "LISTENING" | awk '{print $NF}' | sort -u)
            for p in $pids; do
                if [ -n "$p" ] && [ "$p" -ne 0 ] 2>/dev/null; then
                    taskkill //F //PID "$p" >/dev/null 2>&1 || true
                fi
            done
        fi
    fi

    echo -e "${GREEN}✓ $name parado.${NC}"
}

stop_all() {
    echo -e "${BLUE}=== Parando Lendas da Poeira ===${NC}"
    stop_service "Frontend (Phaser / Vite)" "$FRONTEND_PID_FILE" "$FRONTEND_PORT"
    stop_service "Backend (Spring Boot)" "$BACKEND_PID_FILE" "$BACKEND_PORT"
}

start_backend() {
    echo -e "${CYAN}Iniciando Backend (Java / Spring Boot na porta $BACKEND_PORT)...${NC}"
    cd "$BACKEND_DIR" || exit 1
    if command -v setsid >/dev/null 2>&1; then
        setsid ./mvnw spring-boot:run < /dev/null > "$BACKEND_LOG" 2>&1 &
    elif command -v nohup >/dev/null 2>&1; then
        nohup ./mvnw spring-boot:run < /dev/null > "$BACKEND_LOG" 2>&1 &
    else
        ./mvnw spring-boot:run < /dev/null > "$BACKEND_LOG" 2>&1 &
    fi
    local pid=$!
    disown "$pid" 2>/dev/null || true
    echo "$pid" > "$BACKEND_PID_FILE"
    cd "$ROOT_DIR" || exit 1
}

start_frontend() {
    echo -e "${CYAN}Iniciando Frontend (Phaser 3 / Vite na porta $FRONTEND_PORT)...${NC}"
    cd "$FRONTEND_DIR" || exit 1
    if [ ! -d "node_modules" ]; then
        echo -e "${YELLOW}node_modules não encontrado. Executando npm install...${NC}"
        npm install
    fi
    if command -v setsid >/dev/null 2>&1; then
        setsid npm run dev < /dev/null > "$FRONTEND_LOG" 2>&1 &
    elif command -v nohup >/dev/null 2>&1; then
        nohup npm run dev < /dev/null > "$FRONTEND_LOG" 2>&1 &
    else
        npm run dev < /dev/null > "$FRONTEND_LOG" 2>&1 &
    fi
    local pid=$!
    disown "$pid" 2>/dev/null || true
    echo "$pid" > "$FRONTEND_PID_FILE"
    cd "$ROOT_DIR" || exit 1
}

start_all() {
    echo -e "${BLUE}=== Iniciando Lendas da Poeira ===${NC}"
    start_backend
    start_frontend

    echo -e "${YELLOW}Aguardando inicialização dos serviços...${NC}"
    sleep 3

    echo ""
    echo -e "${GREEN}====================================================${NC}"
    echo -e "${GREEN}✓ Serviços iniciados em segundo plano!${NC}"
    echo -e "  - ${BLUE}Frontend:${NC} http://localhost:${FRONTEND_PORT}"
    echo -e "  - ${BLUE}Backend:${NC}  http://localhost:${BACKEND_PORT}"
    echo -e "${GREEN}====================================================${NC}"
    echo ""
    echo -e "Comandos disponíveis:"
    echo -e "  ${YELLOW}./dev.sh logs${NC}      -> Acompanhar logs em tempo real"
    echo -e "  ${YELLOW}./dev.sh status${NC}    -> Ver status dos serviços"
    echo -e "  ${YELLOW}./dev.sh restart${NC}   -> Reiniciar backend e frontend"
    echo -e "  ${YELLOW}./dev.sh stop${NC}      -> Parar todos os serviços"
    echo ""
}

restart_all() {
    echo -e "${BLUE}=== Reiniciando Lendas da Poeira ===${NC}"
    stop_all
    sleep 1
    start_all
}

status_all() {
    echo -e "${BLUE}=== Status dos Serviços ===${NC}"
    
    if is_backend_running; then
        local b_pid="N/A"
        [ -f "$BACKEND_PID_FILE" ] && b_pid=$(cat "$BACKEND_PID_FILE")
        echo -e "Backend (Spring Boot):  ${GREEN}RODANDO${NC} (PID: $b_pid, Porta: $BACKEND_PORT)"
    else
        echo -e "Backend (Spring Boot):  ${RED}PARADO${NC}"
    fi

    if is_frontend_running; then
        local f_pid="N/A"
        [ -f "$FRONTEND_PID_FILE" ] && f_pid=$(cat "$FRONTEND_PID_FILE")
        echo -e "Frontend (Vite/Phaser): ${GREEN}RODANDO${NC} (PID: $f_pid, Porta: $FRONTEND_PORT)"
    else
        echo -e "Frontend (Vite/Phaser): ${RED}PARADO${NC}"
    fi
}

show_logs() {
    local target="$1"
    if [ "$target" = "backend" ]; then
        tail -f "$BACKEND_LOG"
    elif [ "$target" = "frontend" ]; then
        tail -f "$FRONTEND_LOG"
    else
        echo -e "${CYAN}Exibindo logs de ambos (Ctrl+C para sair)...${NC}"
        tail -f "$BACKEND_LOG" "$FRONTEND_LOG"
    fi
}

case "$1" in
    start)
        if is_backend_running || is_frontend_running; then
            echo -e "${YELLOW}Serviços já estão em execução. Reiniciando...${NC}"
            restart_all
        else
            start_all
        fi
        ;;
    restart)
        restart_all
        ;;
    stop)
        stop_all
        ;;
    status)
        status_all
        ;;
    logs)
        show_logs "$2"
        ;;
    *)
        # Comportamento padrão: Se já estiver rodando, reinicia; senão, inicia.
        if is_backend_running || is_frontend_running; then
            echo -e "${YELLOW}Um ou mais serviços já detectados em execução. Reiniciando...${NC}"
            restart_all
        else
            start_all
        fi
        ;;
esac

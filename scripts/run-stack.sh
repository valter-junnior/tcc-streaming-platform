#!/bin/bash

# Script para executar diferentes stacks de tecnologia do projeto de streaming
# Permite testar e comparar diferentes combinações de tecnologias

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
APP_DIR="$PROJECT_ROOT/app"

cd "$APP_DIR"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_usage() {
    echo -e "${BLUE}Script para executar diferentes stacks de tecnologia${NC}"
    echo ""
    echo "Uso: $0 [COMANDO] [STACK]"
    echo ""
    echo "Comandos:"
    echo "  up         - Iniciar stack"
    echo "  down       - Parar stack"
    echo "  restart    - Reiniciar stack"
    echo "  logs       - Ver logs"
    echo "  status     - Ver status dos containers"
    echo "  clean      - Parar e remover volumes"
    echo "  list       - Listar stacks disponíveis"
    echo ""
    echo "Stacks disponíveis:"
    echo "  default           - Nginx-RTMP + FFmpeg (padrão)"
    echo "  srs              - SRS + transcodificação interna"
    echo "  gstreamer        - Nginx-RTMP + GStreamer"
    echo ""
    echo "Exemplos:"
    echo "  $0 up default"
    echo "  $0 up srs"
    echo "  $0 logs gstreamer"
    echo "  $0 down"
}

check_requirements() {
    if ! command -v docker &> /dev/null; then
        echo -e "${RED}ERRO: Docker não encontrado. Instale o Docker primeiro.${NC}"
        exit 1
    fi

    if ! command -v docker-compose &> /dev/null; then
        echo -e "${RED}ERRO: Docker Compose não encontrado. Instale o Docker Compose primeiro.${NC}"
        exit 1
    fi

    if [ ! -f ".env" ]; then
        if [ -f ".env.example" ]; then
            echo -e "${YELLOW}Arquivo .env não encontrado. Copiando .env.example...${NC}"
            cp .env.example .env
        else
            echo -e "${RED}ERRO: Arquivo .env não encontrado e .env.example não existe.${NC}"
            exit 1
        fi
    fi
}

get_compose_file() {
    local stack="$1"
    case "$stack" in
        "default"|"nginx"|"nginx-rtmp"|"ffmpeg")
            echo "docker-compose.yml"
            ;;
        "srs")
            echo "docker-compose.srs.yml"
            ;;
        "gstreamer")
            echo "docker-compose.gstreamer.yml"
            ;;
        *)
            echo -e "${RED}ERRO: Stack inválida: $stack${NC}"
            echo "Use '$0 list' para ver stacks disponíveis."
            exit 1
            ;;
    esac
}

list_stacks() {
    echo -e "${BLUE}Stacks disponíveis:${NC}"
    echo ""
    echo -e "${GREEN}1. default (padrão)${NC}"
    echo "   - RTMP Server: Nginx-RTMP"
    echo "   - Transcodificação: FFmpeg"
    echo "   - Message Broker: RabbitMQ"
    echo "   - Cache: Redis"
    echo ""
    echo -e "${GREEN}2. srs${NC}"
    echo "   - RTMP Server: SRS (Simple Realtime Server)"
    echo "   - Transcodificação: SRS interno"
    echo "   - Message Broker: RabbitMQ"
    echo "   - Cache: Redis"
    echo ""
    echo -e "${GREEN}3. gstreamer${NC}"
    echo "   - RTMP Server: Nginx-RTMP"
    echo "   - Transcodificação: GStreamer"
    echo "   - Message Broker: RabbitMQ"
    echo "   - Cache: Redis"
    echo ""
    echo "Para executar uma stack, use: $0 up <stack_name>"
}

# Comando principal
COMMAND=${1:-"help"}
STACK=${2:-"default"}

case "$COMMAND" in
    "help"|"--help"|"-h")
        print_usage
        ;;
    "list")
        list_stacks
        ;;
    "up")
        check_requirements
        COMPOSE_FILE=$(get_compose_file "$STACK")
        echo -e "${BLUE}Iniciando stack: $STACK${NC}"
        echo -e "${YELLOW}Arquivo compose: $COMPOSE_FILE${NC}"
        docker-compose -f "$COMPOSE_FILE" up -d --build
        echo -e "${GREEN}Stack $STACK iniciada com sucesso!${NC}"
        echo ""
        echo -e "${YELLOW}URLs de acesso:${NC}"
        echo "  Frontend: http://localhost:3001"
        echo "  Backend API: http://localhost:8080"
        echo "  RabbitMQ Management: http://localhost:15672"
        if [ "$STACK" = "srs" ]; then
            echo "  SRS HTTP API: http://localhost:8080"
        fi
        echo ""
        echo "Use '$0 logs $STACK' para ver os logs"
        ;;
    "down")
        if [ "$STACK" != "default" ]; then
            COMPOSE_FILE=$(get_compose_file "$STACK")
            echo -e "${YELLOW}Parando stack: $STACK${NC}"
            docker-compose -f "$COMPOSE_FILE" down
        else
            echo -e "${YELLOW}Parando todas as stacks...${NC}"
            docker-compose down 2>/dev/null || true
            docker-compose -f docker-compose.srs.yml down 2>/dev/null || true
            docker-compose -f docker-compose.gstreamer.yml down 2>/dev/null || true
        fi
        echo -e "${GREEN}Stack parada com sucesso!${NC}"
        ;;
    "restart")
        check_requirements
        COMPOSE_FILE=$(get_compose_file "$STACK")
        echo -e "${YELLOW}Reiniciando stack: $STACK${NC}"
        docker-compose -f "$COMPOSE_FILE" restart
        echo -e "${GREEN}Stack $STACK reiniciada com sucesso!${NC}"
        ;;
    "logs")
        COMPOSE_FILE=$(get_compose_file "$STACK")
        echo -e "${BLUE}Logs da stack: $STACK${NC}"
        docker-compose -f "$COMPOSE_FILE" logs -f --tail=50
        ;;
    "status")
        COMPOSE_FILE=$(get_compose_file "$STACK")
        echo -e "${BLUE}Status da stack: $STACK${NC}"
        docker-compose -f "$COMPOSE_FILE" ps
        ;;
    "clean")
        echo -e "${RED}ATENÇÃO: Isso removerá todos os containers, imagens e volumes!${NC}"
        read -p "Tem certeza? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo -e "${YELLOW}Parando e removendo tudo...${NC}"
            docker-compose down -v --rmi all 2>/dev/null || true
            docker-compose -f docker-compose.srs.yml down -v --rmi all 2>/dev/null || true
            docker-compose -f docker-compose.gstreamer.yml down -v --rmi all 2>/dev/null || true
            docker system prune -f
            echo -e "${GREEN}Limpeza completa realizada!${NC}"
        else
            echo -e "${YELLOW}Operação cancelada.${NC}"
        fi
        ;;
    *)
        echo -e "${RED}Comando inválido: $COMMAND${NC}"
        print_usage
        exit 1
        ;;
esac
#!/bin/bash

set -e

COMPOSE_FILE="docker-compose.yml"
VALID_PROFILES=("nginx" "srs")

usage() {
  echo "Uso: ./run.sh [comando] [perfil]"
  echo ""
  echo "Comandos:"
  echo "  up   <perfil>   Sobe o ambiente com o servidor RTMP especificado"
  echo "  down <perfil>   Derruba o ambiente do servidor RTMP especificado"
  echo "  swap <perfil>   Troca o servidor RTMP ativo pelo especificado"
  echo "  logs <perfil>   Exibe os logs do servidor RTMP especificado"
  echo "  status          Exibe o status de todos os containers"
  echo ""
  echo "Perfis disponíveis: nginx, srs"
  echo ""
  echo "Exemplos:"
  echo "  ./run.sh up nginx"
  echo "  ./run.sh swap srs"
  echo "  ./run.sh down nginx"
}

is_valid_profile() {
  local profile="$1"
  for p in "${VALID_PROFILES[@]}"; do
    [[ "$p" == "$profile" ]] && return 0
  done
  return 1
}

get_other_profile() {
  [[ "$1" == "nginx" ]] && echo "srs" || echo "nginx"
}

set_env_profile() {
  local profile="$1"
  if grep -q "^COMPOSE_PROFILES=" .env 2>/dev/null; then
    sed -i "s/^COMPOSE_PROFILES=.*/COMPOSE_PROFILES=$profile/" .env
  else
    echo "COMPOSE_PROFILES=$profile" >> .env
  fi
}

cmd_up() {
  local profile="$1"
  set_env_profile "$profile"
  echo ">> Subindo ambiente com perfil: $profile"
  docker compose -f "$COMPOSE_FILE" up -d --build
}

cmd_down() {
  local profile="$1"
  echo ">> Encerrando streams LIVE antes do shutdown..."
  curl -s -X POST http://localhost:8080/api/streams/cleanup/stale 2>/dev/null \
    && echo "   Streams encerradas via API." \
    || echo "   Backend indisponível, ignorando cleanup de streams."
  echo ">> Derrubando ambiente com perfil: $profile"
  docker compose -f "$COMPOSE_FILE" down
}

cmd_swap() {
  local new_profile="$1"
  local old_profile
  old_profile=$(get_other_profile "$new_profile")

  echo ">> Trocando de '$old_profile' para '$new_profile'..."
  echo ""

  echo ">> [1/2] Derrubando '$old_profile'..."
  echo ">> Encerrando streams LIVE antes do shutdown..."
  curl -s -X POST http://localhost:8080/api/streams/cleanup/stale 2>/dev/null \
    && echo "   Streams encerradas via API." \
    || echo "   Backend indisponível, ignorando cleanup de streams."
  docker compose -f "$COMPOSE_FILE" down

  set_env_profile "$new_profile"

  echo ""
  echo ">> [2/2] Subindo '$new_profile'..."
  docker compose -f "$COMPOSE_FILE" up -d --build

  echo ""
  echo ">> Troca concluída! Servidor RTMP ativo: $new_profile"
}

cmd_logs() {
  local profile="$1"
  local service="rtmp-server-$profile"
  echo ">> Exibindo logs de: $service"
  docker compose -f "$COMPOSE_FILE" logs -f "$service"
}

cmd_status() {
  echo ">> Status dos containers:"
  docker compose -f "$COMPOSE_FILE" ps
}

# --- Main ---

COMMAND="${1:-}"
PROFILE="${2:-}"

case "$COMMAND" in
  up|down|swap|logs)
    if [[ -z "$PROFILE" ]]; then
      echo "Erro: informe um perfil (nginx ou srs)"
      echo ""
      usage
      exit 1
    fi
    if ! is_valid_profile "$PROFILE"; then
      echo "Erro: perfil '$PROFILE' inválido. Use 'nginx' ou 'srs'."
      exit 1
    fi
    "cmd_$COMMAND" "$PROFILE"
    ;;
  status)
    cmd_status
    ;;
  help|--help|-h)
    usage
    ;;
  *)
    usage
    exit 1
    ;;
esac

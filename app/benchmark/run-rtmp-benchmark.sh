#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${APP_DIR}/docker-compose.yml"
ENV_FILE="${APP_DIR}/.env"
RESULTS_ROOT="${SCRIPT_DIR}/results"
DOCS_BENCHMARK_DIR="$(cd "${APP_DIR}/.." && pwd)/docs/benchmark"

MODE="sanity"
SCENARIO_FILTER="all"
DURATION_SECONDS=90
VIEWERS_SINGLE=1
VIEWERS_LIST="1,10,50"
REPEATS=3
RETRIES=1
BACKEND_PORT=8080
RTMP_PORT=1935
HLS_PORT=8081
HLS_HTTP_PORT=8081
KEEP_UP=0
AGGREGATE=0
PUBLISH=0
STATS_PID=0
HOST_VMSTAT_PID=0
PUBLISHER_NAME=""
CONSOLE_LOG_FILE=""
LOG_MIRROR_ENABLED=0

SCENARIOS=(
  "nginx+ffmpeg"
  "nginx+gstreamer"
  "srs+ffmpeg"
  "srs+gstreamer"
)

VIEWER_PIDS=()

CSV_HEADER="run_id,mode,scenario,server,transcoder,repeat_index,viewers,duration_seconds,status,error_message,measurement_start_ts,measurement_end_ts,measurement_window_seconds,startup_hls_seconds,latencia_ponta_a_ponta_s,tempo_primeiro_segmento_s,request_total,request_errors,error_rate_percent,cpu_avg_percent,cpu_max_percent,mem_avg_mb,mem_max_mb,net_in_mb,net_out_mb,bitrate_kbps,restarts_before,restarts_after,retry_index,compose_network,rtmp_container,master_playlist_url,scenario_log_file,metrics_samples_file,viewer_results_dir"

usage() {
  cat <<EOF
Uso: ./benchmark/run-rtmp-benchmark.sh [opcoes]

Opcoes:
  --mode <sanity|full>         Modo de execucao (default: sanity)
  --scenario <all|combinacao>  Filtro de cenario (ex.: nginx+ffmpeg)
  --duration <segundos>        Duracao da carga de viewers por execucao
  --viewers <n>                Numero de viewers (usado no modo sanity)
  --viewers-list <lista>       Lista CSV de viewers (modo full, default: 1,10,50)
  --repeats <n>                Repeticoes por cenario (modo full default: 3)
  --retries <n>                Retries curtos por cenario em caso de falha
  --backend-port <port>        Porta do backend exposta no host (default: 8080)
  --rtmp-port <port>           Porta RTMP interna dos servicos (default: 1935)
  --hls-port <port>            Porta HLS exposta no host (default: 8081)
  --keep-up                    Nao derruba stack no final
  --aggregate                  Apos execucao full, agrega resultados na pasta local da run
  --publish                    Publica CSVs revisados em docs/benchmark/runs/<run_id>/ (requer matriz completa)
  --help                       Exibe ajuda

Exemplos:
  ./benchmark/run-rtmp-benchmark.sh --mode sanity --duration 90
  ./benchmark/run-rtmp-benchmark.sh --mode full --duration 300 --repeats 3
  ./benchmark/run-rtmp-benchmark.sh --mode full --duration 300 --repeats 3 --aggregate
EOF
}

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" >&2
}

die() {
  log "ERRO: $*"
  exit 1
}

sanitize() {
  echo "$1" | tr -c 'a-zA-Z0-9._-' '_'
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "Comando obrigatorio nao encontrado: $1"
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --mode)
        MODE="$2"
        shift 2
        ;;
      --scenario)
        SCENARIO_FILTER="$2"
        shift 2
        ;;
      --duration)
        DURATION_SECONDS="$2"
        shift 2
        ;;
      --viewers)
        VIEWERS_SINGLE="$2"
        shift 2
        ;;
      --viewers-list)
        VIEWERS_LIST="$2"
        shift 2
        ;;
      --repeats)
        REPEATS="$2"
        shift 2
        ;;
      --retries)
        RETRIES="$2"
        shift 2
        ;;
      --backend-port)
        BACKEND_PORT="$2"
        shift 2
        ;;
      --rtmp-port)
        RTMP_PORT="$2"
        shift 2
        ;;
      --hls-port)
        HLS_PORT="$2"
        shift 2
        ;;
      --keep-up)
        KEEP_UP=1
        shift
        ;;
      --aggregate)
        AGGREGATE=1
        shift
        ;;
      --publish)
        PUBLISH=1
        shift
        ;;
      --help|-h)
        usage
        exit 0
        ;;
      *)
        die "Opcao invalida: $1"
        ;;
    esac
  done

  [[ "$MODE" == "sanity" || "$MODE" == "full" ]] || die "--mode deve ser sanity ou full"
  [[ "$DURATION_SECONDS" =~ ^[0-9]+$ ]] || die "--duration deve ser inteiro"
  [[ "$VIEWERS_SINGLE" =~ ^[0-9]+$ ]] || die "--viewers deve ser inteiro"
  [[ "$REPEATS" =~ ^[0-9]+$ ]] || die "--repeats deve ser inteiro"
  [[ "$RETRIES" =~ ^[0-9]+$ ]] || die "--retries deve ser inteiro"
  [[ "$BACKEND_PORT" =~ ^[0-9]+$ ]] || die "--backend-port deve ser inteiro"
  [[ "$RTMP_PORT" =~ ^[0-9]+$ ]] || die "--rtmp-port deve ser inteiro"
  [[ "$HLS_PORT" =~ ^[0-9]+$ ]] || die "--hls-port deve ser inteiro"
  if [[ "$PUBLISH" -eq 1 ]]; then
    [[ "$MODE" == "full" && "$AGGREGATE" -eq 1 && "$SCENARIO_FILTER" == "all" && "$VIEWERS_LIST" == "1,10,50" && "$REPEATS" -eq 3 ]] || die "--publish requer matriz completa (4 cenarios, cargas 1,10,50, tres repeticoes) e --aggregate"
  fi
}

is_port_in_use() {
  local port="$1"
  if command -v ss >/dev/null 2>&1; then
    ss -ltn | awk '{print $4}' | grep -Eq "(^|:)${port}$"
  elif command -v netstat >/dev/null 2>&1; then
    netstat -ltn 2>/dev/null | awk '{print $4}' | grep -Eq "(^|:)${port}$"
  else
    return 1
  fi
}

validate_host_ports() {
  local ports=()
  ports+=("$BACKEND_PORT")
  ports+=("$RTMP_PORT")
  ports+=("$HLS_PORT")

  local p
  for p in "${ports[@]}"; do
    if is_port_in_use "$p"; then
      die "Porta ${p} ja esta em uso no host. Libere a porta ou rode com outra porta (ex.: --hls-port 18081)."
    fi
  done
}

compose() {
  (
    cd "$APP_DIR"
    docker compose -f "$COMPOSE_FILE" "$@"
  )
}

ensure_results() {
  mkdir -p "$RESULTS_ROOT"
  mkdir -p "$DOCS_BENCHMARK_DIR"
}

validate_preflight() {
  require_cmd docker
  if [[ "$PUBLISH" -eq 1 ]]; then
    require_cmd python3
    [[ -f "${SCRIPT_DIR}/export-public-results.py" ]] || die "Exportador publico nao encontrado"
  fi
  require_cmd awk
  require_cmd sed
  [[ -f "$COMPOSE_FILE" ]] || die "docker-compose.yml nao encontrado em $COMPOSE_FILE"
  [[ -f "$ENV_FILE" ]] || die ".env nao encontrado em $ENV_FILE"

  mkdir -p "$RESULTS_ROOT"
  touch "$RESULTS_ROOT/.write_check"
  rm -f "$RESULTS_ROOT/.write_check"

  if ! docker info >/dev/null 2>&1; then
    die "Docker nao esta disponivel"
  fi

  validate_host_ports
}

prepare_run_dirs() {
  local timestamp
  timestamp="$(date '+%Y%m%d-%H%M%S')"
  RUN_ID="bench-${MODE}-${timestamp}"
  RUN_DIR="${RESULTS_ROOT}/${RUN_ID}"
  LOG_DIR="${RUN_DIR}/logs"
  CSV_FILE="${RUN_DIR}/results.csv"
  CONSOLE_LOG_FILE="${LOG_DIR}/run-console.log"
  mkdir -p "$RUN_DIR" "$LOG_DIR"
  echo "$CSV_HEADER" > "$CSV_FILE"
  ln -sfn "$RUN_DIR" "${RESULTS_ROOT}/latest"
}

capture_run_environment() {
  {
    date --iso-8601=seconds
    git -C "$APP_DIR" rev-parse HEAD 2>/dev/null || true
    sha256sum "$SCRIPT_DIR/run-rtmp-benchmark.sh"
    uname -a
    docker version
    docker compose version
    echo "--- host memory ---"
    free -m
    echo "--- active containers before benchmark ---"
    docker ps --format '{{.Names}} {{.Image}} {{.Status}}'
  } > "${RUN_DIR}/environment.txt" 2>&1
}

capture_attempt_environment() {
  local container_name="$1"
  local output_file="$2"
  {
    date --iso-8601=seconds
    docker inspect -f 'container={{.Name}} image_ref={{.Config.Image}} image_id={{.Image}}' "$container_name"
    local image_id
    image_id="$(docker inspect -f '{{.Image}}' "$container_name")"
    docker image inspect -f 'created={{.Created}} repo_digests={{json .RepoDigests}}' "$image_id"
    docker exec "$container_name" sh -c 'ffmpeg -version 2>&1 | head -n 1; gst-launch-1.0 --version 2>&1 | head -n 1; if [ -x /usr/local/nginx/sbin/nginx ]; then /usr/local/nginx/sbin/nginx -v 2>&1; fi; if [ -x /usr/local/srs/objs/srs ]; then /usr/local/srs/objs/srs -v 2>&1 | head -n 3; fi' || true
  } > "$output_file" 2>&1
}

capture_host_snapshot() {
  local output_file="$1"
  {
    date --iso-8601=seconds
    uptime
    free -m
    docker ps --format '{{.Names}} {{.Image}} {{.Status}}'
    docker stats --no-stream --format '{{.Name}} {{.CPUPerc}} {{.MemUsage}}' || true
  } > "$output_file" 2>&1
}

enable_console_log_mirror() {
  if [[ "$LOG_MIRROR_ENABLED" -eq 1 ]]; then
    return
  fi
  LOG_MIRROR_ENABLED=1
  exec > >(tee -a "$CONSOLE_LOG_FILE") 2>&1
}

publish_reports() {
  local aggregate_dir="$1"
  python3 "${SCRIPT_DIR}/audit-results.py" "$RUN_DIR" --require-full >/dev/null || die "Auditoria da matriz falhou; publicacao cancelada"
  local destination="${DOCS_BENCHMARK_DIR}/runs/${RUN_ID}"
  [[ ! -e "$destination" ]] || die "Destino de publicacao ja existe: $destination"
  mkdir -p "$destination"
  python3 "${SCRIPT_DIR}/export-public-results.py" "$CSV_FILE" "${destination}/results.csv"
  cp "${aggregate_dir}/results-aggregated.csv" "${destination}/results-aggregated.csv"
  cp "${aggregate_dir}/resultado.csv" "${destination}/resultado.csv"
}

build_selected_images() {
  local scenario server
  local built_nginx=0 built_srs=0
  for scenario in "${SCENARIOS[@]}"; do
    scenario_selected "$scenario" || continue
    server="${scenario%%+*}"
    if [[ "$server" == "nginx" && "$built_nginx" -eq 1 ]] || [[ "$server" == "srs" && "$built_srs" -eq 1 ]]; then
      continue
    fi
    setup_environment_for_scenario "$server" "ffmpeg"
    log "Construindo imagem RTMP uma vez para ${server}"
    compose build "rtmp-server-${server}" >> "${RUN_DIR}/logs/build.log" 2>&1 || die "Falha ao construir imagem RTMP de ${server}"
    if [[ "$server" == "nginx" ]]; then built_nginx=1; else built_srs=1; fi
  done
}

scenario_selected() {
  local scenario="$1"
  if [[ "$SCENARIO_FILTER" == "all" ]]; then
    return 0
  fi
  [[ "$SCENARIO_FILTER" == "$scenario" ]]
}

build_execution_plan() {
  PLAN_ITEMS=()

  local scenario
  for scenario in "${SCENARIOS[@]}"; do
    scenario_selected "$scenario" || continue

    local viewers_values
    local repeats_value

    if [[ "$MODE" == "sanity" ]]; then
      viewers_values="$VIEWERS_SINGLE"
      repeats_value=1
    else
      viewers_values="$VIEWERS_LIST"
      repeats_value="$REPEATS"
    fi

    IFS=',' read -r -a viewers_arr <<< "$viewers_values"
    local viewers
    for viewers in "${viewers_arr[@]}"; do
      viewers="${viewers// /}"
      [[ "$viewers" =~ ^[0-9]+$ ]] || die "viewers invalido no plano: $viewers"
      local r
      for ((r=1; r<=repeats_value; r++)); do
        PLAN_ITEMS+=("${scenario}|${viewers}|${r}")
      done
    done
  done

  [[ ${#PLAN_ITEMS[@]} -gt 0 ]] || die "Nenhum cenario selecionado"
}

compose_down_quiet() {
  compose down --remove-orphans >/dev/null 2>&1 || true
}

cleanup() {
  if [[ -n "${PUBLISHER_NAME:-}" ]]; then
    docker stop -t 5 "$PUBLISHER_NAME" >/dev/null 2>&1 || true
  fi
  if [[ "${KEEP_UP:-0}" -eq 0 ]]; then
    compose_down_quiet
  fi
  if [[ "${STATS_PID:-0}" -gt 0 ]]; then
    kill "$STATS_PID" 2>/dev/null || true
  fi
  if [[ "${HOST_VMSTAT_PID:-0}" -gt 0 ]]; then
    kill "$HOST_VMSTAT_PID" 2>/dev/null || true
  fi
}

setup_environment_for_scenario() {
  local server="$1"
  local transcoder="$2"

  export COMPOSE_PROFILES="$server"
  export TRANSCODER="$transcoder"
  export BACKEND_PORT
  export RTMP_PORT
  export HLS_PORT
  export HLS_HTTP_PORT
}

wait_backend_ready() {
  local timeout=240
  local elapsed=0
  while (( elapsed < timeout )); do
    if docker run --rm --network host curlimages/curl:8.7.1 -sS "http://localhost:${BACKEND_PORT}/actuator/health" >/tmp/bench-health.json 2>/dev/null; then
      if grep -q '"status"' /tmp/bench-health.json; then
        rm -f /tmp/bench-health.json
        return 0
      fi
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
  rm -f /tmp/bench-health.json
  return 1
}

wait_container_running() {
  local container_name="$1"
  local timeout=120
  local elapsed=0
  while (( elapsed < timeout )); do
    local state
    state="$(docker inspect -f '{{.State.Running}}' "$container_name" 2>/dev/null || echo "false")"
    if [[ "$state" == "true" ]]; then
      return 0
    fi
    sleep 2
    elapsed=$((elapsed + 2))
  done
  return 1
}

get_compose_network() {
  local backend_id
  backend_id="$(compose ps -q streaming-platform | head -n 1)"
  [[ -n "$backend_id" ]] || return 1
  docker inspect -f '{{range $name, $_ := .NetworkSettings.Networks}}{{println $name}}{{end}}' "$backend_id" | head -n 1
}

create_stream_via_api() {
  local payload="$1"
  docker run --rm --network host curlimages/curl:8.7.1 -sS \
    -H "Content-Type: application/json" \
    -X POST "http://localhost:${BACKEND_PORT}/api/streams" \
    -d "$payload"
}

extract_json_field() {
  local json="$1"
  local key="$2"
  echo "$json" | sed -n "s/.*\"${key}\"[[:space:]]*:[[:space:]]*\"\([^\"]*\)\".*/\1/p" | head -n 1
}

extract_restart_count() {
  local container_name="$1"
  docker inspect -f '{{.RestartCount}}' "$container_name" 2>/dev/null || echo "0"
}

to_mb() {
  local value="$1"
  if [[ "$value" == "N/A" || -z "$value" ]]; then
    echo "0"
    return
  fi

  local number unit
  number="$(echo "$value" | sed -E 's/^([0-9.]+).*/\1/')"
  unit="$(echo "$value" | sed -E 's/^[0-9.]+\s*([[:alpha:]]+).*/\1/')"
  case "$unit" in
    B|Bytes) awk -v n="$number" 'BEGIN{printf "%.6f", n/1024/1024}' ;;
    kB|KB|KiB) awk -v n="$number" 'BEGIN{printf "%.6f", n/1024}' ;;
    MB|MiB) awk -v n="$number" 'BEGIN{printf "%.6f", n}' ;;
    GB|GiB) awk -v n="$number" 'BEGIN{printf "%.6f", n*1024}' ;;
    TB|TiB) awk -v n="$number" 'BEGIN{printf "%.6f", n*1024*1024}' ;;
    *) echo "0" ;;
  esac
}

to_bytes() {
  local value="$1"
  if [[ "$value" == "N/A" || -z "$value" ]]; then
    echo "0"
    return
  fi

  local number unit
  number="$(echo "$value" | sed -E 's/^([0-9.]+).*/\1/')"
  unit="$(echo "$value" | sed -E 's/^[0-9.]+\s*([[:alpha:]]+).*/\1/')"
  case "$unit" in
    B|Bytes) awk -v n="$number" 'BEGIN{printf "%.0f", n}' ;;
    kB|KB|KiB) awk -v n="$number" 'BEGIN{printf "%.0f", n*1024}' ;;
    MB|MiB) awk -v n="$number" 'BEGIN{printf "%.0f", n*1024*1024}' ;;
    GB|GiB) awk -v n="$number" 'BEGIN{printf "%.0f", n*1024*1024*1024}' ;;
    TB|TiB) awk -v n="$number" 'BEGIN{printf "%.0f", n*1024*1024*1024*1024}' ;;
    *) echo "0" ;;
  esac
}

collect_stats_loop() {
  local container_name="$1"
  local outfile="$2"
  : > "$outfile"
  echo "timestamp,cpu_percent,mem_used_mb,net_in_bytes,net_out_bytes" >> "$outfile"

  while [[ ! -f "${outfile}.stop" ]]; do
    local line
    line="$(docker stats --no-stream --format '{{.CPUPerc}},{{.MemUsage}},{{.NetIO}}' "$container_name" 2>/dev/null || true)"
    if [[ -n "$line" ]]; then
      local cpu mem_usage netio
      cpu="$(echo "$line" | cut -d',' -f1 | tr -d '%' | tr -d ' ')"
      mem_usage="$(echo "$line" | cut -d',' -f2 | sed 's/\/.*$//' | xargs)"
      netio="$(echo "$line" | cut -d',' -f3-)"
      local net_in_raw net_out_raw
      net_in_raw="$(echo "$netio" | cut -d'/' -f1 | xargs)"
      net_out_raw="$(echo "$netio" | cut -d'/' -f2 | xargs)"
      local mem_mb net_in_bytes net_out_bytes
      mem_mb="$(to_mb "$mem_usage")"
      net_in_bytes="$(to_bytes "$net_in_raw")"
      net_out_bytes="$(to_bytes "$net_out_raw")"
      echo "$(date +%s),${cpu:-0},${mem_mb:-0},${net_in_bytes:-0},${net_out_bytes:-0}" >> "$outfile"
    fi
    sleep 2
  done
}

start_viewers() {
  local network="$1"
  local base_url="$2"
  local stream_key="$3"
  local viewers="$4"
  local duration="$5"
  local out_dir="$6"

  mkdir -p "$out_dir"
  VIEWER_PIDS=()
  local i
  for ((i=1; i<=viewers; i++)); do
    local file_name="viewer-${i}.txt"
    docker run --rm \
      --network "$network" \
      -v "$out_dir:/out" \
      curlimages/curl:8.7.1 \
      sh -c '
        set -eu
        BASE_URL="$0"
        STREAM_KEY="$1"
        DURATION="$2"
        OUT_FILE="$3"
        REQUEST_LOG="/out/${OUT_FILE%.txt}-requests.csv"
        printf "timestamp,resource,http_code,curl_exit_code,time_total_seconds\n" > "$REQUEST_LOG"
        END_TS=$(( $(date +%s) + DURATION ))
        TOTAL=0
        ERRORS=0

        fetch_and_record() {
          TS="$1"
          RESOURCE="$2"
          OUTPUT="$3"
          URL="$4"
          RESPONSE=$(curl -s --connect-timeout 2 --max-time 5 -o "$OUTPUT" -w "%{http_code},%{exitcode},%{time_total}" "$URL" || true)
          CODE=${RESPONSE%%,*}
          REST=${RESPONSE#*,}
          if [ "$REST" = "$RESPONSE" ]; then
            EXIT_CODE=unknown
            ELAPSED=0
          else
            EXIT_CODE=${REST%%,*}
            ELAPSED=${REST#*,}
          fi
          CODE=${CODE:-000}
          printf "%s,%s,%s,%s,%s\n" "$TS" "$RESOURCE" "$CODE" "$EXIT_CODE" "$ELAPSED" >> "$REQUEST_LOG"
          printf "%s" "$CODE"
        }

        while :; do
          CYCLE_TS=$(date +%s)
          [ "$CYCLE_TS" -lt "$END_TS" ] || break
          MASTER_URL="${BASE_URL}/hls/${STREAM_KEY}/master.m3u8"
          CODE=$(fetch_and_record "$CYCLE_TS" master /tmp/master.m3u8 "$MASTER_URL")
          TOTAL=$((TOTAL + 1))
          if [ "$CODE" != "200" ]; then
            ERRORS=$((ERRORS + 1))
            sleep 1
            continue
          fi

          PLAYLIST=$(grep -E "^[^#]" /tmp/master.m3u8 | head -n1 || true)
          case "$PLAYLIST" in
            v[0-3]/playlist.m3u8) ;;
            *)
              printf "%s,master_content,invalid,,\n" "$CYCLE_TS" >> "$REQUEST_LOG"
              ERRORS=$((ERRORS + 1))
              sleep 1
              continue
              ;;
          esac
          PURL="${BASE_URL}/hls/${STREAM_KEY}/${PLAYLIST}"
          PCODE=$(fetch_and_record "$CYCLE_TS" variant /tmp/variant.m3u8 "$PURL")
          TOTAL=$((TOTAL + 1))
          if [ "$PCODE" != "200" ]; then
            ERRORS=$((ERRORS + 1))
            sleep 1
            continue
          fi

          SEGMENT=$(grep -E "^[^#]" /tmp/variant.m3u8 | tail -n1 || true)
          case "$SEGMENT" in
            segment_*.ts) ;;
            *)
              printf "%s,variant_content,invalid,,\n" "$CYCLE_TS" >> "$REQUEST_LOG"
              ERRORS=$((ERRORS + 1))
              sleep 1
              continue
              ;;
          esac
          SURL="${BASE_URL}/hls/${STREAM_KEY}/${PLAYLIST%/*}/${SEGMENT}"
          SCODE=$(fetch_and_record "$CYCLE_TS" segment /dev/null "$SURL")
          TOTAL=$((TOTAL + 1))
          if [ "$SCODE" != "200" ]; then
            ERRORS=$((ERRORS + 1))
          fi

          sleep 1
        done

        printf "%s,%s\n" "$TOTAL" "$ERRORS" > "/out/${OUT_FILE}"
      ' "$base_url" "$stream_key" "$duration" "$file_name" >/dev/null 2>&1 &
    VIEWER_PIDS+=("$!")
  done
}

wait_for_viewers() {
  local pid
  for pid in "${VIEWER_PIDS[@]:-}"; do
    wait "$pid" || true
  done
}

sum_viewer_results() {
  local viewer_dir="$1"
  local total=0
  local errors=0

  local file
  for file in "$viewer_dir"/viewer-*.txt; do
    [[ -f "$file" ]] || continue
    local row t e
    row="$(cat "$file")"
    t="${row%%,*}"
    e="${row##*,}"
    total=$((total + ${t:-0}))
    errors=$((errors + ${e:-0}))
  done

  echo "$total,$errors"
}

append_csv_row() {
  local row="$1"
  echo "$row" >> "$CSV_FILE"
}

format_decimal() {
  awk -v v="$1" 'BEGIN{printf "%.4f", v+0}'
}

compute_metrics_from_samples() {
  local file="$1"
  local start_ts="${2:-0}"
  local end_ts="${3:-0}"
  awk -F',' -v start_ts="$start_ts" -v end_ts="$end_ts" '
    NR==1 {next}
    ($1 < start_ts || (end_ts > 0 && $1 > end_ts)) {next}
    {
      c+=$2; if($2>cmax)cmax=$2;
      m+=$3; if($3>mmax)mmax=$3;
      ni+=$4; no+=$5; n++;
    }
    END {
      if(n==0){print "0,0,0,0,0,0"; exit}
      printf "%.4f,%.4f,%.4f,%.4f,%.4f,%.4f", c/n, cmax, m/n, mmax, ni/1024/1024, no/1024/1024
    }
  ' "$file"
}

measure_startup_latency() {
  local network="$1"
  local master_url="$2"
  local timeout="$3"

  local elapsed=0
  while (( elapsed < timeout )); do
    local code
    code="$(docker run --rm --network "$network" curlimages/curl:8.7.1 -s --connect-timeout 2 --max-time 5 -o /dev/null -w '%{http_code}' "$master_url" || true)"
    if [[ "$code" == "200" ]]; then
      echo "$elapsed"
      return 0
    fi
    sleep 1
    elapsed=$((elapsed + 1))
  done

  echo "-1"
  return 1
}

measure_first_segment_latency() {
  local network="$1"
  local master_url="$2"
  local timeout="$3"

  local elapsed=0
  while (( elapsed < timeout )); do
    if docker run --rm --network "$network" -i curlimages/curl:8.7.1 sh -s -- "$master_url" >/dev/null 2>&1 <<'EOF'
set -eu

MASTER_URL="$1"
MASTER=$(curl -s --connect-timeout 2 --max-time 5 "$MASTER_URL" || true)
PLAYLIST=$(printf "%s\n" "$MASTER" | grep -E "^[^#]" | head -n1 || true)
case "$PLAYLIST" in v[0-3]/playlist.m3u8) ;; *) exit 1 ;; esac

BASE="${MASTER_URL%/master.m3u8}"
VARIANT_URL="${BASE}/${PLAYLIST}"
VARIANT=$(curl -s --connect-timeout 2 --max-time 5 "$VARIANT_URL" || true)
SEGMENT=$(printf "%s\n" "$VARIANT" | grep -E "^[^#]" | tail -n1 || true)
case "$SEGMENT" in segment_*.ts) ;; *) exit 1 ;; esac

SEGMENT_URL="${BASE}/$(dirname "$PLAYLIST")/${SEGMENT}"
CODE=$(curl -s --connect-timeout 2 --max-time 5 -o /dev/null -w '%{http_code}' "$SEGMENT_URL" || true)
[ "$CODE" = "200" ]
EOF
    then
      echo "$elapsed"
      return 0
    fi
    sleep 1
    elapsed=$((elapsed + 1))
  done

  echo "-1"
  return 1
}

check_hls_variants() {
  local network="$1"
  local master_url="$2"
  local output_dir="$3"

  docker run --rm --network "$network" -v "$output_dir:/out" -i curlimages/curl:8.7.1 sh -s -- "$master_url" <<'EOF'
set -eu
MASTER_URL="$1"
BASE="${MASTER_URL%/master.m3u8}"

ATTEMPT=0
while [ "$ATTEMPT" -lt 30 ]; do
  MASTER_CODE=$(curl -s --connect-timeout 2 --max-time 5 -o /out/master.m3u8 -w '%{http_code}' "$MASTER_URL" || true)
  printf 'variant,playlist_http_code,segment_http_code\n' > /out/hls-variants.csv
  OK=0
  if [ "$MASTER_CODE" = 200 ]; then
    DECLARED=$(grep -Ec '^v[0-3]/playlist[.]m3u8$' /out/master.m3u8 || true)
    [ "$DECLARED" -eq 4 ] || DECLARED=0
  else
    DECLARED=0
  fi

  for N in 0 1 2 3; do
    VARIANT="v${N}/playlist.m3u8"
    PLAYLIST_CODE=$(curl -s --connect-timeout 2 --max-time 5 -o "/out/v${N}-playlist.m3u8" -w '%{http_code}' "${BASE}/${VARIANT}" || true)
    SEGMENT_CODE=0
    if [ "$PLAYLIST_CODE" = 200 ]; then
      SEGMENT=$(grep -E '^segment_[0-9]+[.]ts$' "/out/v${N}-playlist.m3u8" | tail -n 1 || true)
      if [ -n "$SEGMENT" ]; then
        SEGMENT_CODE=$(curl -s --connect-timeout 2 --max-time 5 -o /dev/null -w '%{http_code}' "${BASE}/v${N}/${SEGMENT}" || true)
      fi
    fi
    printf '%s,%s,%s\n' "$VARIANT" "$PLAYLIST_CODE" "$SEGMENT_CODE" >> /out/hls-variants.csv
    if [ "$PLAYLIST_CODE" = 200 ] && [ "$SEGMENT_CODE" = 200 ]; then
      OK=$((OK + 1))
    fi
  done

  if [ "$DECLARED" -eq 4 ] && [ "$OK" -eq 4 ]; then
    exit 0
  fi
  ATTEMPT=$((ATTEMPT + 1))
  sleep 1
done
exit 1
EOF
}

build_failed_attempt_result() {
  local error_message="$1"
  local rtmp_container="$2"
  local scenario_log_file="$3"
  local samples_file="$4"
  local viewers_dir="$5"

  echo "FAIL,${error_message},${rtmp_container},${scenario_log_file},${samples_file},${viewers_dir},0,0,0,-1,-1,-1,0,0,0,0,0,0,0,0,0,0,0,0,,"
}

probe_bitrate_kbps() {
  local network="$1"
  local master_url="$2"

  docker run --rm --network "$network" -i curlimages/curl:8.7.1 sh -s -- "$master_url" <<'EOF'
set -eu

MASTER_URL="$1"
MASTER=$(curl -s "$MASTER_URL" || true)
PLAYLIST=$(printf "%s" "$MASTER" | grep -E "^[^#]" | head -n1 || true)

if [ -z "$PLAYLIST" ]; then
  echo "0"
  exit 0
fi

BASE="${MASTER_URL%/master.m3u8}"
VARIANT_URL="${BASE}/${PLAYLIST}"
VARIANT=$(curl -s "$VARIANT_URL" || true)
TOTAL_DURATION=$(printf "%s\n" "$VARIANT" | awk -F: '/^#EXTINF:/ {gsub(/,.*/, "", $2); s+=$2} END {printf "%.4f", s+0}')

if [ "$TOTAL_DURATION" = "0.0000" ]; then
  echo "0"
  exit 0
fi

SEGMENT_DIR=$(dirname "$PLAYLIST")
TOTAL_BYTES=0

while IFS= read -r SEG; do
  [ -n "$SEG" ] || continue
  SEG_URL="${BASE}/${SEGMENT_DIR}/${SEG}"
  LEN=$(curl -sI "$SEG_URL" | awk -F': ' "tolower(\$1)==\"content-length\" {gsub(\"\\r\", \"\", \$2); print \$2; exit}")
  LEN=${LEN:-0}
  TOTAL_BYTES=$((TOTAL_BYTES + LEN))
done <<EOL
$(printf "%s\n" "$VARIANT" | grep -E "^[^#]" || true)
EOL

awk -v bytes="$TOTAL_BYTES" -v dur="$TOTAL_DURATION" 'BEGIN { if(dur<=0){print "0"} else { printf "%.4f", (bytes*8/1000)/dur } }'
EOF
}

run_one_attempt() {
  local scenario="$1"
  local viewers="$2"
  local repeat_idx="$3"
  local retry_idx="$4"

  local server transcoder
  server="${scenario%%+*}"
  transcoder="${scenario##*+}"

  local scenario_slug
  scenario_slug="$(sanitize "${scenario}-v${viewers}-r${repeat_idx}-try${retry_idx}")"

  local scenario_dir="${RUN_DIR}/${scenario_slug}"
  local scenario_logs="${scenario_dir}/logs"
  local scenario_viewers="${scenario_dir}/viewers"
  local samples_file="${scenario_dir}/metrics-samples.csv"
  local scenario_log_file="${scenario_logs}/scenario.log"

  mkdir -p "$scenario_dir" "$scenario_logs" "$scenario_viewers"
  : > "$scenario_log_file"

  log "Iniciando cenario ${scenario} (viewers=${viewers}, repeat=${repeat_idx}, retry=${retry_idx})" | tee -a "$scenario_log_file"

  setup_environment_for_scenario "$server" "$transcoder"

  compose_down_quiet

  local rtmp_service="rtmp-server-${server}"
  local rtmp_container="streaming-rtmp-server-${server}"

  if ! compose up -d --no-build postgres rabbitmq streaming-platform "$rtmp_service" >> "$scenario_log_file" 2>&1; then
    build_failed_attempt_result "compose_up_failed" "$rtmp_container" "$scenario_log_file" "$samples_file" "$scenario_viewers"
    return 1
  fi

  if ! wait_backend_ready; then
    compose logs --no-color streaming-platform >> "$scenario_log_file" 2>&1 || true
    build_failed_attempt_result "backend_not_ready" "$rtmp_container" "$scenario_log_file" "$samples_file" "$scenario_viewers"
    return 1
  fi

  if ! wait_container_running "$rtmp_container"; then
    compose logs --no-color "$rtmp_service" >> "$scenario_log_file" 2>&1 || true
    build_failed_attempt_result "rtmp_container_not_running" "$rtmp_container" "$scenario_log_file" "$samples_file" "$scenario_viewers"
    return 1
  fi

  capture_attempt_environment "$rtmp_container" "${scenario_dir}/environment.txt"
  capture_host_snapshot "${scenario_dir}/host-before.txt"
  vmstat -n 2 > "${scenario_dir}/host-vmstat.log" &
  HOST_VMSTAT_PID=$!

  local compose_network
  compose_network="$(get_compose_network || true)"
  [[ -n "$compose_network" ]] || compose_network="app_streaming-network"

  local owner_id
  owner_id="bench-${server}-${transcoder}-owner"
  local create_payload
  create_payload="{\"title\":\"Bench ${scenario}\",\"description\":\"${RUN_ID}\",\"ownerId\":\"${owner_id}\"}"

  local create_response
  create_response="$(create_stream_via_api "$create_payload" || true)"
  local stream_key
  stream_key="$(extract_json_field "$create_response" "streamKey")"

  if [[ -z "$stream_key" ]]; then
    echo "$create_response" >> "$scenario_log_file"
    echo "FAIL,stream_create_failed,$rtmp_container,$scenario_log_file,$samples_file,$scenario_viewers,0,0,0,0,0,0,0,0,0,0,0,0,0"
    return 1
  fi

  local restarts_before
  restarts_before="$(extract_restart_count "$rtmp_container")"

  collect_stats_loop "$rtmp_container" "$samples_file" &
  STATS_PID=$!

  local publisher_name
  publisher_name="bench-pub-${scenario_slug}"
  publisher_name="${publisher_name:0:55}"
  publisher_name="${publisher_name%-}"
  PUBLISHER_NAME="$publisher_name"
  trap 'if [[ -n "${PUBLISHER_NAME:-}" ]]; then docker stop -t 5 "$PUBLISHER_NAME" >/dev/null 2>&1 || true; fi; if [[ "${HOST_VMSTAT_PID:-0}" -gt 0 ]]; then kill "$HOST_VMSTAT_PID" 2>/dev/null || true; fi' EXIT

  local stream_url
  stream_url="rtmp://${rtmp_service}:${RTMP_PORT}/live/${stream_key}"

  local measurement_start_ts
  measurement_start_ts="$(date +%s)"

  docker run --rm \
    --name "$publisher_name" \
    --network "$compose_network" \
    jrottenberg/ffmpeg:6.0-alpine \
    -hide_banner -loglevel error \
    -re \
    -f lavfi -i "testsrc=size=1280x720:rate=30" \
    -f lavfi -i "sine=frequency=1000:sample_rate=44100" \
    -c:v libx264 -preset veryfast -pix_fmt yuv420p \
    -c:a aac -ar 44100 -ac 2 \
    -f flv "$stream_url" >> "$scenario_log_file" 2>&1 &
  local publisher_pid=$!

  local base_hls_url
  base_hls_url="http://${rtmp_service}:${HLS_HTTP_PORT}"
  local master_url
  master_url="${base_hls_url}/hls/${stream_key}/master.m3u8"

  local probe_start_ts probe_end_ts probe_elapsed_seconds
  probe_start_ts="$(date +%s)"
  local startup_hls_seconds
  startup_hls_seconds="$(measure_startup_latency "$compose_network" "$master_url" 90 || true)"
  if [[ -z "$startup_hls_seconds" ]]; then
    startup_hls_seconds="-1"
  fi

  local tempo_primeiro_segmento_s
  tempo_primeiro_segmento_s="$(measure_first_segment_latency "$compose_network" "$master_url" 120 || true)"
  if [[ -z "$tempo_primeiro_segmento_s" ]]; then
    tempo_primeiro_segmento_s="-1"
  fi
  probe_end_ts="$(date +%s)"
  probe_elapsed_seconds=$((probe_end_ts - probe_start_ts))

  local variants_check_start_ts variants_check_end_ts variants_ok=0
  variants_check_start_ts="$(date +%s)"
  if check_hls_variants "$compose_network" "$master_url" "$scenario_dir" >> "$scenario_log_file" 2>&1; then
    variants_ok=1
  fi
  variants_check_end_ts="$(date +%s)"

  local latencia_ponta_a_ponta_s
  latencia_ponta_a_ponta_s="$tempo_primeiro_segmento_s"

  local publisher_alive_before_viewers=0
  if kill -0 "$publisher_pid" 2>/dev/null &&
     [[ "$(docker inspect -f '{{.State.Running}}' "$publisher_name" 2>/dev/null || true)" == "true" ]]; then
    publisher_alive_before_viewers=1
  fi
  local viewers_start_ts viewers_end_ts viewers_window_seconds
  viewers_start_ts="$(date +%s)"
  start_viewers "$compose_network" "$base_hls_url" "$stream_key" "$viewers" "$DURATION_SECONDS" "$scenario_viewers"
  wait_for_viewers
  viewers_end_ts="$(date +%s)"
  viewers_window_seconds=$((viewers_end_ts - viewers_start_ts))
  capture_host_snapshot "${scenario_dir}/host-after.txt"
  kill "$HOST_VMSTAT_PID" 2>/dev/null || true
  wait "$HOST_VMSTAT_PID" 2>/dev/null || true
  HOST_VMSTAT_PID=0

  local publisher_alive_after_viewers=0
  if kill -0 "$publisher_pid" 2>/dev/null &&
     [[ "$(docker inspect -f '{{.State.Running}}' "$publisher_name" 2>/dev/null || true)" == "true" ]]; then
    publisher_alive_after_viewers=1
  fi

  local bitrate_kbps
  bitrate_kbps="$(probe_bitrate_kbps "$compose_network" "$master_url" 2>> "$scenario_log_file" || true)"
  [[ -n "$bitrate_kbps" ]] || bitrate_kbps="0"

  local measurement_end_ts
  measurement_end_ts="$viewers_end_ts"
  docker stop -t 5 "$publisher_name" >/dev/null 2>&1 || true
  local publisher_exit
  if wait "$publisher_pid"; then
    publisher_exit=0
  else
    publisher_exit=$?
  fi
  PUBLISHER_NAME=""

  printf 'probe_start_ts,probe_end_ts,probe_elapsed_seconds,variants_check_start_ts,variants_check_end_ts,variants_ok,viewers_start_ts,viewers_end_ts,viewers_window_seconds,publisher_alive_before_viewers,publisher_alive_after_viewers,publisher_exit_code\n%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n' \
    "$probe_start_ts" "$probe_end_ts" "$probe_elapsed_seconds" \
    "$variants_check_start_ts" "$variants_check_end_ts" "$variants_ok" \
    "$viewers_start_ts" "$viewers_end_ts" "$viewers_window_seconds" \
    "$publisher_alive_before_viewers" "$publisher_alive_after_viewers" "$publisher_exit" \
    > "${scenario_dir}/timing.csv"

  sleep 2
  touch "${samples_file}.stop"
  wait "$STATS_PID" || true

  compose logs --no-color "$rtmp_service" >> "${scenario_logs}/rtmp.log" 2>&1 || true
  compose logs --no-color streaming-platform >> "${scenario_logs}/backend.log" 2>&1 || true

  local restarts_after
  restarts_after="$(extract_restart_count "$rtmp_container")"

  local req_summary
  req_summary="$(sum_viewer_results "$scenario_viewers")"
  local request_total request_errors
  request_total="${req_summary%%,*}"
  request_errors="${req_summary##*,}"
  local completed_viewers=0 request_logs=0 viewer_result
  for viewer_result in "$scenario_viewers"/viewer-*.txt; do
    [[ -f "$viewer_result" ]] || continue
    completed_viewers=$((completed_viewers + 1))
  done
  for viewer_result in "$scenario_viewers"/viewer-*-requests.csv; do
    [[ -f "$viewer_result" ]] || continue
    request_logs=$((request_logs + 1))
  done

  local error_rate_percent
  if [[ "$request_total" -gt 0 ]]; then
    error_rate_percent="$(awk -v e="$request_errors" -v t="$request_total" 'BEGIN{printf "%.4f", (e*100)/t}')"
  else
    error_rate_percent="0"
  fi

  local metrics
  metrics="$(compute_metrics_from_samples "$samples_file" "$viewers_start_ts" "$viewers_end_ts")"
  local cpu_avg cpu_max mem_avg mem_max net_in_mb net_out_mb
  IFS=',' read -r cpu_avg cpu_max mem_avg mem_max net_in_mb net_out_mb <<< "$metrics"
  local sample_count
  sample_count="$(awk -F',' -v first="$viewers_start_ts" -v last="$viewers_end_ts" 'NR>1 && $1>=first && $1<=last {n++} END{print n+0}' "$samples_file")"

  local measurement_window
  measurement_window=$((measurement_end_ts - measurement_start_ts))

  local status="PASS"
  local error_message=""

  if [[ "$startup_hls_seconds" == "-1" ]]; then
    status="FAIL"
    error_message="master_playlist_timeout"
  elif [[ "$tempo_primeiro_segmento_s" == "-1" ]]; then
    status="FAIL"
    error_message="first_segment_timeout"
  elif [[ "$variants_ok" -ne 1 ]]; then
    status="FAIL"
    error_message="hls_variants_unavailable"
  elif [[ "$publisher_alive_before_viewers" -ne 1 || "$publisher_alive_after_viewers" -ne 1 ]]; then
    status="FAIL"
    error_message="publisher_not_active_through_viewers_exit_${publisher_exit}"
  elif [[ "$completed_viewers" -ne "$viewers" || "$request_logs" -ne "$viewers" || "$request_total" -eq 0 ]]; then
    status="FAIL"
    error_message="incomplete_viewer_results_${completed_viewers}_of_${viewers}"
  elif [[ "$sample_count" -lt 3 ]]; then
    status="FAIL"
    error_message="insufficient_cpu_memory_samples_${sample_count}"
  elif ! awk -v v="$bitrate_kbps" 'BEGIN {exit !(v+0>0)}'; then
    status="FAIL"
    error_message="bitrate_probe_failed"
  fi

  echo "$status,$error_message,$rtmp_container,$scenario_log_file,$samples_file,$scenario_viewers,$measurement_start_ts,$measurement_end_ts,$measurement_window,$startup_hls_seconds,$latencia_ponta_a_ponta_s,$tempo_primeiro_segmento_s,$request_total,$request_errors,$error_rate_percent,$cpu_avg,$cpu_max,$mem_avg,$mem_max,$net_in_mb,$net_out_mb,$bitrate_kbps,$restarts_before,$restarts_after,$compose_network,$master_url"

  if [[ "$KEEP_UP" -eq 0 ]]; then
    compose_down_quiet
  fi

  if [[ "$status" == "PASS" ]]; then
    return 0
  fi
  return 1
}

execute_plan() {
  local total=${#PLAN_ITEMS[@]}
  local idx=0
  local passed=0
  local failed=0

  for item in "${PLAN_ITEMS[@]}"; do
    idx=$((idx + 1))
    local scenario viewers repeat_idx
    IFS='|' read -r scenario viewers repeat_idx <<< "$item"

    log "[$idx/$total] Executando $scenario viewers=$viewers repeat=$repeat_idx"

    local attempt=0
    local final_result=""

    while (( attempt <= RETRIES )); do
      local result_line
      if result_line="$(run_one_attempt "$scenario" "$viewers" "$repeat_idx" "$attempt")"; then
        final_result="$result_line"
        break
      else
        final_result="$result_line"
        attempt=$((attempt + 1))
        if (( attempt <= RETRIES )); then
          log "Retry curto para $scenario (tentativa $attempt de $RETRIES)"
        fi
      fi
    done

    local status error_message rtmp_container scenario_log_file samples_file viewers_dir
    local measurement_start_ts measurement_end_ts measurement_window startup_hls_seconds latencia_ponta_a_ponta_s tempo_primeiro_segmento_s
    local request_total request_errors error_rate_percent cpu_avg cpu_max mem_avg mem_max net_in_mb net_out_mb
    local bitrate_kbps restarts_before restarts_after compose_network master_url

    IFS=',' read -r status error_message rtmp_container scenario_log_file samples_file viewers_dir \
      measurement_start_ts measurement_end_ts measurement_window startup_hls_seconds latencia_ponta_a_ponta_s tempo_primeiro_segmento_s request_total request_errors \
      error_rate_percent cpu_avg cpu_max mem_avg mem_max net_in_mb net_out_mb bitrate_kbps \
      restarts_before restarts_after compose_network master_url <<< "$final_result"

    if [[ "$status" == "PASS" ]]; then
      passed=$((passed + 1))
    else
      failed=$((failed + 1))
      if [[ "$error_message" == "" ]]; then
        error_message="unknown"
      fi
    fi

    local row
    row="${RUN_ID},${MODE},${scenario},${scenario%%+*},${scenario##*+},${repeat_idx},${viewers},${DURATION_SECONDS},${status},${error_message},${measurement_start_ts},${measurement_end_ts},${measurement_window},${startup_hls_seconds},${latencia_ponta_a_ponta_s},${tempo_primeiro_segmento_s},${request_total},${request_errors},${error_rate_percent},${cpu_avg},${cpu_max},${mem_avg},${mem_max},${net_in_mb},${net_out_mb},${bitrate_kbps},${restarts_before},${restarts_after},${attempt},${compose_network},${rtmp_container},${master_url},${scenario_log_file},${samples_file},${viewers_dir}"
    append_csv_row "$row"

    log "Resultado: ${status} (${scenario}, viewers=${viewers}, repeat=${repeat_idx})"
  done

  log "Execucao finalizada. PASS=${passed} FAIL=${failed}"
  log "CSV consolidado: ${CSV_FILE}"

  local summary_file="${RUN_DIR}/summary.txt"
  {
    echo "run_id=${RUN_ID}"
    echo "mode=${MODE}"
    echo "duration_seconds=${DURATION_SECONDS}"
    echo "repeats=${REPEATS}"
    echo "viewers_list=${VIEWERS_LIST}"
    echo "total=${total}"
    echo "passed=${passed}"
    echo "failed=${failed}"
    echo "csv=${CSV_FILE}"
  } > "$summary_file"

  if [[ "$failed" -gt 0 ]]; then
    return 1
  fi
  return 0
}

main() {
  parse_args "$@"
  trap cleanup EXIT INT TERM
  validate_preflight
  ensure_results
  prepare_run_dirs
  capture_run_environment
  enable_console_log_mirror
  build_execution_plan
  build_selected_images

  log "RUN_ID: ${RUN_ID}"
  log "Log da execucao: ${CONSOLE_LOG_FILE}"
  log "Modo: ${MODE}"
  log "Duracao da carga de viewers: ${DURATION_SECONDS}s"
  log "Cenarios planejados: ${#PLAN_ITEMS[@]}"

  local execution_status=0
  if execute_plan; then
    log "Benchmark concluido com sucesso"
  else
    log "Benchmark concluido com falhas parciais"
    execution_status=1
  fi

  if [[ "$AGGREGATE" -eq 1 && "$MODE" == "full" ]]; then
    local aggregate_script="${SCRIPT_DIR}/aggregate-results.py"
    if command -v python3 >/dev/null 2>&1 && [[ -f "$aggregate_script" ]]; then
      log "Agregando resultados com aggregate-results.py ..."
      python3 "$aggregate_script" "$CSV_FILE" "$RUN_DIR"
      log "CSV agregado: ${RUN_DIR}/results-aggregated.csv"
      if [[ "$PUBLISH" -eq 1 && "$execution_status" -eq 0 ]]; then
        publish_reports "$RUN_DIR"
        log "CSVs publicos gravados em ${DOCS_BENCHMARK_DIR}/runs/${RUN_ID}"
      fi
    else
      log "AVISO: python3 ou aggregate-results.py nao encontrado, pulando agregacao."
    fi
  fi
  return "$execution_status"
}

main "$@"

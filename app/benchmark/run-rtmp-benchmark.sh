#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
COMPOSE_FILE="${APP_DIR}/docker-compose.yml"
ENV_FILE="${APP_DIR}/.env"
RESULTS_ROOT="${SCRIPT_DIR}/results"

MODE="sanity"
SCENARIO_FILTER="all"
DURATION_SECONDS=90
VIEWERS_SINGLE=1
VIEWERS_LIST="1,10,50"
REPEATS=3
RETRIES=1
BACKEND_PORT=8080
RTMP_PORT=1935
HLS_HTTP_PORT=8081
KEEP_UP=0

SCENARIOS=(
  "nginx+ffmpeg"
  "nginx+gstreamer"
  "srs+ffmpeg"
  "srs+gstreamer"
)

VIEWER_PIDS=()

CSV_HEADER="run_id,mode,scenario,server,transcoder,repeat_index,viewers,duration_seconds,status,error_message,start_ts,end_ts,measurement_start_ts,measurement_end_ts,measurement_window_seconds,startup_hls_seconds,request_total,request_errors,error_rate_percent,cpu_avg_percent,cpu_max_percent,mem_avg_mb,mem_max_mb,net_in_mb,net_out_mb,bitrate_kbps,restarts_before,restarts_after,retry_index,compose_network,rtmp_container,master_playlist_url,scenario_log_file,metrics_samples_file,viewer_results_dir"
TCC_CSV_HEADER="Execucao,Servidor,Transcodificador,Espectadores,Duracao_s,Repeticao,Startup_HLS_s,Tempo_Primeiro_Segmento_s,CPU_medio_percent,CPU_max_percent,RAM_media_MB,RAM_max_MB,Bitrate_efetivo_kbps,Erros_segmento,Taxa_erros_percent,Reinicios_sessao,Latencia_ponta_a_ponta_s,Status,Observacoes"

usage() {
  cat <<EOF
Uso: ./benchmark/run-rtmp-benchmark.sh [opcoes]

Opcoes:
  --mode <sanity|full>         Modo de execucao (default: sanity)
  --scenario <all|combinacao>  Filtro de cenario (ex.: nginx+ffmpeg)
  --duration <segundos>        Duracao da live ativa por execucao
  --viewers <n>                Numero de viewers (usado no modo sanity)
  --viewers-list <lista>       Lista CSV de viewers (modo full, default: 1,10,50)
  --repeats <n>                Repeticoes por cenario (modo full default: 3)
  --retries <n>                Retries curtos por cenario em caso de falha
  --backend-port <port>        Porta do backend exposta no host (default: 8080)
  --rtmp-port <port>           Porta RTMP interna dos servicos (default: 1935)
  --hls-port <port>            Porta HLS interna dos servicos (default: 8081)
  --keep-up                    Nao derruba stack no final
  --help                       Exibe ajuda

Exemplos:
  ./benchmark/run-rtmp-benchmark.sh --mode sanity --duration 90
  ./benchmark/run-rtmp-benchmark.sh --mode full --duration 300 --repeats 3
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
        HLS_HTTP_PORT="$2"
        shift 2
        ;;
      --keep-up)
        KEEP_UP=1
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
  [[ "$HLS_HTTP_PORT" =~ ^[0-9]+$ ]] || die "--hls-port deve ser inteiro"
}

compose() {
  (
    cd "$APP_DIR"
    docker compose -f "$COMPOSE_FILE" "$@"
  )
}

ensure_results() {
  mkdir -p "$RESULTS_ROOT"
}

validate_preflight() {
  require_cmd docker
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
}

prepare_run_dirs() {
  local timestamp
  timestamp="$(date '+%Y%m%d-%H%M%S')"
  RUN_ID="bench-${MODE}-${timestamp}"
  RUN_DIR="${RESULTS_ROOT}/${RUN_ID}"
  LOG_DIR="${RUN_DIR}/logs"
  CSV_FILE="${RUN_DIR}/results.csv"
  TCC_CSV_FILE="${RUN_DIR}/results-tcc.csv"
  TECHNICAL_REPORT_FILE="${RUN_DIR}/report-technical.md"
  TCC_REPORT_FILE="${RUN_DIR}/report-tcc.md"
  FINAL_RAW_CSV="${SCRIPT_DIR}/resultado_bruto.csv"
  FINAL_TCC_CSV="${SCRIPT_DIR}/resultado_tcc.csv"
  FINAL_TECHNICAL_MD="${SCRIPT_DIR}/resultado_tecnico.md"
  FINAL_TCC_MD="${SCRIPT_DIR}/resultado_tcc.md"
  mkdir -p "$RUN_DIR" "$LOG_DIR"
  echo "$CSV_HEADER" > "$CSV_FILE"
  echo "$TCC_CSV_HEADER" > "$TCC_CSV_FILE"
  ln -sfn "$RUN_DIR" "${RESULTS_ROOT}/latest"
}

test_id_for_row() {
  local scenario="$1"
  local viewers="$2"

  if [[ "$scenario" == "nginx+ffmpeg" && "$viewers" == "1" ]]; then echo "T01"; return; fi
  if [[ "$scenario" == "nginx+ffmpeg" && "$viewers" == "10" ]]; then echo "T02"; return; fi
  if [[ "$scenario" == "nginx+ffmpeg" && "$viewers" == "50" ]]; then echo "T03"; return; fi
  if [[ "$scenario" == "nginx+gstreamer" && "$viewers" == "1" ]]; then echo "T04"; return; fi
  if [[ "$scenario" == "nginx+gstreamer" && "$viewers" == "10" ]]; then echo "T05"; return; fi
  if [[ "$scenario" == "nginx+gstreamer" && "$viewers" == "50" ]]; then echo "T06"; return; fi
  if [[ "$scenario" == "srs+ffmpeg" && "$viewers" == "1" ]]; then echo "T07"; return; fi
  if [[ "$scenario" == "srs+ffmpeg" && "$viewers" == "10" ]]; then echo "T08"; return; fi
  if [[ "$scenario" == "srs+ffmpeg" && "$viewers" == "50" ]]; then echo "T09"; return; fi
  if [[ "$scenario" == "srs+gstreamer" && "$viewers" == "1" ]]; then echo "T10"; return; fi
  if [[ "$scenario" == "srs+gstreamer" && "$viewers" == "10" ]]; then echo "T11"; return; fi
  if [[ "$scenario" == "srs+gstreamer" && "$viewers" == "50" ]]; then echo "T12"; return; fi
  echo "NA"
}

build_observation() {
  local status="$1"
  local startup="$2"
  local error_rate="$3"
  local cpu_avg="$4"
  local restarts_before="$5"
  local restarts_after="$6"

  local notes=()
  local restarts_session=$((restarts_after - restarts_before))

  [[ "$status" == "PASS" ]] || notes+=("status=${status}")
  awk -v value="$startup" 'BEGIN{exit !(value > 15)}' && notes+=("startup_hls>15s") || true
  awk -v value="$error_rate" 'BEGIN{exit !(value > 1)}' && notes+=("erro_segmento>1%") || true
  awk -v value="$cpu_avg" 'BEGIN{exit !(value > 80)}' && notes+=("cpu_medio>80%") || true
  (( restarts_session > 0 )) && notes+=("reinicios=${restarts_session}")

  if [[ ${#notes[@]} -eq 0 ]]; then
    echo "ok"
  else
    local joined="${notes[*]}"
    echo "${joined// /; }"
  fi
}

append_tcc_csv_row() {
  local scenario="$1"
  local viewers="$2"
  local repeat_idx="$3"
  local status="$4"
  local startup_hls_seconds="$5"
  local cpu_avg="$6"
  local cpu_max="$7"
  local mem_avg="$8"
  local mem_max="$9"
  local bitrate_kbps="${10}"
  local request_errors="${11}"
  local error_rate_percent="${12}"
  local restarts_before="${13}"
  local restarts_after="${14}"
  local measurement_window="${15}"
  local test_id
  test_id="$(test_id_for_row "$scenario" "$viewers")"
  local restarts_session=$((restarts_after - restarts_before))
  local observations
  observations="$(build_observation "$status" "$startup_hls_seconds" "$error_rate_percent" "$cpu_avg" "$restarts_before" "$restarts_after")"

  echo "${test_id},${scenario%%+*},${scenario##*+},${viewers},${DURATION_SECONDS},${repeat_idx},${startup_hls_seconds},${startup_hls_seconds},${cpu_avg},${cpu_max},${mem_avg},${mem_max},${bitrate_kbps},${request_errors},${error_rate_percent},${restarts_session},manual,${status},${observations}" >> "$TCC_CSV_FILE"
}

generate_reports() {
  local summary_file="${RUN_DIR}/summary.txt"
  local total passed failed
  total="$(sed -n 's/^total=//p' "$summary_file")"
  passed="$(sed -n 's/^passed=//p' "$summary_file")"
  failed="$(sed -n 's/^failed=//p' "$summary_file")"

  cat > "$TECHNICAL_REPORT_FILE" <<EOF
# Relatorio Tecnico do Benchmark RTMP

## Execucao
- Run ID: ${RUN_ID}
- Modo: ${MODE}
- Duracao configurada por cenario: ${DURATION_SECONDS}s
- Repeticoes configuradas: ${REPEATS}
- Viewers configurados: ${VIEWERS_LIST}
- Total de execucoes: ${total}
- PASS: ${passed}
- FAIL: ${failed}

## Artefatos principais
- CSV robusto: ${CSV_FILE}
- CSV resumido TCC: ${TCC_CSV_FILE}
- Summary: ${summary_file}

## Observacoes
- O CSV robusto preserva metadados operacionais, paths de logs e evidencias por cenario.
- O CSV resumido foca nas metricas centrais da matriz de testes do TCC.
- A latencia ponta-a-ponta permanece como medicao manual e por isso aparece como manual no CSV resumido.

## Tabela resumida
| Teste | Servidor | Transcodificador | Viewers | Repeticao | Status | Startup HLS (s) | CPU medio (%) | Mem media (MB) | Bitrate (kbps) | Erros segmento | Reinicios |
|---|---|---|---:|---:|---|---:|---:|---:|---:|---:|---:|
EOF

  awk -F',' 'NR>1 {printf "| %s | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s |\n", $1, $2, $3, $4, $6, $18, $7, $9, $11, $13, $14, $16}' "$TCC_CSV_FILE" >> "$TECHNICAL_REPORT_FILE"

  cat > "$TCC_REPORT_FILE" <<EOF
# Resultados Preliminares do Benchmark RTMP

## Contexto
Foram executados os cenarios comparativos entre Nginx e SRS com FFmpeg e GStreamer, incluindo as cargas sinteticas definidas para esta execucao (${VIEWERS_LIST}) via Docker.

## Metricas destacadas
- Startup HLS
- CPU media e maxima do container RTMP
- Memoria media e maxima do container RTMP
- Bitrate efetivo de saida
- Contagem e taxa de erros de segmento
- Reinicios involuntarios por sessao

## Resultados
| Teste | Servidor | Transcodificador | Espectadores | Repeticao | Status | Startup HLS (s) | CPU medio (%) | Mem media (MB) | Bitrate (kbps) | Erros de segmento | Observacoes |
|---|---|---|---:|---:|---|---:|---:|---:|---:|---:|---|
EOF

  awk -F',' 'NR>1 {printf "| %s | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s | %s |\n", $1, $2, $3, $4, $6, $18, $7, $9, $11, $13, $14, $19}' "$TCC_CSV_FILE" >> "$TCC_REPORT_FILE"

  cp "$CSV_FILE" "$FINAL_RAW_CSV"
  cp "$TCC_CSV_FILE" "$FINAL_TCC_CSV"
  cp "$TECHNICAL_REPORT_FILE" "$FINAL_TECHNICAL_MD"
  cp "$TCC_REPORT_FILE" "$FINAL_TCC_MD"
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

write_runner_manifest() {
  local manifest_file="${RUN_DIR}/runner-manifest.md"
  cat > "$manifest_file" <<EOF
# Runner Manifest - ${RUN_ID}

## Objetivo
Executar benchmark RTMP em Docker para os cenarios:
- nginx+ffmpeg
- nginx+gstreamer
- srs+ffmpeg
- srs+gstreamer

## Containers essenciais por cenario
- postgres (database)
- rabbitmq (broker)
- streaming-platform (callbacks de validacao de stream)
- rtmp-server-nginx ou rtmp-server-srs (servidor RTMP alvo)
- publisher container (ffmpeg via docker run)
- viewer containers (curl via docker run)

## Politica de medicao
- Medir apenas janela de live ativa (start -> end)
- Excluir setup/teardown da stack
- Persistir CSV incremental por cenario para tolerancia a falhas
EOF
}

compose_down_quiet() {
  compose down --remove-orphans >/dev/null 2>&1 || true
}

setup_environment_for_scenario() {
  local server="$1"
  local transcoder="$2"

  export COMPOSE_PROFILES="$server"
  export TRANSCODER="$transcoder"
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
        END_TS=$(( $(date +%s) + DURATION ))
        TOTAL=0
        ERRORS=0

        while [ "$(date +%s)" -lt "$END_TS" ]; do
          MASTER_URL="${BASE_URL}/hls/${STREAM_KEY}/master.m3u8"
          CODE=$(curl -s -o /tmp/master.m3u8 -w "%{http_code}" "$MASTER_URL" || true)
          TOTAL=$((TOTAL + 1))
          if [ "$CODE" != "200" ]; then
            ERRORS=$((ERRORS + 1))
            sleep 1
            continue
          fi

          PLAYLIST=$(grep -E "^[^#]" /tmp/master.m3u8 | head -n1 || true)
          if [ -z "$PLAYLIST" ]; then
            ERRORS=$((ERRORS + 1))
            sleep 1
            continue
          fi

          PURL="${BASE_URL}/hls/${STREAM_KEY}/${PLAYLIST}"
          PCODE=$(curl -s -o /tmp/variant.m3u8 -w "%{http_code}" "$PURL" || true)
          TOTAL=$((TOTAL + 1))
          if [ "$PCODE" != "200" ]; then
            ERRORS=$((ERRORS + 1))
            sleep 1
            continue
          fi

          SEGMENT=$(grep -E "^[^#]" /tmp/variant.m3u8 | tail -n1 || true)
          if [ -n "$SEGMENT" ]; then
            SURL="${BASE_URL}/hls/${STREAM_KEY}/$(dirname "$PLAYLIST")/${SEGMENT}"
            SCODE=$(curl -s -o /dev/null -w "%{http_code}" "$SURL" || true)
            TOTAL=$((TOTAL + 1))
            if [ "$SCODE" != "200" ]; then
              ERRORS=$((ERRORS + 1))
            fi
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
  awk -F',' '
    NR==1 {next}
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

  local started
  started="$(date +%s)"
  local elapsed=0
  while (( elapsed < timeout )); do
    local code
    code="$(docker run --rm --network "$network" curlimages/curl:8.7.1 -s -o /dev/null -w '%{http_code}' "$master_url" || true)"
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

probe_bitrate_kbps() {
  local network="$1"
  local master_url="$2"

  docker run --rm --network "$network" -i curlimages/curl:8.7.1 sh -s "$master_url" <<'EOF'
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

  if ! compose up -d --build postgres rabbitmq streaming-platform "$rtmp_service" >> "$scenario_log_file" 2>&1; then
    echo "FAIL,compose_up_failed,$rtmp_container,$scenario_log_file,$samples_file,$scenario_viewers,0,0,0,0,0,0,0,0,0,0,0,0,0"
    return 1
  fi

  if ! wait_backend_ready; then
    compose logs --no-color streaming-platform >> "$scenario_log_file" 2>&1 || true
    echo "FAIL,backend_not_ready,$rtmp_container,$scenario_log_file,$samples_file,$scenario_viewers,0,0,0,0,0,0,0,0,0,0,0,0,0"
    return 1
  fi

  if ! wait_container_running "$rtmp_container"; then
    compose logs --no-color "$rtmp_service" >> "$scenario_log_file" 2>&1 || true
    echo "FAIL,rtmp_container_not_running,$rtmp_container,$scenario_log_file,$samples_file,$scenario_viewers,0,0,0,0,0,0,0,0,0,0,0,0,0"
    return 1
  fi

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
  local stats_pid=$!

  local publisher_name
  publisher_name="bench-pub-${scenario_slug}"
  publisher_name="${publisher_name:0:55}"

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
    -t "$DURATION_SECONDS" \
    -c:v libx264 -preset veryfast -pix_fmt yuv420p \
    -c:a aac -ar 44100 -ac 2 \
    -f flv "$stream_url" >> "$scenario_log_file" 2>&1 &
  local publisher_pid=$!

  local base_hls_url
  base_hls_url="http://${rtmp_service}:${HLS_HTTP_PORT}"
  local master_url
  master_url="${base_hls_url}/hls/${stream_key}/master.m3u8"

  local startup_hls_seconds
  startup_hls_seconds="$(measure_startup_latency "$compose_network" "$master_url" 90 || true)"
  if [[ -z "$startup_hls_seconds" ]]; then
    startup_hls_seconds="-1"
  fi

  start_viewers "$compose_network" "$base_hls_url" "$stream_key" "$viewers" "$DURATION_SECONDS" "$scenario_viewers"

  wait "$publisher_pid" || true
  local publisher_exit=$?

  wait_for_viewers

  local measurement_end_ts
  measurement_end_ts="$(date +%s)"

  sleep 2
  touch "${samples_file}.stop"
  wait "$stats_pid" || true

  compose logs --no-color "$rtmp_service" >> "${scenario_logs}/rtmp.log" 2>&1 || true
  compose logs --no-color streaming-platform >> "${scenario_logs}/backend.log" 2>&1 || true

  local restarts_after
  restarts_after="$(extract_restart_count "$rtmp_container")"

  local req_summary
  req_summary="$(sum_viewer_results "$scenario_viewers")"
  local request_total request_errors
  request_total="${req_summary%%,*}"
  request_errors="${req_summary##*,}"

  local error_rate_percent
  if [[ "$request_total" -gt 0 ]]; then
    error_rate_percent="$(awk -v e="$request_errors" -v t="$request_total" 'BEGIN{printf "%.4f", (e*100)/t}')"
  else
    error_rate_percent="0"
  fi

  local metrics
  metrics="$(compute_metrics_from_samples "$samples_file")"
  local cpu_avg cpu_max mem_avg mem_max net_in_mb net_out_mb
  IFS=',' read -r cpu_avg cpu_max mem_avg mem_max net_in_mb net_out_mb <<< "$metrics"

  local bitrate_kbps
  bitrate_kbps="$(probe_bitrate_kbps "$compose_network" "$master_url")"

  local measurement_window
  measurement_window=$((measurement_end_ts - measurement_start_ts))

  local status="PASS"
  local error_message=""

  if [[ "$startup_hls_seconds" == "-1" ]]; then
    status="FAIL"
    error_message="master_playlist_timeout"
  elif [[ "$publisher_exit" -ne 0 ]]; then
    status="FAIL"
    error_message="publisher_exit_${publisher_exit}"
  fi

  echo "$status,$error_message,$rtmp_container,$scenario_log_file,$samples_file,$scenario_viewers,$measurement_start_ts,$measurement_end_ts,$measurement_window,$startup_hls_seconds,$request_total,$request_errors,$error_rate_percent,$cpu_avg,$cpu_max,$mem_avg,$mem_max,$net_in_mb,$net_out_mb,$bitrate_kbps,$restarts_before,$restarts_after,$compose_network,$master_url"

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
    local done=0
    local final_result=""

    while (( attempt <= RETRIES )); do
      local result_line
      if result_line="$(run_one_attempt "$scenario" "$viewers" "$repeat_idx" "$attempt")"; then
        final_result="$result_line"
        done=1
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
    local measurement_start_ts measurement_end_ts measurement_window startup_hls_seconds
    local request_total request_errors error_rate_percent cpu_avg cpu_max mem_avg mem_max net_in_mb net_out_mb
    local bitrate_kbps restarts_before restarts_after compose_network master_url

    IFS=',' read -r status error_message rtmp_container scenario_log_file samples_file viewers_dir \
      measurement_start_ts measurement_end_ts measurement_window startup_hls_seconds request_total request_errors \
      error_rate_percent cpu_avg cpu_max mem_avg mem_max net_in_mb net_out_mb bitrate_kbps \
      restarts_before restarts_after compose_network master_url <<< "$final_result"

    local start_ts end_ts
    start_ts="$measurement_start_ts"
    end_ts="$measurement_end_ts"

    if [[ "$status" == "PASS" ]]; then
      passed=$((passed + 1))
    else
      failed=$((failed + 1))
      if [[ "$error_message" == "" ]]; then
        error_message="unknown"
      fi
    fi

    local row
    row="${RUN_ID},${MODE},${scenario},${scenario%%+*},${scenario##*+},${repeat_idx},${viewers},${DURATION_SECONDS},${status},${error_message},${start_ts},${end_ts},${measurement_start_ts},${measurement_end_ts},${measurement_window},${startup_hls_seconds},${request_total},${request_errors},${error_rate_percent},${cpu_avg},${cpu_max},${mem_avg},${mem_max},${net_in_mb},${net_out_mb},${bitrate_kbps},${restarts_before},${restarts_after},${attempt},${compose_network},${rtmp_container},${master_url},${scenario_log_file},${samples_file},${viewers_dir}"
    append_csv_row "$row"
    append_tcc_csv_row "$scenario" "$viewers" "$repeat_idx" "$status" "$startup_hls_seconds" "$cpu_avg" "$cpu_max" "$mem_avg" "$mem_max" "$bitrate_kbps" "$request_errors" "$error_rate_percent" "$restarts_before" "$restarts_after" "$measurement_window"

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
    echo "csv_tcc=${TCC_CSV_FILE}"
  } > "$summary_file"

  generate_reports

  if [[ "$failed" -gt 0 ]]; then
    return 1
  fi
  return 0
}

main() {
  parse_args "$@"
  validate_preflight
  ensure_results
  prepare_run_dirs
  build_execution_plan
  write_runner_manifest

  log "RUN_ID: ${RUN_ID}"
  log "Modo: ${MODE}"
  log "Duracao live ativa: ${DURATION_SECONDS}s"
  log "Cenarios planejados: ${#PLAN_ITEMS[@]}"

  if execute_plan; then
    log "Benchmark concluido com sucesso"
  else
    log "Benchmark concluido com falhas parciais"
    exit 1
  fi
}

main "$@"

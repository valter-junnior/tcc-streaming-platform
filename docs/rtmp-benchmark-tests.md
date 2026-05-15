# Benchmark RTMP - Execucao Tecnica

## Objetivo
Executar testes comparativos de desempenho RTMP em Docker para os cenarios:
- nginx+ffmpeg
- nginx+gstreamer
- srs+ffmpeg
- srs+gstreamer

A medicao considera somente a janela de live ativa (inicio da publicacao ate o fim da publicacao), sem medir setup da stack.

## Arquivo principal
Script unico de execucao:
- app/benchmark/run-rtmp-benchmark.sh

Saidas geradas:
- app/benchmark/results/<run_id>/results.csv
- app/benchmark/results/<run_id>/results-tcc.csv
- app/benchmark/results/<run_id>/summary.txt
- app/benchmark/results/<run_id>/runner-manifest.md
- app/benchmark/results/<run_id>/report-technical.md
- app/benchmark/results/<run_id>/report-tcc.md
- app/benchmark/results/<run_id>/*/logs/*.log

Copias atualizadas em docs a cada execucao:
- docs/3. Resultados Benchmark RTMP - Resultados Completos.csv
- docs/3. Resultados Benchmark RTMP - Resultados TCC.csv
- docs/3. Resultados Benchmark RTMP - Relatorio Tecnico.md
- docs/3. Resultados Benchmark RTMP - Relatorio TCC.md

## Politica Docker-only
Toda interacao operacional do benchmark e feita via Docker:
- docker compose para subir/derrubar stack
- docker run para publisher, viewers headless e chamadas HTTP

## Pre-requisitos
- Docker Engine ativo
- Projeto com app/.env configurado
- Portas de backend/RTMP/HLS disponiveis (default: 8080/1935/8081)

## Comandos
Entrar na pasta app:

```bash
cd app
```

Sanity (caso minimo por combinacao):

```bash
./benchmark/run-rtmp-benchmark.sh --mode sanity --duration 90 --viewers 1
```

Sanity de um cenario especifico:

```bash
./benchmark/run-rtmp-benchmark.sh --mode sanity --scenario nginx+ffmpeg --duration 90 --viewers 1
```

Campanha completa (matriz principal):

```bash
./benchmark/run-rtmp-benchmark.sh --mode full --duration 300 --viewers-list 1,10,50 --repeats 3 --retries 1
```

Campanha full curta (smoke):

```bash
./benchmark/run-rtmp-benchmark.sh --mode full --scenario srs+gstreamer --duration 120 --viewers-list 1 --repeats 1
```

## CSV consolidado
Schema de colunas do arquivo results.csv:
- run_id, mode, scenario, server, transcoder
- repeat_index, viewers, duration_seconds
- status, error_message
- start_ts, end_ts
- measurement_start_ts, measurement_end_ts, measurement_window_seconds
- startup_hls_seconds
- request_total, request_errors, error_rate_percent
- cpu_avg_percent, cpu_max_percent
- mem_avg_mb, mem_max_mb
- net_in_mb, net_out_mb
- bitrate_kbps
- restarts_before, restarts_after
- retry_index
- compose_network, rtmp_container, master_playlist_url
- scenario_log_file, metrics_samples_file, viewer_results_dir

Schema reduzido do arquivo results-tcc.csv:
- test_id, run_id, mode
- server, transcoder, viewers, repeat_index, status
- startup_hls_seconds
- latencia_ponta_a_ponta_seg, tempo_primeiro_segmento_seg
- cpu_avg_percent, cpu_max_percent
- mem_avg_mb, mem_max_mb
- bitrate_kbps
- segment_error_count, error_rate_percent
- restarts_session
- measurement_window_seconds
- observacoes

## Interpretacao rapida
- status=PASS: cenario finalizou com playlist disponivel e publisher sem erro critico.
- startup_hls_seconds: tempo ate master playlist responder 200.
- error_rate_percent: taxa de falhas HTTP dos viewers sinteticos.
- cpu/mem/net: resumo do container RTMP durante a janela ativa.
- restarts_before/after: evidencia de estabilidade (reinicios involuntarios).
- results.csv: diagnostico completo, paths de evidencias e metadados operacionais.
- results-tcc.csv: versao curta focada nas metricas da matriz do TCC.

## Troubleshooting
1. Se backend nao ficar pronto:
- Verifique logs em logs/backend.log do cenario.
- Confirme healthcheck do backend via actuator.

2. Se master playlist nao aparecer:
- Verifique logs do RTMP em logs/rtmp.log.
- Confirme stream key criada e URL RTMP usada pelo publisher.

3. Se houver muita falha HTTP:
- Reduza viewers e repita para confirmar gargalo.
- Verifique capacidade de CPU/memoria local.

4. Se cenario falhar e voce quiser rerun pontual:

```bash
./benchmark/run-rtmp-benchmark.sh --mode sanity --scenario <server+transcoder> --duration 90 --viewers 1
```

## Proximos passos
1. Aumentar viewers-list gradualmente (ex.: 1,10,25,50,75) e comparar curvas.
2. Aumentar duracao para 10-15 min em cenarios finalistas.
3. Exportar o CSV para planilha/analise estatistica (media, desvio, percentis).
4. Opcional: adicionar stack de observabilidade separada (Prometheus/Grafana) sem alterar fluxo funcional.

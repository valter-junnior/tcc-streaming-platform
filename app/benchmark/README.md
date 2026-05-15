# Benchmark RTMP (Docker-only)

## Objetivo
Executar testes comparativos RTMP conforme a matriz em docs/tcc/Matriz e Plano de Testes.md para as combinacoes:
- nginx + ffmpeg
- nginx + gstreamer
- srs + ffmpeg
- srs + gstreamer

Com cargas:
- 1 viewer
- 10 viewers
- 50 viewers

## Como funciona
1. O script sobe a stack com Docker Compose para cada cenario.
2. Cria uma stream via API do backend.
3. Publica uma live sintetica (ffmpeg em container) pelo tempo configurado.
4. Inicia viewers sinteticos (containers curl) para simular audiencia.
5. Coleta metricas do container RTMP com docker stats.
6. Consolida os resultados em dois CSVs:
- resultado_bruto.csv (detalhado)
- resultado_tcc.csv (resumido para uso no TCC)

Toda execucao e feita somente com Docker.

## Comandos
Executar da pasta app:

```bash
cd app
```

Sanity (1 caso por combinacao):

```bash
./benchmark/run-rtmp-benchmark.sh --mode sanity --duration 90 --viewers 1 --retries 0
```

Matriz completa (1, 10, 50 viewers):

```bash
./benchmark/run-rtmp-benchmark.sh --mode full --duration 300 --viewers-list 1,10,50 --repeats 1 --retries 0
```

Matriz completa com agregacao estatistica e relatorio TCC automaticos:

```bash
./benchmark/run-rtmp-benchmark.sh --mode full --duration 300 --repeats 3 --aggregate
```

Ou em dois passos separados:

```bash
# 1. Executar benchmark
./benchmark/run-rtmp-benchmark.sh --mode full --duration 300 --repeats 3

# 2. Agregar resultados (gera results-aggregated.csv)
python3 benchmark/aggregate-results.py benchmark/results/latest/results.csv benchmark/results/latest/

# 3. Gerar relatorio TCC final (gera docs/benchmark/report-tcc-final.md)
python3 benchmark/generate-tcc-report.py benchmark/results/latest/results-aggregated.csv
```

## Estrutura de saida
Todos os resultados ficam em:

- `app/benchmark/results/<run_id>/`   — resultados da execucao especifica
- `app/benchmark/resultado_bruto.csv` — ultimo resultado bruto completo (copia)
- `app/benchmark/resultado_tcc.csv`   — ultimo resultado TCC (copia)
- `docs/benchmark/resultado_tecnico.md` — ultimo relatorio tecnico
- `docs/benchmark/resultado_tcc.md`     — ultimo relatorio TCC resumido
- `docs/benchmark/report-tcc-final.md`  — relatorio final com analise estatistica (gerado com --aggregate)

Dentro de cada `<run_id>/`:
- `results.csv`               — dados brutos de todas as execucoes
- `results-tcc.csv`           — dados resumidos para o TCC
- `results-aggregated.csv`    — media e desvio padrao por combinacao (gerado por aggregate-results.py)
- `report-technical.md`       — relatorio tecnico da execucao
- `report-tcc.md`             — relatorio TCC da execucao
- `report-tcc-final.md`       — relatorio final com tabelas e avaliacao de criterios (gerado por generate-tcc-report.py)
- `runner-manifest.md`        — manifesto dos cenarios planejados
- `summary.txt`               — resumo rapido (pass/fail/totais)
- `logs/`                     — logs por cenario

## Resultados esperados
- PASS para os cenarios que conseguem publicar a live, servir playlists HLS e finalizar sem erro critico.
- Startup HLS idealmente <= 15s (criterio da matriz).
- Taxa de erro de segmento idealmente <= 1%.
- Reinicios involuntarios idealmente 0.

## Dicionario de colunas

### CSV 1: resultado_bruto.csv
- run_id: identificador da execucao.
- mode: sanity ou full.
- scenario: combinacao servidor+transcoder.
- server: nginx ou srs.
- transcoder: ffmpeg ou gstreamer.
- repeat_index: numero da repeticao (inteiro).
- viewers: quantidade de espectadores sinteticos (inteiro).
- duration_seconds: duracao da live ativa em segundos.
- status: PASS ou FAIL.
- error_message: motivo da falha quando houver.
- start_ts: unix timestamp (s) de inicio da medicao.
- end_ts: unix timestamp (s) de fim da medicao.
- measurement_start_ts: timestamp do marcador de inicio da janela ativa.
- measurement_end_ts: timestamp do marcador de fim da janela ativa.
- measurement_window_seconds: duracao real medida da janela ativa (s).
- startup_hls_seconds: tempo ate master playlist responder 200 (s).
- request_total: total de requisicoes HTTP dos viewers.
- request_errors: total de respostas de erro dos viewers.
- error_rate_percent: taxa de erro HTTP (%).
- cpu_avg_percent: uso medio de CPU do container RTMP (%).
- cpu_max_percent: pico de CPU do container RTMP (%).
- mem_avg_mb: memoria media do container RTMP (MB).
- mem_max_mb: memoria maxima do container RTMP (MB).
- net_in_mb: trafego de entrada acumulado durante a medicao (MB).
- net_out_mb: trafego de saida acumulado durante a medicao (MB).
- bitrate_kbps: bitrate efetivo estimado por segmentos HLS (kbps).
- restarts_before: restart count do container antes do cenario.
- restarts_after: restart count do container apos o cenario.
- retry_index: tentativa usada no cenario (0 = primeira tentativa).
- compose_network: rede Docker usada no cenario.
- rtmp_container: nome do container RTMP alvo.
- master_playlist_url: URL da master playlist usada na medicao.
- scenario_log_file: caminho do log principal do cenario.
- metrics_samples_file: caminho da serie temporal de metricas.
- viewer_results_dir: pasta com arquivos de resultado dos viewers.

### CSV 2: resultado_tcc.csv
Formato resumido para analise no TCC.
- Execucao: ID de teste T01..T12 conforme matriz experimental.
- Servidor: nginx ou srs.
- Transcodificador: ffmpeg ou gstreamer.
- Espectadores: 1, 10 ou 50.
- Duracao_s: duracao da live ativa (s).
- Repeticao: indice da repeticao (inteiro).
- Startup_HLS_s: tempo ate disponibilidade da master playlist (s).
- Tempo_Primeiro_Segmento_s: tempo ate primeiro segmento (s).
- CPU_medio_percent: CPU media do container RTMP (%).
- CPU_max_percent: CPU maxima do container RTMP (%).
- RAM_media_MB: memoria media (MB).
- RAM_max_MB: memoria maxima (MB).
- Bitrate_efetivo_kbps: bitrate efetivo (kbps).
- Erros_segmento: contagem total de erros de segmento.
- Taxa_erros_percent: taxa de erro de segmento (%).
- Reinicios_sessao: quantidade de reinicios involuntarios na sessao.
- Latencia_ponta_a_ponta_s: latencia OBS->player (manual nesta versao).
- Status: PASS ou FAIL.
- Observacoes: alertas relevantes para leitura rapida.

## Scripts auxiliares

### aggregate-results.py
Agrega N repeticoes por combinacao (server × transcoder × viewers) calculando media e desvio padrao para cada metrica.

```
python3 aggregate-results.py <results.csv> [output_dir]
```

Saida: `results-aggregated.csv` com colunas `<metrica>_mean` e `<metrica>_stddev`.

### generate-tcc-report.py
Gera `report-tcc-final.md` a partir do `results-aggregated.csv` com:
- Tabelas comparativas (mean ± stddev) por metrica
- Avaliacao PASS/FAIL dos criterios de aceitacao do Plano de Testes
- Notas metodologicas (CPU elevado em ambiente compartilhado, latencia manual)

```
python3 generate-tcc-report.py <results-aggregated.csv> [output_dir]
```

Saida padrao: `docs/benchmark/report-tcc-final.md`.

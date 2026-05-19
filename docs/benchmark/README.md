# Benchmark RTMP (Docker-only)

## Objetivo
Executar testes comparativos RTMP conforme a matriz em docs/tcc/report/Matriz e Plano de Testes.md para as combinacoes:
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
6. Consolida os resultados em CSV bruto e, opcionalmente, CSV agregado:
- results.csv (bruto, por run)
- results-aggregated.csv (agregado estatistico, quando `--aggregate` e usado)
- resultado.csv (planilha final da matriz T01..T12 para analise do TCC)

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

```

## Estrutura de saida
- `app/benchmark/results/<run_id>/`   — resultados da execucao especifica
- `app/benchmark/results/latest/`      — ponteiro para a ultima execucao gerada
- `docs/benchmark/results.csv`         — copia do bruto mais recente (auditoria)
- `docs/benchmark/results-raw.csv`     — copia do bruto mais recente (compatibilidade)
- `docs/benchmark/results-aggregated.csv` — ultimo CSV agregado para analise comparativa e conclusoes (gerado com `--aggregate`)
- `docs/benchmark/resultado.csv`       — planilha final consolidada da matriz (gerada com `--aggregate`)
- `docs/benchmark/run-console.log`     — espelho do console da ultima execucao

Dentro de cada `<run_id>/`:
- `results.csv`               — dados brutos de todas as execucoes (fonte de verdade)
- `results-aggregated.csv`    — consolidacao estatistica por combinacao (media, desvio padrao e avaliacao PASS/FAIL) para comparar cenarios com menor ruido entre repeticoes
- `resultado.csv`             — formato final para apresentacao (T01..T12 + metricas essenciais do plano de testes)
- `run-console.log`           — tudo que apareceu no terminal durante a execucao
- `summary.txt`               — resumo rapido (pass/fail/totais)
- `logs/`                     — logs por cenario

## Para que serve o results-aggregated.csv
- Reduz variacao entre execucoes repetidas ao consolidar os dados por combinacao (`server` x `transcoder` x `viewers`).
- Facilita comparacoes justas entre cenarios sem depender de uma unica execucao isolada.
- Apoia a analise do TCC com indicadores prontos (`*_mean`, `*_stddev`, `pass_*`, `overall_pass_fail`).
- Deve ser usado como base para conclusoes e tabelas comparativas; o `results.csv` continua sendo a fonte de verdade para auditoria detalhada.

## Para que serve o resultado.csv
- Entrega a planilha final no formato da matriz experimental (T01..T12), pronta para uso no TCC.
- Mantem apenas o necessario do plano de testes: startup HLS, CPU, memoria, bitrate, taxa de erros, reinicios e status.
- Inclui colunas de apoio para metricas manuais fora do escopo automatizado atual (`Latencia_ponta_a_ponta_s` e `Tempo_primeiro_segmento_s`).

## Como interpretar as metricas
- `CPU Média (%)` e `CPU Máxima (%)` representam a ocupação observada no container RTMP durante a execução. O valor pode passar de 100% porque o Docker contabiliza uso agregado de mais de uma thread do host.
- `RAM Média (MB)` e `RAM Máxima (MB)` representam o consumo observado no container. Para entender o peso real, compare esses valores com a RAM total documentada no host (24 GB) e com a carga dos demais containers.
- `Taxa de Erros (%)`, `Bitrate Efetivo (kbps)`, `Startup HLS Média (s)` e `Reinícios Sessão` devem ser lidos como indicadores do comportamento do cenário naquele host específico, não como valores absolutos independentes do ambiente.
- A comparação entre cenários só é defensável porque todos eles foram executados no mesmo ambiente de testes descrito no plano.

## Hardware do ambiente de testes
O plano de testes registra o ambiente usado no estudo:

| Item | Especificação |
|---|---|
| Sistema Operacional | Ubuntu 24.04.3 LTS (x86_64), kernel 6.17.0-23-generic |
| CPU | Intel Core i5-13420H (13ª geração, 12 threads, 2 threads/núcleo) |
| RAM total | 24 GB |
| Armazenamento | NVMe SSD (231 GB) |
| Rede | Loopback / bridge Docker |
| Virtualização | Docker Engine |

Observação: a RAM "disponível" durante a execução varia conforme o uso do sistema operacional e dos containers. O valor documentado no plano é a capacidade total do host; para medir disponível em tempo real, use `free -h` ou `cat /proc/meminfo` no momento do benchmark.

## Resultados esperados
- PASS para os cenarios que conseguem publicar a live, servir playlists HLS e finalizar sem erro critico.
- Startup HLS idealmente <= 24s (criterio recalibrado para este estudo).
- CPU media idealmente <= 240% (1 e 10 viewers) e <= 280% (50 viewers).
- Taxa de erro de segmento idealmente <= 1% (1 e 10 viewers) e <= 3% (50 viewers).
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

## Scripts auxiliares

### aggregate-results.py
Agrega N repeticoes por combinacao (server × transcoder × viewers) calculando media e desvio padrao para cada metrica. Tambem avalia os criterios de aceitacao do Plano de Testes, adicionando colunas `pass_*` e `overall_pass_fail`.

```
python3 aggregate-results.py <results.csv> [output_dir]
```

Saida: `results-aggregated.csv` — um CSV unico com tudo necessario para a analise do TCC:
- `<metrica>_mean` e `<metrica>_stddev` para cada metrica
- `pass_startup_hls_seconds`, `pass_cpu_avg_percent`, `pass_error_rate_percent`, `pass_restarts_after`
- `overall_pass_fail`

Saida adicional: `resultado.csv` — visao final para comparacao da matriz:
- `Execucao` (T01..T12), `Servidor`, `Transcodificador`, `Espectadores`, `Repeticoes`
- metricas consolidadas essenciais do plano de testes
- `Status` com legenda (`✓`, `⚠`, `✗`) e `Observacoes`

Saida de rastreamento: `run-console.log` — espelho textual de tudo que apareceu na tela durante a execução.

Interpretacao recomendada:
- Use `*_mean` para comparar desempenho medio entre cenarios.
- Use `*_stddev` para avaliar estabilidade (quanto menor, mais consistente).
- Use `overall_pass_fail` para triagem rapida de conformidade com os criterios.

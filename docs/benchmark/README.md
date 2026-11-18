# Benchmark RTMP

A [execução analisada no TCC](runs/bench-full-20261118-095048/) contém 36 repetições de uma matriz fatorial com dois servidores RTMP (Nginx-RTMP e SRS), dois transcodificadores (FFmpeg e GStreamer) e três cargas (1, 10 e 50 viewers). Os dados públicos estão em [`results.csv`](runs/bench-full-20261118-095048/results.csv), [`results-aggregated.csv`](runs/bench-full-20261118-095048/results-aggregated.csv) e [`resultado.csv`](runs/bench-full-20261118-095048/resultado.csv). O [guia de reprodução](REPRODUCIBILITY.md) explica o procedimento, as métricas e as limitações.

## Executar localmente

```bash
cd app
cp .env.example .env
# Defina senhas próprias em .env e mantenha esse arquivo fora do Git.
./benchmark/run-rtmp-benchmark.sh --mode full --duration 300 --viewers-list 1,10,50 --repeats 3 --aggregate
```

A matriz completa leva várias horas. Os arquivos detalhados são gravados em `app/benchmark/results/<run_id>/`, ignorado pelo Git. Depois de revisar e auditar uma nova execução, a opção `--publish` pode exportar os CSVs sanitizados para `docs/benchmark/runs/<run_id>/` sem sobrescrever as coletas anteriores. Confira os arquivos exportados antes de commitar.

CPU e memória são do contêiner RTMP/transcodificador, não do host inteiro. `bitrate_kbps` estima uma variante HLS. Os campos legados `startup_hls_seconds` e `tempo_primeiro_segmento_s` contam ciclos de sondagem, enquanto `latencia_ponta_a_ponta_s` não mede a reprodução no player. A licença [CC BY 4.0](LICENSE-DATA.md) cobre os CSVs identificados, não o código nem a monografia.

# Relatorio Tecnico do Benchmark RTMP

## Execucao
- Run ID: bench-full-20260507-095238
- Modo: full
- Duracao configurada por cenario: 30s
- Repeticoes configuradas: 1
- Viewers configurados: 1,10,50
- Total de execucoes: 12
- PASS: 12
- FAIL: 0

## Artefatos principais
- CSV robusto: /home/junnior/Documentos/apps/tcc/app/benchmark/results/bench-full-20260507-095238/results.csv
- CSV resumido TCC: /home/junnior/Documentos/apps/tcc/app/benchmark/results/bench-full-20260507-095238/results-tcc.csv
- Summary: /home/junnior/Documentos/apps/tcc/app/benchmark/results/bench-full-20260507-095238/summary.txt

## Observacoes
- O CSV robusto preserva metadados operacionais, paths de logs e evidencias por cenario.
- O CSV resumido foca nas metricas centrais da matriz de testes do TCC.
- A latencia ponta-a-ponta permanece como medicao manual e por isso aparece como manual no CSV resumido.

## Tabela resumida
| Teste | Servidor | Transcodificador | Viewers | Repeticao | Status | Startup HLS (s) | CPU medio (%) | Mem media (MB) | Bitrate (kbps) | Erros segmento | Reinicios |
|---|---|---|---:|---:|---|---:|---:|---:|---:|---:|---:|
| T01 | nginx | ffmpeg | 1 | 1 | PASS | 14 | 172.3661 | 1284.5316 | 3330.0283 | 0 | 0 |
| T02 | nginx | ffmpeg | 10 | 1 | PASS | 15 | 162.8779 | 1424.5521 | 3333.1617 | 0 | 0 |
| T03 | nginx | ffmpeg | 50 | 1 | PASS | 15 | 150.3584 | 1366.7099 | 3341.7000 | 0 | 0 |
| T04 | nginx | gstreamer | 1 | 1 | PASS | 14 | 178.9350 | 636.3156 | 5129.8933 | 0 | 0 |
| T05 | nginx | gstreamer | 10 | 1 | PASS | 16 | 164.0450 | 536.5464 | 5134.2382 | 0 | 0 |
| T06 | nginx | gstreamer | 50 | 1 | PASS | 16 | 152.0419 | 556.3611 | 5132.4836 | 0 | 0 |
| T07 | srs | ffmpeg | 1 | 1 | PASS | 6 | 247.4746 | 1073.8065 | 3111.5253 | 0 | 0 |
| T08 | srs | ffmpeg | 10 | 1 | PASS | 5 | 216.7400 | 1175.9801 | 3105.0707 | 0 | 0 |
| T09 | srs | ffmpeg | 50 | 1 | PASS | 6 | 266.2413 | 1224.0911 | 3112.3400 | 0 | 0 |
| T10 | srs | gstreamer | 1 | 1 | PASS | 1 | 274.3590 | 601.0270 | 5191.3067 | 11 | 0 |
| T11 | srs | gstreamer | 10 | 1 | PASS | 1 | 286.0490 | 514.6230 | 5185.4787 | 115 | 0 |
| T12 | srs | gstreamer | 50 | 1 | PASS | 1 | 190.9233 | 528.9883 | 5039.2773 | 1061 | 0 |

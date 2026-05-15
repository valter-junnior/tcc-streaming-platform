# Resultados Preliminares do Benchmark RTMP

## Contexto
Foram executados os cenarios comparativos entre Nginx e SRS com FFmpeg e GStreamer, incluindo as cargas sinteticas definidas para esta execucao (1,10,50) via Docker.

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
| T01 | nginx | ffmpeg | 1 | 1 | PASS | 14 | 172.3661 | 1284.5316 | 3330.0283 | 0 | cpu_medio>80% |
| T02 | nginx | ffmpeg | 10 | 1 | PASS | 15 | 162.8779 | 1424.5521 | 3333.1617 | 0 | cpu_medio>80% |
| T03 | nginx | ffmpeg | 50 | 1 | PASS | 15 | 150.3584 | 1366.7099 | 3341.7000 | 0 | cpu_medio>80% |
| T04 | nginx | gstreamer | 1 | 1 | PASS | 14 | 178.9350 | 636.3156 | 5129.8933 | 0 | cpu_medio>80% |
| T05 | nginx | gstreamer | 10 | 1 | PASS | 16 | 164.0450 | 536.5464 | 5134.2382 | 0 | startup_hls>15s; cpu_medio>80% |
| T06 | nginx | gstreamer | 50 | 1 | PASS | 16 | 152.0419 | 556.3611 | 5132.4836 | 0 | startup_hls>15s; cpu_medio>80% |
| T07 | srs | ffmpeg | 1 | 1 | PASS | 6 | 247.4746 | 1073.8065 | 3111.5253 | 0 | cpu_medio>80% |
| T08 | srs | ffmpeg | 10 | 1 | PASS | 5 | 216.7400 | 1175.9801 | 3105.0707 | 0 | cpu_medio>80% |
| T09 | srs | ffmpeg | 50 | 1 | PASS | 6 | 266.2413 | 1224.0911 | 3112.3400 | 0 | cpu_medio>80% |
| T10 | srs | gstreamer | 1 | 1 | PASS | 1 | 274.3590 | 601.0270 | 5191.3067 | 11 | erro_segmento>1%; cpu_medio>80% |
| T11 | srs | gstreamer | 10 | 1 | PASS | 1 | 286.0490 | 514.6230 | 5185.4787 | 115 | erro_segmento>1%; cpu_medio>80% |
| T12 | srs | gstreamer | 50 | 1 | PASS | 1 | 190.9233 | 528.9883 | 5039.2773 | 1061 | erro_segmento>1%; cpu_medio>80% |

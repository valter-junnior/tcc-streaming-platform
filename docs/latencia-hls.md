# Latência HLS — Tempo até assistir a live após iniciar no OBS

> Valores baseados nas configurações padrão do `.env` e scripts de transcodificação.  
> `HLS_SEGMENT_DURATION=6`, `STREAM_STABILIZE_SECONDS=3`, GOP=48 frames @ 30fps.

---

## Nginx + FFmpeg

| Etapa | Tempo estimado |
|---|---|
| OBS estabelece conexão RTMP | ~0,5s |
| nginx invoca `on_publish` → callback HTTP para o backend | ~0,2s |
| nginx faz `exec transcode.sh` (fork do processo) | ~0,1s |
| Verificação de espaço em disco + setup de logs | ~0,1s |
| `STREAM_STABILIZE_SECONDS` (sleep fixo) | **3s** |
| FFmpeg conecta ao RTMP local, inicializa `filter_complex split=4` com 4 encoders | ~2–3s |
| FFmpeg codifica e grava o 1º segmento (`HLS_SEGMENT_DURATION` = 6s de vídeo) | **6s** |
| Player HLS acumula buffer mínimo de 2 segmentos antes de dar play | **6s** |
| Player detecta a playlist e inicia reprodução | ~1s |
| **Total** | **~19–22s** |

---

## Nginx + GStreamer

| Etapa | Tempo estimado |
|---|---|
| OBS estabelece conexão RTMP | ~0,5s |
| nginx invoca `on_publish` → callback HTTP para o backend | ~0,2s |
| nginx faz `exec transcode.sh` | ~0,1s |
| Verificação de espaço em disco | ~0,1s |
| `wait_for_input_ready`: loop de ffprobe até o RTMP estar legível | ~1–3s |
| `STREAM_STABILIZE_SECONDS` (sleep adicional após input pronto) | **3s** |
| GStreamer negocia pipelines, conecta ao RTMP, inicializa decoders/encoders | ~3–5s |
| 1º segmento gravado | **6s** |
| Player acumula buffer de 2 segmentos | **6s** |
| Player inicia reprodução | ~1s |
| **Total** | **~21–28s** |

---

## Principais fatores de latência

| Fator | Impacto | Variável |
|---|---|---|
| `STREAM_STABILIZE_SECONDS=3` | Sleep fixo intencional para aguardar os primeiros keyframes do OBS | `TRANSCODER_STREAM_STABILIZE_SECONDS` |
| `HLS_SEGMENT_DURATION=6` | Cada segmento precisa de 6s de vídeo para ser fechado e escrito | `HLS_SEGMENT_DURATION` |
| GOP = 48 frames @ 30fps = ~1,6s | FFmpeg só fecha segmentos em múltiplos do GOP → segmentos reais ~6,4s | `-g 48 -keyint_min 48` nos scripts |
| Buffer do player (hls.js) | Aguarda mínimo de 2 segmentos na playlist antes de iniciar (proteção contra stall) | configuração do player |

---

## Como reduzir a latência

Os maiores ganhos estão em dois parâmetros, com tradeoffs:

| Ajuste | Ganho | Tradeoff |
|---|---|---|
| `HLS_SEGMENT_DURATION=4` (de 6 para 4) | ~4s a menos | Mais requisições HTTP ao servidor HLS; maior sensibilidade a micro-interrupções |
| `STREAM_STABILIZE_SECONDS=1` (de 3 para 1) | ~2s a menos | Maior chance de FFmpeg/GStreamer tentar conectar antes do OBS enviar o primeiro keyframe |
| GOP menor (ex: `-g 30`) | ~1s a menos | Arquivo `.ts` maior por segmento; mais overhead de codificação |

Para ambiente de desenvolvimento/TCC, os valores padrão oferecem boa estabilidade. Para produção com foco em baixa latência, considerar `HLS_SEGMENT_DURATION=4`.

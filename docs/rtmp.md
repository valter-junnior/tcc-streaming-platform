# RTMP/HLS - Nginx e Transcodificacao

Atualizado em: 2026-03-23

## Estrutura relevante

```text
app/backend/rtmp/
  nginx/
    Dockerfile
    configs/nginx.conf
    scripts/
      docker-entrypoint.sh
      transcode-ffmpeg.sh
      transcode-gstreamer.sh
      cleanup-hls.sh
  srs/
    Dockerfile
    srs.conf
    cleanup-hls.sh
```

## Como o container RTMP funciona hoje

No compose principal (`app/docker-compose.yml`) o servico `rtmp-server` usa a imagem custom de `nginx/`.

Fluxo de inicializacao (`docker-entrypoint.sh`):
1. Define defaults das variaveis de ambiente.
2. Escolhe o script de transcodificacao (`ffmpeg` ou `gstreamer`) via symlink em `/usr/local/bin/transcode.sh`.
3. Renderiza `nginx.conf` com `envsubst`.
4. Normaliza permissoes em `${HLS_PATH}`.
5. Aguarda backend (`streaming-platform`) e sobe Nginx.

## Variaveis de ambiente principais

### Ingestao/HTTP
- `RTMP_PORT` (default `1935`)
- `RTMP_CHUNK_SIZE` (default `4096`)
- `HLS_PATH` (default `/tmp/hls`)
- `HLS_HTTP_PORT` (default `8081`)
- `BACKEND_HOST` (default `streaming-platform`)
- `BACKEND_PORT` (default `8080`)
- `HLS_RETENTION_HOURS` (default `6`)

### Transcodificacao
- `TRANSCODER=ffmpeg|gstreamer`
- `HLS_SEGMENT_DURATION` (default `6`)
- `HLS_PLAYLIST_LENGTH` (default `10`)
- `TRANSCODER_MAX_RETRIES` (default `10`)
- `TRANSCODER_RETRY_WAIT` (default `3`)
- `TRANSCODER_STREAM_STABILIZE_SECONDS` (default `8`)
- `TRANSCODER_STARTUP_TIMEOUT` (default `20`, usado no GStreamer)

## Callbacks configurados no Nginx

Arquivo: `app/backend/rtmp/nginx/configs/nginx.conf`

- `on_publish -> POST /api/streams/callback/publish`
- `on_publish_done -> POST /api/streams/callback/publish_done`

Esses callbacks controlam autorizacao de stream key e transicao de status da stream.

## Perfis de transcodificacao no estado atual

### FFmpeg (`transcode-ffmpeg.sh`)
- Entrada: `rtmp://127.0.0.1/live/{streamKey}`
- Saida: `master.m3u8` com variantes:
  - `v0` 1080p
  - `v1` 720p
  - `v2` 480p
  - `v3` 360p
- Implementa retry interno e validacao de stream key por regex.

### GStreamer (`transcode-gstreamer.sh`)
- Entrada: `rtmp://127.0.0.1/live/{streamKey}`
- Saida atual estabilizada: `master.m3u8` com variantes:
  - `v0` 1080p
  - `v2` 480p
- Implementa:
  - checagem de input RTMP antes de iniciar pipelines;
  - bootstrap/retry interno;
  - cleanup de processos filhos;
  - lock por stream key para evitar concorrencia.

## Entrega HLS

- URL base esperada no frontend: `http://localhost:8081/hls/{streamKey}/master.m3u8`
- `location /hls/` usa `alias ${HLS_PATH}/`.
- CORS e headers para playlists/segmentos estao configurados no Nginx.

## Limpeza de arquivos HLS

`cleanup-hls.sh` roda via cron (a cada hora) e remove arquivos antigos conforme `HLS_RETENTION_HOURS`.

Logs:
- Nginx: `/var/log/nginx/error.log`, `/var/log/nginx/access.log`
- Limpeza: `/var/log/nginx/hls-cleanup.log`
- Transcodificacao por stream: `/tmp/hls/{streamKey}/transcode.log`

## SRS no repositorio

A pasta `app/backend/rtmp/srs/` contem Dockerfile e configuracao de SRS para estudos/alternativas, mas o compose principal atual nao sobe um servico SRS.

`Nao confirmado`: fluxo completo de SRS integrado ao backend/frontend no mesmo pipeline principal desta branch.

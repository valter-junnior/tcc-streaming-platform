# Plataforma de Streaming — TCC

Plataforma de streaming ao vivo desenvolvida para comparação de tecnologias de ingestão RTMP e transcodificação.

No estado atual do código, o ambiente principal em `app/docker-compose.yml` sobe:
- PostgreSQL
- RabbitMQ
- Backend Spring Boot (`streaming-platform`)
- RTMP Server selecionável por profile (`Nginx` ou `SRS`)
- Frontend React

O streamer transmite via OBS, o backend valida a stream key por callback HTTP, o transcodificador ativo gera HLS, e o espectador assiste no navegador com HLS.js + Plyr.

## Matriz comparativa (objetivo do TCC)

| | FFmpeg | GStreamer |
|---|---|---|
| **Nginx-RTMP** | Ativo no compose atual | Ativo no compose atual |
| **SRS** | Ativo no compose atual | Ativo no compose atual |

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 3.2, Spring Data JPA, Spring AMQP |
| Banco de dados | PostgreSQL 16 |
| Mensageria | RabbitMQ 3 (management) |
| Ingestão RTMP | Nginx compilado com `nginx-rtmp-module` |
| Transcodificação | FFmpeg ou GStreamer (seleção via `TRANSCODER`) |
| Entrega | HLS via HTTP no container RTMP |
| Frontend | React 19, TypeScript, Vite, React Router 7, TanStack Query 5 |
| Player | HLS.js + Plyr |
| Tempo real | Server-Sent Events (SSE) |

## Subida do ambiente

### Pré-requisitos

- Docker e Docker Compose
- Portas livres: `1935`, `8080`, `8081`, `3001`, `5672`, `15672`

### 1. Configurar ambiente

```bash
cd app
cp .env.example .env
```

### 2. Subir serviços

```bash
docker compose --profile "${RTMP_SERVER:-nginx}" up -d --build
docker compose ps
```

`RTMP_SERVER` define qual servidor RTMP será iniciado:
- `nginx`: sobe `rtmp-server-nginx`
- `srs`: sobe `rtmp-server-srs`

### 3. Validar backend

```bash
curl http://localhost:8080/actuator/health
```

### 4. Abrir frontend

`http://localhost:3001`

## Trocar transcodificador

No `.env`:

```env
TRANSCODER=ffmpeg
# ou
TRANSCODER=gstreamer
```

Recriar o serviço RTMP:

```bash
docker compose --profile "${RTMP_SERVER:-nginx}" up -d --build --force-recreate
```

Observação importante:
- `ffmpeg` gera 4 variantes (`1080p`, `720p`, `480p`, `360p`).
- `gstreamer` está estabilizado com 2 variantes (`1080p`, `480p`) no `master.m3u8` atual.

## Cenários mínimos (RTMP + Transcoder)

Todos os cenários usam o mesmo comando base:

```bash
RTMP_SERVER=<nginx|srs> TRANSCODER=<ffmpeg|gstreamer> \
docker compose --profile "$RTMP_SERVER" up -d --build --force-recreate
```

Exemplos:

```bash
# 1) nginx + ffmpeg
RTMP_SERVER=nginx TRANSCODER=ffmpeg docker compose --profile nginx up -d --build --force-recreate

# 2) nginx + gstreamer
RTMP_SERVER=nginx TRANSCODER=gstreamer docker compose --profile nginx up -d --build --force-recreate

# 3) srs + ffmpeg
RTMP_SERVER=srs TRANSCODER=ffmpeg docker compose --profile srs up -d --build --force-recreate

# 4) srs + gstreamer
RTMP_SERVER=srs TRANSCODER=gstreamer docker compose --profile srs up -d --build --force-recreate
```

## Configuração do OBS

1. Serviço: `Personalizado`
2. Servidor: `rtmp://localhost:1935/live`
3. Stream key: gerada ao criar a stream no frontend

Parâmetros recomendados:
- Encoder: `x264`
- Bitrate: `4000-6000 kbps`
- Keyframe interval: `2s`

## Endpoints úteis

- Root API: `GET /`
- Swagger: `http://localhost:8080/swagger-ui.html`
- Actuator health: `GET /actuator/health`
- Streams: `POST /api/streams`, `GET /api/streams/live`, `GET /api/streams/{id}`
- SSE: `GET /api/sse/stream/{streamId}/subscribe`

## Limitações conhecidas

- O compose principal não possui serviço SRS ativo.
- Monitoramento com Prometheus/Grafana não está orquestrado no compose atual.

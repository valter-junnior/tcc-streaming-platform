# Plataforma de Streaming — TCC

Plataforma de streaming ao vivo desenvolvida para comparação de tecnologias de ingestão RTMP e transcodificação.

No estado atual do código, o ambiente principal em `app/docker-compose.yml` sobe:
- PostgreSQL
- RabbitMQ
- Backend Spring Boot (`streaming-platform`)
- RTMP Server (`Nginx + nginx-rtmp-module`)
- Frontend React

O streamer transmite via OBS, o backend valida a stream key por callback HTTP, o transcodificador ativo gera HLS, e o espectador assiste no navegador com HLS.js + Plyr.

## Matriz comparativa (objetivo do TCC)

| | FFmpeg | GStreamer |
|---|---|---|
| **Nginx-RTMP** | Ativo no compose atual | Ativo no compose atual |
| **SRS** | Alternativa de laboratório | Alternativa de laboratório |

`Nao confirmado`: integração completa do SRS no fluxo principal de execução via compose atual.

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
docker compose up -d --build
docker compose ps
```

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
docker compose up -d --force-recreate rtmp-server
```

Observação importante:
- `ffmpeg` gera 4 variantes (`1080p`, `720p`, `480p`, `360p`).
- `gstreamer` está estabilizado com 2 variantes (`1080p`, `480p`) no `master.m3u8` atual.

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

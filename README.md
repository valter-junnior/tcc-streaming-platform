# Plataforma de Streaming — TCC

Plataforma de streaming ao vivo desenvolvida para comparação de tecnologias de ingestão RTMP e transcodificação. Os [dados tabulares do benchmark e o guia de reprodução](docs/benchmark/REPRODUCIBILITY.md) estão disponíveis neste repositório público, com os CSVs sob [CC BY 4.0](docs/benchmark/LICENSE-DATA.md).

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
| Ingestão RTMP | Nginx com `nginx-rtmp-module` ou SRS (profile do Compose) |
| Transcodificação | FFmpeg ou GStreamer (seleção via `TRANSCODER`) |
| Entrega | HLS via HTTP no container RTMP |
| Frontend | React 19, TypeScript, Vite, React Router 7, TanStack Query 5 |
| Player | HLS.js + Plyr |
| Tempo real | Server-Sent Events (SSE) |

## Subida do ambiente

### Pré-requisitos

- Docker e Docker Compose
- Portas livres: `1935`, `8080`, `8081`, `3001`, `5432`, `5672`, `15672`

### 1. Configurar ambiente

```bash
cd app
cp .env.example .env
# Defina senhas locais próprias em .env antes de iniciar.
```

O exemplo configura `COMPOSE_PROFILES=srs` e `TRANSCODER=gstreamer`. Ajuste esses valores no `.env` para escolher outro cenário.

### 2. Subir serviços

```bash
docker compose up -d --build
docker compose ps
```

`COMPOSE_PROFILES`, lido do `.env` pelo Docker Compose, define qual servidor RTMP será iniciado:
- `nginx`: sobe `rtmp-server-nginx`
- `srs`: sobe `rtmp-server-srs`

Também é possível usar `./run.sh up nginx` ou `./run.sh up srs`: o script atualiza `COMPOSE_PROFILES` no `.env`. Para trocar de servidor com o ambiente já ativo, use `./run.sh swap nginx` ou `./run.sh swap srs`, pois ambos usam as mesmas portas.

### 3. Validar backend

```bash
curl http://localhost:8080/actuator/health
```

### 4. Abrir frontend

[http://localhost:3001](http://localhost:3001)

## Trocar transcodificador

No `.env`:

```env
TRANSCODER=ffmpeg
# ou
TRANSCODER=gstreamer
```

Recriar o serviço RTMP:

```bash
docker compose up -d --build --force-recreate streaming-platform rtmp-server-srs
```

No exemplo acima, troque `rtmp-server-srs` por `rtmp-server-nginx` se esse for o profile selecionado. A variável `TRANSCODER` também é passada ao backend.

Observação importante:
- `ffmpeg` gera 4 variantes (`1080p`, `720p`, `480p`, `360p`).
- `gstreamer` declara 4 variantes (`1080p`, `720p`, `480p`, `360p`) nos scripts atuais. A disponibilidade real de cada variante deve ser confirmada em uma nova execução.

## Cenários mínimos (RTMP + Transcoder)

Execute os exemplos a partir de `app/`. Cada comando define o profile e o transcodificador apenas para aquela execução do Compose:

```bash
# 1) nginx + ffmpeg
COMPOSE_PROFILES=nginx TRANSCODER=ffmpeg docker compose up -d --build --force-recreate

# 2) nginx + gstreamer
COMPOSE_PROFILES=nginx TRANSCODER=gstreamer docker compose up -d --build --force-recreate

# 3) srs + ffmpeg
COMPOSE_PROFILES=srs TRANSCODER=ffmpeg docker compose up -d --build --force-recreate

# 4) srs + gstreamer
COMPOSE_PROFILES=srs TRANSCODER=gstreamer docker compose up -d --build --force-recreate
```

Ao trocar entre Nginx e SRS, encerre primeiro o cenário anterior com `docker compose down` para liberar as portas `1935` e `8081`. Para uso interativo, `./run.sh swap <nginx|srs>` faz essa troca e atualiza o `.env`.

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

- Apenas um servidor RTMP pode usar as portas publicadas por vez; selecione `nginx` ou `srs` em `COMPOSE_PROFILES`.
- Monitoramento com Prometheus/Grafana não está orquestrado no compose atual.

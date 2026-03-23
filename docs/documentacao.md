# Documentacao Tecnica - Plataforma de Streaming (Estado Atual)

Atualizado em: 2026-03-23

## 1. Visao Geral

A plataforma implementa transmissao ao vivo com ingestao RTMP, transcodificacao para HLS e reproducao no navegador.

Fluxo base:
1. Streamer cria stream no frontend.
2. Backend gera `streamKey` e retorna credenciais.
3. OBS publica em `rtmp://localhost:1935/live/{streamKey}`.
4. Nginx-RTMP chama callback no backend para validar e iniciar a stream.
5. Script de transcodificacao gera HLS em `/tmp/hls/{streamKey}`.
6. Frontend reproduz `master.m3u8` com HLS.js + Plyr.
7. Atualizacoes de status/viewers chegam por SSE.

## 2. Arquitetura em Execucao

Servicos orquestrados em `app/docker-compose.yml`:
- `postgres` (PostgreSQL 16)
- `rabbitmq` (RabbitMQ 3 management)
- `streaming-platform` (Spring Boot, executado com Maven)
- `rtmp-server` (Nginx custom com nginx-rtmp-module)
- `frontend` (React + Vite)

Notas:
- O compose principal usa Nginx-RTMP como servidor de ingestao.
- A escolha do transcodificador e feita por `TRANSCODER=ffmpeg|gstreamer` no container `rtmp-server`.

## 3. Stack Tecnologico

### Backend
- Java 21
- Spring Boot 3.2.2
- Spring Web
- Spring Data JPA
- Spring AMQP
- Spring Boot Actuator
- Micrometer Prometheus registry
- Spring Retry
- SpringDoc OpenAPI

### Frontend
- React 19
- TypeScript
- Vite 7
- React Router 7
- TanStack Query 5
- Axios
- HLS.js
- Plyr
- Tailwind CSS 4

### Infra RTMP/HLS
- Nginx 1.24 compilado com `nginx-rtmp-module`
- FFmpeg
- GStreamer + plugins base/good/bad/ugly/libav

## 4. Backend

Codigo em `app/backend/streaming-platform/src/main/java/com/tcc/streaming`.

### 4.1 Modulos logicos
- `stream`: regras de stream, endpoints, SSE e persistencia.
- `consumer`: consumo de eventos RabbitMQ e agregacao de metricas.
- `common`: configuracoes, exceptions, eventos e componentes compartilhados.

### 4.2 Endpoints HTTP

Base principal: `http://localhost:8080`

#### Root e configuracao
- `GET /` (RootController)
- `GET /api/config` (ConfigController)
- `GET /swagger-ui.html`
- `GET /actuator/health`

#### Streams (`/api/streams`)
- `POST /api/streams`
- `GET /api/streams/live`
- `GET /api/streams/{id}`
- `GET /api/streams/{id}/status`
- `POST /api/streams/validate`
- `POST /api/streams/{id}/restart`
- `GET /api/streams/my?ownerId=...`
- `PUT /api/streams/{id}`
- `DELETE /api/streams/{id}?ownerId=...`

#### Callbacks RTMP (`/api/streams/callback`)
- `POST /publish`
- `POST /publish_done`

#### Viewers
- `POST /api/streams/{streamId}/join?viewerId=...&countAsViewer=true|false`
- `GET /api/streams/{streamId}/leave?viewerId=...&countAsViewer=true|false`
- `POST /api/streams/{streamId}/leave?viewerId=...&countAsViewer=true|false`

#### SSE
- `GET /api/sse/stream/{streamId}/subscribe?viewerId=...&countAsViewer=true|false`

Eventos SSE emitidos:
- `stream_status`
- `viewers_update`

### 4.3 Mensageria (RabbitMQ)

Configuracao em `RabbitMQConfig`:
- Exchange: `stream.exchange` (topic)
- Fila: `stream.events` com chaves
  - `stream.created`
  - `stream.started`
  - `stream.ended`
- Fila: `metrics.events` com chaves
  - `viewer.joined`
  - `viewer.left`

Consumers:
- `StreamEventConsumer`
- `ViewerEventConsumer`

Agendador:
- `MetricsAggregationScheduler`

### 4.4 Persistencia

Banco principal: PostgreSQL.

Entidade principal: `Stream` com ciclo de vida `WAITING -> LIVE -> ENDED`.

Observacao operacional:
- `spring.jpa.open-in-view=false` (evita manter conexao de banco aberta durante SSE).

## 5. Frontend

Codigo em `app/frontend/src`.

### 5.1 Rotas
- `/` (Home)
- `/my-streams`
- `/dashboard/:streamId`
- `/watch/:streamId`

### 5.2 Servicos aplicacionais
- `apiService.ts`: CRUD de streams e status.
- `viewerService.ts`: join/leave de viewers.
- `sseService.ts`: assinatura de eventos SSE com tentativa de reconexao.
- `configService.ts`: leitura de `/api/config`.

### 5.3 Variaveis de ambiente (frontend)
- `VITE_API_URL` (default `http://localhost:8080/api`)
- `VITE_API_BASE_URL` (default `http://localhost:8080`)
- `VITE_RTMP_URL` (default `rtmp://localhost:1935/live`)
- `VITE_HLS_URL` (default `http://localhost:8081/hls`)
- `VITE_APP_URL` (default `http://localhost:3001`)

### 5.4 Playback

A pagina de visualizacao monta URL:
- `http://localhost:8081/hls/{streamKey}/master.m3u8`

O player usa:
- HLS.js para ABR e selecao manual de qualidade.
- Plyr como UI do player.

## 6. Infra RTMP e Transcodificacao

### 6.1 Nginx-RTMP

Arquivo: `app/backend/rtmp/nginx/configs/nginx.conf`.

Pontos principais:
- Ingestao em `application live` na porta RTMP configuravel.
- `on_publish` e `on_publish_done` apontando para backend.
- Execucao do script ativo via `exec /usr/local/bin/transcode.sh $name`.
- HLS servido em `/hls/` com `alias ${HLS_PATH}/`.
- CORS habilitado para playlists e segmentos.
- `/stat` e `/control` restritos a rede interna.

### 6.2 EntryPoint e selecao de transcodificador

Arquivo: `app/backend/rtmp/nginx/scripts/docker-entrypoint.sh`.

- Seleciona script por symlink:
  - `transcode-ffmpeg.sh`
  - `transcode-gstreamer.sh`
- Gera `nginx.conf` via `envsubst`.
- Normaliza permissoes de `${HLS_PATH}` para evitar falhas de escrita.
- Aguarda backend antes de subir Nginx.

### 6.3 Perfis de transcodificacao

#### FFmpeg
Arquivo: `transcode-ffmpeg.sh`.

- Gera 4 variantes:
  - `v0` 1080p
  - `v1` 720p
  - `v2` 480p
  - `v3` 360p
- Retry interno para reduzir dependencia do respawn do nginx-rtmp.
- Validacao de `streamKey` com regex segura.

#### GStreamer
Arquivo: `transcode-gstreamer.sh`.

- Perfil atual estabilizado com 2 variantes no master playlist:
  - `v0` 1080p
  - `v2` 480p
- Retry interno com bootstrap de playlists e cleanup de processos filhos.
- Usa `hlssink2` e checagem de input RTMP antes de iniciar pipelines.

## 7. Testes

Testes em `app/backend/streaming-platform/src/test/java`:
- `StreamControllerE2ETest`
- `NginxCallbackControllerE2ETest`
- `AbstractE2ETest`
- `TestConfig`

As suites E2E exercitam os fluxos de stream e callbacks HTTP com ambiente de teste do backend.

## 8. Divergencias Corrigidas Nesta Atualizacao

1. Documentacao citava Redis como dependencia ativa do runtime principal.
- Estado real: Redis nao esta no compose principal atual.

2. Documentacao citava Prometheus/Grafana como servicos de observabilidade no compose.
- Estado real: ha exposicao de metricas via Actuator/Micrometer, mas sem servicos Prometheus/Grafana no compose atual.

3. Documentacao afirmava 4 qualidades para GStreamer.
- Estado real: `master.m3u8` do script GStreamer atual anuncia 2 variantes (`v0`, `v2`).

4. Documentacao descrevia callbacks SRS (`connect`, `play`, etc.) como parte do fluxo principal.
- Estado real: backend implementa callbacks usados pelo Nginx (`publish` e `publish_done`) no fluxo ativo.

5. Documentacao de endpoints de viewers omitia `GET /leave`.
- Estado real: backend suporta `GET` e `POST` para `/leave`.

## 9. Nao Confirmado

1. Integracao end-to-end do SRS no mesmo nivel do Nginx dentro do compose principal.
- Onde validar: `app/docker-compose.yml` e eventual compose alternativo de benchmark.

2. Pipeline de monitoramento com Prometheus/Grafana em execucao nesta branch.
- Onde validar: composicao de infraestrutura adicional e dashboards versionados.

## 10. Estrutura de Pastas Relevante

```text
app/
  docker-compose.yml
  backend/
    rtmp/
      nginx/
        configs/nginx.conf
        scripts/docker-entrypoint.sh
        scripts/transcode-ffmpeg.sh
        scripts/transcode-gstreamer.sh
      srs/
        Dockerfile
        srs.conf
    streaming-platform/
      src/main/java/com/tcc/streaming/
      src/test/java/com/tcc/streaming/
  frontend/
    src/
      app/
      features/

docs/
  documentacao.md
  rtmp.md
  diagrams/
```

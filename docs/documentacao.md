# Documentação Técnica — Plataforma de Streaming com Análise Comparativa

## Índice

1. [Visão Geral](#1-visão-geral)
2. [Arquitetura](#2-arquitetura)
3. [Stack Tecnológico](#3-stack-tecnológico)
4. [Backend](#4-backend)
5. [Frontend](#5-frontend)
6. [Infraestrutura RTMP](#6-infraestrutura-rtmp)
7. [Mensageria e Cache](#7-mensageria-e-cache)
8. [API REST](#8-api-rest)
9. [Eventos SSE](#9-eventos-sse)
10. [Modelo de Dados](#10-modelo-de-dados)
11. [Fluxos Principais](#11-fluxos-principais)
12. [Monitoramento](#12-monitoramento)
13. [Testes](#13-testes)
14. [Estrutura do Projeto](#14-estrutura-do-projeto)
15. [Referências](#15-referências)

---

## 1. Visão Geral

Este Trabalho de Conclusão de Curso desenvolve uma plataforma de streaming de vídeo ao vivo com foco na análise comparativa de diferentes tecnologias de ingestão RTMP e protocolos de entrega de vídeo. O sistema permite que um streamer transmita vídeo via OBS Studio, que espectadores assistam via browser, e coleta métricas para comparar o desempenho de cada abordagem.

O objetivo central é comparar o desempenho de quatro combinações formadas por dois servidores de ingestão RTMP e dois transcodificadores, em uma matriz 2² — eixo Y para o servidor RTMP e eixo X para o transcodificador:

| | FFmpeg (Transcodificador A) | GStreamer (Transcodificador B) |
|---|---|---|
| **Nginx-RTMP (Servidor A)** | Combinação 1 | Combinação 2 |
| **SRS (Servidor B)** | Combinação 3 | Combinação 4 |

A análise foca em métricas de CPU, memória, latência e capacidade de espectadores simultâneos para cada uma das quatro combinações, permitindo isolar o impacto do servidor RTMP e do transcodificador separadamente. A plataforma não exige cadastro de usuários. Streamers criam uma transmissão via browser, recebem as credenciais RTMP e configuram o OBS. Espectadores acessam o link compartilhável para assistir.

---

## 2. Arquitetura

O sistema é composto por cinco camadas:

**Camada de ingestão RTMP**: recebe o stream do OBS Studio via protocolo RTMP. É nessa camada que residem as alternativas comparadas — Nginx-RTMP (Implementação A) e SRS (Implementação B).

**Camada de transcodificação**: converte o stream recebido em múltiplas qualidades de vídeo e gera segmentos HLS. Essa camada é o segundo eixo da matriz comparativa — o **FFmpeg** (Transcodificador A) e o **GStreamer** (Transcodificador B) são acionados de forma equivalente por ambos os servidores RTMP, mantendo os mesmos parâmetros de qualidade para garantir comparação justa.

**Camada de entrega**: serve os segmentos HLS ao browser via HTTP. Todas as quatro combinações utilizam o mesmo mecanismo de entrega HLS — segmentos gerados pelo transcodificador ativo são servidos via servidor HTTP Nginx.

**Camada de backend**: serviço Spring Boot responsável por gerenciar o ciclo de vida das streams, autenticar as transmissões via callbacks HTTP dos servidores RTMP, processar eventos assíncronos via RabbitMQ, e notificar clientes via SSE.

**Camada de frontend**: aplicação React que fornece interface para criar streams, assistir em tempo real e acompanhar métricas básicas.

Serviços de suporte: PostgreSQL para persistência, Redis para estado de viewers em tempo real, RabbitMQ para comunicação assíncrona entre módulos, Prometheus e Grafana para coleta e visualização de métricas.

---

## 3. Stack Tecnológico

### Backend
- Java 21
- Spring Boot 3.2
- Spring Data JPA
- Spring AMQP (RabbitMQ)
- Spring Boot Actuator com Micrometer e Prometheus
- PostgreSQL 16
- RabbitMQ 3 com management plugin
- Testcontainers para testes de integração
- SpringDoc OpenAPI (Swagger UI)

### Frontend
- React 18 com TypeScript
- Vite como bundler
- HLS.js para reprodução de streams HLS
- Plyr como player de vídeo
- TanStack Query para gerenciamento de estado assíncrono
- Axios para chamadas HTTP
- Tailwind CSS para estilização
- Radix UI para componentes de interface

### Infraestrutura RTMP
- Nginx compilado com o módulo nginx-rtmp-module — **Servidor A**
- SRS (Simple Realtime Server) versão 6 — **Servidor B**
- FFmpeg para transcodificação multi-qualidade — **Transcodificador A**
- GStreamer para transcodificação multi-qualidade — **Transcodificador B**

### Infraestrutura Geral
- Docker e Docker Compose para orquestração de containers
- Nginx como servidor HTTP para servir segmentos HLS
- Prometheus para coleta de métricas
- Grafana para dashboards comparativos

---

## 4. Backend

O backend é um monolito Spring Boot com dois módulos lógicos internos: `stream` e `consumer`. A organização interna segue os princípios de Clean Architecture, com separação entre core (entidades, casos de uso, repositórios) e infraestrutura (controllers, persistência JPA, mensageria, SSE).

### Módulo Stream

Responsável por toda a lógica de gerenciamento de streams. Expõe a API REST e o endpoint SSE. Valida transmissões via callbacks HTTP emitidos pelo servidor RTMP. Publica eventos de domínio no RabbitMQ através do `EventPublisher`. Gerencia o contador de viewers com operações atômicas diretamente no banco via JPQL `UPDATE`.

Casos de uso implementados:
- `CreateStreamUseCase` — cria a stream e gera a stream key (UUID)
- `GetStreamUseCase` — busca stream por ID
- `GetStreamStatusUseCase` — retorna status e contadores
- `ListLiveStreamsUseCase` — lista streams com status LIVE
- `ListUserStreamsUseCase` — lista streams de um owner
- `UpdateStreamUseCase` — atualiza título e descrição
- `DeleteStreamUseCase` — encerra a stream (status ENDED)
- `ValidateStreamKeyUseCase` — valida se uma stream key existe e está ativa

### Módulo Consumer

Consome eventos assíncronos do RabbitMQ e atualiza o estado do sistema. Os consumers processam eventos de ciclo de vida da stream e eventos de viewers, registrando logs e atualizando métricas.

Consumers implementados:
- `StreamEventConsumer` — processa `stream.created`, `stream.started`, `stream.ended`
- `ViewerEventConsumer` — processa `viewer.joined`, `viewer.left`
- `MetricsAggregationScheduler` — agendador periódico para agregação de métricas

### Componentes Transversais

- `SseEmitterManager` — gerencia emissores SSE por stream, incluindo keepalive e cleanup por TTL
- `StreamDomainEventListener` — escuta eventos de domínio Spring e os propaga via SSE e RabbitMQ
- `StreamCleanupScheduler` — remove streams inativas com status ENDED há mais de 1 dia
- `GlobalExceptionHandler` — trata exceções e retorna respostas padronizadas
- `EventPublisher` — publica eventos no RabbitMQ com serialização JSON

---

## 5. Frontend

A interface web é uma SPA (Single Page Application) construída com React e TypeScript. O roteamento é gerenciado pelo React Router. Chamadas HTTP ao backend usam Axios com TanStack Query para cache e revalidação automática.

### Páginas e funcionalidades

**Página inicial**: exibe o botão para iniciar uma nova transmissão e lista as streams ao vivo no momento.

**Criação de stream**: formulário com título e descrição obrigatórios. Ao confirmar, a API cria a stream e retorna a URL RTMP, a stream key e o link de espectador.

**Painel do streamer**: exibe a URL RTMP (`rtmp://localhost:1935/live`), a stream key, o status atual (WAITING / LIVE / ENDED), o contador de viewers em tempo real e o pico de espectadores. Permite encerrar a stream.

**Página de visualização**: reproduz o stream HLS via Plyr com HLS.js integrado, com suporte a Adaptive Bitrate Streaming (ABR — seleção automática de qualidade). Exibe título, status e contador de viewers. Atualiza automaticamente via SSE.

**Atualizações em tempo real**: todas as atualizações de status e contadores de viewers chegam ao frontend via SSE através do `EventSource` nativo do browser, conectado ao endpoint `GET /api/sse/stream/{streamId}/subscribe`.

---

## 6. Infraestrutura RTMP

### Servidor A — Nginx-RTMP

Container baseado em imagem customizada com Nginx compilado com `nginx-rtmp-module`. Recebe streams RTMP na porta 1935, application `live`.

**Callbacks HTTP configurados**:
- `on_publish` — dispara `POST /api/streams/callback/publish` para validar e iniciar a stream
- `on_publish_done` — dispara `POST /api/streams/callback/publish_done` para encerrar a stream

**Transcodificação**: ao receber uma nova publicação, executa o script de transcodificação configurado (`transcode-ffmpeg.sh` ou `transcode-gstreamer.sh`) passando o nome da stream. O transcodificador ativo é definido pela variável de ambiente `TRANSCODER=ffmpeg|gstreamer`.

**Entrega HLS**: o mesmo container serve os segmentos HLS via HTTP na porta 8081. Playlists `.m3u8` são servidas sem cache. Segmentos `.ts` são servidos com cache imutável.

**Segurança**: reprodução RTMP direta bloqueada para clientes externos. Os endpoints `/stat` e `/control` são restritos à rede interna Docker.

### Servidor B — SRS

Container baseado na imagem `ossrs/srs:6`. Recebe streams RTMP na porta 1935 e expõe API HTTP na porta 8080 e servidor HTTP para HLS na porta 8081.

**Callbacks HTTP configurados**: SRS notifica o backend com mais granularidade — `on_connect`, `on_close`, `on_publish`, `on_unpublish`, `on_play`, `on_stop`.

**Transcodificação**: o SRS usa seu mecanismo interno de `exec` para invocar o script de transcodificação configurado (`transcode-ffmpeg.sh` ou `transcode-gstreamer.sh`), gerando as mesmas 4 qualidades (1080p, 720p, 480p, 360p).

**Entrega HLS**: segmentos armazenados em `/var/srs-hls`, servidos pelo servidor HTTP interno do SRS com fragmentos de 6 segundos e janela de 60 segundos. O protocolo de entrega ao browser é idêntico ao da Implementação A — HLS via HTTP.

### Transcodificador A — FFmpeg

O FFmpeg gera 4 perfis de qualidade por stream transmitida:

| Qualidade | Resolução | Bitrate de vídeo | Preset |
|-----------|-----------|-----------------|--------|
| 1080p | 1920×1080 | 5000 kbps | medium |
| 720p | 1280×720 | 2800 kbps | medium |
| 480p | 854×480 | 1400 kbps | fast |
| 360p | 640×360 | 800 kbps | faster |

Codec de vídeo: H.264 (libx264). Codec de áudio: AAC, 128 kbps, 44100 Hz, estéreo. Duração de cada segmento HLS: 6 segundos. O resultado é uma master playlist `master.m3u8` referenciando as playlists de cada qualidade.

### Transcodificador B — GStreamer

O GStreamer é a alternativa ao FFmpeg na matriz comparativa. Gera os mesmos 4 perfis de qualidade para garantir comparação justa:

| Qualidade | Resolução | Bitrate de vídeo | Encoder |
|-----------|-----------|-----------------|--------|
| 1080p | 1920×1080 | 5000 kbps | x264enc |
| 720p | 1280×720 | 2800 kbps | x264enc |
| 480p | 854×480 | 1400 kbps | x264enc |
| 360p | 640×360 | 800 kbps | x264enc |

Codec de vídeo: H.264 via `x264enc`. Codec de áudio: AAC via `voaacenc`, 128 kbps, 44100 Hz, estéreo. Duração de cada segmento HLS: 6 segundos. O pipeline GStreamer utiliza `hlssink2` para geração dos segmentos e da master playlist. O script `transcode-gstreamer.sh` encapsula o pipeline e recebe os mesmos parâmetros que a versão FFmpeg.

---

## 7. Mensageria e Cache

### RabbitMQ

O sistema usa RabbitMQ como message broker para desacoplar a produção de eventos (módulo stream) do consumo (módulo consumer).

**Exchange**: `stream.exchange` do tipo topic.

**Filas e routing keys**:

| Fila | Routing keys vinculadas |
|------|------------------------|
| `stream.events` | `stream.created`, `stream.started`, `stream.ended` |
| `metrics.events` | `viewer.joined`, `viewer.left` |

Mensagens são serializadas em JSON via `Jackson2JsonMessageConverter`. A concorrência de consumers é configurada entre 3 e 10 threads.

### Redis

O Redis é utilizado para armazenamento de estado de sessão e operações atômicas sobre o contador de viewers. O contador `current_viewers` e o pico `viewers_peak` são atualizados via JPQL `UPDATE` atômico diretamente no PostgreSQL, com `clearAutomatically = true` para garantir consistência do contexto de persistência.

---

## 8. API REST

Base URL: `http://localhost:8080`

Documentação interativa disponível em `/swagger-ui.html`.

### Streams

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/streams` | Cria nova stream |
| GET | `/api/streams/live` | Lista streams ao vivo |
| GET | `/api/streams/my` | Lista streams do owner |
| GET | `/api/streams/{id}` | Retorna detalhes da stream |
| GET | `/api/streams/{id}/status` | Retorna status e contadores |
| PUT | `/api/streams/{id}` | Atualiza título e descrição |
| DELETE | `/api/streams/{id}` | Encerra a stream |
| POST | `/api/streams/{id}/restart` | Reinicia stream encerrada |
| POST | `/api/streams/validate` | Valida stream key |

### Callbacks RTMP

| Método | Endpoint | Disparado por |
|--------|----------|--------------|
| POST | `/api/streams/callback/publish` | on_publish do servidor RTMP |
| POST | `/api/streams/callback/publish_done` | on_publish_done / on_unpublish |

O callback `/publish` valida a stream key e transita o status para LIVE. O callback `/publish_done` transita o status para ENDED.

### Viewers

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/streams/{streamId}/join` | Registra entrada de espectador |
| POST | `/api/streams/{streamId}/leave` | Registra saída de espectador |

---

## 9. Eventos SSE

O servidor utiliza Server-Sent Events (SSE) para enviar atualizações em tempo real ao browser, eliminando a necessidade de polling.

**Endpoint de inscrição**: `GET /api/sse/stream/{streamId}/subscribe` (Content-Type: `text/event-stream`)

O `SseEmitterManager` gerencia todos os emissores ativos, agrupados por stream ID. Ao receber um evento de domínio (status alterado, viewer entrou/saiu), o `StreamDomainEventListener` propaga o evento para todos os emissores conectados àquela stream.

**Configurações SSE**:
- Timeout de conexão: 30 minutos
- Keepalive: a cada 30 segundos
- TTL de emissores inativos: 2 horas
- Cleanup periódico: a cada 5 minutos

---

## 10. Modelo de Dados

### Tabela `streams`

| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | UUID | Identificador único |
| title | VARCHAR(200) | Título da transmissão |
| description | VARCHAR(1000) | Descrição |
| stream_key | VARCHAR(16) | Chave de autenticação RTMP (unique) |
| owner_id | VARCHAR(36) | Identificador do criador |
| status | VARCHAR(20) | WAITING, LIVE ou ENDED |
| created_at | TIMESTAMP | Data de criação |
| updated_at | TIMESTAMP | Última atualização |
| started_at | TIMESTAMP | Início da transmissão |
| ended_at | TIMESTAMP | Encerramento da transmissão |
| current_viewers | INTEGER | Viewers conectados no momento |
| viewers_peak | INTEGER | Pico de viewers da sessão |

### Ciclo de vida da stream

A stream é criada com status `WAITING`. Quando o OBS inicia a transmissão, o callback do servidor RTMP muda o status para `LIVE`. Quando o OBS para de transmitir ou o streamer encerra manualmente, o status vai para `ENDED`. Streams com status ENDED há mais de 1 dia são removidas automaticamente pelo `StreamCleanupScheduler`.

### Eventos de domínio

O módulo stream publica eventos Spring que o `StreamDomainEventListener` captura e distribui:
- `StreamCreatedEvent` — publicado ao criar stream
- `StreamStartedEvent` — publicado quando status vai para LIVE
- `StreamEndedEvent` — publicado quando status vai para ENDED

---

## 11. Fluxos Principais

### Criação de stream

O frontend envia `POST /api/streams` com título e descrição. O backend gera uma stream key, persiste no PostgreSQL com status WAITING e publica `stream.created` no RabbitMQ. A resposta inclui a URL RTMP e a stream key para configuração no OBS.

### Publicação de stream

O OBS conecta ao servidor RTMP na porta 1935 com a stream key. O servidor RTMP dispara o callback `on_publish` para o backend, que valida a key e atualiza o status para LIVE. O backend publica `stream.started` no RabbitMQ e o `StreamDomainEventListener` notifica os clientes SSE. O servidor RTMP aciona o FFmpeg para iniciar a transcodificação HLS.

### Visualização

O browser acessa a página de visualização e conecta ao endpoint SSE para receber atualizações em tempo real. O player HLS (Plyr + HLS.js) carrega a master playlist gerada pelo FFmpeg e adapta automaticamente a qualidade (1080p → 360p) conforme as condições de rede do espectador.

### Encerramento

Quando o OBS para a transmissão, o callback `on_publish_done` é disparado. O backend atualiza o status para ENDED, publica `stream.ended` no RabbitMQ e o SSE notifica todos os browsers conectados.

---

## 12. Monitoramento

O Spring Boot Actuator expõe métricas via endpoint `/actuator/prometheus` no formato Prometheus. O Micrometer coleta métricas da JVM, do pool de conexões HikariCP, do RabbitMQ e métricas customizadas da aplicação.

O Prometheus faz scraping periódico dos endpoints de cada serviço. O Grafana consome os dados do Prometheus e exibe dashboards com métricas de CPU, memória, conexões ativas e indicadores específicos de streaming.

Os dashboards são estruturados para comparação lado a lado das implementações A e B, permitindo a análise quantitativa central do TCC — latência RTMP para reprodução no browser, uso de CPU e memória por container, e capacidade máxima de viewers simultâneos.

---

## 13. Testes

Os testes de integração usam Testcontainers para subir um container PostgreSQL real e validar os fluxos completos via HTTP com RestAssured.

- `StreamControllerE2ETest` — testa CRUD de streams, listagem de streams ao vivo, restart e validação de stream key (11 testes)
- `NginxCallbackControllerE2ETest` — testa os callbacks de publish e publish_done simulando o comportamento do servidor RTMP (7 testes)

A classe base `AbstractE2ETest` inicializa o container PostgreSQL compartilhado entre todas as suítes e configura o Spring para usar o banco de teste.

Total: 18 testes E2E passando.

---

## 14. Estrutura do Projeto

```
tcc/
├── app/
│   ├── backend/
│   │   ├── streaming-platform/         # Monolito Spring Boot
│   │   │   └── src/main/java/com/tcc/streaming/
│   │   │       ├── common/             # Config, eventos, handlers transversais
│   │   │       ├── stream/             # Módulo de gerenciamento de streams
│   │   │       └── consumer/           # Módulo de consumo de eventos
│   │   └── rtmp/
│   │       ├── nginx/                  # Implementação A: Nginx-RTMP
│   │       │   ├── configs/nginx.conf
│   │       │   └── scripts/transcode-ffmpeg.sh
│   │       └── srs/                    # Implementação B: SRS
│   │           └── srs.conf
│   ├── frontend/                       # React + TypeScript (Vite)
│   ├── config/
│   │   ├── nginx/
│   │   ├── prometheus/
│   │   └── grafana/
│   └── docker-compose.yml
└── docs/
    ├── documentacao.md
    ├── todo.md
    └── diagrams/
```

### Serviços Docker

| Serviço | Imagem | Portas expostas |
|---------|--------|----------------|
| postgres | postgres:16-alpine | 5432 |
| rabbitmq | rabbitmq:3-management-alpine | 5672, 15672 |
| streaming-platform | maven:3.9-eclipse-temurin-21 | 8080 |
| rtmp-server | build customizado (Nginx-RTMP ou SRS) | 1935 (RTMP), 8081 (HLS) |
| frontend | node:20-alpine | 3001 |

O serviço `rtmp-server` é intercambiável por duas variáveis de ambiente independentes: `RTMP_SERVER=nginx|srs` seleciona o servidor de ingestão e `TRANSCODER=ffmpeg|gstreamer` seleciona o transcodificador. As quatro combinações da matriz 2² são ativadas pela combinação dessas duas variáveis. O restante do sistema (backend, frontend, banco, filas) permanece idêntico em todas as configurações.

---

## 15. Referências

- [Nginx-RTMP Module](https://github.com/arut/nginx-rtmp-module)
- [SRS Documentation](https://github.com/ossrs/srs)
- [FFmpeg Documentation](https://ffmpeg.org/documentation.html)
- [HLS Specification — RFC 8216](https://tools.ietf.org/html/rfc8216)
- [Spring Boot Reference](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Spring AMQP Reference](https://docs.spring.io/spring-amqp/reference/)
- [RabbitMQ Documentation](https://www.rabbitmq.com/documentation.html)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [HLS.js Documentation](https://github.com/video-dev/hls.js/)
- [Testcontainers Documentation](https://testcontainers.com/guides/)

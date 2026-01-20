# Arquitetura do Sistema - Plataforma de Streaming

## Diagrama de Arquitetura Geral

```mermaid
architecture-beta
    group frontend(cloud)[Frontend]
    group backend(cloud)[Backend Services]
    group streaming(server)[Streaming Infrastructure]
    group data(database)[Data Layer]
    group monitoring(cloud)[Monitoring]

    service react[React App] in frontend
    service api[Stream Service] in backend
    service consumer[Consumer Service] in backend
    service metrics[Metrics Service] in backend
    
    service nginx_rtmp[Nginx RTMP] in streaming
    service ffmpeg[FFmpeg] in streaming
    service nginx_hls[Nginx HLS] in streaming
    
    service postgres[PostgreSQL] in data
    service redis[Redis Cache] in data
    service kafka[Kafka] in data
    
    service prometheus[Prometheus] in monitoring
    service grafana[Grafana] in monitoring

    react:R -- L:api
    api:R -- L:postgres
    api:R -- L:redis
    api:B -- T:kafka
    consumer:T -- B:kafka
    nginx_rtmp:R -- L:ffmpeg
    ffmpeg:R -- L:nginx_hls
    nginx_hls:T -- B:react
    metrics:L -- R:prometheus
    prometheus:R -- L:grafana
```

## Fluxo de Criação de Stream

```mermaid
sequenceDiagram
    actor User as Usuário
    participant Web as React Frontend
    participant API as Stream Service
    participant DB as PostgreSQL
    participant Cache as Redis
    participant Kafka as Message Broker

    User->>Web: Clica "Iniciar Streaming"
    Web->>API: POST /api/streams/create
    API->>API: Gera Stream Key único
    API->>DB: Salva dados da stream
    API->>Cache: Armazena sessão ativa
    API->>Kafka: Publica evento "stream_created"
    API-->>Web: Retorna URL RTMP + Stream Key + Link
    Web-->>User: Exibe painel com credenciais OBS
```

## Fluxo de Transmissão de Vídeo

```mermaid
sequenceDiagram
    actor Streamer as Streamer (OBS)
    participant RTMP as Nginx RTMP
    participant FFmpeg as FFmpeg
    participant HLS as Nginx HLS
    participant Kafka as Message Broker
    participant Consumer as Consumer Service
    actor Viewer as Espectador

    Streamer->>RTMP: Stream RTMP (1080p)
    RTMP->>RTMP: Valida Stream Key
    RTMP->>FFmpeg: Envia stream bruta
    FFmpeg->>FFmpeg: Transcodifica (1080p, 720p, 480p, 360p)
    FFmpeg->>HLS: Gera segmentos HLS (.ts) + playlist (.m3u8)
    RTMP->>Kafka: Evento "stream_started"
    Consumer->>Kafka: Consome evento
    Consumer->>Consumer: Atualiza métricas
    
    Viewer->>HLS: Requisita playlist.m3u8
    HLS-->>Viewer: Retorna playlist
    Viewer->>HLS: Requisita segmentos .ts
    HLS-->>Viewer: Retorna vídeo
```

## Fluxo de Visualização

```mermaid
sequenceDiagram
    actor Viewer as Espectador
    participant Web as React Frontend
    participant HLS as Nginx HLS
    participant WS as WebSocket Server
    participant API as Stream Service

    Viewer->>Web: Acessa link da stream
    Web->>API: GET /api/streams/:id/status
    API-->>Web: Status da stream (online/offline)
    Web->>HLS: Requisita playlist.m3u8
    HLS-->>Web: Retorna playlist HLS
    Web->>Web: Player carrega vídeo (HLS.js)
    Web->>WS: Conecta WebSocket
    WS-->>Web: Notifica "viewer_joined"
    WS-->>Web: Atualiza contagem de viewers
```

## Arquitetura de Componentes (C4 - Nível Container)

```mermaid
C4Container
    title Diagrama de Containers - Plataforma de Streaming

    Person(user, "Usuário", "Streamer ou Espectador")
    Person(obs, "OBS Studio", "Software de streaming")

    Container_Boundary(frontend, "Frontend") {
        Container(web, "React App", "React, TypeScript", "Interface web para streaming")
    }

    Container_Boundary(backend, "Backend Services") {
        Container(stream_service, "Stream Service", "Spring Boot", "API REST e WebSocket")
        Container(consumer_service, "Consumer Service", "Spring Boot", "Processa eventos")
        Container(metrics_service, "Metrics Service", "Spring Boot", "Coleta métricas")
    }

    Container_Boundary(streaming, "Streaming Infrastructure") {
        Container(nginx_rtmp, "Nginx RTMP", "Nginx + RTMP Module", "Recebe streams RTMP")
        Container(ffmpeg, "FFmpeg", "FFmpeg", "Transcodifica vídeo")
        Container(nginx_hls, "Nginx", "Nginx", "Serve HLS")
    }

    Container_Boundary(data, "Data Layer") {
        ContainerDb(postgres, "PostgreSQL", "PostgreSQL", "Dados persistentes")
        ContainerDb(redis, "Redis", "Redis", "Cache e sessões")
        ContainerQueue(kafka, "Kafka", "Apache Kafka", "Message broker")
    }

    Container_Boundary(monitoring, "Monitoring") {
        Container(prometheus, "Prometheus", "Prometheus", "Métricas")
        Container(grafana, "Grafana", "Grafana", "Dashboards")
    }

    Rel(user, web, "Acessa", "HTTPS")
    Rel(obs, nginx_rtmp, "Envia stream", "RTMP")
    Rel(web, stream_service, "Usa", "REST/WebSocket")
    Rel(web, nginx_hls, "Assiste", "HLS/HTTP")
    
    Rel(stream_service, postgres, "Lê/Escreve")
    Rel(stream_service, redis, "Cache")
    Rel(stream_service, kafka, "Publica eventos")
    
    Rel(nginx_rtmp, ffmpeg, "Stream bruto")
    Rel(ffmpeg, nginx_hls, "Segmentos HLS")
    
    Rel(consumer_service, kafka, "Consome eventos")
    Rel(metrics_service, prometheus, "Expõe métricas")
    Rel(prometheus, grafana, "Datasource")
```

## Estrutura de Pastas do Projeto

```mermaid
graph TD
    root[tcc/]
    
    root --> app[app/]
    root --> docker[docker/]
    root --> docs[docs/]
    root --> scripts[scripts/]
    root --> compose[docker-compose/]
    
    app --> backend[backend/]
    app --> frontend[frontend/]
    app --> config[config/]
    
    backend --> stream[stream-service/]
    backend --> consumer[consumer-service/]
    backend --> metrics[metrics-service/]
    backend --> common[common/]
    
    stream --> src_stream[src/]
    stream --> pom_stream[pom.xml]
    
    frontend --> web[web/]
    web --> src_web[src/]
    web --> public[public/]
    web --> package[package.json]
    
    config --> nginx_cfg[nginx/]
    config --> prom_cfg[prometheus/]
    config --> graf_cfg[grafana/]
    
    docker --> nginx_docker[nginx-rtmp/]
    docker --> kafka_docker[kafka/]
    docker --> rabbit_docker[rabbitmq/]
    docker --> redis_docker[redis/]
    docker --> postgres_docker[postgres/]
    docker --> monitor_docker[monitoring/]
    
    compose --> main_compose[docker-compose.yml]
    compose --> kafka_compose[docker-compose.kafka.yml]
    compose --> rabbit_compose[docker-compose.rabbitmq.yml]
    compose --> srs_compose[docker-compose.srs.yml]
```

## Alternativas Tecnológicas para Comparação

```mermaid
mindmap
  root((Plataforma<br/>Streaming))
    Ingestão RTMP
      Nginx-RTMP
        Principal
        Maduro e estável
      SRS
        Alternativa 1
        Mais moderno
      Node-Media-Server
        Alternativa 2
        Node.js based
    Transcodificação
      FFmpeg
        Principal
        Padrão indústria
      GStreamer
        Alternativa 1
        Framework modular
    Message Broker
      Kafka
        Principal
        Alta performance
      RabbitMQ
        Alternativa 1
        AMQP protocol
      Redis Streams
        Alternativa 2
        Simples setup
      NATS
        Alternativa 3
        Cloud native
    Cache
      Redis
        Principal
        In-memory
      Memcached
        Alternativa 1
        Simples
    Banco de Dados
      PostgreSQL
        Principal
        Relacional
      MySQL
        Alternativa 1
        Popular
      MongoDB
        Alternativa 2
        NoSQL
```

## Diagrama de Estados da Stream

```mermaid
stateDiagram-v2
    [*] --> Criada: Usuário cria stream
    Criada --> Aguardando: Stream key gerada
    Aguardando --> Transmitindo: OBS conecta e inicia
    Transmitindo --> Pausada: Streamer pausa
    Pausada --> Transmitindo: Streamer retoma
    Transmitindo --> Encerrada: Streamer para
    Aguardando --> Expirada: Timeout (30 min sem conexão)
    Encerrada --> [*]
    Expirada --> [*]
    
    Transmitindo --> Erro: Falha técnica
    Erro --> Transmitindo: Reconexão
    Erro --> Encerrada: Falha permanente
```

## Fluxo de Dados com Métricas

```mermaid
flowchart TB
    subgraph Input
        OBS[OBS Studio]
    end
    
    subgraph Ingestion
        RTMP[Nginx RTMP<br/>Coleta: bitrate, fps]
    end
    
    subgraph Processing
        FFmpeg[FFmpeg<br/>Coleta: CPU, tempo transcodificação]
    end
    
    subgraph Serving
        HLS[Nginx HLS<br/>Coleta: requests, bandwidth]
    end
    
    subgraph Events
        Kafka[Kafka<br/>Coleta: throughput, lag]
    end
    
    subgraph Backend
        API[Stream Service<br/>Coleta: response time, errors]
        Consumer[Consumer Service<br/>Coleta: eventos processados]
    end
    
    subgraph Monitoring
        Prom[Prometheus]
        Graf[Grafana]
        Exporter[Custom Exporters]
    end
    
    subgraph Output
        Player[Video Player]
    end
    
    OBS -->|RTMP Stream| RTMP
    RTMP -->|Raw Stream| FFmpeg
    FFmpeg -->|HLS Segments| HLS
    RTMP -->|Eventos| Kafka
    Kafka -->|Consume| Consumer
    Consumer --> API
    HLS -->|HTTP/HLS| Player
    
    RTMP -.->|Métricas| Exporter
    FFmpeg -.->|Métricas| Exporter
    HLS -.->|Métricas| Exporter
    Kafka -.->|Métricas| Prom
    API -.->|Métricas| Prom
    Consumer -.->|Métricas| Prom
    Exporter -.->|Métricas| Prom
    Prom -->|Datasource| Graf
```

## Pipeline de Deployment Local

```mermaid
flowchart LR
    subgraph Development
        Code[Código Fonte]
        Build[Build<br/>Maven/NPM]
    end
    
    subgraph Docker
        Images[Docker Images]
        Compose[Docker Compose]
    end
    
    subgraph Services
        Backend[Backend Services]
        Frontend[Frontend]
        Infra[Infrastructure]
    end
    
    subgraph Monitoring_Deploy
        Mon[Prometheus + Grafana]
    end
    
    Code --> Build
    Build --> Images
    Images --> Compose
    Compose --> Backend
    Compose --> Frontend
    Compose --> Infra
    Compose --> Mon
```

## Comparação de Performance - Template

```mermaid
quadrantChart
    title Comparação de Tecnologias: Performance vs Facilidade de Uso
    x-axis Baixa Facilidade --> Alta Facilidade
    y-axis Baixa Performance --> Alta Performance
    quadrant-1 Ideal
    quadrant-2 Especializado
    quadrant-3 Evitar
    quadrant-4 Simples
    Nginx-RTMP: [0.7, 0.8]
    SRS: [0.6, 0.9]
    Kafka: [0.5, 0.9]
    RabbitMQ: [0.7, 0.7]
    Redis Streams: [0.8, 0.6]
    FFmpeg: [0.5, 0.8]
    GStreamer: [0.4, 0.8]
```

## Cronograma do Projeto (Gantt)

```mermaid
gantt
    title Cronograma do Projeto TCC
    dateFormat YYYY-MM-DD
    section Setup
    Setup Inicial                :setup, 2026-01-20, 14d
    Estrutura Projeto           :struct, after setup, 7d
    
    section Backend
    Backend Base                :backend, after struct, 14d
    APIs REST                   :apis, after backend, 7d
    WebSocket                   :ws, after apis, 7d
    
    section Streaming
    Nginx RTMP                  :rtmp, after struct, 14d
    FFmpeg Config               :ffmpeg, after rtmp, 7d
    HLS Serving                 :hls, after ffmpeg, 7d
    
    section Frontend
    React Setup                 :react, after struct, 14d
    Interface Streamer          :ui1, after react, 7d
    Player Vídeo                :player, after ui1, 7d
    
    section Message Broker
    Kafka Setup                 :kafka, after ws, 14d
    Consumer Service            :consumer, after kafka, 7d
    
    section Monitoring
    Prometheus/Grafana          :mon, after consumer, 14d
    Custom Metrics              :metrics, after mon, 7d
    
    section Alternativas
    Implementar Alternativas    :alt, after metrics, 21d
    
    section Testes
    Testes Performance          :test, after alt, 14d
    Análise Resultados          :analysis, after test, 7d
    
    section Documentação
    Documentação TCC            :docs, after analysis, 21d
```

## Matriz de Decisão Tecnológica

```mermaid
%%{init: {'theme':'base'}}%%
graph TB
    subgraph Decision[Critérios de Decisão]
        P[Performance]
        E[Escalabilidade]
        F[Facilidade]
        C[Comunidade]
        D[Documentação]
    end
    
    subgraph RTMP[Servidor RTMP]
        N1[Nginx-RTMP: ⭐⭐⭐⭐]
        S1[SRS: ⭐⭐⭐⭐⭐]
        NM1[Node-Media: ⭐⭐⭐]
    end
    
    subgraph Broker[Message Broker]
        K1[Kafka: ⭐⭐⭐⭐⭐]
        R1[RabbitMQ: ⭐⭐⭐⭐]
        RS1[Redis Streams: ⭐⭐⭐]
    end
    
    Decision --> RTMP
    Decision --> Broker
```

---

**Nota**: Estes diagramas fornecem uma visão completa da arquitetura proposta. Podem ser ajustados conforme o desenvolvimento do projeto avança.

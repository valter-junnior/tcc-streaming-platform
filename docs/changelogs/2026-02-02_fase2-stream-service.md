# Changelog - Fase 2: Backend Stream Service Completo

**Data:** 2026-02-02  
**Versão:** 1.0.0-SNAPSHOT  
**Status:** ✅ Completo - Fase 2.1-2.6 Implementadas

## 🎯 Objetivo
Implementar o backend completo do Stream Service seguindo Clean Architecture, com todas as 3 camadas (core, application, infrastructure) e integrações completas com Redis, RabbitMQ e WebSocket.

## 📦 Arquivos Criados

### Stream Domain - Core Layer (13 arquivos)
```
stream/
├── core/
│   ├── entities/
│   │   ├── Stream.java               # Entidade de domínio com lógica de negócio
│   │   └── StreamStatus.java         # Enum: WAITING, LIVE, ENDED
│   ├── exceptions/
│   │   ├── StreamNotFoundException.java
│   │   └── InvalidStreamStateException.java
│   ├── dtos/stream/
│   │   ├── CreateStreamDto.java      # Record para criar streams
│   │   └── StreamDto.java            # Record para retornar streams
│   ├── repositories/
│   │   └── StreamRepository.java     # Interface do repositório
│   └── usecases/
│       ├── CreateStreamUseCase.java
│       ├── GetStreamUseCase.java
│       ├── DeleteStreamUseCase.java
│       └── ValidateStreamKeyUseCase.java
```

### Stream Domain - Application Layer (1 arquivo)
```
stream/
└── application/
    └── services/
        └── StreamService.java         # Implementa os 4 use cases + startStream/endStream
```

### Stream Domain - Infrastructure Layer (9 arquivos)
```
stream/
└── infrastructure/
    ├── persistence/
    │   ├── entities/
    │   │   └── StreamJpaEntity.java
    │   ├── repositories/
    │   │   ├── StreamJpaRepository.java      # Extends JpaRepository
    │   │   └── StreamRepositoryImpl.java     # Implementa StreamRepository
    │   └── mappers/
    │       └── StreamMapper.java             # Domain ↔ JPA
    ├── http/
    │   ├── controllers/
    │   │   ├── StreamController.java         # REST API
    │   │   └── NginxCallbackController.java  # Callbacks Nginx-RTMP
    │   ├── requests/
    │   │   ├── CreateStreamRequest.java      # @Valid, @NotBlank
    │   │   └── ValidateStreamKeyRequest.java
    │   └── presenters/
    │       └── StreamPresenter.java
    └── websocket/
        └── controllers/
            └── StreamWebSocketController.java # STOMP WebSocket
```

### Common Domain - Infrastructure Layer (6 arquivos)
```
common/
└── infrastructure/
    ├── config/
    │   ├── DatabaseConfig.java       # JPA config
    │   ├── RedisConfig.java          # Cache config
    │   ├── RabbitMQConfig.java       # Message broker config
    │   └── WebSocketConfig.java      # STOMP config
    ├── events/
    │   └── EventPublisher.java       # Publicador de eventos RabbitMQ
    └── http/
        └── handlers/
            └── GlobalExceptionHandler.java   # @RestControllerAdvice
```

## 🏗️ Clean Architecture Implementada

### Core Layer (Domínio)
- ✅ **Zero dependências** de frameworks (Java puro)
- ✅ **Entidade Stream** com lógica de negócio:
  - Factory method: `Stream.create()`
  - Regras: `start()`, `end()`, `incrementViewers()`, `decrementViewers()`
  - Validações de estado: WAITING → LIVE → ENDED
  - Geração de stream key (UUID 16 chars)
  - URLs: RTMP e Watch
- ✅ **Use cases** definem contratos (interfaces)
- ✅ **DTOs** para input/output
- ✅ **Exceções** de domínio

### Application Layer (Casos de Uso)
- ✅ **StreamService** implementa 4 use cases:
  - `CreateStreamUseCase`: criar stream, publicar evento, cachear
  - `GetStreamUseCase`: buscar por ID com cache (`@Cacheable`)
  - `DeleteStreamUseCase`: finalizar stream, publicar evento, limpar cache (`@CacheEvict`)
  - `ValidateStreamKeyUseCase`: validar key para Nginx-RTMP
- ✅ **Métodos adicionais**:
  - `startStream(streamKey)`: atualizar para LIVE, publicar evento
  - `endStream(streamKey)`: atualizar para ENDED, publicar evento
- ✅ `@Service` + `@Transactional`
- ✅ Depende **apenas** do core (sem Spring no core)
- ✅ Integração completa com EventPublisher (RabbitMQ)

### Infrastructure Layer (Adapters)
- ✅ **JPA**: Entidades separadas do domínio
  - `StreamJpaEntity` com anotações JPA
  - `StreamJpaRepository` extends `Jpa REST
  - `NginxCallbackController` com 3 callbacks Nginx-RTMP:
    - `GET /api/streams/callback/publish?name={key}` - Valida stream key (200 ou 403)
    - `GET /api/streams/callback/publish_done?name={key}` - Stream iniciada (LIVE)
    - `GET /api/streams/callback/done?name={key}` - Stream encerrada (ENDED)Repository`
  - `StreamRepositoryImpl` implementa interface do core
  - `StreamMapper` converte Domain ↔ JPA
- ✅ **HTTP**: Controllers REST
  - `StreamController` com 4 endpoints
  - Request DTOs com  handlers
  - Broadcast de viewers (`/topic/stream/{id}/viewers`)
  - Broadcast de status changes (`/topic/stream/{id}/status`)
  - Integração com EventPublisher para RabbitMQ erros (404, 400, 500)
- ✅ **WebSocket**: STOMP para real-time
  - Join/leave stream
  - Broadcast de viewers
  - Broadcast de status changes
- ✅ **Config**: Configurações Spring, `@Cacheable`, `@CacheEvict`
  - RabbitMQ com exchanges, queues, bindings
  - EventPublisher para publicar eventos (stream_created, stream_started, stream_ended, viewer_joined, viewer_left)
  - RabbitMQ com exchanges, queues, bindings
  - WebSocket com SockJS
  - JPA com entity scan

## 🔧 Configurações

### RabbitMQ (Exchange + Queues)
```
Exchange: stream.exchange (topic)
Queues: stream.events, metrics.events
Routing Keys:
  - stream.created
  - stream.started
  - stream.ended
  - viewer.joined
  - viewer.left
```

### Redis
- Cache manager configurado
- TTL padrão: 10 minutos
- Serialização: JSON

### WebSocket
- Endpoint: `/ws` com SockJS
- Topics: `/topic/stream/{id}/viewers`, `/topic/stream/{id}/status`
- App prefix: `/app`

## 📊 Endpoints REST

### POST `/api/streams`
Criar nova stream
- Request: `{ "title": "...", "description": "..." }`
- Cache: Limpa cache após criação
- Evento: `stream_created` → RabbitMQ

### GET `/api/streams/{id}`
Buscar stream por ID
- Response: StreamPresenter (200)
- Cache: `@Cacheable` (10min TTL)
- Error: 404 se não encontrado

### DELETE `/api/streams/{id}`
Deletar/finalizar stream
- Response: 204 No Content
- Cache: `@CacheEvict` limpa cache
- Evento: `stream_ended` → RabbitMQ
- Error: 404 se não encontrado

### POST `/api/streams/validate`
Validar stream key (para Nginx-RTMP)
- Request: `{ "streamKey": "..." }`
### Primeira Iteração
1. **Conflito de métodos execute()**: Renomeado `DeleteStreamUseCase.execute()` para `deleteStream()` para evitar conflito com `GetStreamUseCase`
2. **Construtor da entidade Stream**: Adicionado construtor completo para permitir mapeamento do JPA
3. **Tipos de data**: Unificado para `LocalDateTime` (era `Instant` em alguns lugares)
4. **Método delete**: Renomeado para `deleteById` no repositório para corresponder ao JPA

### Segunda Iteração (Finalização Fase 2.1)
5. **EventPublisher criado**: Componente para publicar eventos no RabbitMQ
6. **NginxCallbackController criado**: 3 endpoints de callback para Nginx-RTMP
7. **StreamService integrado**:
   - Adicionado `@Cacheable` em GET (buscar stream)
[INFO] Compiling 31 source files
```

### Arquivos criados
- 30 arquivos Java (27 iniciais + 3 adicionais)
- Fase 2.1-2.6 completas:
  - ✅ 2.1: Projeto Spring Boot configurado
  - ✅ 2.2: Modelagem de dados (Stream entity)
  - ✅ 2.3: API REST (4 endpoints + 3 callbacks Nginx)
  - ✅ 2.4: Redis configurado com cache
  - ✅ 2.5: RabbitMQ configurado com EventPublisher
  - ✅ 2.6: WebSocket configurado com STOMPegrado**: EventPublisher adicionado ao WebSocketController
9. **Erro de sintaxe corrigido**: Texto duplicado removido do StreamService
#### GET `/api/streams/callback/publish?name={streamKey}`
Callback quando streamer tenta publicar
- Response: 200 (aceita) ou 403 (rejeita)
- Valida stream key no banco/cache

#### GET `/api/streams/callback/publish_done?name={streamKey}`
Callback quando stream começa a transmitir
- Response: 200
- Atualiza status para LIVE
- Evento: `stream_started` → RabbitMQ

#### GET `/api/streams/callback/done?name={streamKey}`
Callback quando stream termina
- Response: 200
- Atualiza status para ENDED
- Evento: `stream_ended` → RabbitMQ"..." }`
- Response: `boolean` (200)

## 🐛 Correções Realizadas
tainer**: Docker container com callbacks configurados
2. **FFmpeg Transcodificação**: Scripts de transcodificação HLS multi-bitrate
3. **Nginx HLS Server**: Servidor para servir segmentos HLS
4. **Frontend Web**: React app para criar/visualizar streams
5. **Event Consumers**: Consumer domain para processar eventos do RabbitMQ
6. **Metrics Collector**: Coletar métricas de streams e viewers
7. **Stream Cleanup**: Scheduled job para limpar streams expiradas

## 📝 Observações

- ✅ Clean Architecture rigorosamente seguida
- ✅ Core layer tem **zero** dependências de frameworks
- ✅ Infrastructure → Application → Core (nunca o contrário)
- ✅ Mapeadores separam domínio de persistência
- ✅ Cache implementado com Spring Cache (`@Cacheable`, `@CacheEvict`)
- ✅ Eventos publicados no RabbitMQ para todas operações
- ✅ Callbacks Nginx-RTMP implementados para controle de streaming
- ✅ WebSocket integrado para atualizações real-time
- ✅ Nenhum arquivo .md extra criado (apenas changelog)
- ✅ Todo.md atualizado com progresso detalhado
- 27 arquivos Java
- 0 arquivos .md extras (seguindo utils.md)

## 🔜 Próximos Passos (Fase 3)

1. **Nginx-RTMP Configuration**: Configurar callbacks para validação de stream key
2. **Frontend Web**: React app para criar/visualizar streams
3. **Event Sourcing**: Implementar publicação de eventos no RabbitMQ
4. **Cache Layer**: Implementar cache de streams no Redis
5. **Metrics Collector**: Consumer domain para coletar métricas
6. **Stream Cleanup**: Scheduled job para limpar streams expiradas

## 📝 Observações

- Seguindo Clean Architecture rigorosamente
- Core layer tem **zero** dependências de frameworks
- Infrastructure → Application → Core (nunca o contrário)
- Mapeadores separam domínio de persistência
- TODOs marcados no código para integrações futuras (RabbitMQ, Redis)
- Nenhum arquivo .md extra criado (apenas changelog)

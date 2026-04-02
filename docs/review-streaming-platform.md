# Code Review — `app/backend/streaming-platform`

Data: 2026-04-02

---

## 1. Código Morto e Arquivos/Pastas Sem Uso

### 1.1 Métodos `startStream()` e `endStream()` em `StreamService` nunca são chamados
**Arquivo:** `stream/application/services/StreamService.java`

`startStream(String streamKey)` e `endStream(String streamKey)` existem no serviço mas nunca são invocados. O `RtmpCallbackController` usa `validateAndStartStream()` e `validateAndEndStream()` (métodos atômicos criados depois). Esses dois métodos são código morto.

```java
// Nunca chamados — remover
public UUID startStream(String streamKey) { ... }
public UUID endStream(String streamKey) { ... }
```

### 1.2 Módulo `metrics/` completamente vazio
**Caminho:** `com/tcc/streaming/metrics/`

Todos os subpacotes (`core/`, `infrastructure/`, `application/`) contêm apenas arquivos `.gitkeep`. Não há nenhuma classe implementada. Deve ser removido ou marcado claramente como work-in-progress com um `README.md`.

### 1.3 Pasta `stream/infrastructure/webrtc/` vazia
**Caminho:** `com/tcc/streaming/stream/infrastructure/webrtc/`

Diretório vazio sem `.gitkeep` nem código. Deve ser removido.

### 1.4 Pasta `stream/infrastructure/sse/services/` vazia
**Caminho:** `com/tcc/streaming/stream/infrastructure/sse/services/`

Pasta vazia sem uso. Deve ser removida.

### 1.5 `MetricsAggregationScheduler` — scheduler com corpo inteiramente TODO
**Arquivo:** `consumer/infrastructure/scheduler/MetricsAggregationScheduler.java`

Ambos os métodos `aggregateDailyMetrics()` e `cleanupOldEvents()` consistem apenas de comentários TODO. O scheduler está registrado e dispara às 04h/05h, mas não faz nada. É código enganoso — um scheduler que parece funcional mas é um no-op.

---

## 2. Falhas e Erros de Implementação

### 2.1 `StreamController` mistura interfaces (use cases) com injeção direta de `StreamService`
**Arquivo:** `stream/infrastructure/http/controllers/StreamController.java`

O controller recebe todas as interfaces de use cases no construtor **e também** `StreamService` diretamente:

```java
public StreamController(
    CreateStreamUseCase createStreamUseCase,
    ...
    StreamService streamService  // ← injeção de implementação concreta
)
```

Isso viola o Dependency Inversion Principle. `restartStream()`, `forceEndAllLiveStreams()` e outros métodos são acessados via `streamService` sem passar por interface. O correto é criar interfaces para esses métodos e injetar somente via interface.

### 2.2 `ViewerController` captura todas as exceções com `try/catch(Exception)` e retorna 500 genérico
**Arquivo:** `stream/infrastructure/http/controllers/ViewerController.java`

```java
} catch (Exception e) {
    log.error("[Viewer] Error joining stream: {}", e.getMessage(), e);
    return ResponseEntity.internalServerError().build();
}
```

Isso impede que o `GlobalExceptionHandler` processe exceções de domínio (`StreamNotFoundException`, `UnauthorizedException`, etc.) com seus respectivos HTTP status codes. O resultado correto seria deixar as exceções propagarem para o handler global.

### 2.3 `SseEmitterManager.removeAllEmittersForStream()` não executa o callback `onDisconnect`
**Arquivo:** `stream/infrastructure/sse/SseEmitterManager.java`

Quando a stream termina e `removeAllEmittersForStream()` é chamado, os emitters são completados mas o callback `onDisconnect` (que decrementa viewers) **não é disparado**. O viewer count fica incorreto até o próximo `decrementViewers` natural.

```java
public void removeAllEmittersForStream(UUID streamId) {
    // ...
    emitters.forEach(ve -> {
        viewerEmitters.remove(ve.viewerId());
        try {
            ve.emitter().complete(); // ← onCompletion/safeDisconnect NÃO é garantido aqui
```

`SseEmitter.complete()` envia o sinal de fim mas não garante que o callback `onCompletion` seja invocado de forma síncrona antes do `viewerEmitters.remove()`. O `safeDisconnect` verifica `viewerEmitters.get(viewerId) == viewerEmitter`, e como o `viewerEmitters.remove()` ocorre antes, o callback é suprimido → nenhum `decrementViewers` é chamado.

**Correção:** chamar `onDisconnect.run()` explicitamente antes de remover do map quando for uma remoção forçada.

### 2.4 `keepaliveScheduler` de `SseEmitterManager` nunca é encerrado (sem `@PreDestroy`)
**Arquivo:** `stream/infrastructure/sse/SseEmitterManager.java`

```java
private final ScheduledExecutorService keepaliveScheduler = Executors.newScheduledThreadPool(1);
```

O executor não tem shutdown ao contexto Spring ser encerrado. Em restarts/shutdowns, as threads ficam penduradas. Solução: adicionar `@PreDestroy`.

```java
@PreDestroy
public void shutdown() {
    keepaliveScheduler.shutdownNow();
}
```

### 2.5 `CORS`: `allowCredentials(true)` com `allowedOriginPatterns("*")`
**Arquivo:** `common/infrastructure/config/WebConfig.java`

```java
registry.addMapping("/**")
    .allowedOriginPatterns("*")
    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
    .allowedHeaders("*")
    .allowCredentials(true)  // ← problemático
```

Combinação de `allowCredentials(true)` + `allowedOriginPatterns("*")` funciona no Spring (ele espelha a origem real), mas **semanticamente está errado**. A API não usa autenticação por cookie nem credenciais no CORS. Remove `allowCredentials(true)` — a API usa apenas o token no header `Authorization`.

### 2.6 `cleanupInactiveStreams()` publica evento mesmo quando `save()` pode ter falhado
**Arquivo:** `stream/application/services/StreamService.java`

Dentro do loop:
```java
stream.forceEnd();
streamRepository.save(stream);                  // se lançar exceção...
applicationEventPublisher.publishEvent(event); // ... nunca chega aqui
cleanedCount++;                                // ... nem aqui
```

Embora estruturalmente o `publishEvent` só é chamado após o `save`, o `cleanedCount++` só incrementa se nenhum deles lançar. Porém o catch engole a exceção silenciosamente. O problema real é que `publishEvent()` é chamado **dentro da mesma transação** mas o listener usa `@TransactionalEventListener(phase = AFTER_COMMIT)` — como dentro de um `@Transactional` marcado no método, os eventos serão disparados após o commit. Isso é correto. **Mas** há um risco: se o loop falhar na metade, streams já salvas terão seus eventos publicados, as restantes não — inconsistência parcial.

### 2.7 `StreamEventProcessorService.mapEventType()` lança `IllegalArgumentException` para tipo desconhecido
**Arquivo:** `consumer/application/services/StreamEventProcessorService.java`

```java
default -> throw new IllegalArgumentException("Unknown event type: " + eventType);
```

Isso é relançado pelo listener (`throw e`), causando `nack` no RabbitMQ e requeue da mensagem. Se uma mensagem com tipo desconhecido chegar, ela fica em requeue infinito (poison pill). Deveria usar uma DLQ ou logar e ignorar.

### 2.8 `spring.jpa.hibernate.ddl-auto: update` 
**Arquivo:** `src/main/resources/application.yml`

`ddl-auto: update` em ambiente compartilhado/produção é arriscado — pode executar alterações de schema destrutivas sem rastreamento. Deveria usar `validate` com Flyway ou Liquibase para migrations controladas.

---

## 3. Melhorias de Rastreamento (Logs)

### 3.1 `StreamController` ausência de log nos endpoints `updateStream` e `deleteStream`
**Arquivo:** `stream/infrastructure/http/controllers/StreamController.java`

`updateStream()` e `deleteStream()` não logam o início nem o sucesso/erro da operação, ao contrário de `createStream` que loga corretamente.

### 3.2 `broadcastStreamStarted/Ended` não logam streamId antes de chamar `emitterManager.broadcastToStream()`
**Arquivo:** `stream/infrastructure/sse/controllers/StreamSseController.java`

Os métodos `broadcastStreamStarted` e `broadcastStreamEnded` logam a intenção mas, se `broadcastToStream` falhar silenciosamente (sem emitters), o log de "broadcasting to 0 viewers" aparece no WARN do `emitterManager` — fácil de perder. Um log `INFO` de resultado após o broadcast ajudaria.

### 3.3 `StreamEventConsumer.handleStreamEvent()` realiza `processStreamEventUseCase.execute()` e depois lida com `switch` de tipo — dupla passagem pelo evento
**Arquivo:** `consumer/infrastructure/messaging/StreamEventConsumer.java`

O evento é processado (e persistido) primeiro, depois o `switch` executa handlers que apenas logam. Logicamente correto, mas o `switch` poderia ser eliminado — o processamento já loga via `StreamEventProcessorService`. Os métodos privados `handleStreamCreated`, `handleStreamStarted`, `handleStreamEnded` são basicamente wrappers de log desnecessários.

---

## 4. Outros Pontos de Atenção (menores)

| # | Local | Ponto |
|---|-------|-------|
| 4.1 | `Stream.java` | Bloco `if (this.status != StreamStatus.WAITING)` com `throw` está **comentado** no método `start()`. O método `start()` aceitará qualquer estado de entrada silenciosamente. Remover ou descomentar com cautela. |
| 4.2 | `StreamService.update()` | Chama `stream.setUpdatedAt(LocalDateTime.now())` diretamente no campo da entidade de domínio em vez de ter um método de negócio `updateInfo(title, description)` na entidade. Quebra o encapsulamento do domínio. |
| 4.3 | `StreamJpaEntity` | Não tem `@Version` para otimistic locking — apesar de o `GlobalExceptionHandler` tratar `ObjectOptimisticLockingFailureException`, nunca será disparado sem `@Version`. |
| 4.4 | `RtmpCallbackController.resolveStreamKey()` | A leitura do body via `request.getReader()` consome o `InputStream`. Qualquer filtro Spring que precise do body depois (ex: logging, audit) não conseguirá lê-lo. Usar `ContentCachingRequestWrapper` se necessário. |
| 4.5 | `RabbitMQConfig` | `@ConditionalOnProperty(name = "rabbitmq.enabled")` — mas o binding no `application.yml` usa `spring.rabbitmq.host`, não `rabbitmq.enabled`. A condição nunca é satisfeita via app config; só funciona se `rabbitmq.enabled=true` for explicitamente setado. |
| 4.6 | `StreamController` | Endpoint `POST /validate` está documentado com `@ApiResponse(responseCode = "400", description = "Stream key inválida")` mas o endpoint só retorna `200 true`/`200 false`. Nunca retorna 400. Documentação errada. |

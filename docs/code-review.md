# Code Review — Backend (18/03/2026)

> Escopo: `app/backend/` (streaming-platform Spring Boot + nginx-RTMP + scripts shell + docker-compose)


### 1.4 `streaming-platform` no Docker Compose roda via `mvn spring-boot:run`

**Arquivo:** `docker-compose.yml`

```yaml
command: >
  bash -c "mvn spring-boot:run -Dspring-boot.run.jvmArguments='...'"
```

O serviço usa a imagem Maven genérica e compila + sobe via `mvn spring-boot:run` a cada `docker compose up`. Isso tem vários problemas críticos:

- **Sem healthcheck**: o serviço não tem `healthcheck` definido, portanto o `rtmp-server` e o `frontend` que dependem dele via `depends_on: streaming-platform` sobem imediatamente sem esperar a JVM estar pronta (o que leva muitos minutos compilando). O script `docker-entrypoint.sh` do nginx contorna isso com um loop de polling manual — a "gambira" citada.
- **Sem `condition: service_healthy`** no `depends_on` de `rtmp-server` e `frontend`, ao contrário do postgres e rabbitmq que estão corretos.
- **Volumes do Maven local** (`~/.m2`) montados dentro do container: em ambientes CI ou máquinas sem cache local, a primeira inicialização pode baixar centenas de MB de dependências antes de servir qualquer request.

**Correção:** criar um `Dockerfile` para o `streaming-platform` que faça o build em etapa separada (multi-stage build) e rode o JAR final. Adicionar healthcheck (`/actuator/health`) e `condition: service_healthy` nos dependentes.

---

### 1.7 `NginxCallbackController` chama `sseController.broadcastStreamStarted` dentro da requisição HTTP síncrona

**Arquivo:** `NginxCallbackController.java`

```java
public ResponseEntity<Void> onPublish(...) {
    UUID streamId = streamService.validateAndStartStream(name); // transação DB
    sseController.broadcastStreamStarted(streamId);             // loop SSE + I/O
    return ResponseEntity.ok().build();
}
```

O broadcast SSE ocorre **dentro da thread que serve a requisição do nginx**. Se houver muitos viewers, o loop de envio bloqueia a resposta para o nginx, que tem timeout curto no `on_publish`. Se o timeout estourar, o nginx pode rejeitar a publicação ou retentar, potencialmente duplicando eventos.

**Correção:** executar o broadcast de forma assíncrona (`@Async` ou `CompletableFuture.runAsync(...)`), retornando o `200 OK` imediatamente para o nginx.

---

## 2. Melhorias de Performance

### 2.1 Latência de início de live — `STREAM_STABILIZE_SECONDS` desnecessariamente alto

**Arquivo:** `transcode-ffmpeg.sh`

```bash
STREAM_STABILIZE_SECONDS="${TRANSCODER_STREAM_STABILIZE_SECONDS:-8}"
...
sleep "$STREAM_STABILIZE_SECONDS"
```

O script dorme **8 segundos fixos** antes de tentar conectar o FFmpeg ao RTMP. Esse delay existe para aguardar o stream estar estável, mas na prática o FFmpeg com `-rw_timeout 10000000` já lida com reconexão. Combinado com o `HLS_SEGMENT_DURATION=6`, um viewer pode esperar entre **14–20 segundos** desde o início da transmissão até o primeiro segmento disponível.

**Melhoria:** reduzir `STREAM_STABILIZE_SECONDS` para 2–3 s (ou remover e deixar o próprio FFmpeg tentar com retries). Reduzir `HLS_SEGMENT_DURATION` para 2–4 s para diminuir latência de reprodução.

---

### 2.2 Startup do `streaming-platform` atrasa toda a stack

**Arquivo:** `docker-compose.yml`

Compilar via Maven a cada `docker compose up` leva em média 1–3 minutos antes da API estar disponível. Durante esse tempo o `rtmp-server` fica em loop de polling no `wait_for_backend()`, e o `frontend` sobe mas não consegue se comunicar com a API.

**Melhoria imediata:** adicionar um `Dockerfile` multi-stage para o backend:

```dockerfile
# Stage 1 — build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q   # cache de deps em layer separada
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2 — runtime
FROM eclipse-temurin:21-jre-alpine
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Com esse approach o tempo de inicialização cai de ~2 min para ~10 s (apenas `java -jar`), e o cache de layers do Docker evita redownload de dependencies entre builds.

---

### 2.3 Healthcheck ausente no `streaming-platform` quebra a ordem de inicialização

**Arquivo:** `docker-compose.yml`

O `rtmp-server` declara `depends_on: streaming-platform` sem `condition: service_healthy`. Como `streaming-platform` não tem healthcheck, o Docker considera o serviço "pronto" imediatamente após iniciar o processo — muito antes da JVM e Spring estarem up. Isso força o script `docker-entrypoint.sh` do nginx a ter um loop de polling manual que pode durar até 120 s.

**Melhoria:** adicionar healthcheck ao `streaming-platform`:

```yaml
streaming-platform:
  healthcheck:
    test: ["CMD", "wget", "-q", "-O", "/dev/null", "http://localhost:8080/actuator/health"]
    interval: 15s
    timeout: 5s
    retries: 10
    start_period: 60s

rtmp-server:
  depends_on:
    streaming-platform:
      condition: service_healthy   # ← adicionar
```

Eliminaria toda a lógica de `wait_for_backend()` do entrypoint.

### 2.5 `incrementViewers` faz 2 queries separadas (update + select)

**Arquivo:** `StreamService.java`

```java
public StreamDto incrementViewers(UUID streamId) {
    int updated = streamRepository.incrementViewersAtomic(streamId); // UPDATE
    Stream stream = streamRepository.findById(streamId)             // SELECT
        .orElseThrow(...);
    return toDto(stream);
}
```

A query atômica de incremento não retorna o estado atualizado, forçando um segundo `SELECT`. Com muitos viewers entrando simultaneamente, isso gera o dobro de queries desnecessárias.

**Melhoria:** usar uma query JPQL que retorne o estado após update, ou ao menos usar uma projeção/DTO diretamente da query de incremento para evitar recarregar a entidade inteira.

---

### 2.6 `SseEmitterManager` usa `ScheduledExecutorService` com pool fixo de 1 thread para keepalive + TTL

**Arquivo:** `SseEmitterManager.java`

```java
private final ScheduledExecutorService keepaliveScheduler = Executors.newScheduledThreadPool(1);
```

Uma única thread gerencia tanto o keepalive quanto o TTL cleanup. Se o envio de keepalive para muitos viewers demorar mais que o intervalo agendado, as tarefas acumulam. Para um número baixo de viewers (TCC) isso é aceitável, mas a thread não tem nome definido, dificultando debugging e profiling.

**Melhoria:** usar `Executors.newScheduledThreadPool(2, r -> new Thread(r, "sse-scheduler-%d"))` para separar as tarefas e facilitar identificação em thread dumps.

---

### 2.7 Nginx compila a partir do fonte a cada build de imagem

**Arquivo:** `rtmp/nginx/Dockerfile`

```dockerfile
RUN wget https://nginx.org/download/nginx-1.24.0.tar.gz && \
    ...
    make -j"$(nproc)" && make install
```

Compilar nginx + módulo RTMP a cada `docker build` sem cache externo demora 2–5 minutos dependendo do hardware. Além disso, usa `ubuntu:22.04` como base — uma imagem pesada (~77 MB) para rodar nginx.

**Melhoria:** usar uma imagem base Alpine com nginx pré-compilado disponível via `alfg/nginx-rtmp` (imagem pública com nginx-rtmp já compilado), ou ao menos garantir que o `RUN` de compilação tem o máximo de aproveitamento de cache Docker (colocar `COPY configs/` depois da compilação, não antes).

---

### 2.8 `cleanupInactiveStreams` faz N saves dentro de um loop na mesma transação

**Arquivo:** `StreamService.java`

```java
for (Stream stream : inactiveStreams) {
    stream.forceEnd();
    streamRepository.save(stream);          // flush individual
    applicationEventPublisher.publishEvent(...);
}
```

Cada `save()` dentro do loop gera um `UPDATE` individual. Com muitas streams inativas, isso cria N round-trips ao banco.

**Melhoria:** usar uma query `@Modifying @Query("UPDATE ... SET status = 'ENDED' WHERE ...")` em bulk para os status, e publicar os eventos após o loop.
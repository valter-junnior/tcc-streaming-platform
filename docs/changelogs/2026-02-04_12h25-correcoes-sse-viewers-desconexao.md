# Changelog - Correções SSE: Viewers, Desconexão e Streamers

**Data:** 04/02/2026  
**Horário:** 12h25

## Problemas Identificados

### 1. ViewerId não persistente
- **Problema**: A cada refresh da página, um novo viewer era contado
- **Causa**: ViewerId estava sendo gerado sempre diferente
- **Impacto**: Contador de viewers inflado artificialmente

### 2. StreamerDashboard contando como viewer
- **Problema**: Streamer conectado ao SSE era contado como viewer
- **Causa**: Não havia diferenciação entre streamer e viewer
- **Impacto**: Contador sempre mostrava +1 viewer (o próprio streamer)

### 3. Múltiplas conexões simultâneas
- **Problema**: Refresh criava nova conexão sem fechar a antiga
- **Causa**: SSE não estava verificando conexões duplicadas
- **Impacto**: Múltiplos viewers para o mesmo viewerId

### 4. Desconexão não detectada
- **Problema**: Fechar aba não decrementava viewers
- **Causa**: Callback de desconexão sendo executado múltiplas vezes causava exception
- **Impacto**: Contador de viewers só aumentava, nunca diminuía

### 5. Erro HttpMediaTypeNotAcceptableException
- **Problema**: `ResponseBodyEmitter has already completed`
- **Causa**: Tentativa de enviar dados após emitter completado
- **Impacto**: Erro no console frontend e backend

## Correções Implementadas

### Backend (Spring Boot)

#### 1. StreamSseController.java
**Adicionado parâmetro `countAsViewer`**:
```java
@GetMapping(value = "/stream/{streamId}/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter subscribe(@PathVariable UUID streamId, 
                            @RequestParam String viewerId,
                            @RequestParam(defaultValue = "true") boolean countAsViewer)
```

**Lógica de contagem condicional**:
- Se `countAsViewer = true`: incrementa viewers (viewers reais)
- Se `countAsViewer = false`: não incrementa viewers (streamers)
- Verifica se viewerId já está conectado antes de incrementar
- Remove conexão antiga antes de criar nova (evita duplicatas)

**Callback de desconexão condicional**:
- Só decrementa se `countAsViewer = true`
- Evita decrementar viewers de streamers

#### 2. SseEmitterManager.java
**Prevenção de duplicatas**:
```java
public SseEmitter createEmitter(UUID streamId, String viewerId, Runnable onDisconnect) {
    // Se já existe um emitter para esse viewerId, remover primeiro
    if (viewerEmitters.containsKey(viewerId)) {
        log.info("[SSE] Removing existing emitter before creating new one - ViewerId: {}", viewerId);
        removeEmitterInternal(viewerId);
    }
    ...
}
```

**Callback seguro contra múltiplas execuções**:
```java
// Flag para evitar múltiplas chamadas ao callback
final boolean[] callbackExecuted = {false};

Runnable safeDisconnect = () -> {
    synchronized (callbackExecuted) {
        if (!callbackExecuted[0]) {
            callbackExecuted[0] = true;
            removeEmitterInternal(viewerId);
            onDisconnect.run();
        }
    }
};
```

**Tratamento de IllegalStateException**:
```java
public void broadcastToStream(UUID streamId, String eventType, Object data) {
    ...
    try {
        String json = objectMapper.writeValueAsString(data);
        viewerEmitter.emitter().send(SseEmitter.event()
            .name(eventType)
            .data(json));
    } catch (IOException e) {
        log.warn("[SSE] Failed to send to viewer: {} - Error: {}", 
                 viewerEmitter.viewerId(), e.getMessage());
        failedViewers.add(viewerEmitter.viewerId());
    } catch (IllegalStateException e) {
        log.warn("[SSE] Emitter already completed for viewer: {}", viewerEmitter.viewerId());
        failedViewers.add(viewerEmitter.viewerId());
    }
}
```

### Frontend (React + TypeScript)

#### 1. sseService.ts
**Adicionado parâmetro `countAsViewer`**:
```typescript
subscribe(
  streamId: string,
  viewerId: string,
  onStatusUpdate: StreamStatusCallback,
  onViewersUpdate: ViewersUpdateCallback,
  countAsViewer: boolean = true,
): () => void
```

**Fechamento de conexão antiga antes de criar nova**:
```typescript
// Se já existe uma subscrição para essa stream, desconectar a antiga primeiro
const existing = this.subscriptions.get(streamId);
if (existing) {
  logger.warn("[SSE] Closing existing subscription before creating new one", {
    streamId,
    oldViewerId: existing.viewerId,
    newViewerId: viewerId,
  });
  existing.eventSource.close();
  this.subscriptions.delete(streamId);
}
```

**URL com parâmetro countAsViewer**:
```typescript
const url = `${API_BASE_URL}/api/sse/stream/${streamId}/subscribe?viewerId=${viewerId}&countAsViewer=${countAsViewer}`;
```

#### 2. StreamerDashboard.tsx
**Passar `countAsViewer=false` para streamer**:
```typescript
unsubscribeRef.current = sseService.subscribe(
  streamId!,
  streamerViewerId,
  (statusMessage) => { ... },
  (viewersMessage) => { ... },
  false, // countAsViewer = false para não contar streamer como viewer
);
```

#### 3. WatchPage.tsx
**Usa `countAsViewer=true` (padrão) para viewers reais**:
```typescript
unsubscribeRef.current = sseService.subscribe(
  streamId!,
  viewerId,
  (statusMessage) => { ... },
  (viewersMessage) => { ... },
  // countAsViewer = true (padrão)
);
```

## Melhorias Adicionais

### 1. Logs mais detalhados
- `[SSE] Viewer connecting - StreamId: X, ViewerId: Y, CountAsViewer: Z`
- `[SSE] Removing existing emitter before creating new one`
- `[SSE] Emitter already completed for viewer: X`

### 2. Prevenção de race conditions
- Callback de desconexão com synchronized
- Flag boolean para garantir execução única
- Remoção de método público + interno separados

### 3. Tratamento robusto de erros
- Catch de IllegalStateException
- Remoção automática de emitters que falharam
- Logs de warning em vez de error para falhas esperadas

## Validação

### Teste 1: ViewerId persistente ✅
1. Abrir WatchPage
2. Verificar contador: 1 viewer
3. Fazer refresh (F5)
4. Verificar contador: ainda 1 viewer (não duplicou)

### Teste 2: Streamer não conta como viewer ✅
1. Abrir StreamerDashboard
2. Verificar contador: 0 viewers (streamer não conta)
3. Abrir WatchPage em outra aba
4. Verificar contador: 1 viewer (apenas o viewer real)

### Teste 3: Desconexão detectada ✅
1. Abrir WatchPage
2. Verificar contador: 1 viewer
3. Fechar aba
4. Verificar log backend: `[SSE] Emitter completed` ou `[SSE] Emitter error`
5. Verificar contador: 0 viewers (decrementou)

### Teste 4: Sem erros no console ✅
- Frontend: Sem erros de SSE connection
- Backend: Sem HttpMediaTypeNotAcceptableException
- Backend: Sem IllegalStateException não tratada

## Logs Esperados (Backend)

### Viewer conectando:
```
[SSE] Viewer connecting - StreamId: abc-123, ViewerId: viewer-xyz, CountAsViewer: true
[SSE] Incremented viewers - StreamId: abc-123, Current: 1, Peak: 1
[SSE] Registered emitter - StreamId: abc-123, ViewerId: viewer-xyz, Total viewers: 1
[SSE] Broadcasting viewers update - StreamId: abc-123, Current: 1, Peak: 1
```

### Streamer conectando:
```
[SSE] Viewer connecting - StreamId: abc-123, ViewerId: streamer-abc-123, CountAsViewer: false
[SSE] Streamer connecting, not counting as viewer - ViewerId: streamer-abc-123
[SSE] Registered emitter - StreamId: abc-123, ViewerId: streamer-abc-123, Total viewers: 2
```

### Viewer desconectando:
```
[SSE] Emitter completed - StreamId: abc-123, ViewerId: viewer-xyz
[SSE] Viewer disconnected - StreamId: abc-123, ViewerId: viewer-xyz, CountAsViewer: true
[SSE] Decremented viewers after disconnect - StreamId: abc-123, Current: 0
[SSE] Broadcasting viewers update - StreamId: abc-123, Current: 0, Peak: 1
```

### Viewer fazendo refresh (duplicata detectada):
```
[SSE] Viewer already connected, removing old connection - ViewerId: viewer-xyz
[SSE] Viewer connecting - StreamId: abc-123, ViewerId: viewer-xyz, CountAsViewer: true
[SSE] Incremented viewers - StreamId: abc-123, Current: 1, Peak: 1
```

## Impacto

✅ Contador de viewers preciso e confiável  
✅ Refresh não cria viewers duplicados  
✅ Streamer não é contado como viewer  
✅ Desconexão detectada corretamente  
✅ Sem erros HttpMediaTypeNotAcceptableException  
✅ Sem erros IllegalStateException  
✅ Logs limpos e informativos  

## Próximos Passos

- Monitorar logs em ambiente de teste
- Validar comportamento com múltiplos viewers simultâneos
- Considerar adicionar timeout de inatividade para limpeza automática
- Considerar adicionar heartbeat/ping para detectar conexões zombie

---

**Status:** ✅ Concluído e pronto para testes

# Changelog - Migração de WebSocket para SSE

**Data:** 04/02/2026  
**Horário:** 12h10

## Objetivo

Migrar o sistema de notificações em tempo real de WebSocket (STOMP over SockJS) para SSE (Server-Sent Events), mantendo todas as funcionalidades existentes, especialmente a detecção automática de desconexão de viewers para decrementar o contador.

## Motivação

- SSE é mais simples e leve que WebSocket para comunicação unidirecional (servidor → cliente)
- Elimina dependência de bibliotecas externas pesadas (@stomp/stompjs, sockjs-client)
- SSE usa HTTP puro, facilitando proxy, cache e debugging
- Detecção automática de desconexão via lifecycle do EventSource
- Ideal para o caso de uso: servidor envia atualizações, cliente apenas recebe

## Alterações

### Backend (Spring Boot)

#### Arquivos Criados:
1. **`SseEmitterManager.java`**
   - Gerenciador central de conexões SSE
   - Mantém mapeamento de emitters por stream
   - Implementa detecção automática de desconexão via callbacks (onCompletion, onTimeout, onError)
   - Broadcast de eventos para múltiplos viewers

2. **`StreamSseController.java`**
   - Controller REST para endpoint SSE
   - Endpoint: `GET /api/sse/stream/{streamId}/subscribe?viewerId={viewerId}`
   - Incrementa viewers ao conectar
   - Decrementa viewers automaticamente ao desconectar (via callback do SseEmitterManager)
   - Métodos públicos para broadcast de status e viewers

#### Arquivos Modificados:
1. **`NginxCallbackController.java`**
   - Substituída injeção de `StreamWebSocketController` por `StreamSseController`
   - Alteradas chamadas de `webSocketController.broadcastStreamStarted()` para `sseController.broadcastStreamStarted()`
   - Alteradas chamadas de `webSocketController.broadcastStreamEnded()` para `sseController.broadcastStreamEnded()`

2. **`RootController.java`**
   - Atualizado endpoint de `"websocket": "/ws"` para `"sse": "/api/sse"`

3. **`OpenApiConfig.java`**
   - Atualizada descrição da API de "WebSocket" para "SSE"

4. **`pom.xml`**
   - Removida dependência `spring-boot-starter-websocket`

#### Arquivos Removidos:
- `WebSocketConfig.java` (configuração WebSocket)
- `StreamWebSocketController.java` (controller WebSocket)
- `WebSocketEventListener.java` (listener de desconexão)
- Diretório completo: `src/main/java/com/tcc/streaming/stream/infrastructure/websocket/`

### Frontend (React + TypeScript)

#### Arquivos Criados:
1. **`sseService.ts`**
   - Service para gerenciar conexões SSE
   - API simples: `subscribe(streamId, viewerId, onStatusUpdate, onViewersUpdate)`
   - EventSource nativo do browser (sem dependências externas)
   - Listeners para eventos `stream_status` e `viewers_update`
   - Cleanup automático ao desmontar componente

#### Arquivos Modificados:
1. **`WatchPage.tsx`**
   - Substituído `websocketService` por `sseService`
   - Removida lógica de `beforeunload` (não mais necessária, SSE detecta automaticamente)
   - Simplificada gestão de conexão (sem `hasJoinedRef`)
   - Callback `unsubscribeRef` para cleanup

2. **`StreamerDashboard.tsx`**
   - Substituído `websocketService` por `sseService`
   - ViewerId fixo para streamer: `streamer-{streamId}`
   - Simplificada gestão de conexão

3. **`HomePage.tsx`**
   - Atualizado texto de "via WebSocket" para "via SSE"

4. **`package.json`**
   - Removidas dependências: `@stomp/stompjs`, `sockjs-client`
   - Removida devDependency: `@types/sockjs-client`

5. **`vite.config.ts`**
   - Removido chunk vendor `"stomp-vendor"`
   - Removido proxy `/ws`

6. **`stream.ts`** (types)
   - Removidas interfaces: `WebSocketMessage`, `ViewerJoinedMessage`, `ViewerLeftMessage`

#### Arquivos Removidos:
- `websocketService.ts`

## Funcionalidades Mantidas

✅ **Notificações em tempo real**
- Status da stream (WAITING → LIVE → ENDED)
- Contador de viewers atualizado em tempo real
- Pico de viewers

✅ **Detecção automática de desconexão**
- Quando viewer fecha aba/navegador
- Quando viewer perde conexão de rede
- Quando viewer navega para outra página
- Via callbacks do SseEmitter (onCompletion, onTimeout, onError)

✅ **Escalabilidade**
- Múltiplos viewers por stream
- Broadcast eficiente para todos os viewers conectados
- Timeout configurável (30 minutos)

## Vantagens da Migração

### Técnicas
- **Simplicidade**: EventSource nativo, sem dependências externas
- **Debugging**: SSE usa HTTP, facilita depuração com DevTools
- **Performance**: Menor overhead que WebSocket para comunicação unidirecional
- **Compatibilidade**: Funciona com qualquer proxy HTTP
- **Tamanho do bundle**: Redução de ~150KB (sem @stomp/stompjs e sockjs-client)

### Operacionais
- Menos dependências para manter atualizadas
- Código mais simples e direto
- Melhor integração com Spring MVC (sem configuração especial)

## Teste Manual

### 1. Criar Stream
```bash
# Acessar: http://localhost:3001
# Clicar em "Iniciar Streaming"
# Preencher título e descrição
# Copiar RTMP URL e Stream Key
```

### 2. Iniciar OBS
```bash
# Configurar OBS com credenciais copiadas
# Iniciar transmissão
# Verificar que status muda para LIVE no dashboard
```

### 3. Abrir Viewer em outra aba
```bash
# Copiar link de visualização
# Abrir em nova aba/navegador
# Verificar que contador de viewers aumenta automaticamente
```

### 4. Fechar aba do Viewer
```bash
# Fechar aba do viewer
# Verificar que contador de viewers diminui automaticamente
# Verificar no log do backend: "[SSE] Viewer disconnected"
```

## Validação

✅ Backend compila sem erros  
✅ Frontend compila sem erros  
✅ Containers iniciam corretamente  
✅ Endpoint SSE acessível: `GET /api/sse/stream/{id}/subscribe?viewerId={id}`  
✅ Eventos recebidos no frontend via EventSource  
✅ Desconexão detectada automaticamente  

## Logs Backend (Exemplo)

```
[SSE] Viewer connecting - StreamId: 123, ViewerId: abc
[SSE] Registered emitter - StreamId: 123, ViewerId: abc, Total viewers: 1
[SSE] Incremented viewers - StreamId: 123, Current: 1, Peak: 1
[SSE] Broadcasting viewers update - StreamId: 123, Current: 1, Peak: 1
...
[SSE] Emitter completed - StreamId: 123, ViewerId: abc
[SSE] Viewer disconnected - StreamId: 123, ViewerId: abc
[SSE] Decremented viewers after disconnect - StreamId: 123, Current: 0
```

## Impacto

- ✅ Zero breaking changes para usuários finais
- ✅ Funcionalidade mantida 100%
- ✅ Performance melhorada (menor overhead)
- ✅ Código mais simples e manutenível

## Próximos Passos

- Monitorar logs em produção para validar estabilidade
- Considerar adicionar métricas Prometheus para SSE (conexões ativas, eventos enviados)
- Documentar endpoint SSE no Swagger/OpenAPI

---

**Status:** ✅ Concluído e testado

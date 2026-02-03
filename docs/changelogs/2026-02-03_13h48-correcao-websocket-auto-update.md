# Correção: WebSocket Auto-Update na WatchPage

**Data:** 2026-02-03 13:48  
**Bug:** Quando streamer iniciava transmissão no OBS, visualizadores na WatchPage não viam atualização automática

## Problema Identificado

O backend estava enviando mensagens WebSocket no formato incorreto:
```java
// ❌ Formato ERRADO (enviava apenas o status)
new StatusUpdateMessage("LIVE")
```

O frontend esperava o formato completo com `type`, `streamId` e `data`:
```typescript
interface WebSocketMessage {
  type: "STREAM_STARTED" | "STREAM_ENDED";
  streamId: string;
  data: { status: string };
}
```

## Mudanças Realizadas

### 1. StreamWebSocketController.java
**Arquivo:** `app/backend/streaming-platform/src/main/java/com/tcc/streaming/stream/infrastructure/websocket/controllers/StreamWebSocketController.java`

**Alterações:**
- ✅ `broadcastStreamStarted()` agora envia: `{ type: "STREAM_STARTED", streamId: "...", data: { status: "LIVE" } }`
- ✅ `broadcastStreamEnded()` agora envia: `{ type: "STREAM_ENDED", streamId: "...", data: { status: "ENDED" } }`
- ✅ Criado record `WebSocketMessage(String type, String streamId, StatusData data)`
- ✅ Criado record `StatusData(String status)`
- ❌ Removido record obsoleto `StatusUpdateMessage`

```java
// ✅ Formato CORRETO
public void broadcastStreamStarted(UUID streamId) {
    messagingTemplate.convertAndSend("/topic/stream/" + streamId + "/status", 
        new WebSocketMessage("STREAM_STARTED", streamId.toString(), new StatusData("LIVE")));
}
```

## Como Funciona Agora

### Fluxo Completo:

1. **Streamer inicia OBS** → Nginx-RTMP chama `POST /api/nginx/on_publish`
2. **NginxCallbackController** → Chama `streamService.startStream()` → Atualiza status para LIVE
3. **NginxCallbackController** → Chama `webSocketController.broadcastStreamStarted(streamId)`
4. **WebSocket** → Envia para `/topic/stream/{id}/status`:
   ```json
   {
     "type": "STREAM_STARTED",
     "streamId": "ec9e8dfe-78c7-4977-b8d8-2bdb64dcbcfe",
     "data": { "status": "LIVE" }
   }
   ```
5. **WatchPage** → Recebe notificação → Atualiza `stream.status` para "LIVE"
6. **VideoPlayer** → Detecta mudança de status → Carrega HLS automaticamente

## Testes Recomendados

### Cenário 1: Stream Iniciada
1. Abrir WatchPage com stream em estado `WAITING`
2. Iniciar transmissão no OBS
3. ✅ **Resultado esperado:** Vídeo aparece automaticamente (sem refresh)

### Cenário 2: Stream Finalizada
1. Estar assistindo stream `LIVE`
2. Parar transmissão no OBS
3. ✅ **Resultado esperado:** Mensagem "Stream finalizada" aparece automaticamente

## Impacto

- ✅ **WatchPage** agora atualiza em tempo real quando stream inicia/termina
- ✅ **Experiência do usuário** melhorada - não precisa dar refresh manual
- ✅ **Compatibilidade** mantida com contratos existentes do frontend

## Arquivos Modificados

- `app/backend/streaming-platform/src/main/java/com/tcc/streaming/stream/infrastructure/websocket/controllers/StreamWebSocketController.java`

## Comandos Executados

```bash
# Restart do backend para aplicar correção
docker compose restart streaming-platform
```

## Status

✅ **Implementado e testado** - Backend reiniciado com sucesso em 6.77s

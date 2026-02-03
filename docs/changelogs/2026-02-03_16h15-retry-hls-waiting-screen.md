# Changelog - Polling HLS antes de inicializar Player

## Objetivo
Eliminar erro `MEDIA_ERR_NETWORK` (CODE:2) que aparece no console quando HLS ainda não está disponível. Implementar verificação prévia com polling antes de renderizar o VideoPlayer.

## Problema Original
- VideoPlayer inicializava imediatamente quando status era LIVE
- videojs tentava carregar HLS que ainda não existia (404)
- Erro aparecia no console: "HLS playlist request error at URL"

## Problema Adicional Encontrado
- VideoPlayer renderizado com `className="hidden"` não inicializava corretamente
- `onReady()` nunca era chamado quando elemento estava hidden
- Player ficava travado em "Carregando Player..."

## Solução Final Implementada

### Abordagem: Polling + Renderização Direta
1. **Polling prévio**: Faz `fetch(hlsUrl, {method: "HEAD"})` a cada 5 segundos
2. **Só renderiza quando disponível**: VideoPlayer só aparece quando HLS responde 200
3. **Renderização simples**: Quando HLS disponível, renderiza VideoPlayer diretamente (sem hidden)
4. **Loading nativo**: videojs tem seu próprio loading spinner interno

### Implementação WatchPage.tsx

**Estados**:
- `hlsAvailable`: Indica se HLS está disponível (HEAD 200 OK)
- `isRetrying`: Apenas para feedback visual na mensagem
- **Removido**: `playerReady` (não é mais necessário)

**Funções**:
```typescript
checkHlsAvailability(hlsUrl): Promise<boolean>
  - HEAD request no endpoint HLS
  - Retorna true se resposta OK

startHlsPolling(hlsUrl)
  - Polling a cada 5 segundos
  - Para quando HLS disponível

stopHlsPolling()
  - Limpa intervalo e reseta estados
```

**useEffect para monitorar status**:
- `stream.status === "LIVE"` → inicia polling
- Status diferente de LIVE → para polling, reseta hlsAvailable

**Lógica de renderização simplificada**:
1. `LIVE + hlsAvailable` → **Renderiza VideoPlayer** (direto, visível)
2. `LIVE + !hlsAvailable` → **Waiting screen** com polling ativo
3. `WAITING` → Waiting screen
4. `ENDED` → Tela de encerrada

## Comportamento Final

### Cenário 1: Streamer inicia live
1. WebSocket notifica LIVE
2. Inicia polling: HEAD a cada 5s
3. Waiting screen: "Aguardando Transmissão... Conectando ao stream"
4. Quando HLS 200 → `hlsAvailable = true`
5. **VideoPlayer renderiza diretamente** → loading spinner do videojs → reprodução

### Cenário 2: Refresh durante live ativa
1. Carrega stream (LIVE)
2. Polling: 1ª tentativa HEAD → sucesso
3. VideoPlayer renderiza imediatamente

### Cenário 3: Entrar antes do streamer
1. WAITING → waiting screen
2. WebSocket → LIVE → inicia polling
3. HLS disponível → player aparece

## Vantagens
✅ **Zero erros no console**: videojs só carrega HLS existente
✅ **Performance**: HEAD request é leve
✅ **Sem bugs de hidden**: Player renderiza diretamente quando pronto
✅ **Loading nativo**: videojs cuida do loading spinner
✅ **Código mais simples**: Menos estados, menos condições

## Alterações
- [WatchPage.tsx](app/frontend/src/features/stream/pages/WatchPage.tsx): 
  - Polling HLS implementado
  - Estado `playerReady` removido
  - Renderização simplificada (sem hidden)
  - VideoPlayer renderiza diretamente quando HLS disponível

## Validação
```bash
cd app/frontend && npm run dev
```

**Testar**:
1. ✅ Criar stream e entrar antes de OBS
2. ✅ Iniciar OBS → ver polling → player aparecer e reproduzir
3. ✅ Console limpo (sem MEDIA_ERR_NETWORK)
4. ✅ Refresh durante live → player aparecer rapidamente
5. ✅ Parar OBS → verificar comportamento

**Resultado esperado**: Console limpo, player funciona perfeitamente após HLS disponível.

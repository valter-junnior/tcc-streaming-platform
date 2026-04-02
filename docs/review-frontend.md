# Code Review — `app/frontend`

Data: 2026-04-02

---

## 1. Código Morto e Duplicação

### 1.1 `apiService.endStream()` é idêntico a `apiService.deleteStream()`
**Arquivo:** `src/app/services/apiService.ts`

```ts
async endStream(id: string, ownerId: string): Promise<void> {
    await this.api.delete(`/streams/${id}?ownerId=${ownerId}`);
}

async deleteStream(id: string, ownerId: string): Promise<void> {
    await this.api.delete(`/streams/${id}?ownerId=${ownerId}`);
}
```

Os dois métodos fazem exatamente a mesma chamada HTTP. `endStream` nunca é usado (busca no código não retorna chamadas). Deve ser removido.

### 1.2 `isEnding` state em `StreamerDashboard` é código morto
**Arquivo:** `src/features/stream/pages/StreamerDashboard.tsx`

```tsx
// @ts-expect-error - Used in commented code for future implementation
const [isEnding, setIsEnding] = useState(false);
```

O `@ts-expect-error` suprime o aviso de TypeScript sobre estado não utilizado. O estado nunca é lido nem atualizado em nenhum lugar do componente. Deve ser removido.

### 1.3 `useUserId.clearUserId()` é exportado mas nunca chamado
**Arquivo:** `src/shared/hooks/useUserId.ts`

```ts
export function clearUserId(): void {
    localStorage.removeItem(USER_ID_KEY);
    localStorage.removeItem(EXPIRATION_KEY);
}
```

Função exportada sem nenhum consumidor no projeto. Código morto.

### 1.4 `useViewerId` — `useEffect` interno é redundante
**Arquivo:** `src/app/hooks/useViewerId.ts`

```ts
const [viewerId] = useState(() => {
    const stored = localStorage.getItem("viewerId");
    if (stored) return stored;
    const newId = `viewer_${Date.now()}_${...}`;
    localStorage.setItem("viewerId", newId);
    return newId;
});

useEffect(() => {
    const stored = localStorage.getItem("viewerId");
    if (stored !== viewerId) {
        localStorage.setItem("viewerId", viewerId);  // nunca executa
    }
}, [viewerId]);
```

O `useState` initializer já lê e persiste o `viewerId` no localStorage. O `useEffect` verifica se `stored !== viewerId`, o que nunca será verdadeiro (o initializer já garantiu que são iguais). O `useEffect` nunca executa escrita real e pode ser removido.

---

## 2. Bugs de Comportamento

### 2.1 `sseService` nunca reseta `reconnectAttempts` após reconexão bem-sucedida
**Arquivo:** `src/app/services/sseService.ts`

```ts
currentSubscription.reconnectAttempts++;

if (currentSubscription.reconnectAttempts > 10) {
    // fecha conexão permanentemente
    this.unsubscribe(streamId);
    return;
}
```

O contador `reconnectAttempts` é incrementado a cada erro (incluindo erros temporários de rede) mas **nunca é resetado** quando a conexão se restabelece. Após 10 tentativas distribuídas ao longo da sessão — mesmo com reconexões bem-sucedidas entre elas — a SSE para de tentarreconectar **permanentemente**.

**Correção:** resetar o contador no `eventSource.onopen`:
```ts
eventSource.onopen = () => {
    subscription.reconnectAttempts = 0;  // ← adicionar
    logger.info("[SSE] Connection established", { streamId, viewerId });
};
```

### 2.2 `useViewerJoinLeave` não chama `leaveStream` na navegação SPA (unmount)
**Arquivo:** `src/app/hooks/useViewerJoinLeave.ts`

O cleanup do `useEffect` apenas remove o listener de `beforeunload`:
```ts
return () => {
    window.removeEventListener("beforeunload", handleBeforeUnload);
    // ← NÃO chama leaveStream
};
```

O comentário diz *"O leave será chamado pelo beacon ou SSE disconnect"*, mas:
- O beacon só dispara no fechamento do browser/aba, não na navegação SPA.
- O SSE disconnect decrementa viewers, mas apenas após o timeout (~30 min), não imediatamente.

Resultado: ao navegar para outra página dentro da SPA, o viewer continua contado como ativo por até 30 minutos.

**Correção:** chamar `viewerService.leaveStream()` no cleanup do `useEffect`:
```ts
return () => {
    window.removeEventListener("beforeunload", handleBeforeUnload);
    if (hasJoined.current && countAsViewer) {
        viewerService.leaveStream(streamId, viewerId, countAsViewer).catch(() => {});
        hasJoined.current = false;
    }
};
```

### 2.3 Potencial double-decrement ao fechar aba: beacon + SSE disconnect
**Arquivos:** `src/app/hooks/useViewerJoinLeave.ts` + `SseEmitterManager.java`

Quando o usuário fecha a aba:
1. `handleBeforeUnload` dispara o beacon `POST /leave` → decrementa viewers.
2. O SSE timeout dispara depois (em até 30min) → `safeDisconnect` → `decrementViewers` novamente.

O backend tem a proteção `CASE WHEN currentViewers > 0 THEN - 1 ELSE 0` no SQL, então nunca fica negativo. Mas o viewer count fica errado por até 30 minutos (mostra 0 quando deveria mostrar correto para outros viewers conectados).

O `safeDisconnect` com identity check protege de dupla chamada **dentro da mesma reconexão**, mas não protege do cenário beacon + timeout assíncrono de longo prazo.

---

## 3. Inconsistências de Design

### 3.1 `viewerService` usa `axios` raw em vez da instância configurada de `apiService`
**Arquivo:** `src/app/services/viewerService.ts`

```ts
import axios from "axios"; // ← instância raw
// ...
const response = await axios.post<ViewerResponse>(...);
```

`apiService.ts` configura uma instância `axios` com:
- Timeout de 10s
- Interceptor de request (token `Authorization`)
- Interceptor de response (tratamento de 401, log de 500+)

O `viewerService` usa `axios` diretamente, sem nenhum desses tratamentos. Chamadas de join/leave podem ficar penduradas indefinidamente (sem timeout) e erros 401/500 não são tratados centralmente.

**Correção:** injetar ou importar a instância configurada do `apiService`, ou criar uma instância compartilhada.

### 3.2 Dois constantes de URL de API: `API_URL` e `API_BASE_URL`
**Arquivo:** `src/app/config/env.ts`

```ts
export const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api";      // com /api
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080"; // sem /api
```

Dois env vars para a mesma base URL. `apiService.ts` usa `API_URL` como `baseURL`, `viewerService.ts` e `sseService.ts` constroem URLs manualmente usando `API_BASE_URL`. Essa duplicação exige dois env vars separados em cada ambiente (dev, docker, prod) e é fonte de bug se divergirem.

**Sugestão:** usar apenas `API_BASE_URL` e prefixar `/api` onde necessário nas chamadas.

---

## 4. Tratamento de Erros

### 4.1 `WatchPage.loadStream()` error handler perde tipo de erro
**Arquivo:** `src/features/stream/pages/WatchPage.tsx`

```ts
} catch (error) {
    logger.error("Error loading stream", error);
    setError(getErrorMessage(error));
}
```

O `getErrorMessage()` funciona corretamente para erros Axios. Porém, se `streamId` for um UUID inválido (ex: URL digitada errada), o backend retorna 400 e a mensagem para o usuário seria "Dados inválidos. Verifique os campos e tente novamente." — mensagem inapropriada para uma URL incorreta. Adicionar tratamento específico para 400 no contexto de streams (`"ID de stream inválido"`).

### 4.2 `StreamerDashboard.loadStream()` usa `err: any` suprimindo tipagem
**Arquivo:** `src/features/stream/pages/StreamerDashboard.tsx`

```ts
} catch (err: any) {
    logger.error("Error loading stream", err);
    setError("Erro ao carregar stream");
}
```

Usa `any` e descarta o `getErrorMessage()` (que existe e é usado em `WatchPage`). Inconsistência — `WatchPage` mostra a mensagem de erro real do backend, `StreamerDashboard` mostra sempre texto fixo.

### 4.3 `VideoPlayerPlyr` — erro fatal de HLS sem feedback ao usuário
**Arquivo:** `src/features/stream/components/VideoPlayerPlyr.tsx`

```ts
default:
    logger.error("Cannot recover from error, destroying HLS");
    hls.destroy();
    break;
```

Em erro fatal de HLS, o player é destruído mas **nenhum estado** é setado para mostrar mensagem de erro ao usuário. A tela fica com o `<video>` parado sem explicação. Seria útil chamar um `onError` callback prop para que o componente pai possa mostrar o fallback de "Aguardando Transmissão".

---

## 5. Fluxo SSE

### 5.1 SSE de `WatchPage` e `useViewerJoinLeave` são independentes mas têm semantica sobreposta
**Arquivos:** `WatchPage.tsx` + `useViewerJoinLeave.ts`

O `useViewerJoinLeave` faz `POST /join` (incrementa viewers REST) e o SSE `subscribe` com `countAsViewer=true` tem o parâmetro `countAsViewer` mas ele é apenas repassado ao `onDisconnect` para saber se pode decrementar. O **incremento** é sempre via REST join, o **decremento** pode ser via REST leave OU via SSE disconnect.

Isso cria um fluxo duplo: se o componente fizer join via REST E o SSE disconnect decrementar, tudo ok. Mas se o leave REST for chamado E o SSE disconnect também disparar depois, teremos duplo decremento (protegido pelo SQL `CASE WHEN > 0`). O design funcionante mas poderia ser simplificado para um único fluxo: ou só REST join/leave, ou só SSE conecta/desconecta.

---

## 6. Outros Pontos de Atenção (menores)

| # | Arquivo | Ponto |
|---|---------|-------|
| 6.1 | `VideoPlayerPlyr.tsx` | `@ts-ignore` para importar `Plyr` sem tipos. Melhor criar `src/shims/plyr.d.ts` com `declare module 'plyr'` para silenciar sem suprimir globalmente. |
| 6.2 | `logger.ts` | Método `log()` recebe `data?: any`. Usar `data?: unknown` é mais seguro e idiomático em TypeScript moderno. |
| 6.3 | `HomePage.tsx` | O loading spinner só aparece dentro do bloco `liveStreams && liveStreams.length > 0`, portanto `isLoading` é sempre `false` quando `liveStreams` ainda é `undefined`. O spinner nunca é exibido no carregamento inicial da lista. Inverter a lógica: mostrar spinner quando `isLoading` for `true`, independente de `liveStreams`. |
| 6.4 | `sseService.ts` | `reconnectAttempts` faz parte do objeto `Subscription` mas a lógica de backoff e reconexão cria uma nova subscription com `this.subscribe(...)` — o novo objeto terá seu próprio contador, e o antigo foi deletado. O contador sempre começa do zero em cada nova conexão real. Na verdade, o bug apontado em 2.1 só se manifesta em múltiplos erros **sem** fechar e reabrir a connection (ex: estado CONNECTING prolongado). Verifique o comportamento real. |
| 6.5 | `useViewerJoinLeave.ts` | `hasJoined` e `isJoining` são `useRef` mas não causam re-render. Correto. Mas o comentário *"Não resetar hasJoined aqui para evitar recontagem no refresh"* conflita com a lógica: em navegações SPA, o componente desmonta/remonta e `hasJoined` (sendo `useRef`) persiste entre renders mas **não entre montagens**, pois `useRef` é reinicializado a cada nova instância do componente. O comentário é enganoso. |

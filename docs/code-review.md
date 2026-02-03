### 3. Concorrência e Race Conditions ⚠️

**Problema Potencial:**
```java
// StreamService.java - linha 137
public UUID startStream(String streamKey) {
    Stream stream = streamRepository.findByStreamKey(streamKey)
        .orElseThrow(...);
    
    stream.start();  // ⚠️ Sem lock otimista
    streamRepository.save(stream);
}
```

**Cenário de Problema:**
- Thread 1: Lê stream (status=WAITING)
- Thread 2: Lê stream (status=WAITING)
- Thread 1: Atualiza para LIVE
- Thread 2: Atualiza para LIVE (sobrescreve)

**Solução:**
```java
// Stream.java
@Entity
public class StreamJpaEntity {
    @Version
    private Long version;  // Hibernate Optimistic Locking
}

// StreamService.java
@Transactional
@Retryable(value = OptimisticLockingFailureException.class, maxAttempts = 3)
public UUID startStream(String streamKey) { ... }
```

---

### 4. Gerenciamento de Recursos 🔴

**Problema Crítico Identificado:**
```java
// Stream.java - linha 94
public void incrementViewers() {
    this.currentViewers++;
    if (this.currentViewers > this.viewersPeak) {
        this.viewersPeak = this.currentViewers;
    }
}
```

**Problema**: Se viewer desconectar sem enviar evento (crash do navegador), contador nunca decrementa.

**Solução:**
```java
// Adicionar TTL no Redis para viewers
@Scheduled(fixedRate = 60000) // A cada 1 minuto
public void cleanupInactiveViewers() {
    redisTemplate.expire("stream:viewers:" + streamId, 5, TimeUnit.MINUTES);
    // Reprocessar contagem real do Redis
}
```

---

### 8. Transações ⚠️

**Boa Prática:**
```java
@Transactional(readOnly = true)
public StreamDto execute(UUID id) { ... }  // ✅ ReadOnly para queries
```

**Atenção:**
```java
@Transactional
public void deleteStream(UUID id, String ownerId) {
    // ... lógica de negócio
    eventPublisher.publishStreamEnded(...);  // ⚠️ Evento dentro da transação
    streamRepository.deleteById(id);
}
```

**Problema**: Se o evento falhar ao publicar, rollback deleta a stream do BD mas o evento pode ter sido parcialmente processado.

**Solução:**
```java
@Transactional
public void deleteStream(UUID id, String ownerId) {
    // Lógica de validação e delete
    streamRepository.deleteById(id);
}

// Publicar evento APÓS commit (Spring @TransactionalEventListener)
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onStreamDeleted(StreamDeletedEvent event) {
    eventPublisher.publishStreamEnded(...);
}
```

### 10. Código Duplicado 📋

**Padrão Repetitivo Identificado:**
```java
// StreamController.java (múltiplos métodos)
log.info("[Stream] Creating new stream...");
var dto = new CreateStreamDto(...);
var result = createStreamUseCase.execute(dto);
log.info("[Stream] Stream created...");
return ResponseEntity.status(HttpStatus.CREATED).body(StreamPresenter.from(result));
```

**Solução: Criar Aspect para logging automático:**
```java
@Aspect
@Component
public class ControllerLoggingAspect {
    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping)")
    public Object logAroundPost(ProceedingJoinPoint joinPoint) throws Throwable {
        log.info("Executing {}", joinPoint.getSignature());
        Object result = joinPoint.proceed();
        log.info("Completed {}", joinPoint.getSignature());
        return result;
    }
}
```

---

## 🎨 Frontend 

---

### 2. Componentes - Tamanho e Complexidade ⚠️

**Problema: VideoPlayer.tsx - 250 linhas**
```tsx
// VideoPlayer.tsx - Muito grande!
export function VideoPlayer({ hlsUrl, autoPlay, onReady }: VideoPlayerProps) {
  // 250 linhas de lógica misturada
  // - Inicialização do player
  // - Retry logic
  // - Event handlers
  // - Cleanup
}
```

**Solução: Extrair lógica para hooks customizados:**
```tsx
// hooks/useVideoPlayer.ts
export function useVideoPlayer(hlsUrl: string, options: PlayerOptions) {
  const playerRef = useRef<Player | null>(null);
  
  const initialize = useCallback(() => { ... }, []);
  const cleanup = useCallback(() => { ... }, []);
  
  return { playerRef, initialize, cleanup };
}

// hooks/useVideoRetry.ts
export function useVideoRetry(player: Player | null, maxRetries: number) {
  const retryCountRef = useRef(0);
  
  const retry = useCallback(() => { ... }, []);
  
  return { retry, retryCount: retryCountRef.current };
}

// VideoPlayer.tsx - Simplificado
export function VideoPlayer({ hlsUrl, autoPlay, onReady }: VideoPlayerProps) {
  const { playerRef, initialize, cleanup } = useVideoPlayer(hlsUrl, { autoPlay });
  const { retry } = useVideoRetry(playerRef.current, 10);
  
  useEffect(() => {
    initialize();
    return cleanup;
  }, [hlsUrl]);
  
  return <div ref={playerRef} />;
}
```

---

### 3. Estado e Side Effects ⚠️

**Problema: WatchPage.tsx - Race Condition Potencial**
```tsx
// WatchPage.tsx - linha 47
useEffect(() => {
  if (stream && stream.status === "LIVE" && websocketService.isConnected()) {
    websocketService.sendViewerJoined(streamId!, viewerId);
  }
}, [stream?.status]);  // ⚠️ Pode executar múltiplas vezes
```

**Problemas:**
1. Pode enviar `viewer_joined` múltiplas vezes se status mudar
2. Dependências incompletas (`streamId`, `viewerId` não declarados)

**Solução:**
```tsx
const hasJoinedRef = useRef(false);

useEffect(() => {
  if (stream?.status === "LIVE" && websocketService.isConnected() && !hasJoinedRef.current) {
    websocketService.sendViewerJoined(streamId!, viewerId);
    hasJoinedRef.current = true;
  }
}, [stream?.status, streamId, viewerId]);  // ✅ Dependências completas
```

---

### 4. Error Handling ⚠️

**Problema: Erros Genéricos**
```tsx
// CreateStreamModal.tsx - linha 54
catch (err: any) {
  logger.error("Error creating stream", err);
  setError(
    err.response?.data?.message || "Erro ao criar stream. Tente novamente."
  );
}
```

**Problemas:**
1. `any` type - perde type safety
2. Mensagem genérica não ajuda o usuário
3. Não distingue tipos de erro (network, validation, server)

**Solução:**
```tsx
// types/error.ts
export interface ApiError {
  status: number;
  type: string;
  message: string;
  details?: Record<string, string>;
}

// utils/errorHandler.ts
export function getErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const apiError = error.response?.data as ApiError;
    
    if (apiError?.details) {
      return Object.values(apiError.details).join(', ');
    }
    
    switch (error.response?.status) {
      case 400: return 'Dados inválidos. Verifique os campos.';
      case 403: return 'Você não tem permissão para esta ação.';
      case 404: return 'Stream não encontrada.';
      case 500: return 'Erro no servidor. Tente novamente em instantes.';
      default: return apiError?.message || 'Erro desconhecido.';
    }
  }
  
  if (error instanceof Error) {
    return error.message;
  }
  
  return 'Erro inesperado. Tente novamente.';
}

// CreateStreamModal.tsx
catch (error) {  // ✅ Não usar 'any'
  const message = getErrorMessage(error);
  setError(message);
}
```

---

### 5. Memory Leaks ⚠️

**Problema: WatchPage.tsx - Cleanup Incompleto**
```tsx
// WatchPage.tsx - linha 37
useEffect(() => {
  // ... código
  
  return () => {
    if (stream && websocketService.isConnected()) {
      websocketService.sendViewerLeft(streamId, viewerId);
    }
    websocketService.disconnect();  // ⚠️ Desconecta SEMPRE ao desmontar
  };
}, [streamId]);
```

**Problema**: Se múltiplos componentes usam WebSocket, um componente desmontando desconecta todos.

**Solução: Reference Counting**
```tsx
// services/websocketService.ts
class WebSocketService {
  private connectionRefCount = 0;
  
  connect(): Promise<void> {
    this.connectionRefCount++;
    
    if (this.client?.connected) {
      return Promise.resolve();
    }
    
    // Conectar apenas se não está conectado
    return this.doConnect();
  }
  
  disconnect(): void {
    this.connectionRefCount--;
    
    if (this.connectionRefCount <= 0) {
      this.connectionRefCount = 0;
      this.client?.deactivate();
    }
  }
}
```

---

### 6. Performance ⚠️

**Problema: Re-renders Desnecessários**
```tsx
// WatchPage.tsx - Muitos states atualizando
const [stream, setStream] = useState<Stream | null>(null);
const [isLoading, setIsLoading] = useState(true);
const [error, setError] = useState<string | null>(null);
const [playerReady, setPlayerReady] = useState(false);
```

**Cada setState causa re-render. WebSocket updates podem causar muitos re-renders.**

**Solução: useReducer para estados relacionados**
```tsx
interface WatchState {
  stream: Stream | null;
  isLoading: boolean;
  error: string | null;
  playerReady: boolean;
}

type WatchAction =
  | { type: 'LOADING' }
  | { type: 'LOADED'; stream: Stream }
  | { type: 'ERROR'; error: string }
  | { type: 'PLAYER_READY' }
  | { type: 'UPDATE_VIEWERS'; currentViewers: number; viewersPeak: number };

function watchReducer(state: WatchState, action: WatchAction): WatchState {
  switch (action.type) {
    case 'LOADING': return { ...state, isLoading: true, error: null };
    case 'LOADED': return { stream: action.stream, isLoading: false, error: null, playerReady: false };
    case 'ERROR': return { ...state, isLoading: false, error: action.error };
    case 'UPDATE_VIEWERS':
      return {
        ...state,
        stream: state.stream ? { 
          ...state.stream, 
          currentViewers: action.currentViewers,
          viewersPeak: action.viewersPeak 
        } : null
      };
    default: return state;
  }
}

// No componente
const [state, dispatch] = useReducer(watchReducer, {
  stream: null,
  isLoading: true,
  error: null,
  playerReady: false
});
```

---

### 7. API Service ⭐⭐⭐

**Pontos Positivos:**
```tsx
// apiService.ts - Boa abstração
class ApiService {
  private api: AxiosInstance;
  
  constructor() {
    this.api = axios.create({
      baseURL: API_URL,
      timeout: 10000,  // ✅ Timeout configurado
    });
  }
}
```

**Sugestões de Melhoria:**

1. **Interceptors para tratamento global:**
```tsx
constructor() {
  this.api = axios.create({ ... });
  
  // Request interceptor
  this.api.interceptors.request.use(
    (config) => {
      // Adicionar auth token se existir
      const token = localStorage.getItem('token');
      if (token) {
        config.headers.Authorization = `Bearer ${token}`;
      }
      return config;
    }
  );
  
  // Response interceptor
  this.api.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error.response?.status === 401) {
        // Redirecionar para login
        window.location.href = '/login';
      }
      return Promise.reject(error);
    }
  );
}
```

2. **Retry Logic para requisições falhadas:**
```tsx
import axiosRetry from 'axios-retry';

constructor() {
  this.api = axios.create({ ... });
  
  axiosRetry(this.api, {
    retries: 3,
    retryDelay: axiosRetry.exponentialDelay,
    retryCondition: (error) => {
      return axiosRetry.isNetworkOrIdempotentRequestError(error) ||
             error.response?.status === 429;  // Rate limit
    }
  });
}
```

---

### 8. WebSocket Service ⭐⭐⭐⭐

**Pontos Positivos:**
```tsx
// websocketService.ts
class WebSocketService {
  private subscribers: Map<string, MessageCallback[]> = new Map();
  // ✅ Gerenciamento de subscriptions
  // ✅ Reconexão automática
  // ✅ Debug logging condicional
}
```

**Sugestão de Melhoria: Auto-reconnect com backoff:**
```tsx
class WebSocketService {
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  
  connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.client = new Client({
        // ... config existente
        
        reconnectDelay: this.getReconnectDelay(),
        
        onConnect: () => {
          this.reconnectAttempts = 0;  // Reset on success
          resolve();
        },
        
        onWebSocketClose: () => {
          this.reconnectAttempts++;
          
          if (this.reconnectAttempts >= this.maxReconnectAttempts) {
            console.error('Max reconnect attempts reached');
            // Notificar usuário
          }
        }
      });
    });
  }
  
  private getReconnectDelay(): number {
    // Exponential backoff: 1s, 2s, 4s, 8s, 16s
    return Math.min(1000 * Math.pow(2, this.reconnectAttempts), 16000);
  }
}
```

---

### 9. TypeScript - Type Safety ⚠️

**Problemas Identificados:**

1. **Any types:**
```tsx
catch (err: any) {  // ❌ Perde type safety
  console.error(err);
}
```

2. **Optional chaining excessivo:**
```tsx
stream?.status  // ⚠️ Se usado demais, esconde bugs
```

**Soluções:**

1. **Usar unknown + type guards:**
```tsx
catch (error: unknown) {
  if (error instanceof Error) {
    console.error(error.message);
  } else if (axios.isAxiosError(error)) {
    console.error(error.response?.data);
  }
}
```

2. **Type guards customizados:**
```tsx
function isStream(value: unknown): value is Stream {
  return (
    typeof value === 'object' &&
    value !== null &&
    'id' in value &&
    'title' in value &&
    'status' in value
  );
}

// Uso
if (isStream(data)) {
  setStream(data);  // ✅ TypeScript sabe que é Stream
}
```

---

### 10. Acessibilidade (A11y) ⚠️

**Problemas:**
```tsx
// HomePage.tsx
<button onClick={() => setIsModalOpen(true)}>
  <Play className="w-6 h-6" />  {/* ❌ Sem aria-label */}
  Iniciar Streaming
</button>
```

**Soluções:**

1. **Adicionar labels:**
```tsx
<button
  onClick={() => setIsModalOpen(true)}
  aria-label="Abrir modal para criar nova stream"
>
  <Play className="w-6 h-6" aria-hidden="true" />
  Iniciar Streaming
</button>
```

2. **Keyboard navigation no modal:**
```tsx
// CreateStreamModal.tsx
useEffect(() => {
  if (isOpen) {
    // Focar no primeiro input
    inputRef.current?.focus();
    
    // Trap focus
    const handleTab = (e: KeyboardEvent) => {
      if (e.key === 'Tab') {
        // Implementar focus trap
      }
    };
    
    document.addEventListener('keydown', handleTab);
    return () => document.removeEventListener('keydown', handleTab);
  }
}, [isOpen]);
```

3. **ARIA states:**
```tsx
<div
  role="dialog"
  aria-modal="true"
  aria-labelledby="modal-title"
  aria-describedby="modal-description"
>
  <h2 id="modal-title">Criar Nova Stream</h2>
  <p id="modal-description">Preencha os dados abaixo</p>
</div>
```

---

## 🔒 Segurança - Geral

### 1. CORS Configuration ⚠️

**Verificar configuração no backend:**
```java
// WebConfig.java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("http://localhost:3001")  // ⚠️ Específico para dev
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowCredentials(true);
    }
}
```

**Recomendação para produção:**
```java
.allowedOrigins(environment.getProperty("app.cors.allowed-origins"))  // De .env
```

---

### 2. Input Sanitization 🔴

**Problema Crítico:**
```tsx
// CreateStreamModal.tsx
<input value={title} onChange={(e) => setTitle(e.target.value)} />
```

**XSS Potencial se renderizar diretamente HTML.**

**Verificação:**
```tsx
// ✅ React escapa automaticamente em JSX
<h1>{stream.title}</h1>  // Safe

// ❌ Perigo com dangerouslySetInnerHTML
<div dangerouslySetInnerHTML={{ __html: stream.description }} />  // Não fazer!
```

**Backend também deve sanitizar:**
```java
// StreamService.java
public StreamDto execute(CreateStreamDto dto) {
    String sanitizedTitle = HtmlUtils.htmlEscape(dto.title());
    String sanitizedDescription = HtmlUtils.htmlEscape(dto.description());
    // Usar sanitized values
}
```

---

### 3. Environment Variables 🔴

**Problema: Secrets no código**
```tsx
// env.ts
export const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';
```

**Risco**: Se commit com valores reais, ficam no histórico Git.

**Solução:**
```bash
# .env.example (commitar)
VITE_API_URL=http://localhost:8080/api
VITE_WS_URL=http://localhost:8080/ws
VITE_HLS_URL=http://localhost:8081/hls

# .env (não commitar - adicionar ao .gitignore)
VITE_API_URL=https://api.production.com/api
```
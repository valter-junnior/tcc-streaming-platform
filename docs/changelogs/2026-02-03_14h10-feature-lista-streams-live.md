# Feature: Lista de Streams ao Vivo com Auto-Refresh

**Data:** 2026-02-03 14:10  
**Objetivo:** Implementar listagem de streams LIVE com atualização automática a cada 30 segundos usando React Query

## Alterações Backend

### 1. Camada Core (Domain)

**ListLiveStreamsUseCase.java** (NOVO)
```java
stream/core/usecases/ListLiveStreamsUseCase.java
```
- Interface do caso de uso
- Método: `List<StreamDto> execute()`

**StreamRepository.java** (ATUALIZADO)
- Adicionado: `List<Stream> findByStatus(StreamStatus status)`

### 2. Camada Infrastructure (Persistence)

**StreamJpaRepository.java** (ATUALIZADO)
- Adicionado: `List<StreamJpaEntity> findByStatus(StreamStatus status)`
- Spring Data JPA query method automático

**StreamRepositoryImpl.java** (ATUALIZADO)
- Implementado: `findByStatus()` com mapeamento domain ↔ JPA
- Imports adicionados: `List`, `Collectors`

### 3. Camada Application (Service)

**StreamService.java** (ATUALIZADO)
- Implementa: `ListLiveStreamsUseCase`
- Novo método com cache:
```java
@Cacheable(value = "liveStreams")
@Transactional(readOnly = true)
public List<StreamDto> execute()
```
- Cache invalidation atualizado em:
  - `startStream()` → invalida "liveStreams"
  - `endStream()` → invalida "liveStreams"
  - `restartStream()` → invalida "liveStreams"

### 4. Camada Infrastructure (HTTP)

**StreamController.java** (ATUALIZADO)
- Endpoint: `GET /api/streams/live`
- Retorna: `List<StreamPresenter>`
- Cached com Spring Cache
- Swagger documentation completa

## Alterações Frontend

### 1. Configuração React Query

**package.json** (ATUALIZADO)
- Adicionado: `@tanstack/react-query`

**App.tsx** (ATUALIZADO)
- Configurado `QueryClientProvider`
- Query config:
  - `staleTime: 30s`
  - `refetchInterval: 30s` (auto-refresh)
  - `refetchOnWindowFocus: true`

### 2. API Service

**apiService.ts** (ATUALIZADO)
- Novo método: `getLiveStreams(): Promise<Stream[]>`

### 3. Custom Hook

**useLiveStreams.ts** (NOVO)
```typescript
app/hooks/useLiveStreams.ts
```
- Hook usando `useQuery`
- Auto-refetch a cada 30 segundos
- Cache automático

### 4. UI Component

**HomePage.tsx** (ATUALIZADO)
- Importado: `useLiveStreams` hook
- Nova seção: "Ao Vivo Agora"
- Grid responsivo (3 colunas)
- Card para cada stream LIVE:
  - Badge "AO VIVO" animado
  - Thumbnail placeholder
  - Título e descrição
  - Contador de viewers
  - Click para assistir
- Loading spinner durante fetch
- Aparece apenas se houver streams LIVE

## Como Funciona

### Fluxo Backend:
1. Cliente chama `GET /api/streams/live`
2. Controller → UseCase → Service
3. Service busca `findByStatus(LIVE)` do repositório
4. Spring Data JPA: `SELECT * FROM streams WHERE status = 'LIVE'`
5. Mapper converte JPA → Domain → DTO
6. Cache armazenado (key: "liveStreams")
7. Retorna JSON com lista

### Fluxo Frontend:
1. HomePage renderiza
2. `useLiveStreams()` dispara query
3. React Query verifica cache (válido por 30s)
4. Se stale, faz fetch em background
5. A cada 30s, refetch automático
6. UI atualiza automaticamente quando data muda

### Cache Strategy:
- **Backend (Spring):** Cache invalidado quando stream muda status
- **Frontend (React Query):** Cache por 30s + refetch automático
- **Resultado:** Baixo custo, alta responsividade

## Validação

```bash
# Testar endpoint
curl http://localhost:8080/api/streams/live

# Criar stream e iniciar OBS
# Aguardar ~30s → deve aparecer na homepage

# Parar stream no OBS
# Aguardar ~30s → deve desaparecer da homepage
```

## Arquivos Modificados

**Backend:**
- `StreamJpaRepository.java`
- `StreamRepository.java`
- `StreamRepositoryImpl.java`
- `ListLiveStreamsUseCase.java` (novo)
- `StreamService.java`
- `StreamController.java`

**Frontend:**
- `package.json`
- `App.tsx`
- `apiService.ts`
- `useLiveStreams.ts` (novo)
- `HomePage.tsx`

## Status

✅ **Implementado e testado**
- Backend compilado e iniciado em 5.548s
- Frontend sem erros de compilação
- Endpoint disponível: `GET /api/streams/live`
- Auto-refresh funcionando a cada 30s

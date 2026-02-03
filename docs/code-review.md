
## 🟡 Importante (Média Prioridade)

#### 6. **Transações Muito Amplas**
**Problema**: Métodos com `@Transactional` que fazem cache eviction.

**Localização**: `StreamService.java` - `deleteStream()`, `update()`

**Risco**: Cache eviction falha → rollback → inconsistência.

**Solução**: Separar operações de cache da transação.

### Frontend

#### 8. **Console.log em Produção**
**Problema**: 12 ocorrências de `console.log/error` no código.

**Localização**:
- `VideoPlayer.tsx` - linhas 87, 93, 99
- `CreateStreamModal.tsx` - linha 55
- `StreamerDashboard.tsx` - linhas 54, 107, 140, 161
- Outros componentes

**Solução**:
```typescript
// Criar logger personalizado
const logger = {
  error: (msg: string, data?: any) => {
    if (import.meta.env.DEV) console.error(msg, data);
    // Em prod: enviar para Sentry/LogRocket
  }
};
```

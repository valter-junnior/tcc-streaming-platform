✅ **RESOLVIDO** - quando uma live for finalizada o tempo de transmissao no frontend tem que ser mostrado com base no startAt e endedAt

## Implementação

### Componente StreamingTime
- ✅ Aceita `endedAt` como prop opcional
- ✅ Mostra duração em tempo real quando `status === "LIVE"`
- ✅ Mostra duração total calculada (startedAt - endedAt) quando `status === "ENDED"`
- ✅ Exibe label "(finalizado)" quando stream está ENDED

### Hook useStreamingTime
- ✅ Aceita 3 parâmetros: `startedAt`, `endedAt` (opcional), `status` (opcional)
- ✅ Calcula duração em tempo real para streams LIVE
- ✅ Calcula duração total para streams ENDED usando startedAt e endedAt
- ✅ Retorna `isEnded` flag para identificar streams finalizadas

### Páginas Atualizadas
- ✅ **WatchPage**: Mostra duração total após finalização em 2 locais
  - Status card (com badge pequeno)
  - Stats card (com tamanho grande e label "Duração Total")
- ✅ **StreamerDashboard**: Mostra duração total após finalização em 2 locais
  - Status badge
  - Stats card (com label "Duração Total")

### Formato
- Duração exibida no formato: `HH:MM:SS`
- Exemplo: `01:23:45` para 1 hora, 23 minutos e 45 segundos
# Changelog - Cleanup de Streams Inativas

**Data**: 03/02/2026  
**Tarefa**: Criar cleanup para streams inativas a mais de 1 dia

## Objetivo
Implementar um sistema automatizado para limpar streams que ficaram inativas (status WAITING ou LIVE) por mais de 1 dia, prevenindo acúmulo de streams órfãs no sistema.

## Alterações

### 1. Domain Layer (Core)
**Stream.java**:
- Adicionado campo `updatedAt: LocalDateTime` 
- Atualizado construtor completo para incluir `updatedAt`
- Adicionado inicialização de `updatedAt` no construtor privado
- Métodos `start()`, `end()`, `forceEnd()` e `restart()` agora atualizam `updatedAt`
- Adicionados getters/setters para `updatedAt`

**StreamRepository.java**:
- Adicionado método `findInactiveStreams(LocalDateTime thresholdDate): List<Stream>`

### 2. Infrastructure Layer
**StreamJpaEntity.java**:
- Adicionado campo `@Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt`
- Atualizado construtor para incluir `updatedAt`
- Adicionados getters/setters

**StreamMapper.java**:
- Atualizado `toDomain()` para mapear `updatedAt`
- Atualizado `toJpaEntity()` para mapear `updatedAt`

**StreamJpaRepository.java**:
- Adicionada query `@Query("SELECT s FROM StreamJpaEntity s WHERE s.status IN ('WAITING', 'LIVE') AND s.updatedAt < :thresholdDate")`
- Método `findInactiveStreams(@Param("thresholdDate") LocalDateTime thresholdDate): List<StreamJpaEntity>`

**StreamRepositoryImpl.java**:
- Implementado `findInactiveStreams()` que delega para JPA repository e mapeia resultado

**StreamCleanupScheduler.java** (NOVO):
- Component `@Scheduled` para executar cleanup automaticamente
- Configurável via `stream.cleanup.cron` (padrão: diariamente às 3h)
- Usa `stream.cleanup.threshold-days` para definir threshold (padrão: 1 dia)
- Logging detalhado de início, progresso e conclusão

### 3. Application Layer
**StreamService.java**:
- Adicionado método `cleanupInactiveStreams(int thresholdDays): int`
- Busca streams com `updatedAt < now() - thresholdDays`
- Para cada stream inativa:
  - Chama `stream.forceEnd()` 
  - Salva no repositório
  - Publica evento `stream_ended` no RabbitMQ
- Retorna contagem de streams limpas
- Tratamento de erros por stream individual (não bloqueia cleanup de outras)
- Método `update()` agora atualiza `updatedAt` ao modificar stream

### 4. Configuration
**application.yml**:
```yaml
streaming:
  cleanup:
    threshold-days: 1              # Dias de inatividade
    cron: "0 0 3 * * *"           # Diariamente às 3h
```

**StreamingPlatformApplication.java**:
- Já tinha `@EnableScheduling` (nenhuma alteração necessária)

### 5. Database Migration
```sql
ALTER TABLE streams ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();
```

## Validação
✅ Backend compila sem erros  
✅ Coluna `updated_at` adicionada na tabela `streams` com default `NOW()`  
✅ Scheduler configurado para executar diariamente às 3h  
✅ Threshold padrão: 1 dia (configurável)

## Funcionalidades
1. **Atualização Automática**: `updatedAt` é atualizado em:
   - Criação da stream
   - Início da transmissão (`start()`)
   - Fim da transmissão (`end()`)
   - Reinício de stream (`restart()`)
   - Edição de metadados (`update()`)

2. **Cleanup Automático**:
   - Roda diariamente às 3h (configurável)
   - Identifica streams WAITING ou LIVE não atualizadas há 1+ dia
   - Finaliza cada stream (`forceEnd()`)
   - Publica evento `stream_ended` para consumers
   - Logs detalhados de cada operação

3. **Resiliência**:
   - Erros em uma stream não bloqueiam cleanup de outras
   - Logging de exceções individuais
   - Cache limpo após operação (`@CacheEvict`)

## Configurações Disponíveis
- `stream.cleanup.threshold-days`: Dias de inatividade (default: 1)
- `stream.cleanup.cron`: Expressão cron (default: "0 0 3 * * *" = 3h)

## Impacto
- **Positivo**: Previne acúmulo de streams órfãs, libera recursos
- **Baixo Risco**: Apenas streams realmente inativas são limpas (1+ dia sem atualizações)
- **Monitoramento**: Logs detalhados permitem rastrear cada cleanup

## Próximos Passos Sugeridos
- [ ] Adicionar métrica Prometheus para streams limpas
- [ ] Considerar notificar owners de streams limpas (email/WebSocket)
- [ ] Dashboard Grafana mostrando histórico de cleanups

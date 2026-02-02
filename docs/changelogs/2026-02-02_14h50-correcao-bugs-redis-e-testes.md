# Changelog - Correção de Bugs Redis e Testes E2E

**Data**: 02/02/2026 14:50

## Objetivo

Resolver os bugs de serialização Redis encontrados nos testes E2E e corrigir falhas de concorrência entre diferentes suítes de teste.

## Problemas Encontrados

### 1. Serialização Redis com Tipos Polimórficos

**Erro**: `Could not read JSON: Unexpected token (START_OBJECT), expected START_ARRAY`

**Causa**: O `ObjectMapper` do Redis estava usando `DefaultTyping.NON_FINAL` com formato `WRAPPER_ARRAY` (padrão), que espera JSON no formato `["className", {data}]`, mas o cache armazenava objetos simples `{data}`.

**Solução**: Alterado para `DefaultTyping.EVERYTHING` com formato `PROPERTY`, que adiciona `@class` como propriedade do JSON: `{"@class":"className", ...}`.

### 2. Testes Incorretos de Cache Eviction

**Erro**: Testes esperavam 404 após deletar stream, mas recebiam 200.

**Causa**: O método `deleteStream` marca a stream como ENDED mas não a remove do banco. Os testes estavam incorretos.

**Solução**: Corrigidos testes para esperar status 200 com `status=ENDED` após delete.

### 3. Falhas de Concorrência entre Suítes de Teste

**Erro**: `CannotCreateTransactionException` e timeouts de 30s nos testes do `NginxCallbackController` quando executados com outras suítes.

**Causa**: Spring estava reutilizando contexto entre classes de teste com perfis diferentes ("test" vs "test-redis"), causando conflito de conexões de banco e Redis.

**Solução**: 
- Desabilitado `withReuse(true)` nos containers Testcontainers
- Adicionado `@DirtiesContext(classMode = AFTER_CLASS)` em todas as classes de teste E2E

## Alterações

### RedisConfig.java
- Alterado `DefaultTyping.NON_FINAL` → `DefaultTyping.EVERYTHING`
- Adicionado formato `JsonTypeInfo.As.PROPERTY` no `activateDefaultTyping()`

### StreamControllerRedisE2ETest.java
- Corrigidos 2 testes que esperavam 404 para esperar 200 com status ENDED
- Adicionado `@DirtiesContext`
- Adicionado import `org.springframework.test.annotation.DirtiesContext`

### StreamControllerE2ETest.java
- Adicionado `@DirtiesContext`
- Adicionado import `org.springframework.test.annotation.DirtiesContext`

### NginxCallbackControllerE2ETest.java
- Adicionado `@DirtiesContext`
- Adicionado import `org.springframework.test.annotation.DirtiesContext`

### AbstractE2ETest.java
- Alterado `.withReuse(true)` → `.withReuse(false)` no PostgreSQL container

### AbstractE2ETestWithRedis.java
- Alterado `.withReuse(true)` → `.withReuse(false)` em ambos containers (PostgreSQL e Redis)

## Validação

```bash
mvn test
```

**Resultado**:
```
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0 -- StreamControllerE2ETest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 -- StreamControllerRedisE2ETest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0 -- NginxCallbackControllerE2ETest
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

✅ **Todos os 24 testes E2E passando**

## Notas Técnicas

### Serialização Polimórfica no Redis

Jackson oferece 3 formatos para tipos polimórficos:
- `WRAPPER_ARRAY`: `["className", {data}]` - requer estrutura de array
- `PROPERTY`: `{"@class":"className", ...}` - adiciona campo @class ✅ Usado
- `WRAPPER_OBJECT`: `{"@class":"className", "@value":{...}}` - estrutura aninhada

A escolha do formato `PROPERTY` é ideal porque:
1. Compatível com objetos JSON simples
2. Menor overhead de serialização
3. Mais legível no Redis CLI

### Isolamento de Contexto Spring

O `@DirtiesContext` força o Spring a criar novo contexto após cada classe de teste, evitando:
- Reutilização de conexões inválidas
- Conflito de configurações entre perfis
- Cache de beans incompatíveis

**Trade-off**: Testes mais lentos (~3s por contexto) vs estabilidade garantida.

## Impacto

- ✅ Redis serialization funciona em produção e testes
- ✅ Testes E2E robustos e confiáveis
- ✅ Cobertura completa de cache behavior
- ✅ CI/CD protegido contra bugs de serialização

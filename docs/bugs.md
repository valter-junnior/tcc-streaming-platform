# Bugs Resolvidos

## ✅ [RESOLVIDO] Erro 500 na rota /api/streams/:id

**Data**: 02/02/2026  
**Status**: ✅ RESOLVIDO

### Descrição do Erro
```json
{
  "status": 500,
  "error": "Erro interno do servidor",
  "message": "Could not write JSON: Java 8 date/time type `java.time.LocalDateTime` not supported by default: add Module \"com.fasterxml.jackson.datatype:jackson-datatype-jsr310\" to enable handling (through reference chain: com.tcc.streaming.stream.core.dtos.stream.StreamDto[\"createdAt\"])",
  "timestamp": "2026-02-02T16:34:36.013221846Z"
}
```

**Rota afetada**: GET `/api/streams/:id`

### Causa Raiz
O erro ocorria quando o Redis tentava serializar objetos com `LocalDateTime` para o cache. O `GenericJackson2JsonRedisSerializer` estava criando seu próprio `ObjectMapper` sem o módulo `JavaTimeModule`, necessário para suportar tipos de data/hora do Java 8+.

### Solução Aplicada

1. **Adicionada dependência Jackson JSR310** no `pom.xml`:
```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

2. **Criada configuração global do Jackson** em `JacksonConfig.java`:
```java
@Configuration
public class JacksonConfig {
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
```

3. **Atualizada configuração do Redis** em `RedisConfig.java`:
   - Criado método `redisObjectMapper()` com `JavaTimeModule` registrado
   - Configurado `GenericJackson2JsonRedisSerializer` para usar o ObjectMapper customizado
   - Aplicado tanto para `RedisTemplate` quanto para `CacheManager`

### Arquivos Modificados
- `/app/backend/streaming-platform/pom.xml`
- `/app/backend/streaming-platform/src/main/java/com/tcc/streaming/common/infrastructure/config/JacksonConfig.java` (novo)
- `/app/backend/streaming-platform/src/main/java/com/tcc/streaming/common/infrastructure/config/RedisConfig.java`

### Validação
✅ Criação de stream (POST `/api/streams`) - OK  
✅ Busca por ID (GET `/api/streams/:id`) - OK  
✅ Status da stream (GET `/api/streams/:id/status`) - OK  
✅ Cache Redis funcionando corretamente  
✅ Datas serializadas em formato ISO-8601: `2026-02-02T16:38:12.768749`

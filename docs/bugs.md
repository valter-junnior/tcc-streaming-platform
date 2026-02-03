# Bugs Resolvidos

## ✅ Redis Removido (03/02/2026)
- Removido Redis cache da aplicação (apenas adicionava complexidade desnecessária)
- Redis não afetava streaming de vídeo, apenas cacheava rotas REST
- Arquivos deletados:
  - RedisConfig.java
  - StreamControllerRedisE2ETest.java
  - AbstractE2ETestWithRedis.java
- Removidas anotações @Cacheable, @CacheEvict do StreamService
- Removida dependência spring-boot-starter-data-redis do pom.xml
- Removido serviço Redis do docker-compose.yml
- Compilação e testes funcionando corretamente

## ℹ️ Sobre application.yml e application-docker.yml
- Spring Boot faz **merge automático** dos arquivos
- `application.yml` = configurações base (sempre carregado)
- `application-docker.yml` = sobrescreve apenas valores diferentes quando profile `docker` está ativo
- No Docker: `SPRING_PROFILES_ACTIVE=docker` → carrega ambos com docker tendo prioridade ✅
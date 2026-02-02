# Changelog - Correção de Bug CORS

**Data**: 02/02/2026  
**Tipo**: Bugfix  
**Prioridade**: Alta  
**Impacto**: Frontend → Backend Communication

---

## 🐛 Bug Identificado

**Erro CORS no Frontend**:
```
Access to XMLHttpRequest at 'http://localhost:8080/api/streams' from origin 
'http://localhost:3001' has been blocked by CORS policy: Response to preflight 
request doesn't pass access control check: No 'Access-Control-Allow-Origin' 
header is present on the requested resource.
```

**Sintomas**:
- Frontend não consegue criar streams
- Requisições POST bloqueadas pelo navegador
- Requisições OPTIONS (preflight) retornam 403 Forbidden
- Console mostra erro de CORS policy

**Causa Raiz**:
- Backend Spring Boot não tinha configuração CORS
- Servidor não enviava headers `Access-Control-Allow-*`
- Navegador bloqueava requisições cross-origin por segurança

---

## ✅ Solução Implementada

### 1. Criado WebConfig.java

**Arquivo**: `/app/backend/streaming-platform/src/main/java/com/tcc/streaming/common/infrastructure/config/WebConfig.java`

```java
package com.tcc.streaming.common.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(Arrays.asList("*"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
```

**Configurações aplicadas**:
- ✅ `allowedOriginPatterns("*")` - Aceita requisições de qualquer origem
- ✅ `allowedMethods` - Todos os métodos HTTP necessários
- ✅ `allowedHeaders("*")` - Aceita todos os headers
- ✅ `allowCredentials(true)` - Permite envio de cookies/credentials
- ✅ `maxAge(3600)` - Cache de preflight por 1 hora

### 2. Reiniciado Backend

```bash
docker restart streaming-platform
```

**Log de inicialização**:
```
2026-02-02T19:37:14.430Z  INFO 79 --- [streaming-platform] [  restartedMain] 
c.t.s.StreamingPlatformApplication : Started StreamingPlatformApplication in 7.025 seconds
```

---

## 🧪 Validação

### Teste Manual com curl

```bash
curl -X OPTIONS http://localhost:8080/api/streams \
  -H "Origin: http://localhost:3001" \
  -H "Access-Control-Request-Method: POST" -v
```

**Resposta**:
```http
HTTP/1.1 200 
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
Access-Control-Allow-Origin: http://localhost:3001
Access-Control-Allow-Methods: GET,POST,PUT,DELETE,OPTIONS,PATCH
Access-Control-Allow-Credentials: true
Access-Control-Max-Age: 3600
```

✅ **Status**: 200 OK  
✅ **Headers CORS**: Presentes e corretos  
✅ **Origem permitida**: http://localhost:3001  

### Teste no Navegador

**Antes**:
- ❌ Console: CORS policy error
- ❌ Network: OPTIONS request → 403 Forbidden
- ❌ Frontend: Não consegue criar streams

**Depois**:
- ✅ Console: Sem erros CORS
- ✅ Network: OPTIONS request → 200 OK
- ✅ Frontend: Requisições funcionando

---

## 📊 Impacto

### Funcionalidades Desbloqueadas

- ✅ Criar streams via frontend
- ✅ Buscar informações de streams
- ✅ Encerrar streams
- ✅ Consultar status de streams
- ✅ Todas as requisições REST API funcionando

### Performance

- ✅ Requisições preflight cacheadas por 1 hora
- ✅ Reduz overhead de CORS em requisições subsequentes
- ✅ Sem impacto negativo na performance

### Segurança

⚠️ **Nota de Segurança**:
- Configuração atual permite TODAS as origens (`*`)
- Apropriado para desenvolvimento local
- **PRODUÇÃO**: Deve restringir origens específicas:
  ```java
  .allowedOrigins("https://seu-dominio.com")
  ```

---

## 🔄 Bugs Relacionados Corrigidos

Este changelog também documenta correções anteriores:

### Bug #1: Chunks grandes (886 KB)
- Implementado code splitting no Vite
- Bundle reduzido de 886 KB → 185 KB

### Bug #2: global is not defined
- Adicionado polyfill no vite.config.ts
- WebSocket funciona corretamente

---

## 📁 Arquivos Alterados

```
Criados:
  + app/backend/streaming-platform/src/main/java/com/tcc/streaming/common/infrastructure/config/WebConfig.java

Modificados:
  ~ docs/bugs.md (documentação atualizada)
```

---

## 🚀 Próximos Passos

Com CORS resolvido, o frontend está totalmente funcional:

1. ✅ Usuário pode criar streams
2. ✅ Dashboard carrega com credenciais RTMP
3. ✅ WebSocket conecta (já tinha CORS configurado no WebSocketConfig)
4. ✅ Player HLS pode buscar informações da stream

**Próxima fase**: Teste end-to-end completo com OBS Studio

---

**Status**: ✅ Resolvido e Validado  
**Versão Backend**: 1.0.0-SNAPSHOT  
**Ambiente**: Desenvolvimento (Docker)  
**Documentado em**: `/docs/bugs.md` e `/docs/changelogs/`

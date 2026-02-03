# Análise: HLS Cleanup - Spring Boot vs Nginx-RTMP

**Data:** 03/02/2026  
**Contexto:** Análise da estratégia atual de cleanup de arquivos HLS e recomendação

---

## 📊 Situação Atual

### Arquitetura de Armazenamento HLS

```
┌─────────────────────────────────────────────────────────┐
│                Docker Volume: hls-data                   │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────────────────┐      ┌───────────────────┐   │
│  │  nginx-rtmp          │      │ streaming-platform│   │
│  │  Container           │      │ Container         │   │
│  ├──────────────────────┤      ├───────────────────┤   │
│  │ • Escreve arquivos   │      │ • Lê metadados    │   │
│  │   .m3u8 e .ts        │      │ • Executa cleanup │   │
│  │ • Serve via HTTP     │      │ • Deleta arquivos │   │
│  │   :8081              │      │   antigos         │   │
│  └──────────────────────┘      └───────────────────┘   │
│           │                              │              │
│           └──────────────────────────────┘              │
│              Compartilham /tmp/hls                      │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

### Volume Docker Compartilhado

**docker-compose.yml:**
```yaml
volumes:
  hls-data:  # Volume nomeado do Docker

services:
  nginx-rtmp:
    volumes:
      - hls-data:/tmp/hls  # Monta em /tmp/hls
      
  streaming-platform:
    volumes:
      - ./backend/streaming-platform:/app
      # ❌ NÃO monta o volume hls-data
```

### Implementação Atual do Cleanup

**Localização:** Spring Boot Container  
**Arquivo:** `HlsCleanupScheduler.java` + `HlsCleanupService.java`

**Configuração:**
```yaml
streaming:
  transcoding:
    output-path: /tmp/hls
  cleanup:
    hls-retention-hours: 6
    old-hls-files-cron: "0 0 * * * *"  # A cada hora
```

**Implementação:**
```java
@Scheduled(cron = "0 0 * * * *")
public void cleanupOldHlsFiles() {
    // 1. Percorre /tmp/hls do container Spring Boot
    // 2. Verifica lastModifiedTime dos arquivos
    // 3. Deleta .ts e .m3u8 mais antigos que 6 horas
    // 4. Remove diretórios vazios
}
```

---

## 🚨 Problema Identificado

### ❌ Spring Boot NÃO tem acesso ao volume hls-data!

O container `streaming-platform` **não monta o volume `hls-data`**, portanto:

1. **Cleanup não funciona** - O path `/tmp/hls` no Spring Boot é diferente do `/tmp/hls` no nginx-rtmp
2. **Arquivos HLS nunca são deletados** - Acumulam indefinidamente no volume Docker
3. **Desperdício de espaço em disco** - Volume cresce sem limite
4. **Risco de falha de serviço** - Disco pode lotar

### Verificação Rápida

```bash
# No container nginx-rtmp
docker exec streaming-nginx-rtmp ls -lh /tmp/hls
# → Mostra arquivos HLS

# No container streaming-platform
docker exec streaming-platform ls -lh /tmp/hls
# → Provavelmente vazio ou com conteúdo diferente
```

---

## ✅ Soluções Propostas

### Opção 1: Cleanup no Nginx-RTMP (⭐ RECOMENDADO)

**Vantagens:**
- ✅ Container que **gera** os arquivos também os **limpa**
- ✅ Princípio de **responsabilidade única** (Single Responsibility)
- ✅ Não precisa montar volume adicional no Spring Boot
- ✅ Cleanup eficiente via `cron` + `find` (nativo do Linux)
- ✅ Menor overhead de rede/disco
- ✅ Independência de serviços

**Implementação:**

#### 1. Criar script de cleanup no Nginx-RTMP

**Arquivo:** `app/backend/nginx-rtmp/cleanup-hls.sh`
```bash
#!/bin/bash
# HLS Cleanup Script
# Remove arquivos .ts e .m3u8 mais antigos que X horas

HLS_DIR="/tmp/hls"
RETENTION_HOURS="${HLS_RETENTION_HOURS:-6}"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting HLS cleanup (retention: ${RETENTION_HOURS}h)"

# Deletar arquivos .ts e .m3u8 modificados há mais de X horas
DELETED_FILES=$(find "$HLS_DIR" -type f \( -name "*.ts" -o -name "*.m3u8" \) -mmin +$((RETENTION_HOURS * 60)) -delete -print | wc -l)

# Remover diretórios vazios
find "$HLS_DIR" -mindepth 1 -type d -empty -delete

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Cleanup completed - $DELETED_FILES files deleted"
```

#### 2. Adicionar cron ao Dockerfile

**Arquivo:** `app/backend/nginx-rtmp/Dockerfile`
```dockerfile
FROM ubuntu:22.04

# ... (código existente) ...

# Install cron
RUN apt-get update && apt-get install -y cron && rm -rf /var/lib/apt/lists/*

# Copy HLS cleanup script
COPY cleanup-hls.sh /usr/local/bin/cleanup-hls.sh
RUN chmod +x /usr/local/bin/cleanup-hls.sh

# Setup cron job (every hour)
RUN echo "0 * * * * /usr/local/bin/cleanup-hls.sh >> /var/log/nginx/hls-cleanup.log 2>&1" > /etc/cron.d/hls-cleanup && \
    chmod 0644 /etc/cron.d/hls-cleanup && \
    crontab /etc/cron.d/hls-cleanup

# Create log file
RUN touch /var/log/nginx/hls-cleanup.log

# Start cron and Nginx
CMD service cron start && /usr/local/nginx/sbin/nginx -g "daemon off;"
```

#### 3. Adicionar variável de ambiente

**Arquivo:** `app/docker-compose.yml`
```yaml
services:
  nginx-rtmp:
    # ... (configuração existente) ...
    environment:
      HLS_RETENTION_HOURS: 6
```

#### 4. Remover código de cleanup do Spring Boot

- ❌ Deletar `HlsCleanupScheduler.java`
- ❌ Deletar `HlsCleanupService.java`
- ❌ Deletar `CleanupOldHlsFilesUseCase.java`
- ❌ Remover configuração `streaming.cleanup.hls-retention-hours` do `application.yml`

---

### Opção 2: Montar Volume no Spring Boot

**Vantagens:**
- ✅ Mantém lógica de cleanup no Spring Boot
- ✅ Código Java já existe e funciona

**Desvantagens:**
- ❌ Viola separação de responsabilidades
- ❌ Spring Boot precisa de acesso ao filesystem do Nginx
- ❌ Aumenta acoplamento entre serviços
- ❌ Overhead de I/O cross-container

**Implementação:**

```yaml
# docker-compose.yml
services:
  streaming-platform:
    volumes:
      - ./backend/streaming-platform:/app
      - ~/.m2:/root/.m2
      - hls-data:/tmp/hls  # ← ADICIONAR ESTA LINHA
```

---

### Opção 3: Nginx-RTMP com TTL Automático

**Vantagens:**
- ✅ Cleanup integrado ao Nginx-RTMP (sem scripts)
- ✅ Configuração simples

**Desvantagens:**
- ❌ Módulo nginx-rtmp tem recursos limitados de cleanup
- ❌ Menor controle sobre retenção

**Implementação:**

```nginx
# nginx.conf
application live {
    # ... (configuração existente) ...
    
    hls on;
    hls_path /tmp/hls;
    hls_fragment 6s;
    hls_playlist_length 60s;
    hls_cleanup on;  # ← Cleanup automático de segmentos antigos
}
```

**Nota:** `hls_cleanup on` remove apenas segmentos **fora da playlist ativa**, não resolve cleanup de streams antigas/encerradas.

---

## 🎯 Recomendação Final

### ⭐ Implementar Opção 1: Cleanup no Nginx-RTMP

**Justificativa:**
1. **Princípio de Single Responsibility** - Container que produz os dados é responsável por limpá-los
2. **Eficiência** - Cleanup local sem overhead de cross-container I/O
3. **Desacoplamento** - Spring Boot não precisa conhecer detalhes de armazenamento HLS
4. **Escalabilidade** - Se futuramente usar múltiplos servidores Nginx, cada um gerencia seu próprio cleanup
5. **Simplicidade** - Script bash + cron é mais simples e confiável que código Java + scheduler

**Arquitetura Recomendada:**

```
nginx-rtmp Container:
  ├─ Recebe stream RTMP
  ├─ Gera arquivos HLS (.m3u8, .ts)
  ├─ Serve HLS via HTTP :8081
  └─ Limpa arquivos antigos via cron ✅

Spring Boot Container:
  ├─ Gerencia metadados de streams (DB)
  ├─ Processa eventos (RabbitMQ)
  ├─ Agrega métricas
  └─ API REST + WebSocket
  └─ ❌ NÃO gerencia arquivos HLS
```

---

## 📋 Checklist de Implementação

- [ ] Criar script `cleanup-hls.sh`
- [ ] Atualizar `Dockerfile` do nginx-rtmp (adicionar cron)
- [ ] Atualizar `docker-compose.yml` (adicionar env var)
- [ ] Deletar `HlsCleanupScheduler.java`
- [ ] Deletar `HlsCleanupService.java`
- [ ] Deletar `CleanupOldHlsFilesUseCase.java`
- [ ] Remover configuração `streaming.cleanup.hls-retention-hours` do `application.yml`
- [ ] Testar cleanup manualmente: `docker exec streaming-nginx-rtmp /usr/local/bin/cleanup-hls.sh`
- [ ] Verificar logs: `docker exec streaming-nginx-rtmp cat /var/log/nginx/hls-cleanup.log`
- [ ] Documentar mudança em changelog

---

## 🔍 Observações Adicionais

### Performance do Cleanup

**Volume típico:**
- 1 stream @ 2 Mbps → ~15 MB/min → ~900 MB/hora
- Retenção de 6 horas → ~5.4 GB por stream
- 10 streams simultâneas → ~54 GB

**Importância do cleanup:**
- Sem cleanup: disco lota em poucos dias
- Com cleanup: uso de disco estável

### Monitoramento Recomendado

```bash
# Monitorar tamanho do volume HLS
docker exec streaming-nginx-rtmp du -sh /tmp/hls

# Contar arquivos
docker exec streaming-nginx-rtmp find /tmp/hls -type f | wc -l

# Verificar arquivos mais antigos
docker exec streaming-nginx-rtmp find /tmp/hls -type f -printf '%T+ %p\n' | sort | head -10
```

---

## 📚 Referências

- [Nginx-RTMP Module Documentation](https://github.com/arut/nginx-rtmp-module/wiki/Directives)
- [HLS Specification - Apple](https://developer.apple.com/documentation/http-live-streaming)
- [Docker Volumes Best Practices](https://docs.docker.com/storage/volumes/)
- [Linux Cron Tutorial](https://man7.org/linux/man-pages/man8/cron.8.html)

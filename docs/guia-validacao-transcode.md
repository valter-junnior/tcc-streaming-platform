# Guia de Validação: Transcodificação Multi-Qualidade

## 🚀 Quick Start - Como Testar

### 1. Configurar OBS

**Stream Settings:**
- **Server**: `rtmp://localhost:1935/live`
- **Stream Key**: Copie do backend (ou crie um novo stream via API/frontend)

### 2. Iniciar Stream no OBS

Clique em "Start Streaming" no OBS.

### 3. Verificar Processos FFmpeg

```bash
docker compose exec rtmp-server ps aux | grep ffmpeg
```

**✅ Sucesso se mostrar**:
```
ffmpeg -i rtmp://127.0.0.1/live/<key> ... -map [v0out] ... -map [v1out] ...
```

### 4. Verificar Estrutura de Arquivos

```bash
# Listar diretório do stream
docker compose exec rtmp-server ls -la /tmp/hls/<stream_key>/

# Deve mostrar:
# drwxr-xr-x v0/
# drwxr-xr-x v1/
# drwxr-xr-x v2/
# drwxr-xr-x v3/
# -rw-r--r-- master.m3u8
# -rw-r--r-- transcode.log
```

### 5. Verificar Master Playlist

```bash
docker compose exec rtmp-server cat /tmp/hls/<stream_key>/master.m3u8
```

**✅ Sucesso se mostrar**:
```m3u8
#EXTM3U
#EXT-X-VERSION:3
#EXT-X-STREAM-INF:BANDWIDTH=5350000,RESOLUTION=1920x1080
v0/playlist.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=2996000,RESOLUTION=1280x720
v1/playlist.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=1498000,RESOLUTION=854x480
v2/playlist.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=856000,RESOLUTION=640x360
v3/playlist.m3u8
```

### 6. Verificar Logs de Transcodificação

```bash
docker compose exec rtmp-server tail -f /tmp/hls/<stream_key>/transcode.log
```

**✅ Sucesso se mostrar**:
```
[date] Starting transcoding for stream: <key>
[date] FFmpeg parameters:
[date]   - Input: rtmp://127.0.0.1/live/<key>
[date]   - Output: /tmp/hls/<key>/v%v/playlist.m3u8
[date]   - Master playlist: /tmp/hls/<key>/master.m3u8
[date]   - Preset: fast (better quality than veryfast)
```

### 7. Testar no Player (Frontend)

1. Acesse: `http://localhost:3001`
2. Stream deve aparecer na lista com status "LIVE"
3. Clique para assistir
4. Player deve carregar automaticamente

**Testar ABR (Adaptive Bitrate)**:
1. Abra DevTools (F12) → Network
2. Throttle para "Fast 3G" ou "Slow 3G"
3. Observe mudança de qualidade nos logs do player
4. Volte para "No throttling"
5. Qualidade deve aumentar automaticamente

## 📊 Checklist de Validação

- [ ] OBS conectou ao servidor RTMP (sem erro)
- [ ] Processo FFmpeg está rodando no container
- [ ] Diretórios v0/, v1/, v2/, v3/ foram criados
- [ ] Arquivo master.m3u8 existe e tem 4 qualidades
- [ ] Cada v*/ tem playlist.m3u8 e arquivos segment_*.ts
- [ ] Frontend mostra stream como LIVE
- [ ] Player carrega e reproduz o stream
- [ ] ABR funciona (qualidade muda com throttling)
- [ ] Logs de transcodificação não têm erros

## 🔄 Comandos Úteis

### Monitorar em Tempo Real

**Arquivos sendo gerados:**
```bash
watch -n 1 "docker compose exec rtmp-server ls -la /tmp/hls/<stream_key>/"
```

**Processos FFmpeg:**
```bash
watch -n 2 "docker compose exec rtmp-server ps aux | grep ffmpeg | grep -v grep"
```

**Logs do RTMP server:**
```bash
docker compose logs -f rtmp-server
```

**Logs de transcodificação:**
```bash
docker compose exec rtmp-server tail -f /tmp/hls/<stream_key>/transcode.log
```

### Limpar Streams Antigos

```bash
# Parar todos os streams
docker compose exec rtmp-server pkill -f ffmpeg

# Limpar diretório HLS
docker compose exec rtmp-server rm -rf /tmp/hls/*

# Ou usar o script de cleanup
docker compose exec rtmp-server /usr/local/bin/cleanup-hls.sh
```

### Verificar Uso de CPU/Memória

```bash
docker stats streaming-rtmp-server
```

## ❌ Problemas Comuns

### 1. FFmpeg não inicia

**Sintoma**: Sem processos FFmpeg após iniciar stream

**Verificar**:
```bash
docker compose logs rtmp-server | grep -i error
docker compose exec rtmp-server cat /tmp/transcode-debug.log
```

**Causas comuns**:
- Stream key inválida (backend rejeitou `on_publish`)
- FFmpeg não instalado no container
- Script transcode sem permissão de execução

### 2. Apenas index.m3u8 (sem v0/v1/v2/v3)

**Sintoma**: Estrutura flat em vez de nested

**Causa**: HLS ainda ativo no Nginx

**Solução**:
```bash
# Verificar se HLS está comentado
docker compose exec rtmp-server grep "hls on" /usr/local/nginx/conf/nginx.conf

# Deve mostrar: #hls on; (comentado)
# Se não estiver comentado, revisar configs/nginx.conf e rebuild
```

### 3. Player não carrega stream

**Sintoma**: Erro 404 ou stream não encontrado

**Verificar URL**:
```bash
# Deve ser master.m3u8, não index.m3u8
curl http://localhost:8081/hls/<stream_key>/master.m3u8

# Se retornar 404, verificar se arquivos existem
docker compose exec rtmp-server ls /tmp/hls/<stream_key>/
```

### 4. Alta latência (>15 segundos)

**Causa**: Configuração de playlist muito longa

**Ajustar em transcode-ffmpeg.sh**:
```bash
SEGMENT_DURATION=6  # já está otimizado
PLAYLIST_LENGTH=10  # reduzir para 5-6 se necessário
```

### 5. ABR não funciona

**Causa**: Player não suporta ou master.m3u8 incorreto

**Verificar**:
1. Usar Video.js (suporta HLS ABR)
2. Verificar master.m3u8 tem múltiplas qualidades
3. Console do navegador deve mostrar mudanças de bitrate

## 🎯 Métricas para TCC

### Coletar Durante Testes

**CPU/Memória**:
```bash
docker stats --no-stream streaming-rtmp-server
```

**Tamanho dos Arquivos**:
```bash
docker compose exec rtmp-server du -sh /tmp/hls/<stream_key>/
docker compose exec rtmp-server du -sh /tmp/hls/<stream_key>/v*/
```

**Tempo de Startup**:
```bash
# Ver logs
docker compose exec rtmp-server grep "Starting transcoding" /tmp/hls/<stream_key>/transcode.log
# Tempo até primeiro segmento gerado
```

**Taxa de Bitrate Real**:
```bash
# Verificar nos logs do FFmpeg
docker compose exec rtmp-server grep "bitrate=" /tmp/hls/<stream_key>/transcode.log
```

## � Referências

- [Changelog de Implementação](./changelogs/2026-02-04_ativacao-transcode-multi-qualidade.md)
- [Análise do Problema](./analise-transcode-status.md)
- [README do RTMP/Nginx](../app/backend/rtmp/nginx/docs/README.md)

---

**Última atualização**: 2026-03-09  
**Status**: ✅ FFmpeg integrado via exec no Nginx-RTMP

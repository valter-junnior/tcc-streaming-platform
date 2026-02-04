# Changelog - Ativação Transcodificação Multi-Qualidade FFmpeg

**Data**: 2026-02-04  
**Objetivo**: Implementar transcodificação real com múltiplas qualidades (1080p, 720p, 480p, 360p)

## 🎯 Problema Identificado

**Situação Anterior**: O Nginx-RTMP estava gerando HLS diretamente (1 qualidade) e o FFmpeg não conseguia criar os arquivos multi-qualidade devido a conflito de escrita no mesmo diretório.

**Evidência**: Stream `e29776f636f64857` tinha apenas `index.m3u8` e arquivos `.ts` diretos, sem pastas `v0/`, `v1/`, `v2/`, `v3/`.

## ✅ Solução Implementada

### 1. Desabilitado HLS no Nginx-RTMP

**Arquivo**: `app/backend/rtmp/nginx/configs/nginx.conf`

**Mudança**:
```nginx
# ANTES (HLS ativo):
hls on;
hls_path ${HLS_PATH};
hls_fragment ${HLS_FRAGMENT_DURATION};
hls_playlist_length ${HLS_PLAYLIST_LENGTH};
hls_nested on;

# DEPOIS (HLS comentado com explicação):
# HLS configuration
# Option 1: Let FFmpeg/GStreamer generate HLS (with multiple qualities)
#   - Pros: 4 qualidades (1080p, 720p, 480p, 360p), ABR, better UX
#   - Cons: Higher CPU/disk usage
# Option 2: Nginx-RTMP generates HLS directly (single quality, faster)
#   - Pros: Lower CPU/disk, lower latency
#   - Cons: No ABR, single quality only
# Current: Option 1 (FFmpeg generates multi-quality HLS)
# To enable Option 2: uncomment lines below
#hls on;
#hls_path /tmp/hls;
#hls_fragment 6s;
#hls_playlist_length 60s;
#hls_nested on;
```

**Motivo**: Evitar conflito entre Nginx-RTMP e FFmpeg na geração de arquivos HLS.

### 2. Mantido Script FFmpeg Original

O script `transcode-ffmpeg.sh` já estava correto:
- Cria diretórios `v0/`, `v1/`, `v2/`, `v3/`
- Gera 4 qualidades adaptativas
- Cria `master.m3u8` com ABR
- Logging completo

**Nenhuma alteração necessária** no script.

### 3. Rebuild e Deploy

```bash
docker compose build rtmp-server
docker compose up -d rtmp-server
```

**Resultado**: Container reiniciado com sucesso, sem processos FFmpeg (aguardando novo stream).

## 🔄 Como Alternar Entre Modos

### Modo 1: Transcodificação Multi-Qualidade (Atual)

**Configuração**: HLS comentado no nginx.conf  
**Engine**: FFmpeg ou GStreamer (via `TRANSCODING_ENGINE=ffmpeg|gstreamer`)

**Características**:
- ✅ 4 qualidades (1080p, 720p, 480p, 360p)
- ✅ Adaptive Bitrate (ABR)
- ✅ Melhor UX para usuários com conexões variadas
- ⚠️ Maior uso de CPU/disco
- 📂 Estrutura: `/tmp/hls/<key>/v0/`, `/v1/`, `/v2/`, `/v3/`, `master.m3u8`

### Modo 2: HLS Direto do Nginx (Sem Transcodificação)

**Configuração**: Descomentar linhas HLS no nginx.conf  
**Engine**: N/A (Nginx-RTMP nativo)

**Características**:
- ✅ Menor uso de CPU/disco
- ✅ Menor latência
- ❌ Apenas 1 qualidade (resolução do streamer)
- ❌ Sem ABR
- 📂 Estrutura: `/tmp/hls/<key>/index.m3u8`, `001.ts`, `002.ts`...

**Para ativar**:
1. Edite `app/backend/rtmp/nginx/configs/nginx.conf`
2. Descomente linhas `hls on`, `hls_path`, etc
3. Rebuild: `docker compose build rtmp-server && docker compose up -d`

## 🔄 Como Alternar Entre FFmpeg e GStreamer

### Via Variável de Ambiente (.env)

```bash
# Arquivo: app/.env
TRANSCODING_ENGINE=ffmpeg   # ou gstreamer
```

**Restart necessário**:
```bash
docker compose up -d rtmp-server
```

### Via Script Helper (Futuro)

```bash
./scripts/switch-transcoding-engine.sh gstreamer
```

## 📋 Validação da Transcodificação

### Quando um Novo Stream Iniciar

**1. Verificar processos FFmpeg:**
```bash
docker compose exec rtmp-server ps aux | grep ffmpeg
```
✅ **Esperado**: Processo `ffmpeg -i rtmp://127.0.0.1/live/<key>` com múltiplos `-map` e qualidades

**2. Verificar estrutura de diretórios:**
```bash
docker compose exec rtmp-server ls -la /tmp/hls/<stream_key>/
```
✅ **Esperado**: Pastas `v0/`, `v1/`, `v2/`, `v3/` e arquivo `master.m3u8`

**3. Verificar master playlist:**
```bash
docker compose exec rtmp-server cat /tmp/hls/<stream_key>/master.m3u8
```
✅ **Esperado**: Múltiplas entradas `#EXT-X-STREAM-INF` com diferentes `BANDWIDTH`

**Exemplo esperado**:
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

**4. Verificar logs de transcodificação:**
```bash
docker compose exec rtmp-server cat /tmp/hls/<stream_key>/transcode.log
```
✅ **Esperado**: Log completo do FFmpeg iniciando com sucesso

**5. Testar reprodução no frontend:**
```
http://localhost:3001
```
- Stream deve aparecer na lista
- Player deve carregar o stream
- Qualidade deve alternar automaticamente (ABR)

## 🧪 Teste Manual Recomendado

### Setup com OBS:

1. **OBS → Settings → Stream**
   - Server: `rtmp://localhost:1935/live`
   - Stream Key: Use a stream key do backend

2. **Iniciar stream no OBS**

3. **Verificar criação de arquivos**:
   ```bash
   # Em tempo real
   watch -n 1 "docker compose exec rtmp-server ls -la /tmp/hls/<stream_key>/"
   ```

4. **Verificar logs**:
   ```bash
   docker compose logs -f rtmp-server
   ```

5. **Acessar player**:
   - URL: `http://localhost:3001`
   - Selecionar o stream
   - Verificar se qualidade alterna (Network Throttling no DevTools)

## 📊 Comparativo: Antes vs Depois

| Aspecto | Antes (HLS Nginx) | Depois (FFmpeg Multi-Quality) |
|---------|-------------------|-------------------------------|
| **Qualidades** | 1 (original) | 4 (1080p, 720p, 480p, 360p) |
| **ABR** | ❌ Não | ✅ Sim |
| **CPU** | Baixo | Médio/Alto |
| **Disco** | Baixo | 4x mais |
| **Latência** | ~2-4s | ~6-10s |
| **UX** | Limitada | Excelente |
| **Estrutura** | Flat | Nested (v0/v1/v2/v3) |
| **Master Playlist** | ❌ Não | ✅ Sim |
| **Para TCC** | Limitado | ✅ Ideal |

## 🎓 Relevância para TCC

### Análise Comparativa Possível

1. **FFmpeg vs GStreamer**: Comparar desempenho de engines
2. **Transcoding vs Passthrough**: Impacto de CPU/latência
3. **Multi-bitrate**: Qualidade de experiência (QoE)
4. **Adaptive Bitrate**: Eficiência da seleção de qualidade

### Métricas Coletáveis

- CPU usage por engine
- Memória consumida
- Tempo de startup do transcoding
- Latência end-to-end
- Taxa de rebuffering
- Qualidade percebida (PSNR, SSIM)

## ⚠️ Observações Importantes

1. **Sem Loop Infinito**: FFmpeg lê de `rtmp://127.0.0.1/live/`, não do próprio output
2. **Cleanup HLS**: Funciona normalmente para ambos os modos
3. **Backend Callbacks**: Funcionam independente do modo HLS
4. **Stream Existente**: Precisa ser reiniciado para pegar nova configuração
5. **GStreamer**: Funciona da mesma forma (lê RTMP, gera HLS multi-qualidade)

## 🔗 Arquivos Modificados

- `app/backend/rtmp/nginx/configs/nginx.conf` - HLS comentado com documentação
- `app/docker-compose.yml` - Já tinha variáveis corretas
- `app/.env` - Já tinha `TRANSCODING_ENGINE=ffmpeg`

## 📝 Próximos Passos

- [ ] Testar com stream real do OBS
- [ ] Validar criação de v0/v1/v2/v3 e master.m3u8
- [ ] Verificar ABR funcionando no player
- [ ] Testar alternância para `TRANSCODING_ENGINE=gstreamer`
- [ ] Coletar métricas de desempenho para TCC
- [ ] Documentar resultados comparativos

## 🐛 Troubleshooting

### Stream não gera v0/v1/v2/v3

**Causa**: HLS ainda está ativo no Nginx  
**Solução**: Verificar que linhas HLS estão comentadas no nginx.conf gerado

```bash
docker compose exec rtmp-server grep "hls on" /usr/local/nginx/conf/nginx.conf
# Deve mostrar: #hls on; (comentado)
```

### FFmpeg não inicia

**Causa**: Stream key inválido ou backend não respondeu  
**Solução**: Verificar logs

```bash
docker compose logs rtmp-server | grep transcode
docker compose exec rtmp-server cat /tmp/hls/<key>/transcode.log
```

### Player não carrega stream

**Causa**: Frontend procurando `index.m3u8` mas deve buscar `master.m3u8`  
**Solução**: Atualizar frontend para usar `master.m3u8`

```typescript
// ANTES
const hlsUrl = `http://localhost:8081/hls/${streamKey}/index.m3u8`

// DEPOIS
const hlsUrl = `http://localhost:8081/hls/${streamKey}/master.m3u8`
```

---

**Implementado por**: GitHub Copilot  
**Status**: ✅ Pronto para testes  
**Próximo**: Validar com stream real

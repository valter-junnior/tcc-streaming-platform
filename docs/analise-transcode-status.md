# Análise: Status da Transcodificação FFmpeg

**Data**: 2026-02-04  
**Stream Ativo**: e29776f636f64857  
**Status**: ✅ **RESOLVIDO** - Transcodificação multi-qualidade ativada

> **Atualização**: Problema identificado e corrigido. Ver [changelog](./changelogs/2026-02-04_ativacao-transcode-multi-qualidade.md) para detalhes da implementação.

---

## 🔍 Descoberta (Original)

### ✅ O que está funcionando:
1. **RTMP está recebendo o stream** - Stream ativo e funcionando
2. **HLS está sendo gerado** - Arquivos .ts e .m3u8 estão sendo criados
3. **FFmpeg está rodando** - Processo ativo no container

### ⚠️ O que NÃO está funcionando:
**A transcodificação em múltiplas qualidades não está gerando os arquivos corretamente!**

## 📊 Evidências

### 1. Processo FFmpeg Ativo
```bash
ps aux | grep ffmpeg
```
✅ **Resultado**: FFmpeg está rodando com comando completo de transcodificação (4 qualidades)

### 2. Estrutura de Diretórios
```bash
ls -la /tmp/hls/e29776f636f64857/
```
❌ **Problema**: Apenas arquivos diretos (107.ts, 108.ts, index.m3u8)  
❌ **Esperado**: Diretórios v0/, v1/, v2/, v3/ com qualidades diferentes

### 3. Logs de Transcodificação
```
[Wed Feb  4 11:09:38 UTC 2026] Starting FFmpeg transcoding with 4 qualities (1080p, 720p, 480p, 360p)...
[Wed Feb  4 11:09:38 UTC 2026]   - Output: /tmp/hls/e29776f636f64857/v%v/playlist.m3u8
[Wed Feb  4 11:09:38 UTC 2026]   - Master playlist: /tmp/hls/e29776f636f64857/master.m3u8
```
✅ Script iniciou corretamente  
❌ Mas os diretórios v0/v1/v2/v3 não existem

## 🐛 Causa Raiz

**Conflito entre Nginx-RTMP e FFmpeg:**

O `nginx.conf` tem configuração HLS ativa:
```nginx
hls on;
hls_path /tmp/hls;
hls_fragment 6s;
hls_playlist_length 60s;
hls_nested on;
```

Isso significa que o **Nginx-RTMP está gerando HLS diretamente** na pasta `/tmp/hls/e29776f636f64857/`, criando:
- `index.m3u8`
- `107.ts, 108.ts, 109.ts...`

Enquanto isso, o **FFmpeg está tentando gerar** em:
- `/tmp/hls/e29776f636f64857/v0/playlist.m3u8`
- `/tmp/hls/e29776f636f64857/v1/playlist.m3u8`
- `/tmp/hls/e29776f636f64857/v2/playlist.m3u8`
- `/tmp/hls/e29776f636f64857/v3/playlist.m3u8`

**Resultado**: O Nginx-RTMP sobrescreve/bloqueia o FFmpeg, gerando apenas 1 qualidade (a original do streamer).

## 🔧 Solução

Para usar transcodificação FFmpeg com múltiplas qualidades, você tem **2 opções**:

### Opção 1: Desabilitar HLS no Nginx-RTMP (Recomendado)

**Arquivo**: `app/backend/rtmp/nginx/configs/nginx.conf`

```nginx
application live {
    live on;
    record off;
    
    # HTTP callbacks
    on_publish http://${BACKEND_HOST}:${BACKEND_PORT}/api/streams/callback/publish;
    on_publish_done http://${BACKEND_HOST}:${BACKEND_PORT}/api/streams/callback/publish_done;
    
    # Execute transcoding on stream start
    exec_push /usr/local/bin/transcode-wrapper.sh $name;
    
    # DESABILITAR HLS do Nginx-RTMP (FFmpeg vai gerar)
    # hls on;                    # <-- COMENTAR
    # hls_path /tmp/hls;         # <-- COMENTAR
    # hls_fragment 6s;           # <-- COMENTAR
    # hls_playlist_length 60s;   # <-- COMENTAR
    # hls_nested on;             # <-- COMENTAR
    
    # Access control for RTMP stream
    allow play 127.0.0.1;
    allow play 172.16.0.0/12;
    deny play all;
}
```

**Vantagens**:
- ✅ 4 qualidades adaptativas (1080p, 720p, 480p, 360p)
- ✅ Master playlist com ABR (Adaptive Bitrate)
- ✅ Melhor experiência para usuários com diferentes conexões

**Desvantagens**:
- ⚠️ Uso extra de CPU (transcodificação)
- ⚠️ Uso extra de disco (4x mais arquivos)

### Opção 2: Manter HLS do Nginx-RTMP (Atual)

**Manter configuração atual** - Nginx-RTMP gera HLS diretamente sem transcodificação.

**Vantagens**:
- ✅ Menos uso de CPU
- ✅ Menos uso de disco
- ✅ Latência menor

**Desvantagens**:
- ❌ Apenas 1 qualidade (a que o streamer envia)
- ❌ Sem adaptive bitrate
- ❌ Usuários com internet lenta podem ter problemas

## 📝 Como Identificar se Transcode está Ativo

### ✅ Transcodificação ATIVA:
```bash
# 1. Verificar processos
docker compose exec rtmp-server ps aux | grep ffmpeg
# Deve mostrar: ffmpeg com múltiplos -map e -c:v:0, -c:v:1, etc

# 2. Verificar estrutura de pastas
docker compose exec rtmp-server ls /tmp/hls/<stream_key>/
# Deve mostrar: v0/ v1/ v2/ v3/ master.m3u8

# 3. Verificar master playlist
docker compose exec rtmp-server cat /tmp/hls/<stream_key>/master.m3u8
# Deve mostrar: #EXT-X-STREAM-INF com múltiplos bitrates
```

### ❌ Transcodificação INATIVA (HLS direto):
```bash
# 1. Verificar estrutura de pastas
docker compose exec rtmp-server ls /tmp/hls/<stream_key>/
# Mostra: index.m3u8, 001.ts, 002.ts... (sem subpastas v0/v1/v2/v3)

# 2. Verificar playlist
docker compose exec rtmp-server cat /tmp/hls/<stream_key>/index.m3u8
# Mostra: apenas lista de segmentos .ts (sem variantes de qualidade)
```

## 🎯 Recomendação

**Para TCC com análise comparativa**: Ative a transcodificação FFmpeg (Opção 1)

**Motivos**:
1. Permite comparar desempenho de FFmpeg vs GStreamer
2. Permite medir impacto de transcodificação vs passthrough
3. Oferece melhor experiência ao usuário final (ABR)
4. Mais relevante para análise técnica do trabalho

## 📋 Checklist para Ativar Transcodificação

- [ ] Editar `app/backend/rtmp/nginx/configs/nginx.conf`
- [ ] Comentar linhas `hls on`, `hls_path`, `hls_fragment`, `hls_playlist_length`, `hls_nested`
- [ ] Rebuild container: `docker compose build rtmp-server`
- [ ] Restart: `docker compose up -d rtmp-server`
- [ ] Testar stream novo
- [ ] Verificar: `ls /tmp/hls/<stream_key>/` deve mostrar v0/ v1/ v2/ v3/
- [ ] Validar master.m3u8 tem múltiplas qualidades

## 📌 Observações Importantes

1. **Loop Infinito Evitado**: FFmpeg lê do Nginx-RTMP via `rtmp://127.0.0.1/live/`, não cria loop
2. **Cleanup HLS**: Funciona normalmente (remove .ts e .m3u8 antigos)
3. **Engine Toggle**: `TRANSCODING_ENGINE=gstreamer` funcionará apenas se HLS do Nginx estiver desabilitado
4. **Callbacks Backend**: Funcionam independente da configuração HLS

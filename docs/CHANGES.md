# Registro de Bugs e Correções

## Resumo

Sessão completa de testes dos 4 modos de operação do sistema de streaming:

| Combinação | Resultado |
|---|---|
| Nginx + FFmpeg | ✅ Funcionando — 4 qualidades (1080p/720p/480p/360p) |
| Nginx + GStreamer | ✅ Funcionando — 2 qualidades (1080p/480p) |
| SRS + FFmpeg | ✅ Funcionando — 4 qualidades (1080p/720p/480p/360p) |
| SRS + GStreamer | ✅ Funcionando — 2 qualidades (1080p/480p) |

---

## Bug 1 — SRS: Publisher rejeitado em loop (OBS reconectava infinitamente)

**Arquivo:** `app/backend/rtmp/srs/srs.conf`

**Causa raiz:**
As URLs dos hooks HTTP tinham `?name=[stream]` como query param. O SRS 4.x interpreta `[stream]` como placeholder para o `exec` directive, mas **não** expande em URLs de `http_hooks`. O servidor enviava literalmente `/api/streams/callback/publish?name=[stream]` ao backend, que o Tomcat recusava com HTTP 400 (colchetes são caracteres inválidos em URLs conforme RFC 3986). Com o erro 400, o SRS rejeitava o publisher e o OBS tentava reconectar em loop.

**Além disso**, a URL de `on_unpublish` apontava incorretamente para o endpoint `/publish` em vez de `/publish_done`.

**Correção:**
```diff
- on_publish   http://.../api/streams/callback/publish?name=[stream];
- on_unpublish http://.../api/streams/callback/publish_done?name=[stream];
+ on_publish   http://.../api/streams/callback/publish;
+ on_unpublish http://.../api/streams/callback/publish_done;
```
O SRS envia o stream key no corpo JSON da requisição — o backend já tinha sido atualizado para ler do body.

---

## Bug 2 — SRS: Callback rejeitado pelo backend (formato JSON vs form params)

**Arquivo:** `app/backend/streaming-platform/.../RtmpCallbackController.java`

**Causa raiz:**
O SRS 4.x envia callbacks como JSON body `{"stream":"key"}` e espera `{"code":0}` na resposta. O controller original usava `@RequestParam String name` (que funciona com form params do Nginx-RTMP) e retornava `ResponseEntity<Void>` vazio (sem body). Isso causava:
- SRS: body vazio → rejeitava o publisher  
- Nginx-RTMP: JSON body → `name` null → NullPointerException

**Correção:**
O controller foi reescrito para ser agnóstico ao servidor RTMP:
- Usa `HttpServletRequest` e tenta primeiro `request.getParameter("name")` (Nginx-RTMP)
- Se vazio, parseia o body JSON buscando campo `stream` (SRS)
- Retorna `Map<String,Integer>` com `{"code":0}` em caso de sucesso e `{"code":403}` em caso de rejeição

---

## Bug 3 — Nginx-RTMP: HLS nunca iniciava (variável com dupla responsabilidade)

**Arquivos:**
- `app/backend/rtmp/nginx/scripts/transcode-ffmpeg.sh`
- `app/backend/rtmp/nginx/scripts/transcode-gstreamer.sh`
- `app/backend/rtmp/nginx/scripts/docker-entrypoint.sh`
- (mesmos para `srs/`)

**Causa raiz:**
`STREAM_STABILIZE_SECONDS=8` era usado tanto como:
1. Tempo inicial de espera antes de iniciar o FFmpeg
2. Threshold de detecção de "stream encerrado"

Se o FFmpeg falhasse dentro de 10s (timeout `-rw_timeout 10000000`), o loop verificava se `failed_seconds >= STREAM_STABILIZE_SECONDS` (8s) e **encerrava o script** sem tentar novamente.

**Correção:**
As two responsabilidades foram separadas em variáveis distintas:
```diff
- STREAM_STABILIZE_SECONDS=8   # usada para ambos os propósitos
+ STREAM_STABILIZE_SECONDS=3   # apenas espera inicial (cold start)
+ STREAM_END_DURATION_SECONDS=30  # threshold para declarar stream encerrada
```

---

## Bug 4 — Nginx-RTMP: HLS nunca iniciava (múltiplos workers)

**Arquivo:** `app/backend/rtmp/nginx/configs/nginx.conf`

**Causa raiz (confirmada via logs):**
```
23#0: *2 publish  ← worker 23 recebe o publisher
24#0: *4 play     ← worker 24 tenta receber o subscriber (transcoder)
```
Com `worker_processes auto;`, o nginx spawna um worker por CPU. O nginx-rtmp **não compartilha estado de streams entre workers** — publisher e subscriber precisam cair no mesmo worker. Com múltiplos workers, o transcoder conectava a um worker diferente e recebia `Input/output error`.

**Correção:**
```diff
- worker_processes auto;
+ worker_processes 1;
```

---

## Bug 5 — SRS: HTTP server intercepta arquivos `.m3u8`

**Arquivos:**
- `app/backend/rtmp/srs/srs.conf`
- `app/backend/rtmp/srs/Dockerfile`
- `app/backend/rtmp/srs/scripts/docker-entrypoint.sh`

**Causa raiz:**
O `http_server` do SRS 4.x intercepta **qualquer requisição** para arquivos `.m3u8`, ignorando o arquivo estático em disco e gerando uma resposta sintética inválida (`#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=1,...`). Isso ocorre mesmo com `hls { enabled off; }`.

**Correção:**
- Desabilitar o `http_server` nativo do SRS: `enabled off`
- Adicionar `python3` ao Dockerfile
- No entrypoint, iniciar Python HTTP server como processo em background:
  ```bash
  python3 -m http.server "${HLS_HTTP_PORT}" --directory "${HLS_ROOT_PATH}" &
  ```
  `HLS_ROOT_PATH` é `/tmp` (pai de `/tmp/hls`), mantendo o prefixo `/hls/` na URL, compatível com o que o frontend espera (`http://host:8081/hls/{streamKey}/master.m3u8`).

---

## Bug 6 — SRS: URL de estatísticas RTMP na porta errada

**Arquivos:**
- `app/backend/rtmp/srs/scripts/transcode-ffmpeg.sh`
- `app/backend/rtmp/srs/scripts/transcode-gstreamer.sh`

**Causa raiz:**
```bash
RTMP_STAT_URL="http://127.0.0.1:${HLS_HTTP_PORT}/api/v1/streams"
# HLS_HTTP_PORT=8081 → errado (porta do servidor HTTP de arquivos)
```
A API HTTP do SRS roda na porta **1985**, não na porta do servidor HLS.

**Correção:**
```diff
- RTMP_STAT_URL="http://127.0.0.1:${HLS_HTTP_PORT}/api/v1/streams"
+ RTMP_STAT_URL="http://127.0.0.1:1985/api/v1/streams"
```

---

## Bug 7 — Backend: Imports e método ausentes no StreamService

**Arquivo:** `app/backend/streaming-platform/.../StreamService.java`

**Causa raiz:**
Todos os imports de classes de domínio estavam ausentes (pre-existing bug — provavelmente resultado de um refactoring incompleto). Além disso, o método `validateAndEndStream(String streamKey)` era chamado pelo `RtmpCallbackController` mas não existia na classe.

**Correção:**
Adicionados 17 imports ausentes e implementado o método `validateAndEndStream`.

---

## Bug 8 — Frontend: URL hardcoded no hook de viewer

**Arquivo:** `app/frontend/src/app/hooks/useViewerJoinLeave.ts`

**Causa raiz:**
O handler `beforeunload` usava `http://localhost:8080/api/streams/${streamId}/leave` literalmente, ignorando a configuração `API_BASE_URL`.

**Correção:**
```diff
- const url = `http://localhost:8080/api/streams/${streamId}/leave`;
+ const url = `${API_BASE_URL}/api/streams/${streamId}/leave`;
```

---

## Código morto removido

**Arquivo:** `app/frontend/src/app/hooks/useViewerJoinLeave.ts`

Um objeto literal orphan foi encontrado no handler `beforeunload`:
```typescript
// REMOVIDO — remanescente de uma chamada logger.debug():
{
  streamId,
  viewerId,
  countAsViewer,
},
```

---

## Arquivos modificados

| Arquivo | Tipo de mudança |
|---|---|
| `app/backend/rtmp/nginx/configs/nginx.conf` | `worker_processes auto` → `1` |
| `app/backend/rtmp/nginx/scripts/transcode-ffmpeg.sh` | Separar STABILIZE/END_DURATION; ajustar `-rw_timeout` |
| `app/backend/rtmp/nginx/scripts/transcode-gstreamer.sh` | Mesmo que acima |
| `app/backend/rtmp/nginx/scripts/docker-entrypoint.sh` | Novos defaults das variáveis separadas |
| `app/backend/rtmp/srs/srs.conf` | Remover `?name=[stream]` das URLs; fix `on_unpublish`; desabilitar `http_server` |
| `app/backend/rtmp/srs/Dockerfile` | Adicionar `python3`; ajustar healthcheck |
| `app/backend/rtmp/srs/scripts/transcode-ffmpeg.sh` | Separar STABILIZE/END_DURATION; fix `RTMP_STAT_URL` porta 1985 |
| `app/backend/rtmp/srs/scripts/transcode-gstreamer.sh` | Mesmo que acima |
| `app/backend/rtmp/srs/scripts/docker-entrypoint.sh` | Novos defaults; iniciar Python HTTP server |
| `app/backend/streaming-platform/.../RtmpCallbackController.java` | Reescrito para suportar Nginx-RTMP e SRS |
| `app/backend/streaming-platform/.../StreamService.java` | Imports ausentes; método `validateAndEndStream` |
| `app/frontend/src/app/hooks/useViewerJoinLeave.ts` | Fix URL hardcoded; remover objeto orphan |

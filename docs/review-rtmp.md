# Code Review — `app/backend/rtmp`

Data: 2026-04-02

> **Atenção:** Esta revisão aponta apenas problemas e melhorias, sem alterar regras de negócio ou lógica de transcodagem.

---

## 1. Código Duplicado e Arquivos Redundantes

### 1.1 `cleanup-hls.sh` é idêntico entre nginx e srs
**Arquivos:**
- `nginx/scripts/cleanup-hls.sh`
- `srs/scripts/cleanup-hls.sh`

Os dois arquivos são byte-a-byte idênticos. Qualquer correção futura precisará ser aplicada nos dois lugares. Considerar um script compartilhado referenciado via bind mount ou copiado no `docker build`, ou aceitar a duplicação documentada.

---

## 2. Inconsistência de Variáveis de Ambiente

### 2.1 Scripts nginx hardcodam `/tmp/hls/` em vez de usar `HLS_PATH`
**Arquivos:**
- `nginx/scripts/transcode-ffmpeg.sh` → `OUTPUT_DIR="/tmp/hls/${STREAM_KEY}"`
- `nginx/scripts/transcode-gstreamer.sh` → `OUTPUT_DIR="/tmp/hls/${STREAM_KEY}"`

Os scripts SRS equivalentes usam corretamente `HLS_BASE_PATH="${HLS_PATH:-/tmp/hls}"`. Se `HLS_PATH` for alterado via variável de ambiente no Docker Compose, os scripts nginx ignorarão a mudança e continuarão gravando em `/tmp/hls/`. Os scripts nginx deveriam usar:

```bash
HLS_BASE_PATH="${HLS_PATH:-/tmp/hls}"
OUTPUT_DIR="${HLS_BASE_PATH}/${STREAM_KEY}"
```

---

## 3. Falhas de Robustez

### 3.1 `service cron start` pode falhar silenciosamente em imagens não-Debian
**Arquivos:**
- `nginx/scripts/docker-entrypoint.sh`
- `srs/scripts/docker-entrypoint.sh`

O comando `service cron start` assume imagem baseada em Debian/Ubuntu com `sysvinit-utils`. Se a imagem base mudar (ex: Alpine), o cron não inicia e o cleanup do HLS não ocorre. Não há verificação de sucesso:

```bash
service cron start  # não verifica exit code!
```

Adicionar verificação mínima:
```bash
service cron start || echo "[WARN] cron service failed to start — HLS cleanup may not run"
```

### 3.2 `hls-server.py` — `os.chdir()` altera o CWD do processo globalmente
**Arquivo:** `srs/scripts/hls-server.py`

```python
os.chdir(directory)
server = HTTPServer(("", port), CORSRequestHandler)
```

`os.chdir()` modifica o diretório de trabalho de todo o processo Python. É um efeito colateral global. Para um servidor de uso único isso é inofensivo, mas a prática correta é passar `directory` explicitamente via `SimpleHTTPRequestHandler`:

```python
handler = lambda *args, **kwargs: CORSRequestHandler(*args, directory=directory, **kwargs)
server = HTTPServer(("", port), handler)
# sem os.chdir()
```

### 3.3 `transcode-ffmpeg.sh` — arquivo de lock apagado pelo cleanup antes do stream terminar
**Arquivos:** `nginx/scripts/transcode-ffmpeg.sh`, `srs/scripts/transcode-ffmpeg.sh`

O arquivo `${OUTPUT_DIR}/transcode.lock` é criado dentro de `${OUTPUT_DIR}/`. O `cleanup-hls.sh` apaga todos os arquivos do diretório com mais de `RETENTION_HOURS` horas. Se um stream estiver rodando continuamente por mais de `HLS_RETENTION_HOURS` (padrão 6h), o `.lock` não será apagado (pois será modificado recentemente pela `flock`), mas `.transcode.log` pode ser deletado.

O arquivo de log `transcode.log` também fica dentro de `OUTPUT_DIR` e é excluído pelo cleanup:
```bash
find "$HLS_DIR" -type f \( -name "*.ts" -o -name "*.m3u8" -o -name "*.log" \) -mmin "+${RETENTION_MINUTES}"
```

Como o log é aberto no modo `append`, seu `mtime` é atualizado a cada escrita, então na prática só é deletado após 6h sem atividade. Porém, se o transcoder não logar por longos períodos (ex: stream pausada), o log pode sumir. Solução: mover logs para um diretório fora do `HLS_PATH`, ex: `/var/log/transcoder/${STREAM_KEY}.log`.

---

## 4. Problemas de Código nos Scripts Shell

### 4.1 `transcode-gstreamer.sh` (nginx) — cria apenas `v0` e `v2`, sem documentação da razão
**Arquivo:** `nginx/scripts/transcode-gstreamer.sh`

```bash
if ! mkdir -p "${OUTPUT_DIR}/v0" "${OUTPUT_DIR}/v2"; then
```

FFmpeg cria `v0`-`v3`. GStreamer cria apenas `v0` e `v2` (1080p e 480p, aparentemente). Sem comentário explicando por que `v1` e `v3` são omitidos. O `master.m3u8` resultante terá menos variantes e os clientes que tentarem `v1/` ou `v3/` receberão 404. Adicionar v1 e v3 no gstreamer

### 4.2 `is_stream_active()` no nginx — fallback de XML parsing pode ser frágil
**Arquivo:** `nginx/scripts/transcode-ffmpeg.sh` e `nginx/scripts/transcode-gstreamer.sh`

O fallback `awk` que parseia o XML do `/stat` do nginx é complexo e pode falhar silenciosamente se a estrutura do XML do nginx-rtmp mudar. Como já há o `ffprobe` como segunda verificação, o fallback XML é tertiary. O risco real é baixo, mas o awk poderia ser substituído por `xmllint` ou `grep -A` mais simples se disponível.

### 4.3 Variável `RTMP_STAT_URL` no nginx aponta para o HTTP do HLS server, não para RTMP stat separado
**Arquivo:** `nginx/scripts/transcode-ffmpeg.sh`

```bash
RTMP_STAT_URL="http://127.0.0.1:${HLS_HTTP_PORT:-8081}/stat"
```

O endpoint `/stat` do nginx-RTMP é **servido pelo mesmo nginx** na porta `HLS_HTTP_PORT` (via o bloco `location /stat` em `nginx.conf`). Isso é correto. Mas se o nginx ainda não subiu (race condition durante startup), o wget retorna erro. Isso é aceitável pois a função já trata o retorno de erro — apenas garantindo que está ciente do potencial de falha.

### 4.4 Ausência de `set -e` nos scripts de transcodagem (uso de `set -u` apenas)
**Arquivos:** todos os `transcode-*.sh`

Os scripts de transcodagem usam `set -u` (erro em variáveis indefinidas) mas **não** `set -e` (sair em erro de comando). Isso é intencional — o loop de retry precisa continuar mesmo quando ffmpeg/gstreamer retorna código de saída não-zero. Porém, não está documentado como intencional. Adicionar um comentário:

```bash
set -u
# Nota: set -e não é usado intencionalmente — o loop de retry precisa tratar
# exit codes não-zero do ffmpeg/gstreamer sem encerrar o script.
```

---
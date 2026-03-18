# RTMP Server - Nginx Configuration

## Estrutura de Diretórios

```
nginx/
├── Dockerfile              # Imagem Docker do servidor RTMP
├── configs/                # Configurações
│   └── nginx.conf         # Template de configuração do Nginx-RTMP
├── scripts/               # Scripts de operação
│   ├── docker-entrypoint.sh    # Script de inicialização
│   ├── transcode-ffmpeg.sh     # Transcodição com FFmpeg (multi-qualidade HLS)
│   └── cleanup-hls.sh          # Limpeza de arquivos HLS antigos
└── docs/                  # Documentação
    └── README.md          # Este arquivo
```

## Variáveis de Ambiente

Todas as configurações podem ser ajustadas via variáveis de ambiente no arquivo `.env`:

### Configurações RTMP

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `RTMP_PORT` | 1935 | Porta do servidor RTMP |
| `RTMP_CHUNK_SIZE` | 4096 | Tamanho do chunk RTMP |

### Configurações HLS

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `HLS_PATH` | /tmp/hls | Diretório de saída dos arquivos HLS |
| `HLS_HTTP_PORT` | 8081 | Porta do servidor HTTP para HLS |
| `HLS_RETENTION_HOURS` | 6 | Tempo de retenção dos arquivos HLS |

### Configurações Backend

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `BACKEND_HOST` | streaming-platform | Hostname do backend |
| `BACKEND_PORT` | 8080 | Porta do backend |

### Configurações Transcodição

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `TRANSCODER` | ffmpeg | Motor de transcodificação (`ffmpeg` ou `gstreamer`) |
| `HLS_SEGMENT_DURATION` | 6 | Duração dos segmentos HLS (segundos) |
| `HLS_PLAYLIST_LENGTH` | 10 | Tamanho da janela deslizante da playlist HLS |
| `TRANSCODER_MAX_RETRIES` | 10 | Quantidade máxima de tentativas por publish |
| `TRANSCODER_RETRY_WAIT` | 3 | Intervalo entre tentativas (segundos) |
| `TRANSCODER_STREAM_STABILIZE_SECONDS` | 8 | Espera inicial para estabilização do stream |
| `TRANSCODER_STARTUP_TIMEOUT` | 20 | Timeout de bootstrap para playlists (GStreamer) |

O entrypoint seleciona automaticamente o script correto via symlink em `/usr/local/bin/transcode.sh`.

## Como Funciona

### 1. Inicialização

O `docker-entrypoint.sh`:
1. Define valores padrão para todas as variáveis de ambiente
2. Substitui as variáveis no template `nginx.conf.template` usando `envsubst`
3. Aguarda o backend estar disponível
4. Inicia o serviço cron para limpeza HLS
5. Inicia o Nginx em foreground

### 2. Transcodição

Quando um stream RTMP é iniciado:
1. Nginx-RTMP chama o callback `on_publish` no backend para validar
2. Se aprovado, o Nginx-RTMP executa `exec /usr/local/bin/transcode.sh $name`
3. O transcoder selecionado lê `rtmp://127.0.0.1/live/{key}` e gera HLS adaptativo
4. Arquivos gerados em `/tmp/hls/{key}/master.m3u8` e subpastas de variante

Perfis atuais:
- `ffmpeg`: 4 qualidades (`v0` 1080p, `v1` 720p, `v2` 480p, `v3` 360p)
- `gstreamer`: 2 qualidades estáveis (`v0` 1080p, `v2` 480p)
5. Frontend lê `master.m3u8` para ABR automático

### 3. Limpeza HLS

O `cleanup-hls.sh` roda a cada hora via cron e remove:
- Arquivos `.ts` e `.m3u8` mais antigos que `HLS_RETENTION_HOURS`
- Diretórios vazios resultantes

## Callbacks HTTP

O Nginx-RTMP faz callbacks para o backend:

- **on_publish**: `POST http://${BACKEND_HOST}:${BACKEND_PORT}/api/streams/callback/publish`
  - Valida stream key
  - Retorna 200 para aceitar, 403 para rejeitar
  
- **on_publish_done**: `POST http://${BACKEND_HOST}:${BACKEND_PORT}/api/streams/callback/publish_done`
  - Notifica quando o streamer desconecta

## Logs

- **Nginx Error**: `/var/log/nginx/error.log`
- **Nginx Access**: `/var/log/nginx/access.log`
- **HLS Cleanup**: `/var/log/nginx/hls-cleanup.log`
- **Transcodificação**: `/tmp/hls/<stream_key>/transcode.log`

## Troubleshooting

### Stream não inicia

1. Verifique se o backend está acessível:
```bash
docker compose logs rtmp-server | grep "is ready"
```

2. Verifique logs de transcodificação:
```bash
docker compose exec rtmp-server cat /tmp/hls/<stream_key>/transcode.log
```

### Qualidade ruim

Ajuste variáveis no `.env` e reinicie o container:
- `HLS_SEGMENT_DURATION`
- `HLS_PLAYLIST_LENGTH`
- `TRANSCODER_MAX_RETRIES`
- `TRANSCODER_RETRY_WAIT`
- `TRANSCODER_STREAM_STABILIZE_SECONDS`

### Disco cheio

Reduza retenção de HLS:
```bash
HLS_RETENTION_HOURS=3  # no .env
```

Ou execute limpeza manual:
```bash
docker compose exec rtmp-server /usr/local/bin/cleanup-hls.sh
```

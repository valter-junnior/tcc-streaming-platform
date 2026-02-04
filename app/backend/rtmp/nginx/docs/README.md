# RTMP Server - Nginx Configuration

## Estrutura de Diretórios

```
nginx/
├── Dockerfile              # Imagem Docker do servidor RTMP
├── configs/                # Configurações
│   └── nginx.conf         # Template de configuração do Nginx-RTMP
├── scripts/               # Scripts de operação
│   ├── docker-entrypoint.sh    # Script de inicialização
│   ├── transcode-wrapper.sh    # Wrapper para escolher engine de transcodificação
│   ├── transcode-ffmpeg.sh     # Transcodificação com FFmpeg
│   ├── transcode-gstreamer.sh  # Transcodificação com GStreamer
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
| `HLS_FRAGMENT_DURATION` | 6s | Duração de cada segmento .ts |
| `HLS_PLAYLIST_LENGTH` | 60s | Duração total da playlist |
| `HLS_HTTP_PORT` | 8081 | Porta do servidor HTTP para HLS |
| `HLS_RETENTION_HOURS` | 6 | Tempo de retenção dos arquivos HLS |

### Configurações Backend

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `BACKEND_HOST` | streaming-platform | Hostname do backend |
| `BACKEND_PORT` | 8080 | Porta do backend |

### Configurações Transcodificação

| Variável | Padrão | Descrição |
|----------|--------|-----------|
| `TRANSCODING_ENGINE` | ffmpeg | Engine de transcodificação (ffmpeg ou gstreamer) |

## Como Funciona

### 1. Inicialização

O `docker-entrypoint.sh`:
1. Define valores padrão para todas as variáveis de ambiente
2. Substitui as variáveis no template `nginx.conf.template` usando `envsubst`
3. Aguarda o backend estar disponível
4. Inicia o serviço cron para limpeza HLS
5. Inicia o Nginx em foreground

### 2. Transcodificação

Quando um stream RTMP é iniciado:
1. Nginx-RTMP chama o callback `on_publish` no backend para validar
2. Se aprovado, executa `transcode-wrapper.sh` com a stream key
3. O wrapper verifica `TRANSCODING_ENGINE` e chama o script apropriado:
   - `transcode-ffmpeg.sh` - Usa FFmpeg para transcodificação
   - `transcode-gstreamer.sh` - Usa GStreamer para transcodificação
4. O script de transcodificação gera 4 qualidades HLS (1080p, 720p, 480p, 360p)

### 3. Limpeza HLS

O `cleanup-hls.sh` roda a cada hora via cron e remove:
- Arquivos `.ts` e `.m3u8` mais antigos que `HLS_RETENTION_HOURS`
- Diretórios vazios resultantes

## Alternar Engine de Transcodificação

Para alternar entre FFmpeg e GStreamer:

```bash
# Opção 1: Editar .env
TRANSCODING_ENGINE=gstreamer  # ou ffmpeg

# Opção 2: Usar script helper
./scripts/switch-transcoding-engine.sh gstreamer

# Rebuild do container
docker compose up -d --build rtmp-server
```

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

Ajuste parâmetros de transcodificação em `transcode-ffmpeg.sh`:
- Bitrate: `-b:v:0 5000k`
- Preset: `-preset fast` (veryfast, fast, medium, slow)

### Disco cheio

Reduza retenção de HLS:
```bash
HLS_RETENTION_HOURS=3  # no .env
```

Ou execute limpeza manual:
```bash
docker compose exec rtmp-server /usr/local/bin/cleanup-hls.sh
```

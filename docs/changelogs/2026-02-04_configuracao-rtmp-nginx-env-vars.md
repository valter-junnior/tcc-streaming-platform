# Changelog - Configuração RTMP/Nginx com Variáveis de Ambiente

**Data**: 2026-02-04  
**Autor**: GitHub Copilot  
**Task**: Organização e configuração do RTMP + Transcodificação via variáveis de ambiente

## Objetivo

Configurar o servidor RTMP/Nginx para que todas as configurações sejam ajustáveis via variáveis de ambiente (conforme arquivo `.env`), além de reorganizar a estrutura de pastas para melhor manutenibilidade.

## Alterações Realizadas

### 1. Estrutura de Diretórios (`app/backend/rtmp/nginx/`)

**Antes:**
```
nginx/
├── Dockerfile
├── nginx.conf
├── docker-entrypoint.sh
├── transcode-wrapper.sh
├── transcode-ffmpeg.sh
├── transcode-gstreamer.sh
└── cleanup-hls.sh
```

**Depois:**
```
nginx/
├── Dockerfile
├── configs/
│   └── nginx.conf          # Template com placeholders ${VAR}
├── scripts/
│   ├── docker-entrypoint.sh
│   ├── transcode-wrapper.sh
│   ├── transcode-ffmpeg.sh
│   ├── transcode-gstreamer.sh
│   └── cleanup-hls.sh
└── docs/
    └── README.md           # Documentação completa
```

### 2. Variáveis de Ambiente (`.env`)

**Adicionadas:**
```bash
# RTMP Advanced Configuration
RTMP_CHUNK_SIZE=4096
HLS_PATH=/tmp/hls
HLS_FRAGMENT_DURATION=6s
HLS_PLAYLIST_LENGTH=60s
HLS_HTTP_PORT=8081
HLS_RETENTION_HOURS=6
```

**Modificadas no docker-compose.yml:**
```yaml
environment:
  RTMP_PORT: ${RTMP_PORT}
  RTMP_CHUNK_SIZE: ${RTMP_CHUNK_SIZE}
  HLS_PATH: ${HLS_PATH}
  HLS_FRAGMENT_DURATION: ${HLS_FRAGMENT_DURATION}
  HLS_PLAYLIST_LENGTH: ${HLS_PLAYLIST_LENGTH}
  BACKEND_HOST: streaming-platform
  BACKEND_PORT: 8080
  HLS_HTTP_PORT: ${HLS_HTTP_PORT}
  TRANSCODING_ENGINE: ${TRANSCODING_ENGINE:-ffmpeg}
  HLS_RETENTION_HOURS: ${HLS_RETENTION_HOURS}
```

### 3. Arquivo: `configs/nginx.conf`

**Mudanças:**
- Porta RTMP: `listen 1935;` → `listen ${RTMP_PORT};`
- Chunk size: `chunk_size 4096;` → `chunk_size ${RTMP_CHUNK_SIZE};`
- HLS path: `hls_path /tmp/hls;` → `hls_path ${HLS_PATH};`
- HLS fragment: `hls_fragment 6s;` → `hls_fragment ${HLS_FRAGMENT_DURATION};`
- HLS playlist: `hls_playlist_length 60s;` → `hls_playlist_length ${HLS_PLAYLIST_LENGTH};`
- Backend callbacks: `http://streaming-platform:8080` → `http://${BACKEND_HOST}:${BACKEND_PORT}`
- HTTP server: `listen 8081;` → `listen ${HLS_HTTP_PORT};`

### 4. Arquivo: `scripts/docker-entrypoint.sh`

**Novas funcionalidades:**
1. **Definição de valores padrão** para todas as variáveis
2. **Exibição da configuração** no startup (logs)
3. **Geração dinâmica** do `nginx.conf` usando `envsubst`:
   ```bash
   envsubst '${RTMP_PORT} ${RTMP_CHUNK_SIZE} ...' \
       < /usr/local/nginx/conf/nginx.conf.template \
       > /usr/local/nginx/conf/nginx.conf
   ```
4. **Espera dinâmica** pelo backend usando variáveis `${BACKEND_HOST}:${BACKEND_PORT}`

### 5. Arquivo: `Dockerfile`

**Mudanças:**
1. Instalação do pacote `gettext-base` (fornece comando `envsubst`)
2. Ajuste nos paths de COPY:
   ```dockerfile
   COPY configs/nginx.conf /usr/local/nginx/conf/nginx.conf.template
   COPY scripts/*.sh /usr/local/bin/
   ```

### 6. Documentação: `docs/README.md`

Criado arquivo completo com:
- Explicação da estrutura de diretórios
- Tabela de todas as variáveis de ambiente
- Como funciona o fluxo de inicialização
- Como alternar entre engines de transcodificação
- Seção de troubleshooting

## Validação

### Testes Realizados

1. **Build do container:**
   ```bash
   cd app/
   docker compose build rtmp-server
   ```
   ✅ Build bem-sucedido (213.4s)

2. **Startup do container:**
   ```bash
   docker compose up -d rtmp-server
   ```
   ✅ Container iniciado corretamente

3. **Verificação de logs:**
   ```bash
   docker compose logs rtmp-server
   ```
   ✅ Configurações exibidas corretamente:
   ```
   === RTMP Server Configuration ===
   RTMP_PORT: 1935
   RTMP_CHUNK_SIZE: 4096
   HLS_PATH: /tmp/hls
   HLS_FRAGMENT_DURATION: 6s
   HLS_PLAYLIST_LENGTH: 60s
   BACKEND_HOST: streaming-platform
   BACKEND_PORT: 8080
   HLS_HTTP_PORT: 8081
   TRANSCODING_ENGINE: ffmpeg
   HLS_RETENTION_HOURS: 6
   =================================
   ```

4. **Verificação do nginx.conf gerado:**
   ```bash
   docker compose exec rtmp-server cat /usr/local/nginx/conf/nginx.conf
   ```
   ✅ Variáveis substituídas corretamente (ex: `listen 1935;`, `hls_path /tmp/hls;`)

5. **Conexão com backend:**
   ```
   streaming-platform:8080 is ready!
   ```
   ✅ Nginx aguardou e conectou ao backend

## Benefícios

1. **Flexibilidade**: Todas as configurações ajustáveis via `.env` sem rebuild
2. **Organização**: Estrutura de pastas clara (configs/, scripts/, docs/)
3. **Manutenibilidade**: Código organizado e documentado
4. **Reutilização**: Fácil adaptar para diferentes ambientes (dev, prod)
5. **Debugging**: Logs mostram configurações aplicadas no startup

## Próximos Passos

- [ ] Testar streaming RTMP real com OBS
- [ ] Validar callbacks HTTP do backend
- [ ] Testar alternância entre FFmpeg e GStreamer
- [ ] Monitorar uso de disco com HLS cleanup

## Referências

- Documentação: [app/backend/rtmp/nginx/docs/README.md](../app/backend/rtmp/nginx/docs/README.md)
- Variáveis: [app/.env](../app/.env)
- Docker Compose: [app/docker-compose.yml](../app/docker-compose.yml)

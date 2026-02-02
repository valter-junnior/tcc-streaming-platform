# Guia de Teste Manual - Nginx-RTMP

## Pré-requisitos

- Docker e Docker Compose instalados e rodando
- OBS Studio instalado
- Sistema rodando: `docker compose up -d`

## Passo a Passo

### 1. Criar uma Stream

```bash
curl -X POST http://localhost:8080/api/streams \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Teste RTMP",
    "description": "Testando transmissão via OBS"
  }'
```

**Resposta esperada**:
```json
{
  "id": "uuid-aqui",
  "streamKey": "str_xxxxxxxxx",
  "rtmpUrl": "rtmp://nginx-rtmp:1935/live",
  "watchUrl": "http://localhost:3001/watch/uuid-aqui",
  "status": "WAITING"
}
```

**Anote o `streamKey`!**

### 2. Configurar OBS Studio

1. Abra o OBS Studio
2. Vá em **Settings** (Configurações)
3. Selecione **Stream** na lateral esquerda
4. Configure:
   - **Service**: Custom
   - **Server**: `rtmp://localhost:1935/live`
   - **Stream Key**: Cole o `streamKey` obtido no passo 1
5. Clique em **Apply** e depois **OK**

### 3. Iniciar Transmissão

1. No OBS, adicione uma fonte (ex: Display Capture, Window Capture, ou Image)
2. Clique em **Start Streaming**
3. Observe a mensagem: "Live" no canto inferior direito

### 4. Verificar no Backend

**Status da stream**:
```bash
curl http://localhost:8080/api/streams/{stream-id}/status
```

**Resposta esperada**:
```json
{
  "id": "uuid-aqui",
  "status": "LIVE",
  "currentViewers": 0,
  "viewersPeak": 0
}
```

**Logs do backend**:
```bash
docker compose logs streaming-platform | grep -i "stream"
```

Você deve ver logs dos callbacks do Nginx-RTMP:
- `onPublish` → validação da stream key
- `onPublishDone` → stream iniciou

### 5. Verificar Nginx-RTMP

**Estatísticas**:
```bash
curl http://localhost:8081/stat | grep -A5 "live"
```

Você deve ver:
```xml
<application>
  <name>live</name>
  <live>
    <nclients>1</nclients>
    <stream>
      <name>str_xxxxxxxxx</name>
      ...
    </stream>
  </live>
</application>
```

**Arquivos HLS gerados**:
```bash
docker exec streaming-nginx-rtmp ls -lh /tmp/hls/
```

Você deve ver arquivos `.m3u8` e `.ts` sendo gerados.

### 6. Parar Transmissão

1. No OBS, clique em **Stop Streaming**
2. Verifique o status novamente:

```bash
curl http://localhost:8080/api/streams/{stream-id}/status
```

**Resposta esperada**:
```json
{
  "status": "ENDED"
}
```

## Troubleshooting

### Erro: "Failed to connect to server"

**Problema**: OBS não consegue conectar ao servidor RTMP

**Soluções**:
1. Verifique se o container nginx-rtmp está rodando:
   ```bash
   docker compose ps nginx-rtmp
   ```

2. Verifique se a porta 1935 está aberta:
   ```bash
   netstat -tulpn | grep 1935
   ```

3. Verifique logs do Nginx-RTMP:
   ```bash
   docker compose logs nginx-rtmp
   ```

### Erro: 403 Forbidden

**Problema**: Stream key inválida ou stream não criada

**Soluções**:
1. Certifique-se de criar a stream via API antes de tentar transmitir
2. Verifique se copiou o `streamKey` corretamente (sem espaços extras)
3. Verifique logs do backend:
   ```bash
   docker compose logs streaming-platform | grep "callback"
   ```

### Stream não muda para LIVE

**Problema**: Callbacks não estão sendo chamados

**Soluções**:
1. Verifique se o backend está acessível do container nginx-rtmp:
   ```bash
   docker exec streaming-nginx-rtmp ping -c 2 streaming-platform
   ```

2. Verifique a configuração do nginx.conf:
   ```bash
   docker exec streaming-nginx-rtmp cat /usr/local/nginx/conf/nginx.conf | grep on_publish
   ```

3. Verifique logs detalhados:
   ```bash
   docker compose logs -f streaming-platform nginx-rtmp
   ```

## URLs Úteis

- Backend API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Nginx-RTMP Stats: http://localhost:8081/stat
- RabbitMQ Management: http://localhost:15672 (user: streaming_user, pass: streaming_pass)

## Próximos Passos

Após validar que a transmissão RTMP funciona:
1. Implementar FFmpeg para transcodificação multi-bitrate (Fase 3.2)
2. Configurar HLS serving (Fase 3.3)
3. Criar player HLS no frontend (Fase 4)

---

**Última atualização**: 02/02/2026

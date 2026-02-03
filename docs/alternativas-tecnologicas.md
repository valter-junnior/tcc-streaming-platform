# Alternativas Tecnológicas - Guia de Uso

Este documento explica como executar e testar diferentes combinações de tecnologias na plataforma de streaming.

## 📋 Stacks Disponíveis

### 1. **Default Stack** (Nginx-RTMP + FFmpeg)
- **RTMP Server**: Nginx-RTMP
- **Transcodificação**: FFmpeg
- **Message Broker**: RabbitMQ
- **Cache**: Redis
- **Arquivo**: `docker-compose.yml`

**Características**:
- ✅ Mais estável e maduro
- ✅ Menor uso de recursos
- ❌ Nginx-RTMP não é mais mantido
- ❌ Menos features modernas

### 2. **SRS Stack** (SRS)
- **RTMP Server**: SRS (Simple Realtime Server)
- **Transcodificação**: SRS interno
- **Message Broker**: RabbitMQ
- **Cache**: Redis
- **Arquivo**: `docker-compose.srs.yml`

**Características**:
- ✅ Projeto ativo e moderno
- ✅ API REST integrada
- ✅ Dashboard web nativo
- ✅ Suporte a WebRTC
- ❌ Maior consumo de recursos

### 3. **GStreamer Stack** (Nginx-RTMP + GStreamer)
- **RTMP Server**: Nginx-RTMP
- **Transcodificação**: GStreamer
- **Message Broker**: RabbitMQ
- **Cache**: Redis
- **Arquivo**: `docker-compose.gstreamer.yml`

**Características**:
- ✅ Pipelines flexíveis e customizáveis
- ✅ Melhor debugging e controle
- ✅ APIs programáticas poderosas
- ❌ Curva de aprendizado maior
- ❌ Configuração mais complexa

## 🚀 Como Usar

### Script de Execução

Use o script `scripts/run-stack.sh` para gerenciar as diferentes stacks:

```bash
# Listar stacks disponíveis
./scripts/run-stack.sh list

# Iniciar uma stack
./scripts/run-stack.sh up default     # Default (Nginx + FFmpeg)
./scripts/run-stack.sh up srs         # SRS
./scripts/run-stack.sh up gstreamer   # Nginx + GStreamer

# Ver logs
./scripts/run-stack.sh logs default
./scripts/run-stack.sh logs srs

# Ver status
./scripts/run-stack.sh status default

# Parar uma stack específica
./scripts/run-stack.sh down srs

# Parar todas as stacks
./scripts/run-stack.sh down

# Reiniciar uma stack
./scripts/run-stack.sh restart default

# Limpeza completa (remove volumes e imagens)
./scripts/run-stack.sh clean
```

### Execução Manual

Você também pode executar manualmente com docker-compose:

```bash
cd app/

# Stack padrão (Nginx-RTMP + FFmpeg)
docker-compose up -d --build

# Stack SRS
docker-compose -f docker-compose.srs.yml up -d --build

# Stack GStreamer
docker-compose -f docker-compose.gstreamer.yml up -d --build

# Parar
docker-compose down
```

## 📊 Comparando Stacks

### URLs de Acesso

Independente da stack escolhida, as URLs são as mesmas:

| Serviço | URL | Credenciais |
|---------|-----|-------------|
| **Frontend** | http://localhost:3001 | - |
| **Backend API** | http://localhost:8080 | - |
| **RabbitMQ Management** | http://localhost:15672 | guest/guest |
| **HLS Streaming** | http://localhost:8081/hls/ | - |

### URLs Específicas da Stack SRS

Quando usando a stack SRS, você também tem acesso a:

| Serviço | URL | Descrição |
|---------|-----|-----------|
| **SRS HTTP API** | http://localhost:8080 | API REST do SRS |
| **SRS Stats** | http://localhost:8080/api/v1/summaries | Estatísticas |

### RTMP para OBS Studio

Para qualquer stack, configure no OBS:

- **Server**: `rtmp://localhost:1935/live`
- **Stream Key**: Obtido no frontend após criar stream

## 🧪 Cenários de Teste

### 1. Teste Básico de Funcionalidade

Execute com cada stack para validar funcionalidades básicas:

```bash
# Stack 1: Default
./scripts/run-stack.sh up default
# -> Teste: criar stream, transmitir via OBS, assistir no frontend

# Stack 2: SRS  
./scripts/run-stack.sh down
./scripts/run-stack.sh up srs
# -> Teste: criar stream, transmitir via OBS, assistir no frontend

# Stack 3: GStreamer
./scripts/run-stack.sh down  
./scripts/run-stack.sh up gstreamer
# -> Teste: criar stream, transmitir via OBS, assistir no frontend
```

### 2. Teste de Performance

Para cada stack, meça:

- **Latência**: Tempo entre transmissão (OBS) e visualização (frontend)
- **CPU/Memória**: Uso de recursos durante transmissão
- **Qualidade**: Análise da qualidade dos segmentos HLS gerados
- **Estabilidade**: Tempo de uptime durante transmissão longa

### 3. Teste de Carga

Use ferramentas como `ab` ou `k6` para simular múltiplos viewers:

```bash
# Simular 50 viewers simultâneos
ab -n 1000 -c 50 http://localhost:8081/hls/STREAM_KEY/master.m3u8

# Ou com curl para teste contínuo
for i in {1..10}; do
  curl -s http://localhost:8081/hls/STREAM_KEY/master.m3u8 > /dev/null &
done
```

## 📈 Coleta de Métricas

### Métricas Automáticas

O backend coleta automaticamente métricas para todas as stacks:

- **Spring Boot Actuator**: http://localhost:8080/actuator/metrics
- **Prometheus Endpoint**: http://localhost:8080/actuator/prometheus

### Métricas Específicas por Stack

#### Default Stack (Nginx + FFmpeg)
- Logs FFmpeg: Container `streaming-rtmp-server`
- Stats Nginx: http://localhost:8081/stat

#### SRS Stack
- API SRS: http://localhost:8080/api/v1/summaries
- Logs SRS: Container `streaming-srs-server`

#### GStreamer Stack
- Logs GStreamer: Container `streaming-rtmp-server`
- Debug GST: Variáveis de ambiente GST_DEBUG

### Comandos Úteis para Análise

```bash
# Ver logs específicos de transcodificação
docker logs streaming-rtmp-server -f | grep -i ffmpeg

# Ver logs de SRS
docker logs streaming-srs-server -f

# Monitorar CPU/Memória
docker stats

# Ver processos de transcodificação
docker exec streaming-rtmp-server ps aux | grep ffmpeg
```

## 🔧 Configurações Avançadas

### Variáveis de Ambiente

Personalize o comportamento através do arquivo `.env`:

```bash
# Configurações gerais
RTMP_PORT=1935
HLS_PORT=8081
HLS_RETENTION_HOURS=6

# Configurações de transcodificação
TRANSCODING_ENGINE=ffmpeg    # ou gstreamer
FFMPEG_PRESET=medium         # ultrafast, fast, medium, slow

# Configurações SRS
SRS_API_PORT=8080
```

### Troubleshooting

#### Stack não inicia
```bash
# Verificar portas em uso
sudo netstat -tulpn | grep -E ':(1935|8080|8081|3001)'

# Limpar containers órfãos
docker container prune -f
```

#### Transcodificação falha
```bash
# Verificar logs de FFmpeg
docker logs streaming-rtmp-server | grep -A 10 -B 10 "ffmpeg"

# Verificar segmentos HLS
docker exec streaming-rtmp-server ls -la /tmp/hls/
```

#### Performance baixa
```bash
# Verificar uso de recursos
docker stats --no-stream

# Verificar hardware acceleration (se disponível)
docker exec streaming-rtmp-server ffmpeg -hwaccels
```

## 📝 Resultados Esperados

Ao final da comparação, você deve ter dados sobre:

### Performance
- **Latência média** (segundos) por stack
- **Throughput máximo** (Mbps) por stack  
- **Uso de CPU/RAM** durante transcodificação
- **Tempo de inicialização** da transmissão

### Qualidade
- **Consistência do bitrate** gerado
- **Taxa de frames perdidos**
- **Qualidade visual** dos segmentos
- **Tempo de adaptação** entre qualidades

### Operacional
- **Facilidade de configuração**
- **Qualidade dos logs/debugging**
- **Estabilidade** durante uso prolongado
- **Features disponíveis**

Esses dados irão compor a análise comparativa do TCC, fornecendo base objetiva para recomendações de uso de cada stack em diferentes cenários.

---

**Última atualização**: 3 de fevereiro de 2026
# Plataforma de Streaming — TCC

Plataforma de streaming ao vivo desenvolvida como Trabalho de Conclusão de Curso. O objetivo principal é comparar quatro combinações de servidor RTMP × transcodificador em uma matriz 2²:

| | FFmpeg | GStreamer |
|---|---|---|
| **Nginx-RTMP** | Combinação 1 | Combinação 2 |
| **SRS** | Combinação 3 | Combinação 4 |

O streamer transmite via OBS Studio → o backend valida a stream key → o transcodificador gera HLS em 4 qualidades → o espectador assiste via browser com ABR automático.

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 3.2, Spring Data JPA, Spring AMQP |
| Banco de dados | PostgreSQL 16 |
| Mensageria | RabbitMQ 3 |
| Servidor RTMP A | Nginx + nginx-rtmp-module (container customizado) |
| Servidor RTMP B | SRS 6 |
| Transcodificador A | FFmpeg |
| Transcodificador B | GStreamer |
| Frontend | React 18, TypeScript, Vite, HLS.js, Plyr |
| Notificações | Server-Sent Events (SSE) |
| Infraestrutura | Docker Compose |

---

## Instalação e Configuração

### Pré-requisitos

- Docker e Docker Compose instalados
- Portas livres: `1935` (RTMP), `8080` (API), `8081` (HLS), `3001` (frontend), `5672` e `15672` (RabbitMQ)

### 1. Clonar e configurar variáveis

```bash
git clone <repo>
cd tcc/app
cp .env.example .env
```

O `.env` já vem com valores padrão funcionais para desenvolvimento local. As variáveis relevantes:

```env
# Transcodificador ativo: ffmpeg | gstreamer
TRANSCODER=ffmpeg

# Portas expostas
BACKEND_PORT=8080
RTMP_PORT=1935
HLS_PORT=8081
FRONTEND_PORT=3001
```

### 2. Subir os serviços

```bash
docker compose up -d --build
```

Aguardar todos os containers ficarem healthy. Para verificar:

```bash
docker compose ps
```

### 3. Verificar se o backend está pronto

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

### 4. Acessar o frontend

Abrir `http://localhost:3001` no browser.

### Trocar o transcodificador

Editar `.env`:
```env
TRANSCODER=gstreamer  # ou ffmpeg
```

Recriar o container RTMP:
```bash
docker compose up -d --force-recreate rtmp-server
```

---

## Configuração do OBS Studio

1. No OBS, abrir **Configurações → Transmissão**
2. Serviço: **Personalizado**
3. Servidor: `rtmp://localhost:1935/live`
4. Chave de transmissão: copiar do painel do streamer em `http://localhost:3001`

> A stream key é gerada pelo sistema ao criar uma transmissão. Sem uma key válida o Nginx rejeita a conexão.

**Configurações de vídeo recomendadas (OBS → Saída → Codificador):**
- Encoder: x264
- Taxa de bits: 4000–6000 kbps
- Keyframe interval: 2 segundos
- Perfil: main ou high

**Fluxo completo:**
1. Criar transmissão no frontend → receber stream key
2. Configurar OBS com a key
3. Clicar "Iniciar Transmissão" no OBS
4. O painel muda para LIVE em ~8 segundos (tempo de estabilização do HLS)
5. Compartilhar o link de espectador

---

## Principais Desafios

### Latência de inicialização do HLS
O FFmpeg precisa receber o primeiro keyframe do OBS antes de abrir a conexão RTMP interna. Isso causava um loop de falha onde cada tentativa levava ~23 segundos (timeout + respawn pelo nginx-rtmp). Solução: o script de transcodificação implementa retry interno com até 10 tentativas e espera de 3 segundos entre elas, eliminando a dependência do respawn externo.

### Reconexão do streamer com stream em status ENDED
Ao reconectar o OBS após uma queda, o callback `on_publish` chegava com a stream em status `ENDED`, e o backend rejeitava a transmissão. Solução: a entidade `Stream.start()` aceita qualquer status (não apenas `WAITING`), permitindo reconexão sem necessidade de reiniciar manualmente a stream pelo frontend.

### Connection leak no HikariCP com SSE
Com `spring.jpa.open-in-view=true` (padrão do Spring), cada conexão SSE mantinha uma conexão de banco aberta pelo tempo total da sessão (até 30 minutos por viewer). Solução: desabilitado com `spring.jpa.open-in-view: false`.

### Double-decrement de viewers
O callback `onDisconnect` do SSE e o endpoint REST `/leave` podiam ser chamados simultaneamente ao fechar a aba, decrementando o contador duas vezes. Mitigado com flag de idempotência por `viewerId` no `SseEmitterManager`.

### Segurança no exec do nginx-rtmp
A diretiva `exec /usr/local/bin/transcode.sh $name` passa a stream key sem aspas. Com IDs em UUID (apenas hex e hífens) o risco é baixo, mas a validação no script (`^[a-zA-Z0-9_-]+$`) bloqueia qualquer chave fora desse padrão antes de chegar ao FFmpeg.

# TODO - Plataforma de Streaming com Análise Comparativa

## 📋 Informações do Projeto

**Nome do Projeto**: Plataforma de Streaming com Análise Comparativa de Desempenho  
**Tipo**: TCC (Trabalho de Conclusão de Curso)  
**Período**: Janeiro 2026 - Junho 2026 (20 semanas)  
**Status**: 🚀 Iniciando Desenvolvimento

## 🎯 Objetivo Principal

Desenvolver uma plataforma de streaming de vídeo ao vivo que permita avaliar e comparar o desempenho de diferentes tecnologias e bibliotecas para as mesmas funcionalidades, fornecendo análise quantitativa e qualitativa para escolha de tecnologias em projetos de streaming.

## 📅 Roadmap de Desenvolvimento

### 📦 Fase 1: Setup e Estruturação (Semana 1-2)
**Objetivo**: Preparar ambiente de desenvolvimento e estrutura base do projeto

#### 1.1 Configuração do Ambiente
- [x] Instalar Docker e Docker Compose (já estava instalado)
- [x] Instalar Java 21 JDK
- [x] Instalar Node.js 20+ e npm (já estava instalado)
- [x] Instalar Maven
- [x] Configurar VS Code
- [ ] Instalar OBS Studio para testes (usuário vai instalar)

#### 1.2 Estrutura do Projeto
- [x] Criar estrutura de pastas completa
  ```
  tcc/
  ├── app/
  │   ├── backend/
  │   │   └── streaming-platform/    # ⚠️ Projeto Spring Boot ÚNICO
  │   │       ├── pom.xml
  │   │       └── src/main/java/com/tcc/streaming/
  │   │           ├── StreamingPlatformApplication.java
  │   │           ├── common/        # Código compartilhado
  │   │           ├── stream/        # Stream Service
  │   │           ├── consumer/      # Consumer Service
  │   │           └── metrics/       # Metrics Service
  │   ├── frontend/
  │   │   └── web/
  │   └── config/
  │       ├── nginx/
  │       ├── prometheus/
  │       └── grafana/
  ├── docker/
  │   ├── nginx-rtmp/
  │   ├── rabbitmq/
  │   ├── redis/
  │   ├── postgres/
  │   └── monitoring/
  ├── docs/
  ├── scripts/
  └── docker-compose/
  ```
- [x] Inicializar repositório Git
- [x] Criar arquivo `.gitignore`
- [x] Criar README.md inicial
- [x] Documentar estrutura de pastas (utils.md criado)

#### 1.3 Docker Base
- [x] Criar `docker-compose.yml` base
- [x] Configurar rede Docker (`streaming-network`)
- [x] Definir volumes persistentes
- [x] Testar comunicação entre containers

### 🔧 Fase 2: Backend - Stream Service (Semana 3-4)
**Objetivo**: Implementar serviço principal de gerenciamento de streams

#### 2.1 Projeto Spring Boot
- [x] Criar projeto Spring Boot 3.x com Maven
- [x] Adicionar dependências:
  - [x] Spring Web
  - [x] Spring Data JPA
  - [x] Spring Data Redis
  - [x] Spring AMQP (RabbitMQ)
  - [x] Spring WebSocket
  - [x] Spring Boot Actuator
  - [x] PostgreSQL Driver
  - [x] Lombok
  - [x] Validation API
- [x] Configurar `application.yml`
- [x] Configurar perfis (dev, prod)

#### 2.2 Modelagem de Dados
- [x] Criar entidade `Stream`
  - [x] id (UUID)
  - [x] title
  - [x] description
  - [x] streamKey (unique)
  - [x] status (enum: WAITING, LIVE, ENDED)
  - [x] createdAt
  - [x] startedAt
  - [x] endedAt
  - [x] currentViewers
  - [x] viewersPeak
- [x] Criar entidade `StreamEvent`
  - [x] id
  - [x] streamId
  - [x] eventType (enum: CREATED, STARTED, ENDED, VIEWER_JOINED, VIEWER_LEFT)
  - [x] metadata (JSON/TEXT)
  - [x] timestamp
- [x] Criar entidade `ViewerSession`
  - [x] id
  - [x] streamId
  - [x] viewerId (session)
  - [x] joinedAt
  - [x] leftAt
- [x] Criar repositórios JPA
- [x] Adicionar índices no banco de dados

#### 2.3 API REST
- [x] **POST /api/streams** (criar stream)
  - [x] Validar entrada (título, descrição)
  - [x] Gerar stream key única (UUID)
  - [x] Salvar no PostgreSQL
  - [x] Cachear no Redis
  - [x] Publicar evento `stream_created`
  - [x] Retornar credenciais RTMP
- [x] **GET /api/streams/{id}**
  - [x] Buscar stream por ID
  - [x] Retornar detalhes completos
  - [x] Cachear resposta
- [x] **GET /api/streams/{id}/status**
  - [x] Retornar status atual
  - [x] Retornar contador de viewers
  - [x] Consultar Redis (cache)
- [x] **DELETE /api/streams/{id}**
  - [x] Validar permissão (sem auth por enquanto)
  - [x] Marcar como ENDED
  - [x] Limpar cache
  - [x] Publicar evento `stream_ended`
- [x] **POST /api/streams/validate** (Nginx-RTMP callback)
  - [x] Receber stream key
  - [x] Validar no banco/cache
  - [x] Retornar boolean
- [x] **GET /api/streams/callback/publish** (Nginx-RTMP callback)
  - [x] Validar stream key
  - [x] Retornar 200 (aceita) ou 403 (rejeita)
- [x] **GET /api/streams/callback/publish_done** (Nginx-RTMP callback)
  - [x] Atualizar status para LIVE
  - [x] Publicar evento `stream_started`
- [x] **GET /api/streams/callback/done** (Nginx-RTMP callback)
  - [x] Atualizar status para ENDED
  - [x] Publicar evento `stream_ended`

#### 2.4 Configuração Redis
- [x] Configurar conexão com Redis
- [x] Configurar cache manager com TTL
- [x] Implementar cache de streams ativas (`@Cacheable`, `@CacheEvict`)
- [ ] Implementar cache de sessões de viewers ⏳ Fase 3
- [x] Definir TTL apropriado (10min padrão configurado)
- [ ] Criar serviço de gerenciamento de cache ⏳ Optimization Phase

#### 2.5 Configuração RabbitMQ
- [x] Configurar conexão com RabbitMQ
- [x] Criar exchange `stream.exchange` (topic)
- [x] Criar filas:
  - [x] `stream.events` (eventos de stream)
  - [x] `metrics.events` (eventos de métricas)
- [x] Configurar bindings com routing keys:
  - [x] `stream.created`
  - [x] `stream.started`
  - [x] `stream.ended`
  - [x] `viewer.joined`
  - [x] `viewer.left`
- [x] Implementar Producer de eventos (EventPublisher criado)
- [x] Adicionar serialização JSON
- [ ] Configurar Dead Letter Queue ⏳ Optimization Phase

#### 2.6 WebSocket
- [x] Configurar STOMP over WebSocket
- [x] Endpoint: `/ws` com SockJS
- [x] Criar tópico: `/topic/stream/{streamId}/status`
- [x] Criar tópico: `/topic/stream/{streamId}/viewers`
- [x] Implementar lógica de broadcast de eventos (controller criado)
- [x] Handlers para join/leave (implementado)
- [x] Publicar eventos no RabbitMQ (EventPublisher integrado)
- [ ] Integrar contador de viewers real-time ⏳ Fase 3

### 📹 Fase 3: Infraestrutura de Streaming (Semana 5-6)
**Objetivo**: Configurar ingestão RTMP, transcodificação e serving HLS

#### 3.1 Nginx-RTMP
- [x] Criar Dockerfile para Nginx-RTMP
- [x] Configurar `nginx.conf`:
  - [x] Porta RTMP: 1935
  - [x] Application: `live`
  - [x] Callbacks HTTP para Stream Service
  - [ ] Exec FFmpeg on publish (pendente - fase 3.2)
- [x] Configurar autenticação via callback
- [x] Adicionar ao docker-compose.yml
- [x] Criar abstração com interfaces (RtmpServerGateway) ⭐
- [x] Implementar testes de integração
- [ ] Testar conexão RTMP com OBS (teste manual pendente)

#### 3.2 FFmpeg Transcodificação
- [x] Criar abstração com interfaces (TranscoderGateway) ⭐
- [x] Implementar FFmpegTranscoderGateway
- [x] Criar script de transcodificação (transcode.sh)
- [x] Configurar presets de qualidade:
  - [x] 1080p: 1920x1080, 5000kbps, H.264 medium
  - [x] 720p: 1280x720, 2800kbps, H.264 medium
  - [x] 480p: 854x480, 1400kbps, H.264 fast
  - [x] 360p: 640x360, 800kbps, H.264 faster
- [x] Gerar segmentos HLS (.ts)
- [x] Gerar playlists HLS (.m3u8)
- [x] Configurar duração de segmentos (6 segundos)
- [x] Criar master playlist com variantes
- [x] Configurar logging de FFmpeg
- [x] Integrar com Nginx-RTMP via exec_push
- [x] Criar testes unitários (QualityPresetTest)
- [ ] Testar transcodificação end-to-end com OBS (teste manual pendente)

#### 3.3 Nginx HLS Server
- [x] Configurar Nginx para servir HLS (já configurado no nginx.conf)
- [x] Porta: 8081
- [x] Location `/hls/` aponta para pasta de segmentos
- [x] Configurar headers CORS ⭐
- [x] Configurar cache headers ⭐
- [ ] Testar acesso a playlists com streaming real (aguardando teste OBS) (ainda nao)

#### 3.4 PostgreSQL
- [x] Criar Dockerfile para PostgreSQL
- [x] Configurar banco de dados `streaming_db`
- [x] Criar usuário e senha
- [x] Configurar volume persistente
- [x] Criar scripts de inicialização (schema)
- [x] Adicionar ao docker-compose.yml

#### 3.5 Redis
- [x] Configurar Redis container
- [x] Porta: 6379
- [x] Configurar persistência (RDB)
- [x] Configurar maxmemory policy
- [x] Adicionar ao docker-compose.yml

#### 3.6 RabbitMQ
- [x] Configurar RabbitMQ container
- [x] Porta AMQP: 5672
- [x] Porta Management: 15672
- [x] Configurar credenciais
- [x] Habilitar management plugin
- [x] Adicionar ao docker-compose.yml

### 🎨 Fase 4: Frontend React (Semana 7-8)
**Objetivo**: Criar interface web para streamers e espectadores

#### 4.1 Setup React
- [x] Criar projeto React com Vite (/frontend)
- [x] Configurar TypeScript
- [x] Adicionar dependências:
  - [x] React Router
  - [x] Axios
  - [x] Video.js ou HLS.js
  - [x] STOMP client (WebSocket)
  - [x] Tailwind CSS
  - [x] Shadcn
  - [x] Lucide Icons
- [x] Configurar estrutura de pastas
- [x] Configurar proxy para backend

#### 4.2 Página Inicial
- [x] Criar componente `HomePage`
- [x] Botão "Iniciar Streaming" destacado
- [x] Seção de explicação sobre a plataforma
- [x] Design responsivo

#### 4.3 Criação de Stream
- [x] Criar componente `CreateStreamModal`
- [x] Formulário: título e descrição
- [x] Validação de inputs
- [x] Integração com API: POST /api/streams
- [x] Loading states
- [x] Error handling
- [x] Redirecionamento para painel após criação

#### 4.4 Painel do Streamer
- [x] Criar componente `StreamerDashboard`
- [x] Exibir URL RTMP: `rtmp://localhost:1935/live`
- [x] Exibir Stream Key (com botão copiar)
- [x] Exibir link compartilhável
- [x] Instruções de configuração do OBS
- [x] Status da stream (WAITING, LIVE, ENDED)
- [x] Contador de viewers online
- [x] Botão "Encerrar Stream"
- [x] WebSocket connection para atualizações

#### 4.5 Player de Vídeo
- [x] Criar componente `VideoPlayer`
- [x] Integrar HLS.js
- [x] Configurar player responsivo
- [x] Adaptive Bitrate Streaming (ABR)
- [x] Controles de reprodução
- [x] Indicador de qualidade atual
- [x] Tratamento de erros (stream offline)
- [x] Loading states

#### 4.6 Página de Visualização
- [x] Criar componente `WatchPage`
- [x] Rota: `/watch/:streamId`
- [x] Carregar informações da stream
- [x] Renderizar VideoPlayer
- [x] Exibir título e descrição
- [x] Contador de viewers
- [x] Mensagem se stream ainda não iniciou
- [x] Mensagem se stream encerrou
- [x] WebSocket para atualizações

#### 4.7 WebSocket Integration
- [x] Configurar STOMP client
- [x] Conectar ao endpoint `/ws`
- [x] Subscrever a tópicos de stream
- [x] Enviar evento `viewer_joined` ao entrar
- [x] Enviar evento `viewer_left` ao sair
- [x] Atualizar UI baseado em eventos recebidos
- [x] Reconexão automática

### 🔄 Fase 5: Consumer Service (Semana 9-10)
**Objetivo**: Processar eventos assíncronos do message broker

#### 5.1 Projeto Spring Boot
- [x] Criar projeto consumer-service
- [x] Adicionar dependências (AMQP, JPA, Redis)
- [x] Configurar conexão com RabbitMQ
- [x] Configurar conexão com PostgreSQL

#### 5.2 Event Consumers
- [x] Criar Consumer para `stream_created`
  - [x] Registrar métricas iniciais
  - [x] Log do evento
- [x] Criar Consumer para `stream_started`
  - [x] Atualizar estatísticas
  - [x] Iniciar coleta de métricas detalhadas
  - [x] Registrar timestamp exato
- [x] Criar Consumer para `stream_ended`
  - [x] Calcular duração total
  - [x] Calcular estatísticas finais
  - [x] Limpar cache de sessão
  - [x] Persistir histórico
- [x] Criar Consumer para `viewer_joined`
  - [x] Incrementar contador
  - [x] Registrar evento
  - [x] Atualizar pico se necessário
- [x] Criar Consumer para `viewer_left`
  - [x] Decrementar contador
  - [x] Registrar evento
- [x] Configurar concorrência de consumers
- [x] Implementar retry policy
- [x] Implementar error handling

#### 5.3 Tarefas Agendadas
- [x] Criar scheduled task para limpar arquivos HLS antigos
  - [x] Remover segmentos de streams encerradas (após 6 horas)
  - [x] Liberar espaço em disco
- [x] Criar scheduled task para agregação de métricas
  - [x] Calcular médias diárias
  - [x] Persistir estatísticas agregadas

### 📊 Fase 6: Monitoramento (Semana 11-12)
**Objetivo**: Implementar observabilidade completa do sistema

#### 6.1 Prometheus
- [ ] Configurar Prometheus container
- [ ] Criar `prometheus.yml`:
  - [ ] Scrape stream-service (Spring Boot Actuator)
  - [ ] Scrape consumer-service
  - [ ] Scrape metrics-service
  - [ ] Intervalo: 15 segundos
- [ ] Adicionar ao docker-compose.yml
- [ ] Testar acesso à UI (porta 9090)

#### 6.2 Grafana
- [ ] Configurar Grafana container
- [ ] Porta: 3000
- [ ] Configurar datasource Prometheus
- [ ] Criar dashboard: **Visão Geral do Sistema**
  - [ ] Total de streams ativas
  - [ ] Total de viewers online
  - [ ] CPU e memória por serviço
  - [ ] Latência de APIs
- [ ] Criar dashboard: **Métricas de Streaming**
  - [ ] FPS médio
  - [ ] Bitrate de entrada/saída
  - [ ] Latência RTMP → HLS
  - [ ] Taxa de transcodificação
- [ ] Criar dashboard: **Recursos de Infraestrutura**
  - [ ] Uso de CPU/memória por container
  - [ ] I/O de disco
  - [ ] Uso de rede
- [ ] Exportar dashboards para JSON
- [ ] Adicionar ao docker-compose.yml

#### 6.3 Metrics Service
- [ ] Criar projeto metrics-service
- [ ] Implementar coleta de métricas do FFmpeg
  - [ ] Parsear logs do FFmpeg
  - [ ] Extrair FPS, bitrate, frame drops
  - [ ] Expor para Prometheus
- [ ] Implementar coleta de métricas do Nginx-RTMP
  - [ ] Consultar módulo de stats
  - [ ] Extrair conexões ativas, bandwidth
  - [ ] Expor para Prometheus
- [ ] Criar métricas customizadas:
  - [ ] `streaming_latency_seconds` (RTMP to HLS)
  - [ ] `transcoding_fps`
  - [ ] `active_viewers_count`
  - [ ] `stream_duration_seconds`
  - [ ] `peak_viewers`
- [ ] Configurar Spring Boot Actuator
- [ ] Expor endpoint `/actuator/prometheus`

#### 6.4 Logging
- [ ] Configurar Logback para JSON logging
- [ ] Diferentes níveis por ambiente (dev: DEBUG, prod: INFO)
- [ ] Centralizar logs (opcional: ELK Stack)
- [ ] Rotação de logs

### 🔄 Fase 7: Alternativas Tecnológicas (Semana 13-15)
**Objetivo**: Implementar variantes com diferentes tecnologias

#### 7.1 Alternativa: Redis Streams (Message Broker)
- [ ] Criar `docker-compose.redis-streams.yml`
- [ ] Implementar Producer com Redis Streams
- [ ] Implementar Consumer com Redis Streams
- [ ] Configurar consumer groups
- [ ] Testar funcionamento completo
- [ ] Coletar métricas de performance
- [ ] Documentar diferenças

#### 7.2 Alternativa: NATS (Message Broker)
- [ ] Criar `docker-compose.nats.yml`
- [ ] Configurar NATS container
- [ ] Implementar Producer com NATS
- [ ] Implementar Consumer com NATS
- [ ] Configurar JetStream para persistência
- [ ] Testar funcionamento completo
- [ ] Coletar métricas de performance
- [ ] Documentar diferenças

#### 7.3 Alternativa: SRS (RTMP Server)
- [ ] Criar `docker-compose.srs.yml`
- [ ] Configurar SRS container
- [ ] Configurar callbacks HTTP
- [ ] Adaptar transcodificação
- [ ] Testar com OBS
- [ ] Coletar métricas de performance
- [ ] Comparar com Nginx-RTMP
- [ ] Documentar diferenças

#### 7.4 Alternativa: GStreamer (Transcodificação)
- [ ] Criar pipeline GStreamer
- [ ] Configurar presets de qualidade
- [ ] Gerar segmentos HLS
- [ ] Integrar com Nginx-RTMP
- [ ] Testar funcionamento
- [ ] Coletar métricas de performance
- [ ] Comparar com FFmpeg
- [ ] Documentar diferenças

### 🧪 Fase 8: Testes e Otimização (Semana 16-17)
**Objetivo**: Validar sistema e coletar dados para análise comparativa

#### 8.1 Testes Funcionais
- [ ] Testar criação de stream
- [ ] Testar transmissão RTMP
- [ ] Testar visualização HLS
- [ ] Testar contador de viewers
- [ ] Testar encerramento de stream
- [ ] Testar reconexão
- [ ] Testar expiração de streams

#### 8.2 Cenários de Teste de Carga
- [ ] **Cenário 1: Baseline**
  - [ ] 1 streamer, 10 viewers
  - [ ] Coletar todas as métricas
- [ ] **Cenário 2: Média Carga**
  - [ ] 1 streamer, 100 viewers
  - [ ] Coletar todas as métricas
- [ ] **Cenário 3: Alta Carga**
  - [ ] 1 streamer, 500 viewers
  - [ ] Coletar todas as métricas
- [ ] **Cenário 4: Múltiplas Streams**
  - [ ] 5 streamers simultâneos, 20 viewers cada
  - [ ] Coletar todas as métricas
- [ ] **Cenário 5: Stress Test**
  - [ ] 1 streamer, 1000 viewers
  - [ ] Identificar limites do sistema

#### 8.3 Ferramentas de Teste
- [ ] Configurar JMeter ou K6 para testes de carga
- [ ] Criar scripts de teste automatizados
- [ ] Simular múltiplos viewers
- [ ] Coletar logs durante testes

#### 8.4 Análise Comparativa
- [ ] Executar cada cenário com:
  - [ ] RabbitMQ vs Redis Streams vs NATS
  - [ ] Nginx-RTMP vs SRS
  - [ ] FFmpeg vs GStreamer
- [ ] Coletar dados de:
  - [ ] Latência (RTMP → HLS)
  - [ ] Throughput do message broker
  - [ ] Uso de CPU e memória
  - [ ] I/O de disco
  - [ ] FPS e qualidade de vídeo
- [ ] Gerar gráficos comparativos
- [ ] Documentar resultados

#### 8.5 Otimizações
- [ ] Otimizar queries do banco de dados
- [ ] Ajustar configurações de cache
- [ ] Tunar FFmpeg presets
- [ ] Ajustar configurações de JVM
- [ ] Otimizar tamanho de segmentos HLS
- [ ] Revisar configuração de RabbitMQ/alternativas

### 📖 Fase 9: Documentação Final e TCC (Semana 18-20)
**Objetivo**: Documentar projeto e escrever TCC

#### 9.1 Documentação Técnica
- [ ] Atualizar README.md
- [ ] Criar guia de instalação detalhado
- [ ] Criar guia de configuração
- [ ] Documentar APIs (Swagger/OpenAPI)
- [ ] Documentar arquitetura completa
- [ ] Criar diagramas finais
- [ ] Documentar decisões técnicas
- [ ] Criar troubleshooting guide

#### 9.2 Redação do TCC
- [ ] **Introdução**
  - [ ] Contextualização
  - [ ] Motivação
  - [ ] Objetivos
  - [ ] Justificativa
- [ ] **Referencial Teórico**
  - [ ] Streaming de vídeo (conceitos)
  - [ ] Protocolos (RTMP, HLS)
  - [ ] Transcodificação de vídeo
  - [ ] Arquitetura de microserviços
  - [ ] Message brokers
  - [ ] Trabalhos relacionados
- [ ] **Metodologia**
  - [ ] Arquitetura proposta
  - [ ] Tecnologias utilizadas
  - [ ] Métricas definidas
  - [ ] Cenários de teste
  - [ ] Processo de coleta de dados
- [ ] **Implementação**
  - [ ] Descrição dos componentes
  - [ ] Decisões técnicas
  - [ ] Desafios enfrentados
  - [ ] Soluções implementadas
- [ ] **Resultados**
  - [ ] Análise quantitativa
  - [ ] Gráficos comparativos
  - [ ] Análise qualitativa
  - [ ] Trade-offs identificados
  - [ ] Recomendações
- [ ] **Conclusão**
  - [ ] Objetivos alcançados
  - [ ] Aprendizados
  - [ ] Trabalhos futuros
  - [ ] Contribuições

#### 9.3 Apresentação
- [ ] Criar slides (PowerPoint/Google Slides)
- [ ] Preparar demonstração ao vivo
- [ ] Criar vídeo de demonstração (backup)
- [ ] Ensaiar apresentação
- [ ] Preparar para perguntas

#### 9.4 Entregáveis Finais
- [ ] Código-fonte no GitHub (público ou privado)
- [ ] Documentação completa
- [ ] TCC escrito (PDF)
- [ ] Apresentação (slides)
- [ ] Vídeo demonstrativo (opcional)
- [ ] Dados e gráficos de análise

## 🎯 Critérios de Comparação

Para cada tecnologia alternativa testada, avaliar:

### Performance
- [ ] Throughput (mensagens/segundo para brokers, FPS para transcodificação)
- [ ] Latência (end-to-end: RTMP → visualização)
- [ ] Uso de CPU (por container e total)
- [ ] Uso de memória (por container e total)
- [ ] I/O de disco (leitura/escrita)
- [ ] Uso de rede (bandwidth)

### Escalabilidade
- [ ] Capacidade máxima de viewers simultâneos
- [ ] Degradação de performance com carga crescente
- [ ] Possibilidade de escalamento horizontal

### Confiabilidade
- [ ] Taxa de erros
- [ ] Comportamento sob falhas
- [ ] Tempo de recuperação
- [ ] Perda de mensagens/frames

### Facilidade de Uso
- [ ] Complexidade de configuração
- [ ] Qualidade da documentação
- [ ] Curva de aprendizado
- [ ] Ferramentas de debugging disponíveis

### Comunidade e Suporte
- [ ] Atividade do projeto (commits recentes)
- [ ] Tamanho da comunidade
- [ ] Disponibilidade de recursos (tutoriais, exemplos)
- [ ] Última atualização

## 🛡️ Aspectos Adicionais

### Segurança
- [ ] Validação rigorosa de stream keys
- [ ] Rate limiting em APIs críticas
- [ ] Sanitização de inputs
- [ ] CORS configurado corretamente
- [ ] Secrets management (environment variables)

### Resiliência
- [ ] Health checks em todos os serviços
- [ ] Retry policies configuradas
- [ ] Circuit breakers (opcional)
- [ ] Graceful shutdown
- [ ] Dead letter queues

### Qualidade de Código
- [x] Testes unitários (cobertura básica implementada)
- [x] Testes de integração (testes E2E com Testcontainers)
- [x] **Testes E2E para backend** ✅ 24 testes passando
  - [x] StreamControllerE2ETest (11 testes)
  - [x] StreamControllerRedisE2ETest (6 testes com Redis real)
  - [x] NginxCallbackControllerE2ETest (7 testes)
  - [x] Testcontainers (PostgreSQL + Redis)
  - [x] Correção de bugs de serialização Redis
  - [x] Correção de concorrência entre suítes de teste
- [ ] Testes E2E para frontend
- [ ] Code review
- [ ] Análise estática de código (SonarQube - opcional)

### UX/UI
- [ ] Design responsivo (mobile, tablet, desktop)
- [ ] Loading states em todas as operações assíncronas
- [ ] Error handling com mensagens claras
- [ ] Feedback visual para ações do usuário
- [ ] Acessibilidade (ARIA labels, contraste)

## 📚 Recursos e Referências

### Documentação Técnica
- [Nginx-RTMP Module](https://github.com/arut/nginx-rtmp-module)
- [SRS Documentation](https://github.com/ossrs/srs)
- [FFmpeg Documentation](https://ffmpeg.org/documentation.html)
- [GStreamer Documentation](https://gstreamer.freedesktop.org/documentation/)
- [HLS Specification (RFC 8216)](https://tools.ietf.org/html/rfc8216)
- [Spring Boot Docs](https://spring.io/projects/spring-boot)
- [React Docs](https://react.dev/)
- [RabbitMQ Docs](https://www.rabbitmq.com/documentation.html)
- [Redis Streams](https://redis.io/docs/data-types/streams/)
- [NATS Docs](https://docs.nats.io/)
- [Prometheus Docs](https://prometheus.io/docs/)
- [Grafana Docs](https://grafana.com/docs/)

### Tutoriais e Artigos
- HLS Streaming with FFmpeg
- Building Microservices with Spring Boot
- WebSocket with STOMP
- Load Testing with K6

## 🚀 Próximos Passos Imediatos

### Sprint 1 (Esta Semana)
1. [ ] Criar estrutura completa de pastas do projeto
2. [ ] Inicializar repositório Git
3. [ ] Criar `docker-compose.yml` base com PostgreSQL, Redis e RabbitMQ
4. [ ] Inicializar projeto Spring Boot (stream-service)
5. [ ] Inicializar projeto React (frontend)
6. [ ] Testar comunicação básica entre containers

### Sprint 2 (Próxima Semana)
1. [ ] Implementar models e repositórios JPA
2. [ ] Implementar API REST básica (criar stream)
3. [ ] Configurar Nginx-RTMP container
4. [ ] Criar página inicial do frontend
5. [ ] Testar criação de stream ponta a ponta

## 📈 Acompanhamento

**Última Atualização**: 02/02/2026  
**Progresso Geral**: 3% (11/400+ tarefas)  
**Fase Atual**: Fase 1 - Setup e Estruturação (100% completa ✅)  
**Prazo Final**: Junho/2026

---

💡 **Dica**: Marque os checkboxes conforme completar as tarefas e mantenha este documento atualizado semanalmente!

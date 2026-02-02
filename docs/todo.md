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
- [ ] Criar projeto Spring Boot 3.x com Maven
- [ ] Adicionar dependências:
  - [ ] Spring Web
  - [ ] Spring Data JPA
  - [ ] Spring Data Redis
  - [ ] Spring AMQP (RabbitMQ)
  - [ ] Spring WebSocket
  - [ ] Spring Boot Actuator
  - [ ] PostgreSQL Driver
  - [ ] Lombok
  - [ ] Validation API
- [ ] Configurar `application.yml`
- [ ] Configurar perfis (dev, prod)

#### 2.2 Modelagem de Dados
- [ ] Criar entidade `Stream`
  - [ ] id (UUID)
  - [ ] title
  - [ ] description
  - [ ] streamKey (unique)
  - [ ] status (enum: WAITING, LIVE, PAUSED, ENDED, ERROR, EXPIRED)
  - [ ] createdAt
  - [ ] startedAt
  - [ ] endedAt
  - [ ] viewersPeak
- [ ] Criar entidade `StreamEvent`
  - [ ] id
  - [ ] streamId
  - [ ] eventType (enum)
  - [ ] metadata (JSON)
  - [ ] timestamp
- [ ] Criar entidade `ViewerSession`
  - [ ] id
  - [ ] streamId
  - [ ] viewerId (session)
  - [ ] joinedAt
  - [ ] leftAt
- [ ] Criar repositórios JPA
- [ ] Adicionar índices no banco de dados

#### 2.3 API REST
- [ ] **POST /api/streams/create**
  - [ ] Validar entrada (título, descrição)
  - [ ] Gerar stream key única (UUID)
  - [ ] Salvar no PostgreSQL
  - [ ] Cachear no Redis
  - [ ] Publicar evento `stream_created`
  - [ ] Retornar credenciais RTMP
- [ ] **GET /api/streams/{id}**
  - [ ] Buscar stream por ID
  - [ ] Retornar detalhes completos
  - [ ] Cachear resposta
- [ ] **GET /api/streams/{id}/status**
  - [ ] Retornar status atual
  - [ ] Retornar contador de viewers
  - [ ] Consultar Redis (cache)
- [ ] **DELETE /api/streams/{id}**
  - [ ] Validar permissão
  - [ ] Marcar como ENDED
  - [ ] Limpar cache
  - [ ] Publicar evento `stream_ended`
- [ ] **POST /api/streams/callback/publish** (Nginx-RTMP callback)
  - [ ] Receber stream key
  - [ ] Validar no banco/cache
  - [ ] Retornar 200 (aceita) ou 403 (rejeita)
- [ ] **POST /api/streams/callback/publish_done** (Nginx-RTMP callback)
  - [ ] Atualizar status para LIVE
  - [ ] Publicar evento `stream_started`
  - [ ] Notificar via WebSocket
- [ ] **POST /api/streams/callback/done** (Nginx-RTMP callback)
  - [ ] Atualizar status para ENDED
  - [ ] Calcular duração
  - [ ] Publicar evento `stream_ended`

#### 2.4 Configuração Redis
- [ ] Configurar conexão com Redis
- [ ] Implementar cache de streams ativas
- [ ] Implementar cache de sessões de viewers
- [ ] Definir TTL apropriado (2 horas para streams WAITING)
- [ ] Criar serviço de gerenciamento de cache

#### 2.5 Configuração RabbitMQ
- [ ] Configurar conexão com RabbitMQ
- [ ] Criar exchange `streaming.events` (topic)
- [ ] Criar filas:
  - [ ] `stream.events.all` (todos os eventos)
  - [ ] `stream.events.started` (stream iniciada)
  - [ ] `stream.events.ended` (stream encerrada)
  - [ ] `stream.events.viewers` (viewers entraram/saíram)
- [ ] Implementar Producer de eventos
- [ ] Adicionar serialização JSON
- [ ] Configurar Dead Letter Queue

#### 2.6 WebSocket
- [ ] Configurar STOMP over WebSocket
- [ ] Endpoint: `/ws`
- [ ] Criar tópico: `/topic/stream/{streamId}/status`
- [ ] Criar tópico: `/topic/stream/{streamId}/viewers`
- [ ] Implementar lógica de broadcast de eventos
- [ ] Registrar/desregistrar viewers

### 📹 Fase 3: Infraestrutura de Streaming (Semana 5-6)
**Objetivo**: Configurar ingestão RTMP, transcodificação e serving HLS

#### 3.1 Nginx-RTMP
- [ ] Criar Dockerfile para Nginx-RTMP
- [ ] Configurar `nginx.conf`:
  - [ ] Porta RTMP: 1935
  - [ ] Application: `live`
  - [ ] Callbacks HTTP para Stream Service
  - [ ] Exec FFmpeg on publish
- [ ] Configurar autenticação via callback
- [ ] Adicionar ao docker-compose.yml
- [ ] Testar conexão RTMP com OBS

#### 3.2 FFmpeg Transcodificação
- [ ] Criar script de transcodificação
- [ ] Configurar presets de qualidade:
  - [ ] 1080p: 1920x1080, 5000kbps, H.264 medium
  - [ ] 720p: 1280x720, 2800kbps, H.264 medium
  - [ ] 480p: 854x480, 1400kbps, H.264 fast
  - [ ] 360p: 640x360, 800kbps, H.264 faster
- [ ] Gerar segmentos HLS (.ts)
- [ ] Gerar playlists HLS (.m3u8)
- [ ] Configurar duração de segmentos (6 segundos)
- [ ] Criar master playlist com variantes
- [ ] Configurar logging de FFmpeg

#### 3.3 Nginx HLS Server
- [ ] Configurar Nginx para servir HLS
- [ ] Porta: 8081
- [ ] Location `/hls/` aponta para pasta de segmentos
- [ ] Configurar headers CORS
- [ ] Configurar cache headers
- [ ] Testar acesso a playlists

#### 3.4 PostgreSQL
- [ ] Criar Dockerfile para PostgreSQL
- [ ] Configurar banco de dados `streaming_db`
- [ ] Criar usuário e senha
- [ ] Configurar volume persistente
- [ ] Criar scripts de inicialização (schema)
- [ ] Adicionar ao docker-compose.yml

#### 3.5 Redis
- [ ] Configurar Redis container
- [ ] Porta: 6379
- [ ] Configurar persistência (RDB)
- [ ] Configurar maxmemory policy
- [ ] Adicionar ao docker-compose.yml

#### 3.6 RabbitMQ
- [ ] Configurar RabbitMQ container
- [ ] Porta AMQP: 5672
- [ ] Porta Management: 15672
- [ ] Configurar credenciais
- [ ] Habilitar management plugin
- [ ] Adicionar ao docker-compose.yml

### 🎨 Fase 4: Frontend React (Semana 7-8)
**Objetivo**: Criar interface web para streamers e espectadores

#### 4.1 Setup React
- [ ] Criar projeto React com Vite
- [ ] Configurar TypeScript
- [ ] Adicionar dependências:
  - [ ] React Router
  - [ ] Axios
  - [ ] Video.js ou HLS.js
  - [ ] STOMP client (WebSocket)
  - [ ] Tailwind CSS ou Material-UI
  - [ ] React Icons
- [ ] Configurar estrutura de pastas
- [ ] Configurar proxy para backend

#### 4.2 Página Inicial
- [ ] Criar componente `HomePage`
- [ ] Botão "Iniciar Streaming" destacado
- [ ] Seção de explicação sobre a plataforma
- [ ] Design responsivo

#### 4.3 Criação de Stream
- [ ] Criar componente `CreateStreamModal`
- [ ] Formulário: título e descrição
- [ ] Validação de inputs
- [ ] Integração com API: POST /api/streams/create
- [ ] Loading states
- [ ] Error handling
- [ ] Redirecionamento para painel após criação

#### 4.4 Painel do Streamer
- [ ] Criar componente `StreamerDashboard`
- [ ] Exibir URL RTMP: `rtmp://localhost:1935/live`
- [ ] Exibir Stream Key (com botão copiar)
- [ ] Exibir link compartilhável
- [ ] Instruções de configuração do OBS
- [ ] Status da stream (WAITING, LIVE, ENDED)
- [ ] Contador de viewers online
- [ ] Botão "Encerrar Stream"
- [ ] WebSocket connection para atualizações

#### 4.5 Player de Vídeo
- [ ] Criar componente `VideoPlayer`
- [ ] Integrar HLS.js
- [ ] Configurar player responsivo
- [ ] Adaptive Bitrate Streaming (ABR)
- [ ] Controles de reprodução
- [ ] Indicador de qualidade atual
- [ ] Tratamento de erros (stream offline)
- [ ] Loading states

#### 4.6 Página de Visualização
- [ ] Criar componente `WatchPage`
- [ ] Rota: `/watch/:streamId`
- [ ] Carregar informações da stream
- [ ] Renderizar VideoPlayer
- [ ] Exibir título e descrição
- [ ] Contador de viewers
- [ ] Mensagem se stream ainda não iniciou
- [ ] Mensagem se stream encerrou
- [ ] WebSocket para atualizações

#### 4.7 WebSocket Integration
- [ ] Configurar STOMP client
- [ ] Conectar ao endpoint `/ws`
- [ ] Subscrever a tópicos de stream
- [ ] Enviar evento `viewer_joined` ao entrar
- [ ] Enviar evento `viewer_left` ao sair
- [ ] Atualizar UI baseado em eventos recebidos
- [ ] Reconexão automática

### 🔄 Fase 5: Consumer Service (Semana 9-10)
**Objetivo**: Processar eventos assíncronos do message broker

#### 5.1 Projeto Spring Boot
- [ ] Criar projeto consumer-service
- [ ] Adicionar dependências (AMQP, JPA, Redis)
- [ ] Configurar conexão com RabbitMQ
- [ ] Configurar conexão com PostgreSQL

#### 5.2 Event Consumers
- [ ] Criar Consumer para `stream_created`
  - [ ] Registrar métricas iniciais
  - [ ] Log do evento
- [ ] Criar Consumer para `stream_started`
  - [ ] Atualizar estatísticas
  - [ ] Iniciar coleta de métricas detalhadas
  - [ ] Registrar timestamp exato
- [ ] Criar Consumer para `stream_ended`
  - [ ] Calcular duração total
  - [ ] Calcular estatísticas finais
  - [ ] Limpar cache de sessão
  - [ ] Persistir histórico
- [ ] Criar Consumer para `viewer_joined`
  - [ ] Incrementar contador
  - [ ] Registrar evento
  - [ ] Atualizar pico se necessário
- [ ] Criar Consumer para `viewer_left`
  - [ ] Decrementar contador
  - [ ] Registrar evento
- [ ] Configurar concorrência de consumers
- [ ] Implementar retry policy
- [ ] Implementar error handling

#### 5.3 Tarefas Agendadas
- [ ] Criar scheduled task para limpar streams expiradas
  - [ ] Buscar streams WAITING há mais de 30 minutos
  - [ ] Marcar como EXPIRED
  - [ ] Limpar cache
- [ ] Criar scheduled task para limpar arquivos HLS antigos
  - [ ] Remover segmentos de streams encerradas (após 6 horas)
  - [ ] Liberar espaço em disco
- [ ] Criar scheduled task para agregação de métricas
  - [ ] Calcular médias diárias
  - [ ] Persistir estatísticas agregadas

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
- [ ] Testes unitários (cobertura > 70%)
- [ ] Testes de integração
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

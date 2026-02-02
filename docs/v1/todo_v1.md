# TODO - Projeto TCC: Plataforma de Streaming com Análise Comparativa de Desempenho

## 📋 Visão Geral
Desenvolvimento de uma plataforma de streaming de vídeo ao vivo que permite avaliar e comparar o desempenho de diferentes tecnologias e bibliotecas para a mesma funcionalidade.

**Objetivo Principal**: Avaliar o desempenho de uma plataforma de streaming utilizando diferentes bibliotecas e ferramentas na composição do projeto.

## 🎯 Funcionalidades Principais

### Para o Streamer
- [ ] Acesso via `stream.localhost/` sem necessidade de login/cadastro
- [ ] Botão "Iniciar Streaming" que gera:
  - Painel de gestão (título, descrição, configurações)
  - URL RTMP para configurar no OBS
  - Stream Key (chave de transmissão)
  - Link compartilhável para visualização
- [ ] Gerenciamento básico da transmissão (iniciar/parar)

### Para os Espectadores
- [ ] Visualização da transmissão ao vivo via link compartilhado
- [ ] Sem necessidade de login
- [ ] Player de vídeo responsivo
- [ ] Indicador de viewers online (opcional)

## 🏗️ Arquitetura do Sistema

### Componentes Principais

#### 1. Ingestão de Vídeo (RTMP)
**Opção A (Principal)**:
- Nginx-RTMP Module

**Alternativas para Comparação**:
- SRS (Simple Realtime Server)
- Node-Media-Server

#### 2. Processamento de Vídeo
**Opção A (Principal)**:
- FFmpeg

**Alternativas para Comparação**:
- GStreamer
- Jitsi Video Bridge (para cenários específicos)

#### 3. Message Broker / Event Streaming
**Opção A (Principal)**:
- RabbitMQ

**Alternativas para Comparação**:
- Redis Streams
- NATS

#### 4. Servidor de Vídeo (HLS/DASH)
**Opção A (Principal)**:
- Nginx (HLS serving)

**Alternativas para Comparação**:
- Caddy Server
- Apache HTTP Server

#### 5. Cache
**Opção A (Principal)**:
- Redis

**Alternativas para Comparação**:
- Memcached
- Hazelcast

#### 6. Banco de Dados
**Opção A (Principal)**:
- PostgreSQL

**Alternativas para Comparação**:
- MySQL
- MongoDB (para dados de sessão)

#### 7. Monitoramento e Métricas
- Prometheus (coleta de métricas)
- Grafana (visualização)
- Exporters customizados

#### 8. Backend
- Java Spring Boot
  - API REST
  - WebSocket para eventos em tempo real
  - Serviços de gerenciamento de streams

#### 9. Frontend
- React
  - Interface de criação de streaming
  - Player de vídeo (Video.js ou HLS.js)
  - Gerenciamento de sessões

## 📁 Estrutura do Projeto

```
tcc/
├── app/
│   ├── backend/
│   │   ├── stream-service/          # Serviço principal de streaming
│   │   ├── consumer-service/        # Consumidor de eventos
│   │   ├── metrics-service/         # Serviço de métricas
│   │   └── common/                  # Bibliotecas compartilhadas
│   ├── frontend/
│   │   ├── web/                     # React application
│   │   └── public/
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
│   ├── todo.md                      # Este arquivo
│   ├── architecture.md              # Diagramas de arquitetura
│   ├── setup.md                     # Guia de setup
│   ├── testing-scenarios.md         # Cenários de teste
│   └── performance-results.md       # Resultados dos testes
├── scripts/
│   ├── setup.sh
│   ├── start-services.sh
│   └── monitoring/
└── docker-compose/
    ├── docker-compose.yml           # Setup principal (RabbitMQ)
    ├── docker-compose.redis-streams.yml  # Variante com Redis Streams
    ├── docker-compose.nats.yml      # Variante com NATS
    └── docker-compose.srs.yml       # Variante com SRS
```

## 🔄 Fluxo de Dados

### 1. Iniciar Streaming
```
Usuário → Frontend → Backend API → Gera Stream Key → Salva no DB → 
Retorna URL RTMP + Key → Exibe no painel
```

### 2. Transmissão de Vídeo
```
OBS → RTMP Server (Nginx/SRS) → FFmpeg (transcodificação) → 
HLS Segments → Nginx (serving) → Message Broker (notificação) → 
Consumer Service (métricas/processamento)
```

### 3. Visualização
```
Espectador → Frontend → Requisita playlist.m3u8 → 
Nginx serve HLS → Player renderiza vídeo
```

## 📊 Métricas a Serem Coletadas

### Performance
- [ ] Latência de ingestão (RTMP)
- [ ] Tempo de transcodificação
- [ ] Latência de entrega (HLS)
- [ ] Taxa de quadros (FPS)
- [ ] Bitrate de entrada e saída

### Recursos
- [ ] Uso de CPU por componente
- [ ] Uso de memória
- [ ] I/O de disco
- [ ] Uso de rede (bandwidth)

### Qualidade
- [ ] Qualidade do vídeo (PSNR, SSIM)
- [ ] Dropped frames
- [ ] Buffer underruns

### Escalabilidade
- [ ] Número de viewers simultâneos
- [ ] Número de streams simultâneos
- [ ] Throughput do message broker

## 🚀 Roadmap de Implementação

### Fase 1: Setup Inicial (Semana 1-2)
- [ ] Configurar ambiente de desenvolvimento
- [ ] Criar estrutura de pastas do projeto
- [ ] Setup Docker e Docker Compose
- [ ] Configurar repositório Git
- [ ] Documentação inicial (README.md)

### Fase 2: Backend Base (Semana 3-4)
- [ ] Criar projeto Spring Boot (stream-service)
- [ ] Implementar API REST básica
  - [ ] POST /api/streams/create - Criar nova stream
  - [ ] GET /api/streams/:id - Obter informações da stream
  - [ ] DELETE /api/streams/:id - Encerrar stream
  - [ ] GET /api/streams/:id/status - Status da stream
- [ ] Configurar PostgreSQL
- [ ] Implementar models/entities (Stream, StreamSession)
- [ ] Configurar Redis para cache

### Fase 3: Ingestão de Vídeo (Semana 5-6)
- [ ] Configurar Nginx-RTMP no Docker
- [ ] Implementar callbacks do Nginx-RTMP
  - [ ] on_publish
  - [ ] on_publish_done
  - [ ] on_play
  - [ ] on_record_done
- [ ] Integrar FFmpeg para transcodificação
  - [ ] Configurar presets (1080p, 720p, 480p, 360p)
  - [ ] Gerar HLS playlists
- [ ] Armazenar segments HLS

### Fase 4: Frontend (Semana 7-8)
- [ ] Criar aplicação React
- [ ] Implementar tela inicial
  - [ ] Botão "Iniciar Streaming"
  - [ ] Modal/página de configuração
- [ ] Implementar painel do streamer
  - [ ] Exibir URL RTMP e Stream Key
  - [ ] Botão copiar credenciais
  - [ ] Exibir link compartilhável
  - [ ] Controles (parar streaming)
- [ ] Implementar player de vídeo
  - [ ] Integrar HLS.js ou Video.js
  - [ ] Player responsivo
  - [ ] Controles de reprodução
- [ ] Implementar contador de viewers (WebSocket)

### Fase 5: Message Broker e Eventos (Semana 9-10)
- [ ] Configurar RabbitMQ no Docker
- [ ] Implementar Producer no stream-service
  - [ ] Eventos: stream_started, stream_ended, viewer_joined, viewer_left
  - [ ] Configurar exchanges e queues
- [ ] Criar consumer-service
  - [ ] Processar eventos do RabbitMQ
  - [ ] Atualizar métricas em tempo real
  - [ ] Persistir eventos no banco
  - [ ] Configurar Dead Letter Queue
- [ ] Implementar WebSocket no backend
  - [ ] Notificar frontend sobre eventos

### Fase 6: Monitoramento (Semana 11-12)
- [ ] Configurar Prometheus
  - [ ] Scrape targets (Spring Boot Actuator)
  - [ ] Custom metrics
- [ ] Configurar Grafana
  - [ ] Dashboards de monitoramento
  - [ ] Painéis de comparação
- [ ] Implementar metrics-service
  - [ ] Coletar métricas do FFmpeg
  - [ ] Coletar métricas do Nginx
  - [ ] Expor métricas para Prometheus

### Fase 7: Implementação de Alternativas (Semana 13-15)
- [ ] **Alternativa 1: Redis Streams (Message Broker)**
  - [ ] Configurar Redis Streams
  - [ ] Criar docker-compose.redis-streams.yml
  - [ ] Implementar adapter para Redis Streams
  - [ ] Testar e coletar métricas
  
- [ ] **Alternativa 2: NATS (Message Broker)**
  - [ ] Configurar NATS no Docker
  - [ ] Criar docker-compose.nats.yml
  - [ ] Implementar adapter para NATS
  - [ ] Testar e coletar métricas
  
- [ ] **Alternativa 3: SRS (RTMP Server)**
  - [ ] Configurar SRS no Docker
  - [ ] Adaptar callbacks para SRS
  - [ ] Testar e coletar métricas
  
- [ ] **Alternativa 4: GStreamer (Transcodificação)**
  - [ ] Configurar GStreamer
  - [ ] Implementar pipeline de transcodificação
  - [ ] Testar e coletar métricas

### Fase 8: Testes e Otimização (Semana 16-17)
- [ ] Criar cenários de teste
  - [ ] 1 streamer, 10 viewers
  - [ ] 1 streamer, 100 viewers
  - [ ] 1 streamer, 1000 viewers
  - [ ] 5 streamers simultâneos
- [ ] Executar testes de carga (JMeter, K6)
- [ ] Coletar todas as métricas
- [ ] Analisar resultados
- [ ] Otimizações necessárias

### Fase 9: Documentação e TCC (Semana 18-20)
- [ ] Escrever documentação técnica completa
- [ ] Criar gráficos comparativos
- [ ] Escrever TCC
  - [ ] Introdução
  - [ ] Referencial teórico
  - [ ] Metodologia
  - [ ] Implementação
  - [ ] Resultados
  - [ ] Conclusão
- [ ] Preparar apresentação

## 🛠️ Tecnologias e Ferramentas

### Backend
- Java 21
- Spring Boot 3.x
  - Spring Web
  - Spring WebSocket
  - Spring Data JPA
  - Spring Boot Actuator
- Maven ou Gradle

### Frontend
- React 18+
- TypeScript
- Video.js ou HLS.js
- Axios
- WebSocket client
- Tailwind CSS ou Material-UI

### Infraestrutura
- Docker
- Docker Compose
- Nginx
- Nginx-RTMP Module
- FFmpeg
- PostgreSQL
- Redis
- RabbitMQ
- Prometheus
- Grafana

### Ferramentas de Desenvolvimento
- Git
- Postman/Insomnia (teste de APIs)
- OBS Studio (testes de streaming)
- Chrome DevTools

## 📝 Arquivos de Configuração Necessários

### Docker Compose
- [ ] `docker-compose.yml` (setup principal)
- [ ] Variantes para cada alternativa tecnológica

### Nginx
- [ ] `nginx.conf` (configuração base)
- [ ] `nginx-rtmp.conf` (módulo RTMP)
- [ ] `nginx-hls.conf` (serving HLS)

### Spring Boot
- [ ] `application.yml` (configurações principais)
- [ ] `application-dev.yml` (ambiente de desenvolvimento)
- [ ] `application-prod.yml` (ambiente de produção - mesmo que local)

### Prometheus
- [ ] `prometheus.yml` (configuração de scraping)

### Grafana
- [ ] Dashboards em JSON
- [ ] Datasources configuration

## 🔍 Pontos Adicionais Sugeridos

### Segurança
- [ ] Validação de Stream Keys
- [ ] Rate limiting para criação de streams
- [ ] CORS configurado corretamente
- [ ] Sanitização de inputs

### Resiliência
- [ ] Health checks para todos os serviços
- [ ] Retry policies
- [ ] Circuit breakers
- [ ] Graceful shutdown

### Logging
- [ ] Logging estruturado (JSON)
- [ ] Diferentes níveis de log
- [ ] Rotação de logs
- [ ] Centralização (opcional: ELK stack)

### Qualidade de Código
- [ ] Testes unitários (backend)
- [ ] Testes de integração
- [ ] Testes E2E (frontend)
- [ ] Code coverage > 70%
- [ ] SonarQube analysis (opcional)

### Melhorias de UX
- [ ] Loading states
- [ ] Error handling e feedback
- [ ] Responsive design
- [ ] Instruções claras para configurar OBS
- [ ] Preview da stream no painel do streamer

### Features Extras (Se houver tempo)
- [ ] Gravação automática das streams
- [ ] VOD (Video on Demand) das streams passadas
- [ ] Chat básico (usando WebSocket)
- [ ] Estatísticas em tempo real para o streamer
- [ ] Suporte a múltiplas qualidades (ABR - Adaptive Bitrate)
- [ ] Thumbnail automático da stream

## 📊 Critérios de Comparação

Para cada tecnologia alternativa, avaliar:

1. **Performance**
   - Throughput
   - Latência
   - Uso de recursos

2. **Facilidade de Uso**
   - Complexidade de configuração
   - Documentação disponível
   - Curva de aprendizado

3. **Escalabilidade**
   - Capacidade de lidar com carga
   - Horizontal scaling

4. **Confiabilidade**
   - Taxa de erros
   - Recuperação de falhas

5. **Comunidade e Suporte**
   - Atividade do projeto
   - Issues/PRs
   - Última atualização

## 🎓 Entregáveis do TCC

- [ ] Código-fonte completo (GitHub)
- [ ] Documentação técnica
- [ ] Relatório de testes e comparações
- [ ] Artigo/Monografia
- [ ] Apresentação (slides)
- [ ] Vídeo demonstrativo (opcional)

## 📅 Timeline Estimado

**Total: ~20 semanas (5 meses)**

- Desenvolvimento: 17 semanas
- Testes e refinamento: 2 semanas
- Documentação final: 1 semana

## 🎯 Próximos Passos Imediatos

1. Criar diagramas de arquitetura (Mermaid)
2. Criar arquivo `architecture.md` com diagramas
3. Criar arquivo `setup.md` com instruções de instalação
4. Inicializar projeto Spring Boot
5. Inicializar projeto React
6. Criar primeiro docker-compose.yml

---

**Data de Início**: Janeiro 2026  
**Data Prevista de Conclusão**: Junho 2026  
**Última Atualização**: 20/01/2026
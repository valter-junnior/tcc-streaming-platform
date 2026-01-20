# Documentação do Projeto - Plataforma de Streaming

## 📚 Índice

1. [Visão Geral](#visão-geral)
2. [Arquitetura do Sistema](#arquitetura-do-sistema)
3. [Tecnologias e Ferramentas](#tecnologias-e-ferramentas)
4. [Componentes e Serviços](#componentes-e-serviços)
5. [Alternativas Tecnológicas](#alternativas-tecnológicas)
6. [Fluxos do Sistema](#fluxos-do-sistema)
7. [Monitoramento e Métricas](#monitoramento-e-métricas)
8. [Estratégia de Testes](#estratégia-de-testes)

---

## 📋 Visão Geral

### Sobre o Projeto

Este TCC (Trabalho de Conclusão de Curso) propõe o desenvolvimento de uma **plataforma de streaming de vídeo ao vivo** com um objetivo diferenciado: não apenas criar um sistema funcional, mas realizar uma **análise comparativa de desempenho** entre diferentes tecnologias e bibliotecas que podem ser utilizadas para implementar as mesmas funcionalidades.

### Motivação

No ecossistema de streaming, existem diversas ferramentas que prometem resolver os mesmos problemas, mas com abordagens diferentes. Este projeto busca responder questões como:

- Qual servidor RTMP oferece melhor performance: Nginx-RTMP ou SRS?
- FFmpeg ou GStreamer para transcodificação de vídeo?
- Kafka, RabbitMQ ou Redis Streams para mensageria?
- Como essas escolhas impactam latência, uso de recursos e escalabilidade?

### O que a Plataforma Faz

**Para Streamers:**
- Acesso simples via navegador (sem cadastro ou login)
- Um clique em "Iniciar Streaming" gera automaticamente:
  - URL do servidor RTMP
  - Stream Key única e segura
  - Link compartilhável para espectadores
- Painel de controle com informações da transmissão

**Para Espectadores:**
- Acesso direto via link compartilhado (sem login)
- Visualização da transmissão ao vivo
- Contador de espectadores online
- Player de vídeo adaptativo com múltiplas qualidades

**Para o TCC:**
- Coleta automática de métricas de performance
- Comparação objetiva entre diferentes stacks tecnológicos
- Dados para análise de trade-offs entre as tecnologias

### Características Técnicas

- **Arquitetura de Microserviços**: Componentes independentes e substituíveis
- **Containerização Completa**: Todo o ambiente roda em Docker
- **Ambiente Local**: Projetado para execução em máquina local, não produção
- **Monitoramento em Tempo Real**: Métricas detalhadas de cada componente
- **Streaming HLS**: Protocolo HTTP Live Streaming para compatibilidade ampla
- **Transcodificação Adaptativa**: Múltiplas qualidades de vídeo (360p a 1080p)

---

## 🏗️ Arquitetura do Sistema

### Visão Geral

O sistema utiliza uma **arquitetura em camadas** com componentes desacoplados, permitindo a substituição de tecnologias sem afetar o sistema como um todo. Esta modularidade é essencial para o objetivo do TCC: comparar diferentes implementações.

Para visualizar os diagramas completos, consulte a pasta [diagrams/](diagrams/).

### Camadas da Arquitetura

#### 1. Frontend Layer (Camada de Apresentação)
**Função**: Interface com o usuário, tanto para streamers quanto espectadores.

**Responsabilidades**:
- Renderizar interfaces de criação e controle de streams
- Reproduzir vídeo ao vivo via player HLS
- Comunicação em tempo real via WebSocket
- Gerenciar estado da aplicação

**Por que esta abordagem?**
- Interface web = acessível de qualquer dispositivo
- SPA (Single Page Application) = experiência fluida
- WebSocket = atualizações em tempo real sem polling

#### 2. Backend Services Layer (Camada de Negócios)
**Função**: Lógica de negócios, orquestração e processamento de eventos.

**Responsabilidades**:
- Gerenciar ciclo de vida das streams
- Validar e autenticar requisições
- Processar eventos assíncronos
- Coletar e agregar métricas

**Por que microserviços?**
- Escalabilidade independente de cada serviço
- Isolamento de falhas
- Possibilidade de usar diferentes tecnologias
- Facilita testes de diferentes implementações

#### 3. Streaming Infrastructure Layer (Camada de Streaming)
**Função**: Ingestão, processamento e entrega de vídeo.

**Responsabilidades**:
- Receber stream RTMP do OBS
- Transcodificar vídeo em múltiplas qualidades
- Gerar e servir segmentos HLS
- Validar credenciais de streaming

**Por que esta pipeline?**
- RTMP = padrão da indústria para ingestão
- Transcodificação = compatibilidade com diversos dispositivos
- HLS = melhor suporte cross-platform via HTTP

#### 4. Data Layer (Camada de Dados)
**Função**: Persistência, cache e mensageria.

**Responsabilidades**:
- Armazenar informações das streams
- Cachear dados de sessão
- Transportar eventos entre serviços
- Garantir consistência dos dados

**Por que múltiplas soluções de dados?**
- Banco relacional = dados estruturados e relacionamentos
- Cache = redução de latência em reads frequentes
- Message broker = comunicação assíncrona e desacoplada

#### 5. Monitoring Layer (Camada de Observabilidade)
**Função**: Coleta, armazenamento e visualização de métricas.

**Responsabilidades**:
- Coletar métricas de todos os componentes
- Armazenar séries temporais
- Visualizar dados em dashboards
- Alertar sobre anomalias

**Por que monitoramento dedicado?**
- Essencial para comparação de performance
- Identificação de gargalos
- Dados objetivos para o TCC

---

## 🛠️ Tecnologias e Ferramentas

### Linguagens de Programação

#### Java 21
**Usado em**: Backend Services (Spring Boot)

**Para que serve**:
Java é a linguagem principal para os microserviços de backend, escolhida pela sua maturidade, ecossistema robusto e excelente suporte para aplicações empresariais.

**Prós**:
- ✅ Tipagem forte reduz erros em tempo de execução
- ✅ Ecossistema maduro com milhares de bibliotecas
- ✅ JVM otimizada para alta performance
- ✅ Excelente para aplicações multithread
- ✅ Ampla adoção no mercado
- ✅ Ferramentas de debugging e profiling robustas

**Contras**:
- ❌ Verbosidade do código comparado a linguagens modernas
- ❌ Consumo de memória pode ser alto
- ❌ Tempo de inicialização mais lento que linguagens compiladas
- ❌ Curva de aprendizado para iniciantes

#### TypeScript
**Usado em**: Frontend (React)

**Para que serve**:
TypeScript adiciona tipagem estática ao JavaScript, tornando o código mais seguro e manutenível, especialmente importante em aplicações React complexas.

**Prós**:
- ✅ Detecta erros em tempo de desenvolvimento
- ✅ Autocomplete e IntelliSense melhorados
- ✅ Melhor refatoração de código
- ✅ Documentação implícita via tipos
- ✅ Compatível com todo código JavaScript

**Contras**:
- ❌ Necessita compilação
- ❌ Configuração inicial pode ser complexa
- ❌ Curva de aprendizado adicional
- ❌ Algumas bibliotecas têm tipagem incompleta

### Frameworks e Bibliotecas

#### Spring Boot 3.x
**Usado em**: Backend Services

**Para que serve**:
Framework Java que simplifica a criação de aplicações empresariais, fornecendo configuração automática, injeção de dependências e padrões estabelecidos.

**Prós**:
- ✅ Convenção sobre configuração (menos boilerplate)
- ✅ Ecossistema Spring é imenso
- ✅ Suporte nativo para microserviços
- ✅ Integração fácil com bancos de dados (Spring Data)
- ✅ Segurança robusta (Spring Security)
- ✅ Monitoramento integrado (Actuator)
- ✅ Documentação excelente

**Contras**:
- ❌ Pode ser "mágico" demais para iniciantes
- ❌ Consumo de memória elevado
- ❌ Tempo de inicialização pode ser alto
- ❌ Curva de aprendizado íngreme

**Alternativas consideradas**:
- Quarkus: Mais leve e rápido, mas menos maduro
- Micronaut: Melhor startup time, mas ecossistema menor

#### React 18+
**Usado em**: Frontend

**Para que serve**:
Biblioteca JavaScript para construção de interfaces de usuário interativas através de componentes reutilizáveis.

**Prós**:
- ✅ Component-based architecture
- ✅ Virtual DOM para performance
- ✅ Ecossistema gigante de bibliotecas
- ✅ Grande comunidade e suporte
- ✅ React Hooks simplificam gestão de estado
- ✅ Fácil integração com outras bibliotecas

**Contras**:
- ❌ Apenas a view layer (precisa de outras libs)
- ❌ JSX pode ser estranho inicialmente
- ❌ Mudanças frequentes nas melhores práticas
- ❌ Necessita bundler (Webpack, Vite)

**Alternativas consideradas**:
- Vue.js: Mais simples, mas comunidade menor
- Angular: Mais completo, mas mais pesado

### Infraestrutura de Streaming

#### Nginx-RTMP Module
**Usado em**: Ingestão de vídeo RTMP

**Para que serve**:
Extensão do Nginx que adiciona suporte ao protocolo RTMP (Real-Time Messaging Protocol), permitindo receber streams de software como OBS Studio.

**Prós**:
- ✅ Extremamente estável e testado em produção
- ✅ Performance excelente
- ✅ Baixo consumo de recursos
- ✅ Configuração relativamente simples
- ✅ Suporte a HLS nativo
- ✅ Callbacks HTTP para integração

**Contras**:
- ❌ Não é mais mantido ativamente (último commit: 2017)
- ❌ Funcionalidades limitadas comparado a alternativas modernas
- ❌ Documentação pode ser confusa
- ❌ Configuração via arquivo de texto apenas

**Alternativas no projeto**:
- **SRS**: Mais moderno, ativamente mantido, melhor documentação
- **Node-Media-Server**: JavaScript-based, fácil de estender

#### FFmpeg
**Usado em**: Transcodificação de vídeo

**Para que serve**:
Suite completa de ferramentas para manipulação de áudio e vídeo. No projeto, é usado para transcodificar o stream em múltiplas resoluções e bitrates.

**Prós**:
- ✅ Padrão da indústria
- ✅ Suporta praticamente todos os formatos
- ✅ Altamente otimizado
- ✅ Extremamente flexível
- ✅ Gratuito e open source
- ✅ Hardware acceleration disponível

**Contras**:
- ❌ Curva de aprendizado muito íngreme
- ❌ Sintaxe de comandos complexa
- ❌ Alto consumo de CPU para transcodificação
- ❌ Documentação pode ser intimidadora
- ❌ Debugging difícil

**Alternativas no projeto**:
- **GStreamer**: Arquitetura mais modular, melhor para programar contra

#### HLS (HTTP Live Streaming)
**Usado em**: Protocolo de entrega de vídeo

**Para que serve**:
Protocolo desenvolvido pela Apple para streaming adaptativo sobre HTTP. Divide o vídeo em pequenos segmentos e usa playlists para controle.

**Por que HLS?**:
- ✅ Funciona sobre HTTP (sem portas especiais)
- ✅ Suporte nativo em iOS/Safari
- ✅ Passa por CDNs e firewalls facilmente
- ✅ Adaptive Bitrate (ABR) nativo
- ✅ Simples de implementar no servidor

**Limitações**:
- ❌ Latência maior que RTMP (6-30 segundos)
- ❌ Não ideal para interação em tempo real
- ❌ Overhead de múltiplos arquivos pequenos

**Alternativas**:
- DASH: Mais flexível, mas menos suporte
- WebRTC: Latência ultra-baixa, mas complexidade alta

### Banco de Dados e Cache

#### PostgreSQL
**Usado em**: Banco de dados principal

**Para que serve**:
Banco de dados relacional open-source usado para armazenar informações das streams, eventos e métricas históricas.

**Prós**:
- ✅ ACID compliant (confiabilidade)
- ✅ Suporte a JSONB para dados semi-estruturados
- ✅ Performance excelente
- ✅ Recursos avançados (window functions, CTEs)
- ✅ Extensível (PostGIS, TimescaleDB)
- ✅ Replicação robusta

**Contras**:
- ❌ Mais complexo que MySQL para iniciantes
- ❌ Configuração de performance pode ser trabalhosa
- ❌ Consome mais recursos que bancos NoSQL

**Alternativas no projeto**:
- **MySQL**: Mais popular, mas menos features
- **MongoDB**: NoSQL, mais flexível para dados não estruturados

#### Redis
**Usado em**: Cache e sessões

**Para que serve**:
Banco de dados em memória usado para cache de dados frequentemente acessados e armazenamento de sessões ativas de streams.

**Prós**:
- ✅ Extremamente rápido (in-memory)
- ✅ Estruturas de dados ricas (strings, lists, sets, hashes)
- ✅ Pub/Sub nativo
- ✅ Persistência opcional
- ✅ Replicação simples
- ✅ Baixo consumo de recursos

**Contras**:
- ❌ Limitado pela RAM disponível
- ❌ Dados podem ser perdidos se não persistidos
- ❌ Single-threaded (mas muito rápido)
- ❌ Sem queries complexas como SQL

**Alternativas no projeto**:
- **Memcached**: Mais simples, mas menos features

### Message Brokers (Comunicação Entre Serviços)

O projeto precisa de um sistema de mensageria para permitir que os microserviços se comuniquem de forma assíncrona e desacoplada. Quando eventos importantes acontecem (stream iniciada, espectador entrou, etc.), eles são publicados no message broker e os serviços interessados os consomem.

#### RabbitMQ (⭐ Principal)
**Usado em**: Comunicação assíncrona entre microserviços

**Para que serve**:
RabbitMQ é um message broker tradicional que implementa o protocolo AMQP. Funciona como um "correio" entre os serviços: um serviço envia uma mensagem e outro a recebe, sem que os dois precisem estar diretamente conectados.

**Responsabilidades no projeto**:
- Transportar eventos entre serviços (stream_started, stream_ended, viewer_joined, viewer_left)
- Garantir entrega de mensagens mesmo se um serviço estiver offline
- Fazer roteamento de mensagens para múltiplos consumidores
- Desacoplar os microserviços

**Prós**:
- ✅ Extremamente maduro e estável
- ✅ Interface de gerenciamento web completa
- ✅ Suporte a múltiplos protocolos (AMQP, MQTT, STOMP)
- ✅ Plugins para diferentes funcionalidades
- ✅ Dead Letter Queues (mensagens com erro)
- ✅ Message persistence (mensagens não são perdidas)
- ✅ Comunidade grande e ativa
- ✅ Documentação excelente

**Contras**:
- ❌ Throughput menor que Kafka para alto volume
- ❌ Consumo de memória pode ser alto
- ❌ Configuração pode ser complexa para iniciantes
- ❌ Não é otimizado para streaming de dados em tempo real
- ❌ Requer Erlang instalado

**Quando usar**:
- Comunicação ponto-a-ponto entre serviços
- Necessidade de garantias de entrega
- Roteamento complexo de mensagens
- Projetos que não precisam de throughput extremo

#### Redis Streams (Alternativa 1)
**Usado em**: Message broker leve usando Redis

**Para que serve**:
Redis Streams é uma estrutura de dados do Redis que funciona como um log de eventos append-only, similar a Kafka mas muito mais simples. Permite pub/sub e processamento de streams de dados.

**Responsabilidades no projeto**:
- Mesmas do RabbitMQ, mas de forma mais leve
- Aproveitar Redis que já está no projeto para cache
- Processar eventos em tempo real

**Prós**:
- ✅ Extremamente rápido (in-memory)
- ✅ Não precisa de serviço adicional (já temos Redis)
- ✅ Configuração muito simples
- ✅ Consumer groups nativos
- ✅ Baixo consumo de recursos
- ✅ Ótimo para volume moderado de mensagens

**Contras**:
- ❌ Menos features que RabbitMQ ou Kafka
- ❌ Limitado pela memória RAM
- ❌ Sem interface gráfica de gerenciamento
- ❌ Ferramentas de monitoramento limitadas
- ❌ Persistência opcional (pode perder dados)
- ❌ Não é ideal para mensagens muito grandes

**Quando usar**:
- Projeto já usa Redis
- Volume moderado de mensagens
- Necessidade de simplicidade
- Performance é crítica

#### NATS (Alternativa 2)
**Usado em**: Message broker ultra-rápido e leve

**Para que serve**:
NATS é um message broker moderno focado em simplicidade e performance. Desenvolvido em Go, é extremamente leve e rápido, ideal para arquiteturas cloud-native.

**Responsabilidades no projeto**:
- Mesmas do RabbitMQ, com foco em velocidade
- Comunicação em tempo real entre serviços
- Pub/Sub com baixíssima latência

**Prós**:
- ✅ Extremamente rápido e leve
- ✅ Latência ultra-baixa (microsegundos)
- ✅ Configuração minimalista
- ✅ Baixíssimo consumo de recursos
- ✅ Suporte nativo a clustering
- ✅ Desenvolvido em Go (binary único)
- ✅ JetStream para persistência (NATS 2.0+)

**Contras**:
- ❌ Menos features que RabbitMQ
- ❌ Comunidade menor
- ❌ Menos ferramentas de terceiros
- ❌ Persistência é opcional e mais recente
- ❌ Menos documentação e exemplos
- ❌ Curva de aprendizado para conceitos específicos

**Quando usar**:
- Performance é absolutamente crítica
- Arquitetura cloud-native
- Necessidade de baixíssima latência
- Simplicidade operacional é importante

**Comparação: Qual escolher?**

| Critério | RabbitMQ | Redis Streams | NATS |
|----------|----------|---------------|------|
| Performance | ⭐⭐⭐ Bom | ⭐⭐⭐⭐⭐ Excelente | ⭐⭐⭐⭐⭐ Excelente |
| Confiabilidade | ⭐⭐⭐⭐⭐ Excelente | ⭐⭐⭐ Bom | ⭐⭐⭐⭐ Muito bom |
| Features | ⭐⭐⭐⭐⭐ Excelente | ⭐⭐⭐ Bom | ⭐⭐⭐ Bom |
| Facilidade | ⭐⭐⭐ Médio | ⭐⭐⭐⭐⭐ Fácil | ⭐⭐⭐⭐ Fácil |
| Comunidade | ⭐⭐⭐⭐⭐ Grande | ⭐⭐⭐⭐ Grande | ⭐⭐⭐ Média |
| Recursos | ⭐⭐ Alto | ⭐⭐⭐⭐ Baixo | ⭐⭐⭐⭐⭐ Muito baixo |

**Recomendação para o TCC**:
- **RabbitMQ**: Melhor para aprender conceitos de mensageria, mais didático, features completas
- **Redis Streams**: Melhor custo-benefício se já usa Redis, ótima performance
- **NATS**: Melhor para arquiteturas modernas e foco em performance pura

### Ferramentas de Monitoramento

#### Prometheus
**Usado em**: Coleta e armazenamento de métricas

**Para que serve**:
Prometheus é um sistema de monitoramento e alerta open-source que coleta métricas de time series (séries temporais). Ele "scrapes" (busca) métricas dos serviços em intervalos regulares e as armazena em um banco de dados otimizado para séries temporais.

**Responsabilidades no projeto**:
- Coletar métricas de todos os microserviços (via Spring Boot Actuator)
- Coletar métricas customizadas da aplicação
- Armazenar histórico de métricas
- Executar queries sobre os dados coletados
- Disparar alertas quando métricas ultrapassam thresholds

**Prós**:
- ✅ Modelo pull (os serviços não precisam enviar dados)
- ✅ PromQL: linguagem de query poderosa
- ✅ Dimensões múltiplas via labels
- ✅ Armazenamento eficiente de time series
- ✅ Descoberta automática de serviços
- ✅ Amplamente adotado (padrão da indústria)
- ✅ Integração nativa com Kubernetes
- ✅ Sistema de alerting integrado

**Contras**:
- ❌ Não é distribuído nativamente (single server)
- ❌ Retenção de dados limitada
- ❌ Sem autenticação nativa
- ❌ UI básica (melhor usar Grafana)
- ❌ Não ideal para logs ou traces
- ❌ Queries complexas podem ser lentas

**Alternativas**:
- **InfluxDB**: Melhor para retenção longa, mas mais complexo
- **Datadog**: SaaS completo, mas pago
- **New Relic**: APM completo, mas pago

#### Grafana
**Usado em**: Visualização de métricas e dashboards

**Para que serve**:
Grafana é uma plataforma de observabilidade que cria dashboards interativos e visualizações bonitas a partir de múltiplas fontes de dados, incluindo Prometheus.

**Responsabilidades no projeto**:
- Conectar ao Prometheus como datasource
- Criar dashboards de monitoramento
- Visualizar métricas em tempo real
- Criar alertas visuais
- Comparar métricas entre diferentes configurações

**Prós**:
- ✅ Interface linda e intuitiva
- ✅ Suporta múltiplas fontes de dados
- ✅ Dashboards compartilháveis
- ✅ Sistema de alerting visual
- ✅ Plugins para extensibilidade
- ✅ Painel de variáveis dinâmicas
- ✅ Exportação de dashboards (JSON)
- ✅ Comunidade grande com dashboards prontos

**Contras**:
- ❌ Pode consumir muitos recursos com muitos dashboards
- ❌ Curva de aprendizado para queries complexas
- ❌ Configuração inicial pode ser trabalhosa
- ❌ Alerting menos poderoso que ferramentas dedicadas

**Alternativas**:
- **Kibana**: Melhor para logs (Elasticsearch), mas mais pesado
- **Chronograf**: Específico para InfluxDB
- **Prometheus UI**: Básico, mas suficiente para queries simples

---

## 🏢 Componentes e Serviços

Esta seção detalha cada serviço/componente do sistema, suas responsabilidades e trade-offs.

### Serviços Backend (Spring Boot)

Todos os serviços backend são desenvolvidos em Java 21 com Spring Boot 3.x, aproveitando o ecossistema Spring para criar microserviços robustos e escaláveis.

#### Stream Service (Serviço Principal)

**Para que serve**:
É o coração da aplicação, responsável por gerenciar todo o ciclo de vida das transmissões ao vivo. Quando um usuário clica em "Iniciar Streaming", é este serviço que cria a stream, gera as credenciais e coordena tudo.

**Responsabilidades**:
- Criar e gerenciar streams (operações CRUD)
- Gerar stream keys únicas e seguras (UUID ou hash)
- Validar credenciais de transmissão via callbacks HTTP do Nginx-RTMP
- Gerenciar sessões ativas de streaming no Redis
- Controlar status das streams (WAITING, LIVE, PAUSED, ENDED, ERROR)
- Publicar eventos no message broker (stream_started, stream_ended, viewer_joined, viewer_left)
- Fornecer API REST para o frontend
- Fornecer WebSocket para atualizações em tempo real
- Contar e gerenciar viewers conectados
- Persistir dados das streams no PostgreSQL

**Tecnologias usadas**:
- Spring Boot (framework base)
- Spring Web (REST API)
- Spring WebSocket (comunicação real-time)
- Spring Data JPA (acesso ao banco)
- Spring Data Redis (cache)
- Spring AMQP/Spring Kafka (mensageria)
- Spring Boot Actuator (métricas)

**Prós desta abordagem**:
- ✅ Centraliza a lógica de negócio de streaming
- ✅ API REST facilita integração com frontend
- ✅ WebSocket permite atualizações em tempo real
- ✅ Spring Boot simplifica configuração
- ✅ Actuator fornece métricas prontas

**Contras**:
- ❌ Ponto único de falha se não replicado
- ❌ Pode ficar complexo com muitas responsabilidades
- ❌ Consumo de memória do Spring Boot

**Como interage com outros componentes**:
- **Frontend**: Recebe requisições HTTP e envia eventos via WebSocket
- **Nginx-RTMP**: Recebe callbacks HTTP para validar streams
- **PostgreSQL**: Persiste dados das streams
- **Redis**: Cacheia sessões ativas
- **Message Broker**: Publica eventos para outros serviços
- **Prometheus**: Expõe métricas via /actuator/prometheus

#### Consumer Service (Processador de Eventos)

**Para que serve**:
Consome eventos publicados pelo Stream Service no message broker e executa ações assíncronas baseadas nesses eventos. É o "trabalhador nos bastidores" que processa tudo que não precisa ser feito em tempo real.

**Responsabilidades**:
- Consumir eventos do message broker (RabbitMQ/Redis Streams/NATS)
- Processar evento de stream iniciada (atualizar contadores, iniciar coleta de métricas)
- Processar evento de stream encerrada (calcular estatísticas finais, limpar cache)
- Processar eventos de viewers (atualizar contador em tempo real)
- Persistir eventos históricos no banco
- Agregar métricas de performance
- Executar tarefas de limpeza (streams expiradas)
- Notificar outros sistemas (opcional)

**Tecnologias usadas**:
- Spring Boot
- Spring AMQP (RabbitMQ) / Spring Kafka / Lettuce (Redis)
- Spring Data JPA
- Spring Scheduling (tarefas agendadas)

**Prós desta abordagem**:
- ✅ Desacoplamento: Stream Service não precisa esperar processamento
- ✅ Escalabilidade: Pode ter múltiplas instâncias consumindo
- ✅ Resiliência: Mensagens não são perdidas se o serviço cair
- ✅ Assíncrono: Não afeta latência do Stream Service
- ✅ Facilita adicionar novos processamentos

**Contras**:
- ❌ Complexidade adicional de ter outro serviço
- ❌ Eventual consistency (dados podem não estar sincronizados imediatamente)
- ❌ Debugging mais difícil (eventos assíncronos)
- ❌ Precisa gerenciar dead letter queues

**Como interage com outros componentes**:
- **Message Broker**: Consome eventos publicados
- **PostgreSQL**: Persiste eventos e métricas agregadas
- **Redis**: Atualiza cache de estatísticas
- **Prometheus**: Expõe métricas de processamento

#### Metrics Service (Coletor de Métricas)

**Para que serve**:
Responsável por coletar métricas específicas de streaming que não são facilmente capturadas pelo Prometheus sozinho, como métricas do FFmpeg, qualidade de vídeo, e estatísticas do Nginx-RTMP.

**Responsabilidades**:
- Parsear logs do FFmpeg para extrair métricas de transcodificação
- Coletar estatísticas do Nginx-RTMP via API ou logs
- Calcular métricas de qualidade (PSNR, SSIM quando disponível)
- Agregar métricas de latência (RTMP → Transcoding → HLS)
- Expor métricas customizadas para Prometheus
- Calcular métricas de negócio (tempo médio de stream, pico de viewers)
- Gerar relatórios periódicos

**Tecnologias usadas**:
- Spring Boot
- Micrometer (biblioteca de métricas do Spring)
- Spring Scheduling (coleta periódica)
- Prometheus Client (expor métricas)

**Prós desta abordagem**:
- ✅ Centraliza coleta de métricas complexas
- ✅ Não sobrecarrega Stream Service
- ✅ Permite correlação de métricas de diferentes fontes
- ✅ Facilita análise comparativa entre tecnologias

**Contras**:
- ❌ Parsing de logs pode ser frágil
- ❌ Overhead de mais um serviço
- ❌ Necessita acesso aos logs do FFmpeg e Nginx

**Como interage com outros componentes**:
- **FFmpeg**: Lê logs ou stats do processo
- **Nginx-RTMP**: Consulta módulo de stats
- **PostgreSQL**: Persiste métricas históricas
- **Prometheus**: Expõe métricas coletadas

### Infraestrutura de Streaming

#### Nginx-RTMP Server

**Para que serve**:
Servidor RTMP que recebe o stream de vídeo do OBS Studio e o repassa para o FFmpeg transcodificar. É o "porteiro" que recebe a transmissão bruta.

**Responsabilidades**:
- Receber conexão RTMP do OBS Studio
- Validar stream key via callback HTTP ao Stream Service
- Aceitar ou rejeitar publicação baseado na validação
- Repassar stream bruto para FFmpeg via exec
- Notificar Stream Service quando stream inicia/termina
- Servir estatísticas do módulo RTMP

**Configuração típica**:
- Porta RTMP: 1935
- Callbacks HTTP para validação
- Exec para invocar FFmpeg
- Módulo de stats habilitado

**Prós**:
- ✅ Nginx é extremamente estável
- ✅ Performance excelente
- ✅ Baixo consumo de recursos
- ✅ Callbacks HTTP facilitam integração

**Contras**:
- ❌ Módulo não é mais mantido
- ❌ Configuração via arquivo texto
- ❌ Funcionalidades limitadas

**Alternativas**:
- **SRS**: Detalhado na próxima seção
- **Node-Media-Server**: Baseado em Node.js, fácil de estender programaticamente

#### FFmpeg Transcoder

**Para que serve**:
Ferramenta que transcodifica o vídeo recebido em múltiplas resoluções e bitrates, gerando os segmentos HLS que serão servidos aos espectadores.

**Responsabilidades**:
- Receber stream do Nginx-RTMP (via pipe ou RTMP pull)
- Transcodificar para múltiplas qualidades (1080p, 720p, 480p, 360p)
- Gerar segmentos HLS (.ts files)
- Gerar playlists HLS (.m3u8 files)
- Aplicar filtros de vídeo se necessário
- Gerar thumbnails periódicos (opcional)

**Qualidades típicas**:
- 1080p: 1920x1080, 5000 kbps, H.264 preset medium
- 720p: 1280x720, 2800 kbps, H.264 preset medium
- 480p: 854x480, 1400 kbps, H.264 preset fast
- 360p: 640x360, 800 kbps, H.264 preset faster

**Prós**:
- ✅ Padrão da indústria
- ✅ Suporta todos os formatos imagináveis
- ✅ Hardware acceleration (NVENC, QSV, VAAPI)
- ✅ Qualidade excelente

**Contras**:
- ❌ Alto consumo de CPU (especialmente múltiplas qualidades)
- ❌ Latência adicional pelo processamento
- ❌ Sintaxe complexa de comandos

**Alternativa**:
- **GStreamer**: Detalhado na próxima seção

#### Nginx HLS Server

**Para que serve**:
Servidor web que serve os segmentos HLS e playlists gerados pelo FFmpeg para os players dos espectadores.

**Responsabilidades**:
- Servir arquivos .m3u8 (playlists)
- Servir arquivos .ts (segmentos de vídeo)
- Configurar headers CORS para acesso web
- Configurar cache headers
- Servir diferentes qualidades via ABR

**Configuração típica**:
- Porta HTTP: 8080 ou 8081
- Location /hls/ aponta para pasta de segmentos
- Headers CORS habilitados
- Cache desabilitado ou TTL baixo

**Prós**:
- ✅ Nginx é extremamente rápido para servir arquivos estáticos
- ✅ Baixíssimo consumo de recursos
- ✅ Amplamente conhecido e documentado

**Contras**:
- ❌ Segmentos HLS ocupam espaço em disco
- ❌ Necessita limpeza periódica de arquivos antigos

### Frontend Application

#### React SPA (Single Page Application)

**Para que serve**:
Interface web que permite streamers criarem transmissões e espectadores assistirem, tudo no navegador sem necessidade de instalação.

**Responsabilidades**:
- Renderizar interface de criação de stream
- Exibir credenciais RTMP e stream key
- Reproduzir vídeo HLS via player
- Conectar ao WebSocket para atualizações em tempo real
- Exibir contador de viewers
- Gerenciar estado da aplicação (React state/context)
- Fazer requisições HTTP ao backend

**Bibliotecas principais**:
- React 18+ (framework)
- TypeScript (type safety)
- Video.js ou HLS.js (player de vídeo)
- Axios ou Fetch (requisições HTTP)
- STOMP/SockJS (WebSocket client)
- React Router (navegação)
- Tailwind CSS ou Material-UI (estilização)

**Prós**:
- ✅ Interface única para streamers e viewers
- ✅ Não precisa instalação
- ✅ Atualizações em tempo real via WebSocket
- ✅ Player HLS funciona em todos os navegadores

**Contras**:
- ❌ JavaScript pode ser desabilitado
- ❌ Necessita bundler (Vite, Webpack)
- ❌ Player HLS adiciona latência

---

## 🔄 Alternativas Tecnológicas Detalhadas

Esta seção apresenta as alternativas para cada componente principal, com análise detalhada para ajudar na decisão.

### Alternativas para Ingestão RTMP

#### SRS (Simple Realtime Server)

**Para que serve**:
Servidor de streaming moderno desenvolvido em C++, focado em simplicidade e performance. É uma alternativa mais moderna ao Nginx-RTMP, com desenvolvimento ativo.

**Responsabilidades (mesmas do Nginx-RTMP)**:
- Receber streams RTMP
- Validar autenticação
- Repassar para transcodificação
- Notificar eventos

**Prós**:
- ✅ Desenvolvimento ativo (última release recente)
- ✅ Documentação excelente e em inglês
- ✅ API HTTP nativa para controle
- ✅ WebRTC support nativo
- ✅ Clustering support
- ✅ Dashboard web incluído
- ✅ Estatísticas em JSON
- ✅ Callbacks HTTP mais flexíveis

**Contras**:
- ❌ Menos maduro que Nginx-RTMP
- ❌ Comunidade menor
- ❌ Menos exemplos e tutoriais
- ❌ Curva de aprendizado inicial

**Quando escolher SRS**:
- Necessidade de features modernas (WebRTC, clustering)
- Preferência por API REST para controle
- Projeto precisa de manutenção ativa
- Dashboard integrado é importante

**Comparação direta**:
| Critério | Nginx-RTMP | SRS |
|----------|------------|-----|
| Maturidade | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| Manutenção | ⭐ Parado | ⭐⭐⭐⭐⭐ Ativo |
| Performance | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| Features | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Facilidade | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| Documentação | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

#### Node-Media-Server

**Para que serve**:
Servidor RTMP/HTTP-FLV/WebSocket-FLV desenvolvido em Node.js. Extremamente fácil de estender programaticamente por ser JavaScript.

**Prós**:
- ✅ Desenvolvido em Node.js (fácil para devs JavaScript)
- ✅ Fácil de estender programaticamente
- ✅ API programática nativa
- ✅ WebSocket support
- ✅ HTTP-FLV support
- ✅ Instalação via npm

**Contras**:
- ❌ Performance inferior ao Nginx-RTMP e SRS
- ❌ Single-threaded (limitação do Node.js)
- ❌ Menos estável para alta carga
- ❌ Consome mais memória
- ❌ Menos testado em produção

**Quando escolher Node-Media-Server**:
- Equipe é primariamente JavaScript
- Necessidade de customizações complexas
- Projeto pequeno/médio (não milhares de viewers)
- Integração profunda com backend Node.js

### Alternativas para Transcodificação

#### GStreamer

**Para que serve**:
Framework multimídia modular desenvolvido em C. Permite construir pipelines de processamento de mídia de forma programática com blocos reutilizáveis.

**Responsabilidades (mesmas do FFmpeg)**:
- Receber stream de vídeo
- Transcodificar para múltiplas qualidades
- Gerar segmentos HLS
- Processar áudio e vídeo

**Prós**:
- ✅ Arquitetura modular (plugins)
- ✅ API programática robusta (C, Python, outros)
- ✅ Mais fácil de integrar em código
- ✅ Debugging melhor que FFmpeg
- ✅ Desenvolvimento ativo
- ✅ Hardware acceleration bem suportado
- ✅ Pipelines reutilizáveis

**Contras**:
- ❌ Curva de aprendizado íngreme
- ❌ Menos documentação que FFmpeg
- ❌ Menos plugins que FFmpeg
- ❌ Performance levemente inferior ao FFmpeg
- ❌ Configuração mais complexa

**Quando escolher GStreamer**:
- Necessidade de controle programático fino
- Pipelines complexos e customizados
- Integração profunda com aplicação
- Equipe prefere APIs a comandos CLI

**Comparação direta**:
| Critério | FFmpeg | GStreamer |
|----------|--------|-----------|
| Performance | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| Formatos | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| CLI | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| API Programática | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| Documentação | ⭐⭐⭐ | ⭐⭐ |
| Debugging | ⭐⭐ | ⭐⭐⭐⭐ |
| Comunidade | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |

### Alternativas para Banco de Dados

#### MySQL

**Para que serve**:
Banco de dados relacional open-source, alternativa ao PostgreSQL. É o banco relacional mais popular do mundo.

**Responsabilidades (mesmas do PostgreSQL)**:
- Armazenar dados de streams
- Persistir eventos e métricas
- Queries transacionais

**Prós**:
- ✅ Extremamente popular (mais que PostgreSQL)
- ✅ Performance excelente para reads
- ✅ Replicação master-slave simples
- ✅ Mais tutoriais e recursos
- ✅ Configuração mais simples
- ✅ Menos consumo de recursos

**Contras**:
- ❌ Menos features avançadas que PostgreSQL
- ❌ Sem suporte a JSONB nativo
- ❌ Window functions limitadas
- ❌ Full-text search inferior
- ❌ Extensibilidade limitada

**Quando escolher MySQL**:
- Equipe já conhece MySQL
- Simplicidade é prioridade
- Não precisa de features avançadas
- Foco em performance de reads

#### MongoDB

**Para que serve**:
Banco de dados NoSQL orientado a documentos. Armazena dados em formato JSON-like (BSON), sem schema fixo.

**Responsabilidades**:
- Armazenar dados semi-estruturados
- Armazenar eventos com estruturas variáveis
- Queries flexíveis

**Prós**:
- ✅ Schema flexível (sem migrações rígidas)
- ✅ Performance excelente para writes
- ✅ Horizontal scaling nativo (sharding)
- ✅ Bom para dados não relacionais
- ✅ Queries em JSON natural
- ✅ Aggregation framework poderoso
- ✅ Replica sets para alta disponibilidade

**Contras**:
- ❌ Sem ACID completo (antes versão 4.0)
- ❌ Sem JOINs tradicionais
- ❌ Consumo de memória alto
- ❌ Backup/restore mais complexo
- ❌ Não ideal para dados altamente relacionais
- ❌ Pode levar a duplicação de dados

**Quando escolher MongoDB**:
- Dados têm estrutura variável
- Necessidade de escalabilidade horizontal
- Workload write-heavy
- Dados são documentos completos (não muitos relacionamentos)

**Comparação direta**:
| Critério | PostgreSQL | MySQL | MongoDB |
|----------|------------|-------|---------|
| Performance Reads | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| Performance Writes | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| ACID | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| Features Avançadas | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| Escalabilidade | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Flexibilidade Schema | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| Facilidade | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ |

### Alternativas para Cache

#### Memcached

**Para que serve**:
Sistema de cache distribuído em memória, mais simples que Redis. Funciona como um hashmap gigante distribuído.

**Responsabilidades**:
- Cachear dados frequentemente acessados
- Reduzir latência de reads

**Prós**:
- ✅ Extremamente simples
- ✅ Performance excelente
- ✅ Multithreaded (melhor uso de CPU)
- ✅ Consumo de memória ligeiramente menor que Redis
- ✅ Distribuído nativamente
- ✅ Protocolo muito simples

**Contras**:
- ❌ Apenas strings (sem estruturas de dados)
- ❌ Sem persistência
- ❌ Sem Pub/Sub
- ❌ Sem replicação
- ❌ Funcionalidades muito limitadas
- ❌ Sem clustering nativo

**Quando escolher Memcached**:
- Apenas precisa de cache key-value simples
- Não precisa de persistência
- Necessidade de multithread (workload pesado)
- Simplicidade operacional é importante

**Comparação direta**:
| Critério | Redis | Memcached |
|----------|-------|-----------|
| Performance | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Features | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| Estruturas de Dados | ⭐⭐⭐⭐⭐ | ⭐ |
| Persistência | ⭐⭐⭐⭐ | ❌ |
| Pub/Sub | ⭐⭐⭐⭐⭐ | ❌ |
| Multithreading | ❌ | ⭐⭐⭐⭐⭐ |
| Simplicidade | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

---

## 🔄 Fluxos do Sistema

### Fluxo de Criação de Stream

**Objetivo**: Permitir que um usuário crie uma nova transmissão e obtenha as credenciais necessárias.

**Atores**: Usuário (Streamer), Frontend, Stream Service, PostgreSQL, Redis, Message Broker

**Etapas detalhadas**:

1. **Usuário acessa** `stream.localhost/` no navegador
2. **Frontend renderiza** página inicial com botão "Iniciar Streaming"
3. **Usuário clica** em "Iniciar Streaming"
4. **Frontend abre modal** solicitando título e descrição (opcional)
5. **Usuário preenche** e confirma
6. **Frontend envia** requisição HTTP POST para Stream Service
7. **Stream Service valida** dados recebidos
8. **Stream Service gera** stream key única (UUID ou hash SHA-256)
9. **Stream Service cria** registro no PostgreSQL com status `WAITING`
10. **Stream Service armazena** sessão no Redis com TTL (ex: 2 horas)
11. **Stream Service publica** evento `stream_created` no message broker
12. **Consumer Service** (em background) consome evento e registra métricas iniciais
13. **Stream Service retorna** para Frontend:
    - Stream ID
    - URL RTMP: `rtmp://localhost:1935/live`
    - Stream Key gerada
    - URL de visualização: `stream.localhost/watch/{id}`
14. **Frontend exibe** painel com as credenciais
15. **Frontend mostra** instruções de como configurar no OBS

**Diagrama disponível em**: [diagrams/03-fluxo-criacao-stream.mermaid](diagrams/03-fluxo-criacao-stream.mermaid)

### Fluxo de Transmissão (Streaming)

**Objetivo**: Receber o vídeo do OBS, processar e disponibilizar para visualização.

**Atores**: Streamer, OBS Studio, Nginx-RTMP, Stream Service, FFmpeg, Message Broker, Consumer Service

**Etapas detalhadas**:

1. **Streamer configura OBS** com URL RTMP e stream key
2. **Streamer inicia transmissão** no OBS
3. **OBS estabelece** conexão RTMP com Nginx-RTMP na porta 1935
4. **Nginx-RTMP recebe** tentativa de publicação
5. **Nginx-RTMP faz callback HTTP** para Stream Service: `POST /api/streams/callback/publish`
6. **Stream Service valida** stream key no Redis e PostgreSQL
7. **Stream Service retorna** 200 OK (aceita) ou 403 Forbidden (rejeita)
8. **Se aceito**, Nginx-RTMP começa a receber stream
9. **Nginx-RTMP invoca FFmpeg** via `exec` directive
10. **FFmpeg recebe** stream bruto e inicia transcodificação
11. **FFmpeg gera** múltiplas qualidades simultaneamente:
    - 1080p em `/tmp/hls/stream-key-1080p/`
    - 720p em `/tmp/hls/stream-key-720p/`
    - 480p em `/tmp/hls/stream-key-480p/`
    - 360p em `/tmp/hls/stream-key-360p/`
12. **FFmpeg cria** segmentos `.ts` (geralmente 3-6 segundos cada)
13. **FFmpeg atualiza** playlists `.m3u8` continuamente
14. **Nginx-RTMP faz callback**: `POST /api/streams/callback/publish_done` (stream iniciou)
15. **Stream Service atualiza** status para `LIVE` no PostgreSQL
16. **Stream Service publica** evento `stream_started` no message broker
17. **Consumer Service consome** evento e:
    - Atualiza estatísticas
    - Inicia coleta de métricas de streaming
18. **Stream Service notifica** via WebSocket todos os clientes conectados
19. **Frontend atualiza** interface mostrando stream como LIVE

**Diagrama disponível em**: [diagrams/04-fluxo-transmissao.mermaid](diagrams/04-fluxo-transmissao.mermaid)

### Fluxo de Visualização

**Objetivo**: Permitir que espectadores assistam à transmissão ao vivo.

**Atores**: Espectador, Frontend, Stream Service, Nginx HLS Server, WebSocket

**Etapas detalhadas**:

1. **Espectador acessa** link compartilhado: `stream.localhost/watch/{stream-id}`
2. **Frontend carrega** página de visualização
3. **Frontend requisita** status da stream: `GET /api/streams/{id}/status`
4. **Stream Service consulta** Redis e retorna:
    - Status (WAITING, LIVE, ENDED)
    - Viewer count atual
    - URL do HLS
5. **Se status = WAITING**, Frontend exibe mensagem "Aguardando transmissão"
6. **Se status = LIVE**, Frontend:
    - Inicializa player HLS (Video.js ou HLS.js)
    - Carrega playlist master: `http://localhost:8081/hls/{stream-key}/master.m3u8`
7. **Player requisita** playlist master do Nginx HLS Server
8. **Nginx retorna** playlist com links para playlists de cada qualidade
9. **Player escolhe** qualidade inicial (geralmente baseado em bandwidth)
10. **Player requisita** playlist da qualidade escolhida: `{stream-key}-720p.m3u8`
11. **Nginx retorna** playlist com lista de segmentos disponíveis
12. **Player começa a baixar** segmentos `.ts` sequencialmente
13. **Player renderiza** vídeo conforme segmentos são baixados
14. **Frontend conecta** ao WebSocket do Stream Service
15. **Frontend subscreve** tópico: `/topic/stream/{id}/viewers`
16. **Frontend envia** evento `viewer_joined`
17. **Stream Service atualiza** contador de viewers no Redis
18. **Stream Service publica** evento `viewer_joined` no message broker
19. **Consumer Service atualiza** métricas de viewers
20. **Stream Service notifica** via WebSocket novo viewer count
21. **Frontend atualiza** contador na interface
22. **Player continua** requisitando novos segmentos a cada 3-6 segundos (adaptive)
23. **Player ajusta** qualidade automaticamente baseado em bandwidth (ABR - Adaptive Bitrate)

**Quando espectador sai**:
24. **Frontend envia** evento `viewer_left` ao desconectar WebSocket
25. **Stream Service decrementa** contador
26. **Stream Service publica** evento no broker
27. **Outros viewers** são notificados do novo count

**Diagrama disponível em**: [diagrams/05-fluxo-visualizacao.mermaid](diagrams/05-fluxo-visualizacao.mermaid)

### Fluxo de Encerramento de Stream

**Objetivo**: Finalizar transmissão e limpar recursos.

**Atores**: Streamer, OBS, Nginx-RTMP, Stream Service, FFmpeg, Message Broker

**Etapas detalhadas**:

1. **Streamer clica** "Parar Streaming" no OBS ou fecha o OBS
2. **OBS encerra** conexão RTMP
3. **Nginx-RTMP detecta** desconexão
4. **Nginx-RTMP termina** processo FFmpeg (SIGTERM)
5. **FFmpeg finaliza** transcodificação e fecha arquivos
6. **Nginx-RTMP faz callback**: `POST /api/streams/callback/publish_done`
7. **Stream Service atualiza** status para `ENDED` no PostgreSQL
8. **Stream Service registra** timestamp de encerramento
9. **Stream Service calcula** métricas finais (duração total, pico de viewers)
10. **Stream Service publica** evento `stream_ended` no message broker
11. **Consumer Service consome** evento e:
    - Persiste eventos históricos
    - Calcula estatísticas agregadas
    - Limpa cache no Redis
12. **Stream Service notifica** via WebSocket todos os viewers
13. **Frontend dos viewers** exibe mensagem "Transmissão encerrada"
14. **Task agendada** no Consumer Service limpa arquivos HLS antigos (após algumas horas)

---

## 📊 Monitoramento e Métricas

### Métricas Coletadas

#### Performance Metrics
- **RTMP Latency**: Tempo de recepção do stream RTMP
- **Transcoding Latency**: Tempo de transcodificação do FFmpeg
- **HLS Latency**: Tempo de entrega dos segmentos HLS
- **FPS**: Frames por segundo
- **Bitrate**: Taxa de bits de entrada e saída

#### Resource Metrics
- **CPU Usage**: Uso de CPU por componente
- **Memory Usage**: Uso de memória
- **Disk I/O**: Leitura/escrita em disco
- **Network Bandwidth**: Largura de banda utilizada

#### Quality Metrics
- **PSNR**: Peak Signal-to-Noise Ratio
- **SSIM**: Structural Similarity Index
- **Dropped Frames**: Quadros perdidos
- **Buffer Underruns**: Eventos de buffer vazio

#### Business Metrics
- **Concurrent Viewers**: Espectadores simultâneos
- **Peak Viewers**: Pico de espectadores
- **Stream Duration**: Duração total da stream
- **Total Streams**: Total de streams criadas


## 📖 Referências

### Documentação Externa

- [Nginx-RTMP Module](https://github.com/arut/nginx-rtmp-module)
- [FFmpeg Documentation](https://ffmpeg.org/documentation.html)
- [HLS Specification](https://tools.ietf.org/html/rfc8216)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [React Documentation](https://react.dev/)
- [Prometheus Documentation](https://prometheus.io/docs/)

### Documentação Interna

- [TODO e Roadmap](todo.md)
- [Guia de Setup](setup.md)
- [Diagramas de Arquitetura](diagrams/)

---

**Última atualização**: 20/01/2026  
**Versão**: 1.0.0

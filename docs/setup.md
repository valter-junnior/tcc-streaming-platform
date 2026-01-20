# Guia de Setup - Plataforma de Streaming

## 📋 Pré-requisitos

### Software Necessário

#### Obrigatórios
- **Docker** (versão 20.10+)
  ```bash
  docker --version
  ```
- **Docker Compose** (versão 2.0+)
  ```bash
  docker compose version
  ```
- **Java JDK** (versão 17+)
  ```bash
  java -version
  ```
- **Maven** (versão 3.8+)
  ```bash
  mvn -version
  ```
- **Node.js** (versão 18+)
  ```bash
  node --version
  ```
- **npm ou yarn**
  ```bash
  npm --version
  ```

#### Recomendados
- **Git**
  ```bash
  git --version
  ```
- **OBS Studio** (para testes de streaming)
- **Postman ou Insomnia** (para testes de API)

### Hardware Recomendado
- **CPU**: 4 cores ou mais
- **RAM**: 8GB mínimo, 16GB recomendado
- **Disco**: 20GB de espaço livre
- **Rede**: Conexão estável (upload mínimo de 5 Mbps para streaming)

## 🚀 Setup Inicial

### 1. Clonar o Repositório

```bash
# Clone o repositório (quando criado)
git clone https://github.com/seu-usuario/tcc-streaming-platform.git
cd tcc-streaming-platform
```

### 2. Configurar Variáveis de Ambiente

Crie um arquivo `.env` na raiz do projeto:

```bash
# .env
# Database
POSTGRES_DB=streaming_db
POSTGRES_USER=streaming_user
POSTGRES_PASSWORD=streaming_pass
POSTGRES_PORT=5432

# Redis
REDIS_PORT=6379
REDIS_PASSWORD=redis_pass

# Kafka
KAFKA_PORT=9092
KAFKA_ZOOKEEPER_PORT=2181

# Backend
BACKEND_PORT=8080
BACKEND_PROFILE=dev

# Frontend
FRONTEND_PORT=3000

# Nginx RTMP
RTMP_PORT=1935

# Nginx HLS
HLS_PORT=8081

# Monitoring
PROMETHEUS_PORT=9090
GRAFANA_PORT=3001
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=admin

# Stream Configuration
STREAM_KEY_LENGTH=32
MAX_STREAMS_PER_USER=5
STREAM_TIMEOUT_MINUTES=30
```

### 3. Estrutura de Pastas

Crie a estrutura inicial:

```bash
mkdir -p app/{backend,frontend,config/{nginx,prometheus,grafana}}
mkdir -p docker/{nginx-rtmp,kafka,rabbitmq,redis,postgres,monitoring}
mkdir -p scripts/{setup,monitoring}
mkdir -p docker-compose
mkdir -p docs
mkdir -p data/{postgres,redis,kafka,hls-segments}
```

## 🐳 Setup com Docker

### 1. Docker Compose Principal

Crie o arquivo `docker-compose/docker-compose.yml`:

```yaml
version: '3.8'

services:
  # PostgreSQL
  postgres:
    image: postgres:15-alpine
    container_name: streaming-postgres
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    ports:
      - "${POSTGRES_PORT}:5432"
    volumes:
      - ./data/postgres:/var/lib/postgresql/data
    networks:
      - streaming-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER}"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis
  redis:
    image: redis:7-alpine
    container_name: streaming-redis
    command: redis-server --requirepass ${REDIS_PASSWORD}
    ports:
      - "${REDIS_PORT}:6379"
    volumes:
      - ./data/redis:/data
    networks:
      - streaming-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Zookeeper (para Kafka)
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: streaming-zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
      ZOOKEEPER_TICK_TIME: 2000
    ports:
      - "${KAFKA_ZOOKEEPER_PORT}:2181"
    networks:
      - streaming-network

  # Kafka
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    container_name: streaming-kafka
    depends_on:
      - zookeeper
    ports:
      - "${KAFKA_PORT}:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
    networks:
      - streaming-network
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:9092"]
      interval: 30s
      timeout: 10s
      retries: 5

  # Nginx RTMP
  nginx-rtmp:
    build:
      context: ../docker/nginx-rtmp
      dockerfile: Dockerfile
    container_name: streaming-nginx-rtmp
    ports:
      - "${RTMP_PORT}:1935"
      - "${HLS_PORT}:80"
    volumes:
      - ../app/config/nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./data/hls-segments:/tmp/hls
    networks:
      - streaming-network
    depends_on:
      - kafka

  # Prometheus
  prometheus:
    image: prom/prometheus:latest
    container_name: streaming-prometheus
    ports:
      - "${PROMETHEUS_PORT}:9090"
    volumes:
      - ../app/config/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - ./data/prometheus:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
    networks:
      - streaming-network

  # Grafana
  grafana:
    image: grafana/grafana:latest
    container_name: streaming-grafana
    ports:
      - "${GRAFANA_PORT}:3000"
    environment:
      GF_SECURITY_ADMIN_USER: ${GRAFANA_ADMIN_USER}
      GF_SECURITY_ADMIN_PASSWORD: ${GRAFANA_ADMIN_PASSWORD}
    volumes:
      - ../app/config/grafana:/etc/grafana/provisioning
      - ./data/grafana:/var/lib/grafana
    networks:
      - streaming-network
    depends_on:
      - prometheus

networks:
  streaming-network:
    driver: bridge

volumes:
  postgres-data:
  redis-data:
  kafka-data:
  prometheus-data:
  grafana-data:
  hls-segments:
```

### 2. Dockerfile para Nginx RTMP

Crie `docker/nginx-rtmp/Dockerfile`:

```dockerfile
FROM buildpack-deps:bullseye

# Install dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    libpcre3 \
    libpcre3-dev \
    libssl-dev \
    zlib1g-dev \
    wget \
    git \
    ffmpeg

# Download Nginx
WORKDIR /tmp
RUN wget http://nginx.org/download/nginx-1.24.0.tar.gz && \
    tar -zxvf nginx-1.24.0.tar.gz

# Clone nginx-rtmp-module
RUN git clone https://github.com/arut/nginx-rtmp-module.git

# Build Nginx with RTMP module
WORKDIR /tmp/nginx-1.24.0
RUN ./configure --with-http_ssl_module --add-module=../nginx-rtmp-module && \
    make && \
    make install

# Clean up
RUN rm -rf /tmp/*

# Create directories
RUN mkdir -p /tmp/hls /var/log/nginx

EXPOSE 1935 80

CMD ["/usr/local/nginx/sbin/nginx", "-g", "daemon off;"]
```

### 3. Configuração do Nginx

Crie `app/config/nginx/nginx.conf`:

```nginx
worker_processes auto;
rtmp_auto_push on;

events {
    worker_connections 1024;
}

# RTMP configuration
rtmp {
    server {
        listen 1935;
        chunk_size 4096;
        
        application live {
            live on;
            record off;
            
            # HLS
            hls on;
            hls_path /tmp/hls;
            hls_fragment 3;
            hls_playlist_length 60;
            
            # Callbacks to backend
            on_publish http://host.docker.internal:8080/api/streams/callback/publish;
            on_publish_done http://host.docker.internal:8080/api/streams/callback/publish_done;
            on_play http://host.docker.internal:8080/api/streams/callback/play;
            on_record_done http://host.docker.internal:8080/api/streams/callback/record_done;
            
            # FFmpeg transcoding
            exec ffmpeg -i rtmp://localhost/live/$name
                -c:v libx264 -preset veryfast -tune zerolatency -profile:v baseline -level 3.0
                -s 1920x1080 -b:v 5000k -maxrate 5000k -bufsize 10000k -g 60 -r 30
                -c:a aac -b:a 128k -ar 44100
                -f flv rtmp://localhost/hls/$name_1080p
                
                -c:v libx264 -preset veryfast -tune zerolatency -profile:v baseline -level 3.0
                -s 1280x720 -b:v 2800k -maxrate 2800k -bufsize 5600k -g 60 -r 30
                -c:a aac -b:a 128k -ar 44100
                -f flv rtmp://localhost/hls/$name_720p
                
                -c:v libx264 -preset veryfast -tune zerolatency -profile:v baseline -level 3.0
                -s 854x480 -b:v 1400k -maxrate 1400k -bufsize 2800k -g 60 -r 30
                -c:a aac -b:a 96k -ar 44100
                -f flv rtmp://localhost/hls/$name_480p
                
                -c:v libx264 -preset veryfast -tune zerolatency -profile:v baseline -level 3.0
                -s 640x360 -b:v 800k -maxrate 800k -bufsize 1600k -g 60 -r 30
                -c:a aac -b:a 64k -ar 44100
                -f flv rtmp://localhost/hls/$name_360p;
        }
        
        application hls {
            live on;
            hls on;
            hls_path /tmp/hls;
            hls_fragment 3;
            hls_playlist_length 60;
            hls_nested on;
            
            hls_variant _1080p BANDWIDTH=5128000;
            hls_variant _720p BANDWIDTH=2928000;
            hls_variant _480p BANDWIDTH=1496000;
            hls_variant _360p BANDWIDTH=864000;
        }
    }
}

# HTTP configuration
http {
    include mime.types;
    default_type application/octet-stream;
    
    sendfile on;
    tcp_nopush on;
    
    server {
        listen 80;
        
        # Serve HLS fragments
        location /hls {
            types {
                application/vnd.apple.mpegurl m3u8;
                video/mp2t ts;
            }
            root /tmp;
            add_header Cache-Control no-cache;
            add_header Access-Control-Allow-Origin *;
        }
        
        # RTMP stat
        location /stat {
            rtmp_stat all;
            rtmp_stat_stylesheet stat.xsl;
        }
        
        location /stat.xsl {
            root /path/to/nginx-rtmp-module;
        }
    }
}
```

### 4. Configuração do Prometheus

Crie `app/config/prometheus/prometheus.yml`:

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'prometheus'
    static_configs:
      - targets: ['localhost:9090']

  - job_name: 'stream-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8080']

  - job_name: 'consumer-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8081']

  - job_name: 'metrics-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8082']

  - job_name: 'kafka'
    static_configs:
      - targets: ['kafka:9092']
```

## ⚙️ Setup do Backend (Spring Boot)

### 1. Criar Projeto Stream Service

```bash
cd app/backend
mkdir stream-service
cd stream-service

# Usando Spring Initializr CLI ou manual
# Dependencies: Web, WebSocket, JPA, PostgreSQL, Redis, Kafka, Actuator
```

### 2. `pom.xml` do Stream Service

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>
    
    <groupId>com.tcc</groupId>
    <artifactId>stream-service</artifactId>
    <version>1.0.0</version>
    <name>Stream Service</name>
    
    <properties>
        <java.version>17</java.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Micrometer for Prometheus -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 3. `application.yml`

```yaml
spring:
  application:
    name: stream-service
  
  datasource:
    url: jdbc:postgresql://localhost:5432/${POSTGRES_DB}
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
  
  data:
    redis:
      host: localhost
      port: ${REDIS_PORT}
      password: ${REDIS_PASSWORD}
  
  kafka:
    bootstrap-servers: localhost:${KAFKA_PORT}
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer
    consumer:
      group-id: stream-service-group
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer

server:
  port: ${BACKEND_PORT}

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true

streaming:
  rtmp:
    url: rtmp://localhost:${RTMP_PORT}/live
  hls:
    base-url: http://localhost:${HLS_PORT}/hls
  stream-key-length: ${STREAM_KEY_LENGTH}
  max-streams-per-user: ${MAX_STREAMS_PER_USER}
  stream-timeout-minutes: ${STREAM_TIMEOUT_MINUTES}
```

## 🎨 Setup do Frontend (React)

### 1. Criar Aplicação React

```bash
cd app/frontend
npx create-react-app web --template typescript
cd web
```

### 2. Instalar Dependências

```bash
npm install axios
npm install video.js @videojs/http-streaming
npm install @types/video.js --save-dev
npm install react-router-dom
npm install sockjs-client
npm install @stomp/stompjs
```

### 3. Configuração Básica

Criar `src/config/api.ts`:

```typescript
export const API_CONFIG = {
  BASE_URL: process.env.REACT_APP_API_URL || 'http://localhost:8080',
  WS_URL: process.env.REACT_APP_WS_URL || 'ws://localhost:8080/ws',
  HLS_URL: process.env.REACT_APP_HLS_URL || 'http://localhost:8081/hls',
};
```

## 🔧 Scripts de Automação

### 1. Script de Setup Inicial

Crie `scripts/setup.sh`:

```bash
#!/bin/bash

echo "🚀 Iniciando setup do projeto..."

# Criar estrutura de diretórios
echo "📁 Criando estrutura de diretórios..."
mkdir -p data/{postgres,redis,kafka,hls-segments,prometheus,grafana}

# Copiar arquivo .env de exemplo
if [ ! -f .env ]; then
    echo "📝 Criando arquivo .env..."
    cp .env.example .env
    echo "⚠️  Configure as variáveis no arquivo .env"
fi

# Build das imagens Docker
echo "🐳 Construindo imagens Docker..."
cd docker-compose
docker compose build

echo "✅ Setup concluído!"
echo "Execute 'docker compose up -d' para iniciar os serviços"
```

### 2. Script de Start

Crie `scripts/start-services.sh`:

```bash
#!/bin/bash

echo "🚀 Iniciando serviços..."

# Start Docker services
cd docker-compose
docker compose up -d

# Wait for services
echo "⏳ Aguardando serviços iniciarem..."
sleep 10

# Check health
echo "🏥 Verificando saúde dos serviços..."
docker compose ps

echo "✅ Serviços iniciados!"
echo "📊 Acesse:"
echo "   - Frontend: http://localhost:3000"
echo "   - Backend API: http://localhost:8080"
echo "   - Grafana: http://localhost:3001"
echo "   - Prometheus: http://localhost:9090"
```

## ✅ Verificação de Setup

### 1. Testar Docker Compose

```bash
cd docker-compose
docker compose up -d
docker compose ps
```

### 2. Testar Backend

```bash
cd app/backend/stream-service
mvn clean install
mvn spring-boot:run
```

Acesse: `http://localhost:8080/actuator/health`

### 3. Testar Frontend

```bash
cd app/frontend/web
npm start
```

Acesse: `http://localhost:3000`

### 4. Testar Nginx RTMP

Use OBS Studio:
- Server: `rtmp://localhost:1935/live`
- Stream Key: `test`

Verifique: `http://localhost:8081/hls/test.m3u8`

## 🐛 Troubleshooting

### Problemas Comuns

#### Porta já em uso
```bash
# Verificar portas em uso
sudo lsof -i :8080
sudo lsof -i :3000

# Matar processo
kill -9 <PID>
```

#### Docker não inicia
```bash
# Verificar logs
docker compose logs <service-name>

# Restart
docker compose restart <service-name>
```

#### Permissões de arquivo
```bash
# Dar permissão aos scripts
chmod +x scripts/*.sh
```

## 📚 Próximos Passos

1. Implementar modelos de dados no backend
2. Criar APIs REST
3. Implementar componentes React
4. Configurar WebSocket
5. Integrar Kafka
6. Configurar monitoramento

---

**Última atualização**: 20/01/2026

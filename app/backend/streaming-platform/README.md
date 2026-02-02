# Streaming Platform - Backend

Backend da plataforma de streaming, implementado como um único projeto Spring Boot.

## 📦 Estrutura de Pacotes

```
com.tcc.streaming/
├── StreamingPlatformApplication.java    # Classe principal
├── common/                              # Código compartilhado (models, DTOs, utils)
├── stream/                              # Stream Service (API REST + WebSocket)
├── consumer/                            # Consumer Service (processamento de eventos)
└── metrics/                             # Metrics Service (coleta de métricas)
```

## 🛠️ Tecnologias

- **Java**: 21
- **Spring Boot**: 3.2.2
- **Maven**: 3.9+
- **Database**: PostgreSQL
- **Cache**: Redis
- **Message Broker**: RabbitMQ
- **Monitoring**: Prometheus + Actuator

## 🚀 Como Compilar

### Via Docker (Recomendado)

```bash
docker run --rm \
  -v "$PWD":/app \
  -v ~/.m2:/root/.m2 \
  -w /app \
  maven:3.9-eclipse-temurin-21 \
  mvn clean package -DskipTests
```

### Localmente (se Java 21 e Maven instalados)

```bash
mvn clean package -DskipTests
```

## 🧪 Como Testar

```bash
# Via Docker
docker run --rm \
  -v "$PWD":/app \
  -v ~/.m2:/root/.m2 \
  -w /app \
  maven:3.9-eclipse-temurin-21 \
  mvn test
```

## 🐳 Como Executar (Docker Compose)

O serviço é executado via Docker Compose na raiz do projeto:

```bash
cd ../../..  # Volta para a raiz do projeto
docker-compose up -d
```

## 🔌 Porta

- **Aplicação**: 8080

## 📚 Documentação

Veja a documentação completa em: `docs/documentacao_v2.md`

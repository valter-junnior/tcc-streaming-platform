# Plataforma de Streaming com Análise Comparativa

**TCC** - Trabalho de Conclusão de Curso  
**Período**: Janeiro 2026 - Junho 2026

## 📋 Sobre o Projeto

Plataforma de streaming de vídeo ao vivo que permite avaliar e comparar o desempenho de diferentes tecnologias para as mesmas funcionalidades.

## 🏗️ Estrutura do Projeto

```
tcc/
├── app/
│   ├── backend/streaming-platform/    # Backend Spring Boot
│   ├── frontend/web/                  # Frontend React
│   └── config/                        # Configurações (Nginx, Prometheus, Grafana)
├── docker/                            # Dockerfiles customizados
├── docs/                              # Documentação
├── scripts/                           # Scripts utilitários
└── docker-compose.yml                 # Orquestração de containers
```

## 🚀 Quick Start

### Pré-requisitos

- Docker e Docker Compose
- Java 21 (para desenvolvimento local)
- Node.js 20+ (para desenvolvimento local)

### Iniciar Infraestrutura

```bash
# Subir PostgreSQL, Redis e RabbitMQ
docker-compose up -d
```

### Verificar Status

```bash
docker-compose ps
```

## 🔗 Acessos

- **RabbitMQ Management**: http://localhost:15672 (streaming_user / streaming_pass)
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379

## 📚 Documentação

- [Documentação Completa](docs/documentacao_v2.md)
- [TODO - Roadmap](docs/todo.md)
- [Utils - Orientações Especiais](docs/utils.md)

## 🛠️ Stack Tecnológico

- **Backend**: Java 21 + Spring Boot 3.2.2
- **Frontend**: React 18 + TypeScript
- **Streaming**: Nginx-RTMP + FFmpeg
- **Database**: PostgreSQL
- **Cache**: Redis
- **Message Broker**: RabbitMQ
- **Monitoring**: Prometheus + Grafana

## 📊 Progresso

Veja [docs/todo.md](docs/todo.md) para acompanhar o progresso do desenvolvimento.

---

**Status**: 🚀 Em Desenvolvimento  
**Última Atualização**: 02/02/2026

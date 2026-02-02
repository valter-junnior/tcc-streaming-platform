# Utils - Orientações do Projeto

Orientações específicas que devem ser consultadas ao retomar o trabalho.

---

### TODO

Atualize sempre o todo.md após fazer uma tarefa dele

---

## Dúvidas

Crie `/docs/questions.md` quando tiver dúvidas ou precisar de aprovação:

```markdown
# Questions - [Data]
## Questão: [Título]
**Contexto**: ...
**Dúvida**: ...
**Opções**: A, B, C
**Recomendação**: ...
```

---

## Arquitetura

### Backend - Projeto Único Spring Boot

```
app/backend/streaming-platform/src/main/java/com/tcc/streaming/
├── StreamingPlatformApplication.java
├── common/      # Código compartilhado
├── stream/      # Stream Service
├── consumer/    # Consumer Service  
└── metrics/     # Metrics Service
```

### Clean Architecture por Domain

```
domain/
├── core/                      # Domínio (Java puro)
│   ├── entities/
│   ├── exceptions/
│   ├── repositories/          # Interfaces
│   ├── usecases/              # Interfaces
│   └── dtos/
├── application/               # Casos de uso
│   └── services/              # Implementações
└── infrastructure/            # Adapters
    ├── config/
    ├── database/jpa/
    │   ├── entities/          # @Entity
    │   ├── repositories/      # Spring Data
    │   └── mappers/           # Domain ↔ JPA
    ├── http/
    │   ├── controllers/
    │   ├── requests/          # @Valid
    │   ├── presenters/
    │   └── handlers/
    └── ws/controllers/
```

**Fluxo**: HTTP → Controller → Service → Repository → JPA → DB

**Regra**: Infrastructure → Application → Core (nunca o inverso)

---

## 🐳 Comandos

### Setup
```bash
cp .env.example .env
docker compose up -d
docker compose logs -f streaming-platform
```

### Desenvolvimento
- Edite .java e salve → hot reload automático (~10s)
- Build manual: `docker run --rm -v "$PWD":/app -v ~/.m2:/root/.m2 -w /app maven:3.9-eclipse-temurin-21 mvn clean package -DskipTests`

### Testes
```bash
docker compose up -d postgres redis rabbitmq
docker compose exec streaming-platform mvn test
```

---

## Documentação

### Changelog Obrigatório

**Quando**: Ao completar task do [todo.md](./todo.md)  
**Onde**: `/docs/changelog/YYYY-MM-DD_HHhMM.md`

**REGRAS IMPORTANTES**:
- **SEMPRE criar changelog** após completar uma task
- **NUNCA criar arquivos .md extras** sem necessidade
- **Evite**: SETUP.md, GUIDE.md, HOW_TO.md, ARCHITECTURE.md, etc
- **Evite**: READMEs em pastas de código (apenas se realmente necessário)
- **Changelog é suficiente** para documentar mudanças
- Mantenha documentação **simples e objetiva**

**Formato Simples**:
```markdown
# Changelog - [Nome Task]

## Objetivo
Breve descrição.

## Alterações
- Arquivo X criado
- Arquivo Y modificado

## Validação
- Comando executado
- Resultado obtido
```

**Observação**: Changelogs devem ser diretos. Evite repetir informações já presentes em outros docs.

### TODO.md
Sempre atualizar ao completar/adicionar tarefas ou encontrar bloqueadores.

---

## Portas Reservadas

| Porta | Serviço |
|-------|---------|
| 1935 | Nginx-RTMP |
| 8080 | Backend API |
| 8081 | Nginx HLS |
| 5432 | PostgreSQL |
| 6379 | Redis |
| 5672 | RabbitMQ |
| 15672 | RabbitMQ UI |
| 9090 | Prometheus |
| 3000 | Grafana |
| 3001 | Frontend |

---

**Atualizado**: 02/02/2026

# Utils - Orientações Especiais do Projeto

## 📘 Sobre este Arquivo

Este documento contém **orientações específicas do projeto** que devem ser sempre consultadas ao retomar o trabalho.

**⚠️ IMPORTANTE**: Atualizar este arquivo quando houver informações críticas sobre o projeto.

---

## 🏗️ Arquitetura Especial do Projeto

### Backend Java - Projeto Único

⚠️ **IMPORTANTE**: O backend é um **único projeto Spring Boot** (não multi-módulo):

```
app/backend/streaming-platform/
└── src/main/java/com/tcc/streaming/
    ├── StreamingPlatformApplication.java    # Classe principal
    ├── common/                              # Código compartilhado
    ├── stream/                              # Stream Service (REST + WebSocket)
    ├── consumer/                            # Consumer Service (eventos)
    └── metrics/                             # Metrics Service (coleta)
```

**Por quê?**
- Todos os serviços no mesmo código-fonte
- Único `pom.xml` e único JAR final
- Simplicidade no desenvolvimento e deploy

---

## 🐳 Comandos Específicos do Projeto

### Setup Inicial

⚠️ **PRIMEIRO PASSO**: Configurar variáveis de ambiente:

```bash
# Copiar template de variáveis
cp .env.example .env

# Editar valores (opcional - valores padrão funcionam)
nano .env
```

**Veja**: [/ENV_SETUP.md](../ENV_SETUP.md) para documentação completa

### Executar Projeto Completo (Hot Reload Ativado)

⚠️ **RECOMENDADO**: Backend roda no Docker Compose com **hot reload automático**:

```bash
# Iniciar todos os serviços (infraestrutura + backend)
docker-compose up -d

# Ver logs do backend em tempo real
docker-compose logs -f streaming-platform

# Edite qualquer arquivo .java e salve (Ctrl+S)
# O Spring Boot DevTools recompila e reinicia automaticamente (~10s)
```

**Veja**: [/app/backend/streaming-platform/HOT_RELOAD.md](../app/backend/streaming-platform/HOT_RELOAD.md) para guia completo

### Build Manual do Backend (Opcional)

Se precisar compilar manualmente fora do Docker Compose:

```bash
cd app/backend/streaming-platform
docker run --rm -v "$PWD":/app -v ~/.m2:/root/.m2 -w /app \
  maven:3.9-eclipse-temurin-21 mvn clean package -DskipTests
```

### Testes de Integração

⚠️ **Requerem infraestrutura completa** (PostgreSQL, Redis, RabbitMQ):

```bash
# Subir infraestrutura primeiro
docker-compose up -d postgres redis rabbitmq

# Depois executar testes
docker-compose exec streaming-platform mvn test
```
---

## 🔄 Manutenção do TODO.md

⚠️ **SEMPRE atualizar** [todo.md](./todo.md) quando:

- ✅ Completar uma tarefa
- ➕ Adicionar nova tarefa
- ⚠️ Encontrar bloqueadores

---

## 📝 Changelog Obrigatório

⚠️ **SEMPRE gerar changelog** ao final de cada task:

**Quando**: Ao completar qualquer task do [todo.md](./todo.md)

**Onde**: `/docs/changelog/YYYY-MM-DD_HHhMM.md`

**⚠️ IMPORTANTE**: 
- **NÃO criar arquivos .md extras** (como SETUP.md, GUIDE.md, HOW_TO.md, etc)
- **Apenas o changelog é necessário** para documentar mudanças
- O changelog já contém toda informação necessária
- Mantenha documentação simples e objetiva

**Formato**:
```markdown

**Observações**: Crie changelogs simples e direto
# Changelog - [Nome da Task]

**Data**: DD/MM/YYYY HH:MM  
**Task**: [Fase X.Y] Nome da Task  
**Status**: ✅ Concluída

## 🎯 Objetivo

Breve descrição do que foi feito.

## ✅ Alterações

- Item 1 criado/modificado
- Item 2 criado/modificado
- Item 3 criado/modificado

## 📁 Arquivos Afetados

- `/caminho/para/arquivo1`
- `/caminho/para/arquivo2`

## 🧪 Validação

- Comando executado e resultado
- Testes realizados

## 📊 Progresso

- **Fase atual**: X% completa
- **Progresso geral**: Y%

## 🚀 Próximos Passos

- Próxima task a ser realizada
```

**Exemplo de nome**: `2026-02-02_12h30.md`

---

## ⚠️ Portas Reservadas (Específicas do Projeto)

| Porta | Serviço | Observação |
|-------|---------|------------|
| 1935 | Nginx-RTMP | Ingestão RTMP do OBS |
| 8080 | Backend (Spring Boot) | API REST + WebSocket |
| 8081 | Nginx HLS | Servir segmentos HLS |
| 5432 | PostgreSQL | Dados persistentes |
| 6379 | Redis | Cache + sessões |
| 5672 | RabbitMQ | Message broker |
| 15672 | RabbitMQ Management | Interface web |
| 9090 | Prometheus | Coleta de métricas |
| 3000 | Grafana | Dashboards |
| 3001 | Frontend React | Interface web |

---

**Última Atualização**: 02/02/2026  
**Atualizado por**: Simplificação do utils.md

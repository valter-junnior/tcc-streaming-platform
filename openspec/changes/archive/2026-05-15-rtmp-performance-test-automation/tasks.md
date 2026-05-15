## 1. Baseline de benchmark em Docker

- [x] 1.1 Mapear e documentar no runner os serviços/containers necessários para cada combinação (`nginx+ffmpeg`, `nginx+gstreamer`, `srs+ffmpeg`, `srs+gstreamer`)
- [x] 1.2 Implementar script base de orquestração com modos `sanity` e `full` e parâmetros de duração/viewers
- [x] 1.3 Adicionar validações de pré-execução (containers saudáveis, rede e caminhos de logs/artefatos)

## 2. Fase sanity (caso simples por combinação)

- [x] 2.1 Implementar execução de um caso mínimo para `nginx+ffmpeg` com status pass/fail
- [x] 2.2 Generalizar a rotina para `nginx+gstreamer`, `srs+ffmpeg` e `srs+gstreamer` rodando um por vez
- [x] 2.3 Registrar evidências de execução (timestamps, status, links para logs) para depuração rápida

## 3. Coleta de métricas durante live ativa

- [x] 3.1 Implementar marcadores explícitos de início/fim da janela de medição (somente live ativa)
- [x] 3.2 Coletar métricas mínimas por cenário (cpu, memória, rede, viewers, duração, erros/timeout)
- [x] 3.3 Persistir resultados intermediários por execução para tolerância a falhas parciais

## 4. Campanha completa com comando único

- [x] 4.1 Implementar comando único para percorrer a matriz completa em ordem determinística
- [x] 4.2 Adicionar controle de retries curtos e política de continuidade para cenários que falharem
- [x] 4.3 Gerar resumo final no terminal com sucesso/falha por cenário e localização dos artefatos

## 5. Matriz CSV consolidada

- [x] 5.1 Definir schema CSV final com colunas de identificação, parâmetros, métricas e status de erro
- [x] 5.2 Implementar consolidação em arquivo CSV único por execução de campanha
- [x] 5.3 Validar consistência do CSV em execuções sanity e full (incluindo cenários com falha)

## 6. Documentação técnica de operação

- [x] 6.1 Criar um único `.md` técnico com objetivo, pré-requisitos, execução sanity/full e leitura do CSV
- [x] 6.2 Documentar fluxo de troubleshooting (logs, saúde de containers, rerun por cenário)
- [x] 6.3 Incluir próximos passos para expansão de carga e tempo de teste sem alterar código funcional da aplicação

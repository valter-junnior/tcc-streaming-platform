## Context

O benchmark RTMP já está funcional e executa os 12 cenários (4 combinações × 3 cargas de viewers) via `run-rtmp-benchmark.sh`. A execução de referência (`bench-full-20260507-095238`) usou `--duration 30 --repeats 1`, gerando dados insuficientes para o TCC: janela de medição entre 35s–84s e ausência de repetições impedem cálculo de desvio padrão.

O script já aceita os parâmetros `--duration` e `--repeats`; não há refatoração do orquestrador necessária. A lacuna está na falta de um pós-processador que consolide as repetições em estatísticas e gere o relatório final.

Estado atual dos critérios de aceitação do Plano de Testes:

| Critério | Status |
|---|---|
| Duração ≥ 5 min | ❌ (30s obtidos) |
| 3 repetições | ❌ (1 obtida) |
| CPU ≤ 80% em 1 viewer | ❌ (achado metodológico: medido no host com todos os serviços) |
| Startup HLS ≤ 15s | ⚠️ T05/T06 com 16s |
| Taxa de erros ≤ 1% | ❌ SRS+GStreamer: 13–42% |
| Latência ponta-a-ponta | ❌ ausente (requer medição manual) |

## Goals / Non-Goals

**Goals:**
- Re-executar o benchmark com `--duration 300 --repeats 3` para gerar 36 execuções válidas
- Implementar script de pós-processamento `aggregate-results.sh` que lê o CSV bruto e produz CSV agregado (média ± desvio padrão por métrica e combinação)
- Gerar relatório `report-tcc-final.md` com tabelas comparativas prontas para inserção no texto do TCC
- Documentar o achado de CPU elevado como limitação metodológica (ambiente compartilhado)

**Non-Goals:**
- Alterar a arquitetura do orquestrador de benchmark existente
- Implementar medição automática de latência ponta-a-ponta (mantida como medição manual conforme plano original)
- Adicionar novos cenários além dos 12 já definidos
- Migrar para ambiente de nuvem ou máquina dedicada

## Decisions

### D1 — Manter `run-rtmp-benchmark.sh` sem modificação estrutural

O script já suporta `--duration` e `--repeats`. A re-execução correta é feita apenas passando os parâmetros corretos. Alternativa (reescrever o script) foi descartada por risco e custo desnecessários.

### D2 — Pós-processamento em script Python standalone

O CSV bruto contém as colunas `server`, `transcoder`, `viewers`, `repeat_index` e todas as métricas necessárias para agregação. Um script Python (sem dependências além da stdlib) é suficiente e portável. Alternativa (AWK/bash) descartada por legibilidade e dificuldade de calcular desvio padrão.

### D3 — CSV agregado com colunas `_mean` e `_stddev`

O CSV de saída agregado segue o padrão: uma linha por combinação `(server, transcoder, viewers)` com colunas `<metrica>_mean` e `<metrica>_stddev`. Isso facilita exportação para planilha ou LaTeX sem processamento adicional.

### D4 — Documentar CPU elevado como achado, não como falha

CPU >80% ocorre porque o benchmark executa server RTMP, transcodificador, viewers simulados e o próprio orquestrador na mesma máquina. Isso deve ser documentado no relatório final como limitação do ambiente de testes, não como violação de critério. O critério "CPU ≤ 80%" do plano refere-se a ambiente de produção com máquina dedicada.

## Risks / Trade-offs

- **[Risco] Re-execução de 36 cenários a 300s cada ≈ 3h de runtime** → Mitigação: executar uma vez em ambiente estável, documentar run_id no relatório
- **[Risco] SRS+GStreamer pode continuar com alta taxa de erros** → Mitigação: é um resultado válido — documenta instabilidade da combinação; não é necessário corrigir para o TCC
- **[Trade-off] Latência ponta-a-ponta permanece manual** → Aceitável conforme Plano de Testes original; documentar como limitação

## Open Questions

- Especificação do hardware da máquina de testes (CPU, RAM) precisa ser confirmada e documentada no relatório — item "A definir" no Plano de Testes

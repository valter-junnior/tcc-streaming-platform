## Why

Os resultados atuais do benchmark foram gerados com duração de 30 segundos por cenário e apenas 1 repetição, enquanto o Plano de Testes do TCC exige mínimo de 5 minutos por cenário e 3 repetições para permitir análise estatística. Sem esses requisitos atendidos, os dados não são válidos como evidência científica para o TCC.

## What Changes

- Ajustar o benchmark para rodar com **duração de 300 segundos** (5 min) por cenário
- Ajustar para **3 repetições** por combinação (4 combinações × 3 viewers × 3 reps = 36 execuções)
- Gerar CSV consolidado com **agregação estatística** (média e desvio padrão por métrica)
- Gerar relatório final em markdown com tabelas prontas para uso no TCC
- Documentar o comportamento de CPU elevado (>80%) como achado metodológico, explicando que o benchmark executa server, transcoder e viewers na mesma máquina
- **Simplificar** o `run-rtmp-benchmark.sh`: remover a cópia de arquivos `.md` para `app/benchmark/` e redirecionar a saída final dos relatórios para `docs/benchmark/`

## Capabilities

### New Capabilities

- `benchmark-statistical-aggregation`: Geração de CSV e relatório com média/desvio-padrão por combinação de cenário a partir de múltiplas repetições
- `benchmark-tcc-final-report`: Relatório markdown final formatado para uso direto no TCC, cobrindo análise dos critérios de aceitação e comparação entre as 4 combinações

### Modified Capabilities

- `rtmp-performance-test-orchestration`: Adição de parâmetros de duração (300s) e repetições (3) ao comando de execução do benchmark full
- `rtmp-metrics-collection-and-csv-report`: Extensão do CSV de saída para incluir colunas de média e desvio padrão por métrica quando múltiplas repetições estão presentes

## Impact

- `app/benchmark/run-rtmp-benchmark.sh`: ajuste dos parâmetros `--duration` e `--repeat`; remoção das variáveis `FINAL_TECHNICAL_MD` e `FINAL_TCC_MD` apontando para `app/benchmark/`; saída final de `.md` redirecionada para `docs/benchmark/`
- `app/benchmark/`: novos scripts de pós-processamento para agregar repetições; remoção dos arquivos `resultado_tecnico.md` e `resultado_tcc.md` do diretório raiz
- `app/benchmark/results/`: novo conjunto de resultados gerado pela re-execução
- `docs/benchmark/`: diretório criado para receber os relatórios `.md` gerados pelo script
- `docs/tcc/`: tabelas de resultados finais prontas para inclusão no texto do TCC

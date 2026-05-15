## 1. Re-execução do Benchmark com Parâmetros Corretos

- [x] 1.1 Confirmar e documentar as especificações de hardware da máquina de testes (CPU, RAM, SO) no arquivo `docs/tcc/Matriz e Plano de Testes.md`
- [ ] 1.2 Executar o benchmark completo com `--mode full --duration 300 --repeats 3` e anotar o run_id gerado
- [ ] 1.3 Verificar que o CSV bruto contém 36 linhas de execução (4 combinações × 3 cargas × 3 repetições) com `status=PASS` ou `status=FAIL`

## 2. Script de Agregação Estatística

- [x] 2.1 Criar `app/benchmark/aggregate-results.py` que leia o CSV bruto e agrupe por `(server, transcoder, viewers)`
- [x] 2.2 Implementar cálculo de média e desvio padrão para as métricas: `startup_hls_seconds`, `cpu_avg_percent`, `cpu_max_percent`, `mem_avg_mb`, `mem_max_mb`, `bitrate_kbps`, `error_rate_percent`, `restarts_after`
- [x] 2.3 Escrever o CSV agregado `results-aggregated.csv` no diretório da execução com colunas `<metrica>_mean` e `<metrica>_stddev`
- [x] 2.4 Validar que o script funciona com `python3` sem dependências externas
- [ ] 2.5 Executar `aggregate-results.py` sobre o CSV da nova execução e verificar o output

## 3. Gerador de Relatório Final para TCC

- [x] 3.1 Criar `app/benchmark/generate-tcc-report.py` que leia `results-aggregated.csv` e gere `report-tcc-final.md`
- [x] 3.2 Implementar tabelas comparativas em markdown com mean ± stddev para cada métrica (Startup HLS, CPU médio, RAM média, Bitrate, Taxa de erros, Reinicializações)
- [x] 3.3 Implementar seção de avaliação dos critérios de aceitação: comparar cada valor médio contra o threshold do Plano de Testes e marcar PASS/FAIL
- [x] 3.4 Adicionar seção "Notas Metodológicas" com: explicação do CPU elevado (ambiente compartilhado), ausência de latência ponta-a-ponta automatizada, e run_id da execução
- [ ] 3.5 Executar `generate-tcc-report.py` e revisar o `report-tcc-final.md` gerado

## 4. Integração ao Script Principal

- [x] 4.1 Adicionar chamada opcional ao `aggregate-results.py` ao final do `run-rtmp-benchmark.sh` (modo `--mode full`) com flag `--aggregate` para acionamento explícito
- [x] 4.2 Atualizar o `README.md` do benchmark documentando os novos scripts e o fluxo de uso: benchmark → agregação → relatório

## 5. Simplificação do Script de Benchmark

- [x] 5.1 Remover as variáveis `FINAL_TECHNICAL_MD` e `FINAL_TCC_MD` que apontam para `${SCRIPT_DIR}/resultado_*.md` em `run-rtmp-benchmark.sh`
- [x] 5.2 Adicionar variável `DOCS_BENCHMARK_DIR` calculada como `${SCRIPT_DIR}/../../docs/benchmark` (ou caminho absoluto equivalente) no bloco de inicialização do script
- [x] 5.3 Substituir as linhas `cp "$TECHNICAL_REPORT_FILE" "$FINAL_TECHNICAL_MD"` e `cp "$TCC_REPORT_FILE" "$FINAL_TCC_MD"` por gravação direta em `${DOCS_BENCHMARK_DIR}/resultado_tecnico.md` e `${DOCS_BENCHMARK_DIR}/resultado_tcc.md` com `mkdir -p` antes
- [x] 5.4 Deletar os arquivos `app/benchmark/resultado_tecnico.md` e `app/benchmark/resultado_tcc.md` do repositório (foram substituídos por `docs/benchmark/`)
- [ ] 5.5 Verificar que após execução os relatórios aparecem em `docs/benchmark/` e não em `app/benchmark/`

## 6. Validação Final

- [ ] 6.1 Conferir que `results-aggregated.csv` tem exatamente 12 linhas (4 combinações × 3 cargas de viewers)
- [ ] 6.2 Conferir que `report-tcc-final.md` cobre todos os 12 cenários e todas as métricas requeridas pelo Plano de Testes
- [ ] 6.3 Copiar as tabelas do relatório para `docs/tcc/tcc.md` na seção de resultados

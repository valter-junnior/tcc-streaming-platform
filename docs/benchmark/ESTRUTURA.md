# Estrutura de Pastas e Versionamento do Benchmark

## Pasta Local de Resultados

```
app/benchmark/results/
├── bench-full-20260517-113843/          ← Run específica (RUN_ID)
│   ├── logs/                             ← Todos os logs desta run
│   │   ├── run-console.log               ← Saída completa do script (tudo que saiu na tela)
│   │   ├── scenario.log                  ← Logs dos containers (compõe, healthcheck, etc.)
│   │   ├── nginx_ffmpeg-v1-r1-try0_/    ← Pasta de um cenário específico
│   │   │   ├── scenario.log
│   │   │   ├── rtmp.log
│   │   │   └── backend.log
│   │   └── ...mais cenários...
│   ├── results.csv                       ← CSV bruto: dados de cada execução (fonte de verdade)
│   ├── results-aggregated.csv            ← CSV técnico: agregação com médias/desvios e pass/fail
│   ├── resultado.csv                     ← CSV final: formato de matriz T01..T12 (para TCC)
│   └── summary.txt                       ← Resumo rápido (passed/failed/totais)
└── latest -> bench-full-20260517-113843  ← Symlink para a run mais recente
```

## Pasta de Documentação (Versionamento)

```
docs/benchmark/
├── README.md                     ← Documentação de uso
├── resultado.csv                 ← Última cópia de resultado.csv (para TCC)
├── results-aggregated.csv        ← Última cópia de results-aggregated.csv (análise técnica)
├── results-raw.csv               ← Última cópia de results.csv bruto (auditoria)
├── run-console-latest.log        ← Último console.log da execução
└── results.csv                   ← Cópia do resultado.csv bruto (compatibilidade)
```

## Fluxo de Versionamento

1. **Durante a execução:**
   - Saída da tela é espelhada em tempo real para `<run_id>/logs/run-console.log`
   - Cenários geram logs em `<run_id>/logs/<scenario>_*/`
   - Todos os eventos aparecem no console com timestamp ISO

2. **Ao finalizar (sem --aggregate):**
   - Dados brutos salvos em `<run_id>/results.csv`
   - Symlink `latest` aponta para `<run_id>`
   - Cópia em `docs/benchmark/results.csv` para referência

3. **Ao finalizar (com --aggregate):**
   - Tudo do passo anterior, MAIS:
   - `results-aggregated.csv` gerado (agregação estatística)
   - `resultado.csv` gerado (matriz T01..T12 para TCC)
   - Todas as três versões copiadas para `docs/benchmark/`:
     - `resultado.csv` ← Use isto no TCC
     - `results-aggregated.csv` ← Use isto para análise comparativa
     - `results-raw.csv` ← Use isto para auditoria de dados brutos
     - `run-console-latest.log` ← Último log de execução

## Qual arquivo usar para quê?

| Arquivo | Localização | Propósito |
|---------|-------------|----------|
| `resultado.csv` | `docs/benchmark/` | **TCC**: matriz T01..T12 com legenda e critérios |
| `results-aggregated.csv` | `docs/benchmark/` | Análise: médias, desvios, pass/fail técnico |
| `results-raw.csv` | `docs/benchmark/` | Auditoria: todos os dados brutos (fonte de verdade) |
| `run-console-latest.log` | `docs/benchmark/` | Rastreamento: tudo que aconteceu na execução |
| `results.csv` | `<run_id>/` | Versão original de cada run (nunca deletar) |

## Estrutura da pasta `logs`

Guarda todos os artefatos de rastreamento de uma execução:

- `run-console.log` — Espelho da saída da tela (stdout + stderr)
- `scenario.log` — Log Docker Compose e healthcheck
- `<scenario_slug>/` — Subpasta por cenário testado
  - `scenario.log` — Detalhe de execução do cenário
  - `rtmp.log` — Saída dos containers RTMP (nginx/srs)
  - `backend.log` — Saída do backend Spring Boot
- Etc. (um por cenário)

## Resumo

- **Sempre ativo:** `docs/benchmark/` recebe cópias dos artefatos mais recentes (versionamento contínuo)
- **Pasta logs:** Centraliza todos os registros de execução e cenários
- **run-console.log:** Dentro de logs, captura tudo que saiu no terminal durante a run


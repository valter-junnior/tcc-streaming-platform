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
├── README.md                  ← Documentação de uso
├── resultado.csv              ← Última cópia da matriz final (para TCC)
├── results-aggregated.csv     ← Última cópia do agregado técnico
├── results.csv                ← Última cópia do bruto (auditoria)
└── run-console.log            ← Último espelho do console da execução
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
   - Cópias atualizadas em `docs/benchmark/`:
     - `resultado.csv` ← Use isto no TCC
     - `results-aggregated.csv` ← Use isto para análise comparativa
     - `results.csv` ← Use isto para auditoria dos dados brutos mais recentes
     - `run-console.log` ← Último espelho do console

## Qual arquivo usar para quê?

| Arquivo | Localização | Propósito |
|---------|-------------|----------|
| `resultado.csv` | `docs/benchmark/` | **TCC**: matriz T01..T12 com legenda e critérios |
| `results-aggregated.csv` | `docs/benchmark/` | Análise: médias, desvios, pass/fail técnico |
| `results.csv` | `docs/benchmark/` | Auditoria: bruto mais recente copiado do último run |
| `run-console.log` | `docs/benchmark/` | Rastreamento: espelho do console da última execução |
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


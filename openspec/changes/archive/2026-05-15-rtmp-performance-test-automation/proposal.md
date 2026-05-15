## Why

A fase de testes de desempenho RTMP ainda depende de execução manual e validações ad-hoc, o que impede comparações reprodutíveis entre NGINX RTMP e SRS com FFmpeg e GStreamer. Este trabalho é necessário agora para viabilizar medições padronizadas por cenário, com coleta estruturada em CSV para análise técnica.

## What Changes

- Adicionar infraestrutura de testes de desempenho executada exclusivamente via Docker, sem dependência da aplicação frontend e backend para medição.
- Implementar um caso mínimo de validação inicial para os quatro cenários: nginx+ffmpeg, nginx+gstreamer, srs+ffmpeg, srs+gstreamer.
- Implementar suíte completa com um único comando/script para executar todos os cenários definidos na matriz de testes.
- Coletar métricas de execução de live com audiência sintética por um tempo configurável e registrar resultados em uma matriz CSV consolidada.
- Incluir documentação técnica única e objetiva com fluxo de execução, pré-requisitos, interpretação de saída e próximos passos.

## Capabilities

### New Capabilities
- `rtmp-performance-test-orchestration`: Orquestra cenários de benchmark RTMP via Docker para NGINX e SRS com transcodificação FFmpeg/GStreamer.
- `rtmp-metrics-collection-and-csv-report`: Coleta métricas durante live+audiência e exporta matriz CSV consolidada por execução e cenário.
- `rtmp-benchmark-operational-guide`: Documenta de forma técnica a preparação, execução e continuidade da campanha de testes.

### Modified Capabilities
- Nenhuma.

## Impact

- Novos scripts e assets de teste no repositório, focados em orquestração de containers, geração de carga de visualização e coleta de métricas.
- Criação de artefatos de saída de benchmark (logs e CSV) para comparação entre cenários.
- Ajustes apenas em documentação e automação de testes, sem alterar comportamento funcional do frontend/backend existente.

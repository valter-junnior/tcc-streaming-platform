# Matriz complementar de 17–18 de novembro de 2026

Este conjunto contém 36 execuções completas: quatro combinações de servidor RTMP e transcodificador, três cargas (1, 10 e 50 viewers) e três repetições por tratamento. O arquivo [`results.csv`](results.csv) preserva o `run_id` da sessão de cada linha. [`results-aggregated.csv`](results-aggregated.csv) contém média, desvio padrão amostral e classificação pelos limites exploratórios do projeto.

A sessão `bench-full-20261117-170322` produziu 30 linhas completas. O lançador foi interrompido depois de iniciar a 31ª execução, sem gravar uma linha para ela. A sessão `bench-full-20261118-080637` produziu as seis linhas faltantes: SRS + GStreamer com 10 e 50 viewers. As duas sessões usaram o mesmo hash SHA-256 do roteiro, `ea32f04cf344fcc70187a0f78651110c8b74d3f879014756ae4ace4e9b3d84eb`, mas ocorreram em dias diferentes. Esta matriz é uma consolidação analítica, não uma sessão contínua.

O roteiro desta coleta manteve o publicador ativo até depois da janela dos viewers e verificou HTTP 200 para as quatro playlists de variante e um segmento de cada uma. As métricas de CPU e memória representam o contêiner RTMP/transcodificador durante a janela dos viewers. `startup_hls_seconds` e `tempo_primeiro_segmento_s` contam ciclos malsucedidos de sondagem; não são latência de reprodução nem tempo de parede. `error_rate_percent` considera as requisições dos viewers. O valor `000` registrado nos logs locais significa ausência de resposta HTTP; o CSV público registra a quantidade de erros, sem os arquivos operacionais por requisição.

O auditor local validou as 36 combinações únicas, os resumos e logs individuais dos viewers, as quatro variantes, os tempos e o cálculo das médias de CPU e memória. Todas as linhas tiveram `status=PASS` operacional, que indica coleta completa. Na agregação, todos os tratamentos ultrapassaram o limite exploratório local de CPU; SRS + GStreamer com 50 viewers ultrapassou também o limite de erros, com média simples de 8,04% nas três repetições. Esses limites vieram de testes exploratórios e não constituem requisitos de implantação.

O host não era dedicado. Além disso, o roteiro reconstruía a imagem RTMP a cada repetição, gerando identificadores distintos, embora as versões dos binários consultados permanecessem iguais. As configurações de bitrate e alguns detalhes dos pipelines FFmpeg e GStreamer não foram igualados. Essas condições limitam comparações causais e a generalização dos resultados. A partir da coleta diagnóstica posterior, o roteiro foi alterado para construir uma imagem por sessão e registrar o código de saída e o tempo do `curl`; essas mudanças não foram usadas nesta matriz.

Para reagregar os dados públicos a partir da raiz do repositório:

```bash
python3 app/benchmark/aggregate-results.py docs/benchmark/runs/bench-combined-20261117-18/results.csv /tmp/tcc-reagregado-novembro
```

Os dois CSVs desta pasta estão sob [CC BY 4.0](../../LICENSE-DATA.md). O código do benchmark está em [`app/benchmark/`](../../../../app/benchmark/). O texto da monografia e os logs operacionais não fazem parte deste conjunto público.

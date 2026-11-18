# Dados públicos e reprodução do benchmark

## Conjunto publicado

- Repositório: https://github.com/valter-junnior/tcc-streaming-platform
- Execução analisada: `bench-full-20260517-164317` (17 de maio de 2026).
- [`results.csv`](results.csv): 36 repetições, com apenas as colunas necessárias à auditoria estatística.
- [`results-aggregated.csv`](results-aggregated.csv): 12 tratamentos agregados.
- [`resultado.csv`](resultado.csv): matriz resumida.
- [`LICENSE-DATA.md`](LICENSE-DATA.md): licença CC BY 4.0 dos três CSVs.
- [`runs/bench-combined-20261117-18/`](runs/bench-combined-20261117-18/): matriz complementar de novembro, com 36 linhas provenientes de duas sessões, CSV bruto sanitizado, agregação e notas de proveniência.
- Código: [`run-rtmp-benchmark.sh`](../../app/benchmark/run-rtmp-benchmark.sh), [`export-public-results.py`](../../app/benchmark/export-public-results.py) e [`aggregate-results.py`](../../app/benchmark/aggregate-results.py).

O CSV público não contém URLs internas, chaves de transmissão, nomes de contêiner, caminhos absolutos nem mensagens livres de erro. O resultado original detalhado e os logs permanecem na pasta local `app/benchmark/results/<run_id>/`, excluída do Git. A ausência dessas amostras e logs limita a investigação posterior de falhas por requisição.

Na execução de maio, a primeira repetição de SRS + GStreamer com 50 viewers recebeu `PASS` apesar de `tempo_primeiro_segmento_s=-1`: foram 3.688 erros em 38.986 requisições e uma janela total de 472 s. As repetições 2 e 3 localizaram o segmento, duraram 320 s e não registraram erros de viewers. A média agregada de 3,15% mistura essas condições. O roteiro daquela coleta iniciava o publicador de 300 s antes das sondagens e só depois iniciava os viewers por outros 300 s; também mascarava o código de saída do publicador. O log local do transcodificador mostra dez tentativas com erro de diretório inexistente ao gravar segmentos, seguidas de uma nova inicialização bem-sucedida. A limpeza horária então vigente removia diretórios HLS vazios sem verificar sua idade, o que pode explicar a falha na virada da hora; não há log histórico da limpeza para comprovar esse elo. O CSV histórico não demonstra degradação causada pelos 50 viewers.

## Reagregação dos dados publicados

A partir da raiz do repositório:

```bash
python3 app/benchmark/aggregate-results.py docs/benchmark/results.csv /tmp/tcc-reagregado
```

A rotina usa apenas a biblioteca padrão do Python. A agregação do CSV sanitizado foi comparada aos 12 tratamentos publicados e produziu os mesmos valores. Os dados permitem auditar médias, desvios padrão e critérios operacionais.

## Nova execução

Use Docker Engine com Compose, Python 3, portas locais livres e capacidade de construir as imagens. Configure `app/.env` a partir de `app/.env.example`, substituindo as senhas de exemplo e sem publicar o arquivo. Execute:

```bash
cd app
cp .env.example .env
./benchmark/run-rtmp-benchmark.sh --mode full --duration 300 --viewers-list 1,10,50 --repeats 3 --aggregate
```

O script combina dois servidores RTMP, dois transcodificadores e três cargas, com três repetições por tratamento. Uma nova matriz dura várias horas. O resultado bruto e os logs ficam em `app/benchmark/results/<run_id>/`; a opção `--publish` grava uma matriz completa e auditada em `docs/benchmark/runs/<run_id>/`, preservando os CSVs históricos desta pasta. Uma matriz obtida em sessões separadas deve manter os identificadores de origem e um manifesto de proveniência; o comando `--publish` não publica automaticamente esse tipo de consolidação.

O roteiro atual mantém a fonte ativa até o fim da janela dos viewers, reprova o timeout do primeiro segmento e grava `timing.csv` em cada pasta de repetição, separando sondagens, verificação das quatro variantes HLS e carga. O arquivo `hls-variants.csv` e as playlists locais mostram se cada variante e um segmento responderam HTTP 200. Cada viewer grava `viewer-N-requests.csv` com o timestamp do início do ciclo, tipo de recurso, código HTTP, código de saída e tempo do `curl`; o registro também sinaliza conteúdo inválido de playlist. `environment.txt`, os snapshots do host e `host-vmstat.log` documentam versões e atividade concorrente. A imagem RTMP é construída uma vez por sessão e seu identificador é registrado em cada repetição. As médias de CPU e memória de uma nova execução usam apenas amostras colhidas durante a janela dos viewers; por isso, não devem substituir diretamente as médias históricas, que incluíam preparação e sondagens.

Após a execução, confira a integridade dos arquivos locais com:

```bash
python3 app/benchmark/audit-results.py app/benchmark/results/<run_id> --require-full
```

O auditor compara os 36 registros esperados com os arquivos de timing, viewers, requisições, variantes e amostras de CPU/memória. A opção `--publish` deve ser usada somente depois da revisão dos resultados; ela exige a matriz completa. Os logs detalhados contêm informações operacionais e permanecem fora do conjunto público.

## Versões e limites

No código consultado, o Dockerfile de Nginx fixa Nginx 1.24.0 e nginx-rtmp-module v1.2.2. O Dockerfile de SRS usa `ossrs/srs:4`. A fonte sintética usa `jrottenberg/ffmpeg:6.0-alpine`; os viewers usam `curlimages/curl:8.7.1`. O Compose declara PostgreSQL 16 Alpine, RabbitMQ 3 management Alpine, Maven 3.9 com Java 21 e Node 20 Alpine. FFmpeg e GStreamer instalados dentro dos contêineres RTMP vêm de `apt` sem versão fixada. Os logs locais de maio preservam os digests das imagens-base e dos manifests finais das imagens RTMP, mas não as versões efetivas dos binários internos nem do Docker Engine/Compose naquela data. As imagens finais antigas já não estão disponíveis no Docker local. Uma nova execução registra `docker version`, `docker compose version`, os identificadores das imagens e as versões dos binários após a construção.

Os dados publicados não incluem todas as amostras temporais nem os arquivos de cada viewer. Também não há captura de vídeo nem timestamps de reprodução. É possível auditar a agregação e repetir o método, mas não reconstruir bit a bit a execução histórica ou medir latência percebida pelo usuário.

CPU e memória são do contêiner RTMP/transcodificador, não do host inteiro. `bitrate_kbps` é uma estimativa da primeira variante HLS. `latencia_ponta_a_ponta_s` copia `tempo_primeiro_segmento_s` e não representa latência ponta a ponta. Consulte a metodologia da monografia para fórmulas e procedimento.

## Integridade dos arquivos

Os hashes SHA-256 dos três CSVs publicados são registrados abaixo.

```text
6e23ed981da3bafdad60816ba9900b06103cfe50b3a9d4b6ed69b043cf8bf939  results.csv
1219317a215747938cdcf76ea6f05a68b2fbe4d3cc625be2e74ce12af97cb920  results-aggregated.csv
3bf883cb960eee92c468e2613fb3c9fbf5e3ac831991dd9994ac7c217799e42a  resultado.csv
```

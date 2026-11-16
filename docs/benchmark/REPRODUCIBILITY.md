# Dados públicos e reprodução do benchmark

## Conjunto publicado

- Repositório público: https://github.com/valter-junnior/tcc-streaming-platform
- Execução analisada: `bench-full-20260517-164317` (17 de maio de 2026).
- Dados por repetição (36 linhas): [`results.csv`](results.csv). Este é o arquivo usado como fonte da análise.
- Dados agregados por tratamento (12 linhas): [`results-aggregated.csv`](results-aggregated.csv).
- Matriz resumida: [`resultado.csv`](resultado.csv).
- Registro do console: [`run-console.log`](run-console.log).
- Código do experimento: [`../../app/benchmark/run-rtmp-benchmark.sh`](../../app/benchmark/run-rtmp-benchmark.sh) e [`../../app/benchmark/aggregate-results.py`](../../app/benchmark/aggregate-results.py).

O repositório é público e os arquivos podem ser baixados sem autenticação. Não há licença explícita de reutilização no repositório; é necessário definir uma antes de chamar o conjunto de dados de aberto no sentido jurídico.

## Reagregação dos dados existentes

A partir da raiz do repositório:

```bash
python3 app/benchmark/aggregate-results.py docs/benchmark/results.csv /tmp/tcc-reagregado
```

Compare `/tmp/tcc-reagregado/results-aggregated.csv` com o arquivo publicado. A rotina usa apenas a biblioteca padrão do Python. A comparação deve considerar que arquivos gerados podem ter caminhos e metadados diferentes conforme o diretório de saída.

## Nova execução

É necessário Docker Engine com Compose, Python 3, portas locais livres e capacidade de construir as imagens. Configure `app/.env` a partir de `app/.env.example`, sem publicar credenciais. Execute:

```bash
cd app
cp .env.example .env
./benchmark/run-rtmp-benchmark.sh --mode full --duration 300 --viewers-list 1,10,50 --repeats 3 --aggregate
```

O script cria quatro combinações de servidor RTMP e transcodificador, aplica três cargas e executa três repetições por tratamento. Uma nova matriz dura várias horas. O comando `--aggregate` grava cópias em `docs/benchmark/`; preserve os arquivos originais antes de sobrescrevê-los. Registre o commit e o estado do host junto ao novo run.

## Versões e limites

No código consultado, o Dockerfile de Nginx fixa Nginx 1.24.0 e nginx-rtmp-module v1.2.2. O Dockerfile de SRS usa `ossrs/srs:4`; um relatório mais antigo menciona SRS 6 e está desatualizado em relação ao Dockerfile. A fonte sintética usa `jrottenberg/ffmpeg:6.0-alpine`; os viewers usam `curlimages/curl:8.7.1`. O Compose declara PostgreSQL 16 Alpine, RabbitMQ 3 management Alpine, Maven 3.9 com Java 21 e Node 20 Alpine. FFmpeg e GStreamer instalados dentro dos contêineres RTMP vêm de `apt` sem versão fixada. A versão efetiva do Docker Engine/Compose e os digests das imagens da execução de maio não foram preservados. Para uma réplica futura, grave `docker version`, `docker compose version`, `docker image inspect` e as versões dos binários após a construção.

O CSV publicado não inclui todas as amostras temporais, logs e arquivos de cada viewer: os caminhos absolutos registrados nele apontam ao computador da execução original. Também não há captura de vídeo nem timestamps de reprodução. Os dados permitem auditar a agregação e repetir o método, mas não reconstruir bit a bit a execução histórica ou medir latência percebida pelo usuário.

CPU e memória são do contêiner RTMP/transcodificador, não do host inteiro. `bitrate_kbps` é uma estimativa da primeira variante HLS. `latencia_ponta_a_ponta_s` copia `tempo_primeiro_segmento_s` e não representa latência ponta a ponta. Consulte o capítulo de metodologia para fórmulas e procedimento.

## Integridade dos arquivos da execução analisada

Hashes SHA-256 dos arquivos tabulares e do log público:

```text
dbe4b5db685bd3cbcaf52a19de46793ef5a7714494277cdbc862e237b0225929  results.csv
1219317a215747938cdcf76ea6f05a68b2fbe4d3cc625be2e74ce12af97cb920  results-aggregated.csv
3bf883cb960eee92c468e2613fb3c9fbf5e3ac831991dd9994ac7c217799e42a  resultado.csv
80a43a53c187a470d981cd37b2e0cd9f2da0918abe6374d6d7b3ba3024269165  run-console.log
```

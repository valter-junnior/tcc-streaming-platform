# Dados públicos e reprodução do benchmark

## Conjunto publicado

- Repositório: https://github.com/valter-junnior/tcc-streaming-platform
- Execução analisada: `bench-full-20260517-164317` (17 de maio de 2026).
- [`results.csv`](results.csv): 36 repetições, com apenas as colunas necessárias à auditoria estatística.
- [`results-aggregated.csv`](results-aggregated.csv): 12 tratamentos agregados.
- [`resultado.csv`](resultado.csv): matriz resumida.
- [`LICENSE-DATA.md`](LICENSE-DATA.md): licença CC BY 4.0 dos três CSVs.
- Código: [`run-rtmp-benchmark.sh`](../../app/benchmark/run-rtmp-benchmark.sh), [`export-public-results.py`](../../app/benchmark/export-public-results.py) e [`aggregate-results.py`](../../app/benchmark/aggregate-results.py).

O CSV público não contém URLs internas, chaves de transmissão, nomes de contêiner, caminhos absolutos nem mensagens livres de erro. O resultado original detalhado e os logs permanecem na pasta local `app/benchmark/results/<run_id>/`, excluída do Git. A ausência dessas amostras e logs limita a investigação posterior de falhas por requisição.

Na execução de maio, a primeira repetição de SRS + GStreamer com 50 viewers recebeu `PASS` apesar de `tempo_primeiro_segmento_s=-1`: foram 3.688 erros em 38.986 requisições e uma janela total de 472 s. As repetições 2 e 3 localizaram o segmento, duraram 320 s e não registraram erros de viewers. A média agregada de 3,15% mistura essas condições. O roteiro daquela coleta iniciava o publicador de 300 s antes das sondagens e só depois iniciava os viewers por outros 300 s; também mascarava o código de saída do publicador. Portanto, o CSV histórico não demonstra que a publicação permaneceu ativa durante toda a carga, nem que a combinação tecnológica degrada especificamente com 50 viewers.

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

O script combina dois servidores RTMP, dois transcodificadores e três cargas, com três repetições por tratamento. Uma nova matriz dura várias horas. O resultado bruto e os logs ficam em `app/benchmark/results/<run_id>/`; a opção `--publish` é separada para evitar que uma execução local sobrescreva os CSVs públicos automaticamente. Se decidir publicar uma nova rodada, revise os resultados e use a mesma linha de comando com `--publish`.

O roteiro atual mantém a fonte ativa até o fim da janela dos viewers, reprova o timeout do primeiro segmento e grava `timing.csv` em cada pasta de repetição, separando os intervalos de sondagem e de carga. Cada viewer também grava um arquivo `viewer-N-requests.csv` com timestamp, tipo de recurso e código HTTP. As médias de CPU e memória de uma nova execução usam apenas amostras colhidas durante a janela dos viewers; por isso, não devem substituir diretamente as médias históricas, que incluíam preparação e sondagens.

## Versões e limites

No código consultado, o Dockerfile de Nginx fixa Nginx 1.24.0 e nginx-rtmp-module v1.2.2. O Dockerfile de SRS usa `ossrs/srs:4`. A fonte sintética usa `jrottenberg/ffmpeg:6.0-alpine`; os viewers usam `curlimages/curl:8.7.1`. O Compose declara PostgreSQL 16 Alpine, RabbitMQ 3 management Alpine, Maven 3.9 com Java 21 e Node 20 Alpine. FFmpeg e GStreamer instalados dentro dos contêineres RTMP vêm de `apt` sem versão fixada. A versão efetiva do Docker Engine/Compose e os digests das imagens da execução de maio não foram preservados. Para uma réplica futura, grave `docker version`, `docker compose version`, `docker image inspect` e as versões dos binários após a construção.

Os dados publicados não incluem todas as amostras temporais nem os arquivos de cada viewer. Também não há captura de vídeo nem timestamps de reprodução. É possível auditar a agregação e repetir o método, mas não reconstruir bit a bit a execução histórica ou medir latência percebida pelo usuário.

CPU e memória são do contêiner RTMP/transcodificador, não do host inteiro. `bitrate_kbps` é uma estimativa da primeira variante HLS. `latencia_ponta_a_ponta_s` copia `tempo_primeiro_segmento_s` e não representa latência ponta a ponta. Consulte a metodologia da monografia para fórmulas e procedimento.

## Integridade dos arquivos

Os hashes SHA-256 dos três CSVs publicados são registrados abaixo.

```text
6e23ed981da3bafdad60816ba9900b06103cfe50b3a9d4b6ed69b043cf8bf939  results.csv
1219317a215747938cdcf76ea6f05a68b2fbe4d3cc625be2e74ce12af97cb920  results-aggregated.csv
3bf883cb960eee92c468e2613fb3c9fbf5e3ac831991dd9994ac7c217799e42a  resultado.csv
```

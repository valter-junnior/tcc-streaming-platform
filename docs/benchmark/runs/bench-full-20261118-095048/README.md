# Execução analisada no TCC

`bench-full-20261118-095048` foi executada em 18 de novembro de 2026, das 09:50 às 13:51 (America/Fortaleza). A sessão contém 36 execuções: Nginx-RTMP ou SRS, FFmpeg ou GStreamer, 1, 10 ou 50 viewers sintéticos, com três repetições por tratamento e 300 segundos nominais para cada viewer. O roteiro terminou com `PASS=36 FAIL=0`; a auditoria local `audit-results.py --require-full` não apontou inconsistências. Os 12 tratamentos estão em [`results-aggregated.csv`](results-aggregated.csv), as 36 linhas sanitizadas em [`results.csv`](results.csv), e a matriz resumida em [`resultado.csv`](resultado.csv).

A revisão do código executada foi `7ac4e7b128b42ce696cec927bebabf8e2eb9c29b`; o SHA-256 de `app/benchmark/run-rtmp-benchmark.sh` foi `cca8297820faf8b24c4e40d473a30e1af28c6f2ffe15c6107054b8308083854a`. Cada imagem RTMP foi construída uma vez na sessão e reutilizada nas repetições dos transcodificadores associados ao servidor. Os identificadores locais foram `sha256:27fde47752a1ca3be27df505b32eccea41f337844eb80aa17c953ce82c5301ce` (Nginx) e `sha256:056c8bc10f1280285eb758950d5b751a75ac6a978d3febb8ae9bedada71b68c4` (SRS). O ambiente usou Docker Client 29.6.1, Engine 28.5.1 e Compose 2.40.3-desktop.1. O host não era dedicado ao benchmark.

O `status=PASS` valida a integridade operacional de cada repetição: playlist mestre, primeiro segmento e quatro variantes HLS disponíveis, publicador ativo durante a carga, arquivos de viewer completos, amostras suficientes e bitrate calculável. Ele não significa aprovação dos limiares exploratórios de CPU ou erros. Em 546.352 requisições dos viewers houve um erro, na terceira repetição de SRS + GStreamer com 50 viewers: a playlist mestre não retornou resposta HTTP (`curl` código de saída 28, timeout de conexão após 2,039 s). As outras 546.351 requisições tiveram HTTP 200. A taxa média simples desse tratamento foi 0,0012%; os demais tratamentos registraram 0%. Não houve reinícios dos contêineres RTMP.

CPU e memória pertencem ao contêiner RTMP/transcodificador e são calculadas somente com amostras durante a janela dos viewers. `startup_hls_seconds` e `tempo_primeiro_segmento_s` são nomes legados de colunas: seus valores são **contagens de ciclos malsucedidos**, não segundos cronometrados. `latencia_ponta_a_ponta_s` copia a sondagem do primeiro segmento e não mede a reprodução no player. `bitrate_kbps` estima uma janela da primeira variante HLS; os pipelines FFmpeg e GStreamer possuem configurações de saída diferentes, de modo que consumo de recursos não demonstra eficiência para qualidade equivalente.

Para reagregar os CSVs públicos a partir da raiz do repositório:

```bash
python3 app/benchmark/aggregate-results.py docs/benchmark/runs/bench-full-20261118-095048/results.csv /tmp/tcc-reagregado
```

Os três CSVs estão sob [CC BY 4.0](../../LICENSE-DATA.md). O [guia de reprodução](../../REPRODUCIBILITY.md) descreve o procedimento e suas limitações. Logs operacionais detalhados, amostras temporais, URLs internas e chaves de transmissão ficam fora do conjunto público; os CSVs permitem conferir a agregação, mas não auditar cada requisição individualmente.

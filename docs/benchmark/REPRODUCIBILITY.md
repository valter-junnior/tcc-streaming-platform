# Dados públicos e reprodução do benchmark

## Execução analisada

A sessão `bench-full-20261118-095048`, realizada em 18 de novembro de 2026, é a fonte das tabelas quantitativas do TCC. Os [dados da sessão](runs/bench-full-20261118-095048/) incluem 36 linhas sanitizadas por repetição, 12 tratamentos agregados, matriz resumida e notas de proveniência. Os três CSVs estão sob [CC BY 4.0](LICENSE-DATA.md). A pasta mantém seu próprio identificador, para que uma nova execução não substitua os dados citados no documento.

A execução usou a revisão `7ac4e7b128b42ce696cec927bebabf8e2eb9c29b` e o SHA-256 `cca8297820faf8b24c4e40d473a30e1af28c6f2ffe15c6107054b8308083854a` do roteiro. O auditor local confirmou 36 combinações únicas, arquivos individuais dos viewers, quatro variantes, timestamps, integridade das amostras de CPU/memória e consistência dos resumos; não apontou problemas. A sessão registrou `PASS=36 FAIL=0`. Os critérios exploratórios da agregação são separados do status operacional.

As versões consultadas foram Docker Client 29.6.1, Engine 28.5.1, Compose 2.40.3-desktop.1, Nginx 1.24.0 com módulo RTMP v1.2.2 declarado, SRS 4.0.271, FFmpeg 4.4.2 e GStreamer 1.20.3 no contêiner Nginx, FFmpeg 4.2.7 e GStreamer 1.16.3 no contêiner SRS. O publicador usa a imagem declarada `jrottenberg/ffmpeg:6.0-alpine` e cada viewer usa `curlimages/curl:8.7.1`. Os identificadores das imagens RTMP construídas estão na nota da sessão. As tags de imagens-base e pacotes `apt` não são fixados por digest, de modo que uma reconstrução futura bit a bit não é garantida.

## Métricas e limites de interpretação

CPU e memória vêm de `docker stats` para o contêiner RTMP/transcodificador, amostrado aproximadamente a cada dois segundos; as médias por repetição usam somente a janela dos viewers. O percentual de CPU pode ultrapassar 100% porque representa o uso de mais de um núcleo lógico. A memória rotulada `mb` foi convertida pela convenção binária do coletor. A agregação calcula média e desvio padrão amostral das três repetições de cada tratamento.

O arquivo local também contém campos de tráfego de rede, mas o coletor soma snapshots de contadores acumulados do Docker. Essa operação não produz um volume transferido válido; por isso, os campos de rede foram excluídos do CSV público e da análise do TCC.

`startup_hls_seconds` e `tempo_primeiro_segmento_s` são nomes legados: os valores são contagens de tentativas malsucedidas antes da resposta HTTP 200 da playlist mestre e da disponibilidade de um segmento, respectivamente. Não são segundos de tempo de parede. `latencia_ponta_a_ponta_s` copia a segunda sondagem e não mede o primeiro quadro exibido. `bitrate_kbps` estima a primeira variante HLS a partir dos tamanhos e durações dos segmentos listados no fim da execução. `error_rate_percent` em cada linha é a taxa de erros nas requisições dos viewers sintéticos; o CSV agregado calcula a média simples das três taxas. Se uma playlist falha, as requisições dependentes desse ciclo não ocorrem.

Os critérios locais de aceitação são startup de até 24 ciclos, CPU média de até 240% com 1 ou 10 viewers e 280% com 50, taxa de erros de até 1% ou 3% nessas cargas, respectivamente, e nenhum reinício. Foram escolhidos durante testes exploratórios, sem derivação de uma norma externa ou requisito de implantação. `PASS` individual significa que o roteiro conseguiu coletar as medições necessárias, não que os limites tenham sido atendidos.

A fonte é sintética, a rede é local, a codificação usa CPU e o host não estava dedicado. Os pipelines FFmpeg e GStreamer têm alvos de bitrate e outros parâmetros diferentes. Não houve medição de qualidade visual, buffering ou primeiro quadro no player. Portanto, a comparação caracteriza estas configurações no ambiente descrito e não determina uma escolha universal nem custo por espectador.

## Reagregação e nova execução

Da raiz do repositório, para reproduzir as médias e os desvios padrão dos CSVs públicos:

```bash
python3 app/benchmark/aggregate-results.py docs/benchmark/runs/bench-full-20261118-095048/results.csv /tmp/tcc-reagregado
```

Para executar uma nova sessão, use Docker Engine com Compose, Python 3, portas locais livres e recursos suficientes para construir as imagens:

```bash
cd app
cp .env.example .env
# Configure senhas próprias em .env; não publique esse arquivo.
./benchmark/run-rtmp-benchmark.sh --mode full --duration 300 --viewers-list 1,10,50 --repeats 3 --aggregate
RUN_ID=bench-full-AAAAMMDD-HHMMSS
python3 benchmark/audit-results.py "benchmark/results/$RUN_ID" --require-full
```

O roteiro constrói cada imagem RTMP uma vez por sessão, recria a stack para cada tratamento, inicia a fonte sintética, sonda a playlist mestre e o primeiro segmento, verifica as quatro variantes e mantém a publicação ativa durante a janela de cada viewer. Cada viewer grava localmente timestamp, tipo de recurso, código HTTP, código de saída e tempo de requisição; `timing.csv` separa sondagens e carga. Os logs e amostras detalhadas ficam em `app/benchmark/results/<run_id>/`, fora do Git. O arquivo `.env` também fica fora do Git.

Após revisar uma matriz completa e validá-la, execute uma nova sessão com `--publish` além de `--aggregate` para exportar apenas os campos permitidos em `docs/benchmark/runs/<run_id>/`. O CSV público não inclui chaves de transmissão, URLs internas, caminhos absolutos, nomes de contêiner ou logs individuais. Ele permite auditar a agregação, mas não inspecionar cada requisição de uma execução já concluída.

As coletas anteriores permanecem arquivadas em `docs/benchmark/` e `docs/benchmark/runs/`; elas não compõem a sessão analisada no TCC.

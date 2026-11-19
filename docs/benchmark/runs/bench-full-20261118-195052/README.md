# Dados da sessão analisada no TCC

A sessão `bench-full-20261118-195052` ocorreu em 18 de novembro de 2026, das 19:50 às 23:52 (America/Fortaleza). Contém 36 execuções: Nginx-RTMP ou SRS, FFmpeg ou GStreamer, 1, 10 ou 50 viewers sintéticos, três repetições por tratamento e uma janela comum de 300 segundos. O roteiro registrou `PASS=36 FAIL=0`; `audit-results.py --require-full` retornou `valid=true` sem achados. Os dados estão em [results.csv](results.csv) (36 repetições), [results-aggregated.csv](results-aggregated.csv) (12 tratamentos) e [resultado.csv](resultado.csv) (matriz resumida).

A revisão executada foi `d648b07489137fa873f034e86c7c8ec0647d86cd`. O SHA-256 de `app/benchmark/run-rtmp-benchmark.sh` foi `d3ba332ea3d4afbd964badf69ece9a9a8593feea9b31da3093de96677d8545e6`. A imagem Nginx utilizada foi `sha256:d616777a37b6872e1c9eb78fbb319e46d8d12d20971e6effb158022d18a007c9`; a imagem SRS utilizada foi `sha256:7dfabfaf4e4a94665e585daa3401028fb663018a3e547056cdef1bf48937ac55`. O comando de build de cada imagem RTMP foi executado uma vez na sessão; o cache do Docker reutilizou imagens já existentes, identificadas pelos digests acima, em todas as repetições do mesmo servidor. Docker Client 29.6.1, Engine 28.5.1 e Compose 2.40.3-desktop.1 foram registrados no início. O host não era dedicado.

Todos os viewers de uma repetição aguardaram uma barreira de início e requisitaram playlist mestre, playlist de variante e segmento em ciclos programados de dois segundos. `viewers_start_ts` e `viewers_end_ts` delimitam a janela de 300 segundos usada tanto para CPU e memória quanto para `request_rate_per_second = request_total / viewers_window_seconds`. Nas repetições com 50 viewers, a taxa ficou entre 74,39 e 75,00 requisições/s. Os 328.932 pedidos da sessão terminaram sem erro registrado pelos clientes, e não houve reinícios dos contêineres RTMP. `PASS` significa coleta operacional íntegra; não é aprovação dos limites exploratórios de CPU ou uma medida de qualidade percebida.

CPU e memória pertencem ao contêiner RTMP/transcodificador, não ao host inteiro. `startup_hls_seconds` e `tempo_primeiro_segmento_s` são nomes legados: seus valores contam ciclos malsucedidos de sondagem, não segundos. `latencia_ponta_a_ponta_s` copia a segunda sondagem e não mede reprodução no player. `bitrate_kbps` estima uma janela da primeira variante HLS. Os pipelines FFmpeg e GStreamer têm parâmetros de saída diferentes; a comparação não demonstra custo para qualidade visual equivalente. As colunas locais de rede foram excluídas porque o coletor não calcula volume transferido válido.

Para reagregar os CSVs públicos a partir da raiz do repositório:

```bash
python3 app/benchmark/aggregate-results.py docs/benchmark/runs/bench-full-20261118-195052/results.csv /tmp/tcc-reagregado
```

Os três CSVs estão sob [CC BY 4.0](../../LICENSE-DATA.md). O [guia de reprodução](../../REPRODUCIBILITY.md) descreve o procedimento e os limites. Logs de serviço e arquivos individuais dos viewers permanecem locais; os CSVs públicos permitem reproduzir as médias e verificar a janela e a taxa de requisições, mas não auditar cada pedido isoladamente.

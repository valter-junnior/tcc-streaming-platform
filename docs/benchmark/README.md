# Benchmark RTMP

O experimento compara Nginx-RTMP e SRS, cada um com FFmpeg e GStreamer, sob 1, 10 e 50 espectadores sintéticos. Há três repetições por tratamento na execução publicada. O [guia de reprodução](REPRODUCIBILITY.md) identifica os dados abertos, as métricas, o ambiente, os limites das conclusões e o procedimento de auditoria.

## Executar localmente

```bash
cd app
cp .env.example .env
# Edite as senhas de .env antes de iniciar a stack.
./benchmark/run-rtmp-benchmark.sh --mode full --duration 300 --viewers-list 1,10,50 --repeats 3 --aggregate
```

O script exige Docker Engine e Compose, além das portas configuradas em `.env` disponíveis. A execução completa demora várias horas. Os arquivos detalhados ficam em `app/benchmark/results/<run_id>/` e não entram no Git. O comando `--aggregate` gera `results-aggregated.csv` e `resultado.csv` nessa pasta local.

Para publicar uma **nova** rodada, revise os arquivos locais e execute com `--publish` além de `--aggregate`. Essa opção exporta somente colunas de medidas explicitamente permitidas para `docs/benchmark/results.csv` e copia os dois CSVs agregados. Não publica logs, caminhos absolutos, URLs internas nem chaves de transmissão. A publicação é uma atualização deliberada dos dados do repositório; confira o diff antes de commitar.

```bash
./benchmark/run-rtmp-benchmark.sh --mode full --duration 300 --viewers-list 1,10,50 --repeats 3 --aggregate --publish
```

## Arquivos públicos

| Arquivo | Papel |
|---|---|
| `results.csv` | Medidas por repetição, com campos locais e chaves removidos |
| `results-aggregated.csv` | Médias, desvios padrão e critérios por tratamento |
| `resultado.csv` | Resumo da matriz para apresentação |
| `LICENSE-DATA.md` | Licença CC BY 4.0 para os três CSVs |
| `REPRODUCIBILITY.md` | Versões, comandos e limitações de reprodução |

CPU e memória referem-se ao contêiner RTMP/transcodificador, não ao host inteiro. `bitrate_kbps` estima a primeira variante HLS. `latencia_ponta_a_ponta_s` é uma cópia da sondagem do primeiro segmento e não mede latência de reprodução. A taxa de erros agrega requisições HTTP dos viewers sintéticos.

# TCC — Plataforma de Streaming

## 1. Matriz de Comparação

![Matriz de Comparação](diagrama-comparacao.png)

As quatro combinações são o objeto central do estudo comparativo. As métricas coletadas permitirão posicionar cada combinação quanto a latência, uso de recursos e qualidade de entrega.

---

## 2. Plano de Testes

### Título

Análise Comparativa de Desempenho de Servidores RTMP e Transcodificadores em Plataforma de Streaming ao Vivo

### Objetivo

Medir e comparar, nas quatro combinações (Nginx/SRS × FFmpeg/GStreamer), as métricas de latência ponta-a-ponta, uso de CPU, uso de memória, bitrate efetivo de saída e estabilidade do pipeline de transcodificação, sob diferentes cargas de espectadores simultâneos.

### Ambiente de Testes

| Item | Especificação |
|---|---|
| Sistema Operacional | Ubuntu 24.04.3 LTS (x86\_64), kernel 6.17.0-23-generic |
| Arquitetura | x86\_64 |
| CPU | Intel Core i5-13420H (13ª geração, 12 threads, 2 threads/núcleo) |
| RAM | 24 GB |
| Armazenamento | NVMe SSD (231 GB) |
| Rede | Loopback / bridge Docker (testes locais) |
| Virtualização | Docker Engine (sem VM adicional) |

### Fatores e Níveis

| Fator | Níveis |
|---|---|
| Servidor RTMP | Nginx-RTMP, SRS |
| Transcodificador | FFmpeg, GStreamer |
| Número de espectadores | 1, 10, 50 |
| Resolução de entrada (OBS) | 1080p30, 720p30 |
| Bitrate de entrada (OBS) | 4000 kbps, 6000 kbps |

### Matriz Experimental

Combinação completa (fatorial) dos fatores principais:

| Teste | Servidor | Transcodificador | Espectadores | Resolução entrada |
|---|---|---|---|---|
| T01 | Nginx | FFmpeg | 1 | 1080p |
| T02 | Nginx | FFmpeg | 10 | 1080p |
| T03 | Nginx | FFmpeg | 50 | 1080p |
| T04 | Nginx | GStreamer | 1 | 1080p |
| T05 | Nginx | GStreamer | 10 | 1080p |
| T06 | Nginx | GStreamer | 50 | 1080p |
| T07 | SRS | FFmpeg | 1 | 1080p |
| T08 | SRS | FFmpeg | 10 | 1080p |
| T09 | SRS | FFmpeg | 50 | 1080p |
| T10 | SRS | GStreamer | 1 | 1080p |
| T11 | SRS | GStreamer | 10 | 1080p |
| T12 | SRS | GStreamer | 50 | 1080p |

Cada teste executado com duração mínima de 5 minutos de stream ativa. Repetições: 3 por combinação.

### Métricas de Desempenho

| Métrica | Unidade | Coleta |
|---|---|---|
| Latência ponta-a-ponta (OBS → player) | segundos | Manual / marca de tempo visível no vídeo |
| Latência de startup HLS (primeiro segmento disponível) | segundos | Log do transcodificador |
| Uso de CPU do container rtmp-server | % | `docker stats` |
| Uso de memória do container rtmp-server | MB | `docker stats` |
| Bitrate efetivo de saída por variante | kbps | Análise de segmentos `.ts` |
| Taxa de erros de segmento (404 / falhas HLS.js) | contagem | Log do player |
| Tempo até primeiro segmento após `on_publish` | segundos | Log do transcodificador |
| Estabilidade do pipeline (restarts por sessão) | contagem | Log do transcodificador |

Observação de leitura:
- As métricas de CPU e memória devem ser interpretadas com base no ambiente de testes documentado neste plano. O percentual de CPU é o uso observado no container e pode exceder 100% quando houver consumo de múltiplas threads do host.
- A métrica de memória representa o consumo observado no container e deve ser comparada com a RAM total do host (24 GB) e com o restante da carga dos containers.
- As métricas de latência, bitrate, taxa de erros e reinícios também dependem do host e da stack Docker usada na execução; por isso o mesmo cenário deve ser comparado apenas dentro do mesmo ambiente documentado.

### Ferramentas de Testes

| Ferramenta | Finalidade |
|---|---|
| OBS Studio | Publicação RTMP com parâmetros controlados |
| FFprobe | Análise de segmentos HLS e bitrate |
| `docker stats` | Monitoramento de CPU e memória por container |
| HLS.js (debug mode) | Erros de segmento e tempo de startup no player |
| Prometheus + Micrometer | Métricas de aplicação (backend) |
| Script de carga (headless) | Simular múltiplos espectadores via SSE/HLS |
| Planilha | Consolidação e análise estatística dos dados |

### Critério de Aceitação

| Critério | Valor mínimo aceitável |
|---|---|
| Latência ponta-a-ponta | ≤ 30 segundos (HLS low-latency não está no escopo) |
| Startup HLS | ≤ 24 segundos após início da transmissão |
| CPU média do container RTMP | ≤ 240% (1 e 10 viewers) e ≤ 280% (50 viewers) |
| Estabilidade do pipeline | 0 restarts involuntários em 5 minutos |
| Taxa de erros de segmento | ≤ 1% (1 e 10 viewers) e ≤ 3% (50 viewers) |
| Disponibilidade de variantes | Todas as variantes anunciadas no `master.m3u8` devem responder 200 |

Observação metodológica:
- Os limiares foram recalibrados para o ambiente deste estudo (CPU-only, múltiplas variantes HLS em Docker). O percentual de CPU do container pode ultrapassar 100% por representar consumo agregado em múltiplos núcleos lógicos.

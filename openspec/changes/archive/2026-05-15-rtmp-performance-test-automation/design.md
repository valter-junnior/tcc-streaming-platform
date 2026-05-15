## Context

O projeto já possui execução funcional de live RTMP com variações de servidor (NGINX RTMP e SRS) e transcodificação (FFmpeg e GStreamer), mas a validação de desempenho ainda é manual e pouco repetível. O objetivo desta change é padronizar uma campanha de benchmark operacional executada exclusivamente em Docker, sem introduzir mudanças no comportamento da aplicação existente.

Restrições relevantes:
- Medição deve considerar apenas o intervalo da live ativa (início ao fim), excluindo fases de setup.
- Frontend React e backend Java não são alvo de medição de desempenho nesta etapa.
- A execução inicial deve validar um caso simples em cada combinação de servidor/transcoder antes da suíte completa.
- A suíte final deve rodar todos os cenários com um único comando e gerar CSV consolidado.

Stakeholders primários: autor técnico do TCC, avaliadores de desempenho e operadores que repetirão os testes.

## Goals / Non-Goals

**Goals:**
- Definir uma arquitetura de testes reprodutível para NGINX+FFmpeg, NGINX+GStreamer, SRS+FFmpeg e SRS+GStreamer.
- Estruturar a execução em duas fases: sanity (caso simples por combinação) e campanha completa automatizada.
- Capturar métricas centrais de streaming sob audiência sintética por duração configurável e exportar matriz CSV única.
- Fornecer documentação técnica curta para operação e continuidade dos testes.

**Non-Goals:**
- Alterar código funcional de frontend/backend para otimização de performance.
- Introduzir benchmark distribuído multi-host nesta primeira entrega.
- Cobrir tuning avançado de codecs além das combinações já previstas na matriz.

## Decisions

1. Orquestração via scripts shell + Docker Compose
- Decisão: implementar runner em shell que aciona cenários por perfis/overrides de Compose.
- Racional: mantém aderência ao requisito "apenas em docker" e reduz acoplamento com stack de aplicação.
- Alternativas consideradas:
  - Ferramenta externa de benchmark (k6/locust): descartada para evitar dependência adicional e por não representar tráfego RTMP/HLS fim-a-fim nativamente.
  - Pipeline CI imediato: adiado; foco inicial é execução local controlada e validação metodológica.

2. Dois níveis de execução (sanity e full)
- Decisão: criar modo `sanity` para um caso mínimo por combinação e modo `full` para matriz completa.
- Racional: garante feedback rápido de saúde dos cenários antes de campanha longa.
- Alternativas consideradas:
  - Rodar somente full: aumenta custo de depuração quando um cenário base falha.

3. Coleta de métricas com fontes do host Docker e serviços
- Decisão: coletar CPU/memória/rede dos containers + latência de segmento/erros de reprodução + disponibilidade da live.
- Racional: equilibra custo de implementação e valor analítico para comparação entre NGINX e SRS.
- Alternativas consideradas:
  - Telemetria interna profunda em cada servidor: descartada na primeira versão por elevar complexidade e risco de intrusão.

4. Saída tabular única em CSV
- Decisão: consolidar todos os resultados em um CSV único por execução completa, com colunas normalizadas.
- Racional: facilita geração de matriz comparativa e uso em planilhas/estatística.
- Alternativas consideradas:
  - JSON-only: bom para automação, mas pior para análise rápida no contexto acadêmico.

## Risks / Trade-offs

- [Carga sintética não representar audiência real] -> Definir perfis de viewers por degraus e explicitar limites metodológicos no relatório.
- [Variabilidade por ambiente local] -> Registrar metadados de execução (host, timestamp, versão de imagem, duração) no CSV.
- [Falhas transitórias de container/rede] -> Incluir warmup curto, retries controlados e marcação explícita de erro por cenário.
- [Tempo total elevado na suíte completa] -> Permitir parametrização de duração e quantidade de viewers por rodada.

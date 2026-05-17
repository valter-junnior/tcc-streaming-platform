Atua como um Engenheiro de Testes de Performance e Especialista em Docker e Streaming de Vídeo (RTMP/HLS). 

Estou a desenvolver um Trabalho de Conclusão de Curso (TCC) focado na "Análise Comparativa de Desempenho de Servidores RTMP e Transcodificadores em Plataforma de Streaming ao Vivo". Preciso que me ajudes a escrever (ou melhorar) os scripts de automação do benchmark (em Bash ou Python).

O objetivo do script de teste é iniciar a infraestrutura (via docker-compose), publicar uma stream sintética, simular a carga de espectadores (1, 10 ou 50 viewers simultâneos), recolher as métricas de desempenho e GUARDAR O RESULTADO DIRETAMENTE num ficheiro chamado `resultado.csv`.

REQUISITOS DO SCRIPT DE TESTE:
1. Deve iterar sobre a seguinte matriz de testes:
   - Servidor: Nginx-RTMP ou SRS
   - Transcodificador: FFmpeg ou GStreamer
   - Espectadores (Viewers): 1, 10, 50

2. Deve recolher as seguintes métricas de desempenho durante uma janela de tempo (ex: 5 minutos):
   - Tempo de Startup HLS (segundos)
   - Uso de CPU médio e máximo do contentor RTMP (através do comando `docker stats`)
   - Uso de RAM médio e máximo do contentor RTMP (através do comando `docker stats`)
   - Erros de segmento HLS (respostas HTTP 404 ao fazer curl nas playlists)
   - Bitrate efetivo de saída (calculado ou extraído via ffprobe/logs)
   - Reinícios involuntários do contentor durante a sessão

3. FORMATO DE SAÍDA EXIGIDO (`resultado.csv`):
Ao finalizar cada cenário, o script DEVE anexar (append) uma nova linha ao ficheiro `resultado.csv` utilizando EXATAMENTE o seguinte cabeçalho separado por vírgulas:
Execucao,Servidor,Transcodificador,Espectadores,Duracao_s,Repeticao,Startup_HLS_s,Tempo_Primeiro_Segmento_s,CPU_medio_percent,CPU_max_percent,RAM_media_MB,RAM_max_MB,Bitrate_efetivo_kbps,Erros_segmento,Taxa_erros_percent,Reinicios_sessao,Latencia_ponta_a_ponta_s,Status,Observacoes

Regras para o CSV:
- "Execucao" deve ser o ID do teste (ex: T01, T02, até T12).
- "Status" deve ser 'PASS' ou 'FAIL'.
- A métrica "Latencia_ponta_a_ponta_s" pode ficar como 'manual' se não for automatizável neste momento.
- Campos decimais devem usar ponto (.) e não vírgula (,).

Por favor, gera o código do script de benchmark que realiza a execução destes testes em Docker e implementa a função que formata estas variáveis e faz o 'echo' ou 'write' diretamente para o `resultado.csv` no formato exigido.
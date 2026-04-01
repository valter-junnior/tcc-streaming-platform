---
description: "Implementa alternancia entre nginx e srs com suporte a TRANSCODER=ffmpeg|gstreamer, seguindo o padrao existente do nginx"
name: "Implementar SRS + GStreamer com Alternancia"
argument-hint: "Foco opcional: compose, srs, scripts, backend, testes"
agent: "agent"
---
Implemente no workspace a estrategia de alternancia de servidor RTMP e transcodificador para testes de desempenho, reutilizando regras ja existentes no fluxo com nginx.

Use o argumento informado pelo usuario como foco principal. Se nao houver argumento, cubra `tudo`.

Objetivo principal:
- Permitir alternar na inicializacao dos containers entre `nginx` e `srs`.
- Manter a variavel `TRANSCODER` como chave de selecao de pipeline (`ffmpeg` ou `gstreamer`) de forma consistente.
- Adicionar suporte a `gstreamer` no caminho do `srs`, preservando o `ffmpeg` ja existente.
- Seguir o comportamento e regras de negocio do nginx como referencia quando aplicavel.

Resultado esperado da implementacao:
- `nginx + ffmpeg` e `nginx + gstreamer` continuam funcionando.
- `srs + ffmpeg` e `srs + gstreamer` passam a funcionar.
- A troca de servidor RTMP usa `profiles` do Docker Compose e variavel `COMPOSE_PROFILES` documentada para padronizar os comandos de inicializacao.

Requisitos tecnicos:
1. Revisar e ajustar `docker-compose` e/ou arquivos de override para habilitar alternancia de servidor RTMP por `profiles` (`nginx` e `srs`).
2. Padronizar e documentar o uso de `COMPOSE_PROFILES` como variavel de referencia para os comandos de subida.
3. Garantir que o container SRS tenha scripts e entrypoint equivalentes ao modelo do nginx para selecionar o transcodificador via `TRANSCODER`.
4. Criar/ajustar scripts necessarios para SRS + GStreamer sem quebrar SRS + FFmpeg.
5. Manter nomes de variaveis simples e documentados, evitando duplicacao de logica.
6. Reaproveitar o maximo possivel de convencoes ja usadas no nginx.

Validacao obrigatoria:
- Executar testes tecnicos adequados (build, testes automatizados, validacoes de compose/scripts).
- Verificar cenarios minimos:
  - servidor=`nginx` + `TRANSCODER=ffmpeg`
  - servidor=`nginx` + `TRANSCODER=gstreamer`
  - servidor=`srs` + `TRANSCODER=ffmpeg`
  - servidor=`srs` + `TRANSCODER=gstreamer`
- Se criar artefatos temporarios apenas para validar (scripts ad-hoc, arquivos de teste descartaveis), remova-os antes de finalizar.

Regras de seguranca da mudanca:
- Nao remover suporte existente do nginx.
- Nao alterar contratos de API sem necessidade.
- Evitar hardcode de caminhos/comandos quando ja houver padrao no projeto.

Formato da resposta final:
1. Resumo objetivo do que foi implementado.
2. Lista de arquivos alterados e motivo de cada alteracao.
3. Como executar os 4 cenarios de validacao via variaveis de ambiente.
4. Resultado dos testes executados (incluindo falhas, se houver).
5. Pendencias e proximos passos curtos.

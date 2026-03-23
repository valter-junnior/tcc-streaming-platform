---
description: "Atualiza documentacao e diagramas do projeto com base no estado real do codigo, containers e infraestrutura"
name: "Atualizar Documentacao Completa"
argument-hint: "Foco opcional: tudo, backend, frontend, containers, diagramas, testes"
agent: "agent"
---
Atualize toda a documentacao tecnica do workspace com base no estado real do projeto.

Use o argumento informado pelo usuario como foco principal. Se nenhum foco for informado, cubra `tudo`.

Objetivo:
- Sincronizar documentacao e diagramas com a implementacao atual.
- Corrigir informacoes desatualizadas, inconsistentes ou incompletas.
- Garantir que arquitetura, fluxo e operacao reflitam backend, frontend, containers e pipeline de execucao.

Escopo minimo de analise:
- README e arquivos em `docs/`.
- Backend Java (`app/streaming-platform/`), incluindo configuracoes, testes e recursos.
- Frontend (`app/frontend/`), estrutura de features, rotas e servicos.
- Infra/containers (`app/docker-compose.yml`, `app/backend/rtmp/nginx/`, `app/backend/rtmp/srs/`, scripts de entrada e transcodificacao).
- Diagramas Mermaid existentes em `docs/diagrams/`.

Processo esperado:
1. Mapear o estado atual do codigo, configuracoes e estrutura de pastas relevantes ao foco.
2. Comparar com a documentacao atual e listar divergencias.
3. Atualizar os arquivos de documentacao necessarios com conteudo objetivo e verificavel.
4. Atualizar ou criar diagramas Mermaid necessarios para refletir:
- arquitetura geral
- fluxo de streaming (ingestao, transcodificacao, distribuicao)
- componentes e responsabilidades
- integracoes entre servicos e containers
5. Validar sintaxe dos diagramas Mermaid apos cada alteracao.
6. Garantir consistencia de termos, nomes de servicos e caminhos.

Regras:
- Nao inventar componentes ou fluxos nao presentes no codigo/configuracao.
- Quando houver incerteza, marcar explicitamente como "Nao confirmado" e indicar onde validar.
- Priorizar clareza tecnica e rastreabilidade para arquivos reais do repositorio.
- Preservar o idioma e estilo predominantes da documentacao existente.

Formato da resposta final:
- Resumo das alteracoes realizadas.
- Lista de arquivos alterados com objetivo de cada alteracao.
- Lista de divergencias encontradas e resolvidas.
- Pendencias, riscos ou pontos "Nao confirmado".
- Proximos passos recomendados (curtos e praticos).

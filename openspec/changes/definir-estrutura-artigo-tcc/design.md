## Context

O repositório possui referências de artigos em `docs/examples/`, com prioridade para `artigo_pedro_assunção.pdf`, e um direcionamento explícito em `docs/planning/todo.md` para discutir a estrutura antes de iniciar a escrita. O problema atual é a ausência de um padrão acordado de organização e estilo, o que pode gerar retrabalho nas próximas fases do TCC.

## Goals / Non-Goals

**Goals:**
- Definir um fluxo reprodutível para leitura e extração da macroestrutura dos artigos de referência.
- Produzir uma proposta inicial de estrutura do artigo TCC orientada por evidências dos PDFs analisados.
- Incluir critérios claros para discutir e validar a proposta com o orientador/autor antes da redação.

**Non-Goals:**
- Escrever o conteúdo final do artigo.
- Definir resultados experimentais, tabelas ou conclusões finais nesta etapa.
- Alterar o escopo técnico do projeto de streaming.

## Decisions

- Decisão: Adotar `artigo_pedro_assunção.pdf` como referência principal de forma e escrita, usando os demais PDFs como apoio comparativo.
  Rationale: O pedido no planejamento prioriza explicitamente esse arquivo; a decisão reduz ambiguidades e estabelece referência primária.
  Alternativa considerada: Tratar todos os PDFs com o mesmo peso.
  Motivo da rejeição: Dilui o padrão desejado e pode produzir estrutura híbrida inconsistente.

- Decisão: Formalizar a proposta em estrutura hierárquica de seções obrigatórias, opcionais e diretrizes de escrita por seção.
  Rationale: Facilita revisão e aprovação incremental antes da redação integral.
  Alternativa considerada: Apenas listar títulos de seções.
  Motivo da rejeição: Não garante alinhamento de profundidade e estilo textual.

- Decisão: Definir checklist de validação com critérios de aderência (clareza, sequência lógica, correspondência com referências e completude).
  Rationale: Permite avaliar objetivamente se a proposta está pronta para virar rascunho do artigo.
  Alternativa considerada: Validação subjetiva sem critérios explícitos.
  Motivo da rejeição: Aumenta risco de retrabalho e divergência entre revisores.

## Risks / Trade-offs

- [Interpretação parcial dos PDFs] -> Mitigação: consolidar estrutura comparando mais de uma referência, mas mantendo prioridade do artigo principal.
- [Proposta excessivamente rígida] -> Mitigação: separar seções obrigatórias de opcionais e registrar justificativas.
- [Atraso por falta de consenso] -> Mitigação: incluir perguntas objetivas de aprovação junto da proposta inicial.

## Migration Plan

1. Ler os PDFs em `docs/examples/`, com foco principal em `artigo_pedro_assunção.pdf`.
2. Extrair padrões de organização: sequência de seções, granularidade e tom de escrita.
3. Sintetizar proposta de estrutura inicial com seções e objetivos por seção.
4. Aplicar checklist de validação e listar pontos de decisão para discussão.
5. Após aprovação, usar a estrutura acordada como base para iniciar a redação em `docs/tcc/report/`.

Rollback strategy:
- Se a proposta for rejeitada, retornar à etapa de síntese e gerar versão revisada, mantendo histórico das decisões recusadas.

## Open Questions

- Qual evento/disciplina define o template normativo final (ABNT, SBC, IEEE ou outro)?
- O artigo deve priorizar relato de engenharia, avaliação experimental ou equilíbrio entre ambos?
- Há limite de páginas ou seções obrigatório já definido pelo curso/orientador?

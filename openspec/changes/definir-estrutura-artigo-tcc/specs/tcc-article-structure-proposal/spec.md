## ADDED Requirements

### Requirement: Analyze reference PDFs before drafting structure
O processo MUST analisar os arquivos PDF em `docs/examples/` antes de propor qualquer estrutura inicial do artigo, com prioridade explícita para `artigo_pedro_assunção.pdf` como referência principal.

#### Scenario: Main reference is prioritized
- **WHEN** a estrutura inicial do artigo for preparada
- **THEN** a análise MUST registrar `artigo_pedro_assunção.pdf` como referência principal e os demais PDFs como apoio comparativo

### Requirement: Produce discussable initial article structure
O processo SHALL gerar uma proposta inicial de estrutura do artigo TCC antes da escrita completa, contendo seções sugeridas, objetivo de cada seção e ordem lógica de apresentação.

#### Scenario: Structure proposal is ready for discussion
- **WHEN** a fase de preparação for concluída
- **THEN** deve existir uma proposta com seções nomeadas, descrição breve por seção e sequência de leitura recomendada

### Requirement: Include writing-style alignment criteria
A proposta MUST incluir critérios de aderência de escrita para garantir consistência com os artigos de referência (clareza, profundidade esperada e estilo técnico-acadêmico).

#### Scenario: Validation criteria are explicit
- **WHEN** a proposta de estrutura for apresentada para revisão
- **THEN** os critérios de aderência de escrita devem estar explícitos e utilizáveis como checklist de aprovação

### Requirement: Gate drafting on structure approval
A redação completa do artigo SHALL iniciar somente após a discussão e validação da proposta de estrutura inicial.

#### Scenario: Drafting starts only after approval
- **WHEN** não houver validação da proposta de estrutura
- **THEN** a próxima etapa deve permanecer em revisão e não avançar para redação integral

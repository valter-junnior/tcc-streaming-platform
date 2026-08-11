# Novo template do TCC

O arquivo de entrada é `main.tex`. A estrutura segue exatamente os capítulos solicitados:

1. Introdução
2. Trabalhos Relacionados
3. Metodologia
4. Resultados e Discussão
5. Conclusão
6. Referências

Compile com:

```bash
cd docs/tcc/latex/novo
pdflatex -interaction=nonstopmode main.tex
pdflatex -interaction=nonstopmode main.tex
```

O template usa os resultados agregados versionados em `docs/benchmark/results-aggregated.csv`, mas mantém explícitas as limitações encontradas na revisão: latência ponta a ponta não instrumentada, diferença a validar na configuração dos pipelines e necessidade de investigar erros de segmentos em carga alta.

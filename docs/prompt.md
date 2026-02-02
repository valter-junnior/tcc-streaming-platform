1. siga esta estrutura para o frontend

+ app (logica de regras de negocio em geral)
    + config
    + services
    + types
    + routes
+ features (implementação das features)
    + user(usando user como exemplo tudo implementado aqui é especifico do user mas claro podendo ser chaado por outras features)
        + components
        + pages
        + hooks
+ shared (components, libs, hooks compartilhados entre features, layout)

2. utilize .env na aplicação

3. faça toda a task 4 do arquivo todo.md (Fase 4: Frontend React )
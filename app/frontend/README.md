# Frontend da plataforma

Aplicação React e TypeScript usada para criar transmissões e assistir às lives da plataforma. O ambiente completo e as variáveis estão descritos no [README principal](../../README.md).

```bash
cd app
cp .env.example .env
# Edite as senhas de .env antes de iniciar a stack.
docker compose --profile nginx up -d --build
```

Para desenvolver o frontend fora do Docker:

```bash
cd app/frontend
npm ci
npm run dev
```

A interface usa a API do backend e reproduz a saída HLS. Os valores `VITE_*` são definidos em `app/.env.example` e devem ser ajustados para o ambiente local.

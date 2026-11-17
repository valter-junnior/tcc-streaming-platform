# Frontend da plataforma

Aplicação React e TypeScript usada para criar transmissões e assistir às lives da plataforma. O ambiente completo e as variáveis estão descritos no [README principal](../../README.md). Execute os comandos abaixo a partir da raiz do repositório.

```bash
cd app
cp .env.example .env
# Edite as senhas de .env antes de iniciar a stack.
docker compose up -d --build
```

O `.env.example` inicia SRS com GStreamer. Para usar Nginx ou FFmpeg, altere `COMPOSE_PROFILES` e `TRANSCODER` no `.env` antes de subir os serviços. A interface fica em [http://localhost:3001](http://localhost:3001).

Para desenvolver o frontend fora do Docker:

```bash
cd app/frontend
npm ci
npm run dev
```

O Vite local usa a porta `3001`, a mesma do contêiner `frontend`. Se a stack Docker já estiver rodando, pare apenas esse contêiner com `docker compose stop frontend` (a partir de `app/`) antes de executar `npm run dev`.

O Vite executado fora do Docker não lê automaticamente `app/.env`. Sem configuração adicional, a aplicação usa os endereços locais definidos em `src/app/config/env.ts`: API em `localhost:8080`, RTMP em `localhost:1935` e HLS em `localhost:8081`. Para outros endereços, crie `app/frontend/.env.local` com as variáveis `VITE_*` correspondentes e reinicie o Vite. O proxy de desenvolvimento encaminha `/api` ao backend configurado por `VITE_BACKEND_HOST` e `VITE_BACKEND_PORT` (padrão `localhost:8080`).

Para validar a interface:

```bash
npm run build
npm run lint
```

O build passa atualmente. O lint ainda aponta erros preexistentes de tipos e hooks, que precisam ser corrigidos antes de usar essa verificação como critério de aprovação.

# Streaming Platform - Frontend

Frontend da plataforma de streaming construído com React, TypeScript e Vite.

## 🚀 Desenvolvimento

### Docker (Recomendado)

O frontend está configurado para rodar automaticamente via Docker Compose:

```bash
# A partir da raiz do projeto
cd app/
docker compose up -d frontend

# Ver logs
docker compose logs -f frontend
```

**Variáveis de ambiente**: Configuradas no arquivo `/app/.env` (centralizado).

### Desenvolvimento Local

Se preferir rodar localmente fora do Docker:

```bash
cd app/frontend/

# Instalar dependências
npm install

# Rodar em modo desenvolvimento
npm run dev

# Build para produção
npm run build

# Preview da build
npm run preview
```

**Nota**: Para desenvolvimento local, você precisa das variáveis de ambiente. Elas são carregadas automaticamente do arquivo `/app/.env` quando rodando via Docker.

## 🔧 Configuração

### Variáveis de Ambiente

Todas as variáveis estão centralizadas em `/app/.env`:

- `FRONTEND_PORT`: Porta do servidor (padrão: 3001)
- `VITE_API_URL`: URL da API backend
- `VITE_WS_URL`: URL do WebSocket
- `VITE_RTMP_URL`: URL do servidor RTMP
- `VITE_HLS_URL`: URL do servidor HLS
- `VITE_APP_URL`: URL do frontend

### Proxy de Desenvolvimento

O Vite está configurado para fazer proxy das requisições:
- `/api` → Backend (porta 8080)
- `/ws` → WebSocket (porta 8080)

Isso evita problemas de CORS durante o desenvolvimento.

## 📦 Stack Tecnológico

- **React 19**: Framework UI
- **TypeScript**: Type safety
- **Vite**: Build tool e dev server
- **TailwindCSS**: Estilização
- **React Router**: Roteamento
- **Axios**: Cliente HTTP
- **HLS.js**: Player de vídeo HLS
- **STOMP.js**: Cliente WebSocket
- **Radix UI**: Componentes acessíveis

## 🏗️ Estrutura

```
src/
├── app/
│   ├── config/         # Configurações
│   ├── routes/         # Rotas
│   ├── services/       # Services (API, WebSocket)
│   └── types/          # TypeScript types
├── features/
│   └── stream/         # Feature de streaming
│       ├── components/
│       └── pages/
└── shared/
    ├── components/     # Componentes compartilhados
    ├── hooks/          # Custom hooks
    ├── layouts/        # Layouts
    └── lib/            # Utilitários
```

## 🔗 Acessos

- **Frontend**: http://localhost:3001
- **Backend API**: http://localhost:8080
- **HLS Player**: Acesse uma stream em `http://localhost:3001/watch/{stream-key}`

---

## React + TypeScript + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Babel](https://babeljs.io/) (or [oxc](https://oxc.rs) when used in [rolldown-vite](https://vite.dev/guide/rolldown)) for Fast Refresh
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/) for Fast Refresh

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend updating the configuration to enable type-aware lint rules:

```js
export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...

      // Remove tseslint.configs.recommended and replace with this
      tseslint.configs.recommendedTypeChecked,
      // Alternatively, use this for stricter rules
      tseslint.configs.strictTypeChecked,
      // Optionally, add this for stylistic rules
      tseslint.configs.stylisticTypeChecked,

      // Other configs...
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])
```

You can also install [eslint-plugin-react-x](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-x) and [eslint-plugin-react-dom](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-dom) for React-specific lint rules:

```js
// eslint.config.js
import reactX from 'eslint-plugin-react-x'
import reactDom from 'eslint-plugin-react-dom'

export default defineConfig([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...
      // Enable lint rules for React
      reactX.configs['recommended-typescript'],
      // Enable lint rules for React DOM
      reactDom.configs.recommended,
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])
```

## ✅ Frontend - Erro de build e runtime com video.js [RESOLVIDO]

### Erro 1: Build-time
```
Cannot read file: /app/src/shims/global.ts/window
Cannot read file: /app/src/shims/global.ts/document
```

**Causa:**
- Alias `global: path.resolve(__dirname, "./src/shims/global.ts")` no vite.config.ts
- Conflitava com pacote npm `global` que video.js importa (`global/window`, `global/document`)

**Solução:**
1. Remover alias conflitante do vite.config.ts
2. Adicionar `optimizeDeps: { include: ["video.js", "@videojs/http-streaming"] }`
3. Atualizar manualChunks: `hls-vendor` → `video-vendor`

### Erro 2: Runtime (navegador)
```
Uncaught ReferenceError: global is not defined
```

**Causa:**
- Variável `global` do Node.js não existe no navegador
- video.js tenta acessar `global` em runtime

**Solução:**
1. Adicionar polyfill no vite.config.ts: `define: { global: "globalThis" }`
2. Limpar cache: `rm -rf node_modules/.vite`
3. Reiniciar container

**Status:** Frontend rodando em http://localhost:3001 ✅
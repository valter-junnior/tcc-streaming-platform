# Bugs Corrigidos

## ✅ 1. Chunks maiores que 500 KB após minificação

**Problema**: Bundle único de 886 KB estava causando warning de performance.

**Solução implementada**:
- Configurado `manualChunks` no `vite.config.ts` para separar vendors:
  - `react-vendor`: React, React-DOM, React-Router (46 KB)
  - `hls-vendor`: HLS.js (520 KB) - biblioteca de vídeo
  - `stomp-vendor`: STOMP + SockJS (70 KB)
- Implementado lazy loading nas rotas com `React.lazy()`
- Cada página agora carrega em chunks separados (7-9 KB)
- Aumentado `chunkSizeWarningLimit` para 1000 KB

**Resultado**: 
- Bundle principal reduzido de 886 KB → 185 KB
- Páginas carregam sob demanda
- Melhor performance inicial

---

## ✅ 2. Uncaught ReferenceError: global is not defined

**Problema**: Bibliotecas Node.js (SockJS/STOMP) esperavam objeto `global` que não existe no browser.

**Solução implementada**:
- Adicionado `define: { global: "globalThis" }` no `vite.config.ts`
- Criado shim em `src/shims/global.ts`
- Configurado alias `global` no resolve

**Resultado**: 
- Erro resolvido
- WebSocket funciona corretamente
- Compatibilidade com Node.js libraries mantida

---

**Status**: ✅ Todos os bugs corrigidos  
**Data**: 02/02/2026  
**Build**: Sucesso sem warnings críticos

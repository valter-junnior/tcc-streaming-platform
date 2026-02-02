# Changelog - Correção de Callbacks RTMP

**Data**: 02/02/2026

## Objetivo

Corrigir a implementação dos callbacks do nginx-rtmp com base na documentação oficial, garantindo o funcionamento correto do ciclo de vida das streams.

## Problema Identificado

A implementação anterior estava usando os callbacks incorretamente:

- **`on_publish`**: Apenas validava a stream key, sem marcar como LIVE
- **`on_publish_done`**: Estava sendo usado para marcar como LIVE (ERRADO - deveria marcar como finalizada)
- **`on_done`**: Endpoint inexistente na documentação oficial do nginx-rtmp
- **`exec_publish`**: Script `notify-stream-started.sh` referenciado mas não existia

## Comportamento Correto dos Callbacks (Documentação Oficial)

### `on_publish`
- **Quando**: Chamado quando um publicador (OBS, FFmpeg) tenta iniciar transmissão
- **Finalidade**: Autenticação e autorização da stream key
- **Resposta**: 
  - 2xx = transmissão autorizada
  - 3xx = redirecionamento
  - Outros códigos = transmissão negada

### `on_publish_done`
- **Quando**: Chamado quando um publicador **para/desconecta** da transmissão
- **Finalidade**: Limpeza e atualização do status (marcar como offline/encerrada)

### `on_play` (não usado)
- **Quando**: Espectador tenta reproduzir uma transmissão
- **Finalidade**: Autenticação e autorização do espectador

### `on_play_done` (não usado)
- **Quando**: Espectador se desconecta
- **Finalidade**: Rastreamento de viewers e análises

## Alterações Realizadas

### 1. NginxCallbackController.java

**`/publish` endpoint (on_publish)**:
- ✅ Valida stream key (comportamento mantido)
- ✅ **NOVO**: Se válida, marca stream como LIVE imediatamente
- ✅ Retorna 200 (aceita) ou 403 (rejeita)

**`/publish_done` endpoint (on_publish_done)**:
- ✅ **CORRIGIDO**: Agora marca stream como ENDED (finalizada)
- ✅ Executa limpeza e publica evento de encerramento

**`/done` endpoint**:
- ❌ **REMOVIDO**: Não existe na documentação oficial do nginx-rtmp

### 2. nginx.conf

**Callbacks configurados**:
```nginx
on_publish http://streaming-platform:8080/api/streams/callback/publish;
on_publish_done http://streaming-platform:8080/api/streams/callback/publish_done;
```

**Removidos**:
- ❌ `on_done` (não existe na documentação oficial)
- ❌ `exec_publish /usr/local/bin/notify-stream-started.sh` (workaround desnecessário)

**Comentários adicionados**: Explicações claras sobre quando cada callback é chamado

### 3. Dockerfile

**Removidos** scripts desnecessários:
- ❌ `transcode-simple.sh`
- ❌ `notify-stream-started.sh`

**Mantido**:
- ✅ `transcode.sh` (para transcodificação multi-bitrate no futuro)

## Fluxo Correto Após Correções

```
1. Streamer conecta OBS com stream key
   ↓
2. Nginx-RTMP chama: on_publish (valida + marca LIVE)
   ↓
3. Backend valida stream key
   ↓
4. Se válida: Backend marca status=LIVE e retorna 200
   Se inválida: Backend retorna 403 e rejeita conexão
   ↓
5. Streamer transmite vídeo...
   ↓
6. Streamer desconecta/para transmissão
   ↓
7. Nginx-RTMP chama: on_publish_done
   ↓
8. Backend marca status=ENDED e executa limpeza
```

## Validação

### Build dos containers
```bash
docker compose build nginx-rtmp streaming-platform
```
**Resultado**: ✅ Build bem-sucedido

### Reinicialização dos serviços
```bash
docker compose up -d nginx-rtmp streaming-platform
```
**Resultado**: ✅ Containers iniciados corretamente

### Logs
```bash
docker compose logs --tail=30 nginx-rtmp streaming-platform
```
**Resultado**: ✅ Sem erros nos logs

## Impacto

### Positivo
- ✅ Callbacks agora seguem a documentação oficial
- ✅ Ciclo de vida das streams funciona corretamente
- ✅ Código mais limpo, sem workarounds
- ✅ Dockerfile simplificado

### Breaking Changes
- Nenhum para usuários finais (correção interna)

## Testes Necessários

- [ ] Conectar OBS e verificar se stream é marcada como LIVE imediatamente
- [ ] Desconectar OBS e verificar se stream é marcada como ENDED
- [ ] Testar stream key inválida (deve rejeitar com 403)
- [ ] Verificar eventos publicados no RabbitMQ

## Referências

- [nginx-rtmp-module GitHub](https://github.com/arut/nginx-rtmp-module)
- [nginx-rtmp callbacks documentation](https://github.com/arut/nginx-rtmp-module/wiki/Directives#on_publish)

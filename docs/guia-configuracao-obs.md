# Guia de Configuração - OBS Studio para Streaming

**Plataforma**: StreamLab - TCC  
**Data**: 02/02/2026  
**Versão OBS**: 30.0+ (compatível com versões anteriores)

---

## 📋 Pré-requisitos

Antes de iniciar, certifique-se de que:

- ✅ Todos os serviços Docker estão rodando:
  ```bash
  docker ps
  # Deve mostrar: streaming-platform, nginx-rtmp, postgres, redis, rabbitmq
  ```

- ✅ Frontend está acessível em `http://localhost:3001`

- ✅ Backend está respondendo em `http://localhost:8080`

- ✅ Servidor RTMP está ativo na porta `1935`

- ✅ OBS Studio está instalado no seu sistema

---

## 🎯 Passo 1: Criar Stream no Frontend

1. **Acesse o frontend**:
   ```
   http://localhost:3001
   ```

2. **Clique em "Iniciar Streaming"**

3. **Preencha o formulário**:
   - **Título**: Ex: "Teste de Streaming"
   - **Descrição**: Ex: "Testando a plataforma de streaming"

4. **Clique em "Criar Stream"**

5. **Você será redirecionado para o Dashboard**

6. **Anote as credenciais exibidas**:
   ```
   URL do Servidor: rtmp://localhost:1935/live
   Chave de Transmissão: [UUID gerado automaticamente]
   ```

---

## 🎥 Passo 2: Configurar OBS Studio

### 2.1 Abrir Configurações

1. Abra o **OBS Studio**

2. Clique em **Arquivo** → **Configurações** (ou **File** → **Settings**)

   Atalho: `Ctrl + ,` (Windows/Linux) ou `Cmd + ,` (Mac)

### 2.2 Configurar Transmissão (Stream)

1. No menu lateral, clique em **Transmissão** (ou **Stream**)

2. Configure os seguintes campos:

   **Serviço**: `Personalizado...` (ou `Custom...`)
   
   **Servidor**:
   ```
   rtmp://localhost:1935/live
   ```
   
   **Chave de Transmissão**:
   ```
   [Cole a chave copiada do dashboard]
   ```
   
   Exemplo:
   ```
   550e8400-e29b-41d4-a716-446655440000
   ```

3. **NÃO marque** a opção "Usar autenticação"

4. Clique em **Aplicar**

### 2.3 Configurar Saída (Output)

1. No menu lateral, clique em **Saída** (ou **Output**)

2. **Modo de Saída**: Selecione `Avançado` (ou `Advanced`)

3. Aba **Transmissão** (ou **Streaming**):

   **Encoder de Vídeo**: `x264`
   
   **Bitrate**: `2500 Kbps` (para 720p) ou `5000 Kbps` (para 1080p)
   
   **Keyframe Interval**: `2` segundos
   
   **Preset**: `veryfast` (melhor para streaming ao vivo)
   
   **Profile**: `main`
   
   **Tune**: `zerolatency` (importante!)

4. Clique em **Aplicar**

### 2.4 Configurar Vídeo

1. No menu lateral, clique em **Vídeo** (ou **Video**)

2. Configure:

   **Resolução Base (Canvas)**: `1920x1080` (ou sua resolução nativa)
   
   **Resolução de Saída (Escalada)**: 
   - Para 1080p: `1920x1080`
   - Para 720p: `1280x720`
   
   **Filtro de Redução de Escala**: `Bicubic` (melhor qualidade)
   
   **FPS Comum**: `30` (ou `60` se tiver PC potente)

3. Clique em **Aplicar**

### 2.5 Configurar Áudio

1. No menu lateral, clique em **Áudio** (ou **Audio**)

2. Configure:

   **Taxa de Amostragem**: `44.1 kHz` ou `48 kHz`
   
   **Canais**: `Estéreo`
   
   **Dispositivos de Áudio**: Selecione seu microfone/desktop audio

3. Clique em **OK** para salvar todas as configurações

---

## 🎬 Passo 3: Adicionar Fontes (Sources)

### 3.1 Adicionar Fonte de Captura

1. Na seção **Fontes** (ou **Sources**), clique em **+**

2. Escolha o tipo de fonte:

   **Para capturar tela inteira**:
   - Selecione `Captura de Tela` (ou `Display Capture`)
   
   **Para capturar janela específica**:
   - Selecione `Captura de Janela` (ou `Window Capture`)
   
   **Para capturar webcam**:
   - Selecione `Dispositivo de Captura de Vídeo` (ou `Video Capture Device`)
   
   **Para capturar jogo**:
   - Selecione `Captura de Jogo` (ou `Game Capture`)

3. Dê um nome para a fonte e clique em **OK**

4. Configure as opções da fonte e clique em **OK**

### 3.2 Adicionar Texto ou Imagens (Opcional)

- **Texto**: `Texto (GDI+)` ou `Text (GDI+)`
- **Imagem**: `Imagem` ou `Image`
- **Navegador**: `Fonte do Navegador` ou `Browser Source`

---

## 🚀 Passo 4: Iniciar Transmissão

### 4.1 Verificar Configurações

✅ Servidor RTMP configurado corretamente  
✅ Chave de transmissão colada  
✅ Fontes de vídeo/áudio adicionadas  
✅ Preview mostrando conteúdo

### 4.2 Iniciar Stream

1. Clique no botão **Iniciar Transmissão** (ou **Start Streaming**)

   Localização: Canto inferior direito do OBS

2. **Aguarde 5-10 segundos** para o servidor processar

3. **Verifique o dashboard** no navegador:
   - Status deve mudar de `AGUARDANDO` para `AO VIVO`
   - Indicador verde deve aparecer

### 4.3 Verificar Transmissão

1. No dashboard, **copie o link compartilhável**:
   ```
   http://localhost:3001/watch/[stream-id]
   ```

2. **Abra em uma aba anônima** ou outro navegador

3. O **player HLS deve carregar** e exibir sua transmissão

4. **Latência esperada**: 10-20 segundos (normal para HLS)

---

## 📊 Passo 5: Monitorar Stream

### 5.1 Indicadores no OBS

**Barra inferior direita**:
- 🟢 **Verde**: Transmissão estável
- 🟡 **Amarelo**: Problemas leves
- 🔴 **Vermelho**: Problemas graves

**Estatísticas** (clique no ícone de gráfico):
- **FPS de Renderização**: Deve estar próximo ao configurado (30/60)
- **FPS de Codificação**: Não deve cair
- **Frames Perdidos**: Deve ser 0% ou próximo

### 5.2 Indicadores no Dashboard

- **Status**: `🟢 AO VIVO`
- **Espectadores Atuais**: Atualiza em tempo real
- **Pico de Espectadores**: Maior número alcançado

---

## ⚠️ Troubleshooting - Problemas Comuns

### Problema 1: OBS não conecta ao servidor

**Sintomas**:
- Botão "Iniciar Transmissão" fica carregando
- Mensagem de erro "Failed to connect to server"

**Soluções**:
1. Verificar se o Nginx-RTMP está rodando:
   ```bash
   docker ps | grep nginx-rtmp
   ```

2. Verificar logs do Nginx:
   ```bash
   docker logs nginx-rtmp
   ```

3. Testar conectividade:
   ```bash
   telnet localhost 1935
   ```

4. Verificar se a chave está correta (copiar novamente do dashboard)

---

### Problema 2: Stream conecta mas não fica "AO VIVO"

**Sintomas**:
- OBS mostra "transmitindo"
- Dashboard continua em "AGUARDANDO"

**Soluções**:
1. Verificar logs do backend:
   ```bash
   docker logs streaming-platform --tail 50
   ```

2. Verificar se o callback está sendo chamado:
   - Deve aparecer log de `publish_done` no backend

3. Reiniciar backend:
   ```bash
   docker restart streaming-platform
   ```

---

### Problema 3: Player não carrega/não exibe vídeo

**Sintomas**:
- Status "AO VIVO" mas player fica carregando
- Erro "Stream offline"

**Soluções**:
1. Verificar se o HLS está sendo gerado:
   ```bash
   docker exec nginx-rtmp ls -la /hls/[stream-key]/
   ```
   
   Deve mostrar arquivos `.m3u8` e `.ts`

2. Verificar logs do FFmpeg (transcodificação):
   ```bash
   docker logs nginx-rtmp | grep ffmpeg
   ```

3. Aguardar 10-15 segundos (tempo para gerar segmentos HLS)

4. Atualizar página do player

---

### Problema 4: Frames perdidos / Lag

**Sintomas**:
- OBS mostra frames perdidos
- Transmissão travando

**Soluções**:
1. **Reduzir bitrate**:
   - Configurações → Saída → Bitrate: `1500-2000 Kbps`

2. **Mudar preset**:
   - Configurações → Saída → Preset: `ultrafast`

3. **Reduzir resolução**:
   - Configurações → Vídeo → Resolução de Saída: `1280x720`

4. **Reduzir FPS**:
   - Configurações → Vídeo → FPS: `30`

5. **Fechar programas em segundo plano**

---

### Problema 5: Áudio dessincronizado

**Sintomas**:
- Áudio não combina com vídeo

**Soluções**:
1. **Adicionar delay de áudio**:
   - Clique com botão direito na fonte de áudio
   - Propriedades → Sincronização → Deslocamento: `+200ms` (ajustar conforme necessário)

2. **Verificar taxa de amostragem**:
   - Configurações → Áudio → Taxa de Amostragem: Usar mesma em todos dispositivos

---

## 🎓 Dicas de Performance

### Para PC com configuração básica:
```
Resolução: 1280x720
Bitrate: 1500 Kbps
FPS: 30
Preset: ultrafast
```

### Para PC com configuração média:
```
Resolução: 1280x720
Bitrate: 2500 Kbps
FPS: 30
Preset: veryfast
```

### Para PC com configuração alta:
```
Resolução: 1920x1080
Bitrate: 5000 Kbps
FPS: 60
Preset: fast
```

---

## 📝 Checklist Rápido

Antes de cada transmissão:

- [ ] Docker containers rodando
- [ ] Stream criada no frontend
- [ ] Credenciais RTMP copiadas
- [ ] OBS configurado corretamente
- [ ] Fontes de vídeo/áudio adicionadas
- [ ] Preview mostrando conteúdo
- [ ] Áudio testado
- [ ] Link de compartilhamento testado

Durante a transmissão:

- [ ] Status "AO VIVO" no dashboard
- [ ] FPS estável no OBS
- [ ] Frames perdidos = 0%
- [ ] Player funcionando em aba anônima

Ao finalizar:

- [ ] Clicar "Parar Transmissão" no OBS
- [ ] Clicar "Encerrar Transmissão" no dashboard
- [ ] Verificar status "ENCERRADA"

---

## 🔗 Links Úteis

- **Frontend**: http://localhost:3001
- **Backend API**: http://localhost:8080/api
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **RabbitMQ Management**: http://localhost:15672 (guest/guest)
- **Documentação OBS**: https://obsproject.com/wiki/

---

## 📞 Suporte

Em caso de problemas não resolvidos:

1. **Verificar logs**:
   ```bash
   docker logs streaming-platform
   docker logs nginx-rtmp
   ```

2. **Verificar issues conhecidos**: `/docs/bugs.md`

3. **Consultar documentação**: `/docs/documentacao.md`

---

**Última atualização**: 02/02/2026  
**Testado com**: OBS Studio 30.0.2, Ubuntu 22.04, Docker 24.0

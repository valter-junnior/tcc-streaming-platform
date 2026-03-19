Analise da live e61d6ba69e064693

Erros identificados no GStreamer

- O pipeline falhou na origem RTMP, nao na escrita HLS.
- O log mostrou a sequencia abaixo na primeira tentativa:

```text
ERROR: from element /GstPipeline:pipeline0/GstRTMPSrc:rtmpsrc0: Could not read from resource.
Failed to read any data from stream, check your URL
ERROR: from element /GstPipeline:pipeline0/GstFlvDemux:demux: Internal data stream error.
Got EOS before any data
ERROR: pipeline doesn't want to preroll.
```

- O bootstrap tambem falhou duas vezes por ausencia de playlists:

```text
Attempt 1 failed bootstrap: no variant playlist created after 20s
WARNING: HLS playlists NOT generated - GStreamer did not produce output
```

Diagnostico

- O stream foi autorizado pelo backend, mas o GStreamer foi iniciado antes de haver midia RTMP legivel.
- O script usava apenas espera fixa e depois desistia no primeiro bootstrap sem playlist, mesmo quando a stream ainda estava marcada como ativa.
- Nao houve indicio de erro de permissao nesta live: os arquivos em /tmp/hls/e61d6ba69e064693 estavam com nobody:nogroup e permissao de escrita correta.

Correcao aplicada

- O script [app/backend/rtmp/nginx/scripts/transcode-gstreamer.sh](app/backend/rtmp/nginx/scripts/transcode-gstreamer.sh) agora espera ate que o ffprobe consiga ler a origem RTMP antes de lancar o gst-launch-1.0.
- O fluxo de retry deixou de abortar imediatamente em falha de bootstrap; se a stream ainda estiver ativa, o transcoder tenta novamente dentro do limite configurado.
- Isso ataca exatamente o erro visto nesta live: inicializacao prematura do pipeline quando ainda nao havia dados suficientes no RTMP.

Validacao esperada

- O transcode.log deve passar a registrar "RTMP input became readable" antes do primeiro launch.
- Em falha temporaria de bootstrap, o log deve mostrar retry em vez de encerramento imediato.
- As playlists v0/playlist.m3u8 e/ou v2/playlist.m3u8 devem ser geradas quando a origem estiver realmente entregando audio/video.
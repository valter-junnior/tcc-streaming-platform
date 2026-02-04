#!/bin/bash

# Wrapper script para escolher entre FFmpeg ou GStreamer baseado na variável TRANSCODING_ENGINE
# Uso: transcode-wrapper.sh <stream_key>

STREAM_KEY=$1
TRANSCODING_ENGINE=${TRANSCODING_ENGINE:-ffmpeg}

if [ -z "$STREAM_KEY" ]; then
    echo "ERRO: Stream key não fornecida"
    echo "Uso: $0 <stream_key>"
    exit 1
fi

echo "$(date): Iniciando transcodificação com engine: $TRANSCODING_ENGINE para stream: $STREAM_KEY"

case "$TRANSCODING_ENGINE" in
    "ffmpeg")
        echo "$(date): Usando FFmpeg para transcodificação"
        exec /usr/local/bin/transcode-ffmpeg.sh "$STREAM_KEY"
        ;;
    "gstreamer")
        echo "$(date): Usando GStreamer para transcodificação"
        exec /usr/local/bin/transcode-gstreamer.sh "$STREAM_KEY"
        ;;
    *)
        echo "ERRO: Engine de transcodificação inválido: $TRANSCODING_ENGINE"
        echo "Engines suportados: ffmpeg, gstreamer"
        exit 1
        ;;
esac
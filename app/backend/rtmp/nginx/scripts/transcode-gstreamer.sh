#!/bin/bash

# Script de Transcodificação com GStreamer para Streaming Platform
# Converte stream RTMP em múltiplas qualidades e gera HLS

# Parâmetros
STREAM_KEY=$1
INPUT_URL="rtmp://localhost:1935/live/$STREAM_KEY"
OUTPUT_DIR="/tmp/hls/$STREAM_KEY"

# Validações
if [ -z "$STREAM_KEY" ]; then
    echo "ERRO: Stream key não fornecida"
    echo "Uso: $0 <stream_key>"
    exit 1
fi

# Criar diretório de saída
mkdir -p "$OUTPUT_DIR"

echo "$(date): Iniciando transcodificação GStreamer para stream: $STREAM_KEY"
echo "Input: $INPUT_URL"
echo "Output: $OUTPUT_DIR"

# Pipeline GStreamer para múltiplas qualidades
# Usando tee para dividir o stream em múltiplos pipelines de saída

gst-launch-1.0 \
    rtmpsrc location="$INPUT_URL" ! \
    flvdemux name=demux \
    demux.video ! queue ! h264parse ! avdec_h264 ! \
    tee name=video_tee \
    demux.audio ! queue ! aacparse ! avdec_aac ! \
    audioconvert ! audioresample ! \
    tee name=audio_tee \
    \
    video_tee. ! queue ! \
    videoscale ! video/x-raw,width=1920,height=1080 ! \
    x264enc bitrate=5000 speed-preset=4 tune=zerolatency ! \
    h264parse ! queue ! \
    mux1.video \
    audio_tee. ! queue ! \
    voaacenc bitrate=128000 ! aacparse ! queue ! \
    mux1.audio \
    hlssink2 name=mux1 \
        location="$OUTPUT_DIR/${STREAM_KEY}-1080p_%05d.ts" \
        playlist-location="$OUTPUT_DIR/${STREAM_KEY}-1080p.m3u8" \
        playlist-length=10 \
        target-duration=6 \
        max-files=10 \
    \
    video_tee. ! queue ! \
    videoscale ! video/x-raw,width=1280,height=720 ! \
    x264enc bitrate=2800 speed-preset=4 tune=zerolatency ! \
    h264parse ! queue ! \
    mux2.video \
    audio_tee. ! queue ! \
    voaacenc bitrate=128000 ! aacparse ! queue ! \
    mux2.audio \
    hlssink2 name=mux2 \
        location="$OUTPUT_DIR/${STREAM_KEY}-720p_%05d.ts" \
        playlist-location="$OUTPUT_DIR/${STREAM_KEY}-720p.m3u8" \
        playlist-length=10 \
        target-duration=6 \
        max-files=10 \
    \
    video_tee. ! queue ! \
    videoscale ! video/x-raw,width=854,height=480 ! \
    x264enc bitrate=1400 speed-preset=6 tune=zerolatency ! \
    h264parse ! queue ! \
    mux3.video \
    audio_tee. ! queue ! \
    voaacenc bitrate=128000 ! aacparse ! queue ! \
    mux3.audio \
    hlssink2 name=mux3 \
        location="$OUTPUT_DIR/${STREAM_KEY}-480p_%05d.ts" \
        playlist-location="$OUTPUT_DIR/${STREAM_KEY}-480p.m3u8" \
        playlist-length=10 \
        target-duration=6 \
        max-files=10 \
    \
    video_tee. ! queue ! \
    videoscale ! video/x-raw,width=640,height=360 ! \
    x264enc bitrate=800 speed-preset=8 tune=zerolatency ! \
    h264parse ! queue ! \
    mux4.video \
    audio_tee. ! queue ! \
    voaacenc bitrate=128000 ! aacparse ! queue ! \
    mux4.audio \
    hlssink2 name=mux4 \
        location="$OUTPUT_DIR/${STREAM_KEY}-360p_%05d.ts" \
        playlist-location="$OUTPUT_DIR/${STREAM_KEY}-360p.m3u8" \
        playlist-length=10 \
        target-duration=6 \
        max-files=10 &

GSTREAMER_PID=$!

# Função para cleanup
cleanup() {
    echo "$(date): Encerrando transcodificação para stream: $STREAM_KEY"
    kill $GSTREAMER_PID 2>/dev/null
    wait $GSTREAMER_PID 2>/dev/null
    
    # Criar master playlist
    create_master_playlist
    
    echo "$(date): Transcodificação encerrada para stream: $STREAM_KEY"
}

# Função para criar master playlist
create_master_playlist() {
    MASTER_PLAYLIST="$OUTPUT_DIR/master.m3u8"
    
    cat > "$MASTER_PLAYLIST" << EOF
#EXTM3U
#EXT-X-VERSION:3
#EXT-X-STREAM-INF:BANDWIDTH=5128000,RESOLUTION=1920x1080,CODECS="avc1.640028,mp4a.40.2"
${STREAM_KEY}-1080p.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=2928000,RESOLUTION=1280x720,CODECS="avc1.64001f,mp4a.40.2"
${STREAM_KEY}-720p.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=1528000,RESOLUTION=854x480,CODECS="avc1.64001e,mp4a.40.2"
${STREAM_KEY}-480p.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=928000,RESOLUTION=640x360,CODECS="avc1.64001e,mp4a.40.2"
${STREAM_KEY}-360p.m3u8
EOF

    echo "Master playlist criada: $MASTER_PLAYLIST"
}

# Trap para cleanup
trap cleanup SIGINT SIGTERM EXIT

# Aguardar o processo GStreamer
wait $GSTREAMER_PID

echo "$(date): Pipeline GStreamer finalizado para stream: $STREAM_KEY"
#!/bin/bash

# GStreamer Transcoding Script for HLS Multi-Bitrate Streaming
# This script is called by Nginx-RTMP when a stream starts
# Stream key must be passed as first argument

STREAM_KEY=$1

# Debug: Log all parameters
echo "[DEBUG] transcode-gstreamer.sh called with args: $@" >> /tmp/transcode-debug.log
echo "[DEBUG] STREAM_KEY: '$STREAM_KEY'" >> /tmp/transcode-debug.log
echo "[DEBUG] PWD: $(pwd)" >> /tmp/transcode-debug.log

if [ -z "$STREAM_KEY" ]; then
    echo "[ERROR] No stream key provided. Usage: $0 <stream_key>" >> /tmp/transcode-debug.log
    exit 1
fi

# Sanitize STREAM_KEY to prevent command injection
if ! [[ "$STREAM_KEY" =~ ^[a-zA-Z0-9_-]+$ ]]; then
    echo "[ERROR] Invalid stream key format. Only alphanumeric, underscore and hyphen allowed." >> /tmp/transcode-debug.log
    exit 1
fi

# Configuration
OUTPUT_DIR="/tmp/hls/${STREAM_KEY}"
INPUT_URL="rtmp://127.0.0.1/live/${STREAM_KEY}"
SEGMENT_DURATION=6
PLAYLIST_LENGTH=10

# Log file
LOG_FILE="${OUTPUT_DIR}/transcode.log"

# Create output directories
if ! mkdir -p "${OUTPUT_DIR}/v0" "${OUTPUT_DIR}/v1" "${OUTPUT_DIR}/v2" "${OUTPUT_DIR}/v3"; then
    echo "[ERROR] Failed to create output directories" >> /tmp/transcode-debug.log
    exit 1
fi

echo "[$(date)] Starting GStreamer transcoding for stream: ${STREAM_KEY}" >> "$LOG_FILE"
echo "[$(date)] INPUT_URL: ${INPUT_URL}" >> "$LOG_FILE"
echo "[$(date)] OUTPUT_DIR: ${OUTPUT_DIR}" >> "$LOG_FILE"

# Check available disk space (require at least 1GB free)
AVAILABLE_SPACE=$(df /tmp | tail -1 | awk '{print $4}')
REQUIRED_SPACE=1048576  # 1GB in KB
if [ "$AVAILABLE_SPACE" -lt "$REQUIRED_SPACE" ]; then
    echo "[$(date)] ERROR: Insufficient disk space. Available: ${AVAILABLE_SPACE}KB, Required: ${REQUIRED_SPACE}KB" >> "$LOG_FILE"
    exit 1
fi

# Setup cleanup trap for orphaned GStreamer processes
trap 'echo "[$(date)] Cleaning up GStreamer processes..." >> "$LOG_FILE"; kill $(jobs -p) 2>/dev/null; wait' EXIT SIGTERM SIGINT

# Write master playlist upfront so HLS player can start probing
cat > "${OUTPUT_DIR}/master.m3u8" << EOF
#EXTM3U
#EXT-X-VERSION:3
#EXT-X-STREAM-INF:BANDWIDTH=5128000,RESOLUTION=1920x1080,CODECS="avc1.640028,mp4a.40.2"
v0/playlist.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=2928000,RESOLUTION=1280x720,CODECS="avc1.64001f,mp4a.40.2"
v1/playlist.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=1496000,RESOLUTION=854x480,CODECS="avc1.64001e,mp4a.40.2"
v2/playlist.m3u8
#EXT-X-STREAM-INF:BANDWIDTH=896000,RESOLUTION=640x360,CODECS="avc1.64001e,mp4a.40.2"
v3/playlist.m3u8
EOF

echo "[$(date)] Master playlist written to ${OUTPUT_DIR}/master.m3u8" >> "$LOG_FILE"

# Wait briefly for stream to stabilize
echo "[$(date)] Waiting 3 seconds for stream to stabilize..." >> "$LOG_FILE"
sleep 3

echo "[$(date)] Starting GStreamer pipelines (4 quality levels in parallel)..." >> "$LOG_FILE"

# Quality definitions: "label width height video_bitrate audio_bitrate dir"
QUALITIES=(
    "1080p 1920 1080 5000 128000 v0"
    "720p  1280  720 2800 128000 v1"
    "480p   854  480 1400  96000 v2"
    "360p   640  360  800  96000 v3"
)

PIDS=()

for QUALITY in "${QUALITIES[@]}"; do
    read -r LABEL WIDTH HEIGHT VBITRATE ABITRATE DIR <<< "$QUALITY"

    echo "[$(date)] Launching pipeline: ${LABEL} (${WIDTH}x${HEIGHT} @ ${VBITRATE}kbps)" >> "$LOG_FILE"

    gst-launch-1.0 -e \
        rtmpsrc location="${INPUT_URL} live=1" \
        ! queue max-size-buffers=0 max-size-time=0 \
        ! flvdemux name=demux \
        demux.video \
        ! queue \
        ! h264parse \
        ! avdec_h264 \
        ! videoconvert \
        ! videoscale method=linear \
        ! "video/x-raw,width=${WIDTH},height=${HEIGHT}" \
        ! x264enc bitrate="${VBITRATE}" key-int-max=48 speed-preset=fast \
        ! h264parse \
        ! queue \
        ! mux. \
        demux.audio \
        ! queue \
        ! aacparse \
        ! avdec_aac \
        ! audioconvert \
        ! audioresample \
        ! "audio/x-raw,rate=44100,channels=2" \
        ! avenc_aac bitrate="${ABITRATE}" compliance=-2 \
        ! aacparse \
        ! queue \
        ! mux. \
        mpegtsmux name=mux \
        ! hlssink \
            location="${OUTPUT_DIR}/${DIR}/segment%05d.ts" \
            playlist-location="${OUTPUT_DIR}/${DIR}/playlist.m3u8" \
            target-duration="${SEGMENT_DURATION}" \
            max-files="${PLAYLIST_LENGTH}" \
        >> "$LOG_FILE" 2>&1 &

    PIDS+=($!)
    echo "[$(date)] Pipeline ${LABEL} started with PID ${PIDS[-1]}" >> "$LOG_FILE"
done

echo "[$(date)] All GStreamer pipelines started. PIDs: ${PIDS[*]}" >> "$LOG_FILE"

# Wait for all pipelines to finish
for PID in "${PIDS[@]}"; do
    wait "$PID"
    EXIT_CODE=$?
    echo "[$(date)] GStreamer pipeline PID ${PID} finished with exit code: ${EXIT_CODE}" >> "$LOG_FILE"
done

echo "[$(date)] All GStreamer pipelines finished" >> "$LOG_FILE"

if [ -f "${OUTPUT_DIR}/master.m3u8" ]; then
    echo "[$(date)] SUCCESS: Master playlist present at ${OUTPUT_DIR}/master.m3u8" >> "$LOG_FILE"
else
    echo "[$(date)] WARNING: Master playlist missing" >> "$LOG_FILE"
fi

echo "[$(date)] GStreamer transcoding script finished" >> "$LOG_FILE"

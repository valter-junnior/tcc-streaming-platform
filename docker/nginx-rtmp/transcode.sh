#!/bin/bash

# FFmpeg Transcoding Script for HLS Multi-Bitrate Streaming
# This script is called by Nginx-RTMP when a stream starts
# Stream key must be passed as first argument

STREAM_KEY=$1

# Debug: Log all parameters
echo "[DEBUG] transcode.sh called with args: $@" >> /tmp/transcode-debug.log
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

# Setup cleanup trap for orphaned FFmpeg processes
trap 'echo "[$(date)] Cleaning up FFmpeg processes..." >> "$LOG_FILE"; kill $(jobs -p) 2>/dev/null' EXIT SIGTERM SIGINT

# Create output directory
if ! mkdir -p "${OUTPUT_DIR}/v0" "${OUTPUT_DIR}/v1" "${OUTPUT_DIR}/v2" "${OUTPUT_DIR}/v3"; then
    echo "[ERROR] Failed to create output directories" >> /tmp/transcode-debug.log
    exit 1
fi

# Log file
LOG_FILE="${OUTPUT_DIR}/transcode.log"

echo "[$(date)] Starting transcoding for stream: ${STREAM_KEY}" >> "$LOG_FILE"
echo "[$(date)] INPUT_URL: ${INPUT_URL}" >> "$LOG_FILE"
echo "[$(date)] OUTPUT_DIR: ${OUTPUT_DIR}" >> "$LOG_FILE"

# Check available disk space (require at least 1GB free)
AVAILABLE_SPACE=$(df /tmp | tail -1 | awk '{print $4}')
REQUIRED_SPACE=1048576  # 1GB in KB
if [ "$AVAILABLE_SPACE" -lt "$REQUIRED_SPACE" ]; then
    echo "[$(date)] ERROR: Insufficient disk space. Available: ${AVAILABLE_SPACE}KB, Required: ${REQUIRED_SPACE}KB" >> "$LOG_FILE"
    exit 1
fi
echo "[$(date)] Disk space check passed. Available: ${AVAILABLE_SPACE}KB" >> "$LOG_FILE"

# Wait briefly for stream to stabilize
echo "[$(date)] Waiting 3 seconds for stream to stabilize..." >> "$LOG_FILE"
sleep 3

# Quick check if stream exists (with timeout)
echo "[$(date)] Verifying stream availability..." >> "$LOG_FILE"
if timeout 5 ffprobe -v error -analyzeduration 1000000 -probesize 1000000 -i "${INPUT_URL}" >/dev/null 2>&1; then
    echo "[$(date)] Stream verified successfully" >> "$LOG_FILE"
else
    echo "[$(date)] WARNING: Could not verify stream, but proceeding anyway..." >> "$LOG_FILE"
    echo "[$(date)] FFmpeg will handle connection itself" >> "$LOG_FILE"
fi

echo "[$(date)] Stream is ready, starting FFmpeg transcoding..." >> "$LOG_FILE"

# FFmpeg transcoding with 4 qualities for smooth adaptive bitrate
echo "[$(date)] Starting FFmpeg transcoding with 4 qualities (1080p, 720p, 480p, 360p)..." >> "$LOG_FILE"
echo "[$(date)] FFmpeg parameters:" >> "$LOG_FILE"
echo "[$(date)]   - Input: ${INPUT_URL}" >> "$LOG_FILE"
echo "[$(date)]   - Output: ${OUTPUT_DIR}/v%v/playlist.m3u8" >> "$LOG_FILE"
echo "[$(date)]   - Master playlist: ${OUTPUT_DIR}/master.m3u8" >> "$LOG_FILE"
echo "[$(date)]   - Preset: fast (better quality than veryfast)" >> "$LOG_FILE"

ffmpeg \
    -i "${INPUT_URL}" \
    -filter_complex \
    "[v:0]split=4[v0][v1][v2][v3]; \
     [v0]scale=w=1920:h=1080[v0out]; \
     [v1]scale=w=1280:h=720[v1out]; \
     [v2]scale=w=854:h=480[v2out]; \
     [v3]scale=w=640:h=360[v3out]" \
    -map "[v0out]" -c:v:0 libx264 -b:v:0 5000k -maxrate 5350k -bufsize 7500k -preset fast -map a:0? -c:a:0 aac -b:a:0 128k \
    -map "[v1out]" -c:v:1 libx264 -b:v:1 2800k -maxrate 2996k -bufsize 4200k -preset fast -map a:0? -c:a:1 aac -b:a:1 128k \
    -map "[v2out]" -c:v:2 libx264 -b:v:2 1400k -maxrate 1498k -bufsize 2100k -preset fast -map a:0? -c:a:2 aac -b:a:2 96k \
    -map "[v3out]" -c:v:3 libx264 -b:v:3 800k  -maxrate 856k  -bufsize 1200k -preset fast -map a:0? -c:a:3 aac -b:a:3 96k \
    -g 48 -keyint_min 48 -sc_threshold 0 \
    -f hls \
    -hls_time ${SEGMENT_DURATION} \
    -hls_list_size ${PLAYLIST_LENGTH} \
    -hls_flags delete_segments+append_list+independent_segments \
    -master_pl_name master.m3u8 \
    -var_stream_map "v:0,a:0 v:1,a:1 v:2,a:2 v:3,a:3" \
    -hls_segment_filename "${OUTPUT_DIR}/v%v/segment_%03d.ts" \
    "${OUTPUT_DIR}/v%v/playlist.m3u8" \
    >> "$LOG_FILE" 2>&1

EXIT_CODE=$?

echo "[$(date)] FFmpeg process finished" >> "$LOG_FILE"
echo "[$(date)] FFmpeg exit code: $EXIT_CODE" >> "$LOG_FILE"

if [ $EXIT_CODE -eq 0 ]; then
    echo "[$(date)] Transcoding completed successfully" >> "$LOG_FILE"
else
    echo "[$(date)] Transcoding failed with exit code: $EXIT_CODE" >> "$LOG_FILE"
    echo "[$(date)] Check FFmpeg output above for detailed error information" >> "$LOG_FILE"
fi

echo "[$(date)] Script execution finished" >> "$LOG_FILE"
exit $EXIT_CODE

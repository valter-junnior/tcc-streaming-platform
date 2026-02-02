#!/bin/bash

# FFmpeg Transcoding Script for HLS Multi-Bitrate Streaming
# This script is called by Nginx-RTMP when a stream starts

STREAM_KEY=$1
INPUT_URL=$2

if [ -z "$STREAM_KEY" ] || [ -z "$INPUT_URL" ]; then
    echo "Usage: $0 <stream_key> <input_url>"
    exit 1
fi

# Configuration
OUTPUT_DIR="/tmp/hls/${STREAM_KEY}"
SEGMENT_DURATION=6
PLAYLIST_LENGTH=10

# Create output directory
mkdir -p "${OUTPUT_DIR}/v0" "${OUTPUT_DIR}/v1" "${OUTPUT_DIR}/v2" "${OUTPUT_DIR}/v3"

# Log file
LOG_FILE="${OUTPUT_DIR}/transcode.log"

echo "[$(date)] Starting transcoding for stream: ${STREAM_KEY}" >> "$LOG_FILE"

# FFmpeg command with multiple quality variants
ffmpeg -i "${INPUT_URL}" \
    -filter_complex \
    "[v:0]split=4[v0][v1][v2][v3]; \
     [v0]scale=w=1920:h=1080[v0out]; \
     [v1]scale=w=1280:h=720[v1out]; \
     [v2]scale=w=854:h=480[v2out]; \
     [v3]scale=w=640:h=360[v3out]" \
    -map "[v0out]" -c:v:0 libx264 -b:v:0 5000k -preset medium -map a:0 -c:a:0 aac -b:a:0 128k \
    -map "[v1out]" -c:v:1 libx264 -b:v:1 2800k -preset medium -map a:0 -c:a:1 aac -b:a:1 128k \
    -map "[v2out]" -c:v:2 libx264 -b:v:2 1400k -preset fast -map a:0 -c:a:2 aac -b:a:2 128k \
    -map "[v3out]" -c:v:3 libx264 -b:v:3 800k -preset faster -map a:0 -c:a:3 aac -b:a:3 96k \
    -g 48 -keyint_min 48 -sc_threshold 0 \
    -f hls \
    -hls_time ${SEGMENT_DURATION} \
    -hls_list_size ${PLAYLIST_LENGTH} \
    -hls_flags delete_segments+append_list \
    -master_pl_name master.m3u8 \
    -var_stream_map "v:0,a:0 v:1,a:1 v:2,a:2 v:3,a:3" \
    -hls_segment_filename "${OUTPUT_DIR}/v%v/segment_%03d.ts" \
    "${OUTPUT_DIR}/v%v/playlist.m3u8" \
    >> "$LOG_FILE" 2>&1

EXIT_CODE=$?

if [ $EXIT_CODE -eq 0 ]; then
    echo "[$(date)] Transcoding completed successfully" >> "$LOG_FILE"
else
    echo "[$(date)] Transcoding failed with exit code: $EXIT_CODE" >> "$LOG_FILE"
fi

exit $EXIT_CODE

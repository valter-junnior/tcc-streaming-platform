#!/bin/bash
set -e

# Default environment variables
export RTMP_PORT="${RTMP_PORT:-1935}"
export RTMP_CHUNK_SIZE="${RTMP_CHUNK_SIZE:-4096}"
export HLS_PATH="${HLS_PATH:-/tmp/hls}"
export BACKEND_HOST="${BACKEND_HOST:-streaming-platform}"
export BACKEND_PORT="${BACKEND_PORT:-8080}"
export HLS_HTTP_PORT="${HLS_HTTP_PORT:-8081}"
export HLS_RETENTION_HOURS="${HLS_RETENTION_HOURS:-6}"
export TRANSCODER="${TRANSCODER:-ffmpeg}"

echo "Starting RTMP server: RTMP=$RTMP_PORT, HLS=$HLS_HTTP_PORT, TRANSCODER=$TRANSCODER"

# Select the transcoding script based on TRANSCODER env var
# nginx.conf always calls /usr/local/bin/transcode.sh — the symlink points to the active script
case "${TRANSCODER,,}" in
    gstreamer)
        echo "Transcoder selected: GStreamer"
        ln -sf /usr/local/bin/transcode-gstreamer.sh /usr/local/bin/transcode.sh
        ;;
    *)
        echo "Transcoder selected: FFmpeg (default)"
        ln -sf /usr/local/bin/transcode-ffmpeg.sh /usr/local/bin/transcode.sh
        ;;
esac

# Replace environment variables in nginx configuration
echo "Generating nginx.conf from template..."
envsubst '${RTMP_PORT} ${RTMP_CHUNK_SIZE} ${HLS_PATH} ${BACKEND_HOST} ${BACKEND_PORT} ${HLS_HTTP_PORT}' \
    < /usr/local/nginx/conf/nginx.conf.template \
    > /usr/local/nginx/conf/nginx.conf

echo "nginx.conf generated successfully"

# Function to wait for backend to be available
wait_for_backend() {
    echo "Waiting for backend (${BACKEND_HOST}:${BACKEND_PORT}) to be available..."
    
    # Try to resolve the hostname first
    while ! getent hosts "${BACKEND_HOST}" > /dev/null 2>&1; do
        echo "Waiting for ${BACKEND_HOST} hostname to resolve..."
        sleep 2
    done
    
    echo "${BACKEND_HOST} hostname resolved successfully"
    
    # Now wait for the actual service to be ready
    while ! nc -z "${BACKEND_HOST}" "${BACKEND_PORT}"; do
        echo "Waiting for ${BACKEND_HOST}:${BACKEND_PORT} to be ready..."
        sleep 2
    done
    
    echo "${BACKEND_HOST}:${BACKEND_PORT} is ready!"
}

# Wait for backend to be available
wait_for_backend

# Start cron service
service cron start

# Start nginx in foreground
exec /usr/local/nginx/sbin/nginx -g "daemon off;"
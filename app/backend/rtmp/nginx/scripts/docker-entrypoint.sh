#!/bin/bash
set -e

# Default environment variables
export RTMP_PORT="${RTMP_PORT:-1935}"
export RTMP_CHUNK_SIZE="${RTMP_CHUNK_SIZE:-4096}"
export HLS_PATH="${HLS_PATH:-/tmp/hls}"
export HLS_FRAGMENT_DURATION="${HLS_FRAGMENT_DURATION:-6s}"
export HLS_PLAYLIST_LENGTH="${HLS_PLAYLIST_LENGTH:-60s}"
export BACKEND_HOST="${BACKEND_HOST:-streaming-platform}"
export BACKEND_PORT="${BACKEND_PORT:-8080}"
export HLS_HTTP_PORT="${HLS_HTTP_PORT:-8081}"
export TRANSCODING_ENGINE="${TRANSCODING_ENGINE:-ffmpeg}"
export HLS_RETENTION_HOURS="${HLS_RETENTION_HOURS:-6}"

echo "=== RTMP Server Configuration ==="
echo "RTMP_PORT: $RTMP_PORT"
echo "RTMP_CHUNK_SIZE: $RTMP_CHUNK_SIZE"
echo "HLS_PATH: $HLS_PATH"
echo "HLS_FRAGMENT_DURATION: $HLS_FRAGMENT_DURATION"
echo "HLS_PLAYLIST_LENGTH: $HLS_PLAYLIST_LENGTH"
echo "BACKEND_HOST: $BACKEND_HOST"
echo "BACKEND_PORT: $BACKEND_PORT"
echo "HLS_HTTP_PORT: $HLS_HTTP_PORT"
echo "TRANSCODING_ENGINE: $TRANSCODING_ENGINE"
echo "HLS_RETENTION_HOURS: $HLS_RETENTION_HOURS"
echo "================================="

# Replace environment variables in nginx configuration
echo "Generating nginx.conf from template..."
envsubst '${RTMP_PORT} ${RTMP_CHUNK_SIZE} ${HLS_PATH} ${HLS_FRAGMENT_DURATION} ${HLS_PLAYLIST_LENGTH} ${BACKEND_HOST} ${BACKEND_PORT} ${HLS_HTTP_PORT}' \
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
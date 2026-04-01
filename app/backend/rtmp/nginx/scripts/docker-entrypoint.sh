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
export HLS_SEGMENT_DURATION="${HLS_SEGMENT_DURATION:-6}"
export HLS_PLAYLIST_LENGTH="${HLS_PLAYLIST_LENGTH:-10}"
export TRANSCODER="${TRANSCODER:-ffmpeg}"
export TRANSCODER_MAX_RETRIES="${TRANSCODER_MAX_RETRIES:-10}"
export TRANSCODER_RETRY_WAIT="${TRANSCODER_RETRY_WAIT:-3}"
export TRANSCODER_STARTUP_TIMEOUT="${TRANSCODER_STARTUP_TIMEOUT:-20}"
export TRANSCODER_STREAM_STABILIZE_SECONDS="${TRANSCODER_STREAM_STABILIZE_SECONDS:-3}"
export TRANSCODER_STREAM_END_DURATION_SECONDS="${TRANSCODER_STREAM_END_DURATION_SECONDS:-30}"

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

# Ensure persisted HLS volume is writable by nginx workers (user: nobody).
mkdir -p "${HLS_PATH}"
chown -R nobody:nogroup "${HLS_PATH}" 2>/dev/null || true
find "${HLS_PATH}" -type d -exec chmod 0777 {} + 2>/dev/null || true
find "${HLS_PATH}" -type f -exec chmod 0666 {} + 2>/dev/null || true
echo "HLS path normalized: ${HLS_PATH}"

# Function to wait for backend to be available
# Exits after BACKEND_WAIT_TIMEOUT seconds (default: 120) to avoid hanging forever.
wait_for_backend() {
    local TIMEOUT="${BACKEND_WAIT_TIMEOUT:-120}"
    local ELAPSED=0

    echo "Waiting for backend (${BACKEND_HOST}:${BACKEND_PORT}) — timeout: ${TIMEOUT}s"

    # Wait for hostname to resolve
    while ! getent hosts "${BACKEND_HOST}" > /dev/null 2>&1; do
        if [ "$ELAPSED" -ge "$TIMEOUT" ]; then
            echo "[ERROR] Timed out waiting for ${BACKEND_HOST} to resolve after ${TIMEOUT}s" >&2
            exit 1
        fi
        echo "Waiting for ${BACKEND_HOST} hostname to resolve... (${ELAPSED}s)"
        sleep 2
        ELAPSED=$(( ELAPSED + 2 ))
    done

    echo "${BACKEND_HOST} hostname resolved"

    # Wait for the port to be reachable
    while ! nc -z "${BACKEND_HOST}" "${BACKEND_PORT}"; do
        if [ "$ELAPSED" -ge "$TIMEOUT" ]; then
            echo "[ERROR] Timed out waiting for ${BACKEND_HOST}:${BACKEND_PORT} after ${TIMEOUT}s" >&2
            exit 1
        fi
        echo "Waiting for ${BACKEND_HOST}:${BACKEND_PORT} to be ready... (${ELAPSED}s)"
        sleep 2
        ELAPSED=$(( ELAPSED + 2 ))
    done

    echo "${BACKEND_HOST}:${BACKEND_PORT} is ready!"
}

# Wait for backend to be available
wait_for_backend

# Start cron service
service cron start

# Start nginx in foreground
exec /usr/local/nginx/sbin/nginx -g "daemon off;"
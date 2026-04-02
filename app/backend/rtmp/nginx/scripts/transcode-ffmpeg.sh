#!/bin/bash
set -u

STREAM_KEY=$1

if [ -z "$STREAM_KEY" ]; then
    echo "[ERROR] No stream key provided. Usage: $0 <stream_key>" >&2
    exit 1
fi

# Sanitize STREAM_KEY to prevent command injection
if ! [[ "$STREAM_KEY" =~ ^[a-zA-Z0-9_-]+$ ]]; then
    echo "[ERROR] Invalid stream key format. Only alphanumeric, underscore and hyphen allowed." >&2
    exit 1
fi

OUTPUT_DIR="/tmp/hls/${STREAM_KEY}"
INPUT_URL="rtmp://127.0.0.1/live/${STREAM_KEY}"
SEGMENT_DURATION="${HLS_SEGMENT_DURATION:-6}"
PLAYLIST_LENGTH="${HLS_PLAYLIST_LENGTH:-10}"
MAX_RETRIES="${TRANSCODER_MAX_RETRIES:-10}"
RETRY_WAIT="${TRANSCODER_RETRY_WAIT:-3}"
STREAM_STABILIZE_SECONDS="${TRANSCODER_STREAM_STABILIZE_SECONDS:-3}"
STREAM_END_DURATION_SECONDS="${TRANSCODER_STREAM_END_DURATION_SECONDS:-30}"
RTMP_STAT_URL="http://127.0.0.1:${HLS_HTTP_PORT:-8081}/stat"
BACKEND_LIVE_URL="${STREAM_BACKEND_LIVE_URL:-http://streaming-platform:8080/api/streams/live}"
INPUT_PROBE_TIMEOUT_SECONDS="${TRANSCODER_INPUT_PROBE_TIMEOUT_SECONDS:-3}"

if ! [[ "$SEGMENT_DURATION" =~ ^[0-9]+$ ]] || [ "$SEGMENT_DURATION" -le 0 ]; then
    SEGMENT_DURATION=6
fi
if ! [[ "$PLAYLIST_LENGTH" =~ ^[0-9]+$ ]] || [ "$PLAYLIST_LENGTH" -le 0 ]; then
    PLAYLIST_LENGTH=10
fi
if ! [[ "$MAX_RETRIES" =~ ^[0-9]+$ ]] || [ "$MAX_RETRIES" -le 0 ]; then
    MAX_RETRIES=10
fi
if ! [[ "$RETRY_WAIT" =~ ^[0-9]+$ ]] || [ "$RETRY_WAIT" -lt 0 ]; then
    RETRY_WAIT=3
fi
if ! [[ "$STREAM_STABILIZE_SECONDS" =~ ^[0-9]+$ ]] || [ "$STREAM_STABILIZE_SECONDS" -lt 0 ]; then
    STREAM_STABILIZE_SECONDS=3
fi
if ! [[ "$STREAM_END_DURATION_SECONDS" =~ ^[0-9]+$ ]] || [ "$STREAM_END_DURATION_SECONDS" -le 0 ]; then
    STREAM_END_DURATION_SECONDS=30
fi
if ! [[ "$INPUT_PROBE_TIMEOUT_SECONDS" =~ ^[0-9]+$ ]] || [ "$INPUT_PROBE_TIMEOUT_SECONDS" -le 0 ]; then
    INPUT_PROBE_TIMEOUT_SECONDS=3
fi

# Create output directory
if ! mkdir -p "${OUTPUT_DIR}/v0" "${OUTPUT_DIR}/v1" "${OUTPUT_DIR}/v2" "${OUTPUT_DIR}/v3"; then
    echo "[ERROR] Failed to create output directories for stream: ${STREAM_KEY}" >&2
    exit 1
fi

LOG_FILE="${OUTPUT_DIR}/transcode.log"
LOCK_FILE="${OUTPUT_DIR}/transcode.lock"

# Avoid concurrent ffmpeg instances for the same stream key (common on quick reconnects).
# Uses a blocking wait so that a resume after pause waits for the previous instance to finish
# (which exits after STREAM_END_DURATION_SECONDS) instead of failing immediately.
exec 9>"${LOCK_FILE}"
if ! flock -w 45 9; then
    echo "[$(date)] Timed out waiting for lock - another FFmpeg transcoder may be stuck for stream: ${STREAM_KEY}" >> "$LOG_FILE"
    exit 1
fi

# Setup cleanup trap for orphaned FFmpeg processes
trap 'echo "[$(date)] Cleaning up FFmpeg processes..." >> "$LOG_FILE"; kill $(jobs -p) 2>/dev/null' EXIT SIGTERM SIGINT

is_stream_active() {
    # Primary detection: authoritative backend status (stream marked LIVE).
    if wget -q -O - "$BACKEND_LIVE_URL" 2>/dev/null | tr -d '\n' | grep -q "\"streamKey\":\"${STREAM_KEY}\""; then
        return 0
    fi

    # Primary detection: probe the actual RTMP input URL for this stream key.
    # This is resilient even when /stat omits per-stream nodes.
    if command -v ffprobe >/dev/null 2>&1; then
        if timeout "${INPUT_PROBE_TIMEOUT_SECONDS}" ffprobe \
            -v error \
            -rw_timeout 3000000 \
            -i "${INPUT_URL}" \
            -show_entries format=format_name \
            -of default=nokey=1:noprint_wrappers=1 \
            >/dev/null 2>&1; then
            return 0
        fi
    fi

    # Fallback: parse RTMP /stat XML when ffprobe is unavailable or times out.
    wget -q -O - "$RTMP_STAT_URL" 2>/dev/null | awk -v key="$STREAM_KEY" '
        /<stream>/ { in_stream=1; name=""; publishing=0; clients=0 }
        /<name>/ && in_stream {
            line=$0
            gsub(/^.*<name>/, "", line)
            gsub(/<\/name>.*$/, "", line)
            name=line
        }
        /<publishing\/>/ && in_stream { publishing=1 }
        /<publishing>[[:space:]]*1[[:space:]]*<\/publishing>/ && in_stream { publishing=1 }
        /<active\/>/ && in_stream { publishing=1 }
        /<nclients>/ && in_stream {
            line=$0
            gsub(/^.*<nclients>/, "", line)
            gsub(/<\/nclients>.*$/, "", line)
            clients=line+0
        }
        /<\/stream>/ {
            if (in_stream && name==key && (publishing==1 || clients>0)) {
                found=1
                exit
            }
            in_stream=0
        }
        END { exit(found ? 0 : 1) }
    '
}

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

# Wait for stream to be established by the publisher (keyframe buffering)
echo "[$(date)] Waiting ${STREAM_STABILIZE_SECONDS}s for stream to stabilize..." >> "$LOG_FILE"
sleep "$STREAM_STABILIZE_SECONDS"

# Retry loop: handles its own retries instead of relying on nginx-rtmp respawn.
# nginx-rtmp respawns exec on non-zero exit, creating a slow 23s-per-cycle loop.
# By retrying internally we go from >2min delay to <30s on first success.
ATTEMPT=0
EXIT_CODE=1

while [ $ATTEMPT -lt $MAX_RETRIES ]; do
    ATTEMPT=$((ATTEMPT + 1))
    ATTEMPT_STARTED_AT=$(date +%s)
    echo "[$(date)] FFmpeg attempt $ATTEMPT of $MAX_RETRIES — connecting to ${INPUT_URL}..." >> "$LOG_FILE"

    ffmpeg \
        -loglevel warning \
        -rtmp_live live \
        -rw_timeout 15000000 \
        -i "${INPUT_URL}" \
        -filter_complex \
        "[v:0]split=4[v0][v1][v2][v3]; \
         [v0]scale=w=1920:h=1080[v0out]; \
         [v1]scale=w=1280:h=720[v1out]; \
         [v2]scale=w=854:h=480[v2out]; \
         [v3]scale=w=640:h=360[v3out]" \
        -map "[v0out]" -c:v:0 libx264 -b:v:0 5000k -maxrate:v:0 5350k -bufsize:v:0 7500k -preset fast -map a:0? -c:a:0 aac -b:a:0 128k \
        -map "[v1out]" -c:v:1 libx264 -b:v:1 2800k -maxrate:v:1 2996k -bufsize:v:1 4200k -preset fast -map a:0? -c:a:1 aac -b:a:1 128k \
        -map "[v2out]" -c:v:2 libx264 -b:v:2 1400k -maxrate:v:2 1498k -bufsize:v:2 2100k -preset fast -map a:0? -c:a:2 aac -b:a:2 96k \
        -map "[v3out]" -c:v:3 libx264 -b:v:3 800k  -maxrate:v:3 856k  -bufsize:v:3 1200k -preset fast -map a:0? -c:a:3 aac -b:a:3 96k \
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
    ATTEMPT_DURATION=$(( $(date +%s) - ATTEMPT_STARTED_AT ))
    echo "[$(date)] FFmpeg attempt $ATTEMPT finished with exit code: $EXIT_CODE" >> "$LOG_FILE"
    echo "[$(date)] FFmpeg attempt $ATTEMPT duration: ${ATTEMPT_DURATION}s" >> "$LOG_FILE"

    # Exit code 0 means stream ended normally — no retry needed
    if [ $EXIT_CODE -eq 0 ]; then
        echo "[$(date)] Transcoding completed successfully" >> "$LOG_FILE"
        break
    fi

    # If FFmpeg ran for a while before exiting, this is usually a normal stream end
    # (or publisher disconnect), not a startup failure. Avoid retry loops in this case.
    if [ "$ATTEMPT_DURATION" -ge "$STREAM_END_DURATION_SECONDS" ]; then
        echo "[$(date)] FFmpeg ran for ${ATTEMPT_DURATION}s before exit; treating as stream ended and stopping retries." >> "$LOG_FILE"
        break
    fi

    if [ $ATTEMPT -lt $MAX_RETRIES ]; then
        echo "[$(date)] FFmpeg failed, retrying in ${RETRY_WAIT}s..." >> "$LOG_FILE"
        sleep $RETRY_WAIT
    else
        echo "[$(date)] ERROR: All $MAX_RETRIES attempts failed. Giving up." >> "$LOG_FILE"
    fi
done

# Check if any files were generated
if [ -f "${OUTPUT_DIR}/master.m3u8" ]; then
    echo "[$(date)] SUCCESS: Master playlist generated" >> "$LOG_FILE"
else
    echo "[$(date)] WARNING: Master playlist NOT generated - FFmpeg did not produce output" >> "$LOG_FILE"
fi

echo "[$(date)] Script execution finished" >> "$LOG_FILE"
# Always exit 0 to prevent nginx-rtmp from respawning this script.
# Retries are handled internally by the loop above.
exit 0

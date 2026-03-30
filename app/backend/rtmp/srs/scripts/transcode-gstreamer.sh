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

HLS_BASE_PATH="${HLS_PATH:-/tmp/hls}"
OUTPUT_DIR="${HLS_BASE_PATH}/${STREAM_KEY}"
INPUT_URL="rtmp://127.0.0.1/live/${STREAM_KEY}"
SEGMENT_DURATION="${HLS_SEGMENT_DURATION:-6}"
PLAYLIST_LENGTH="${HLS_PLAYLIST_LENGTH:-10}"
MAX_RETRIES="${TRANSCODER_MAX_RETRIES:-10}"
RETRY_WAIT="${TRANSCODER_RETRY_WAIT:-3}"
STARTUP_TIMEOUT="${TRANSCODER_STARTUP_TIMEOUT:-20}"
STREAM_STABILIZE_SECONDS="${TRANSCODER_STREAM_STABILIZE_SECONDS:-8}"
RTMP_STAT_URL="http://127.0.0.1:${HLS_HTTP_PORT:-8081}/api/v1/streams"
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
if ! [[ "$STARTUP_TIMEOUT" =~ ^[0-9]+$ ]] || [ "$STARTUP_TIMEOUT" -le 0 ]; then
    STARTUP_TIMEOUT=20
fi
if ! [[ "$STREAM_STABILIZE_SECONDS" =~ ^[0-9]+$ ]] || [ "$STREAM_STABILIZE_SECONDS" -lt 0 ]; then
    STREAM_STABILIZE_SECONDS=8
fi
if ! [[ "$INPUT_PROBE_TIMEOUT_SECONDS" =~ ^[0-9]+$ ]] || [ "$INPUT_PROBE_TIMEOUT_SECONDS" -le 0 ]; then
    INPUT_PROBE_TIMEOUT_SECONDS=3
fi

# Create output directory structure for stable GStreamer renditions.
if ! mkdir -p "${OUTPUT_DIR}/v0" "${OUTPUT_DIR}/v2"; then
    echo "[ERROR] Failed to create output directories for stream: ${STREAM_KEY}" >&2
    exit 1
fi

LOG_FILE="${OUTPUT_DIR}/transcode.log"
LOCK_FILE="${OUTPUT_DIR}/transcode.lock"

exec 9>"${LOCK_FILE}"
if ! flock -n 9; then
    echo "[$(date)] Another GStreamer transcoder is already running for stream: ${STREAM_KEY}" >> "$LOG_FILE"
    exit 0
fi

# Keep track of children so we can terminate all variant pipelines on exit/retry.
PIDS=()
cleanup_children() {
    if [ ${#PIDS[@]} -gt 0 ]; then
        kill "${PIDS[@]}" 2>/dev/null || true
        wait "${PIDS[@]}" 2>/dev/null || true
    fi
}

trap 'echo "[$(date)] Cleaning up GStreamer processes..." >> "$LOG_FILE"; cleanup_children' EXIT SIGTERM SIGINT

probe_input_ready() {
    if ! command -v ffprobe >/dev/null 2>&1; then
        return 1
    fi

    timeout "${INPUT_PROBE_TIMEOUT_SECONDS}" ffprobe \
        -v error \
        -rw_timeout 3000000 \
        -i "${INPUT_URL}" \
        -show_entries format=format_name \
        -of default=nokey=1:noprint_wrappers=1 \
        >/dev/null 2>&1
}

is_stream_active() {
    # Primary detection: authoritative backend status (stream marked LIVE).
    if wget -q -O - "$BACKEND_LIVE_URL" 2>/dev/null | tr -d '\n' | grep -q "\"streamKey\":\"${STREAM_KEY}\""; then
        return 0
    fi

    # Primary detection: probe the actual RTMP input URL for this stream key.
    # This is resilient even when RTMP stats endpoint is unavailable.
    if probe_input_ready; then
        return 0
    fi

    # Fallback: parse SRS streams API when ffprobe is unavailable or times out.
    wget -q -O - "$RTMP_STAT_URL" 2>/dev/null | tr -d '[:space:]' | grep -q "\"name\":\"${STREAM_KEY}\""
}

wait_for_input_ready() {
    local TIMEOUT_SECONDS=$1
    local ELAPSED=0

    echo "[$(date)] Waiting up to ${TIMEOUT_SECONDS}s for RTMP input to become readable..." >> "$LOG_FILE"

    while [ "$ELAPSED" -lt "$TIMEOUT_SECONDS" ]; do
        if probe_input_ready; then
            echo "[$(date)] RTMP input became readable after ${ELAPSED}s" >> "$LOG_FILE"
            return 0
        fi

        if ! is_stream_active; then
            echo "[$(date)] No active publisher detected while waiting for RTMP input" >> "$LOG_FILE"
            return 1
        fi

        sleep 1
        ELAPSED=$((ELAPSED + 1))
    done

    echo "[$(date)] Timed out waiting ${TIMEOUT_SECONDS}s for readable RTMP input" >> "$LOG_FILE"
    return 1
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

# Pre-generate the master playlist.
# Structure mirrors what FFmpeg generates so the frontend needs no changes.
cat > "${OUTPUT_DIR}/master.m3u8" << 'MASTER_EOF'
#EXTM3U
#EXT-X-VERSION:3

#EXT-X-STREAM-INF:BANDWIDTH=5128000,RESOLUTION=1920x1080
v0/playlist.m3u8

#EXT-X-STREAM-INF:BANDWIDTH=1496000,RESOLUTION=854x480
v2/playlist.m3u8
MASTER_EOF

echo "[$(date)] Master playlist generated at ${OUTPUT_DIR}/master.m3u8" >> "$LOG_FILE"

# Run a single-quality GStreamer pipeline using hlssink2.
# This keeps playlist generation inside GStreamer (no external manager needed),
# which is more reliable than manual multifilesink polling for this workload.
#
# Arguments: <variant_index> <width> <height> <video_bitrate_kbps> <audio_bitrate_bps>
run_variant() {
    local VARIANT=$1
    local WIDTH=$2
    local HEIGHT=$3
    local VBITRATE=$4
    local ABITRATE=$5
    local OUT="${OUTPUT_DIR}/v${VARIANT}"

    rm -f "${OUT}"/segment_*.ts "${OUT}"/playlist.m3u8 2>/dev/null

    gst-launch-1.0 -e \
        rtmpsrc location="${INPUT_URL} live=1" ! \
        flvdemux name=demux \
        "demux.video" ! queue max-size-time=3000000000 max-size-buffers=0 max-size-bytes=0 ! \
            h264parse ! avdec_h264 ! videoconvert ! videoscale ! \
            "video/x-raw,width=${WIDTH},height=${HEIGHT}" ! \
            x264enc bitrate=${VBITRATE} speed-preset=5 key-int-max=48 bframes=0 tune=zerolatency ! \
            h264parse config-interval=1 ! \
            hls.video \
        "demux.audio" ! queue max-size-time=3000000000 max-size-buffers=0 max-size-bytes=0 ! \
            aacparse ! avdec_aac ! audioconvert ! audioresample ! \
            voaacenc bitrate=${ABITRATE} ! aacparse ! \
            hls.audio \
        hlssink2 name=hls \
            target-duration=${SEGMENT_DURATION} \
            playlist-length=${PLAYLIST_LENGTH} \
            max-files=${PLAYLIST_LENGTH} \
            location="${OUT}/segment_%05d.ts" \
            playlist-location="${OUT}/playlist.m3u8" \
        >> "$LOG_FILE" 2>&1
}

# Wait for stream to be established by the publisher before launching GStreamer.
if ! wait_for_input_ready "$STARTUP_TIMEOUT"; then
    echo "[$(date)] WARNING: RTMP input never became readable; aborting GStreamer launch" >> "$LOG_FILE"
    echo "[$(date)] Script execution finished" >> "$LOG_FILE"
    exit 0
fi

if [ "$STREAM_STABILIZE_SECONDS" -gt 0 ]; then
    echo "[$(date)] Waiting ${STREAM_STABILIZE_SECONDS}s for stream to stabilize after input detection..." >> "$LOG_FILE"
    sleep "$STREAM_STABILIZE_SECONDS"
fi

# Retry loop with bootstrap validation.
# If variant playlists are not created quickly, kill pipelines and retry.
ATTEMPT=0
SCRIPT_STARTED_AT=$(date +%s)
MAX_TOTAL_RUNTIME=$(((STARTUP_TIMEOUT * MAX_RETRIES) + (RETRY_WAIT * (MAX_RETRIES - 1))))

while [ $ATTEMPT -lt $MAX_RETRIES ]; do
    ATTEMPT=$((ATTEMPT + 1))
    ATTEMPT_STARTED_AT=$(date +%s)
    echo "[$(date)] GStreamer attempt $ATTEMPT of $MAX_RETRIES - launching 2 variant pipelines..." >> "$LOG_FILE"

    run_variant 0 1920 1080 5000 128000 & PIDS[0]=$!
    run_variant 2 854  480  1400 96000  & PIDS[1]=$!

    BOOTSTRAP_OK=false
    for _ in $(seq 1 "$STARTUP_TIMEOUT"); do
        if [ -f "${OUTPUT_DIR}/v0/playlist.m3u8" ] || [ -f "${OUTPUT_DIR}/v2/playlist.m3u8" ]; then
            BOOTSTRAP_OK=true
            break
        fi
        sleep 1
    done

    if ! $BOOTSTRAP_OK; then
        TOTAL_RUNTIME=$(( $(date +%s) - SCRIPT_STARTED_AT ))
        echo "[$(date)] Attempt $ATTEMPT failed bootstrap: no variant playlist created after ${STARTUP_TIMEOUT}s" >> "$LOG_FILE"
        cleanup_children

        if [ "$TOTAL_RUNTIME" -ge "$MAX_TOTAL_RUNTIME" ]; then
            echo "[$(date)] Retry budget exhausted after bootstrap failure (total runtime: ${TOTAL_RUNTIME}s)." >> "$LOG_FILE"
            break
        fi

        if ! is_stream_active; then
            echo "[$(date)] No active publisher for stream ${STREAM_KEY} after bootstrap failure; stopping retries." >> "$LOG_FILE"
            break
        fi

        if [ $ATTEMPT -lt $MAX_RETRIES ]; then
            echo "[$(date)] Stream is still active after bootstrap failure; retrying in ${RETRY_WAIT}s..." >> "$LOG_FILE"
            sleep "$RETRY_WAIT"
            continue
        fi

        echo "[$(date)] ERROR: All $MAX_RETRIES attempts failed bootstrap. Giving up." >> "$LOG_FILE"
        break
    fi

    echo "[$(date)] Bootstrap succeeded, waiting pipelines to finish..." >> "$LOG_FILE"

    ALL_OK=true
    for i in 0 1; do
        wait "${PIDS[$i]}"
        EXIT_CODES[$i]=$?
        if [ "$i" -eq 0 ]; then
            VARIANT_NAME="v0"
        else
            VARIANT_NAME="v2"
        fi
        echo "[$(date)] Variant ${VARIANT_NAME} exited with code: ${EXIT_CODES[$i]}" >> "$LOG_FILE"
        if [ "${EXIT_CODES[$i]}" -ne 0 ] && [ "${EXIT_CODES[$i]}" -ne 143 ]; then
            ALL_OK=false
        fi
    done

    if $ALL_OK; then
        echo "[$(date)] Transcoding completed successfully" >> "$LOG_FILE"
        break
    fi

    ATTEMPT_DURATION=$(( $(date +%s) - ATTEMPT_STARTED_AT ))
    echo "[$(date)] GStreamer attempt $ATTEMPT duration: ${ATTEMPT_DURATION}s" >> "$LOG_FILE"

    # If pipelines ran for a while before exiting non-zero, this is usually a normal
    # stream end/disconnect rather than startup failure. Avoid retry loops in this case.
    if [ "$ATTEMPT_DURATION" -ge "$STREAM_STABILIZE_SECONDS" ]; then
        echo "[$(date)] GStreamer ran for ${ATTEMPT_DURATION}s before exit; treating as stream ended and stopping retries." >> "$LOG_FILE"
        break
    fi

    cleanup_children

    if [ $ATTEMPT -lt $MAX_RETRIES ]; then
        TOTAL_RUNTIME=$(( $(date +%s) - SCRIPT_STARTED_AT ))
        if [ "$TOTAL_RUNTIME" -ge "$MAX_TOTAL_RUNTIME" ]; then
            echo "[$(date)] Retry budget exhausted after ${TOTAL_RUNTIME}s; stopping retries." >> "$LOG_FILE"
            break
        fi
        if ! is_stream_active; then
            echo "[$(date)] No active publisher for stream ${STREAM_KEY}; stopping retries." >> "$LOG_FILE"
            break
        fi
        echo "[$(date)] One or more variants failed, retrying in ${RETRY_WAIT}s..." >> "$LOG_FILE"
        sleep $RETRY_WAIT
    else
        echo "[$(date)] ERROR: All $MAX_RETRIES attempts failed. Giving up." >> "$LOG_FILE"
    fi
done

if [ -f "${OUTPUT_DIR}/v0/playlist.m3u8" ]; then
    echo "[$(date)] SUCCESS: HLS playlists generated" >> "$LOG_FILE"
else
    echo "[$(date)] WARNING: HLS playlists NOT generated - GStreamer did not produce output" >> "$LOG_FILE"
fi

echo "[$(date)] Script execution finished" >> "$LOG_FILE"
# Always exit 0 to avoid restart loops by SRS exec hooks.
exit 0

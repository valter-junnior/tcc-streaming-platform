#!/bin/bash

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
SEGMENT_DURATION=6
PLAYLIST_LENGTH=10

# Create output directory structure for stable GStreamer renditions.
if ! mkdir -p "${OUTPUT_DIR}/v0" "${OUTPUT_DIR}/v2"; then
    echo "[ERROR] Failed to create output directories for stream: ${STREAM_KEY}" >&2
    exit 1
fi

LOG_FILE="${OUTPUT_DIR}/transcode.log"

# Keep track of children so we can terminate all variant pipelines on exit/retry.
PIDS=()
cleanup_children() {
    if [ ${#PIDS[@]} -gt 0 ]; then
        kill "${PIDS[@]}" 2>/dev/null || true
        wait "${PIDS[@]}" 2>/dev/null || true
    fi
}

trap 'echo "[$(date)] Cleaning up GStreamer processes..." >> "$LOG_FILE"; cleanup_children' EXIT SIGTERM SIGINT

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

# Wait for stream to be established by the publisher (keyframe buffering)
echo "[$(date)] Waiting 8 seconds for stream to stabilize..." >> "$LOG_FILE"
sleep 8

# Retry loop with bootstrap validation.
# If variant playlists are not created quickly, kill pipelines and retry.
MAX_RETRIES=10
RETRY_WAIT=3
STARTUP_TIMEOUT=20
ATTEMPT=0
EXIT_CODE=1

while [ $ATTEMPT -lt $MAX_RETRIES ]; do
    ATTEMPT=$((ATTEMPT + 1))
    echo "[$(date)] GStreamer attempt $ATTEMPT of $MAX_RETRIES — launching 4 variant pipelines..." >> "$LOG_FILE"

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
        echo "[$(date)] Attempt $ATTEMPT failed bootstrap: no variant playlist created after ${STARTUP_TIMEOUT}s" >> "$LOG_FILE"
        cleanup_children

        if [ $ATTEMPT -lt $MAX_RETRIES ]; then
            echo "[$(date)] Retrying in ${RETRY_WAIT}s..." >> "$LOG_FILE"
            sleep $RETRY_WAIT
            continue
        fi
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
        EXIT_CODE=0
        break
    fi

    cleanup_children

    if [ $ATTEMPT -lt $MAX_RETRIES ]; then
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
# Always exit 0 to prevent nginx-rtmp from respawning this script.
# Retries are handled internally by the loop above.
exit 0

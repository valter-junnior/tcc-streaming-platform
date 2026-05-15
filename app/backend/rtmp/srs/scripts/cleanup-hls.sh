#!/bin/bash
set -euo pipefail

HLS_DIR="${HLS_PATH:-/tmp/hls}"
RETENTION_HOURS="${HLS_RETENTION_HOURS:-6}"

if ! [[ "$RETENTION_HOURS" =~ ^[0-9]+$ ]] || [ "$RETENTION_HOURS" -le 0 ]; then
    RETENTION_HOURS=6
fi

RETENTION_MINUTES=$(( RETENTION_HOURS * 60 ))

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting HLS cleanup (retention: ${RETENTION_HOURS}h)"

DELETED_COUNT=0

if [ -d "$HLS_DIR" ]; then
    DELETED_COUNT=$(find "$HLS_DIR" -xdev -type f \( -name "*.ts" -o -name "*.m3u8" -o -name "*.log" \) -mmin "+${RETENTION_MINUTES}" -print -delete 2>/dev/null | wc -l)

    find "$HLS_DIR" -xdev -mindepth 1 -type d -empty -delete 2>/dev/null

    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Cleanup completed - $DELETED_COUNT files deleted"
else
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] HLS directory does not exist: $HLS_DIR"
fi

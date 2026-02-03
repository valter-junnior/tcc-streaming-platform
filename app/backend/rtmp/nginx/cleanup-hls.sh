#!/bin/bash
# HLS Cleanup Script
# Remove arquivos .ts e .m3u8 mais antigos que X horas

HLS_DIR="/tmp/hls"
RETENTION_HOURS="${HLS_RETENTION_HOURS:-6}"

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Starting HLS cleanup (retention: ${RETENTION_HOURS}h)"

# Deletar arquivos .ts e .m3u8 modificados há mais de X horas
DELETED_COUNT=0

if [ -d "$HLS_DIR" ]; then
    # Find e delete arquivos antigos
    DELETED_COUNT=$(find "$HLS_DIR" -type f \( -name "*.ts" -o -name "*.m3u8" \) -mmin +$((RETENTION_HOURS * 60)) -delete -print 2>/dev/null | wc -l)
    
    # Remover diretórios vazios
    find "$HLS_DIR" -mindepth 1 -type d -empty -delete 2>/dev/null
    
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Cleanup completed - $DELETED_COUNT files deleted"
else
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] HLS directory does not exist: $HLS_DIR"
fi

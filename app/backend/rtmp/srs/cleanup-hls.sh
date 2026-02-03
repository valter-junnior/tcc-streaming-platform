#!/bin/bash

# Script de limpeza para arquivos HLS antigos - SRS
# Remove segmentos .ts e playlists .m3u8 de streams encerradas

HLS_DIR="/var/srs-hls"
RETENTION_HOURS=${HLS_RETENTION_HOURS:-6}

echo "$(date): Iniciando limpeza de arquivos HLS antigos..."
echo "Diretório: $HLS_DIR"
echo "Retenção: $RETENTION_HOURS horas"

if [ ! -d "$HLS_DIR" ]; then
    echo "ERRO: Diretório $HLS_DIR não encontrado!"
    exit 1
fi

# Encontrar e remover arquivos .ts mais antigos que X horas
echo "Removendo arquivos .ts antigos..."
find "$HLS_DIR" -name "*.ts" -type f -mtime "+$(echo "scale=2; $RETENTION_HOURS/24" | bc)" -exec rm -f {} \;

# Encontrar e remover arquivos .m3u8 mais antigos que X horas
echo "Removendo arquivos .m3u8 antigos..."
find "$HLS_DIR" -name "*.m3u8" -type f -mtime "+$(echo "scale=2; $RETENTION_HOURS/24" | bc)" -exec rm -f {} \;

# Remover diretórios vazios
echo "Removendo diretórios vazios..."
find "$HLS_DIR" -type d -empty -delete

# Estatísticas finais
REMAINING_FILES=$(find "$HLS_DIR" -type f | wc -l)
DISK_USAGE=$(du -sh "$HLS_DIR" 2>/dev/null | cut -f1)

echo "$(date): Limpeza concluída"
echo "Arquivos restantes: $REMAINING_FILES"
echo "Uso de disco: $DISK_USAGE"
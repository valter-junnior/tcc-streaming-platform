# Dockerfile para GStreamer Transcoding Service
FROM ubuntu:22.04

# Evitar prompts interativos durante instalação
ENV DEBIAN_FRONTEND=noninteractive

# Instalar dependências
RUN apt-get update && apt-get install -y \
    gstreamer1.0-tools \
    gstreamer1.0-plugins-base \
    gstreamer1.0-plugins-good \
    gstreamer1.0-plugins-bad \
    gstreamer1.0-plugins-ugly \
    gstreamer1.0-libav \
    gstreamer1.0-rtsp \
    libgstreamer1.0-dev \
    libgstreamer-plugins-base1.0-dev \
    libgstreamer-plugins-bad1.0-dev \
    gstreamer1.0-vaapi \
    && rm -rf /var/lib/apt/lists/*

# Criar diretórios
RUN mkdir -p /var/hls /scripts

# Copiar script de transcodificação
COPY transcode.sh /scripts/
RUN chmod +x /scripts/transcode.sh

# Definir working directory
WORKDIR /scripts

# Variáveis de ambiente
ENV GST_DEBUG=2
ENV GST_DEBUG_NO_COLOR=1
ENV GST_PLUGIN_PATH=/usr/lib/x86_64-linux-gnu/gstreamer-1.0

# Comando padrão
CMD ["/bin/bash"]
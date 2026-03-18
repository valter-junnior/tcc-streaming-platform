FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y \
    build-essential \
    libpcre3 \
    libpcre3-dev \
    libssl-dev \
    zlib1g-dev \
    wget \
    git \
    ffmpeg \
    gstreamer1.0-tools \
    gstreamer1.0-plugins-base \
    gstreamer1.0-plugins-good \
    gstreamer1.0-plugins-bad \
    gstreamer1.0-plugins-ugly \
    gstreamer1.0-libav \
    netcat-openbsd \
    dnsutils \
    cron \
    bc \
    gettext-base \
    && rm -rf /var/lib/apt/lists/*

# Download Nginx and RTMP module
WORKDIR /tmp
RUN wget https://nginx.org/download/nginx-1.24.0.tar.gz && \
    tar -xzf nginx-1.24.0.tar.gz && \
    git clone --depth 1 --branch v1.2.2 https://github.com/arut/nginx-rtmp-module.git

# Compile Nginx with RTMP module
WORKDIR /tmp/nginx-1.24.0
RUN ./configure \
    --prefix=/usr/local/nginx \
    --with-http_ssl_module \
    --add-module=/tmp/nginx-rtmp-module \
    && make \
    && make install

RUN mkdir -p /tmp/hls /var/log/nginx && \
    chmod 755 /tmp/hls

RUN rm -rf /tmp/nginx-1.24.0* /tmp/nginx-rtmp-module

COPY configs/nginx.conf /usr/local/nginx/conf/nginx.conf.template

COPY scripts/transcode-ffmpeg.sh /usr/local/bin/transcode-ffmpeg.sh
COPY scripts/transcode-gstreamer.sh /usr/local/bin/transcode-gstreamer.sh
COPY scripts/cleanup-hls.sh /usr/local/bin/cleanup-hls.sh
COPY scripts/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh

RUN chmod +x \
    /usr/local/bin/transcode-ffmpeg.sh \
    /usr/local/bin/transcode-gstreamer.sh \
    /usr/local/bin/cleanup-hls.sh \
    /usr/local/bin/docker-entrypoint.sh

RUN echo "0 * * * * /usr/local/bin/cleanup-hls.sh >> /var/log/nginx/hls-cleanup.log 2>&1" > /etc/cron.d/hls-cleanup && \
    chmod 0644 /etc/cron.d/hls-cleanup && \
    crontab /etc/cron.d/hls-cleanup && \
    touch /var/log/nginx/hls-cleanup.log

EXPOSE 1935 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD wget -q -O /dev/null http://localhost:${HLS_HTTP_PORT:-8081}/ || exit 1

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]

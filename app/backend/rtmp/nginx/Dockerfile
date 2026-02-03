FROM ubuntu:22.04

# Avoid prompts from apt
ENV DEBIAN_FRONTEND=noninteractive

# Install dependencies
RUN apt-get update && apt-get install -y \
    build-essential \
    libpcre3 \
    libpcre3-dev \
    libssl-dev \
    zlib1g-dev \
    wget \
    git \
    ffmpeg \
    netcat \
    cron \
    && rm -rf /var/lib/apt/lists/*

# Download Nginx and RTMP module
WORKDIR /tmp
RUN wget http://nginx.org/download/nginx-1.24.0.tar.gz && \
    tar -xzf nginx-1.24.0.tar.gz && \
    git clone https://github.com/arut/nginx-rtmp-module.git

# Compile Nginx with RTMP module
WORKDIR /tmp/nginx-1.24.0
RUN ./configure \
    --prefix=/usr/local/nginx \
    --with-http_ssl_module \
    --add-module=/tmp/nginx-rtmp-module \
    && make \
    && make install

# Create directories for HLS
RUN mkdir -p /tmp/hls && \
    mkdir -p /var/log/nginx && \
    chmod -R 777 /tmp/hls

# Clean up
RUN rm -rf /tmp/nginx-1.24.0* /tmp/nginx-rtmp-module

# Copy nginx configuration
COPY nginx.conf /usr/local/nginx/conf/nginx.conf

# Copy transcoding script (if needed for multi-bitrate)
COPY transcode.sh /usr/local/bin/transcode.sh
RUN chmod +x /usr/local/bin/transcode.sh

# Copy HLS cleanup script
COPY cleanup-hls.sh /usr/local/bin/cleanup-hls.sh
RUN chmod +x /usr/local/bin/cleanup-hls.sh

# Setup cron job (every hour at minute 0)
RUN echo "0 * * * * /usr/local/bin/cleanup-hls.sh >> /var/log/nginx/hls-cleanup.log 2>&1" > /etc/cron.d/hls-cleanup && \
    chmod 0644 /etc/cron.d/hls-cleanup && \
    crontab /etc/cron.d/hls-cleanup

# Create log file
RUN touch /var/log/nginx/hls-cleanup.log

# Expose ports
EXPOSE 1935 8081

# Start cron and Nginx
CMD service cron start && /usr/local/nginx/sbin/nginx -g "daemon off;"

FROM ossrs/srs:4

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y \
	--no-install-recommends \
	ffmpeg \
	gstreamer1.0-tools \
	gstreamer1.0-plugins-base \
	gstreamer1.0-plugins-good \
	gstreamer1.0-plugins-bad \
	gstreamer1.0-plugins-ugly \
	gstreamer1.0-libav \
	netcat-openbsd \
	cron \
	gettext-base \
	wget \
	python3 \
	&& rm -rf /var/lib/apt/lists/*

COPY srs.conf /usr/local/srs/conf/srs.conf.template

COPY scripts/transcode-ffmpeg.sh /usr/local/bin/transcode-ffmpeg.sh
COPY scripts/transcode-gstreamer.sh /usr/local/bin/transcode-gstreamer.sh
COPY scripts/cleanup-hls.sh /usr/local/bin/cleanup-hls.sh
COPY scripts/docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
COPY scripts/hls-server.py /usr/local/bin/hls-server.py

RUN chmod +x \
	/usr/local/bin/transcode-ffmpeg.sh \
	/usr/local/bin/transcode-gstreamer.sh \
	/usr/local/bin/cleanup-hls.sh \
	/usr/local/bin/docker-entrypoint.sh \
	/usr/local/bin/hls-server.py

RUN echo "0 * * * * /usr/local/bin/cleanup-hls.sh >> /var/log/srs-hls-cleanup.log 2>&1" > /etc/cron.d/hls-cleanup && \
	chmod 0644 /etc/cron.d/hls-cleanup && \
	crontab /etc/cron.d/hls-cleanup && \
	touch /var/log/srs-hls-cleanup.log

RUN mkdir -p /tmp/hls

EXPOSE 1935 8080 8081

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
	CMD wget -q -O /dev/null http://localhost:${HLS_HTTP_PORT:-8081}/ || exit 1

ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
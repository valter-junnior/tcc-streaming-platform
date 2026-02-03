# Dockerfile para SRS (Simple Realtime Server)
FROM ossrs/srs:4

# Copiar arquivo de configuração
COPY srs.conf /usr/local/srs/conf/srs.conf

# Criar diretórios necessários
RUN mkdir -p /var/srs-hls

# Expor portas
# 1935: RTMP
# 8080: HTTP API 
# 8081: HLS streaming
EXPOSE 1935 8080 8081

# Comando de início
CMD ["/usr/local/srs/objs/srs", "-c", "/usr/local/srs/conf/srs.conf"]
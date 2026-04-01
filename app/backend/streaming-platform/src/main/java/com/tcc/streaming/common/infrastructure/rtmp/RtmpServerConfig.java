package com.tcc.streaming.common.infrastructure.rtmp;

import com.tcc.streaming.common.infrastructure.rtmp.nginx.NginxRtmpGateway;
import com.tcc.streaming.common.infrastructure.rtmp.srs.SrsRtmpGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RtmpServerConfig {
    private static final Logger log = LoggerFactory.getLogger(RtmpServerConfig.class);

    @Value("${rtmp.server:nginx}")
    private String rtmpServer;

    @Bean
    public RtmpGateway rtmpGateway() {
        return switch (rtmpServer.toLowerCase()) {
            case "srs" -> {
                log.info("[RtmpServerConfig] Servidor RTMP ativo: SRS");
                yield new SrsRtmpGateway();
            }
            default -> {
                log.info("[RtmpServerConfig] Servidor RTMP ativo: Nginx");
                yield new NginxRtmpGateway();
            }
        };
    }
}

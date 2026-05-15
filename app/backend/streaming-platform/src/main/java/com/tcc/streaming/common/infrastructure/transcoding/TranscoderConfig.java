package com.tcc.streaming.common.infrastructure.transcoding;

import com.tcc.streaming.common.infrastructure.transcoding.ffmpeg.FFmpegTranscoderGateway;
import com.tcc.streaming.common.infrastructure.transcoding.gstreamer.GStreamerTranscoderGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Seleciona o bean {@link TranscoderGateway} de acordo com a propriedade
 * {@code transcoder.type} (ffmpeg ou gstreamer), controlada pela variável
 * de ambiente {@code TRANSCODER} no Docker.
 */
@Configuration
public class TranscoderConfig {

    private static final Logger log = LoggerFactory.getLogger(TranscoderConfig.class);

    @Value("${transcoder.type:ffmpeg}")
    private String transcoderType;

    @Bean
    public TranscoderGateway transcoderGateway() {
        return switch (transcoderType.toLowerCase()) {
            case "gstreamer" -> {
                log.info("[TranscoderConfig] Active transcoder: GStreamer");
                yield new GStreamerTranscoderGateway();
            }
            default -> {
                log.info("[TranscoderConfig] Active transcoder: FFmpeg");
                yield new FFmpegTranscoderGateway();
            }
        };
    }
}

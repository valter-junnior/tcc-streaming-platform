package com.tcc.streaming.common.infrastructure.transcoding.ffmpeg;

import com.tcc.streaming.common.infrastructure.transcoding.TranscoderConfig;
import com.tcc.streaming.common.infrastructure.transcoding.TranscoderGateway;
import com.tcc.streaming.common.infrastructure.transcoding.TranscodingProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for FFmpeg Transcoder Gateway.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "streaming.transcoding.provider=ffmpeg",
    "streaming.transcoding.segment-duration=6",
    "streaming.transcoding.playlist-length=60",
    "streaming.transcoding.output-path=/tmp/hls",
    "streaming.transcoding.hardware-acceleration=false"
})
class FFmpegTranscoderGatewayTest {

    @Autowired
    private TranscoderGateway transcoderGateway;

    @Test
    void shouldBeFFmpegImplementation() {
        assertThat(transcoderGateway).isInstanceOf(FFmpegTranscoderGateway.class);
        assertThat(transcoderGateway.getProvider()).isEqualTo("ffmpeg");
    }

    @Test
    void shouldReturnValidConfig() {
        TranscoderConfig config = transcoderGateway.getConfig();

        assertThat(config).isNotNull();
        assertThat(config.getProvider()).isEqualTo("ffmpeg");
        assertThat(config.getSegmentDuration()).isEqualTo(6);
        assertThat(config.getPlaylistLength()).isEqualTo(60);
        assertThat(config.getOutputPath()).isEqualTo("/tmp/hls");
        assertThat(config.isEnableHardwareAcceleration()).isFalse();
    }

    @Test
    void shouldHaveDefaultQualityPresets() {
        TranscoderConfig config = transcoderGateway.getConfig();

        assertThat(config.getQualityPresets()).isNotEmpty();
        assertThat(config.getQualityPresets()).hasSize(4); // 1080p, 720p, 480p, 360p
    }

    @Test
    void shouldGenerateTranscodingCommand() {
        String command = transcoderGateway.generateTranscodingCommand(
            "test_stream_key",
            "rtmp://localhost:1935/live/test_stream_key"
        );

        assertThat(command).isNotNull();
        assertThat(command).contains("ffmpeg");
        assertThat(command).contains("-i rtmp://localhost:1935/live/test_stream_key");
        assertThat(command).contains("-f hls");
        assertThat(command).contains("-hls_time 6");
        assertThat(command).contains("master.m3u8");
        assertThat(command).contains("scale");
        assertThat(command).contains("libx264");
    }

    @Test
    void shouldGenerateCorrectOutputPath() {
        String outputPath = transcoderGateway.getOutputPath("my_stream");

        assertThat(outputPath).isEqualTo("/tmp/hls/my_stream");
    }

    @Test
    void shouldDetectFFmpegVersion() {
        String version = transcoderGateway.getVersion();

        // Version should not be null or empty
        assertThat(version).isNotNull();
        assertThat(version).isNotEmpty();
    }
}

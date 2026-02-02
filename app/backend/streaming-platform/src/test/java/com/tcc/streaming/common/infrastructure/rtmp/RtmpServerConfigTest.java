package com.tcc.streaming.common.infrastructure.rtmp;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RtmpServerConfig.
 */
class RtmpServerConfigTest {

    @Test
    void shouldCreateConfigWithAllParameters() {
        RtmpServerConfig config = new RtmpServerConfig(
            "localhost",
            "live",
            1935,
            "nginx-rtmp",
            "Test instructions"
        );

        assertThat(config.getServerUrl()).isEqualTo("localhost");
        assertThat(config.getApplicationName()).isEqualTo("live");
        assertThat(config.getPort()).isEqualTo(1935);
        assertThat(config.getServerType()).isEqualTo("nginx-rtmp");
        assertThat(config.getSetupInstructions()).isEqualTo("Test instructions");
    }

    @Test
    void shouldGenerateCompleteRtmpUrl() {
        RtmpServerConfig config = new RtmpServerConfig(
            "example.com",
            "stream",
            1935,
            "nginx-rtmp",
            "Instructions"
        );

        String completeUrl = config.getCompleteRtmpUrl();
        assertThat(completeUrl).isEqualTo("rtmp://example.com:1935/stream");
    }

    @Test
    void shouldHandleDifferentPorts() {
        RtmpServerConfig config = new RtmpServerConfig(
            "localhost",
            "live",
            8935,
            "nginx-rtmp",
            "Instructions"
        );

        assertThat(config.getCompleteRtmpUrl()).isEqualTo("rtmp://localhost:8935/live");
    }

    @Test
    void shouldHandleDifferentApplicationNames() {
        RtmpServerConfig config = new RtmpServerConfig(
            "localhost",
            "myapp",
            1935,
            "nginx-rtmp",
            "Instructions"
        );

        assertThat(config.getCompleteRtmpUrl()).isEqualTo("rtmp://localhost:1935/myapp");
    }
}

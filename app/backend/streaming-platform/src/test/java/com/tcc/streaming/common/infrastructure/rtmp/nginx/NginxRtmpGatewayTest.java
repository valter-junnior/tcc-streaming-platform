package com.tcc.streaming.common.infrastructure.rtmp.nginx;

import com.tcc.streaming.common.infrastructure.rtmp.RtmpServerConfig;
import com.tcc.streaming.common.infrastructure.rtmp.RtmpServerGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Nginx-RTMP Gateway.
 * Tests the configuration and basic functionality of the RTMP server abstraction.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "streaming.rtmp.provider=nginx-rtmp",
    "streaming.rtmp.host=localhost",
    "streaming.rtmp.port=1935",
    "streaming.rtmp.application=live"
})
class NginxRtmpGatewayTest {

    @Autowired
    private RtmpServerGateway rtmpServerGateway;

    @Test
    void shouldBeNginxRtmpImplementation() {
        assertThat(rtmpServerGateway).isInstanceOf(NginxRtmpGateway.class);
        assertThat(rtmpServerGateway.getServerType()).isEqualTo("nginx-rtmp");
    }

    @Test
    void shouldReturnCorrectServerUrl() {
        String serverUrl = rtmpServerGateway.getRtmpServerUrl();
        assertThat(serverUrl).isEqualTo("localhost");
    }

    @Test
    void shouldReturnCorrectApplicationName() {
        String appName = rtmpServerGateway.getApplicationName();
        assertThat(appName).isEqualTo("live");
    }

    @Test
    void shouldReturnValidServerConfig() {
        RtmpServerConfig config = rtmpServerGateway.getServerConfig();
        
        assertThat(config).isNotNull();
        assertThat(config.getServerUrl()).isEqualTo("localhost");
        assertThat(config.getPort()).isEqualTo(1935);
        assertThat(config.getApplicationName()).isEqualTo("live");
        assertThat(config.getServerType()).isEqualTo("nginx-rtmp");
        assertThat(config.getSetupInstructions()).contains("OBS Studio");
    }

    @Test
    void shouldGenerateCompleteRtmpUrl() {
        RtmpServerConfig config = rtmpServerGateway.getServerConfig();
        String completeUrl = config.getCompleteRtmpUrl();
        
        assertThat(completeUrl).isEqualTo("rtmp://localhost:1935/live");
    }
}

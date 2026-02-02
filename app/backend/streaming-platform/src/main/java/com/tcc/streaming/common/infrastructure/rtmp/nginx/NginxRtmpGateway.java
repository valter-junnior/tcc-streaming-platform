package com.tcc.streaming.common.infrastructure.rtmp.nginx;

import com.tcc.streaming.common.infrastructure.rtmp.RtmpServerConfig;
import com.tcc.streaming.common.infrastructure.rtmp.RtmpServerGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Socket;

/**
 * Nginx-RTMP implementation of the RTMP server gateway.
 * Handles communication with Nginx-RTMP server.
 */
@Component
@ConditionalOnProperty(name = "streaming.rtmp.provider", havingValue = "nginx-rtmp", matchIfMissing = true)
public class NginxRtmpGateway implements RtmpServerGateway {
    
    private static final Logger log = LoggerFactory.getLogger(NginxRtmpGateway.class);
    
    @Value("${streaming.rtmp.host:nginx-rtmp}")
    private String rtmpHost;
    
    @Value("${streaming.rtmp.port:1935}")
    private int rtmpPort;
    
    @Value("${streaming.rtmp.application:live}")
    private String applicationName;
    
    @Override
    public String getRtmpServerUrl() {
        return rtmpHost;
    }
    
    @Override
    public String getApplicationName() {
        return applicationName;
    }
    
    @Override
    public boolean isServerHealthy() {
        try (Socket socket = new Socket(rtmpHost, rtmpPort)) {
            log.debug("Nginx-RTMP server is healthy at {}:{}", rtmpHost, rtmpPort);
            return true;
        } catch (IOException e) {
            log.warn("Nginx-RTMP server health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getServerType() {
        return "nginx-rtmp";
    }
    
    @Override
    public RtmpServerConfig getServerConfig() {
        String instructions = """
                OBS Studio Configuration:
                1. Open Settings > Stream
                2. Service: Custom
                3. Server: rtmp://%s:%d/%s
                4. Stream Key: [Use the stream key provided]
                5. Click Apply and start streaming
                
                Note: Nginx-RTMP is stable and widely used for RTMP streaming.
                """.formatted(rtmpHost, rtmpPort, applicationName);
        
        return new RtmpServerConfig(
                rtmpHost,
                applicationName,
                rtmpPort,
                "nginx-rtmp",
                instructions
        );
    }
}

package com.tcc.streaming.common.infrastructure.rtmp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for RTMP server.
 * Allows switching between different RTMP providers via application.yml
 */
@Configuration
@ConfigurationProperties(prefix = "streaming.rtmp")
public class RtmpServerProperties {
    
    /**
     * RTMP server provider: "nginx-rtmp" or "srs"
     */
    private String provider = "nginx-rtmp";
    
    /**
     * RTMP server host
     */
    private String host = "nginx-rtmp";
    
    /**
     * RTMP server port
     */
    private int port = 1935;
    
    /**
     * RTMP application name
     */
    private String application = "live";
    
    /**
     * Backend callback URL for authentication
     */
    private String callbackUrl = "http://streaming-platform:8080/api/streams/callback";
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public String getHost() {
        return host;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public int getPort() {
        return port;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public String getApplication() {
        return application;
    }
    
    public void setApplication(String application) {
        this.application = application;
    }
    
    public String getCallbackUrl() {
        return callbackUrl;
    }
    
    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }
}

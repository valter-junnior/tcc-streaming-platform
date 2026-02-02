package com.tcc.streaming.common.infrastructure.rtmp;

/**
 * Configuration details for an RTMP server implementation.
 * Contains information needed for streamers to configure their streaming software (OBS).
 */
public class RtmpServerConfig {
    
    private final String serverUrl;
    private final String applicationName;
    private final int port;
    private final String serverType;
    private final String setupInstructions;
    
    public RtmpServerConfig(
            String serverUrl, 
            String applicationName, 
            int port, 
            String serverType,
            String setupInstructions) {
        this.serverUrl = serverUrl;
        this.applicationName = applicationName;
        this.port = port;
        this.serverType = serverType;
        this.setupInstructions = setupInstructions;
    }
    
    public String getServerUrl() {
        return serverUrl;
    }
    
    public String getApplicationName() {
        return applicationName;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getServerType() {
        return serverType;
    }
    
    public String getSetupInstructions() {
        return setupInstructions;
    }
    
    /**
     * Get the complete RTMP URL with application name.
     * Example: rtmp://localhost:1935/live
     */
    public String getCompleteRtmpUrl() {
        return String.format("rtmp://%s:%d/%s", serverUrl, port, applicationName);
    }
}

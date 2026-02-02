package com.tcc.streaming.common.infrastructure.rtmp;

/**
 * Gateway interface for RTMP server operations.
 * This abstraction allows switching between different RTMP server implementations
 * (e.g., Nginx-RTMP, SRS) without changing business logic.
 * 
 * @see NginxRtmpGateway
 */
public interface RtmpServerGateway {
    
    /**
     * Get the RTMP server URL for streaming.
     * 
     * @return RTMP server URL (e.g., "rtmp://localhost:1935/live")
     */
    String getRtmpServerUrl();
    
    /**
     * Get the base application name used in RTMP URLs.
     * 
     * @return Application name (e.g., "live")
     */
    String getApplicationName();
    
    /**
     * Validate if the RTMP server is running and accessible.
     * 
     * @return true if server is healthy, false otherwise
     */
    boolean isServerHealthy();
    
    /**
     * Get the type/implementation of the RTMP server.
     * 
     * @return Server type (e.g., "nginx-rtmp", "srs")
     */
    String getServerType();
    
    /**
     * Get server-specific configuration information.
     * Used for displaying setup instructions to streamers.
     * 
     * @return Configuration details
     */
    RtmpServerConfig getServerConfig();
}

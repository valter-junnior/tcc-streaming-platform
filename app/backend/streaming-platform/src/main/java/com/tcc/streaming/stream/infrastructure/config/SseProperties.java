package com.tcc.streaming.stream.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sse")
public class SseProperties {

    /**
     * Timeout for SSE connections in milliseconds
     */
    private Long timeout = 30 * 60 * 1000L; // 30 minutes

    /**
     * Keepalive interval in milliseconds
     */
    private Long keepaliveInterval = 30 * 1000L; // 30 seconds

    /**
     * TTL cleanup interval in milliseconds
     */
    private Long ttlCleanupInterval = 5 * 60 * 1000L; // 5 minutes

    /**
     * Emitter TTL in hours
     */
    private Long emitterTtlHours = 2L; // 2 hours

    // Getters and setters
    public Long getTimeout() {
        return timeout;
    }

    public void setTimeout(Long timeout) {
        this.timeout = timeout;
    }

    public Long getKeepaliveInterval() {
        return keepaliveInterval;
    }

    public void setKeepaliveInterval(Long keepaliveInterval) {
        this.keepaliveInterval = keepaliveInterval;
    }

    public Long getTtlCleanupInterval() {
        return ttlCleanupInterval;
    }

    public void setTtlCleanupInterval(Long ttlCleanupInterval) {
        this.ttlCleanupInterval = ttlCleanupInterval;
    }

    public Long getEmitterTtlHours() {
        return emitterTtlHours;
    }

    public void setEmitterTtlHours(Long emitterTtlHours) {
        this.emitterTtlHours = emitterTtlHours;
    }
}
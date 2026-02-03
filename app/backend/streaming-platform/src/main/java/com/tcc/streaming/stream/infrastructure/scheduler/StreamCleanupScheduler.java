package com.tcc.streaming.stream.infrastructure.scheduler;

import com.tcc.streaming.stream.application.services.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task to cleanup inactive streams
 * Runs daily at 3 AM by default
 */
@Component
public class StreamCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(StreamCleanupScheduler.class);
    
    private final StreamService streamService;
    
    @Value("${stream.cleanup.threshold-days:1}")
    private int thresholdDays;

    public StreamCleanupScheduler(StreamService streamService) {
        this.streamService = streamService;
    }

    /**
     * Cleanup inactive streams daily at 3 AM
     * Cron format: second, minute, hour, day of month, month, day of week
     */
    @Scheduled(cron = "${stream.cleanup.cron:0 0 3 * * *}")
    public void cleanupInactiveStreams() {
        log.info("[StreamCleanupScheduler] Starting scheduled cleanup task (threshold: {} days)", thresholdDays);
        
        try {
            int cleanedCount = streamService.cleanupInactiveStreams(thresholdDays);
            log.info("[StreamCleanupScheduler] Cleanup completed successfully - {} streams cleaned", cleanedCount);
        } catch (Exception e) {
            log.error("[StreamCleanupScheduler] Error during cleanup task: {}", e.getMessage(), e);
        }
    }
}

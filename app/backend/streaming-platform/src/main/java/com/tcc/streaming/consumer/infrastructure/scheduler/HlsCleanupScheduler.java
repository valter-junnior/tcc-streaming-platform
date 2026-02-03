package com.tcc.streaming.consumer.infrastructure.scheduler;

import com.tcc.streaming.consumer.core.usecases.CleanupOldHlsFilesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task para limpar arquivos HLS antigos
 */
@Component
public class HlsCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(HlsCleanupScheduler.class);
    private final CleanupOldHlsFilesUseCase cleanupOldHlsFilesUseCase;

    public HlsCleanupScheduler(CleanupOldHlsFilesUseCase cleanupOldHlsFilesUseCase) {
        this.cleanupOldHlsFilesUseCase = cleanupOldHlsFilesUseCase;
    }

    /**
     * Executa limpeza de arquivos HLS a cada hora
     * Cron: 0 0 * * * * (segundo 0, minuto 0, todas as horas)
     */
    @Scheduled(cron = "${streaming.cleanup.old-hls-files-cron:0 0 * * * *}")
    public void cleanupOldHlsFiles() {
        log.info("[HlsCleanupScheduler] Starting scheduled HLS files cleanup");
        
        try {
            int deletedCount = cleanupOldHlsFilesUseCase.execute();
            log.info("[HlsCleanupScheduler] Cleanup completed - {} files deleted", deletedCount);
        } catch (Exception e) {
            log.error("[HlsCleanupScheduler] Error during HLS cleanup: {}", e.getMessage(), e);
        }
    }
}

package com.tcc.streaming.consumer.application.services;

import com.tcc.streaming.consumer.core.usecases.CleanupOldHlsFilesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Serviço para limpar arquivos HLS antigos de streams encerradas
 */
@Service
public class HlsCleanupService implements CleanupOldHlsFilesUseCase {

    private static final Logger log = LoggerFactory.getLogger(HlsCleanupService.class);
    
    @Value("${streaming.transcoding.output-path:/tmp/hls}")
    private String hlsOutputPath;
    
    @Value("${streaming.cleanup.hls-retention-hours:6}")
    private int retentionHours;

    @Override
    public int execute() {
        log.info("[HlsCleanupService] Starting HLS files cleanup (retention: {} hours)", retentionHours);
        
        Path hlsDirectory = Paths.get(hlsOutputPath);
        if (!Files.exists(hlsDirectory)) {
            log.warn("[HlsCleanupService] HLS directory does not exist: {}", hlsOutputPath);
            return 0;
        }
        
        LocalDateTime thresholdTime = LocalDateTime.now().minusHours(retentionHours);
        AtomicInteger deletedCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        try {
            Files.walkFileTree(hlsDirectory, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        // Obter tempo de modificação do arquivo
                        Instant lastModified = attrs.lastModifiedTime().toInstant();
                        LocalDateTime fileTime = LocalDateTime.ofInstant(lastModified, ZoneId.systemDefault());
                        
                        // Deletar se mais antigo que threshold
                        if (fileTime.isBefore(thresholdTime)) {
                            String fileName = file.getFileName().toString();
                            // Deletar apenas arquivos HLS (.ts, .m3u8)
                            if (fileName.endsWith(".ts") || fileName.endsWith(".m3u8")) {
                                Files.delete(file);
                                deletedCount.incrementAndGet();
                                log.debug("[HlsCleanupService] Deleted old file: {}", file);
                            }
                        }
                    } catch (Exception e) {
                        log.error("[HlsCleanupService] Error deleting file {}: {}", file, e.getMessage());
                        errorCount.incrementAndGet();
                    }
                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                    // Tentar deletar diretórios vazios
                    if (!dir.equals(hlsDirectory)) {
                        try {
                            if (isDirectoryEmpty(dir)) {
                                Files.delete(dir);
                                log.debug("[HlsCleanupService] Deleted empty directory: {}", dir);
                            }
                        } catch (Exception e) {
                            log.debug("[HlsCleanupService] Could not delete directory {}: {}", dir, e.getMessage());
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            
            log.info("[HlsCleanupService] Cleanup completed - {} files deleted, {} errors", 
                     deletedCount.get(), errorCount.get());
            return deletedCount.get();
            
        } catch (Exception e) {
            log.error("[HlsCleanupService] Error during HLS cleanup: {}", e.getMessage(), e);
            return deletedCount.get();
        }
    }
    
    private boolean isDirectoryEmpty(Path directory) throws IOException {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        }
    }
}

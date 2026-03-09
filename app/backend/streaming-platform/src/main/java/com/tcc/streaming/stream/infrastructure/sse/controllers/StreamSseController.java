package com.tcc.streaming.stream.infrastructure.sse.controllers;

import com.tcc.streaming.stream.application.services.StreamService;
import com.tcc.streaming.stream.infrastructure.sse.SseEmitterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/sse")
public class StreamSseController {

    private static final Logger log = LoggerFactory.getLogger(StreamSseController.class);
    
    private final SseEmitterManager emitterManager;
    private final StreamService streamService;

    public StreamSseController(SseEmitterManager emitterManager, StreamService streamService) {
        this.emitterManager = emitterManager;
        this.streamService = streamService;
    }

    @GetMapping(value = "/stream/{streamId}/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable UUID streamId, 
                                @RequestParam String viewerId,
                                @RequestParam(defaultValue = "true") boolean countAsViewer) {
        
        log.info("[SSE] Connecting for notifications - StreamId: {}, ViewerId: {}, CountAsViewer: {}", 
                 streamId, viewerId, countAsViewer);
        
        try {
            streamService.execute(streamId);
        } catch (Exception e) {
            log.error("[SSE] Stream validation failed - StreamId: {}, Error: {}", streamId, e.getMessage());
            throw e;
        }
        
        SseEmitter emitter = emitterManager.createEmitter(streamId, viewerId, () -> {
            log.warn("[SSE] DISCONNECT CALLBACK EXECUTING - StreamId: {}, ViewerId: {}, CountAsViewer: {}", 
                     streamId, viewerId, countAsViewer);
            
            if (countAsViewer) {
                try {
                    log.info("[SSE] Decreasing viewer count on disconnect - StreamId: {}, ViewerId: {}", 
                             streamId, viewerId);
                    streamService.decrementViewers(streamId);
                    
                } catch (Exception e) {
                    log.error("[SSE] Error decreasing viewer count: {}", e.getMessage(), e);
                }
            }
        });
        
        log.info("[SSE] Connection established - StreamId: {}, ViewerId: {}", streamId, viewerId);
        return emitter;
    }

    public void broadcastStreamStarted(UUID streamId) {
        log.info("[SSE] Broadcasting stream started - StreamId: {}", streamId);
        emitterManager.broadcastToStream(streamId, "stream_status", 
            new StreamStatusMessage("STREAM_STARTED", "LIVE"));
    }

    public void broadcastStreamEnded(UUID streamId) {
        log.info("[SSE] Broadcasting stream ended - StreamId: {}", streamId);
        emitterManager.broadcastToStream(streamId, "stream_status", 
            new StreamStatusMessage("STREAM_ENDED", "ENDED"));
        
        // Cleanup assincrono dos emitters após delay
        cleanupEmittersAfterDelay(streamId);
    }

    @Async
    private CompletableFuture<Void> cleanupEmittersAfterDelay(UUID streamId) {
        try {
            Thread.sleep(5000);
            log.info("[SSE] Cleaning up emitters for ended stream: {}", streamId);
            emitterManager.removeAllEmittersForStream(streamId);
        } catch (InterruptedException e) {
            log.warn("[SSE] Cleanup interrupted for stream: {}", streamId);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("[SSE] Error during emitter cleanup for stream {}: {}", streamId, e.getMessage(), e);
        }
        
        return CompletableFuture.completedFuture(null);
    }

    public void broadcastViewersUpdate(UUID streamId, int currentViewers, int viewersPeak) {
        log.debug("[SSE] Broadcasting viewers update - StreamId: {}, Current: {}, Peak: {}", 
                  streamId, currentViewers, viewersPeak);
        emitterManager.broadcastToStream(streamId, "viewers_update", 
            new ViewersUpdateMessage(currentViewers, viewersPeak));
    }

    // DTOs para mensagens SSE
    public record StreamStatusMessage(String type, String status) {}
    public record ViewersUpdateMessage(int currentViewers, int viewersPeak) {}
}

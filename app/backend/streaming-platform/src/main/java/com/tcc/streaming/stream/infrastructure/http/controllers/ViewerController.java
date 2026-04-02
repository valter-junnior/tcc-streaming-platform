package com.tcc.streaming.stream.infrastructure.http.controllers;

import com.tcc.streaming.common.infrastructure.events.EventPublisher;
import com.tcc.streaming.stream.application.services.StreamService;
import com.tcc.streaming.stream.core.dtos.stream.StreamDto;
import com.tcc.streaming.stream.infrastructure.sse.SseEmitterManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller REST para gerenciar entrada/saída de viewers nas streams
 * Separado do SSE para evitar problemas de transações travadas
 */
@RestController
@RequestMapping("/api/streams")
public class ViewerController {

    private static final Logger log = LoggerFactory.getLogger(ViewerController.class);

    private final StreamService streamService;
    private final EventPublisher eventPublisher;
    private final SseEmitterManager sseEmitterManager;

    public ViewerController(StreamService streamService,
                           EventPublisher eventPublisher,
                           SseEmitterManager sseEmitterManager) {
        this.streamService = streamService;
        this.eventPublisher = eventPublisher;
        this.sseEmitterManager = sseEmitterManager;
    }

    @PostMapping("/{streamId}/join")
    public ResponseEntity<ViewerResponse> joinStream(
            @PathVariable UUID streamId,
            @RequestParam String viewerId,
            @RequestParam(defaultValue = "true") boolean countAsViewer) {
        
        log.info("[Viewer] Join request - StreamId: {}, ViewerId: {}, CountAsViewer: {}", 
                 streamId, viewerId, countAsViewer);

        if (countAsViewer && sseEmitterManager.isViewerConnectedToStream(streamId, viewerId)) {
            log.info("[Viewer] Already connected to this stream - ViewerId: {}", viewerId);
            StreamDto stream = streamService.execute(streamId);
            return ResponseEntity.ok(new ViewerResponse(
                stream.currentViewers(),
                stream.viewersPeak(),
                "already_connected"
            ));
        }

        if (countAsViewer) {
            StreamDto stream = streamService.incrementViewers(streamId);
            sseEmitterManager.registerActiveViewer(streamId, viewerId);
            
            log.info("[Viewer] Joined stream - StreamId: {}, Current: {}, Peak: {}", 
                     streamId, stream.currentViewers(), stream.viewersPeak());
            
            eventPublisher.publishViewerJoined(streamId, viewerId);
            sseEmitterManager.broadcastToStream(streamId, "viewers_update", 
                new ViewersUpdateMessage(stream.currentViewers(), stream.viewersPeak()));
            
            return ResponseEntity.ok(new ViewerResponse(
                stream.currentViewers(),
                stream.viewersPeak(),
                "joined"
            ));
        } else {
            StreamDto stream = streamService.execute(streamId);
            log.info("[Viewer] Streamer consulting - ViewerId: {}", viewerId);
            
            return ResponseEntity.ok(new ViewerResponse(
                stream.currentViewers(),
                stream.viewersPeak(),
                "streamer"
            ));
        }
    }

    /**
     * Viewer sai da stream (decrementa contador)
     * GET: usado por sendBeacon quando usuário fecha aba
     * POST: usado por axios em navegação SPA
     */
    @GetMapping("/{streamId}/leave")
    public ResponseEntity<ViewerResponse> leaveStreamGet(
            @PathVariable UUID streamId,
            @RequestParam String viewerId,
            @RequestParam(defaultValue = "true") boolean countAsViewer) {
        return processLeave(streamId, viewerId, countAsViewer);
    }
    
    @PostMapping("/{streamId}/leave")
    public ResponseEntity<ViewerResponse> leaveStream(
            @PathVariable UUID streamId,
            @RequestParam String viewerId,
            @RequestParam(defaultValue = "true") boolean countAsViewer) {
        return processLeave(streamId, viewerId, countAsViewer);
    }
    
    private ResponseEntity<ViewerResponse> processLeave(
            UUID streamId,
            String viewerId,
            boolean countAsViewer) {
        
        log.info("[Viewer] Leave request - StreamId: {}, ViewerId: {}, CountAsViewer: {}", 
                 streamId, viewerId, countAsViewer);

        if (countAsViewer) {
            StreamDto stream = streamService.decrementViewers(streamId);
            sseEmitterManager.unregisterActiveViewer(viewerId);
            
            log.info("[Viewer] Left stream - StreamId: {}, Current: {}", 
                     streamId, stream.currentViewers());
            
            eventPublisher.publishViewerLeft(streamId, viewerId);
            sseEmitterManager.broadcastToStream(streamId, "viewers_update", 
                new ViewersUpdateMessage(stream.currentViewers(), stream.viewersPeak()));
            
            return ResponseEntity.ok(new ViewerResponse(
                stream.currentViewers(),
                stream.viewersPeak(),
                "left"
            ));
        } else {
            StreamDto stream = streamService.execute(streamId);
            return ResponseEntity.ok(new ViewerResponse(
                stream.currentViewers(),
                stream.viewersPeak(),
                "streamer"
            ));
        }
    }

    public record ViewerResponse(int currentViewers, int viewersPeak, String status) {}
    public record ViewersUpdateMessage(int currentViewers, int viewersPeak) {}
}

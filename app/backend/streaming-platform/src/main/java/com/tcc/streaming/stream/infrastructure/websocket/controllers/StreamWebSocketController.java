package com.tcc.streaming.stream.infrastructure.websocket.controllers;

import com.tcc.streaming.common.infrastructure.events.EventPublisher;
import com.tcc.streaming.stream.application.services.StreamService;
import com.tcc.streaming.stream.core.dtos.stream.StreamDto;
import com.tcc.streaming.stream.infrastructure.websocket.listeners.WebSocketEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

@Controller
public class StreamWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(StreamWebSocketController.class);
    private final SimpMessagingTemplate messagingTemplate;
    private final StreamService streamService;
    private final EventPublisher eventPublisher;
    private final WebSocketEventListener webSocketEventListener;

    public StreamWebSocketController(SimpMessagingTemplate messagingTemplate,
                                     StreamService streamService,
                                     EventPublisher eventPublisher,
                                     WebSocketEventListener webSocketEventListener) {
        this.messagingTemplate = messagingTemplate;
        this.streamService = streamService;
        this.eventPublisher = eventPublisher;
        this.webSocketEventListener = webSocketEventListener;
    }

    @MessageMapping("/stream/{streamId}/join")
    public void joinStream(@DestinationVariable UUID streamId, @Payload Map<String, String> payload, StompHeaderAccessor headerAccessor) {
        String viewerId = payload.get("viewerId");
        String sessionId = headerAccessor.getSessionId();
        
        log.info("[WebSocket] Viewer joining - Stream: {}, Viewer: {}, Session: {}", streamId, viewerId, sessionId);
        
        // Registrar sessão para auto-cleanup em caso de desconexão
        webSocketEventListener.registerViewerSession(sessionId, streamId, viewerId);
        
        // Incrementar viewers
        StreamDto stream = streamService.incrementViewers(streamId);
        
        // Publicar evento viewer_joined no RabbitMQ
        eventPublisher.publishViewerJoined(streamId, viewerId);
        
        // Broadcast atualização de viewers
        broadcastViewersUpdate(streamId, stream.currentViewers(), stream.viewersPeak());
    }

    @MessageMapping("/stream/{streamId}/leave")
    public void leaveStream(@DestinationVariable UUID streamId, @Payload Map<String, String> payload, StompHeaderAccessor headerAccessor) {
        String viewerId = payload.get("viewerId");
        String sessionId = headerAccessor.getSessionId();
        
        log.info("[WebSocket] Viewer leaving - Stream: {}, Viewer: {}, Session: {}", streamId, viewerId, sessionId);
        
        // Desregistrar sessão (para não decrementar duas vezes se disconnect event disparar)
        webSocketEventListener.unregisterViewerSession(sessionId);
        
        // Decrementar viewers
        StreamDto stream = streamService.decrementViewers(streamId);
        
        // Publicar evento viewer_left no RabbitMQ
        eventPublisher.publishViewerLeft(streamId, viewerId);
        
        // Broadcast atualização de viewers
        broadcastViewersUpdate(streamId, stream.currentViewers(), stream.viewersPeak());
    }

    public void broadcastStreamStarted(UUID streamId) {
        messagingTemplate.convertAndSend("/topic/stream/" + streamId + "/status", 
            new WebSocketMessage("STREAM_STARTED", streamId.toString(), new StatusData("LIVE")));
    }

    public void broadcastStreamEnded(UUID streamId) {
        messagingTemplate.convertAndSend("/topic/stream/" + streamId + "/status", 
            new WebSocketMessage("STREAM_ENDED", streamId.toString(), new StatusData("ENDED")));
    }

    public void broadcastViewersUpdate(UUID streamId, int currentViewers, int viewersPeak) {
        messagingTemplate.convertAndSend("/topic/stream/" + streamId + "/viewers", 
            new ViewersUpdateMessage("VIEWERS_UPDATE", currentViewers, viewersPeak));
    }

    public record ViewersUpdateMessage(String type, int currentViewers, int viewersPeak) {}
    public record StatusData(String status) {}
    public record WebSocketMessage(String type, String streamId, StatusData data) {}
}

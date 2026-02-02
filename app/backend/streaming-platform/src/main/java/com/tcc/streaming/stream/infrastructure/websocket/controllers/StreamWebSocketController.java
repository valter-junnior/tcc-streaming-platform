package com.tcc.streaming.stream.infrastructure.websocket.controllers;

import com.tcc.streaming.common.infrastructure.events.EventPublisher;
import com.tcc.streaming.stream.application.services.StreamService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class StreamWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final StreamService streamService;
    private final EventPublisher eventPublisher;

    public StreamWebSocketController(SimpMessagingTemplate messagingTemplate,
                                     StreamService streamService,
                                     EventPublisher eventPublisher) {
        this.messagingTemplate = messagingTemplate;
        this.streamService = streamService;
        this.eventPublisher = eventPublisher;
    }

    @MessageMapping("/stream/{streamId}/join")
    public void joinStream(@DestinationVariable UUID streamId) {
        // TODO: Incrementar viewers usando StreamService
        String viewerId = UUID.randomUUID().toString(); // Temporário
        
        // Publicar evento viewer_joined no RabbitMQ
        eventPublisher.publishViewerJoined(streamId, viewerId);
        
        // Broadcast atualização de viewers
        messagingTemplate.convertAndSend("/topic/stream/" + streamId + "/viewers", 
            new ViewerUpdateMessage("joined", 0));
    }

    @MessageMapping("/stream/{streamId}/leave")
    public void leaveStream(@DestinationVariable UUID streamId) {
        // TODO: Decrementar viewers usando StreamService
        String viewerId = UUID.randomUUID().toString(); // Temporário
        
        // Publicar evento viewer_left no RabbitMQ
        eventPublisher.publishViewerLeft(streamId, viewerId);
        
        // Broadcast atualização de viewers
        messagingTemplate.convertAndSend("/topic/stream/" + streamId + "/viewers", 
            new ViewerUpdateMessage("left", 0));
    }

    public void broadcastStreamStarted(UUID streamId) {
        messagingTemplate.convertAndSend("/topic/stream/" + streamId + "/status", 
            new StatusUpdateMessage("LIVE"));
    }

    public void broadcastStreamEnded(UUID streamId) {
        messagingTemplate.convertAndSend("/topic/stream/" + streamId + "/status", 
            new StatusUpdateMessage("ENDED"));
    }

    public record ViewerUpdateMessage(String action, int currentViewers) {}
    public record StatusUpdateMessage(String status) {}
}

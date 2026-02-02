package com.tcc.streaming.common.infrastructure.events;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.tcc.streaming.common.infrastructure.config.RabbitMQConfig.*;

@Component
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public EventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishStreamCreated(UUID streamId, String streamKey, String title) {
        Map<String, Object> event = createEvent("stream_created", streamId);
        event.put("streamKey", streamKey);
        event.put("title", title);
        
        rabbitTemplate.convertAndSend(STREAM_EXCHANGE, STREAM_CREATED_KEY, event);
    }

    public void publishStreamStarted(UUID streamId, String streamKey) {
        Map<String, Object> event = createEvent("stream_started", streamId);
        event.put("streamKey", streamKey);
        
        rabbitTemplate.convertAndSend(STREAM_EXCHANGE, STREAM_STARTED_KEY, event);
    }

    public void publishStreamEnded(UUID streamId, String streamKey, Integer viewersPeak) {
        Map<String, Object> event = createEvent("stream_ended", streamId);
        event.put("streamKey", streamKey);
        event.put("viewersPeak", viewersPeak);
        
        rabbitTemplate.convertAndSend(STREAM_EXCHANGE, STREAM_ENDED_KEY, event);
    }

    public void publishViewerJoined(UUID streamId, String viewerId) {
        Map<String, Object> event = createEvent("viewer_joined", streamId);
        event.put("viewerId", viewerId);
        
        rabbitTemplate.convertAndSend(STREAM_EXCHANGE, VIEWER_JOINED_KEY, event);
    }

    public void publishViewerLeft(UUID streamId, String viewerId) {
        Map<String, Object> event = createEvent("viewer_left", streamId);
        event.put("viewerId", viewerId);
        
        rabbitTemplate.convertAndSend(STREAM_EXCHANGE, VIEWER_LEFT_KEY, event);
    }

    private Map<String, Object> createEvent(String eventType, UUID streamId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("streamId", streamId.toString());
        event.put("timestamp", LocalDateTime.now().toString());
        return event;
    }
}

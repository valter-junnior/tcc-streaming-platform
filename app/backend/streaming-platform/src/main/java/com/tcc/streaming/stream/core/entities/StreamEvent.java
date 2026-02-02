package com.tcc.streaming.stream.core.entities;

import java.time.LocalDateTime;
import java.util.UUID;

public class StreamEvent {
    private Long id;
    private UUID streamId;
    private StreamEventType eventType;
    private String metadata;  // JSON string com informações adicionais
    private LocalDateTime timestamp;

    // Constructor privado
    private StreamEvent() {
        this.timestamp = LocalDateTime.now();
    }

    // Constructor completo para mapeamento
    public StreamEvent(Long id, UUID streamId, StreamEventType eventType, String metadata, LocalDateTime timestamp) {
        this.id = id;
        this.streamId = streamId;
        this.eventType = eventType;
        this.metadata = metadata;
        this.timestamp = timestamp;
    }

    // Factory method
    public static StreamEvent create(UUID streamId, StreamEventType eventType, String metadata) {
        StreamEvent event = new StreamEvent();
        event.streamId = streamId;
        event.eventType = eventType;
        event.metadata = metadata;
        return event;
    }

    // Getters e Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getStreamId() {
        return streamId;
    }

    public void setStreamId(UUID streamId) {
        this.streamId = streamId;
    }

    public StreamEventType getEventType() {
        return eventType;
    }

    public void setEventType(StreamEventType eventType) {
        this.eventType = eventType;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}

package com.tcc.streaming.stream.infrastructure.persistence.entities;

import com.tcc.streaming.stream.core.entities.StreamEventType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stream_events")
public class StreamEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stream_id", nullable = false)
    private UUID streamId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private StreamEventType eventType;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    public StreamEventJpaEntity() {
    }

    public StreamEventJpaEntity(Long id, UUID streamId, StreamEventType eventType, String metadata, LocalDateTime timestamp) {
        this.id = id;
        this.streamId = streamId;
        this.eventType = eventType;
        this.metadata = metadata;
        this.timestamp = timestamp;
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

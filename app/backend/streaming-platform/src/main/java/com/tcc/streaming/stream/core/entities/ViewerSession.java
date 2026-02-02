package com.tcc.streaming.stream.core.entities;

import java.time.LocalDateTime;
import java.util.UUID;

public class ViewerSession {
    private Long id;
    private UUID streamId;
    private String viewerId;  // Pode ser IP, sessionId, userId, etc
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;

    // Constructor privado
    private ViewerSession() {
        this.joinedAt = LocalDateTime.now();
    }

    // Constructor completo para mapeamento
    public ViewerSession(Long id, UUID streamId, String viewerId, LocalDateTime joinedAt, LocalDateTime leftAt) {
        this.id = id;
        this.streamId = streamId;
        this.viewerId = viewerId;
        this.joinedAt = joinedAt;
        this.leftAt = leftAt;
    }

    // Factory method
    public static ViewerSession create(UUID streamId, String viewerId) {
        ViewerSession session = new ViewerSession();
        session.streamId = streamId;
        session.viewerId = viewerId;
        return session;
    }

    // Regras de negócio
    public void leave() {
        if (this.leftAt != null) {
            throw new IllegalStateException("Viewer already left");
        }
        this.leftAt = LocalDateTime.now();
    }

    public boolean isActive() {
        return this.leftAt == null;
    }

    public long getDurationSeconds() {
        LocalDateTime endTime = this.leftAt != null ? this.leftAt : LocalDateTime.now();
        return java.time.Duration.between(this.joinedAt, endTime).getSeconds();
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

    public String getViewerId() {
        return viewerId;
    }

    public void setViewerId(String viewerId) {
        this.viewerId = viewerId;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public LocalDateTime getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(LocalDateTime leftAt) {
        this.leftAt = leftAt;
    }
}

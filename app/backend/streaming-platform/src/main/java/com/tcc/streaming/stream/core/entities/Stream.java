package com.tcc.streaming.stream.core.entities;

import java.time.LocalDateTime;
import java.util.UUID;

public class Stream {
    private UUID id;
    private String title;
    private String description;
    private String streamKey;
    private String ownerId;
    private StreamStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer currentViewers;
    private Integer viewersPeak;

    // Constructor privado - use factory method
    private Stream() {
        this.id = UUID.randomUUID();
        this.status = StreamStatus.WAITING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.currentViewers = 0;
        this.viewersPeak = 0;
    }

    // Constructor completo para mapeamento
    public Stream(UUID id, String title, String description, String streamKey, String ownerId,
                  StreamStatus status, LocalDateTime createdAt, LocalDateTime updatedAt,
                  LocalDateTime startedAt, LocalDateTime endedAt, Integer currentViewers, Integer viewersPeak) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.streamKey = streamKey;
        this.ownerId = ownerId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.currentViewers = currentViewers;
        this.viewersPeak = viewersPeak;
    }

    // Factory method
    public static Stream create(String title, String description, String ownerId) {
        Stream stream = new Stream();
        stream.title = title;
        stream.description = description;
        stream.ownerId = ownerId;
        stream.streamKey = generateStreamKey();
        return stream;
    }

    // Regras de negócio
    public void start() {
        if (this.status == StreamStatus.LIVE) {
            return; // idempotente: reconexão do broadcaster
        }
        if (this.status != StreamStatus.WAITING) {
            throw new IllegalStateException("Stream can only be started from WAITING status, current: " + this.status);
        }
        this.status = StreamStatus.LIVE;
        this.startedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void end() {
        endStream(false);
    }

    public void forceEnd() {
        // Force end without status check - used for cleanup/delete
        endStream(true);
    }

    private void endStream(boolean force) {
        if (!force && this.status == StreamStatus.ENDED) {
            return; // idempotente: evita encerrar stream já finalizada
        }
        this.status = StreamStatus.ENDED;
        if (force || this.endedAt == null) {
            this.endedAt = LocalDateTime.now();
        }
        this.updatedAt = LocalDateTime.now();
    }

    public void restart() {
        if (this.status != StreamStatus.ENDED) {
            throw new IllegalStateException("Only ENDED streams can be restarted, current status: " + this.status);
        }
        this.status = StreamStatus.WAITING;
        this.startedAt = null;
        this.endedAt = null;
        this.currentViewers = 0;
        this.viewersPeak = 0;
        this.updatedAt = LocalDateTime.now();
    }

    public void incrementViewers() {
        this.currentViewers++;
        if (this.currentViewers > this.viewersPeak) {
            this.viewersPeak = this.currentViewers;
        }
    }

    public void decrementViewers() {
        if (this.currentViewers > 0) {
            this.currentViewers--;
        }
    }

    // Geração de stream key única
    private static String generateStreamKey() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    // Getters
    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStreamKey() {
        return streamKey;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public StreamStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public Integer getCurrentViewers() {
        return currentViewers;
    }

    public Integer getViewersPeak() {
        return viewersPeak;
    }
}

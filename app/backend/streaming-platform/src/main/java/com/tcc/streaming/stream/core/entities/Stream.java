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

    public String getRtmpUrl() {
        return "rtmp://localhost:1935/live";
    }

    public String getWatchUrl() {
        return "http://localhost:3001/watch/" + this.id;
    }

    // Geração de stream key única
    private static String generateStreamKey() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    // Getters e Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public void setStreamKey(String streamKey) {
        this.streamKey = streamKey;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public StreamStatus getStatus() {
        return status;
    }

    public void setStatus(StreamStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public Integer getCurrentViewers() {
        return currentViewers;
    }

    public void setCurrentViewers(Integer currentViewers) {
        this.currentViewers = currentViewers;
    }

    public Integer getViewersPeak() {
        return viewersPeak;
    }

    public void setViewersPeak(Integer viewersPeak) {
        this.viewersPeak = viewersPeak;
    }
}

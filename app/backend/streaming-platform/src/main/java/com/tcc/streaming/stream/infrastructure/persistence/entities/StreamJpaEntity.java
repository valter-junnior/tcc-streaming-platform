package com.tcc.streaming.stream.infrastructure.persistence.entities;

import com.tcc.streaming.stream.core.entities.StreamStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "streams")
public class StreamJpaEntity {

    @Id
    private UUID id;

    @Version
    private Long version;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "stream_key", nullable = false, unique = true, length = 16)
    private String streamKey;

    @Column(name = "owner_id", nullable = false, length = 36)
    private String ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StreamStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "current_viewers", nullable = false)
    private Integer currentViewers = 0;

    @Column(name = "viewers_peak", nullable = false)
    private Integer viewersPeak = 0;

    public StreamJpaEntity() {
    }

    public StreamJpaEntity(UUID id, String title, String description, String streamKey, String ownerId, StreamStatus status,
                           LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime startedAt, LocalDateTime endedAt, Integer currentViewers, Integer viewersPeak) {
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
        this.version = 0L;
    }

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

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

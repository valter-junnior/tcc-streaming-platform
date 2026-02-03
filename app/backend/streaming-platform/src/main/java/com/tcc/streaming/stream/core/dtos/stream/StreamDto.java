package com.tcc.streaming.stream.core.dtos.stream;

import com.tcc.streaming.stream.core.entities.StreamStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record StreamDto(
    UUID id,
    String title,
    String description,
    String streamKey,
    String ownerId,
    StreamStatus status,
    LocalDateTime createdAt,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    Integer currentViewers,
    Integer viewersPeak,
    String rtmpUrl,
    String watchUrl
) {}

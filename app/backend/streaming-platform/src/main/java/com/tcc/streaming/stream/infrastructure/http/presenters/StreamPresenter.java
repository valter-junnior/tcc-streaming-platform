package com.tcc.streaming.stream.infrastructure.http.presenters;

import com.tcc.streaming.stream.core.dtos.stream.StreamDto;
import com.tcc.streaming.stream.core.entities.StreamStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record StreamPresenter(
    UUID id,
    String title,
    String description,
    String streamKey,
    StreamStatus status,
    LocalDateTime createdAt,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    Integer currentViewers,
    Integer viewersPeak,
    String rtmpUrl,
    String watchUrl
) {
    public static StreamPresenter from(StreamDto dto) {
        return new StreamPresenter(
            dto.id(),
            dto.title(),
            dto.description(),
            dto.streamKey(),
            dto.status(),
            dto.createdAt(),
            dto.startedAt(),
            dto.endedAt(),
            dto.currentViewers(),
            dto.viewersPeak(),
            dto.rtmpUrl(),
            dto.watchUrl()
        );
    }
}

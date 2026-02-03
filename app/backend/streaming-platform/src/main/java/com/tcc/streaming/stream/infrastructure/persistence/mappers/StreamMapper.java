package com.tcc.streaming.stream.infrastructure.persistence.mappers;

import com.tcc.streaming.stream.core.entities.Stream;
import com.tcc.streaming.stream.infrastructure.persistence.entities.StreamJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class StreamMapper {

    public Stream toDomain(StreamJpaEntity entity) {
        return new Stream(
            entity.getId(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getStreamKey(),
            entity.getOwnerId(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getStartedAt(),
            entity.getEndedAt(),
            entity.getCurrentViewers(),
            entity.getViewersPeak()
        );
    }

    public StreamJpaEntity toJpaEntity(Stream domain) {
        return new StreamJpaEntity(
            domain.getId(),
            domain.getTitle(),
            domain.getDescription(),
            domain.getStreamKey(),
            domain.getOwnerId(),
            domain.getStatus(),
            domain.getCreatedAt(),
            domain.getUpdatedAt(),
            domain.getStartedAt(),
            domain.getEndedAt(),
            domain.getCurrentViewers(),
            domain.getViewersPeak()
        );
    }
}

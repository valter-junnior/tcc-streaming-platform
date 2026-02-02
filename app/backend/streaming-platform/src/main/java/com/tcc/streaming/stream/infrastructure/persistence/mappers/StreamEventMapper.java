package com.tcc.streaming.stream.infrastructure.persistence.mappers;

import com.tcc.streaming.stream.core.entities.StreamEvent;
import com.tcc.streaming.stream.infrastructure.persistence.entities.StreamEventJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class StreamEventMapper {

    public StreamEvent toDomain(StreamEventJpaEntity entity) {
        return new StreamEvent(
            entity.getId(),
            entity.getStreamId(),
            entity.getEventType(),
            entity.getMetadata(),
            entity.getTimestamp()
        );
    }

    public StreamEventJpaEntity toJpaEntity(StreamEvent domain) {
        return new StreamEventJpaEntity(
            domain.getId(),
            domain.getStreamId(),
            domain.getEventType(),
            domain.getMetadata(),
            domain.getTimestamp()
        );
    }
}

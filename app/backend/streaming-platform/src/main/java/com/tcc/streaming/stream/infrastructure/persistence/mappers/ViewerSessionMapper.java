package com.tcc.streaming.stream.infrastructure.persistence.mappers;

import com.tcc.streaming.stream.core.entities.ViewerSession;
import com.tcc.streaming.stream.infrastructure.persistence.entities.ViewerSessionJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ViewerSessionMapper {

    public ViewerSession toDomain(ViewerSessionJpaEntity entity) {
        return new ViewerSession(
            entity.getId(),
            entity.getStreamId(),
            entity.getViewerId(),
            entity.getJoinedAt(),
            entity.getLeftAt()
        );
    }

    public ViewerSessionJpaEntity toJpaEntity(ViewerSession domain) {
        return new ViewerSessionJpaEntity(
            domain.getId(),
            domain.getStreamId(),
            domain.getViewerId(),
            domain.getJoinedAt(),
            domain.getLeftAt()
        );
    }
}

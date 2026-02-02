package com.tcc.streaming.stream.infrastructure.persistence.repositories;

import com.tcc.streaming.stream.core.entities.StreamEventType;
import com.tcc.streaming.stream.infrastructure.persistence.entities.StreamEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StreamEventJpaRepository extends JpaRepository<StreamEventJpaEntity, Long> {
    
    List<StreamEventJpaEntity> findByStreamId(UUID streamId);
    
    List<StreamEventJpaEntity> findByStreamIdAndEventType(UUID streamId, StreamEventType eventType);
}

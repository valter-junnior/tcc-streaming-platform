package com.tcc.streaming.stream.infrastructure.persistence.repositories;

import com.tcc.streaming.stream.core.entities.StreamEvent;
import com.tcc.streaming.stream.core.entities.StreamEventType;
import com.tcc.streaming.stream.core.repositories.StreamEventRepository;
import com.tcc.streaming.stream.infrastructure.persistence.mappers.StreamEventMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class StreamEventRepositoryImpl implements StreamEventRepository {

    private final StreamEventJpaRepository jpaRepository;
    private final StreamEventMapper mapper;

    public StreamEventRepositoryImpl(StreamEventJpaRepository jpaRepository, StreamEventMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public StreamEvent save(StreamEvent event) {
        var entity = mapper.toJpaEntity(event);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<StreamEvent> findByStreamId(UUID streamId) {
        return jpaRepository.findByStreamId(streamId)
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<StreamEvent> findByStreamIdAndEventType(UUID streamId, StreamEventType eventType) {
        return jpaRepository.findByStreamIdAndEventType(streamId, eventType)
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }
}

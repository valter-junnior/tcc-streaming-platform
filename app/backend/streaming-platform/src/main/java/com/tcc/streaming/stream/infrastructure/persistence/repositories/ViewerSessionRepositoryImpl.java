package com.tcc.streaming.stream.infrastructure.persistence.repositories;

import com.tcc.streaming.stream.core.entities.ViewerSession;
import com.tcc.streaming.stream.core.repositories.ViewerSessionRepository;
import com.tcc.streaming.stream.infrastructure.persistence.mappers.ViewerSessionMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ViewerSessionRepositoryImpl implements ViewerSessionRepository {

    private final ViewerSessionJpaRepository jpaRepository;
    private final ViewerSessionMapper mapper;

    public ViewerSessionRepositoryImpl(ViewerSessionJpaRepository jpaRepository, ViewerSessionMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ViewerSession save(ViewerSession session) {
        var entity = mapper.toJpaEntity(session);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<ViewerSession> findById(Long id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public List<ViewerSession> findByStreamId(UUID streamId) {
        return jpaRepository.findByStreamId(streamId)
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<ViewerSession> findActiveByStreamId(UUID streamId) {
        return jpaRepository.findActiveByStreamId(streamId)
            .stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public Optional<ViewerSession> findActiveByStreamIdAndViewerId(UUID streamId, String viewerId) {
        return jpaRepository.findActiveByStreamIdAndViewerId(streamId, viewerId)
            .map(mapper::toDomain);
    }
}

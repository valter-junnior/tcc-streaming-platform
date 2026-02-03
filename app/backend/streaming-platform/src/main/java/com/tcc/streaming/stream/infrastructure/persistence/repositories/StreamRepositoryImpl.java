package com.tcc.streaming.stream.infrastructure.persistence.repositories;

import com.tcc.streaming.stream.core.entities.Stream;
import com.tcc.streaming.stream.core.repositories.StreamRepository;
import com.tcc.streaming.stream.infrastructure.persistence.mappers.StreamMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class StreamRepositoryImpl implements StreamRepository {

    private final StreamJpaRepository jpaRepository;
    private final StreamMapper mapper;

    public StreamRepositoryImpl(StreamJpaRepository jpaRepository, StreamMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Stream save(Stream stream) {
        var entity = mapper.toJpaEntity(stream);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Stream> findById(UUID id) {
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<Stream> findByStreamKey(String streamKey) {
        return jpaRepository.findByStreamKey(streamKey)
            .map(mapper::toDomain);
    }

    @Override
    public List<Stream> findByStatus(com.tcc.streaming.stream.core.entities.StreamStatus status) {
        return jpaRepository.findByStatus(status)
            .stream()
            .map(mapper::toDomain)
            .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<Stream> findByOwnerId(String ownerId) {
        return jpaRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId)
            .stream()
            .map(mapper::toDomain)
            .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public List<Stream> findInactiveStreams(java.time.LocalDateTime thresholdDate) {
        return jpaRepository.findInactiveStreams(thresholdDate)
            .stream()
            .map(mapper::toDomain)
            .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}

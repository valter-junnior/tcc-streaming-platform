package com.tcc.streaming.stream.core.repositories;

import com.tcc.streaming.stream.core.entities.Stream;
import com.tcc.streaming.stream.core.entities.StreamStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StreamRepository {
    Stream save(Stream stream);
    Optional<Stream> findById(UUID id);
    Optional<Stream> findByStreamKey(String streamKey);
    List<Stream> findByStatus(StreamStatus status);
    void deleteById(UUID id);
}

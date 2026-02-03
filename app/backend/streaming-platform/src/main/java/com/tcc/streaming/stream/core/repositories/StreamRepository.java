package com.tcc.streaming.stream.core.repositories;

import com.tcc.streaming.stream.core.entities.Stream;
import com.tcc.streaming.stream.core.entities.StreamStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StreamRepository {
    Stream save(Stream stream);
    Optional<Stream> findById(UUID id);
    Optional<Stream> findByStreamKey(String streamKey);
    List<Stream> findByStatus(StreamStatus status);
    List<Stream> findByOwnerId(String ownerId);
    List<Stream> findInactiveStreams(LocalDateTime thresholdDate);
    void deleteById(UUID id);
    
    /**
     * Incrementa viewers de forma atômica no banco de dados
     * @param id Stream ID
     * @return número de linhas afetadas (1 se sucesso, 0 se stream não existe)
     */
    int incrementViewersAtomic(UUID id);
    
    /**
     * Decrementa viewers de forma atômica no banco de dados
     * @param id Stream ID
     * @return número de linhas afetadas (1 se sucesso, 0 se stream não existe)
     */
    int decrementViewersAtomic(UUID id);
}

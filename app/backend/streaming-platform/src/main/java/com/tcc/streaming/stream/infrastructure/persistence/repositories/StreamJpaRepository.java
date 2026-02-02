package com.tcc.streaming.stream.infrastructure.persistence.repositories;

import com.tcc.streaming.stream.infrastructure.persistence.entities.StreamJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StreamJpaRepository extends JpaRepository<StreamJpaEntity, UUID> {
    
    Optional<StreamJpaEntity> findByStreamKey(String streamKey);
}

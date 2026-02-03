package com.tcc.streaming.stream.infrastructure.persistence.repositories;

import com.tcc.streaming.stream.infrastructure.persistence.entities.StreamJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StreamJpaRepository extends JpaRepository<StreamJpaEntity, UUID> {
    
    Optional<StreamJpaEntity> findByStreamKey(String streamKey);
    
    List<StreamJpaEntity> findByStatus(com.tcc.streaming.stream.core.entities.StreamStatus status);
    
    List<StreamJpaEntity> findByOwnerIdOrderByCreatedAtDesc(String ownerId);
    
    @Query("SELECT s FROM StreamJpaEntity s WHERE s.status IN ('WAITING', 'LIVE') AND s.updatedAt < :thresholdDate")
    List<StreamJpaEntity> findInactiveStreams(@Param("thresholdDate") LocalDateTime thresholdDate);
}

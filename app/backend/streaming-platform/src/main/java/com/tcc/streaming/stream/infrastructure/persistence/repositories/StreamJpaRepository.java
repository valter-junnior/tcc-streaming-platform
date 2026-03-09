package com.tcc.streaming.stream.infrastructure.persistence.repositories;

import com.tcc.streaming.stream.infrastructure.persistence.entities.StreamJpaEntity;
import com.tcc.streaming.stream.core.entities.StreamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StreamJpaRepository extends JpaRepository<StreamJpaEntity, UUID> {
    
    Optional<StreamJpaEntity> findByStreamKey(String streamKey);
    
    List<StreamJpaEntity> findByStatus(com.tcc.streaming.stream.core.entities.StreamStatus status);
    
    List<StreamJpaEntity> findByOwnerIdOrderByCreatedAtDesc(String ownerId);
    
    @Query("SELECT s FROM StreamJpaEntity s WHERE s.status IN :statuses AND s.updatedAt < :thresholdDate")
    List<StreamJpaEntity> findInactiveStreams(@Param("statuses") Collection<StreamStatus> statuses, @Param("thresholdDate") LocalDateTime thresholdDate);
    
    @Modifying(clearAutomatically = true)
    @Query("UPDATE StreamJpaEntity s SET s.currentViewers = s.currentViewers + 1, s.viewersPeak = CASE WHEN s.currentViewers + 1 > s.viewersPeak THEN s.currentViewers + 1 ELSE s.viewersPeak END, s.updatedAt = CURRENT_TIMESTAMP WHERE s.id = :id")
    int incrementViewersAtomic(@Param("id") UUID id);
    
    @Modifying(clearAutomatically = true)
    @Query("UPDATE StreamJpaEntity s SET s.currentViewers = CASE WHEN s.currentViewers > 0 THEN s.currentViewers - 1 ELSE 0 END, s.updatedAt = CURRENT_TIMESTAMP WHERE s.id = :id")
    int decrementViewersAtomic(@Param("id") UUID id);
}

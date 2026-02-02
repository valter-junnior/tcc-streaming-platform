package com.tcc.streaming.stream.infrastructure.persistence.repositories;

import com.tcc.streaming.stream.infrastructure.persistence.entities.ViewerSessionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ViewerSessionJpaRepository extends JpaRepository<ViewerSessionJpaEntity, Long> {
    
    List<ViewerSessionJpaEntity> findByStreamId(UUID streamId);
    
    @Query("SELECT v FROM ViewerSessionJpaEntity v WHERE v.streamId = :streamId AND v.leftAt IS NULL")
    List<ViewerSessionJpaEntity> findActiveByStreamId(UUID streamId);
    
    @Query("SELECT v FROM ViewerSessionJpaEntity v WHERE v.streamId = :streamId AND v.viewerId = :viewerId AND v.leftAt IS NULL")
    Optional<ViewerSessionJpaEntity> findActiveByStreamIdAndViewerId(UUID streamId, String viewerId);
}

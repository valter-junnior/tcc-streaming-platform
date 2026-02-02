package com.tcc.streaming.stream.core.repositories;

import com.tcc.streaming.stream.core.entities.ViewerSession;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ViewerSessionRepository {
    ViewerSession save(ViewerSession session);
    Optional<ViewerSession> findById(Long id);
    List<ViewerSession> findByStreamId(UUID streamId);
    List<ViewerSession> findActiveByStreamId(UUID streamId);
    Optional<ViewerSession> findActiveByStreamIdAndViewerId(UUID streamId, String viewerId);
}

package com.tcc.streaming.stream.core.repositories;

import com.tcc.streaming.stream.core.entities.StreamEvent;
import com.tcc.streaming.stream.core.entities.StreamEventType;

import java.util.List;
import java.util.UUID;

public interface StreamEventRepository {
    StreamEvent save(StreamEvent event);
    List<StreamEvent> findByStreamId(UUID streamId);
    List<StreamEvent> findByStreamIdAndEventType(UUID streamId, StreamEventType eventType);
}

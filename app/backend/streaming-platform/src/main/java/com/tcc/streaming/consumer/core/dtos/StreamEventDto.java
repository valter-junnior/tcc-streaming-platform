package com.tcc.streaming.consumer.core.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO para eventos de stream recebidos do message broker
 */
public record StreamEventDto(
    UUID streamId,
    String eventType,
    String streamKey,
    String title,
    Integer viewersPeak,
    String viewerId,
    LocalDateTime timestamp
) {
    public StreamEventDto {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}

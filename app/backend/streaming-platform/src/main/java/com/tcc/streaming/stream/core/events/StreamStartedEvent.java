package com.tcc.streaming.stream.core.events;

import java.util.UUID;

/**
 * Evento de domínio para stream iniciada
 */
public record StreamStartedEvent(
    UUID streamId,
    String streamKey
) {}

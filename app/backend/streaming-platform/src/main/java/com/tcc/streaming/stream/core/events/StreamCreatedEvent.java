package com.tcc.streaming.stream.core.events;

import java.util.UUID;

/**
 * Evento de domínio para stream criada
 */
public record StreamCreatedEvent(
    UUID streamId,
    String streamKey,
    String title
) {}

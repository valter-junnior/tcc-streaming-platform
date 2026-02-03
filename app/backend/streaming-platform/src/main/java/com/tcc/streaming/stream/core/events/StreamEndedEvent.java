package com.tcc.streaming.stream.core.events;

import java.util.UUID;

/**
 * Evento de domínio para stream encerrada
 */
public record StreamEndedEvent(
    UUID streamId,
    String streamKey,
    Integer viewersPeak
) {}

package com.tcc.streaming.stream.core.usecases;

import java.util.UUID;

public interface RestartStreamUseCase {
    /**
     * Reinicia uma stream com status ENDED, resetando métricas de viewers
     * @param id ID da stream
     * @throws com.tcc.streaming.stream.core.exceptions.StreamNotFoundException se stream não existir
     * @throws IllegalStateException se a stream não estiver com status ENDED
     */
    void restartStream(UUID id);
}

package com.tcc.streaming.stream.core.usecases;

import java.util.UUID;

public interface DeleteStreamUseCase {
    /**
     * Deleta uma stream
     * @param id ID da stream
     * @param ownerId ID do usuário dono da stream
     * @throws com.tcc.streaming.stream.core.exceptions.StreamNotFoundException se stream não existir
     * @throws com.tcc.streaming.stream.core.exceptions.UnauthorizedException se ownerId não corresponder
     * @throws IllegalStateException se stream estiver com status LIVE
     */
    void deleteStream(UUID id, String ownerId);
}

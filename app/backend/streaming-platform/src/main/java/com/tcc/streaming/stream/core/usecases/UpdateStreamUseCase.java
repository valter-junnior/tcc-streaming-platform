package com.tcc.streaming.stream.core.usecases;

import com.tcc.streaming.stream.core.dtos.stream.StreamDto;
import com.tcc.streaming.stream.core.dtos.stream.UpdateStreamDto;

/**
 * Use case para atualizar informações de uma stream
 */
public interface UpdateStreamUseCase {
    /**
     * Atualiza título e descrição de uma stream
     * @param dto Dados para atualização com streamId, ownerId, title e description
     * @return Stream atualizada
     * @throws com.tcc.streaming.stream.core.exceptions.StreamNotFoundException se stream não existir
     * @throws com.tcc.streaming.stream.core.exceptions.UnauthorizedException se ownerId não corresponder
     */
    StreamDto update(UpdateStreamDto dto);
}

package com.tcc.streaming.stream.core.usecases;

import com.tcc.streaming.stream.core.dtos.stream.StreamDto;

import java.util.List;

/**
 * Use case para listar todas as streams de um usuário específico
 */
public interface ListUserStreamsUseCase {
    /**
     * Lista todas as streams criadas por um usuário
     * @param ownerId ID do usuário dono das streams
     * @return Lista de streams do usuário ordenadas por data de criação (mais recentes primeiro)
     */
    List<StreamDto> listByOwner(String ownerId);
}

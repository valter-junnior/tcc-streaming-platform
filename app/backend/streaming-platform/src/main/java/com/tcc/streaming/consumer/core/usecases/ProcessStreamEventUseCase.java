package com.tcc.streaming.consumer.core.usecases;

import com.tcc.streaming.consumer.core.dtos.StreamEventDto;

/**
 * Use case para processar eventos de stream
 */
public interface ProcessStreamEventUseCase {
    void execute(StreamEventDto event);
}

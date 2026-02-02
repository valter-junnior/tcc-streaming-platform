package com.tcc.streaming.stream.core.usecases;

import com.tcc.streaming.stream.core.dtos.stream.StreamDto;

import java.util.UUID;

public interface GetStreamUseCase {
    StreamDto execute(UUID id);
}

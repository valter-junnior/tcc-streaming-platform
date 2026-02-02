package com.tcc.streaming.stream.core.usecases;

import com.tcc.streaming.stream.core.dtos.stream.StreamStatusDto;

import java.util.UUID;

public interface GetStreamStatusUseCase {
    StreamStatusDto getStatus(UUID id);
}

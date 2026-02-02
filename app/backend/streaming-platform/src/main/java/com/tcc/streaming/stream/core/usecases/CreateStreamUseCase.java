package com.tcc.streaming.stream.core.usecases;

import com.tcc.streaming.stream.core.dtos.stream.CreateStreamDto;
import com.tcc.streaming.stream.core.dtos.stream.StreamDto;

public interface CreateStreamUseCase {
    StreamDto execute(CreateStreamDto dto);
}

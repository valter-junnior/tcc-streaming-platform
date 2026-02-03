package com.tcc.streaming.stream.core.usecases;

import com.tcc.streaming.stream.core.dtos.stream.StreamDto;

import java.util.List;

public interface ListLiveStreamsUseCase {
    List<StreamDto> execute();
}

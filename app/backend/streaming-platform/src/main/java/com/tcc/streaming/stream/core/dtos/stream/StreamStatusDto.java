package com.tcc.streaming.stream.core.dtos.stream;

import com.tcc.streaming.stream.core.entities.StreamStatus;

import java.util.UUID;

public record StreamStatusDto(
    UUID id,
    StreamStatus status,
    Integer currentViewers,
    Integer viewersPeak,
    String watchUrl
) {}

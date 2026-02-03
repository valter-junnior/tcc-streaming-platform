package com.tcc.streaming.stream.core.dtos.stream;

import java.util.UUID;

public record UpdateStreamDto(
    UUID streamId,
    String ownerId,
    String title,
    String description
) {}

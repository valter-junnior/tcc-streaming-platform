package com.tcc.streaming.stream.core.exceptions;

import com.tcc.streaming.common.core.exceptions.NotFoundException;

import java.util.UUID;

public class StreamNotFoundException extends NotFoundException {
    public StreamNotFoundException(UUID id) {
        super("Stream not found: " + id);
    }

    public StreamNotFoundException(String streamKey) {
        super("Stream not found with key: " + streamKey);
    }
}

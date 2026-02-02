package com.tcc.streaming.stream.core.entities;

public enum StreamEventType {
    CREATED,        // Stream foi criada
    STARTED,        // Stream iniciou transmissão
    ENDED,          // Stream encerrou transmissão
    VIEWER_JOINED,  // Viewer entrou na stream
    VIEWER_LEFT     // Viewer saiu da stream
}

package com.tcc.streaming.stream.core.entities;

public enum StreamStatus {
    WAITING,    // Stream criada, aguardando conexão RTMP
    LIVE,       // Stream ao vivo (transmitindo)
    ENDED       // Stream encerrada
}

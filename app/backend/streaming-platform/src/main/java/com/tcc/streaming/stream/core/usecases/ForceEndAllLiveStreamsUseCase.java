package com.tcc.streaming.stream.core.usecases;

public interface ForceEndAllLiveStreamsUseCase {
    /**
     * Força o encerramento de todas as streams com status LIVE.
     * Útil após reinicialização do servidor RTMP.
     * @return Quantidade de streams encerradas
     */
    int forceEndAllLiveStreams();
}

package com.tcc.streaming.common.infrastructure.transcoding;

import java.util.List;

/**
 * Gateway de abstração para o transcodificador de vídeo.
 * Permite alternar entre FFmpeg e GStreamer sem mudanças no restante do sistema.
 */
public interface TranscoderGateway {

    /** Tipo do transcodificador ativo. */
    TranscoderType getType();

    /** Nome legível do transcodificador (ex.: "FFmpeg", "GStreamer"). */
    String getName();

    /** Presets de qualidade suportados pelo transcodificador. */
    List<QualityPreset> getQualityPresets();
}

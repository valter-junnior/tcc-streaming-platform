package com.tcc.streaming.common.infrastructure.transcoding.ffmpeg;

import com.tcc.streaming.common.infrastructure.transcoding.QualityPreset;
import com.tcc.streaming.common.infrastructure.transcoding.TranscoderGateway;
import com.tcc.streaming.common.infrastructure.transcoding.TranscoderType;

import java.util.Arrays;
import java.util.List;

/**
 * Implementação do TranscoderGateway para FFmpeg.
 * Registrada como bean principal quando {@code transcoder.type=ffmpeg} (padrão).
 */
public class FFmpegTranscoderGateway implements TranscoderGateway {

    @Override
    public TranscoderType getType() {
        return TranscoderType.FFMPEG;
    }

    @Override
    public String getName() {
        return "FFmpeg";
    }

    @Override
    public List<QualityPreset> getQualityPresets() {
        return Arrays.asList(QualityPreset.values());
    }
}

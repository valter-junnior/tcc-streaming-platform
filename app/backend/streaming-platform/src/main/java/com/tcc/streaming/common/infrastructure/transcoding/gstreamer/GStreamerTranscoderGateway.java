package com.tcc.streaming.common.infrastructure.transcoding.gstreamer;

import com.tcc.streaming.common.infrastructure.transcoding.QualityPreset;
import com.tcc.streaming.common.infrastructure.transcoding.TranscoderGateway;
import com.tcc.streaming.common.infrastructure.transcoding.TranscoderType;

import java.util.Arrays;
import java.util.List;

/**
 * Implementação do TranscoderGateway para GStreamer.
 * Registrada como bean quando {@code transcoder.type=gstreamer}.
 */
public class GStreamerTranscoderGateway implements TranscoderGateway {

    @Override
    public TranscoderType getType() {
        return TranscoderType.GSTREAMER;
    }

    @Override
    public String getName() {
        return "GStreamer";
    }

    @Override
    public List<QualityPreset> getQualityPresets() {
        return Arrays.asList(QualityPreset.values());
    }
}

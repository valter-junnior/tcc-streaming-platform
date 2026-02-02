package com.tcc.streaming.common.infrastructure.transcoding;

import java.util.List;

/**
 * Configuration for transcoding operations.
 * Contains all settings needed to transcode a stream.
 */
public class TranscoderConfig {
    
    private final String provider;
    private final List<QualityPreset> qualityPresets;
    private final int segmentDuration;  // seconds
    private final int playlistLength;    // seconds
    private final String outputPath;
    private final boolean enableHardwareAcceleration;
    
    public TranscoderConfig(
            String provider,
            List<QualityPreset> qualityPresets,
            int segmentDuration,
            int playlistLength,
            String outputPath,
            boolean enableHardwareAcceleration) {
        this.provider = provider;
        this.qualityPresets = qualityPresets;
        this.segmentDuration = segmentDuration;
        this.playlistLength = playlistLength;
        this.outputPath = outputPath;
        this.enableHardwareAcceleration = enableHardwareAcceleration;
    }
    
    public String getProvider() {
        return provider;
    }
    
    public List<QualityPreset> getQualityPresets() {
        return qualityPresets;
    }
    
    public int getSegmentDuration() {
        return segmentDuration;
    }
    
    public int getPlaylistLength() {
        return playlistLength;
    }
    
    public String getOutputPath() {
        return outputPath;
    }
    
    public boolean isEnableHardwareAcceleration() {
        return enableHardwareAcceleration;
    }
}

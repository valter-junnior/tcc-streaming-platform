package com.tcc.streaming.common.infrastructure.transcoding;

/**
 * Quality preset for video transcoding.
 * Defines resolution, bitrate, and encoder settings for each quality level.
 */
public class QualityPreset {
    
    private final String name;
    private final int width;
    private final int height;
    private final int videoBitrate;  // kbps
    private final int audioBitrate;  // kbps
    private final String encoderPreset;
    
    public QualityPreset(
            String name,
            int width,
            int height,
            int videoBitrate,
            int audioBitrate,
            String encoderPreset) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.videoBitrate = videoBitrate;
        this.audioBitrate = audioBitrate;
        this.encoderPreset = encoderPreset;
    }
    
    public String getName() {
        return name;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }
    
    public int getVideoBitrate() {
        return videoBitrate;
    }
    
    public int getAudioBitrate() {
        return audioBitrate;
    }
    
    public String getEncoderPreset() {
        return encoderPreset;
    }
    
    public String getResolution() {
        return width + "x" + height;
    }
    
    @Override
    public String toString() {
        return String.format("%s (%dx%d @ %dkbps)", name, width, height, videoBitrate);
    }
    
    // Standard presets
    public static QualityPreset PRESET_1080P = new QualityPreset(
        "1080p", 1920, 1080, 5000, 128, "medium"
    );
    
    public static QualityPreset PRESET_720P = new QualityPreset(
        "720p", 1280, 720, 2800, 128, "medium"
    );
    
    public static QualityPreset PRESET_480P = new QualityPreset(
        "480p", 854, 480, 1400, 128, "fast"
    );
    
    public static QualityPreset PRESET_360P = new QualityPreset(
        "360p", 640, 360, 800, 96, "faster"
    );
}

package com.tcc.streaming.common.infrastructure.transcoding;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for video transcoding.
 * Allows switching between different transcoding providers via application.yml
 */
@Configuration
@ConfigurationProperties(prefix = "streaming.transcoding")
public class TranscodingProperties {
    
    /**
     * Transcoding provider: "ffmpeg" or "gstreamer"
     */
    private String provider = "ffmpeg";
    
    /**
     * HLS segment duration in seconds
     */
    private int segmentDuration = 6;
    
    /**
     * HLS playlist length in seconds
     */
    private int playlistLength = 60;
    
    /**
     * Output path for HLS files
     */
    private String outputPath = "/tmp/hls";
    
    /**
     * Enable hardware acceleration
     */
    private boolean hardwareAcceleration = false;
    
    /**
     * Quality presets configuration
     */
    private List<QualityPresetConfig> qualities = new ArrayList<>();
    
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
    }
    
    public int getSegmentDuration() {
        return segmentDuration;
    }
    
    public void setSegmentDuration(int segmentDuration) {
        this.segmentDuration = segmentDuration;
    }
    
    public int getPlaylistLength() {
        return playlistLength;
    }
    
    public void setPlaylistLength(int playlistLength) {
        this.playlistLength = playlistLength;
    }
    
    public String getOutputPath() {
        return outputPath;
    }
    
    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }
    
    public boolean isHardwareAcceleration() {
        return hardwareAcceleration;
    }
    
    public void setHardwareAcceleration(boolean hardwareAcceleration) {
        this.hardwareAcceleration = hardwareAcceleration;
    }
    
    public List<QualityPresetConfig> getQualities() {
        return qualities;
    }
    
    public void setQualities(List<QualityPresetConfig> qualities) {
        this.qualities = qualities;
    }
    
    /**
     * Configuration for a single quality preset
     */
    public static class QualityPresetConfig {
        private String name;
        private int width;
        private int height;
        private int videoBitrate;
        private int audioBitrate;
        private String encoderPreset;
        
        // Getters and Setters
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public int getWidth() {
            return width;
        }
        
        public void setWidth(int width) {
            this.width = width;
        }
        
        public int getHeight() {
            return height;
        }
        
        public void setHeight(int height) {
            this.height = height;
        }
        
        public int getVideoBitrate() {
            return videoBitrate;
        }
        
        public void setVideoBitrate(int videoBitrate) {
            this.videoBitrate = videoBitrate;
        }
        
        public int getAudioBitrate() {
            return audioBitrate;
        }
        
        public void setAudioBitrate(int audioBitrate) {
            this.audioBitrate = audioBitrate;
        }
        
        public String getEncoderPreset() {
            return encoderPreset;
        }
        
        public void setEncoderPreset(String encoderPreset) {
            this.encoderPreset = encoderPreset;
        }
    }
}

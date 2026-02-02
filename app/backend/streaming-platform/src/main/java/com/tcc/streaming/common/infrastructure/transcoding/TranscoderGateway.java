package com.tcc.streaming.common.infrastructure.transcoding;

/**
 * Gateway interface for video transcoding operations.
 * This abstraction allows switching between different transcoding implementations
 * (e.g., FFmpeg, GStreamer) without changing business logic.
 * 
 * @see FFmpegTranscoderGateway
 */
public interface TranscoderGateway {
    
    /**
     * Get the transcoder provider type.
     * 
     * @return Provider name (e.g., "ffmpeg", "gstreamer")
     */
    String getProvider();
    
    /**
     * Get transcoder configuration.
     * 
     * @return Transcoder configuration
     */
    TranscoderConfig getConfig();
    
    /**
     * Generate the transcoding command for a given stream.
     * 
     * @param streamKey The stream key
     * @param inputUrl RTMP input URL
     * @return Command to execute transcoding
     */
    String generateTranscodingCommand(String streamKey, String inputUrl);
    
    /**
     * Validate if the transcoder is available and properly configured.
     * 
     * @return true if transcoder is available
     */
    boolean isAvailable();
    
    /**
     * Get the version of the transcoder.
     * 
     * @return Version string
     */
    String getVersion();
    
    /**
     * Get the output path for HLS segments of a specific stream.
     * 
     * @param streamKey The stream key
     * @return Output path for HLS files
     */
    String getOutputPath(String streamKey);
}

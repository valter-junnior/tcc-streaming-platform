package com.tcc.streaming.common.infrastructure.transcoding.ffmpeg;

import com.tcc.streaming.common.infrastructure.transcoding.QualityPreset;
import com.tcc.streaming.common.infrastructure.transcoding.TranscoderConfig;
import com.tcc.streaming.common.infrastructure.transcoding.TranscoderGateway;
import com.tcc.streaming.common.infrastructure.transcoding.TranscodingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FFmpeg implementation of the transcoder gateway.
 * Handles video transcoding using FFmpeg with HLS output.
 */
@Component
@ConditionalOnProperty(name = "streaming.transcoding.provider", havingValue = "ffmpeg", matchIfMissing = true)
public class FFmpegTranscoderGateway implements TranscoderGateway {
    
    private static final Logger log = LoggerFactory.getLogger(FFmpegTranscoderGateway.class);
    
    private final TranscodingProperties properties;
    private final TranscoderConfig config;
    
    public FFmpegTranscoderGateway(TranscodingProperties properties) {
        this.properties = properties;
        this.config = buildConfig();
    }
    
    private TranscoderConfig buildConfig() {
        List<QualityPreset> presets = new ArrayList<>();
        
        // Use configured presets or defaults
        if (properties.getQualities().isEmpty()) {
            // Default presets
            presets.add(QualityPreset.PRESET_1080P);
            presets.add(QualityPreset.PRESET_720P);
            presets.add(QualityPreset.PRESET_480P);
            presets.add(QualityPreset.PRESET_360P);
        } else {
            // Convert configured presets
            for (TranscodingProperties.QualityPresetConfig config : properties.getQualities()) {
                presets.add(new QualityPreset(
                    config.getName(),
                    config.getWidth(),
                    config.getHeight(),
                    config.getVideoBitrate(),
                    config.getAudioBitrate(),
                    config.getEncoderPreset()
                ));
            }
        }
        
        return new TranscoderConfig(
            "ffmpeg",
            presets,
            properties.getSegmentDuration(),
            properties.getPlaylistLength(),
            properties.getOutputPath(),
            properties.isHardwareAcceleration()
        );
    }
    
    @Override
    public String getProvider() {
        return "ffmpeg";
    }
    
    @Override
    public TranscoderConfig getConfig() {
        return config;
    }
    
    @Override
    public String generateTranscodingCommand(String streamKey, String inputUrl) {
        StringBuilder cmd = new StringBuilder();
        
        cmd.append("ffmpeg");
        cmd.append(" -i ").append(inputUrl);
        
        // Hardware acceleration if enabled
        if (config.isEnableHardwareAcceleration()) {
            cmd.append(" -hwaccel auto");
        }
        
        // Global options
        cmd.append(" -c:a aac");
        cmd.append(" -ar 48000");
        cmd.append(" -g 48");
        cmd.append(" -keyint_min 48");
        cmd.append(" -sc_threshold 0");
        
        // HLS options
        cmd.append(" -f hls");
        cmd.append(" -hls_time ").append(config.getSegmentDuration());
        cmd.append(" -hls_list_size ").append(config.getPlaylistLength() / config.getSegmentDuration());
        cmd.append(" -hls_flags delete_segments+append_list");
        
        String outputDir = getOutputPath(streamKey);
        
        // Generate variant streams
        List<QualityPreset> presets = config.getQualityPresets();
        
        // Build filter complex for multiple outputs
        StringBuilder filterComplex = new StringBuilder();
        StringBuilder variantStreams = new StringBuilder();
        StringBuilder streamMap = new StringBuilder();
        
        for (int i = 0; i < presets.size(); i++) {
            QualityPreset preset = presets.get(i);
            
            // Video filter
            filterComplex.append("[v:0]scale=w=").append(preset.getWidth())
                        .append(":h=").append(preset.getHeight())
                        .append("[v").append(i).append("]; ");
            
            // Stream mapping
            streamMap.append("-map [v").append(i).append("] ");
            streamMap.append("-c:v:").append(i).append(" libx264 ");
            streamMap.append("-b:v:").append(i).append(" ").append(preset.getVideoBitrate()).append("k ");
            streamMap.append("-preset ").append(preset.getEncoderPreset()).append(" ");
            streamMap.append("-map a:0 ");
            streamMap.append("-c:a:").append(i).append(" aac ");
            streamMap.append("-b:a:").append(i).append(" ").append(preset.getAudioBitrate()).append("k ");
            
            // Variant stream definition
            if (i > 0) {
                variantStreams.append(",");
            }
            variantStreams.append("v:").append(i).append(",a:").append(i);
        }
        
        // Remove trailing semicolon and space
        String filterComplexStr = filterComplex.toString().trim();
        if (filterComplexStr.endsWith(";")) {
            filterComplexStr = filterComplexStr.substring(0, filterComplexStr.length() - 1);
        }
        
        cmd.append(" -filter_complex \"").append(filterComplexStr).append("\"");
        cmd.append(" ").append(streamMap.toString().trim());
        
        // Master playlist
        cmd.append(" -master_pl_name master.m3u8");
        cmd.append(" -var_stream_map \"").append(variantStreams).append("\"");
        
        // Output pattern
        cmd.append(" -hls_segment_filename \"").append(outputDir).append("/v%v/segment_%03d.ts\"");
        cmd.append(" \"").append(outputDir).append("/v%v/playlist.m3u8\"");
        
        return cmd.toString();
    }
    
    @Override
    public boolean isAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            log.warn("FFmpeg not available: {}", e.getMessage());
            return false;
        }
    }
    
    @Override
    public String getVersion() {
        try {
            Process process = Runtime.getRuntime().exec("ffmpeg -version");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String firstLine = reader.readLine();
            return firstLine != null ? firstLine : "unknown";
        } catch (Exception e) {
            log.error("Failed to get FFmpeg version", e);
            return "unknown";
        }
    }
    
    @Override
    public String getOutputPath(String streamKey) {
        return config.getOutputPath() + "/" + streamKey;
    }
}

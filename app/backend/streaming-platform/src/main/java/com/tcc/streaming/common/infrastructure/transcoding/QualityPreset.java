package com.tcc.streaming.common.infrastructure.transcoding;

/**
 * Presets de qualidade de vídeo para transcodificação HLS multi-bitrate.
 * Mantém os mesmos valores para FFmpeg e GStreamer, garantindo comparação justa.
 */
public enum QualityPreset {

    V0("1080p", 1920, 1080, 5000, 128, "v0"),
    V1("720p",  1280,  720, 2800, 128, "v1"),
    V2("480p",   854,  480, 1400,  96, "v2"),
    V3("360p",   640,  360,  800,  96, "v3");

    private final String label;
    private final int width;
    private final int height;
    private final int videoBitrateKbps;
    private final int audioBitrateKbps;
    private final String outputDir;

    QualityPreset(String label, int width, int height,
                  int videoBitrateKbps, int audioBitrateKbps, String outputDir) {
        this.label = label;
        this.width = width;
        this.height = height;
        this.videoBitrateKbps = videoBitrateKbps;
        this.audioBitrateKbps = audioBitrateKbps;
        this.outputDir = outputDir;
    }

    public String getLabel() {
        return label;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getVideoBitrateKbps() {
        return videoBitrateKbps;
    }

    public int getAudioBitrateKbps() {
        return audioBitrateKbps;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public String getResolution() {
        return width + "x" + height;
    }

    @Override
    public String toString() {
        return label + " (" + getResolution() + " @ " + videoBitrateKbps + "kbps)";
    }
}

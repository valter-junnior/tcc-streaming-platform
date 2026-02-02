package com.tcc.streaming.common.infrastructure.transcoding;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for QualityPreset.
 */
class QualityPresetTest {

    @Test
    void shouldCreateCustomPreset() {
        QualityPreset preset = new QualityPreset(
            "test", 1920, 1080, 5000, 128, "medium"
        );

        assertThat(preset.getName()).isEqualTo("test");
        assertThat(preset.getWidth()).isEqualTo(1920);
        assertThat(preset.getHeight()).isEqualTo(1080);
        assertThat(preset.getVideoBitrate()).isEqualTo(5000);
        assertThat(preset.getAudioBitrate()).isEqualTo(128);
        assertThat(preset.getEncoderPreset()).isEqualTo("medium");
    }

    @Test
    void shouldGenerateCorrectResolution() {
        QualityPreset preset = new QualityPreset(
            "720p", 1280, 720, 2800, 128, "medium"
        );

        assertThat(preset.getResolution()).isEqualTo("1280x720");
    }

    @Test
    void shouldHaveStandardPresets() {
        assertThat(QualityPreset.PRESET_1080P).isNotNull();
        assertThat(QualityPreset.PRESET_1080P.getName()).isEqualTo("1080p");
        assertThat(QualityPreset.PRESET_1080P.getWidth()).isEqualTo(1920);
        assertThat(QualityPreset.PRESET_1080P.getHeight()).isEqualTo(1080);

        assertThat(QualityPreset.PRESET_720P).isNotNull();
        assertThat(QualityPreset.PRESET_720P.getName()).isEqualTo("720p");

        assertThat(QualityPreset.PRESET_480P).isNotNull();
        assertThat(QualityPreset.PRESET_480P.getName()).isEqualTo("480p");

        assertThat(QualityPreset.PRESET_360P).isNotNull();
        assertThat(QualityPreset.PRESET_360P.getName()).isEqualTo("360p");
    }

    @Test
    void shouldGenerateCorrectToString() {
        QualityPreset preset = QualityPreset.PRESET_1080P;
        String result = preset.toString();

        assertThat(result).contains("1080p");
        assertThat(result).contains("1920");
        assertThat(result).contains("1080");
        assertThat(result).contains("5000");
    }
}

package com.deepguard.scan.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "detection_frames")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectionFrame {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detect_result_id", nullable = false)
    private DetectionResult detectionResult;

    // second in video
    @Column(name = "frame_timestamp", nullable = false)
    private Float frameTimestamp;

    @Column(name = "suspicion_score", nullable = false)
    private Float suspicionScore;

    @Column(name = "frame_index")
    private Integer frameIndex;

    @Column(name = "ai_generated_score")
    private BigDecimal aiGeneratedScore;

    @Column(name = "not_ai_generated_score")
    private BigDecimal notAiGeneratedScore;

    @Column(name = "deepfake_score")
    private BigDecimal deepfakeScore;

    @Column(name = "attributed_generator")
    private String attributedGenerator;

    @Column(name = "ai_generated_audio_score")
    private BigDecimal aiGeneratedAudioScore;

    @Column(name = "not_ai_generated_audio_score")
    private BigDecimal notAiGeneratedAudioScore;

    @Column(name = "frame_image_url")
    private String frameImageUrl;

    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
    }

}



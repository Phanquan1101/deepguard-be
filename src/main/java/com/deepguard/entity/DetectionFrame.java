package com.deepguard.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "detection_frames")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectionFrame {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "detect_result_id", nullable = false)
    private DetectionResult detectionResult;

    // second in video
    @Column(name = "frame_timestamp", nullable = false)
    private Float frameTimestamp;

    @Column(name = "suspicion_score", nullable = false)
    private Float suspicionScore;

    @Column(name = "frame_image_url")
    private String frameImageUrl;

}

package com.deepguard.scan.entity;

import jakarta.persistence.*;
import lombok.*;
import com.deepguard.scan.enums.DetectionLabel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "detection_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DetectionResult {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_job_id", nullable = false, unique = true)
    private ScanJob scanJob;

    @Column(name = "fake_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal fakeScore;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_label", nullable = false)
    private DetectionLabel resultLabel;

    @Column(name = "model_version", nullable = false)
    private String modelVersion;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        processedAt = LocalDateTime.now();
    }

}




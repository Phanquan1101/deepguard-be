package com.deepguard.scan.entity;

import jakarta.persistence.*;
import lombok.*;
import com.deepguard.auth.entity.User;
import com.deepguard.media.entity.MediaFile;
import com.deepguard.scan.enums.ScanJobStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "scan_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScanJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id", nullable = false)
    private MediaFile mediaFile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "error_loggings", columnDefinition = "TEXT")
    private String errorLoggings;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScanJobStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

}




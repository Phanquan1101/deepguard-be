package com.deepguard.media.entity;

import jakarta.persistence.*;
import lombok.*;
import com.deepguard.auth.entity.User;
import com.deepguard.media.enums.FileType;
import com.deepguard.media.enums.UploadStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "media_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaFile {

    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "original_url", nullable = false, columnDefinition = "TEXT")
    private String originalUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false)
    private FileType fileType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    // seconds
    private Integer duration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UploadStatus uploadStatus;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    public void prePersist() {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        uploadedAt = LocalDateTime.now();
    }

}




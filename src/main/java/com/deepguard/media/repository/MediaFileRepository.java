package com.deepguard.media.repository;

import com.deepguard.auth.entity.User;
import com.deepguard.media.entity.MediaFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, String> {
    Page<MediaFile> findByUserId(String userId, Pageable pageable);
    Page<MediaFile> findByUserIdAndUploadedAtBetween(
            String userId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );
    Page<MediaFile> findByUserIdAndUploadedAtGreaterThanEqual(
            String userId,
            LocalDateTime startDate,
            Pageable pageable
    );

    Page<MediaFile> findByUserIdAndUploadedAtLessThanEqual(
            String userId,
            LocalDateTime endDate,
            Pageable pageable
    );

    @Query("""
    SELECT media
    FROM MediaFile media
    JOIN FETCH media.user
    WHERE (
        CAST(:startDate AS timestamp) IS NULL
        OR media.uploadedAt >= :startDate
    )
    AND (
        CAST(:endDate AS timestamp) IS NULL
        OR media.uploadedAt <= :endDate
    )
""")
    Page<MediaFile> findAllWithFilters(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    MediaFile findByIdAndUserId(String id, String userId);

    long countByUserId(String userId);
}

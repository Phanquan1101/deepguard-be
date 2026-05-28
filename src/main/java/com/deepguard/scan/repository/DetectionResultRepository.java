package com.deepguard.scan.repository;

import com.deepguard.auth.entity.User;
import com.deepguard.scan.entity.DetectionResult;
import com.deepguard.scan.enums.DetectionLabel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DetectionResultRepository extends JpaRepository<DetectionResult, String> {
    @EntityGraph(attributePaths = {
            "scanJob",
            "scanJob.mediaFile"
    })
    List<DetectionResult> findByScanJob_UserOrderByProcessedAtDesc(User user);

    @EntityGraph(attributePaths = {
            "scanJob",
            "scanJob.mediaFile",
            "scanJob.user"
    })
    Page<DetectionResult> findAllByResultLabel(DetectionLabel resultLabel, Pageable pageable);

    Optional<DetectionResult> findByScanJobId(String scanJobId);
}

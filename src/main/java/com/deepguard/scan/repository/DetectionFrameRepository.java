package com.deepguard.scan.repository;

import com.deepguard.scan.entity.DetectionFrame;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetectionFrameRepository extends JpaRepository<DetectionFrame, String> {
    List<DetectionFrame> findByDetectionResult_IdOrderByFrameIndexAsc(String detectionResultId);
}

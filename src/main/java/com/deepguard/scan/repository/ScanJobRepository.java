package com.deepguard.scan.repository;

import com.deepguard.auth.entity.User;
import com.deepguard.scan.entity.ScanJob;
import com.deepguard.scan.enums.ScanJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScanJobRepository extends JpaRepository<ScanJob, String> {

    @EntityGraph(attributePaths = {"mediaFile"})
    List<ScanJob> findByUserOrderByStartedAtDesc(User user);

    @EntityGraph(attributePaths = {
            "mediaFile",
            "user"
    })
    Page<ScanJob> findAllByStatus (ScanJobStatus status, Pageable pageable);

}

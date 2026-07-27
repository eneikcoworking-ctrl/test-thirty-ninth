package com.eneik.generated.repository;

import com.eneik.generated.entity.WarmupTaskHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface WarmupTaskHistoryRepository extends JpaRepository<WarmupTaskHistory, Long> {

    @Modifying
    @Query("UPDATE WarmupTaskHistory w SET w.status = :newStatus, w.completedAt = :completedAt, w.scoreImpact = :scoreImpact WHERE w.id = :id AND w.status = :expectedStatus")
    int updateStatusAtomic(
        @Param("id") Long id,
        @Param("expectedStatus") String expectedStatus,
        @Param("newStatus") String newStatus,
        @Param("scoreImpact") double scoreImpact,
        @Param("completedAt") Instant completedAt
    );
}

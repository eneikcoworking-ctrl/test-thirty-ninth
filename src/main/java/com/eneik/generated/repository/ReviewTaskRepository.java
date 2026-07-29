package com.eneik.generated.repository;

import com.eneik.generated.entity.ReviewTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewTaskRepository extends JpaRepository<ReviewTask, Long> {

    List<ReviewTask> findByStatus(String status);

    @Modifying
    @Query("UPDATE ReviewTask r SET r.status = :newStatus WHERE r.id = :id AND r.status = :expectedStatus")
    int updateStatusAtomic(
        @Param("id") Long id,
        @Param("expectedStatus") String expectedStatus,
        @Param("newStatus") String newStatus
    );
}

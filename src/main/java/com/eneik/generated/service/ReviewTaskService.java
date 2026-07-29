package com.eneik.generated.service;

import com.eneik.generated.entity.ReviewTask;
import com.eneik.generated.repository.ReviewTaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewTaskService {

    private final ReviewTaskRepository reviewTaskRepository;

    public ReviewTaskService(ReviewTaskRepository reviewTaskRepository) {
        this.reviewTaskRepository = reviewTaskRepository;
    }

    @Transactional
    public ReviewTask createReviewTask(String reference) {
        ReviewTask task = new ReviewTask(reference, "PENDING");
        return reviewTaskRepository.save(task);
    }

    @Transactional
    public int evaluateAndUnblockStuckTasks() {
        List<ReviewTask> stuckTasks = reviewTaskRepository.findByStatus("STUCK/BLOCKED");
        int unblockedCount = 0;

        for (ReviewTask task : stuckTasks) {
            int updatedRows = reviewTaskRepository.updateStatusAtomic(task.getId(), "STUCK/BLOCKED", "ESCALATED");
            if (updatedRows > 0) {
                // Manually update the entity in context so assertions catch it if it's cached
                task.setStatus("ESCALATED");
                unblockedCount++;
            }
        }

        return unblockedCount;
    }
}

package com.eneik.generated.service;

import com.eneik.generated.entity.ReviewTask;
import com.eneik.generated.repository.ReviewTaskRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
public class ReviewTaskServiceTest {

    @Autowired
    private ReviewTaskService reviewTaskService;

    @Autowired
    private ReviewTaskRepository reviewTaskRepository;

    @Test
    public void testCreateReviewTask_InitializesAsPending() {
        // Given
        String reference = "TASK-123";

        // When
        ReviewTask task = reviewTaskService.createReviewTask(reference);

        // Then
        assertNotNull(task.getId());
        assertEquals("PENDING", task.getStatus());
        assertEquals(reference, task.getReference());
    }

    @Test
    public void testEvaluateAndUnblockStuckTasks_EscalatesStuckTasks() {
        // Given
        ReviewTask stuckTask1 = new ReviewTask("f72b3411", "STUCK/BLOCKED");
        ReviewTask stuckTask2 = new ReviewTask("aec3f8d1", "STUCK/BLOCKED");
        ReviewTask normalTask = new ReviewTask("normal-task", "PENDING");
        reviewTaskRepository.saveAll(List.of(stuckTask1, stuckTask2, normalTask));

        // When
        int unblockedCount = reviewTaskService.evaluateAndUnblockStuckTasks();

        // Then
        assertEquals(2, unblockedCount);

        ReviewTask reloadedTask1 = reviewTaskRepository.findById(stuckTask1.getId()).orElseThrow();
        assertEquals("ESCALATED", reloadedTask1.getStatus());

        ReviewTask reloadedTask2 = reviewTaskRepository.findById(stuckTask2.getId()).orElseThrow();
        assertEquals("ESCALATED", reloadedTask2.getStatus());

        ReviewTask reloadedNormalTask = reviewTaskRepository.findById(normalTask.getId()).orElseThrow();
        assertEquals("PENDING", reloadedNormalTask.getStatus());
    }
}

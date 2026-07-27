package com.eneik.generated.service;

import com.eneik.generated.entity.TelegramAccount;
import com.eneik.generated.entity.WarmupTaskHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class WarmupServiceTest {

    @Autowired
    private WarmupService warmupService;

    @Test
    public void testRegisterSessionPersistsCreationDateAndStage() {
        // Given
        String sessionName = "test-session-1";
        // Fixed seed-like timestamp for reproducibility
        Instant creationDate = Instant.parse("2026-07-27T12:00:00Z");
        String warmupStage = "STAGE_1_INITIAL";
        double initialTrustScore = 15.5;

        // When
        TelegramAccount registered = warmupService.registerSession(sessionName, creationDate, warmupStage, initialTrustScore);

        // Then
        assertNotNull(registered.getId());
        assertEquals(sessionName, registered.getSessionName());
        assertEquals(creationDate, registered.getCreationDate());
        assertEquals(warmupStage, registered.getWarmupStage());
        assertEquals(initialTrustScore, registered.getTrustScore(), 0.0001);

        // Verify retrieval from DB
        TelegramAccount retrieved = warmupService.getAccount(registered.getId()).orElse(null);
        assertNotNull(retrieved);
        assertEquals(creationDate, retrieved.getCreationDate());
        assertEquals(warmupStage, retrieved.getWarmupStage());
    }

    @Test
    public void testLogCompletedTaskUpdatesTrustScore() {
        // Given
        String sessionName = "test-session-2";
        Instant creationDate = Instant.parse("2026-07-27T12:00:00Z");
        String warmupStage = "STAGE_1_INITIAL";
        double initialTrustScore = 10.0;
        TelegramAccount account = warmupService.registerSession(sessionName, creationDate, warmupStage, initialTrustScore);

        String taskType = "SEND_HEURISTIC_MESSAGE";
        double scoreImpact = 2.5;
        Instant completedAt = Instant.parse("2026-07-27T14:30:00Z");

        // When
        WarmupTaskHistory task = warmupService.logCompletedTask(account.getId(), taskType, scoreImpact, completedAt);

        // Then
        assertNotNull(task.getId());
        assertEquals("COMPLETED", task.getStatus());
        assertEquals(scoreImpact, task.getScoreImpact(), 0.0001);
        assertEquals(completedAt, task.getCompletedAt());

        // Verify account's trust score metric updates
        TelegramAccount updatedAccount = warmupService.getAccount(account.getId()).orElse(null);
        assertNotNull(updatedAccount);
        // Expect initialTrustScore + scoreImpact = 10.0 + 2.5 = 12.5
        assertEquals(12.5, updatedAccount.getTrustScore(), 0.0001);
    }

    @Test
    public void testCompletePendingTaskUpdatesTrustScoreAtomically() {
        // Given
        String sessionName = "test-session-3";
        Instant creationDate = Instant.parse("2026-07-27T12:00:00Z");
        String warmupStage = "STAGE_1_INITIAL";
        double initialTrustScore = 5.0;
        TelegramAccount account = warmupService.registerSession(sessionName, creationDate, warmupStage, initialTrustScore);

        String taskType = "JOIN_GROUP";
        double scoreImpact = 4.0;
        WarmupTaskHistory task = warmupService.createTask(account.getId(), taskType, scoreImpact);
        assertEquals("PENDING", task.getStatus());

        Instant completedAt = Instant.parse("2026-07-27T15:00:00Z");

        // When
        WarmupTaskHistory completedTask = warmupService.completeTask(task.getId(), completedAt);

        // Then
        assertEquals("COMPLETED", completedTask.getStatus());
        assertEquals(completedAt, completedTask.getCompletedAt());

        TelegramAccount updatedAccount = warmupService.getAccount(account.getId()).orElse(null);
        assertNotNull(updatedAccount);
        // Expect 5.0 + 4.0 = 9.0
        assertEquals(9.0, updatedAccount.getTrustScore(), 0.0001);

        // Attempting to complete the same task again should be idempotent or return the completed task (in our code, it returns the completed task if status is COMPLETED)
        WarmupTaskHistory doubleCompleted = warmupService.completeTask(task.getId(), completedAt);
        assertEquals("COMPLETED", doubleCompleted.getStatus());
        assertEquals(9.0, updatedAccount.getTrustScore(), 0.0001);
    }
}

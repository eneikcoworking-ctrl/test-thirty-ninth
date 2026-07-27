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

    @Test
    public void testExponentialDelayDeterministicCalculation() {
        // Given
        java.util.Random fixedRandom = new java.util.Random(1337L);
        double lambda = 0.5;

        // When
        double delay1 = warmupService.calculateNextActionDelay(lambda, fixedRandom);
        double delay2 = warmupService.calculateNextActionDelay(lambda, fixedRandom);

        // Then
        // Expected values generated with seed 1337 and lambda 0.5:
        // -Math.log(1.0 - nextDouble()) / 0.5
        java.util.Random expectedRandom = new java.util.Random(1337L);
        double expected1 = -Math.log(1.0 - expectedRandom.nextDouble()) / 0.5;
        double expected2 = -Math.log(1.0 - expectedRandom.nextDouble()) / 0.5;

        assertEquals(expected1, delay1, 1e-9);
        assertEquals(expected2, delay2, 1e-9);
    }

    @Test
    public void testAssignToOutreachThrowsExceptionWhenAccountIsUnder30DaysOld() {
        // Given
        Instant creationDate = Instant.parse("2026-07-20T12:00:00Z"); // Less than 30 days old relative to clock (System clock or the one defined in configuration)
        // Let's check using WarmupService which uses system clock if not injected, but we can verify by registering a very recent account.
        // Or we could register and check. Since clock is systemUTC by default:
        Instant now = Instant.now();
        Instant recentCreationDate = now.minus(java.time.Duration.ofDays(10));
        TelegramAccount recentAccount = warmupService.registerSession("recent-session", recentCreationDate, "WARMUP_1", 10.0);

        // When/Then
        assertThrows(IllegalStateException.class, () -> {
            warmupService.assignToOutreach(recentAccount.getId());
        });

        // The stage should remain unchanged
        TelegramAccount reloaded = warmupService.getAccount(recentAccount.getId()).orElseThrow();
        assertEquals("WARMUP_1", reloaded.getWarmupStage());
    }

    @Test
    public void testAssignToOutreachSuccessfullyTransitionsStageWhenAccountIs30DaysOrOlder() {
        // Given
        Instant now = Instant.now();
        Instant oldCreationDate = now.minus(java.time.Duration.ofDays(35));
        TelegramAccount oldAccount = warmupService.registerSession("old-session", oldCreationDate, "WARMUP_1", 10.0);

        // When
        TelegramAccount updatedAccount = warmupService.assignToOutreach(oldAccount.getId());

        // Then
        assertEquals("OUTREACH", updatedAccount.getWarmupStage());

        TelegramAccount reloaded = warmupService.getAccount(oldAccount.getId()).orElseThrow();
        assertEquals("OUTREACH", reloaded.getWarmupStage());
    }
}

package com.eneik.generated.service;

import com.eneik.generated.entity.TelegramAccount;
import com.eneik.generated.entity.WarmupTaskHistory;
import com.eneik.generated.repository.TelegramAccountRepository;
import com.eneik.generated.repository.WarmupTaskHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class WarmupService {

    private final TelegramAccountRepository accountRepository;
    private final WarmupTaskHistoryRepository taskHistoryRepository;
    private final Clock clock;

    public WarmupService(
            TelegramAccountRepository accountRepository,
            WarmupTaskHistoryRepository taskHistoryRepository,
            Optional<Clock> clockOpt) {
        this.accountRepository = accountRepository;
        this.taskHistoryRepository = taskHistoryRepository;
        this.clock = clockOpt.orElse(Clock.systemUTC());
    }

    @Transactional
    public TelegramAccount registerSession(String sessionName, Instant creationDate, String warmupStage, double initialTrustScore) {
        if (sessionName == null || sessionName.trim().isEmpty()) {
            throw new IllegalArgumentException("Session name cannot be empty");
        }
        Optional<TelegramAccount> existing = accountRepository.findBySessionName(sessionName);
        if (existing.isPresent()) {
            return existing.get();
        }

        Instant actualCreationDate = creationDate != null ? creationDate : clock.instant();
        TelegramAccount account = new TelegramAccount(sessionName, actualCreationDate, warmupStage, initialTrustScore);
        return accountRepository.save(account);
    }

    @Transactional
    public WarmupTaskHistory createTask(Long accountId, String taskType, double scoreImpact) {
        TelegramAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        WarmupTaskHistory task = new WarmupTaskHistory(account, taskType, "PENDING", scoreImpact, null);
        return taskHistoryRepository.save(task);
    }

    @Transactional
    public WarmupTaskHistory completeTask(Long taskId, Instant completedAt) {
        WarmupTaskHistory task = taskHistoryRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if ("COMPLETED".equals(task.getStatus())) {
            return task;
        }

        Instant actualCompletedAt = completedAt != null ? completedAt : clock.instant();

        // Atomically transition the status from PENDING to COMPLETED using UPDATE ... WHERE id = ? AND status = ?
        int updatedRows = taskHistoryRepository.updateStatusAtomic(taskId, "PENDING", "COMPLETED", task.getScoreImpact(), actualCompletedAt);
        if (updatedRows == 0) {
            throw new IllegalStateException("Task " + taskId + " is already completed or modified concurrently.");
        }

        // Refresh state from DB to get the updated status, or construct returned state
        task.setStatus("COMPLETED");
        task.setCompletedAt(actualCompletedAt);

        TelegramAccount account = task.getTelegramAccount();
        // Optimistically locked update of the trust score
        account.setTrustScore(account.getTrustScore() + task.getScoreImpact());
        accountRepository.save(account);

        return taskHistoryRepository.save(task);
    }

    @Transactional
    public WarmupTaskHistory logCompletedTask(Long accountId, String taskType, double scoreImpact, Instant completedAt) {
        TelegramAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        Instant actualCompletedAt = completedAt != null ? completedAt : clock.instant();

        WarmupTaskHistory task = new WarmupTaskHistory(account, taskType, "COMPLETED", scoreImpact, actualCompletedAt);
        taskHistoryRepository.save(task);

        // Optimistically locked update of the trust score
        account.setTrustScore(account.getTrustScore() + scoreImpact);
        accountRepository.saveAndFlush(account);

        return task;
    }

    public Optional<TelegramAccount> getAccount(Long id) {
        return accountRepository.findById(id);
    }

    public Optional<TelegramAccount> getAccountBySessionName(String sessionName) {
        return accountRepository.findBySessionName(sessionName);
    }

    public double calculateNextActionDelay(double lambda, java.util.Random random) {
        if (lambda <= 0) {
            throw new IllegalArgumentException("Lambda must be positive");
        }
        java.util.Random rnd = random != null ? random : new java.util.Random();
        return -Math.log(1.0 - rnd.nextDouble()) / lambda;
    }

    @Transactional
    public TelegramAccount assignToOutreach(Long accountId) {
        TelegramAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        Instant now = clock.instant();
        Instant cutoff = now.minus(java.time.Duration.ofDays(30));

        if (account.getCreationDate().isAfter(cutoff)) {
            throw new IllegalStateException("Account is under 30 days old and cannot be assigned to outreach");
        }

        account.setWarmupStage("OUTREACH");
        return accountRepository.save(account);
    }
}

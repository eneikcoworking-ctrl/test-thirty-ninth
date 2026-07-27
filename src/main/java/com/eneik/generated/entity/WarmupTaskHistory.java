package com.eneik.generated.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "warmup_task_history")
public class WarmupTaskHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "telegram_account_id", nullable = false)
    private TelegramAccount telegramAccount;

    @Column(name = "task_type", nullable = false)
    private String taskType;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "score_impact", nullable = false)
    private double scoreImpact;

    @Column(name = "completed_at")
    private Instant completedAt;

    public WarmupTaskHistory() {}

    public WarmupTaskHistory(TelegramAccount telegramAccount, String taskType, String status, double scoreImpact, Instant completedAt) {
        this.telegramAccount = telegramAccount;
        this.taskType = taskType;
        this.status = status;
        this.scoreImpact = scoreImpact;
        this.completedAt = completedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TelegramAccount getTelegramAccount() {
        return telegramAccount;
    }

    public void setTelegramAccount(TelegramAccount telegramAccount) {
        this.telegramAccount = telegramAccount;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getScoreImpact() {
        return scoreImpact;
    }

    public void setScoreImpact(double scoreImpact) {
        this.scoreImpact = scoreImpact;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}

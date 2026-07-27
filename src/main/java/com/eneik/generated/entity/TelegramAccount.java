package com.eneik.generated.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "telegram_accounts")
public class TelegramAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_name", unique = true, nullable = false)
    private String sessionName;

    @Column(name = "creation_date", nullable = false)
    private Instant creationDate;

    @Column(name = "warmup_stage", nullable = false)
    private String warmupStage;

    @Column(name = "trust_score", nullable = false)
    private double trustScore;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public TelegramAccount() {}

    public TelegramAccount(String sessionName, Instant creationDate, String warmupStage, double trustScore) {
        this.sessionName = sessionName;
        this.creationDate = creationDate;
        this.warmupStage = warmupStage;
        this.trustScore = trustScore;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public Instant getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Instant creationDate) {
        this.creationDate = creationDate;
    }

    public String getWarmupStage() {
        return warmupStage;
    }

    public void setWarmupStage(String warmupStage) {
        this.warmupStage = warmupStage;
    }

    public double getTrustScore() {
        return trustScore;
    }

    public void setTrustScore(double trustScore) {
        this.trustScore = trustScore;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}

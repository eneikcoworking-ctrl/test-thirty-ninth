package com.eneik.generated.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "telegram_account_id", nullable = false)
    private TelegramAccount telegramAccount;

    @Column(name = "lead_username")
    private String leadUsername;

    @Column(name = "lead_phone_number")
    private String leadPhoneNumber;

    private String status;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Conversation() {}

    public Conversation(String id, TelegramAccount telegramAccount, String leadUsername, String leadPhoneNumber, String status, LocalDateTime updatedAt) {
        this.id = id;
        this.telegramAccount = telegramAccount;
        this.leadUsername = leadUsername;
        this.leadPhoneNumber = leadPhoneNumber;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public TelegramAccount getTelegramAccount() {
        return telegramAccount;
    }

    public void setTelegramAccount(TelegramAccount telegramAccount) {
        this.telegramAccount = telegramAccount;
    }

    public String getLeadUsername() {
        return leadUsername;
    }

    public void setLeadUsername(String leadUsername) {
        this.leadUsername = leadUsername;
    }

    public String getLeadPhoneNumber() {
        return leadPhoneNumber;
    }

    public void setLeadPhoneNumber(String leadPhoneNumber) {
        this.leadPhoneNumber = leadPhoneNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

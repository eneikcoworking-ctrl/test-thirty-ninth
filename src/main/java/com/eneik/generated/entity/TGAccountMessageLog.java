package com.eneik.generated.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tg_account_message_log")
public class TGAccountMessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tg_account_id", nullable = false)
    private TGAccount tgAccount;

    @Column(name = "recipient", nullable = false)
    private String recipient;

    @Column(name = "message_text", nullable = false, columnDefinition = "TEXT")
    private String messageText;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    public TGAccountMessageLog() {}

    public TGAccountMessageLog(TGAccount tgAccount, String recipient, String messageText, Instant sentAt) {
        this.tgAccount = tgAccount;
        this.recipient = recipient;
        this.messageText = messageText;
        this.sentAt = sentAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TGAccount getTgAccount() {
        return tgAccount;
    }

    public void setTgAccount(TGAccount tgAccount) {
        this.tgAccount = tgAccount;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }
}

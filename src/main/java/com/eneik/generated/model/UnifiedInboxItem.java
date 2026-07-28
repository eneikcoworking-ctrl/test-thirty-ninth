package com.eneik.generated.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "unified_inbox_view")
public class UnifiedInboxItem {
    @Id
    @Column(name = "dialogue_id")
    private Long dialogueId;

    @Column(name = "tg_account_id")
    private Long tgAccountId;

    @Column(name = "account_phone_number")
    private String accountPhoneNumber;

    @Column(name = "dialogue_status")
    private String dialogueStatus;

    @Column(name = "ai_turns_count")
    private int aiTurnsCount;

    @Column(name = "human_intervention_required")
    private boolean humanInterventionRequired;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    public Long getDialogueId() { return dialogueId; }
    public Long getTgAccountId() { return tgAccountId; }
    public String getAccountPhoneNumber() { return accountPhoneNumber; }
    public String getDialogueStatus() { return dialogueStatus; }
    public int getAiTurnsCount() { return aiTurnsCount; }
    public boolean isHumanInterventionRequired() { return humanInterventionRequired; }
    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
}

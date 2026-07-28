package com.eneik.generated.model;

import com.eneik.generated.entity.TGAccount;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dialogue_state")
public class DialogueState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tg_account_id")
    private TGAccount tgAccount;

    @Column(name = "status", nullable = false)
    private String status = "ACTIVE";

    @Column(name = "ai_turns_count", nullable = false)
    private int aiTurnsCount = 0;

    @Column(name = "human_intervention_required", nullable = false)
    private boolean humanInterventionRequired = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "dialogueState", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("timestamp ASC")
    private List<DialogueTurn> turns = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helpers
    public void addTurn(DialogueTurn turn) {
        turns.add(turn);
        turn.setDialogueState(this);
    }

    // Getters and Setters
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getAiTurnsCount() {
        return aiTurnsCount;
    }

    public void setAiTurnsCount(int aiTurnsCount) {
        this.aiTurnsCount = aiTurnsCount;
    }

    public boolean isHumanInterventionRequired() {
        return humanInterventionRequired;
    }

    public void setHumanInterventionRequired(boolean humanInterventionRequired) {
        this.humanInterventionRequired = humanInterventionRequired;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<DialogueTurn> getTurns() {
        return turns;
    }

    public void setTurns(List<DialogueTurn> turns) {
        this.turns = turns;
    }
}

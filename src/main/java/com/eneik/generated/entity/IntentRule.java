package com.eneik.generated.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "intent_rule")
public class IntentRule {
    @Id
    private String id;

    @Column(name = "intent_name", nullable = false)
    private String intentName;

    @Column(name = "keywords", nullable = false, columnDefinition = "TEXT")
    private String keywords; // Comma-separated list of keywords

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIntentName() {
        return intentName;
    }

    public void setIntentName(String intentName) {
        this.intentName = intentName;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

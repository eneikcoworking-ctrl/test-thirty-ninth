package com.eneik.generated.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_configuration")
public class AiConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "system_prompt", nullable = false, length = 2000)
    private String systemPrompt;

    @Column(name = "stop_triggers", nullable = false, length = 1000)
    private String stopTriggers;

    @Column(name = "model_version", nullable = false)
    private String modelVersion;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public AiConfiguration() {}

    public AiConfiguration(String systemPrompt, String stopTriggers, String modelVersion) {
        this.systemPrompt = systemPrompt;
        this.stopTriggers = stopTriggers;
        this.modelVersion = modelVersion;
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
        this.updatedAt = LocalDateTime.now();
    }

    public String getStopTriggers() {
        return stopTriggers;
    }

    public void setStopTriggers(String stopTriggers) {
        this.stopTriggers = stopTriggers;
        this.updatedAt = LocalDateTime.now();
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

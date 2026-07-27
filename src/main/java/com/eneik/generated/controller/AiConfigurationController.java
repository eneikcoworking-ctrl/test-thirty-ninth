package com.eneik.generated.controller;

import com.eneik.generated.entity.AiConfiguration;
import com.eneik.generated.repository.AiConfigurationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/ai-configuration")
@CrossOrigin(origins = "*")
public class AiConfigurationController {

    private final AiConfigurationRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public AiConfigurationController(AiConfigurationRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<AiConfigurationResponse> getConfiguration() {
        AiConfiguration config = getOrCreateDefault();
        return ResponseEntity.ok(toResponse(config));
    }

    @PutMapping
    public ResponseEntity<AiConfigurationResponse> updateConfiguration(@RequestBody AiConfigurationRequest request) {
        AiConfiguration existing = getOrCreateDefault();

        if (request.getSystemPrompt() != null) {
            existing.setSystemPrompt(request.getSystemPrompt());
        }
        if (request.getStopTriggers() != null) {
            try {
                existing.setStopTriggers(objectMapper.writeValueAsString(request.getStopTriggers()));
            } catch (Exception e) {
                // fallback
            }
        }
        if (request.getModelVersion() != null) {
            existing.setModelVersion(request.getModelVersion());
        }
        existing.setUpdatedAt(LocalDateTime.now());

        AiConfiguration saved = repository.save(existing);
        return ResponseEntity.ok(toResponse(saved));
    }

    @PostMapping("/stop-triggers")
    public ResponseEntity<AiConfigurationResponse> updateStopTriggers(@RequestBody List<StopTrigger> triggers) {
        AiConfiguration existing = getOrCreateDefault();
        try {
            existing.setStopTriggers(objectMapper.writeValueAsString(triggers));
        } catch (Exception e) {
            // fallback
        }
        existing.setUpdatedAt(LocalDateTime.now());
        AiConfiguration saved = repository.save(existing);
        return ResponseEntity.ok(toResponse(saved));
    }

    private AiConfiguration getOrCreateDefault() {
        return repository.findAll().stream().findFirst().orElseGet(() -> {
            AiConfiguration defaultConf = new AiConfiguration(
                "You are a highly analytical AI assistant specialized in technical documentation and software architecture. Your tone is professional, concise, and focused on providing empirical data and verifiable code snippets. Avoid flowery language or conversational fillers. When asked about complex systems, provide high-level abstractions followed by detailed component breakdowns.",
                "[{\"keyword\":\"Exit\",\"enabled\":true},{\"keyword\":\"Cancel\",\"enabled\":true},{\"keyword\":\"Error\",\"enabled\":true}]",
                "GPT-4-Turbo"
            );
            return repository.save(defaultConf);
        });
    }

    private AiConfigurationResponse toResponse(AiConfiguration entity) {
        AiConfigurationResponse resp = new AiConfigurationResponse();
        resp.setId(entity.getId());
        resp.setSystemPrompt(entity.getSystemPrompt());
        resp.setModelVersion(entity.getModelVersion());
        resp.setUpdatedAt(entity.getUpdatedAt());
        try {
            List<StopTrigger> triggers = objectMapper.readValue(entity.getStopTriggers(), new TypeReference<List<StopTrigger>>() {});
            resp.setStopTriggers(triggers);
        } catch (Exception e) {
            resp.setStopTriggers(new ArrayList<>());
        }
        return resp;
    }

    public static class StopTrigger {
        private String keyword;
        private boolean enabled;

        public StopTrigger() {}

        public StopTrigger(String keyword, boolean enabled) {
            this.keyword = keyword;
            this.enabled = enabled;
        }

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class AiConfigurationRequest {
        private String systemPrompt;
        private List<StopTrigger> stopTriggers;
        private String modelVersion;

        public String getSystemPrompt() {
            return systemPrompt;
        }

        public void setSystemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
        }

        public List<StopTrigger> getStopTriggers() {
            return stopTriggers;
        }

        public void setStopTriggers(List<StopTrigger> stopTriggers) {
            this.stopTriggers = stopTriggers;
        }

        public String getModelVersion() {
            return modelVersion;
        }

        public void setModelVersion(String modelVersion) {
            this.modelVersion = modelVersion;
        }
    }

    public static class AiConfigurationResponse {
        private Long id;
        private String systemPrompt;
        private List<StopTrigger> stopTriggers;
        private String modelVersion;
        private LocalDateTime updatedAt;

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
        }

        public List<StopTrigger> getStopTriggers() {
            return stopTriggers;
        }

        public void setStopTriggers(List<StopTrigger> stopTriggers) {
            this.stopTriggers = stopTriggers;
        }

        public String getModelVersion() {
            return modelVersion;
        }

        public void setModelVersion(String modelVersion) {
            this.modelVersion = modelVersion;
        }

        public LocalDateTime getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
        }
    }
}

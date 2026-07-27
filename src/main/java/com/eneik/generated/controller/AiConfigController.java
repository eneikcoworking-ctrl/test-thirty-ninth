package com.eneik.generated.controller;

import com.eneik.generated.entity.AiConfig;
import com.eneik.generated.entity.StopTrigger;
import com.eneik.generated.entity.IntentRule;
import com.eneik.generated.repository.AiConfigRepository;
import com.eneik.generated.repository.StopTriggerRepository;
import com.eneik.generated.repository.IntentRuleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class AiConfigController {

    private final AiConfigRepository aiConfigRepository;
    private final StopTriggerRepository stopTriggerRepository;
    private final IntentRuleRepository intentRuleRepository;

    @Autowired
    public AiConfigController(
            AiConfigRepository aiConfigRepository,
            StopTriggerRepository stopTriggerRepository,
            IntentRuleRepository intentRuleRepository) {
        this.aiConfigRepository = aiConfigRepository;
        this.stopTriggerRepository = stopTriggerRepository;
        this.intentRuleRepository = intentRuleRepository;
    }

    // Helper to map DB entity to API Schema for IntentRule
    private Map<String, Object> mapIntentRuleToDto(IntentRule rule) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", rule.getId());
        dto.put("intentName", rule.getIntentName());

        List<String> keywordsList = new ArrayList<>();
        if (rule.getKeywords() != null && !rule.getKeywords().trim().isEmpty()) {
            keywordsList = Arrays.stream(rule.getKeywords().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        }
        dto.put("keywords", keywordsList);
        dto.put("action", rule.getAction());
        dto.put("priority", rule.getPriority());
        dto.put("enabled", rule.isEnabled());
        return dto;
    }

    // GET /api/v1/ai-config
    @GetMapping("/ai-config")
    public ResponseEntity<Map<String, Object>> getAiConfig() {
        List<AiConfig> configs = aiConfigRepository.findAll();
        String systemPrompt = "";
        if (!configs.isEmpty()) {
            systemPrompt = configs.get(0).getSystemPrompt();
        } else {
            systemPrompt = "Default system prompt";
        }

        List<String> triggers = stopTriggerRepository.findAll().stream()
                .map(StopTrigger::getTriggerWord)
                .collect(Collectors.toList());

        List<Map<String, Object>> rules = intentRuleRepository.findAll().stream()
                .map(this::mapIntentRuleToDto)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("systemPrompt", systemPrompt);
        response.put("stopTriggers", triggers);
        response.put("intentRules", rules);

        return ResponseEntity.ok(response);
    }

    // PUT /api/v1/ai-config
    @PutMapping("/ai-config")
    @Transactional
    public ResponseEntity<Map<String, Object>> updateAiConfig(@RequestBody Map<String, Object> payload) {
        // Update System Prompt
        if (payload.containsKey("systemPrompt")) {
            String systemPrompt = (String) payload.get("systemPrompt");
            List<AiConfig> configs = aiConfigRepository.findAll();
            AiConfig config = configs.isEmpty() ? new AiConfig() : configs.get(0);
            config.setSystemPrompt(systemPrompt);
            aiConfigRepository.save(config);
        }

        // Update Stop Triggers
        if (payload.containsKey("stopTriggers")) {
            List<?> stopTriggers = (List<?>) payload.get("stopTriggers");
            stopTriggerRepository.deleteAllInBatch();
            for (Object obj : stopTriggers) {
                String triggerWord = obj.toString().trim();
                if (!triggerWord.isEmpty()) {
                    StopTrigger trigger = new StopTrigger();
                    trigger.setTriggerWord(triggerWord);
                    stopTriggerRepository.save(trigger);
                }
            }
        }

        // Update Intent Rules
        if (payload.containsKey("intentRules")) {
            List<?> intentRules = (List<?>) payload.get("intentRules");
            intentRuleRepository.deleteAllInBatch();
            for (Object obj : intentRules) {
                if (obj instanceof Map) {
                    Map<?, ?> ruleMap = (Map<?, ?>) obj;
                    IntentRule rule = new IntentRule();
                    rule.setId(ruleMap.get("id").toString());
                    rule.setIntentName(ruleMap.get("intentName").toString());

                    List<?> keywords = (List<?>) ruleMap.get("keywords");
                    String keywordsStr = keywords.stream()
                            .map(Object::toString)
                            .map(String::trim)
                            .collect(Collectors.joining(","));
                    rule.setKeywords(keywordsStr);
                    rule.setAction(ruleMap.get("action").toString());
                    rule.setPriority(Integer.parseInt(ruleMap.get("priority").toString()));
                    rule.setEnabled(Boolean.parseBoolean(ruleMap.get("enabled").toString()));
                    intentRuleRepository.save(rule);
                }
            }
        }

        return getAiConfig();
    }

    // GET /api/v1/ai-config/system-prompt
    @GetMapping("/ai-config/system-prompt")
    public ResponseEntity<Map<String, Object>> getSystemPrompt() {
        List<AiConfig> configs = aiConfigRepository.findAll();
        String systemPrompt = "";
        LocalDateTime updatedAt = LocalDateTime.now();
        if (!configs.isEmpty()) {
            systemPrompt = configs.get(0).getSystemPrompt();
            updatedAt = configs.get(0).getUpdatedAt();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("systemPrompt", systemPrompt);
        response.put("updatedAt", updatedAt.format(DateTimeFormatter.ISO_DATE_TIME));

        return ResponseEntity.ok(response);
    }

    // PUT /api/v1/ai-config/system-prompt
    @PutMapping("/ai-config/system-prompt")
    public ResponseEntity<Map<String, Object>> updateSystemPrompt(@RequestBody Map<String, String> payload) {
        String newPrompt = payload.get("systemPrompt");
        if (newPrompt == null) {
            newPrompt = "";
        }

        List<AiConfig> configs = aiConfigRepository.findAll();
        AiConfig config = configs.isEmpty() ? new AiConfig() : configs.get(0);
        config.setSystemPrompt(newPrompt);
        aiConfigRepository.save(config);

        Map<String, Object> response = new HashMap<>();
        response.put("systemPrompt", newPrompt);
        response.put("updatedAt", config.getUpdatedAt().format(DateTimeFormatter.ISO_DATE_TIME));

        return ResponseEntity.ok(response);
    }

    // GET /api/v1/ai-config/intent-rules
    @GetMapping("/ai-config/intent-rules")
    public ResponseEntity<List<Map<String, Object>>> getIntentRules() {
        List<Map<String, Object>> rules = intentRuleRepository.findAll().stream()
                .map(this::mapIntentRuleToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(rules);
    }

    // PUT /api/v1/ai-config/intent-rules
    @PutMapping("/ai-config/intent-rules")
    @Transactional
    public ResponseEntity<List<Map<String, Object>>> updateIntentRules(@RequestBody List<Map<String, Object>> payload) {
        intentRuleRepository.deleteAllInBatch();
        for (Map<String, Object> ruleMap : payload) {
            IntentRule rule = new IntentRule();
            rule.setId(ruleMap.get("id").toString());
            rule.setIntentName(ruleMap.get("intentName").toString());

            List<?> keywords = (List<?>) ruleMap.get("keywords");
            String keywordsStr = keywords.stream()
                    .map(Object::toString)
                    .map(String::trim)
                    .collect(Collectors.joining(","));
            rule.setKeywords(keywordsStr);
            rule.setAction(ruleMap.get("action").toString());
            rule.setPriority(Integer.parseInt(ruleMap.get("priority").toString()));
            rule.setEnabled(Boolean.parseBoolean(ruleMap.get("enabled").toString()));
            intentRuleRepository.save(rule);
        }

        return getIntentRules();
    }
}

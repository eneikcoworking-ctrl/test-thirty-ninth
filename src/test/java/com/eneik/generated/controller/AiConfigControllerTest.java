package com.eneik.generated.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AiConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetAiConfig() throws Exception {
        mockMvc.perform(get("/api/v1/ai-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemPrompt", notNullValue()))
                .andExpect(jsonPath("$.stopTriggers", hasSize(greaterThanOrEqualTo(4))))
                .andExpect(jsonPath("$.stopTriggers", hasItem("ERR_TIMEOUT")))
                .andExpect(jsonPath("$.intentRules", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.intentRules[0].intentName", is("payment_failure")));
    }

    @Test
    public void testUpdateSystemPromptDirect() throws Exception {
        Map<String, String> payload = new HashMap<>();
        payload.put("systemPrompt", "New customized system prompt with {placeholder}");

        mockMvc.perform(put("/api/v1/ai-config/system-prompt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemPrompt", is("New customized system prompt with {placeholder}")))
                .andExpect(jsonPath("$.updatedAt", notNullValue()));

        // Verify GET returns the updated prompt
        mockMvc.perform(get("/api/v1/ai-config/system-prompt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemPrompt", is("New customized system prompt with {placeholder}")));
    }

    @Test
    public void testUpdateIntentRulesDirect() throws Exception {
        List<Map<String, Object>> payload = new ArrayList<>();
        Map<String, Object> rule = new HashMap<>();
        rule.put("id", "rule-999");
        rule.put("intentName", "custom_intent");
        rule.put("keywords", Arrays.asList("custom1", "custom2"));
        rule.put("action", "AUTO_RESPOND");
        rule.put("priority", 10);
        rule.put("enabled", true);
        payload.add(rule);

        mockMvc.perform(put("/api/v1/ai-config/intent-rules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is("rule-999")))
                .andExpect(jsonPath("$[0].intentName", is("custom_intent")))
                .andExpect(jsonPath("$[0].keywords", hasItems("custom1", "custom2")));
    }

    @Test
    public void testOverwriteFullConfiguration() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("systemPrompt", "Overwritten full system prompt");
        payload.put("stopTriggers", Arrays.asList("HALT", "STOP"));

        List<Map<String, Object>> rules = new ArrayList<>();
        Map<String, Object> rule = new HashMap<>();
        rule.put("id", "rule-888");
        rule.put("intentName", "full_intent");
        rule.put("keywords", Arrays.asList("trigger1"));
        rule.put("action", "STOP_GENERATION");
        rule.put("priority", 5);
        rule.put("enabled", false);
        rules.add(rule);
        payload.put("intentRules", rules);

        mockMvc.perform(put("/api/v1/ai-config")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.systemPrompt", is("Overwritten full system prompt")))
                .andExpect(jsonPath("$.stopTriggers", hasSize(2)))
                .andExpect(jsonPath("$.stopTriggers", hasItems("HALT", "STOP")))
                .andExpect(jsonPath("$.intentRules", hasSize(1)))
                .andExpect(jsonPath("$.intentRules[0].id", is("rule-888")));
    }
}

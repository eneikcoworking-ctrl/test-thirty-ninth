package com.eneik.generated.controller;

import com.eneik.generated.entity.TelegramAccount;
import com.eneik.generated.service.WarmupService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WarmupController.class)
public class WarmupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WarmupService warmupService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testGetNextActionDelayReturnsDelay() throws Exception {
        Mockito.when(warmupService.calculateNextActionDelay(eq(0.05), any()))
                .thenReturn(15.4);

        String content = mockMvc.perform(get("/api/warmup/next-action-delay")
                        .param("lambda", "0.05")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(content);
        double delay = root.get("delay").asDouble();
        assertEquals(15.4, delay, 0.0001);
    }

    @Test
    public void testGetNextActionDelayReturnsBadRequestOnIllegalArgument() throws Exception {
        Mockito.when(warmupService.calculateNextActionDelay(eq(-1.0), any()))
                .thenThrow(new IllegalArgumentException("Lambda must be positive"));

        mockMvc.perform(get("/api/warmup/next-action-delay")
                        .param("lambda", "-1.0")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Lambda must be positive"));
    }

    @Test
    public void testAssignToOutreachSuccess() throws Exception {
        TelegramAccount account = new TelegramAccount("session-1", Instant.now(), "OUTREACH", 20.0);
        account.setId(42L);

        Mockito.when(warmupService.assignToOutreach(42L)).thenReturn(account);

        String content = mockMvc.perform(post("/api/warmup/accounts/42/assign-outreach")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode root = objectMapper.readTree(content);
        assertEquals(42L, root.get("id").asLong());
        assertEquals("session-1", root.get("sessionName").asText());
        assertEquals("OUTREACH", root.get("warmupStage").asText());
        assertEquals(20.0, root.get("trustScore").asDouble(), 0.0001);
    }

    @Test
    public void testAssignToOutreachNotFound() throws Exception {
        Mockito.when(warmupService.assignToOutreach(99L))
                .thenThrow(new IllegalArgumentException("Account not found: 99"));

        mockMvc.perform(post("/api/warmup/accounts/99/assign-outreach")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Account not found: 99"));
    }

    @Test
    public void testAssignToOutreachUnderAgeLimitBadRequest() throws Exception {
        Mockito.when(warmupService.assignToOutreach(10L))
                .thenThrow(new IllegalStateException("Account is under 30 days old and cannot be assigned to outreach"));

        mockMvc.perform(post("/api/warmup/accounts/10/assign-outreach")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Account is under 30 days old and cannot be assigned to outreach"));
    }
}

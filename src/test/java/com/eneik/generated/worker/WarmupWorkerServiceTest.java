package com.eneik.generated.worker;

import com.eneik.generated.client.TelegramClient;
import com.eneik.generated.entity.TelegramAccount;
import com.eneik.generated.service.WarmupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
public class WarmupWorkerServiceTest {

    @MockBean
    private TelegramClient telegramClient;

    @Autowired
    private WarmupWorkerService warmupWorkerService;

    @Autowired
    private WarmupService warmupService;

    @Configuration
    @Import(com.eneik.generated.Application.class)
    static class TestConfig {
        @Bean
        public Clock clock() {
            return Clock.fixed(Instant.parse("2026-07-27T12:00:00Z"), ZoneId.of("UTC"));
        }
    }

    @Test
    public void testExecuteChannelWarmup() {
        // Given
        String sessionName = "worker-session-1";
        Instant creationDate = Instant.parse("2026-07-27T12:00:00Z");
        TelegramAccount account = warmupService.registerSession(sessionName, creationDate, "STAGE_1", 10.0);
        String targetChannel = "target_channel_1";

        // When
        warmupWorkerService.executeChannelWarmup(account.getId(), targetChannel);

        // Then
        verify(telegramClient).subscribeToChannel(sessionName, targetChannel);
        verify(telegramClient).markRecentPostsAsRead(sessionName, targetChannel);

        TelegramAccount updatedAccount = warmupService.getAccount(account.getId()).orElse(null);
        assertEquals(11.0, updatedAccount.getTrustScore(), 0.0001);
    }

    @Test
    public void testMaintainOnlinePresence() {
        // Given
        String sessionName = "worker-session-2";
        Instant creationDate = Instant.parse("2026-07-27T12:00:00Z");
        TelegramAccount account = warmupService.registerSession(sessionName, creationDate, "STAGE_1", 10.0);

        // When
        warmupWorkerService.maintainOnlinePresence(account.getId());

        // Then
        verify(telegramClient).setOnlinePresence(sessionName, true);

        TelegramAccount updatedAccount = warmupService.getAccount(account.getId()).orElse(null);
        assertEquals(10.5, updatedAccount.getTrustScore(), 0.0001);
    }
}

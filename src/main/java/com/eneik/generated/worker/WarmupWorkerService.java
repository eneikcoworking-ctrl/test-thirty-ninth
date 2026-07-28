package com.eneik.generated.worker;

import com.eneik.generated.client.TelegramClient;
import com.eneik.generated.entity.TelegramAccount;
import com.eneik.generated.service.WarmupService;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class WarmupWorkerService {

    private final TelegramClient telegramClient;
    private final WarmupService warmupService;
    private final Clock clock;

    public WarmupWorkerService(TelegramClient telegramClient, WarmupService warmupService, Optional<Clock> clockOpt) {
        this.telegramClient = telegramClient;
        this.warmupService = warmupService;
        this.clock = clockOpt.orElse(Clock.systemUTC());
    }


    public void executeChannelWarmup(Long accountId, String targetChannel) {
        TelegramAccount account = warmupService.getAccount(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        String sessionName = account.getSessionName();
        telegramClient.subscribeToChannel(sessionName, targetChannel);
        telegramClient.markRecentPostsAsRead(sessionName, targetChannel);

        warmupService.logCompletedTask(accountId, "CHANNEL_WARMUP", 1.0, clock.instant());
    }


    public void maintainOnlinePresence(Long accountId) {
        TelegramAccount account = warmupService.getAccount(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        String sessionName = account.getSessionName();
        telegramClient.setOnlinePresence(sessionName, true);

        warmupService.logCompletedTask(accountId, "MAINTAIN_PRESENCE", 0.5, clock.instant());
    }
}

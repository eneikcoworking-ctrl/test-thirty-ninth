package com.eneik.generated.worker;

import com.eneik.generated.entity.TelegramAccount;
import com.eneik.generated.repository.TelegramAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WarmupWorkerJob {

    private static final Logger logger = LoggerFactory.getLogger(WarmupWorkerJob.class);

    private final TelegramAccountRepository accountRepository;
    private final WarmupWorkerService warmupWorkerService;

    public WarmupWorkerJob(TelegramAccountRepository accountRepository, WarmupWorkerService warmupWorkerService) {
        this.accountRepository = accountRepository;
        this.warmupWorkerService = warmupWorkerService;
    }

    @Scheduled(fixedDelay = 60000)
    public void runChannelWarmup() {
        List<TelegramAccount> accounts = accountRepository.findAll();
        for (TelegramAccount account : accounts) {
            try {
                warmupWorkerService.executeChannelWarmup(account.getId(), "default_channel");
            } catch (Exception e) {
                logger.error("Failed to execute channel warmup for account id {}", account.getId(), e);
            }
        }
    }

    @Scheduled(fixedDelay = 120000)
    public void runPresenceMaintainer() {
        List<TelegramAccount> accounts = accountRepository.findAll();
        for (TelegramAccount account : accounts) {
            try {
                warmupWorkerService.maintainOnlinePresence(account.getId());
            } catch (Exception e) {
                logger.error("Failed to maintain online presence for account id {}", account.getId(), e);
            }
        }
    }
}

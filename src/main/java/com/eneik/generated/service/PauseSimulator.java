package com.eneik.generated.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Random;

@Component
public class PauseSimulator {
    private static final Logger log = LoggerFactory.getLogger(PauseSimulator.class);

    private final Random random;
    private final boolean skipActualSleep;

    public PauseSimulator() {
        this.random = new Random();
        this.skipActualSleep = false;
    }

    // For test injectability/seeding
    public PauseSimulator(Random random, boolean skipActualSleep) {
        this.random = random;
        this.skipActualSleep = skipActualSleep;
    }

    public int calculatePauseSeconds() {
        // Enforce randomized pauses (120–300 sec)
        // 120 to 300 inclusive. 300 - 120 = 180, so bound is 181
        return 120 + random.nextInt(181);
    }

    public void applyPause(int seconds) {
        log.info("Emulating human behavior: applying pause of {} seconds.", seconds);
        if (skipActualSleep) {
            log.info("Skipping actual sleep in simulation.");
            return;
        }
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Pause simulation interrupted", e);
        }
    }
}

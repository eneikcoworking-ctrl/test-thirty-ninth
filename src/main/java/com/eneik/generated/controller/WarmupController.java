package com.eneik.generated.controller;

import com.eneik.generated.entity.TelegramAccount;
import com.eneik.generated.service.WarmupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/warmup")
public class WarmupController {

    private final WarmupService warmupService;

    public WarmupController(WarmupService warmupService) {
        this.warmupService = warmupService;
    }

    @GetMapping("/next-action-delay")
    public ResponseEntity<Map<String, Object>> getNextActionDelay(
            @RequestParam(name = "lambda", defaultValue = "0.01") double lambda) {
        try {
            double delay = warmupService.calculateNextActionDelay(lambda, null);
            return ResponseEntity.ok(Map.of("delay", delay));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/accounts/{id}/assign-outreach")
    public ResponseEntity<Map<String, Object>> assignToOutreach(@PathVariable("id") Long id) {
        try {
            TelegramAccount account = warmupService.assignToOutreach(id);
            return ResponseEntity.ok(Map.of(
                    "id", account.getId(),
                    "sessionName", account.getSessionName(),
                    "warmupStage", account.getWarmupStage(),
                    "trustScore", account.getTrustScore()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}

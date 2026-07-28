package com.eneik.generated.controller;

import com.eneik.generated.campaign.model.Campaign;
import com.eneik.generated.campaign.repository.CampaignRepository;
import com.eneik.generated.service.LeadImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/campaigns")
public class LeadImportController {

    private final LeadImportService leadImportService;
    private final CampaignRepository campaignRepository;

    public LeadImportController(LeadImportService leadImportService, CampaignRepository campaignRepository) {
        this.leadImportService = leadImportService;
        this.campaignRepository = campaignRepository;
    }

    @PostMapping("/{campaignId}/import-leads")
    public ResponseEntity<?> importLeads(
            @PathVariable("campaignId") Long campaignId,
            @RequestParam("file") MultipartFile file) {

        if (!campaignRepository.existsById(campaignId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Campaign with ID " + campaignId + " not found."));
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Uploaded file is empty."));
        }

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            String taskId = UUID.randomUUID().toString();

            leadImportService.registerTask(taskId, campaignId);
            leadImportService.importLeadsAsync(taskId, campaignId, content);

            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("campaignId", campaignId);
            response.put("status", "PENDING");
            response.put("message", "Lead import started asynchronously.");

            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to read file: " + e.getMessage()));
        }
    }

    @PostMapping("/{campaignId}/import-leads/raw")
    public ResponseEntity<?> importLeadsRaw(
            @PathVariable("campaignId") Long campaignId,
            @RequestBody String content) {

        if (!campaignRepository.existsById(campaignId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Campaign with ID " + campaignId + " not found."));
        }

        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Content is empty."));
        }

        String taskId = UUID.randomUUID().toString();

        leadImportService.registerTask(taskId, campaignId);
        leadImportService.importLeadsAsync(taskId, campaignId, content);

        Map<String, Object> response = new HashMap<>();
        response.put("taskId", taskId);
        response.put("campaignId", campaignId);
        response.put("status", "PENDING");
        response.put("message", "Lead import started asynchronously.");

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/import-tasks/{taskId}")
    public ResponseEntity<?> getImportTaskStatus(@PathVariable("taskId") String taskId) {
        LeadImportService.ImportTaskStatus taskStatus = leadImportService.getImportStatus(taskId);
        if (taskStatus == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Import task with ID " + taskId + " not found."));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("taskId", taskStatus.getTaskId());
        response.put("campaignId", taskStatus.getCampaignId());
        response.put("status", taskStatus.getStatus());
        response.put("totalRows", taskStatus.getTotalRows());
        response.put("processedRows", taskStatus.getProcessedRows());
        response.put("skippedRows", taskStatus.getSkippedRows());
        response.put("errorMessage", taskStatus.getErrorMessage());

        return ResponseEntity.ok(response);
    }
}

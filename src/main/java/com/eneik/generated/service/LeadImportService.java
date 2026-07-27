package com.eneik.generated.service;

import com.eneik.generated.campaign.model.Campaign;
import com.eneik.generated.campaign.model.Lead;
import com.eneik.generated.campaign.repository.CampaignRepository;
import com.eneik.generated.campaign.repository.LeadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LeadImportService {

    private static final Logger logger = LoggerFactory.getLogger(LeadImportService.class);

    private final CampaignRepository campaignRepository;
    private final LeadRepository leadRepository;

    // Thread-safe in-memory task tracker
    private final Map<String, ImportTaskStatus> taskTracker = new ConcurrentHashMap<>();

    public LeadImportService(CampaignRepository campaignRepository, LeadRepository leadRepository) {
        this.campaignRepository = campaignRepository;
        this.leadRepository = leadRepository;
    }

    public static class ImportTaskStatus {
        private final String taskId;
        private final Long campaignId;
        private String status;
        private int totalRows;
        private int processedRows;
        private int skippedRows;
        private String errorMessage;

        public ImportTaskStatus(String taskId, Long campaignId, String status) {
            this.taskId = taskId;
            this.campaignId = campaignId;
            this.status = status;
        }

        public String getTaskId() { return taskId; }
        public Long getCampaignId() { return campaignId; }
        public synchronized String getStatus() { return status; }
        public synchronized void setStatus(String status) { this.status = status; }
        public synchronized int getTotalRows() { return totalRows; }
        public synchronized void setTotalRows(int totalRows) { this.totalRows = totalRows; }
        public synchronized int getProcessedRows() { return processedRows; }
        public synchronized void incrementProcessedRows(int count) { this.processedRows += count; }
        public synchronized int getSkippedRows() { return skippedRows; }
        public synchronized void incrementSkippedRows(int count) { this.skippedRows += count; }
        public synchronized String getErrorMessage() { return errorMessage; }
        public synchronized void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }

    public ImportTaskStatus getImportStatus(String taskId) {
        return taskTracker.get(taskId);
    }

    public void registerTask(String taskId, Long campaignId) {
        taskTracker.put(taskId, new ImportTaskStatus(taskId, campaignId, "PENDING"));
    }

    @Async
    public void importLeadsAsync(String taskId, Long campaignId, String content) {
        ImportTaskStatus task = taskTracker.get(taskId);
        if (task == null) {
            task = new ImportTaskStatus(taskId, campaignId, "PROCESSING");
            taskTracker.put(taskId, task);
        } else {
            task.setStatus("PROCESSING");
        }

        try {
            Optional<Campaign> campaignOpt = campaignRepository.findById(campaignId);
            if (campaignOpt.isEmpty()) {
                String errorMsg = "Campaign with ID " + campaignId + " not found.";
                task.setStatus("FAILED");
                task.setErrorMessage(errorMsg);
                logger.error(errorMsg);
                return;
            }
            Campaign campaign = campaignOpt.get();

            List<String> lines = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
            }
            task.setTotalRows(lines.size());

            List<Lead> batch = new ArrayList<>();
            int batchSize = 500;
            boolean isFirstLine = true;

            for (String rawLine : lines) {
                if (rawLine == null) continue;
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    task.incrementSkippedRows(1);
                    continue;
                }

                // Header detection for the very first non-empty line
                if (isFirstLine) {
                    isFirstLine = false;
                    if (isHeaderLine(line)) {
                        task.incrementSkippedRows(1);
                        logger.info("Skipped header line: {}", line);
                        continue;
                    }
                }

                try {
                    Lead lead = parseLine(line);
                    if (lead == null) {
                        task.incrementSkippedRows(1);
                        logger.warn("Skipped malformed/invalid row format: {}", line);
                        continue;
                    }

                    lead.setCampaign(campaign);
                    lead.setStatus("PENDING");
                    batch.add(lead);

                    if (batch.size() >= batchSize) {
                        leadRepository.saveAll(batch);
                        leadRepository.flush();
                        task.incrementProcessedRows(batch.size());
                        batch.clear();
                    }
                } catch (Exception e) {
                    task.incrementSkippedRows(1);
                    logger.error("Exception parsing or saving row: " + line, e);
                }
            }

            // Save remaining batch
            if (!batch.isEmpty()) {
                leadRepository.saveAll(batch);
                leadRepository.flush();
                task.incrementProcessedRows(batch.size());
            }

            task.setStatus("COMPLETED");
            logger.info("Import task {} completed successfully. Processed: {}, Skipped: {}",
                    taskId, task.getProcessedRows(), task.getSkippedRows());

        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            logger.error("Failed executing import task " + taskId, e);
        }
    }

    private boolean isHeaderLine(String line) {
        String lower = line.toLowerCase();
        return lower.contains("username") || lower.contains("phone") || lower.contains("phone_number");
    }

    private Lead parseLine(String line) {
        String username = null;
        String phone = null;

        if (line.contains(",")) {
            String[] parts = line.split(",", -1);
            if (parts.length >= 2) {
                String p1 = parts[0].trim();
                String p2 = parts[1].trim();

                boolean p1IsPhone = isPhoneNumber(p1);
                boolean p2IsPhone = isPhoneNumber(p2);

                if (p1IsPhone && !p2IsPhone) {
                    phone = p1;
                    username = p2;
                } else if (p2IsPhone && !p1IsPhone) {
                    phone = p2;
                    username = p1;
                } else {
                    // Fallback to first as username, second as phone
                    username = p1;
                    phone = p2;
                }

                // If username is specified but invalid, or phone is specified but invalid, the entire row format is invalid
                String cleanPhone = null;
                if (!phone.isEmpty()) {
                    cleanPhone = phone.replaceAll("[\\s\\-()]", "");
                    if (!cleanPhone.matches("^\\+?[0-9]{7,15}$")) {
                        return null; // Invalid phone column format!
                    }
                }

                String cleanUsername = null;
                if (!username.isEmpty()) {
                    cleanUsername = username.startsWith("@") ? username.substring(1) : username;
                    if (!cleanUsername.matches("^[a-zA-Z0-9_]{3,32}$")) {
                        return null; // Invalid username column format!
                    }
                }

                if (cleanPhone == null && cleanUsername == null) {
                    return null;
                }

                return new Lead(cleanUsername, cleanPhone, "PENDING", null);
            } else if (parts.length == 1) {
                String p = parts[0].trim();
                if (isPhoneNumber(p)) {
                    String clean = p.replaceAll("[\\s\\-()]", "");
                    return new Lead(null, clean, "PENDING", null);
                } else if (isValidUsername(p)) {
                    String clean = p.startsWith("@") ? p.substring(1) : p;
                    return new Lead(clean, null, "PENDING", null);
                }
            }
        } else {
            if (isPhoneNumber(line)) {
                String clean = line.replaceAll("[\\s\\-()]", "");
                return new Lead(null, clean, "PENDING", null);
            } else if (isValidUsername(line)) {
                String clean = line.startsWith("@") ? line.substring(1) : line;
                return new Lead(clean, null, "PENDING", null);
            }
        }

        return null;
    }

    private boolean isPhoneNumber(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        String clean = value.replaceAll("[\\s\\-()]", "");
        return clean.matches("^\\+?[0-9]{7,15}$");
    }

    private boolean isValidUsername(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        String clean = value.startsWith("@") ? value.substring(1) : value;
        return clean.matches("^[a-zA-Z0-9_]{3,32}$");
    }
}

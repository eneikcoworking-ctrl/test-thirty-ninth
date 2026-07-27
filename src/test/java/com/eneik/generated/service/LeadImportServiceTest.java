package com.eneik.generated.service;

import com.eneik.generated.campaign.model.Campaign;
import com.eneik.generated.campaign.model.Lead;
import com.eneik.generated.campaign.repository.CampaignRepository;
import com.eneik.generated.campaign.repository.LeadRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class LeadImportServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void givenCampaign_whenImporting10kPhoneNumbers_thenProcessedAsynchronouslyInBatches() throws Exception {
        // Arrange
        Campaign campaign = new Campaign("Bulk Campaign", "Import 10k test", "ACTIVE");
        campaign = campaignRepository.saveAndFlush(campaign);
        Long campaignId = campaign.getId();

        // Generate 10,000 phone numbers
        StringBuilder sb = new StringBuilder();
        sb.append("username,phone_number\n"); // Header
        for (int i = 0; i < 10000; i++) {
            sb.append("user_").append(i).append(",+1").append(String.format("%09d", i)).append("\n");
        }
        String fileContent = sb.toString();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "leads.csv",
                MediaType.TEXT_PLAIN_VALUE,
                fileContent.getBytes()
        );

        // Act - Invoke API
        MvcResult result = mockMvc.perform(multipart("/api/campaigns/" + campaignId + "/import-leads")
                        .file(file))
                .andExpect(status().isAccepted())
                .andReturn();

        String responseStr = result.getResponse().getContentAsString();
        Map<?, ?> responseMap = objectMapper.readValue(responseStr, Map.class);
        String taskId = (String) responseMap.get("taskId");
        assertThat(taskId).isNotNull();

        // Poll for task completion
        long startTime = System.currentTimeMillis();
        boolean completed = false;
        int totalRows = 0;
        int processedRows = 0;
        int skippedRows = 0;

        while (System.currentTimeMillis() - startTime < 15000) { // Max 15s timeout
            MvcResult statusResult = mockMvc.perform(get("/api/campaigns/import-tasks/" + taskId))
                    .andExpect(status().isOk())
                    .andReturn();

            String statusStr = statusResult.getResponse().getContentAsString();
            Map<?, ?> statusMap = objectMapper.readValue(statusStr, Map.class);

            String statusValue = (String) statusMap.get("status");
            totalRows = (Integer) statusMap.get("totalRows");
            processedRows = (Integer) statusMap.get("processedRows");
            skippedRows = (Integer) statusMap.get("skippedRows");

            if ("COMPLETED".equals(statusValue) || "FAILED".equals(statusValue)) {
                completed = true;
                break;
            }
            Thread.sleep(200);
        }

        // Assert
        assertThat(completed).isTrue();
        assertThat(totalRows).isEqualTo(10001); // 10,000 leads + 1 header row
        assertThat(processedRows).isEqualTo(10000);
        assertThat(skippedRows).isEqualTo(1); // Header row is skipped

        // Verify database persistence
        List<Lead> leads = leadRepository.findByCampaignId(campaignId);
        assertThat(leads).hasSize(10000);
        assertThat(leads.get(0).getStatus()).isEqualTo("PENDING");
    }

    @Test
    public void givenMixedValidAndMalformedRows_whenImported_thenInvalidRowsAreSkippedAndLogged() throws Exception {
        // Arrange
        Campaign campaign = new Campaign("Mixed Campaign", "Import mixed rows test", "ACTIVE");
        campaign = campaignRepository.saveAndFlush(campaign);
        Long campaignId = campaign.getId();

        // Mixed rows: 2 valid, 4 invalid/empty/comment, 1 header
        String content = "username,phone_number\n" +                    // Header -> skip (1)
                "john_doe,+12345678901\n" +                            // Valid (1)
                "   \n" +                                              // Blank -> skip (2)
                "# comment line\n" +                                   // Comment -> skip (3)
                "invalid_phone,not-a-number\n" +                        // Invalid phone and username -> skip (4)
                ",\n" +                                                // Empty values -> skip (5)
                "+0987654321,jane_smith\n";                           // Valid (alternate order auto-detected) (2)

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "mixed.csv",
                MediaType.TEXT_PLAIN_VALUE,
                content.getBytes()
        );

        // Act - Invoke API
        MvcResult result = mockMvc.perform(multipart("/api/campaigns/" + campaignId + "/import-leads")
                        .file(file))
                .andExpect(status().isAccepted())
                .andReturn();

        String responseStr = result.getResponse().getContentAsString();
        Map<?, ?> responseMap = objectMapper.readValue(responseStr, Map.class);
        String taskId = (String) responseMap.get("taskId");

        // Poll for task completion
        long startTime = System.currentTimeMillis();
        boolean completed = false;
        int totalRows = 0;
        int processedRows = 0;
        int skippedRows = 0;

        while (System.currentTimeMillis() - startTime < 5000) {
            MvcResult statusResult = mockMvc.perform(get("/api/campaigns/import-tasks/" + taskId))
                    .andExpect(status().isOk())
                    .andReturn();

            String statusStr = statusResult.getResponse().getContentAsString();
            Map<?, ?> statusMap = objectMapper.readValue(statusStr, Map.class);

            String statusValue = (String) statusMap.get("status");
            totalRows = (Integer) statusMap.get("totalRows");
            processedRows = (Integer) statusMap.get("processedRows");
            skippedRows = (Integer) statusMap.get("skippedRows");

            if ("COMPLETED".equals(statusValue) || "FAILED".equals(statusValue)) {
                completed = true;
                break;
            }
            Thread.sleep(100);
        }

        // Assert
        assertThat(completed).isTrue();
        assertThat(totalRows).isEqualTo(7);
        assertThat(processedRows).isEqualTo(2); // john_doe and jane_smith
        assertThat(skippedRows).isEqualTo(5); // Header, blank, comment, invalid_phone, empty comma

        // Verify database persistence
        List<Lead> leads = leadRepository.findByCampaignId(campaignId);
        assertThat(leads).hasSize(2);
        assertThat(leads).extracting(Lead::getUsername).containsExactlyInAnyOrder("john_doe", "jane_smith");
        assertThat(leads).extracting(Lead::getPhoneNumber).containsExactlyInAnyOrder("+12345678901", "+0987654321");
    }

    @Test
    public void givenNonExistentCampaign_whenImportRequestReceived_thenReturnNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(multipart("/api/campaigns/999999/import-leads")
                        .file(new MockMultipartFile("file", "test.csv", MediaType.TEXT_PLAIN_VALUE, "data".getBytes())))
                .andExpect(status().isNotFound());
    }
}

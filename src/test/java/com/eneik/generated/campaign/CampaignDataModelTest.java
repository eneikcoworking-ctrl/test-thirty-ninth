package com.eneik.generated.campaign;

import com.eneik.generated.campaign.model.Campaign;
import com.eneik.generated.campaign.model.Lead;
import com.eneik.generated.campaign.model.SpintaxTemplate;
import com.eneik.generated.campaign.repository.CampaignRepository;
import com.eneik.generated.campaign.repository.LeadRepository;
import com.eneik.generated.campaign.repository.SpintaxTemplateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class CampaignDataModelTest {

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private SpintaxTemplateRepository spintaxTemplateRepository;

    @Test
    public void givenNewCampaign_whenSaved_thenCorrectlyLinksToTargetLeadsAndMessageTemplates() {
        // Arrange
        Campaign campaign = new Campaign("Promo Campaign", "Q3 Lead Gen Campaign", "ACTIVE");

        Lead lead1 = new Lead("john_doe", "+1234567890", "PENDING", "{\"source\": \"LinkedIn\"}");
        Lead lead2 = new Lead("jane_smith", "+0987654321", "PENDING", "{\"source\": \"Twitter\"}");

        SpintaxTemplate template1 = new SpintaxTemplate("{Hi|Hello} {there|friend}!");
        SpintaxTemplate template2 = new SpintaxTemplate("Check out our {awesome|cool} service.");

        campaign.addLead(lead1);
        campaign.addLead(lead2);
        campaign.addSpintaxTemplate(template1);
        campaign.addSpintaxTemplate(template2);

        // Act
        Campaign savedCampaign = campaignRepository.save(campaign);
        campaignRepository.flush();

        // Assert
        assertThat(savedCampaign.getId()).isNotNull();

        // Reload from database to verify persistence and linkage
        Campaign reloadedCampaign = campaignRepository.findById(savedCampaign.getId()).orElseThrow();
        assertThat(reloadedCampaign.getName()).isEqualTo("Promo Campaign");
        assertThat(reloadedCampaign.getLeads()).hasSize(2);
        assertThat(reloadedCampaign.getSpintaxTemplates()).hasSize(2);

        // Verify Leads linkage
        List<Lead> reloadedLeads = leadRepository.findByCampaignId(reloadedCampaign.getId());
        assertThat(reloadedLeads).hasSize(2);
        assertThat(reloadedLeads).extracting(Lead::getUsername).containsExactlyInAnyOrder("john_doe", "jane_smith");

        // Verify Spintax Templates linkage
        List<SpintaxTemplate> reloadedTemplates = spintaxTemplateRepository.findByCampaignId(reloadedCampaign.getId());
        assertThat(reloadedTemplates).hasSize(2);
        assertThat(reloadedTemplates).extracting(SpintaxTemplate::getTemplateText)
                .containsExactlyInAnyOrder("{Hi|Hello} {there|friend}!", "Check out our {awesome|cool} service.");
    }

    @Test
    public void givenThousandsOfLeads_whenStoredInLeadsTable_thenIndexedAndRetrievedEfficiently() {
        // Arrange
        Campaign campaign = new Campaign("High Volume Campaign", "Massive lead import", "ACTIVE");
        campaignRepository.save(campaign);

        int count = 5000;
        List<Lead> leads = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Lead lead = new Lead("user_" + i, "+1" + String.format("%09d", i), "PENDING", null);
            lead.setCampaign(campaign);
            leads.add(lead);
        }

        // Act - Store the leads
        long startInsert = System.currentTimeMillis();
        leadRepository.saveAll(leads);
        leadRepository.flush();
        long endInsert = System.currentTimeMillis();

        // Act - Retrieve leads by campaign ID
        long startQuery = System.currentTimeMillis();
        List<Lead> queriedLeads = leadRepository.findByCampaignId(campaign.getId());
        long endQuery = System.currentTimeMillis();

        // Assert
        assertThat(queriedLeads).hasSize(count);

        long insertDuration = endInsert - startInsert;
        long queryDuration = endQuery - startQuery;

        System.out.println("Inserted " + count + " leads in: " + insertDuration + " ms");
        System.out.println("Queried " + count + " leads by indexed campaignId in: " + queryDuration + " ms");

        // Querying indexed field should be extremely fast, within a reasonable warm-up buffer for virtualization (usually < 1000ms)
        assertThat(queryDuration).isLessThan(1000);
    }
}

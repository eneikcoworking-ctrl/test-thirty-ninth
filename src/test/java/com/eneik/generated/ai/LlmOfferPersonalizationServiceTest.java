package com.eneik.generated.ai;

import com.eneik.generated.campaign.model.Lead;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmOfferPersonalizationServiceTest {

    private LlmClient mockLlmClient;
    private LlmOfferPersonalizationService service;

    @BeforeEach
    void setUp() {
        mockLlmClient = mock(LlmClient.class);
        service = new LlmOfferPersonalizationService(mockLlmClient);
    }

    @Test
    void testPersonalizeOffer_success() throws Exception {
        // Given lead bio data
        Lead lead = new Lead();
        lead.setId(1L);
        lead.setMetadata("CTO at TechCorp, interested in AI");
        String baseOffer = "Hello, check out our new product.";

        when(mockLlmClient.generateResponse(anyString())).thenReturn("Hello CTO, check out our new AI product.");

        // When passed to the LLM agent
        String result = service.personalizeOffer(lead, baseOffer);

        // Then a customized outreach message is generated
        assertEquals("Hello CTO, check out our new AI product.", result);
    }

    @Test
    void testPersonalizeOffer_fallbackOnLengthExceeded() throws Exception {
        // Given a strict prompt template
        Lead lead = new Lead();
        lead.setId(2L);
        lead.setMetadata("Engineer");
        String baseOffer = "Short offer.";

        String longResponse = "A".repeat(301); // Exceeds 300 chars
        when(mockLlmClient.generateResponse(anyString())).thenReturn(longResponse);

        // When evaluated
        String result = service.personalizeOffer(lead, baseOffer);

        // Then the LLM output strictly adheres to the requested length (falls back to base offer)
        assertEquals(baseOffer, result);
    }

    @Test
    void testPersonalizeOffer_fallbackOnAiRefusal() throws Exception {
        Lead lead = new Lead();
        lead.setId(3L);
        lead.setMetadata("Engineer");
        String baseOffer = "Short offer.";

        when(mockLlmClient.generateResponse(anyString())).thenReturn("I cannot fulfill this request as an AI.");

        String result = service.personalizeOffer(lead, baseOffer);

        // Falls back to base offer
        assertEquals(baseOffer, result);
    }

    @Test
    void testPersonalizeOffer_fallbackOnException() throws Exception {
        Lead lead = new Lead();
        lead.setId(4L);
        lead.setMetadata("Engineer");
        String baseOffer = "Short offer.";

        when(mockLlmClient.generateResponse(anyString())).thenThrow(new RuntimeException("API error"));

        String result = service.personalizeOffer(lead, baseOffer);

        // Falls back to base offer
        assertEquals(baseOffer, result);
    }

    @Test
    void testPersonalizeOffer_fallbackOnEmptyMetadata() {
        Lead lead = new Lead();
        lead.setId(5L);
        lead.setMetadata(null);
        String baseOffer = "Short offer.";

        String result = service.personalizeOffer(lead, baseOffer);

        // Falls back to base offer immediately without calling LLM
        assertEquals(baseOffer, result);
    }
}

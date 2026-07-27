package com.eneik.generated.campaign.dto;

import java.util.List;

public record CampaignCreateRequest(
    String name,
    String description,
    List<String> spintaxTemplates,
    boolean useLlmPersonalization
) {}

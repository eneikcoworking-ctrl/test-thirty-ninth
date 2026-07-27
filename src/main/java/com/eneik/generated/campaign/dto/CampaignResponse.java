package com.eneik.generated.campaign.dto;

import java.time.LocalDateTime;

public record CampaignResponse(
    Long id,
    String name,
    String description,
    String status,
    boolean useLlmPersonalization,
    LocalDateTime createdAt
) {}

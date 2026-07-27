package com.eneik.generated.campaign.dto;

public record LeadImportResponse(
    Long campaignId,
    int importedCount,
    String status,
    String message
) {}

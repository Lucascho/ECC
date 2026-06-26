package com.example.campaigntouch.campaign;

import java.time.LocalDateTime;

public record CampaignResponse(
        Long id,
        String name,
        CampaignType type,
        String description,
        String landingPageUrl,
        LocalDateTime startTime,
        LocalDateTime endTime,
        CampaignStatus status,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CampaignResponse from(Campaign campaign) {
        return new CampaignResponse(
                campaign.getId(),
                campaign.getName(),
                campaign.getType(),
                campaign.getDescription(),
                campaign.getLandingPageUrl(),
                campaign.getStartTime(),
                campaign.getEndTime(),
                campaign.getStatus(),
                campaign.getCreatedBy(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }
}

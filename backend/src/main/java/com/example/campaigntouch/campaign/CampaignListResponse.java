package com.example.campaigntouch.campaign;

import java.time.LocalDateTime;

public record CampaignListResponse(
        Long id,
        String name,
        CampaignType type,
        LocalDateTime startTime,
        LocalDateTime endTime,
        CampaignStatus status,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CampaignListResponse from(Campaign campaign) {
        return new CampaignListResponse(
                campaign.getId(),
                campaign.getName(),
                campaign.getType(),
                campaign.getStartTime(),
                campaign.getEndTime(),
                campaign.getStatus(),
                campaign.getCreatedBy(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }
}
